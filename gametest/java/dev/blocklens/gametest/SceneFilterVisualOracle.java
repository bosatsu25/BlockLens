package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.scene.SceneFilterOptions;
import dev.blocklens.fabric.AnalyzerClient;
import dev.blocklens.fabric.SceneFilterClient;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;

/** Real world meshes, block entities, entity extraction, analyzer visibility and reload restoration. */
final class SceneFilterVisualOracle {
    private static final BlockPos TARGET=new BlockPos(0,-58,4);
    private SceneFilterVisualOracle() { }
    static void verify(ClientGameTestContext context,TestSingleplayerContext world) {
        var original=BlockLensRuntime.config();
        var output=FabricLoader.getInstance().getGameDir().resolve("scene-filter-visual");
        var evidence=new StringBuilder("minecraft="+BlockLensRuntime.minecraftVersion()+"\n");
        var camera=context.computeOnClient(c->c.options.getCameraType());
        boolean bob=context.computeOnClient(c->c.options.bobView().get());
        var off=all(original,false).withSceneFilterOptions(SceneFilterOptions.defaults());
        try {
            Files.createDirectories(output);
            context.runOnClient(c->{c.options.bobView().set(false);c.options.setCameraType(CameraType.FIRST_PERSON);});
            var server=world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:advance_weather false");
            server.runCommand("gamerule minecraft:random_tick_speed 0");
            server.runCommand("time set noon");server.runCommand("weather clear");
            server.runCommand("gamemode spectator @a");
            server.runCommand("kill @e[type=minecraft:armor_stand]");
            server.runCommand("kill @e[type=minecraft:villager]");
            server.runCommand("fill -12 -61 -8 12 -49 20 minecraft:black_concrete hollow");
            clear(world);server.runCommand("tp @a 0 -55 13 180 12");
            server.runCommand("fill -2 -59 3 2 -56 5 minecraft:stone");
            install(context,off);
            var stone=read(capture(context,output,"block-off"));
            var rules=SceneFilterOptions.defaults().withMode(0,SceneFilterOptions.Mode.BLACKLIST)
                    .withList(0,false,"minecraft:stone,minecraft:dirt");
            var blocks=off.withEnabled(CapabilityId.BLOCK_FILTER,true).withSceneFilterOptions(rules);
            install(context,blocks);
            var hidden=read(capture(context,output,"block-blacklist"));
            distinct(stone,hidden,evidence,"blockBlacklist");
            context.runOnClient(c->{
                var state=c.level.getBlockState(TARGET);
                require(state.is(Blocks.STONE) && state.getRenderShape()==RenderShape.MODEL,"filter mutated physical/model state");
                require(state.isSolidRender(),"filter changed solid occlusion");
                require(SceneFilterClient.compiledLookupCount()==2,"configured-ID compilation count differs");
                require(!SceneFilterClient.hiddenBlock(Blocks.CHEST),"blacklist hid unrelated block");
            });
            // An entity-only edit and equivalent decoded options must not expose a published block rule
            // between config publication / terrain invalidation and the next client tick.
            var entityEdit=blocks.withEnabled(CapabilityId.ENTITY_FILTER,true)
                    .withSceneFilterOptions(rules.withEntityPreset(0));
            context.runOnClient(c->{
                BlockLensRuntime.installConfig(entityEdit);
                dev.blocklens.fabric.MinecraftTerrainInvalidator.invalidateAll(c);
                require(SceneFilterClient.hiddenBlock(Blocks.STONE),"entity-only edit dropped published block rule before tick");
            });
            context.waitTicks(25);
            require(different(hidden,read(capture(context,output,"block-entity-options-changed")))<80,
                    "entity-only edit left visible block terrain");
            var equivalent=dev.blocklens.core.BlockLensConfigCodec.decode(
                    dev.blocklens.core.BlockLensConfigCodec.encode(entityEdit));
            context.runOnClient(c->{
                BlockLensRuntime.installConfig(equivalent);
                dev.blocklens.fabric.MinecraftTerrainInvalidator.invalidateAll(c);
                require(SceneFilterClient.hiddenBlock(Blocks.STONE),"equivalent config dropped published block rule before tick");
            });
            context.waitTicks(25);
            require(different(hidden,read(capture(context,output,"block-equal-options")))<80,
                    "equivalent config left visible block terrain");
            install(context,blocks);
            evidence.append("generationContinuity=true\n");
            // Filtering during model reload must not merge stone and dirt into an invisible model group.
            reload(context);
            capture(context,output,"block-hidden-reloaded");
            install(context,off);
            context.runOnClient(c->require(c.getModelManager().requiresRender(Blocks.STONE.defaultBlockState(),Blocks.DIRT.defaultBlockState()),
                    "hidden reload corrupted stone/dirt model groups"));
            var restored=read(capture(context,output,"block-restored"));
            distinct(hidden,restored,evidence,"blockRestored");
            server.runCommand("fill -2 -59 3 2 -56 5 minecraft:dirt");context.waitTicks(25);
            var dirt=read(capture(context,output,"block-dirt-update"));
            distinct(restored,dirt,evidence,"stoneDirtUpdate");
            install(context,blocks.withSceneFilterOptions(rules.withMode(0,SceneFilterOptions.Mode.WHITELIST)
                    .withList(0,true,"minecraft:black_concrete")));
            distinct(dirt,read(capture(context,output,"block-whitelist")),evidence,"blockWhitelist");
            require(context.computeOnClient(c->SceneFilterClient.hiddenBlock(Blocks.DIRT)),"whitelist retained excluded block");
            install(context,blocks.withSceneFilterOptions(rules.withMode(0,SceneFilterOptions.Mode.NONE)));
            require(context.computeOnClient(c->!SceneFilterClient.blockFilterActive() && !SceneFilterClient.hiddenBlock(Blocks.DIRT)
                    && SceneFilterClient.compiledLookupCount()==0),"NONE compiled or hid blocks");
            capture(context,output,"block-none");

            clear(world);server.runCommand("setblock 0 -58 4 minecraft:chest[facing=south]");
            install(context,off);
            require(context.computeOnClient(c->SceneRenderAccess.extracted(c,TARGET)),"baseline chest extraction missing");
            var chest=read(capture(context,output,"chest-off"));
            var chestRules=SceneFilterOptions.defaults().withMode(0,SceneFilterOptions.Mode.BLACKLIST).withList(0,false,"minecraft:chest");
            install(context,blocks.withSceneFilterOptions(chestRules));
            require(context.computeOnClient(c->!SceneRenderAccess.extracted(c,TARGET)),"hidden chest still extracted");
            distinct(chest,read(capture(context,output,"chest-hidden")),evidence,"chestHidden");
            install(context,off);
            require(context.computeOnClient(c->SceneRenderAccess.extracted(c,TARGET)),"OFF did not restore chest extraction");
            var chestRestored=read(capture(context,output,"chest-restored"));
            require(different(chest,chestRestored)<80,"OFF did not restore controlled chest framebuffer");

            clear(world);
            server.runCommand("summon minecraft:armor_stand 0 -59 4 {NoGravity:1b,Invulnerable:1b,ShowArms:1b}");
            install(context,off);
            var entity=read(capture(context,output,"entity-off"));
            var entityRules=SceneFilterOptions.defaults().withMode(1,SceneFilterOptions.Mode.BLACKLIST).withList(1,false,"minecraft:armor_stand");
            var entities=off.withEnabled(CapabilityId.ENTITY_FILTER,true).withSceneFilterOptions(entityRules);
            install(context,entities);
            distinct(entity,read(capture(context,output,"entity-blacklist")),evidence,"entityBlacklist");
            install(context,entities.withSceneFilterOptions(entityRules.withMode(1,SceneFilterOptions.Mode.WHITELIST).withList(1,true,"")));
            distinct(entity,read(capture(context,output,"entity-empty-whitelist")),evidence,"entityWhitelist");
            install(context,off);
            require(different(entity,read(capture(context,output,"entity-restored")))<80,"OFF did not restore controlled entity framebuffer");
            install(context,entities.withSceneFilterOptions(entityRules.withMode(1,SceneFilterOptions.Mode.WHITELIST).withList(1,true,"")));
            context.runOnClient(c->{require(!SceneFilterClient.hiddenEntity(c.player),"filter hid own player");
                require(!SceneFilterClient.hiddenBlock(Blocks.STONE),"entity toggle enabled block filter");
                c.options.setCameraType(CameraType.THIRD_PERSON_BACK);});
            server.runCommand("gamemode creative @a");
            server.runCommand("item replace entity @a armor.chest with minecraft:diamond_chestplate");
            server.runCommand("item replace entity @a armor.legs with minecraft:diamond_leggings");
            server.runCommand("fill -2 -58 11 2 -53 15 minecraft:light[level=15]");
            context.waitTicks(10);
            require(cyan(read(capture(context,output,"self-third-person")))>=80,"empty entity whitelist suppressed own armored avatar");
            server.runCommand("item replace entity @a armor.chest with minecraft:air");
            server.runCommand("item replace entity @a armor.legs with minecraft:air");
            context.runOnClient(c->c.options.setCameraType(CameraType.FIRST_PERSON));
            server.runCommand("gamemode spectator @a");server.runCommand("kill @e[type=minecraft:armor_stand]");

            // Fluids keep their independent vanilla renderer even if their block ID is filtered.
            clear(world);server.runCommand("fill -1 -60 3 1 -58 5 minecraft:barrier");
            server.runCommand("setblock 0 -59 4 minecraft:lava");install(context,off);
            var fluid=read(capture(context,output,"fluid-off"));
            install(context,blocks.withSceneFilterOptions(SceneFilterOptions.defaults().withMode(0,SceneFilterOptions.Mode.BLACKLIST)
                    .withList(0,false,"minecraft:lava")));
            var fluidHidden=read(capture(context,output,"fluid-filtered"));
            require(orange(fluid)>=80 && orange(fluidHidden)>=80,"block filter suppressed independent fluid rendering");

            clear(world);server.runCommand("setblock 2 -59 6 minecraft:barrel");
            server.runCommand("summon minecraft:villager 0 -59 6 {NoAI:1b,Invulnerable:1b}");
            context.waitFor(c->c.level!=null && findVillager(c.level)!=null,600);
            context.runOnClient(c->findVillager(c.level).getBrain().setMemory(MemoryModuleType.JOB_SITE,
                    GlobalPos.of(c.level.dimension(),new BlockPos(2,-59,6))));
            var analyzer=off.withEnabled(CapabilityId.VILLAGER_JOB_SITE_LINKS,true)
                    .withAnalyzerOptions(original.analyzerOptions().with(4,0,16).with(4,1,8).with(4,3,5));
            install(context,analyzer);
            context.waitFor(c->AnalyzerClient.engine().markers().stream().anyMatch(m->m.kind()==4),600);
            capture(context,output,"job-site-visible");
            install(context,analyzer.withEnabled(CapabilityId.BLOCK_FILTER,true).withSceneFilterOptions(SceneFilterOptions.defaults()
                    .withMode(0,SceneFilterOptions.Mode.BLACKLIST).withList(0,false,"minecraft:barrel")));
            context.waitTicks(25);
            require(context.computeOnClient(c->AnalyzerClient.engine().markers().isEmpty()),"hidden job-site endpoint retained analyzer link");
            capture(context,output,"job-site-hidden");
            install(context,analyzer.withEnabled(CapabilityId.ENTITY_FILTER,true).withSceneFilterOptions(SceneFilterOptions.defaults()
                    .withMode(1,SceneFilterOptions.Mode.BLACKLIST).withList(1,false,"minecraft:villager")));
            context.waitTicks(25);
            require(context.computeOnClient(c->AnalyzerClient.engine().markers().isEmpty()),"hidden villager retained analyzer link");

            server.runCommand("setblock -3 -59 4 minecraft:ancient_debris");
            server.runCommand("fill -1 -60 3 1 -58 5 minecraft:barrier");
            server.runCommand("setblock 0 -59 4 minecraft:lava");
            server.runCommand("fill 2 -60 3 4 -60 5 minecraft:iron_block");
            server.runCommand("setblock 3 -59 4 minecraft:beacon");
            server.runCommand("setblock -6 -59 4 minecraft:lightning_rod");
            var options=original.analyzerOptions().with(0,0,16);
            for(int kind=1;kind<5;kind++) options=options.with(kind,0,8).with(kind,1,5).with(kind,3,5);
            server.runCommand("tp @a 0 -56 12 180 25");
            install(context,off);
            var allOff=read(capture(context,output,"all53-off"));
            var combined=all(original,true).withAnalyzerOptions(options).withSceneFilterOptions(SceneFilterOptions.defaults().withEntityPreset(0)
                    .withMode(0,SceneFilterOptions.Mode.BLACKLIST).withList(0,false,"minecraft:stone"));
            require(Long.bitCount(combined.enabledMask())==53,"all53 fixture incomplete");
            install(context,combined);
            combined(context,output,"all53-on",allOff);
            reload(context);combined(context,output,"all53-reloaded",allOff);
            require(context.computeOnClient(c->SceneFilterClient.hiddenBlock(Blocks.STONE) && !SceneFilterClient.hiddenEntity(c.player)),"reload lost scene rules");
            server.runCommand("execute in minecraft:the_nether run tp @a 0 80 0");
            context.waitFor(c->c.level!=null && c.level.dimension().equals(Level.NETHER),1200);
            context.waitTicks(2);
            require(context.computeOnClient(c->SceneFilterClient.readyForCurrentConfig()),"dimension did not rebuild filter snapshot");
            server.runCommand("execute in minecraft:overworld run tp @a 0 -56 12 180 25");
            context.waitFor(c->c.level!=null && c.level.dimension().equals(Level.OVERWORLD),1200);
            context.waitFor(c->findVillager(c.level)!=null,600);
            context.runOnClient(c->findVillager(c.level).getBrain().setMemory(MemoryModuleType.JOB_SITE,
                    GlobalPos.of(c.level.dimension(),new BlockPos(2,-59,6))));
            combined(context,output,"all53-dimension-return",allOff);
            install(context,off);
            require(context.computeOnClient(c->!SceneFilterClient.blockFilterActive() && SceneFilterClient.compiledLookupCount()==0
                    && !SceneFilterClient.hiddenBlock(Blocks.STONE)),"OFF retained compiled filter state");
            evidence.append("blockModes=true\nentityModes=true\nblockEntityExtraction=true\nselfPreserved=true\nphysicalStatePreserved=true\nfluidPreserved=true\nmodelGroupsPreserved=true\njobSiteHidden=true\nall53Enabled=true\nall53Reload=true\ndimensionCleanup=true\nfinalOffEmpty=true\n");
            Files.writeString(output.resolve("scene-filter-visual-manifest.txt"),evidence,StandardCharsets.UTF_8);
            System.out.println("BLOCKLENS_SCENE_VISUAL blocks=true blockEntities=true entities=true self=true fluids=true modelGroupRestore=true hiddenJobSites=true all53Reload=true dimensionCleanup=true");
        } catch(IOException e) { throw new AssertionError("scene filter framebuffer evidence failed",e); }
        finally {
            BlockLensRuntime.installConfig(original);
            context.runOnClient(c->{c.options.bobView().set(bob);c.options.setCameraType(camera);SceneFilterClient.clear();AnalyzerClient.clear();
                dev.blocklens.fabric.MinecraftTerrainInvalidator.invalidateAll(c);});
        }
    }
    private static void clear(TestSingleplayerContext world) {
        var server=world.getServer();server.runCommand("fill -11 -60 -7 11 -50 19 minecraft:air");
        server.runCommand("fill -10 -60 -6 10 -60 7 minecraft:black_concrete");
        server.runCommand("fill -10 -50 -6 10 -50 7 minecraft:light[level=15]");
    }
    private static Villager findVillager(net.minecraft.client.multiplayer.ClientLevel level) {
        for(var entity:level.entitiesForRendering()) if(entity instanceof Villager v && v.isAlive()) return v;
        return null;
    }
    private static BlockLensConfig all(BlockLensConfig config,boolean enabled) {
        for(var capability:CapabilityId.values()) config=config.withEnabled(capability,enabled);return config;
    }
    private static void install(ClientGameTestContext context,BlockLensConfig config) {
        context.runOnClient(c->BlockLensRuntime.installConfig(config));context.waitTicks(25);
        require(context.computeOnClient(c->SceneFilterClient.readyForCurrentConfig()),"filter snapshot did not catch up");
    }
    private static void reload(ClientGameTestContext context) {
        var future=context.computeOnClient(c->c.reloadResourcePacks());context.waitFor(c->future.isDone(),1200);
        future.join();context.waitFor(SettingsClientAccess::ready,1200);context.waitTicks(25);
    }
    private static Path capture(ClientGameTestContext context,Path output,String name) {
        // Fixture block updates leave transient break particles; clear them before controlled model comparisons.
        context.runOnClient(c->c.particleEngine.clearParticles());
        return context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(output).withSize(640,360).disableCounterPrefix());
    }
    private static BufferedImage read(Path path) throws IOException {
        var image=javax.imageio.ImageIO.read(path.toFile());require(image!=null && image.getWidth()==640 && image.getHeight()==360,"invalid scene framebuffer");return image;
    }
    private static void distinct(BufferedImage a,BufferedImage b,StringBuilder evidence,String key) {
        int count=different(a,b);
        evidence.append(key).append("DifferentPixels=").append(count).append('\n');
        require(count>=80,"filter did not change controlled target framebuffer: "+key+" count="+count);
    }
    private static int different(BufferedImage a,BufferedImage b) {
        int count=0;
        for(int y=70;y<300;y++) for(int x=80;x<560;x++) {
            int first=a.getRGB(x,y),second=b.getRGB(x,y);
            int delta=Math.abs((first>>16 & 255)-(second>>16 & 255))+Math.abs((first>>8 & 255)-(second>>8 & 255))+Math.abs((first & 255)-(second & 255));
            if(delta>=40) count++;
        }
        return count;
    }
    private static int cyan(BufferedImage image) {
        int count=0;
        for(int y=70;y<300;y++) for(int x=80;x<560;x++) {
            int value=image.getRGB(x,y),r=value>>16 & 255,g=value>>8 & 255,b=value & 255;
            if(g>=80 && b>=80 && Math.min(g,b)>=r*1.3) count++;
        }
        return count;
    }
    private static void combined(ClientGameTestContext context,Path output,String name,BufferedImage baseline) throws IOException {
        try {
            context.waitFor(c->AnalyzerClient.engine().markers().stream().map(m->m.kind()).distinct().count()==5,600);
        } catch(AssertionError failure) {
            context.runOnClient(c->{
                int kinds=0;
                for(var marker:AnalyzerClient.engine().markers()) kinds|=1<<marker.kind();
                System.out.println("BLOCKLENS_SCENE_COMBINED_FAILURE sample="+name+" kindMask="+kinds);
            });
            throw failure;
        }
        var frame=read(capture(context,output,name));
        for(int color:new int[]{0xFF8A24,0xFFDB45,0x62FF87,0x56DFFF,0xFF6DEA}) {
            int before=0,after=0;
            for(int y=40;y<320;y++) for(int x=30;x<610;x++) {
                if(dev.blocklens.testing.CueColorEvidence.matches(baseline.getRGB(x,y),color)) before++;
                if(dev.blocklens.testing.CueColorEvidence.matches(frame.getRGB(x,y),color)) after++;
            }
            require(after-before>=12,"all53 filter interaction lost an analyzer cue: "+name);
        }
    }
    static void prepareDisconnect(ClientGameTestContext context) {
        var options=SceneFilterOptions.defaults().withMode(0,SceneFilterOptions.Mode.BLACKLIST).withList(0,false,"minecraft:stone")
                .withMode(1,SceneFilterOptions.Mode.BLACKLIST).withList(1,false,"minecraft:armor_stand");
        install(context,BlockLensRuntime.config().withEnabled(CapabilityId.BLOCK_FILTER,true)
                .withEnabled(CapabilityId.ENTITY_FILTER,true).withSceneFilterOptions(options));
        require(context.computeOnClient(c->SceneFilterClient.compiledLookupCount()==2),"disconnect needs nonempty compiled filter state");
        context.waitFor(c->AnalyzerClient.engine().markers().stream().anyMatch(m->m.kind()==3),600);
    }
    private static int orange(BufferedImage image) {
        int count=0;
        for(int y=70;y<300;y++) for(int x=80;x<560;x++) {
            int value=image.getRGB(x,y),r=value>>16 & 255,g=value>>8 & 255,b=value & 255;
            if(r>=100 && g>=25 && r>=g*1.3 && g>=b*1.3) count++;
        }
        return count;
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
