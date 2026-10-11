package dev.blocklens.gametest.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only inspection of the real retained atlas image. */
@Mixin(SpriteContents.class)
public interface PaletteSpriteAccess {
    @Accessor("originalImage") NativeImage blocklens$originalImage();
}
