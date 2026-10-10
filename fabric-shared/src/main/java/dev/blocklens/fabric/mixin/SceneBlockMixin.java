package dev.blocklens.fabric.mixin;

import dev.blocklens.fabric.SceneFilterClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Suppress world meshes without changing pure state queries or reload-time model groups. */
@Mixin(SectionCompiler.class)
abstract class SceneBlockMixin {
    @ModifyExpressionValue(method="compile",at=@At(value="INVOKE",target=
            "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"))
    private RenderShape blocklens$filterBlock(RenderShape original,@Local(ordinal=0) BlockState state) {
        return SceneFilterClient.blockFilterActive() && SceneFilterClient.hiddenBlock(state.getBlock())
                ?RenderShape.INVISIBLE:original;
    }
}
