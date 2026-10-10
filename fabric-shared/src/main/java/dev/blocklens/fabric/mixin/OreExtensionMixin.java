package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.blocklens.fabric.MinecraftOreExtensions;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Explicit and conventional ore overlays are selected only while terrain sections compile. */
@Mixin(SectionCompiler.class)
abstract class OreExtensionMixin {
    @ModifyExpressionValue(method = "compile", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/block/BlockStateModelSet;get(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;"))
    private BlockStateModel blocklens$ore(BlockStateModel original, @Local(ordinal = 0) BlockState state) {
        return MinecraftOreExtensions.model(original, state);
    }
}
