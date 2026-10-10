package dev.blocklens.fabric.mixin;

import dev.blocklens.fabric.SceneFilterClient;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
abstract class SceneBlockEntityMixin {
    @Inject(method="tryExtractRenderState",at=@At("HEAD"),cancellable=true)
    private void blocklens$filterBlockEntity(BlockEntity entity,float delta,ModelFeatureRenderer.CrumblingOverlay overlay,
            boolean visible,CallbackInfoReturnable<BlockEntityRenderState> info) {
        if (SceneFilterClient.hiddenBlock(entity.getBlockState().getBlock())) info.setReturnValue(null);
    }
}
