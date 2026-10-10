package dev.blocklens.fabric.mixin;

import dev.blocklens.fabric.MinecraftOreExtensions;
import dev.blocklens.fabric.SceneFilterClient;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exact-version terrain entry; fluid rendering and solid occlusion remain with the mesher. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
abstract class SodiumTerrainMixin {
    @Inject(method = "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void blocklens$filterBlock(BlockStateModel model, BlockState state,
            BlockPos position, BlockPos sectionPosition, CallbackInfo ci) {
        if (SceneFilterClient.blockFilterActive() && SceneFilterClient.hiddenBlock(state.getBlock())) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;"
            + "Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1, remap = false)
    private BlockStateModel blocklens$oreExtension(BlockStateModel original,
            BlockStateModel suppliedModel, BlockState state) {
        // Capture leading method arguments to read state without Sodium types or compiler locals.
        return MinecraftOreExtensions.model(original, state);
    }
}
