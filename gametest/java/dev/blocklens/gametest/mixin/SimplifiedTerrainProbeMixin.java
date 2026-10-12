package dev.blocklens.gametest.mixin;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.SimplifiedTerrainPolicy;
import dev.blocklens.gametest.SimplifiedTerrainProbe;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="dev.blocklens.fabric.MinecraftSimplifiedTerrain$SimplifiedModel",remap=false)
abstract class SimplifiedTerrainProbeMixin {
    @Shadow @Final private SimplifiedTerrainPolicy.Kind kind;
    @Inject(method="emitQuads",at=@At("HEAD"),remap=false)
    private void blocklens$realEmission(QuadEmitter emitter,BlockAndTintGetter level,BlockPos pos,BlockState state,
            RandomSource random,Predicate<@Nullable Direction> cullTest,CallbackInfo info) {
        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        SimplifiedTerrainProbe.record(id.getNamespace(),id.getPath(),kind,
                BlockLensRuntime.config().isEnabled(CapabilityId.SIMPLIFIED_TERRAIN));
    }
    @ModifyArg(method="emitQuads",at=@At(value="INVOKE",target="Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;pushTransform(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadTransform;)V"),index=0,remap=false)
    private QuadTransform blocklens$observeNeutralTint(QuadTransform transform) {
        return quad->{
            boolean emitted=transform.transform(quad);
            if(emitted)SimplifiedTerrainProbe.recordNeutral(kind,
                    BlockLensRuntime.config().isEnabled(CapabilityId.SIMPLIFIED_TERRAIN),
                    quad.tintIndex()==-1 && quad.color(0)==-1 && quad.color(1)==-1 && quad.color(2)==-1 && quad.color(3)==-1);
            return emitted;
        };
    }

}