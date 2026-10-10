package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.analyzer.AnalyzerCatalog;
import dev.blocklens.core.analyzer.AnalyzerEngine;
import dev.blocklens.fabric.mixin.ClientEntityStorageAccess;
import dev.blocklens.fabric.mixin.EntitySectionStorageAccess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Loaded client inspection occurs on ticks; rendering consumes only immutable marker values. */
public final class AnalyzerClient {
    private static final AnalyzerEngine ENGINE=new AnalyzerEngine();
    private static final int[] COLORS={0xFFFF8A24,0xFFFFDB45,0xFF62FF87,0xFF56DFFF,0xFFFF6DEA};
    private AnalyzerClient() { }
    public static AnalyzerEngine engine() { return ENGINE; }
    public static void clear() { ENGINE.clear(); }
    public static void register() { ClientTickEvents.END_CLIENT_TICK.register(AnalyzerClient::tick); }
    private static void tick(Minecraft client) {
        var config=BlockLensRuntime.config();
        int mask=AnalyzerCatalog.mask(config);
        if(mask==0 || client.level==null || client.player==null) { ENGINE.clear(); return; }
        Vec3 eye=client.player.getEyePosition();
        ENGINE.tick(client.level,new LoadedView(client.level),point(eye),mask,config.analyzerOptions());
    }
    public static void draw(LevelRenderer renderer) {
        var config=BlockLensRuntime.config();
        int mask=AnalyzerCatalog.mask(config);
        List<AnalyzerEngine.Marker> markers=ENGINE.markers(mask,config.analyzerOptions());
        if(mask==0 || markers.isEmpty() || !SceneFilterClient.readyForCurrentConfig()) return;
        try(var collection=AnalyzerGizmos.collect(renderer)) {
            for(var marker:markers) {
                if((mask&(1<<marker.kind()))==0) continue;
                int color=COLORS[marker.kind()];
                if(marker.kind()==4) {
                    Gizmos.line(vec(marker.from()),vec(marker.to()),color,2).setAlwaysOnTop();
                    continue;
                }
                var p=marker.from();
                Gizmos.cuboid(new AABB(p.x()-.502,p.y()-.502,p.z()-.502,
                        p.x()+.502,p.y()+.502,p.z()+.502),GizmoStyle.stroke(color,2)).setAlwaysOnTop();
                if(marker.radius()>0) {
                    double r=marker.radius(),y=p.y()+.55;
                    Vec3[] corners={new Vec3(p.x()-r,y,p.z()-r),new Vec3(p.x()+r,y,p.z()-r),
                            new Vec3(p.x()+r,y,p.z()+r),new Vec3(p.x()-r,y,p.z()+r)};
                    for(int i=0;i<4;i++) Gizmos.line(corners[i],corners[(i+1)%4],color,2).setAlwaysOnTop();
                }
            }
        }
    }
    private static AnalyzerEngine.Point point(Vec3 value) { return new AnalyzerEngine.Point(value.x,value.y,value.z); }
    private static Vec3 vec(AnalyzerEngine.Point value) { return new Vec3(value.x(),value.y(),value.z()); }

    private static final class LoadedView implements AnalyzerEngine.WorldView {
        private final ClientLevel level;
        private final BlockPos.MutableBlockPos position=new BlockPos.MutableBlockPos();
        LoadedView(ClientLevel level) { this.level=level; }
        @Override public int cell(int x,int y,int z) {
            if(y<level.getMinY() || y>=level.getMaxY()) return AnalyzerEngine.UNKNOWN;
            var chunk=level.getChunkSource().getChunk(Math.floorDiv(x,16),Math.floorDiv(z,16),ChunkStatus.FULL,false);
            if(chunk==null) return AnalyzerEngine.UNKNOWN;
            var state=chunk.getBlockState(position.set(x,y,z));
            int value;
            var fluid=state.getFluidState();
            if(state.is(Blocks.ANCIENT_DEBRIS)) value=AnalyzerEngine.DEBRIS;
            else if(state.is(Blocks.BEACON)) value=AnalyzerEngine.BEACON;
            else if(state.getBlock() instanceof net.minecraft.world.level.block.LightningRodBlock) value=AnalyzerEngine.ROD;
            else if(fluid.is(FluidTags.LAVA) && fluid.isSource()) value=AnalyzerEngine.LAVA;
            else value=state.is(BlockTags.BEACON_BASE_BLOCKS)?AnalyzerEngine.BASE:AnalyzerEngine.OTHER;
            return SceneFilterClient.hiddenBlock(state.getBlock())?value | AnalyzerEngine.HIDDEN:value;
        }
        @Override public boolean debrisSection(int x,int y,int z) {
            var chunk=level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
            if(chunk==null) return false;
            int index=chunk.getSectionIndexFromSectionY(y);
            if(index<0 || index>=chunk.getSectionsCount()) return false;
            var section=chunk.getSection(index);
            return !section.hasOnlyAir() && section.maybeHas(state->state.is(Blocks.ANCIENT_DEBRIS));
        }
        @Override public AnalyzerEngine.VillageSample villagers(AnalyzerEngine.Point eye,int h,int v,
                int sectionLimit,int entityLimit,int candidateLimit) {
            var manager=((ClientEntityStorageAccess)level).blocklens$entityStorage();
            var storage=((EntitySectionStorageAccess)manager).blocklens$sections();
            ArrayList<Villager> candidates=new ArrayList<>();
            int sections=0,entities=0;
            int cx=(int)Math.floor(eye.x()),cy=(int)Math.floor(eye.y()),cz=(int)Math.floor(eye.z());
            sections: for(int sx=Math.floorDiv(cx-h,16);sx<=Math.floorDiv(cx+h,16);sx++)
                for(int sy=Math.floorDiv(cy-v,16);sy<=Math.floorDiv(cy+v,16);sy++)
                    for(int sz=Math.floorDiv(cz-h,16);sz<=Math.floorDiv(cz+h,16);sz++) {
                        if(sections>=sectionLimit || entities>=entityLimit) break sections;
                        sections++;
                        var section=storage.getSection(SectionPos.asLong(sx,sy,sz));
                        if(section==null || !section.getStatus().isAccessible()) continue;
                        try(var stream=section.getEntities()) {
                            var iterator=stream.iterator();
                            while(entities<entityLimit && iterator.hasNext()) {
                                var entity=iterator.next(); entities++;
                                if(entity instanceof Villager villager && villager.isAlive() && !SceneFilterClient.hiddenEntity(villager)
                                        && Math.abs(villager.getX()-eye.x())<=h
                                        && Math.abs(villager.getY()-eye.y())<=v
                                        && Math.abs(villager.getZ()-eye.z())<=h) candidates.add(villager);
                            }
                        }
                    }
            candidates.sort(Comparator.comparingDouble(entity->entity.position().distanceToSqr(vec(eye))));
            ArrayList<AnalyzerEngine.Villager> result=new ArrayList<>();
            for(int i=0;i<Math.min(candidateLimit,candidates.size());i++) {
                var villager=candidates.get(i);
                var site=villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                AnalyzerEngine.Point endpoint=null;
                if(site.isPresent() && site.get().dimension().equals(level.dimension())) {
                    var p=site.get().pos();
                    endpoint=new AnalyzerEngine.Point(p.getX()+.5,p.getY()+.5,p.getZ()+.5);
                }
                result.add(new AnalyzerEngine.Villager(new AnalyzerEngine.Point(villager.getX(),
                        villager.getY()+villager.getBbHeight()*.65,villager.getZ()),endpoint));
            }
            return new AnalyzerEngine.VillageSample(result,sections,entities);
        }
    }
}
