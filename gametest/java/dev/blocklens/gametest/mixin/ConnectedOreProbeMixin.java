package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.ConnectedOreProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="dev.blocklens.fabric.MinecraftOreBorderLayer",remap=false)
abstract class ConnectedOreProbeMixin {
    @Unique private static final ThreadLocal<int[]> blocklens$emitting=new ThreadLocal<>();
    @Inject(method="emit",at=@At("HEAD"),remap=false)
    private void blocklens$startUv(QuadEmitter emitter,BlockAndTintGetter level,BlockPos pos,BlockState state,
            RandomSource random,Predicate<@Nullable Direction> cullTest,int connectedFaces,CallbackInfo info){
        if(ConnectedOreProbe.active() && connectedFaces>=0)blocklens$emitting.set(new int[]{pos.getX(),pos.getY(),pos.getZ()});
        else blocklens$emitting.remove();
    }
    @Inject(method="emit",at=@At("RETURN"),remap=false)
    private void blocklens$finishUv(QuadEmitter emitter,BlockAndTintGetter level,BlockPos pos,BlockState state,
            RandomSource random,Predicate<@Nullable Direction> cullTest,int connectedFaces,CallbackInfo info){blocklens$emitting.remove();}
    @ModifyArg(method="emit",at=@At(value="INVOKE",target="Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;pushTransform(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadTransform;)V"),index=0,remap=false)
    private QuadTransform blocklens$actualUv(QuadTransform transform){
        if(!ConnectedOreProbe.active())return transform;
        return quad->{
            boolean visible=transform.transform(quad);var position=blocklens$emitting.get();
            if(visible && position!=null && quad.cullFace()!=null){
                var sprite=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                        .getSprite(Identifier.fromNamespaceAndPath("blocklens","block/lightweight/connected_ore_border"));
                float minU=Float.POSITIVE_INFINITY,minV=minU,maxU=Float.NEGATIVE_INFINITY,maxV=maxU;
                for(int vertex=0;vertex<4;vertex++){minU=Math.min(minU,quad.u(vertex));minV=Math.min(minV,quad.v(vertex));maxU=Math.max(maxU,quad.u(vertex));maxV=Math.max(maxV,quad.v(vertex));}
                float u=(minU-sprite.getU0())/(sprite.getU1()-sprite.getU0())*16;
                float v=(minV-sprite.getV0())/(sprite.getV1()-sprite.getV0())*16;
                boolean valid=Math.abs(u-Math.round(u))<.001 && Math.abs(v-Math.round(v))<.001
                        && Math.abs((maxU-minU)/(sprite.getU1()-sprite.getU0())*16-1)<.001
                        && Math.abs((maxV-minV)/(sprite.getV1()-sprite.getV0())*16-1)<.001;
                for(int vertex=0;vertex<4;vertex++)valid &= dev.blocklens.testing.ConnectedQuadOrientation.matches(
                        quad.cullFace().ordinal(),quad.x(vertex),quad.y(vertex),quad.z(vertex),
                        (quad.u(vertex)-minU)/(maxU-minU),(quad.v(vertex)-minV)/(maxV-minV));
                ConnectedOreProbe.recordCell(position[0],position[1],position[2],quad.cullFace().ordinal(),Math.round(u)+16*Math.round(v),valid);
            }
            return visible;
        };
    }
    @Inject(method="connectedNeighbors",at=@At("RETURN"),remap=false)
    private static void blocklens$actualNeighbors(BlockAndTintGetter level,BlockPos pos,BlockState state,
            Direction face,@Nullable BlockStateModelPart part,CallbackInfoReturnable<Integer> info){
        ConnectedOreProbe.record(pos.getX(),pos.getY(),pos.getZ(),face.ordinal(),info.getReturnValueI());
    }
}
