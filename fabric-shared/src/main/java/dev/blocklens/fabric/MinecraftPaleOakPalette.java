package dev.blocklens.fabric;

import com.mojang.blaze3d.platform.NativeImage;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.PaleOakPalette;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/** Mutates only the image already owned by the normal loader, before upload/mipmap generation. */
public final class MinecraftPaleOakPalette {
    private MinecraftPaleOakPalette() { }

    public static NativeImage apply(NativeImage image, Identifier texture, Resource source, boolean sprite) {
        boolean enabled = BlockLensRuntime.config().isEnabled(CapabilityId.PALE_OAK_NEUTRAL);
        if (!enabled) return image;
        String path = sprite ? "textures/" + texture.getPath() + ".png" : texture.getPath();
        if (!PaleOakPalette.applies(true, texture.getNamespace(), path, source.sourcePackId(),
                image.getWidth(), image.getHeight()) || image.format() != NativeImage.Format.RGBA) return image;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int old = image.getPixel(x, y);
                int recolored = PaleOakPalette.neutralWood(old);
                if (old != recolored) image.setPixel(x, y, recolored);
            }
        }
        return image;
    }
}
