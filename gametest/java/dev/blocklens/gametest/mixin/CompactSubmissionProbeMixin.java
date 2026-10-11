package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.CompactDisplayProbe;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Excludes parse/bounds/HUD contexts from actual submitted pose observations. */
@Mixin(ItemStackRenderState.class)
abstract class CompactSubmissionProbeMixin {
    @Shadow ItemDisplayContext displayContext;
    @Inject(method = "submit", at = @At("HEAD"))
    private void blocklens$begin(CallbackInfo callback) { CompactDisplayProbe.begin(displayContext); }
    @Inject(method = "submit", at = @At("RETURN"))
    private void blocklens$end(CallbackInfo callback) { CompactDisplayProbe.end(); }
}
