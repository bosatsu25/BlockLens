package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.SodiumTerrainProbe;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the actual emission path without substituting models or cancelling rendering. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
abstract class SodiumTerrainProbeMixin {
    @Inject(method = "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/services/PlatformModelEmitter;emitModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Ljava/util/function/Predicate;Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;Lnet/minecraft/util/RandomSource;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/caffeinemc/mods/sodium/client/services/PlatformModelEmitter$Bufferer;)V"), require = 1, remap = false)
    private void blocklens$observeEmission(BlockStateModel model, BlockState state,
            BlockPos position, BlockPos offset, CallbackInfo callback) {
        SodiumTerrainProbe.emitted(model, state, position);
    }
}
