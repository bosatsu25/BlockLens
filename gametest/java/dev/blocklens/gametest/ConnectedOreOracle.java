package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.io.IOException;
import java.nio.file.Files;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;

/** Actual-render regression: incompatible neighbor geometry must retain both borders. */
final class ConnectedOreOracle {
    private ConnectedOreOracle() { }
    static void verify(ClientGameTestContext context,TestSingleplayerContext world){
        var original=BlockLensRuntime.config();
        var output=FabricLoader.getInstance().getGameDir().resolve("connected-visual");
        try {
            Files.createDirectories(output);
            Files.writeString(output.resolve("connected-manifest.txt"),"status=started\nminecraft="+BlockLensRuntime.minecraftVersion()+"\n");
            var server=world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");server.runCommand("weather clear");server.runCommand("time set noon");
            server.runCommand("gamemode spectator @a");server.runCommand("tick freeze");
            server.runCommand("fill -12 -59 -8 12 -45 20 minecraft:air");server.runCommand("fill -11 -60 -7 11 -60 19 minecraft:black_concrete");
            server.runCommand("tp @a 0.5 -57.38 10 180 0");
            var enabled=original;for(var id:CapabilityId.values())enabled=enabled.withEnabled(id,false);
            enabled=enabled.withEnabled(CapabilityId.DIAMOND_ORE,true).withEnabled(CapabilityId.CONNECTED_ORE_EDGES,true);
            var on=enabled;
            for(int shape=0;shape<=5;shape++){
                server.runCommand("setblock 0 -59 4 "+OreExtensionFixture.GEOMETRY+"[shape=0]");
                server.runCommand("setblock 1 -59 4 "+OreExtensionFixture.GEOMETRY+"[shape="+shape+"]");
                context.waitTicks(20);
                context.runOnClient(c->{ConnectedOreProbe.reset();BlockLensRuntime.installConfig(on);MinecraftTerrainInvalidator.invalidateAll(c);});
                context.waitTicks(30);
                context.takeScreenshot(TestScreenshotOptions.of("geometry-"+shape).withDestinationDir(output).withSize(640,360).disableCounterPrefix());
                require(ConnectedOreProbe.only(shape==0 || shape==5 ? 2 : 0),"Connected border linked incompatible neighbor shape="+shape+" observed="+ConnectedOreProbe.observed());
                require(shape==0 || shape==5 ? ConnectedOreProbe.onlyAt(1,8) : ConnectedOreProbe.absentAt(1),
                        "Neighbor geometry boundary is asymmetric: "+shape+" observed="+ConnectedOreProbe.observed());
            }
            Files.writeString(output.resolve("connected-manifest.txt"),"minecraft="+BlockLensRuntime.minecraftVersion()+"\ngeometryFallback=true\n");
        }catch(IOException failure){throw new AssertionError("Connected evidence failed",failure);}
        finally{ConnectedOreProbe.stop();context.runOnClient(c->{BlockLensRuntime.installConfig(original);MinecraftTerrainInvalidator.invalidateAll(c);});}
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
