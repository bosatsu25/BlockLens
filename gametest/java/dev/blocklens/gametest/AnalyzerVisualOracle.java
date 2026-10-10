package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.analyzer.AnalyzerCatalog;
import dev.blocklens.core.analyzer.AnalyzerOptions;
import dev.blocklens.fabric.AnalyzerClient;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

/** Real loaded-client discovery, gizmo framebuffer controls, lifecycle and all-capability reload. */
final class AnalyzerVisualOracle {
    private static final int[] COLORS={0xFF8A24,0xFFDB45,0x62FF87,0x56DFFF,0xFF6DEA};
    private AnalyzerVisualOracle() { }
    static void verify(ClientGameTestContext context,TestSingleplayerContext world) {
        var original=BlockLensRuntime.config();
        var output=FabricLoader.getInstance().getGameDir().resolve("analyzer-visual");
        var evidence=new StringBuilder("minecraft="+BlockLensRuntime.minecraftVersion()+"\n");
        var server=world.getServer();
        boolean bob=context.computeOnClient(c->c.options.bobView().get());
        try {
            Files.createDirectories(output);
            context.runOnClient(c->c.options.bobView().set(false));
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:advance_weather false");
            server.runCommand("gamerule minecraft:random_tick_speed 0");
            server.runCommand("time set midnight");
            server.runCommand("weather clear");
            server.runCommand("gamemode spectator @a");
            server.runCommand("kill @e[type=minecraft:villager]");
            server.runCommand("fill -12 -61 -8 12 -49 20 minecraft:black_concrete hollow");
            server.runCommand("tp @a 0 -56 12 180 25");
            var options=AnalyzerOptions.defaults().with(0,0,16);
            for(int kind=1;kind<5;kind++) options=options.with(kind,0,8).with(kind,1,5);
            var off=all(original,false).withAnalyzerOptions(options);
            for(int kind=0;kind<5;kind++) {
                scene(world,kind);
                install(context,off);
                require(context.computeOnClient(c->AnalyzerClient.engine().markers().isEmpty()),"OFF retained markers");
                sample(context,evidence,"kind"+kind+"Off",true);
                var baseline=read(capture(context,output,"analyzer-"+kind+"-off"));
                var on=off.withEnabled(AnalyzerCatalog.CAPABILITIES.get(kind),true);
                install(context,on);
                int selected=kind;
                if(kind==4) {
                    require(context.computeOnClient(c->villager(c.level).getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()),
                            "missing-memory control already has a site");
                    context.waitTicks(40);
                    require(context.computeOnClient(c->AnalyzerClient.engine().markers().isEmpty()),"inferred unavailable job memory");
                    capture(context,output,"villager-memory-missing");
                    setMemory(context,true,false);
                }
                context.waitFor(c->AnalyzerClient.engine().markers().stream().anyMatch(m->m.kind()==selected),600);
                context.runOnClient(c->{
                    for(var marker:AnalyzerClient.engine().markers()) {
                        require(marker.kind()==selected,"independent analyzer enabled another feature");
                        if(selected==2) require(marker.radius()==20,"one beacon layer radius changed");
                        if(selected==3) require(marker.radius()==128,"rod radius changed");
                    }
                });
                sample(context,evidence,"kind"+kind+"On",false);
                context.waitTicks(3);
                var enabled=read(capture(context,output,"analyzer-"+kind+"-on"));
                int delta=colored(enabled,COLORS[kind])-colored(baseline,COLORS[kind]);
                require(delta>=12,"actual analyzer cue is not visible for kind "+kind+": "+delta);
                evidence.append("kind").append(kind).append("CuePixelDelta=").append(delta).append('\n');
                if(kind==4) {
                    setMemory(context,true,true);
                    context.waitTicks(25);
                    require(context.computeOnClient(c->AnalyzerClient.engine().markers().isEmpty()),"cross-dimension job memory produced a line");
                    setMemory(context,false,false);
                    context.waitTicks(25);
                    require(context.computeOnClient(c->AnalyzerClient.engine().markers().isEmpty()),"erased job memory retained a line");
                    evidence.append("jobMemoryMissing=true\njobMemoryLoaded=true\njobMemoryWrongDimension=true\njobMemoryErased=true\n");
                }
                install(context,off);
                require(context.computeOnClient(c->AnalyzerClient.engine().markers().isEmpty()),"OFF did not clear analyzer");
            }
            scene(world,-1);
            context.waitTicks(25);
            setMemory(context,true,false);
            var combinedOff=read(capture(context,output,"all51-off"));
            var all=all(original,true).withAnalyzerOptions(options);
            require(Long.bitCount(all.enabledMask())==51,"all-capability fixture incomplete");
            install(context,all);
            context.waitFor(c->AnalyzerClient.engine().markers().stream().map(m->m.kind()).distinct().count()==5,600);
            sample(context,evidence,"all51",false);
            verifyCombined(context,output,"all51-on",combinedOff,evidence);
            reload(context);
            context.waitFor(c->AnalyzerClient.engine().markers().stream().map(m->m.kind()).distinct().count()==5,600);
            verifyCombined(context,output,"all51-reloaded",combinedOff,evidence);
            server.runCommand("execute in minecraft:the_nether run tp @a 0 80 0");
            context.waitFor(c->c.level!=null && c.level.dimension().equals(Level.NETHER),1200);
            context.runOnClient(c->require(AnalyzerClient.engine().markers().stream().noneMatch(m->m.kind()==2 || m.kind()==3 || m.kind()==4),
                    "previous-world beacon/rod/village markers survived dimension change"));
            server.runCommand("execute in minecraft:overworld run tp @a 0 -56 12 180 25");
            context.waitFor(c->c.level!=null && c.level.dimension().equals(Level.OVERWORLD),1200);
            setMemory(context,true,false);
            context.waitFor(c->AnalyzerClient.engine().markers().stream().map(m->m.kind()).distinct().count()==5,600);
            verifyCombined(context,output,"all51-dimension-return",combinedOff,evidence);
            install(context,off);
            require(context.computeOnClient(c->AnalyzerClient.engine().markers().isEmpty()),"final OFF retained markers");
            evidence.append("all51Enabled=true\nall51Reload=true\ndimensionCleanup=true\nfinalOffEmpty=true\n");
            Files.writeString(output.resolve("analyzer-visual-manifest.txt"),evidence,StandardCharsets.UTF_8);
            System.out.println("BLOCKLENS_ANALYZER_VISUAL independent=5 actualGizmoPixels=true memoryControls=true all51Reload=true dimensionCleanup=true");
        } catch(IOException e) { throw new AssertionError("analyzer framebuffer evidence failed",e); }
        finally {
            AnalyzerTickProbe.stop();
            BlockLensRuntime.installConfig(original);
            context.runOnClient(c->{c.options.bobView().set(bob);AnalyzerClient.clear();});
        }
    }
    private static void scene(TestSingleplayerContext world,int kind) {
        var server=world.getServer();
        server.runCommand("kill @e[type=minecraft:villager]");
        server.runCommand("fill -11 -60 -7 11 -50 19 minecraft:air");
        server.runCommand("fill -10 -60 -6 10 -60 7 minecraft:black_concrete");
        if(kind==0 || kind==-1) {
            server.runCommand("setblock -3 -59 4 minecraft:ancient_debris");
            if(kind==0) server.runCommand("fill -5 -59 6 -1 -55 6 minecraft:stone");
        }
        if(kind==1 || kind==-1) {
            server.runCommand("fill -1 -60 3 1 -58 5 minecraft:barrier");
            server.runCommand("setblock 0 -59 4 minecraft:lava");
            if(kind==1) server.runCommand("fill -2 -59 6 2 -55 6 minecraft:stone");
        }
        if(kind==2 || kind==-1) {
            server.runCommand("fill 2 -60 3 4 -60 5 minecraft:iron_block");
            server.runCommand("setblock 3 -59 4 minecraft:beacon");
        }
        if(kind==3 || kind==-1) server.runCommand("setblock -6 -59 4 minecraft:lightning_rod");
        if(kind==4 || kind==-1) {
            server.runCommand("setblock 2 -59 6 minecraft:barrel");
            server.runCommand("summon minecraft:villager 0 -59 6 {NoAI:1b,Invulnerable:1b}");
        }
    }
    private static Villager villager(net.minecraft.client.multiplayer.ClientLevel level) {
        for(var entity:level.entitiesForRendering()) if(entity instanceof Villager v && v.isAlive()) return v;
        throw new AssertionError("controlled client villager fixture missing");
    }
    private static void setMemory(ClientGameTestContext context,boolean present,boolean wrongDimension) {
        context.waitFor(c->{
            if(c.level==null) return false;
            for(var entity:c.level.entitiesForRendering()) if(entity instanceof Villager v && v.isAlive()) return true;
            return false;
        },1200);
        context.runOnClient(c->{
            var brain=villager(c.level).getBrain();
            if(present) brain.setMemory(MemoryModuleType.JOB_SITE,GlobalPos.of(wrongDimension?Level.NETHER:c.level.dimension(),new BlockPos(2,-59,6)));
            else brain.eraseMemory(MemoryModuleType.JOB_SITE);
        });
    }
    private static BlockLensConfig all(BlockLensConfig base,boolean enabled) {
        for(var capability:CapabilityId.values()) base=base.withEnabled(capability,enabled);
        return base;
    }
    private static void install(ClientGameTestContext context,BlockLensConfig config) {
        context.runOnClient(c->{BlockLensRuntime.installConfig(config);dev.blocklens.fabric.MinecraftTerrainInvalidator.invalidateAll(c);});context.waitTicks(25);
    }
    private static void reload(ClientGameTestContext context) {
        var future=context.computeOnClient(c->c.reloadResourcePacks());
        context.waitFor(c->future.isDone(),1200);future.join();context.waitFor(SettingsClientAccess::ready,1200);context.waitTicks(25);
    }
    private static Path capture(ClientGameTestContext context,Path output,String name) throws IOException {
        var path=context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(output).withSize(640,360).disableCounterPrefix());
        require(Files.isRegularFile(path) && Files.size(path)>1000,"analyzer screenshot missing");return path;
    }
    private static BufferedImage read(Path path) throws IOException {
        var image=ImageIO.read(path.toFile());require(image!=null && image.getWidth()==640 && image.getHeight()==360,"invalid analyzer framebuffer");return image;
    }
    private static int colored(BufferedImage image,int color) {
        int count=0;
        for(int y=40;y<320;y++) for(int x=30;x<610;x++) {
            int actual=image.getRGB(x,y);
            if(dev.blocklens.testing.CueColorEvidence.matches(actual,color)) count++;
        }
        return count;
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private static void sample(ClientGameTestContext context,StringBuilder evidence,String prefix,boolean off) {
        context.runOnClient(c->AnalyzerTickProbe.begin());
        context.waitTicks(40);
        String sample=context.computeOnClient(c->AnalyzerTickProbe.end(prefix,off));
        evidence.append(sample);
    }
    private static void verifyCombined(ClientGameTestContext context,Path output,String name,
            BufferedImage baseline,StringBuilder evidence) throws IOException {
        var image=read(capture(context,output,name));
        for(int kind=0;kind<5;kind++) {
            int delta=colored(image,COLORS[kind])-colored(baseline,COLORS[kind]);
            require(delta>=12,"combined/reloaded analyzer cue missing: "+name+" kind="+kind+" delta="+delta);
            evidence.append(name).append("Kind").append(kind).append("CuePixelDelta=").append(delta).append('\n');
        }
    }
    static void prepareDisconnect(ClientGameTestContext context,TestSingleplayerContext world) {
        var server=world.getServer();
        server.runCommand("setblock -6 -59 4 minecraft:lightning_rod");
        server.runCommand("tp @a 0 -56 12 180 25");
        var config=all(BlockLensRuntime.config(),false).withEnabled(CapabilityId.LIGHTNING_ROD_RANGE,true)
                .withAnalyzerOptions(AnalyzerOptions.defaults().with(3,0,8).with(3,1,5));
        install(context,config);
        context.waitFor(c->AnalyzerClient.engine().markers().stream().anyMatch(m->m.kind()==3),600);
        require(context.computeOnClient(c->!AnalyzerClient.engine().markers().isEmpty()),"disconnect needs a nonempty baseline");
    }
}
