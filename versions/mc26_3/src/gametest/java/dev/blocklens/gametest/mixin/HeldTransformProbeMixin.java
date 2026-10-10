package dev.blocklens.gametest.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.gametest.ComfortTransformProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemStackRenderState.class)
abstract class HeldTransformProbeMixin {
    @Shadow ItemDisplayContext displayContext;
    @Inject(method = "submit", at = @At("HEAD"))
    private void blocklens$probe(PoseStack poses, SubmitNodeCollector collector,
            int light, int overlay, int outline, CallbackInfo ci) {
        ComfortTransformProbe.held(displayContext, poses);
    }
}
