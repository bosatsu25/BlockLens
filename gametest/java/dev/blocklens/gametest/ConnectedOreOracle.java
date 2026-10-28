package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.ResourceBorderPolicy;
import dev.blocklens.core.scene.SceneFilterOptions;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

/** Paired real frames, actual neighbor masks and final atlas UVs from the distributed runtime. */
final class ConnectedOreOracle {
    private static final int[] DX={0,1,0,-1,1,1,-1,-1},DY={1,0,-1,0,1,-1,-1,1};
    private ConnectedOreOracle() { }
    static void verify(ClientGameTestContext context,TestSingleplayerContext world) {
        var original=BlockLensRuntime.config();
        Path output=FabricLoader.getInstance().getGameDir().resolve("connected-visual");
        var originalPacks=context.computeOnClient(c->java.util.List.copyOf(c.getResourcePackRepository().getSelectedIds()));
        var configDirectory=FabricLoader.getInstance().getConfigDir();
        byte[] originalFile;
        try {originalFile=Files.readAllBytes(configDirectory.resolve(dev.blocklens.core.BlockLensConfigFiles.FILE_NAME));}
        catch(IOException failure){throw new AssertionError("Connected baseline storage unavailable",failure);}
        try {
            Files.createDirectories(output);
            Files.writeString(output.resolve("connected-manifest.txt"),"status=started\nminecraft="+BlockLensRuntime.minecraftVersion()+"\n");
            var server=world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:advance_weather false");
            server.runCommand("weather clear");server.runCommand("time set noon");
            server.runCommand("gamemode spectator @a");server.runCommand("tick freeze");
            server.runCommand("fill -12 -60 -8 20 -45 20 minecraft:air");
            server.runCommand("fill -11 -61 -7 20 -61 19 minecraft:black_concrete");
            server.runCommand("tp @a 0.5 -57.8 10 180 22");
            context.waitTicks(25);
            var off=original.withSceneFilterOptions(SceneFilterOptions.defaults());
            for(var id:CapabilityId.values())off=off.withEnabled(id,false);
            var diamond=off.withEnabled(CapabilityId.DIAMOND_ORE,true);
            var on=diamond.withEnabled(CapabilityId.CONNECTED_ORE_EDGES,true);
            ConnectedOreSettingsOracle.verify(context,output,diamond);
            // M5 intentionally overrides two vanilla ores with non-cube geometry. Use a separate
            // cube control for vanilla topology, and restore that active external model below.
            context.runOnClient(c->{c.getResourcePackRepository().reload();String id=c.getResourcePackRepository().getAvailableIds()
                    .stream().filter(name->name.endsWith("connected-vanilla-control")).findFirst().orElseThrow();
                var selected=new java.util.ArrayList<>(originalPacks);selected.add(id);c.getResourcePackRepository().setSelected(selected);});
            reload(context);
            int roles=0;
            for(int shape=0;shape<=5;shape++) {
                final int value=shape;
                scenario(context,world,output,"geometry-"+shape,0,OreExtensionFixture.GEOMETRY+"[shape=0]",
                        OreExtensionFixture.GEOMETRY+"[shape="+shape+"]",2,diamond,
                        shape==0||shape==5 ? 2 : 0,true);
                require(shape==0||shape==5 ? ConnectedOreProbe.onlyAt(1,8)&&ConnectedOreProbe.cellAt(1,8)
                        : ConnectedOreProbe.absentAt(1),"Asymmetric neighbor geometry: "+value);
                roles++;
            }
            for(int shape=1;shape<=4;shape++) {
                scenario(context,world,output,"geometry-reverse-"+shape,0,OreExtensionFixture.GEOMETRY+"[shape="+shape+"]",
                        OreExtensionFixture.GEOMETRY+"[shape=0]",2,diamond,-1,false);
                require(ConnectedOreProbe.onlyAt(1,0)&&ConnectedOreProbe.cellAt(1,0),"Reverse geometry linked incompatible face");roles++;
            }
            String[] names={"isolated","pair","square","l","diagonal","concave","ring"};
            int[] masks={0,2,19,3,16,15,255};
            for(int i=0;i<masks.length;i++) {
                scenario(context,world,output,"topology-"+names[i],0,"minecraft:diamond_ore","minecraft:diamond_ore",masks[i],diamond,masks[i],true);roles++;
            }
            scenario(context,world,output,"covered-neighbor",0,"minecraft:diamond_ore","minecraft:diamond_ore",2,diamond,0,true);roles++;
            int variants=0;
            for(var ore:CapabilityId.values())if(ResourceBorderPolicy.supports(ore)) {
                String id="minecraft:"+ore.sourceKey().substring("gaming.".length());
                scenario(context,world,output,"vanilla-"+ore.name().toLowerCase(java.util.Locale.ROOT),0,id,id,2,off.withEnabled(ore,true),2,true);
                require(ConnectedOreProbe.onlyAt(1,8)&&ConnectedOreProbe.cellAt(1,8),"Vanilla ore opposite seam not joined: "+ore);
                roles++;variants++;
            }
            require(variants==18,"Connected vanilla inventory drifted");
            for(var ore:new CapabilityId[]{CapabilityId.REDSTONE_ORE,CapabilityId.DEEPSLATE_REDSTONE_ORE}) {
                String id="minecraft:"+ore.sourceKey().substring("gaming.".length());
                scenario(context,world,output,"lit-"+ore.name().toLowerCase(java.util.Locale.ROOT),0,id+"[lit=false]",id+"[lit=true]",2,
                        off.withEnabled(ore,true),2,true);roles++;
            }
            scenario(context,world,output,"chunk-boundary",15,"minecraft:diamond_ore","minecraft:diamond_ore",2,diamond,2,true);roles++;
            updates(context,world,output,on,15,"chunk-neighbor");
            server.runCommand("tp @a 0.5 -57.8 10 180 22");context.waitTicks(25);
            place(world,0,"minecraft:diamond_ore","minecraft:diamond_ore",2);
            install(context,on,0);capture(context,output,"neighbor-before");
            context.runOnClient(c->ConnectedOreProbe.reset());server.runCommand("setblock 1 -59 4 minecraft:air");
            context.waitFor(c->ConnectedOreProbe.only(0)&&ConnectedOreProbe.cellAt(0,0),1200);
            capture(context,output,"neighbor-removed");
            context.runOnClient(c->ConnectedOreProbe.reset());server.runCommand("setblock 1 -59 4 minecraft:diamond_ore");
            context.waitFor(c->ConnectedOreProbe.only(2)&&ConnectedOreProbe.cellAt(0,2),1200);
            var before=capture(context,output,"neighbor-added");
            dev.blocklens.core.BlockLensConfigFiles.save(configDirectory,on);
            var reload=context.computeOnClient(c->c.reloadResourcePacks());context.waitFor(c->reload.isDone(),1200);reload.join();
            context.waitFor(SettingsClientAccess::ready,1200);
            require(BlockLensRuntime.config().isEnabled(CapabilityId.CONNECTED_ORE_EDGES),"Reload lost saved connection");
            install(context,on,0);
            var after=capture(context,output,"connected-reloaded");require(stable(before,after),"Reload changed connected border");
            install(context,off,0);var plain=capture(context,output,"independent-off");
            install(context,off.withEnabled(CapabilityId.CONNECTED_ORE_EDGES,true),0);
            require(stable(plain,capture(context,output,"independent-connection-only")),"Connection alone emitted an ore cue");
            install(context,on,0);var visible=capture(context,output,"filter-visible");
            var physical=context.computeOnClient(c->c.level.getBlockState(new BlockPos(0,-59,4)));
            var hidden=on.withEnabled(CapabilityId.BLOCK_FILTER,true).withSceneFilterOptions(SceneFilterOptions.defaults()
                    .withList(0,false,"minecraft:diamond_ore").withMode(0,SceneFilterOptions.Mode.BLACKLIST));
            install(context,hidden,0);var removed=capture(context,output,"filter-hidden");
            require(ConnectedOreProbe.absentAt(0)&&difference(visible,removed)>80,"Filter did not suppress connected ore");
            context.runOnClient(c->require(c.level.getBlockState(new BlockPos(0,-59,4))==physical,"Filter changed physical ore state"));
            var all=off;for(var id:CapabilityId.values())all=all.withEnabled(id,true);
            persistAndReload(context,configDirectory,all);
            require(BlockLensRuntime.config().sameCapabilities(all),"All-feature reload did not apply complete configuration");
            clear(world,0);install(context,all,0);var allAir=capture(context,output,"all-features-air");
            place(world,0,"minecraft:diamond_ore","minecraft:diamond_ore",2);
            var allLegacy=all.withEnabled(CapabilityId.CONNECTED_ORE_EDGES,false);
            persistAndReload(context,configDirectory,allLegacy);install(context,allLegacy,0);
            var allOff=capture(context,output,"all-features-off");
            persistAndReload(context,configDirectory,all);install(context,all,0);
            var allOn=capture(context,output,"all-features");
            require(difference(allOff,allAir)>80 && difference(allOn,allAir)>80 && difference(allOff,allOn)>20,
                    "All-enabled rendered connection/foreground absent");
            require(ConnectedOreProbe.only(2)&&ConnectedOreProbe.cellAt(0,2),"All-enabled composition lost connection");
            context.runOnClient(c->c.getResourcePackRepository().setSelected(originalPacks));reload(context);
            scenario(context,world,output,"external-model",0,OreExtensionFixture.EXPLICIT,OreExtensionFixture.EXPLICIT,2,diamond,-1,false);roles++;
            Files.writeString(output.resolve("connected-manifest.txt"),"minecraft="+BlockLensRuntime.minecraftVersion()+"\nroles="+roles
                    +"\nvanillaVariants="+variants+"\nactualEmission=true\nactualUv=true\ngeometryFallback=true\ncollisionSymmetry=true\n"
                    +"topology=true\ncoveredNeighbor=true\nlitStates=true\nchunkBoundary=true\nneighborUpdates=true\nindependent=true\nreload=true\noffRestored=true\n"
                    +"uiSave=true\nuiDiscard=true\nfailedSave=true\nfilterComposition=true\nallFeatures=true\nexternalModel=true\n");
            System.out.println("BLOCKLENS_CONNECTED roles="+roles+" vanilla="+variants+" actualUv=true symmetry=true ui=true");
        }catch(IOException failure){throw new AssertionError("Connected evidence failed",failure);}
        finally {
            ConnectedOreProbe.stop();world.getServer().runCommand("tick unfreeze");
            context.runOnClient(c->{c.getResourcePackRepository().setSelected(originalPacks);BlockLensRuntime.installConfig(original);MinecraftTerrainInvalidator.invalidateAll(c);});
            try {Files.write(configDirectory.resolve(dev.blocklens.core.BlockLensConfigFiles.FILE_NAME),originalFile);}
            catch(IOException failure){throw new AssertionError("Connected baseline restore failed",failure);}
        }
    }
    private static void scenario(ClientGameTestContext context,TestSingleplayerContext world,Path output,String name,int anchor,
            String target,String neighbor,int neighbors,BlockLensConfig material,int expected,boolean observe) throws IOException {
        if(anchor==15){world.getServer().runCommand("tp @a 15.5 -57.8 10 180 22");context.waitTicks(25);}
        clear(world,anchor);install(context,material,anchor);var empty=capture(context,output,name+"-air");
        place(world,anchor,target,neighbor,neighbors);
        if(name.equals("covered-neighbor"))world.getServer().runCommand("setblock "+(anchor+1)+" -59 5 minecraft:stone");
        install(context,material,anchor);var legacy=capture(context,output,name+"-off");
        var physical=context.computeOnClient(c->c.level.getBlockState(new BlockPos(anchor,-59,4)));
        install(context,material.withEnabled(CapabilityId.CONNECTED_ORE_EDGES,true),anchor);var connected=capture(context,output,name+"-on");
        if(observe)require(ConnectedOreProbe.only(expected)&&ConnectedOreProbe.cellAt(0,expected),"Connected actual mask/UV drift: "+name+" "+ConnectedOreProbe.observed());
        else require(ConnectedOreProbe.absentAt(0),"Unsupported base geometry was connected: "+name);
        require(difference(legacy,empty)>80&&difference(connected,empty)>80,"Missing connected foreground: "+name);
        if(observe && ResourceBorderPolicy.connectedBorderMask(expected)!=255)require(difference(legacy,connected)>20,"Connected seam did not visibly change: "+name);
        else require(stable(legacy,connected),"Fallback or isolated border changed: "+name);
        context.runOnClient(c->require(c.level.getBlockState(new BlockPos(anchor,-59,4))==physical,"Connection changed physical state: "+name));
        // Preserve ON observations while capturing the paired OFF restoration.
        context.runOnClient(c->{BlockLensRuntime.installConfig(material);MinecraftTerrainInvalidator.invalidateAll(c);});context.waitTicks(20);
        require(stable(legacy,capture(context,output,name+"-restored")),"OFF did not restore border: "+name);
    }
    private static void updates(ClientGameTestContext context,TestSingleplayerContext world,Path output,BlockLensConfig config,int anchor,String prefix) throws IOException {
        place(world,anchor,"minecraft:diamond_ore","minecraft:diamond_ore",2);install(context,config,anchor);
        capture(context,output,prefix+"-before");
        context.runOnClient(c->ConnectedOreProbe.reset(anchor));world.getServer().runCommand("setblock "+(anchor+1)+" -59 4 minecraft:air");
        context.waitFor(c->ConnectedOreProbe.only(0)&&ConnectedOreProbe.cellAt(0,0),1200);capture(context,output,prefix+"-removed");
        context.runOnClient(c->ConnectedOreProbe.reset(anchor));world.getServer().runCommand("setblock "+(anchor+1)+" -59 4 minecraft:diamond_ore");
        context.waitFor(c->ConnectedOreProbe.only(2)&&ConnectedOreProbe.cellAt(0,2),1200);capture(context,output,prefix+"-added");
    }
    private static void persistAndReload(ClientGameTestContext context,Path directory,BlockLensConfig config) throws IOException {
        dev.blocklens.core.BlockLensConfigFiles.save(directory,config);
        var prior=context.computeOnClient(c->{BlockLensRuntime.installConfig(config);return c.getModelManager().getBlockStateModelSet();});
        reload(context);
        require(BlockLensRuntime.config().equals(config) && dev.blocklens.core.BlockLensConfigFiles.loadOrCreate(directory).equals(config),
                "Saved and active all-feature state differ");
        context.runOnClient(c->require(c.getModelManager().getBlockStateModelSet()!=prior,"All-feature models were not rebuilt"));
    }
    private static void reload(ClientGameTestContext context){
        var future=context.computeOnClient(c->c.reloadResourcePacks());context.waitFor(c->future.isDone(),1200);future.join();
        context.waitFor(SettingsClientAccess::ready,1200);context.waitTicks(20);
    }
    private static void clear(TestSingleplayerContext world,int anchor){world.getServer().runCommand("fill "+(anchor-1)+" -60 4 "+(anchor+2)+" -58 5 minecraft:air");}
    private static void place(TestSingleplayerContext world,int anchor,String target,String neighbor,int neighbors){
        clear(world,anchor);world.getServer().runCommand("setblock "+anchor+" -59 4 "+target);
        for(int bit=0;bit<8;bit++)if((neighbors&(1<<bit))!=0)world.getServer().runCommand("setblock "+(anchor+DX[bit])+" "+(-59+DY[bit])+" 4 "+neighbor);
    }
    private static void install(ClientGameTestContext context,BlockLensConfig config,int anchor){
        context.runOnClient(c->{ConnectedOreProbe.reset(anchor);BlockLensRuntime.installConfig(config);MinecraftTerrainInvalidator.invalidateAll(c);});context.waitTicks(20);
    }
    static BufferedImage capture(ClientGameTestContext context,Path output,String name) throws IOException {
        context.waitTicks(10);context.runOnClient(c->c.particleEngine.clearParticles());
        var path=(ExternalFrameCapture.required() || "shader-on".equals(System.getProperty("blocklens.test.external.profile"))) ? ExternalFrameCapture.capture(context,output,name,640,360)
                : context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(output).withSize(640,360).disableCounterPrefix());
        var image=ImageIO.read(path.toFile());require(image!=null&&image.getWidth()==640&&image.getHeight()==360,"Connected frame unavailable");return image;
    }
    private static int difference(BufferedImage a,BufferedImage b){
        int changed=0;for(int y=90;y<310;y++)for(int x=200;x<480;x++) {
            int p=a.getRGB(x,y),q=b.getRGB(x,y);
            if(Math.abs((p>>>16&255)-(q>>>16&255))+Math.abs((p>>>8&255)-(q>>>8&255))+Math.abs((p&255)-(q&255))>=24)changed++;
        }return changed;
    }
    private static boolean stable(BufferedImage a,BufferedImage b){return dev.blocklens.testing.SpatialFrameDifference.matches(a,b,200,90,480,310,20);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}