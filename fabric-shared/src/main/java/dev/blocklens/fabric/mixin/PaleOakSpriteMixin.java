package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.NativeImage;
import dev.blocklens.fabric.MinecraftPaleOakPalette;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader.class)
interface PaleOakSpriteMixin {
    @ModifyExpressionValue(method = "lambda$create$0", at = @At(value = "INVOKE", target =
            "Lcom/mojang/blaze3d/platform/NativeImage;read(Ljava/io/InputStream;)Lcom/mojang/blaze3d/platform/NativeImage;"))
    private static NativeImage blocklens$palette(NativeImage image,
            @Local(argsOnly = true) Identifier texture, @Local(argsOnly = true) Resource source) {
        return MinecraftPaleOakPalette.apply(image, texture, source, true);
    }
}
