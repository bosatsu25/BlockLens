package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.SimplifiedTerrainPolicy;
import dev.blocklens.core.scene.SceneFilterOptions;
import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.fabric.AbstractBlockLensConfigScreen;
import dev.blocklens.fabric.BlockLensConfigScreen;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Saved UI, real model emission, opaque/sparse rendering and external ownership on each target. */
final class SimplifiedTerrainOracle {
    private static final BlockPos TARGET=new BlockPos(0,-59,4);
    private static final String[] ROLES={"stone","oak_slab","oak_leaves","big_dripleaf","big_dripleaf_stem",
            "small_dripleaf","pointed_dripstone","vine","glow_lichen","moss_carpet","leaf_litter","short_grass","tall_grass","bush"};
    private static final String[] STATES={"stone","oak_slab[type=bottom]","oak_leaves[persistent=true]",
            "big_dripleaf[facing=east,tilt=partial]","big_dripleaf_stem[facing=west]",
            "small_dripleaf[facing=south,half=lower]","pointed_dripstone[vertical_direction=up,thickness=frustum]",
            "vine[north=true]","glow_lichen[north=true]","moss_carpet","leaf_litter[facing=east,segment_amount=3]",
            "short_grass","tall_grass[half=lower]","bush"};
    private SimplifiedTerrainOracle() { }
    static void verify(ClientGameTestContext context,TestSingleplayerContext world) {
        var original=BlockLensRuntime.config();
        var directory=FabricLoader.getInstance().getConfigDir();
        var output=FabricLoader.getInstance().getGameDir().resolve("simplified-visual");
        var packs=context.computeOnClient(c->List.copyOf(c.getResourcePackRepository().getSelectedIds()));
        byte[] originalFile;
        try { originalFile=Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME)); }
        catch(IOException failure) { throw new AssertionError("Simplified baseline storage unavailable",failure); }
        SimplifiedTerrainProbe.start();
        try {
            Files.createDirectories(output);
            Files.writeString(output.resolve("simplified-manifest.txt"),"status=started\nminecraft="+BlockLensRuntime.minecraftVersion()+"\n");
            var off=original;for(var id:CapabilityId.values()) off=off.withEnabled(id,false);
            var offConfig=off;
            context.runOnClient(c->{
                c.getResourcePackRepository().setSelected(packs.stream().filter(id->!id.endsWith("palette-external") && !id.endsWith("compact-external") && !id.endsWith("simplified-external")).toList());
                BlockLensRuntime.installConfig(offConfig);
                try { BlockLensConfigFiles.save(directory,offConfig); }
                catch(IOException failure) { throw new AssertionError("Simplified test save failed",failure); }
            });
            reload(context);
            var server=world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");server.runCommand("gamerule minecraft:advance_weather false");
            server.runCommand("time set noon");server.runCommand("weather clear");server.runCommand("gamemode spectator @a");
            server.runCommand("tick freeze");
            server.runCommand("fill -12 -59 -8 12 -45 20 minecraft:air");
            server.runCommand("fill -11 -60 -7 11 -60 19 minecraft:white_concrete");
            server.runCommand("setblock 0 -60 4 minecraft:mud");
            server.runCommand("fill -4 -59 3 4 -52 3 minecraft:white_concrete");
            server.runCommand("tp @a 0.5 -56.5 11.5 180 18");
            verifyDiscardAndFailure(context,directory);
            int changed=0;
            for(int role=0;role<ROLES.length;role++) {
                server.runCommand("fill 0 -59 4 0 -57 4 minecraft:air");
                server.runCommand("setblock 0 -59 4 minecraft:"+STATES[role]);
                if(ROLES[role].equals("small_dripleaf") || ROLES[role].equals("tall_grass")) {
                    boolean dripleaf=ROLES[role].equals("small_dripleaf");
                    server.runOnServer(srv->{
                        var lower=(dripleaf ? net.minecraft.world.level.block.Blocks.SMALL_DRIPLEAF : net.minecraft.world.level.block.Blocks.TALL_GRASS).defaultBlockState()
                                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                                        net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
                        if(dripleaf) lower=lower.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,net.minecraft.core.Direction.SOUTH);
                        int flags=net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
                        srv.overworld().setBlock(TARGET,lower,flags);
                        srv.overworld().setBlock(TARGET.above(),lower.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),flags);
                    });
                }
                context.waitTicks(20);
                var state=context.computeOnClient(c->c.level.getBlockState(TARGET));
                require(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().equals(ROLES[role]),
                        "Simplified fixture state absent: "+ROLES[role]);
                var baseline=capture(context,output,ROLES[role]+"-off");
                saveUiToggle(context);
                verifyTextures(context);
                var enabled=capture(context,output,ROLES[role]+"-on");
                int pixels=difference(baseline,enabled);require(pixels>20,"Simplification invisible: "+ROLES[role]);changed+=pixels;
                require(SimplifiedTerrainProbe.seen(ROLES[role],SimplifiedTerrainPolicy.kind("minecraft",ROLES[role])),
                        "Actual model emission not observed: "+ROLES[role]);
                context.runOnClient(c->require(c.level.getBlockState(TARGET)==state,"Simplification changed world state"));
                var upper=context.computeOnClient(c->c.level.getBlockState(TARGET.above()));
                server.runOnServer(srv->{
                    int flags=net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
                    srv.overworld().setBlock(TARGET,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),flags);
                    srv.overworld().setBlock(TARGET.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),flags);
                });
                var empty=capture(context,output,ROLES[role]+"-air");
                int foreground=difference(enabled,empty);
                require(foreground>20,"Simplification removed visible terrain: "+ROLES[role]);
                if(role<3) require(foreground*10>=difference(baseline,empty)*9,"Opaque cube lost original silhouette: "+ROLES[role]);
                server.runOnServer(srv->{
                    int flags=net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
                    srv.overworld().setBlock(TARGET,state,flags);srv.overworld().setBlock(TARGET.above(),upper,flags);
                });
                saveUiToggle(context);
                var restored=capture(context,output,ROLES[role]+"-restored");
                require(difference(enabled,restored)>20,"Simplification did not visibly restore: "+ROLES[role]);
                // Same frozen state and fixed backdrop as OFF; use the unchanged-reload bound.
                require(difference(baseline,restored)<200,"Restored terrain differs from OFF baseline: "+ROLES[role]);
                require(difference(restored,empty)>20,"Restoration removed visible terrain: "+ROLES[role]);
                context.runOnClient(c->require(c.level.getBlockState(TARGET)==state,"Restoration changed world state"));
            }
            server.runCommand("fill 0 -59 4 0 -57 4 minecraft:air");server.runCommand("setblock 0 -59 4 minecraft:stone");
            saveUiToggle(context);var before=capture(context,output,"simplified-reload-before");reload(context);verifyTextures(context);
            var after=capture(context,output,"simplified-reloaded");require(difference(before,after)<200,"Unchanged terrain reload drifted");
            // Hidden solids retain physical occlusion; fixed adjacent light isolates mesh suppression.
            for(var direction:net.minecraft.core.Direction.values()) {
                var light=TARGET.relative(direction);
                server.runCommand("setblock "+light.getX()+" "+light.getY()+" "+light.getZ()+" minecraft:light[level=15]");
            }
            SimplifiedTerrainProbe.start();
            install(context,offConfig.withEnabled(CapabilityId.SIMPLIFIED_TERRAIN,true));
            var visible=capture(context,output,"simplified-filter-visible");
            require(SimplifiedTerrainProbe.seen("stone",SimplifiedTerrainPolicy.Kind.CUBE),"Filter positive control emitted no stone");
            var physical=context.computeOnClient(c->c.level.getBlockState(TARGET));
            SimplifiedTerrainProbe.start();
            var rules=SceneFilterOptions.defaults().withMode(0,SceneFilterOptions.Mode.BLACKLIST).withList(0,false,"minecraft:stone");
            install(context,offConfig.withEnabled(CapabilityId.SIMPLIFIED_TERRAIN,true).withEnabled(CapabilityId.BLOCK_FILTER,true).withSceneFilterOptions(rules));
            var hidden=capture(context,output,"simplified-filtered");
            require(context.computeOnClient(c->dev.blocklens.fabric.SceneFilterClient.hiddenBlock(net.minecraft.world.level.block.Blocks.STONE)),"Hide snapshot not published");
            require(!SimplifiedTerrainProbe.seen("stone",SimplifiedTerrainPolicy.Kind.CUBE),"Hidden simplified model still emitted");
            context.runOnClient(c->require(c.level.getBlockState(TARGET)==physical,"Hide composition changed physical state"));
            require(filterInteriorDifference(visible,hidden)>80,"Hide composition did not remove the rendered stone interior");
            server.runCommand("setblock 0 -59 4 minecraft:air");
            var air=capture(context,output,"simplified-air");require(filterInteriorDifference(hidden,air)<100,"Hide filter lost precedence over simplification");
            server.runCommand("setblock 0 -59 4 minecraft:oak_slab[type=bottom]");
            install(context,offConfig.withEnabled(CapabilityId.SIMPLIFIED_TERRAIN,true));
            var cueBefore=capture(context,output,"simplified-cue-before");
            install(context,offConfig.withEnabled(CapabilityId.SIMPLIFIED_TERRAIN,true).withEnabled(CapabilityId.SLABS,true));
            var cueOn=capture(context,output,"simplified-cue-on");
            require(cuePixels(cueBefore,cueOn)>=12,"Independent slab state cue lost over simplified geometry");
            var all=offConfig;for(var flag:CapabilityId.values()) all=all.withEnabled(flag,true);
            install(context,all);reload(context);var combined=capture(context,output,"simplified-all-features");
            require(cuePixels(cueBefore,combined)>=12,"All-enabled composition lost the slab state cue");
            String external=context.computeOnClient(c->{c.getResourcePackRepository().reload();return c.getResourcePackRepository().getAvailableIds()
                    .stream().filter(id->id.endsWith("simplified-external")).findFirst().orElseThrow();});
            context.runOnClient(c->{var selected=new ArrayList<>(c.getResourcePackRepository().getSelectedIds());selected.add(external);c.getResourcePackRepository().setSelected(selected);});
            String[] externalRoles={"stone","oak_slab","oak_leaves"};
            String[] externalNames={"simplified-external","simplified-external-model","simplified-external-texture"};
            for(int role=0;role<externalRoles.length;role++) {
                server.runCommand("setblock 0 -59 4 minecraft:"+externalRoles[role]);
                install(context,offConfig);reload(context);
                var externalOff=capture(context,output,externalNames[role]+"-off");
                SimplifiedTerrainProbe.start();saveUiToggle(context);
                var externalOn=capture(context,output,externalNames[role]+"-on");
                require(!SimplifiedTerrainProbe.seen(externalRoles[role],SimplifiedTerrainPolicy.Kind.CUBE),"Externally owned dependency was wrapped");
                require(difference(externalOff,externalOn)<100,"External resource priority overridden: "+externalRoles[role]);
            }
            Files.writeString(output.resolve("simplified-manifest.txt"),"minecraft="+BlockLensRuntime.minecraftVersion()+"\nroles="+ROLES.length
                    +"\nchangedPixels="+changed+"\nactualEmission=true\nopaque=true\nsparse=true\nstatesPreserved=true\nuiSave=true\nuiDiscard=true\nfailedSave=true"
                    +"\nreload=true\noffRestored=true\nfilterComposition=true\ncueComposition=true\nallFeatures=true\nexternalPriority=true\n");
            System.out.println("BLOCKLENS_SIMPLIFIED roles="+ROLES.length+" changed="+changed+" actualEmission=true states=true ui=true reload=true external=true");
        } catch(IOException failure) { throw new AssertionError("Simplified evidence failed",failure); }
        finally {
            world.getServer().runCommand("tick unfreeze");
            context.runOnClient(c->{BlockLensConfigScreen.show(c,null);c.getResourcePackRepository().setSelected(packs);BlockLensRuntime.installConfig(original);});
            try { Files.write(directory.resolve(BlockLensConfigFiles.FILE_NAME),originalFile); }
            catch(IOException failure) { throw new AssertionError("Simplified baseline restore failed",failure); }
            reload(context);SimplifiedTerrainProbe.stop();
        }
    }
    private static void verifyTextures(ClientGameTestContext context) {
        context.runOnClient(c->{
            var kinds=new SimplifiedTerrainPolicy.Kind[]{SimplifiedTerrainPolicy.Kind.CUBE,SimplifiedTerrainPolicy.Kind.SHORT_GRASS,
                    SimplifiedTerrainPolicy.Kind.TALL_GRASS,SimplifiedTerrainPolicy.Kind.BUSH};
            var names=new String[]{"grain","short_grass","tall_grass","bush"};
            for(int i=0;i<names.length;i++) try(var image=TextureContents.load(c.getResourceManager(),Identifier.fromNamespaceAndPath("blocklens","textures/block/simplified/"+names[i]+".png"))) {
                require(image.image().getWidth()==16 && image.image().getHeight()==16,"Procedural texture dimensions changed");
                for(int y=0;y<16;y++) for(int x=0;x<16;x++) require(image.image().getPixel(x,y)==SimplifiedTerrainPolicy.pixel(kinds[i],x,y),"Procedural opaque/sparse pixel diverged");
            } catch(IOException failure) { throw new AssertionError("Procedural texture unavailable",failure); }
        });
    }
    private static void install(ClientGameTestContext context,BlockLensConfig config) {
        context.runOnClient(c->{BlockLensRuntime.installConfig(config);MinecraftTerrainInvalidator.invalidateAll(c);});context.waitTicks(25);
    }
    private static void reload(ClientGameTestContext context) {
        var future=context.computeOnClient(c->c.reloadResourcePacks());context.waitFor(c->future.isDone(),1200);future.join();
        context.waitFor(SettingsClientAccess::ready,1200);context.waitTicks(15);
    }
    private static void saveUiToggle(ClientGameTestContext context) {
        var before=context.computeOnClient(c->c.getModelManager().getBlockStateModelSet());
        context.runOnClient(c->{var screen=new BlockLensConfigScreen(null);BlockLensConfigScreen.show(c,screen);toggle(screen);press(screen,"blocklens.settings.apply");});
        context.waitFor(c->SettingsClientAccess.ready(c) && c.getModelManager().getBlockStateModelSet()!=before,1200);context.waitTicks(20);
    }
    private static void toggle(Screen screen) {
        press(screen,"blocklens.category.other");int index=SettingsCatalog.capabilities(CapabilityId.Category.OTHER).indexOf(CapabilityId.SIMPLIFIED_TERRAIN);
        var button=ResponsiveSettingsScreenOracle.buttons(screen).get(4+index);int remaining=ResponsiveSettingsScreenOracle.buttons(screen).size()*2;
        while(screen.getFocused()!=button && remaining-->0) screen.keyPressed(new KeyEvent(InputConstants.KEY_TAB,0,0));
        require(screen.getFocused()==button && button.visible,"Simplified row not keyboard reachable");screen.keyPressed(new KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    private static void press(Screen screen,String key) {
        String label=Component.translatable(key).getString();ResponsiveSettingsScreenOracle.buttons(screen).stream().filter(b->b.getMessage().getString().equals(label))
                .findFirst().orElseThrow().onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    private static void verifyDiscardAndFailure(ClientGameTestContext context,Path directory) throws IOException {
        byte[] before=Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME));
        context.runOnClient(c->{var screen=new BlockLensConfigScreen(null);BlockLensConfigScreen.show(c,screen);toggle(screen);screen.onClose();});
        require(Arrays.equals(before,Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME))) && !BlockLensRuntime.config().isEnabled(CapabilityId.SIMPLIFIED_TERRAIN),"Discard applied simplified config");
        Path fixture=Files.createTempDirectory(directory,"simplified-failure-");Path occupied=fixture.resolve("occupied");Files.writeString(occupied,"keep");
        try {
            context.runOnClient(c->{var screen=new FailingScreen(occupied);BlockLensConfigScreen.show(c,screen);toggle(screen);press(screen,"blocklens.settings.apply");
                require(BlockLensConfigScreen.current(c)==screen && !BlockLensRuntime.config().isEnabled(CapabilityId.SIMPLIFIED_TERRAIN),"Failed simplified save applied or discarded draft");});
            require(Files.readString(occupied).equals("keep"),"Failed save overwrote a file");
        } finally { context.runOnClient(c->BlockLensConfigScreen.show(c,null));Files.deleteIfExists(occupied);Files.deleteIfExists(fixture); }
    }
    private static final class FailingScreen extends AbstractBlockLensConfigScreen {
        FailingScreen(Path directory) { super(null,directory); }
        @Override protected void showParent(Screen parent) { BlockLensConfigScreen.show(minecraft,parent); }
    }
    private static BufferedImage capture(ClientGameTestContext context,Path output,String name) throws IOException {
        context.waitTicks(15);context.runOnClient(c->c.particleEngine.clearParticles());
        var path=ExternalFrameCapture.required() ? ExternalFrameCapture.capture(context,output,name,640,360)
                : context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(output).withSize(640,360).disableCounterPrefix());
        var image=ImageIO.read(path.toFile());require(image!=null && image.getWidth()==640 && image.getHeight()==360,"Simplified framebuffer missing");return image;
    }
    /** Paired chroma gain rejects global gray brightness changes and requires the red slab marker. */
    private static int cuePixels(BufferedImage before,BufferedImage after) {
        int changed=0;
        for(int y=198;y<212;y++) for(int x=308;x<328;x++) {
            int p=before.getRGB(x,y),q=after.getRGB(x,y);
            int old=2*(p>>>16&255)-(p>>>8&255)-(p&255);
            int marked=2*(q>>>16&255)-(q>>>8&255)-(q&255);
            if(marked-old>=12) changed++;
        }
        return changed;
    }
    /** Upper mesh interior excludes side/floor AO which retained physical solids intentionally affect. */
    private static int filterInteriorDifference(BufferedImage a,BufferedImage b) {
        return difference(a,b,308,198,328,210);
    }
    private static int difference(BufferedImage a,BufferedImage b) { return difference(a,b,280,140,360,270); }
    private static int difference(BufferedImage a,BufferedImage b,int x0,int y0,int x1,int y1) {
        int changed=0;for(int y=y0;y<y1;y++) for(int x=x0;x<x1;x++) {
            int p=a.getRGB(x,y),q=b.getRGB(x,y);if(Math.abs((p>>>16&255)-(q>>>16&255))+Math.abs((p>>>8&255)-(q>>>8&255))+Math.abs((p&255)-(q&255))>=24) changed++;
        }return changed;
    }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}