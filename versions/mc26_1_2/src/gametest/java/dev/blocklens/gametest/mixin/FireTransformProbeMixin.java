package dev.blocklens.gametest.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.gametest.ComfortTransformProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ScreenEffectRenderer.class)
abstract class FireTransformProbeMixin {
    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void blocklens$probe(PoseStack poses, MultiBufferSource collector,
            TextureAtlasSprite sprite, CallbackInfo ci) {
        ComfortTransformProbe.fire(poses);
    }
}
