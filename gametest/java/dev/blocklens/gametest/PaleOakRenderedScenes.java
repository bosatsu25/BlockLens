package dev.blocklens.gametest;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.fabric.BlockLensConfigScreen;
import dev.blocklens.testing.PaleOakObjectFrames;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/** Actual boat/entity/editor rendering with absent-object, ON and restoration controls. */
final class PaleOakRenderedScenes {
    private static final BlockPos TARGET=new BlockPos(0,-59,4);
    private static final String[] ROLES={"boat","chest-boat","standing-sign","hanging-sign","sign-gui","hanging-sign-gui"};
    private static final PaleOakObjectFrames.Region WORLD=new PaleOakObjectFrames.Region(220,130,420,250);
    private static final PaleOakObjectFrames.Region GUI=new PaleOakObjectFrames.Region(100,60,540,300);
    private PaleOakRenderedScenes() { }
    static void verify(ClientGameTestContext context,TestSingleplayerContext world,Path parent,BlockLensConfig offConfig) throws IOException {
        Path output=parent.resolveSibling("pale-oak-objects");Files.createDirectories(output);
        Files.writeString(output.resolve("palette-objects-manifest.txt"),"status=started\nminecraft="+BlockLensRuntime.minecraftVersion()+"\n");
        var server=world.getServer();
        server.runCommand("tick freeze");
        try {
            context.runOnClient(c->BlockLensRuntime.installConfig(offConfig));PaleOakPaletteOracle.reload(context);
            for(int role=0;role<ROLES.length;role++) {
                boolean gui=role>=4,hanging=role==3 || role==5;
                server.runCommand("kill @e[type=!minecraft:player]");
                server.runCommand("fill -12 -59 -8 12 -45 20 minecraft:air");
                server.runCommand("fill -12 -60 -8 12 -60 20 minecraft:black_concrete");
                server.runCommand("tp @a 0.5 -56.5 11.5 180 25");
                context.waitFor(PaleOakRenderedScenes::clearedSceneReady,200);
                context.waitTicks(20);
                BufferedImage air;
                if(gui) {
                    placeSign(world,hanging);
                    context.waitTicks(20);
                    openEditor(context,hanging);PaletteSignBackground.hide(true);
                    air=PaleOakPaletteOracle.capture(context,output,ROLES[role]+"-air");
                    PaletteSignBackground.hide(false);
                } else {
                    air=PaleOakPaletteOracle.capture(context,output,ROLES[role]+"-air");
                    if(role<2) server.runCommand("summon minecraft:pale_oak_"+(role==0 ? "boat" : "chest_boat")+" 0.5 -59 4.5");
                    else placeSign(world,hanging);
                }
                context.waitTicks(20);
                var off=PaleOakPaletteOracle.capture(context,output,ROLES[role]+"-off");
                context.runOnClient(c->BlockLensConfigScreen.show(c,null));PaleOakPaletteOracle.saveUiToggle(context);
                if(gui) openEditor(context,hanging);
                var on=PaleOakPaletteOracle.capture(context,output,ROLES[role]+"-on");
                var region=gui ? GUI : WORLD;
                require(PaleOakObjectFrames.changed(off,on,air,region),"Rendered pale-oak chroma/grain/foreground absent: "+ROLES[role]);
                context.runOnClient(c->BlockLensConfigScreen.show(c,null));PaleOakPaletteOracle.saveUiToggle(context);
                if(gui) openEditor(context,hanging);
                var restored=PaleOakPaletteOracle.capture(context,output,ROLES[role]+"-restored");
                require(PaleOakObjectFrames.restored(off,restored,air,region),"Rendered pale-oak restoration failed: "+ROLES[role]);
                context.runOnClient(c->BlockLensConfigScreen.show(c,null));
            }
            Files.writeString(output.resolve("palette-objects-manifest.txt"),"minecraft="+BlockLensRuntime.minecraftVersion()
                    +"\nroles=6\nactualBoats=true\nactualSignEntities=true\nactualSignEditors=true\nforegroundRetained=true"
                    +"\nwoodChroma=true\ngrainRetained=true\noffRestored=true\nblankFixtures=true\n");
            System.out.println("BLOCKLENS_PALE_OAK_OBJECTS roles=6 boats=true signs=true editors=true foreground=true grain=true restored=true");
        } finally {
            PaletteSignBackground.hide(false);context.runOnClient(c->BlockLensConfigScreen.show(c,null));server.runCommand("tick unfreeze");
        }
    }
    private static boolean clearedSceneReady(net.minecraft.client.Minecraft client) {
        if(client.level==null || client.player==null || !client.level.getBlockState(TARGET).isAir()
                || !client.level.getBlockState(TARGET.above()).isAir()
                || Math.abs(client.player.getX()-.5)>.01 || Math.abs(client.player.getY()+56.5)>.01
                || Math.abs(client.player.getZ()-11.5)>.01 || Math.abs(client.player.getXRot()-25)>.01
                || Math.abs(Math.abs(client.player.getYRot()%360)-180)>.01) return false;
        for(var entity:client.level.entitiesForRendering()) if(!(entity instanceof net.minecraft.world.entity.player.Player)) return false;
        return true;
    }
    private static void placeSign(TestSingleplayerContext world,boolean hanging) {
        var server=world.getServer();
        if(hanging) server.runCommand("setblock 0 -58 4 minecraft:white_concrete");
        server.runCommand("setblock 0 -59 4 minecraft:pale_oak_"+(hanging ? "hanging_sign[rotation=0]" : "sign[rotation=0]"));
    }
    private static void openEditor(ClientGameTestContext context,boolean hanging) {
        context.runOnClient(c->{
            if(!(c.level.getBlockEntity(TARGET) instanceof SignBlockEntity sign)) throw new AssertionError("Blank native sign missing");
            BlockLensConfigScreen.show(c,PaletteSignScreens.create(sign,hanging));
        });context.waitTicks(10);
    }
    private static void require(boolean condition,String message) {if(!condition) throw new AssertionError(message);}
}
