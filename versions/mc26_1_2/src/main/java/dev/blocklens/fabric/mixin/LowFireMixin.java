package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.fabric.MinecraftComfortTransforms;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ScreenEffectRenderer.class)
abstract class LowFireMixin {
    @WrapMethod(method = "renderFire")
    private static void blocklens$fire(PoseStack poses, MultiBufferSource collector,
            TextureAtlasSprite sprite, Operation<Void> original) {
        boolean pushed = MinecraftComfortTransforms.beginFire(poses);
        try {
            original.call(poses, collector, sprite);
        } finally {
            if (pushed) poses.popPose();
        }
    }
}
