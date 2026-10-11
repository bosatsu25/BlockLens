package dev.blocklens.fabric.mixin;

import dev.blocklens.core.render.BBEChestCompatibility;
import dev.blocklens.fabric.BBEBrightChestMaterials;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.TriState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apply emissive material semantics to BBE's final quad without bypassing optimized terrain. */
@Pseudo
@Mixin(targets = "betterblockentities.client.chunk.pipeline.BBEEmitter", remap = false)
abstract class BBEBrightChestEmitterMixin {
    @Shadow private TextureAtlasSprite sprite;

    @Inject(method = "applyFlags", at = @At("RETURN"), require = 1, remap = false)
    private void blocklens$emissiveMaterial(MutableQuadViewImpl quad, CallbackInfo ci) {
        if (sprite == null || !BBEBrightChestMaterials.owns(sprite.contents().name())) return;
        // BBE's flag 2 selects a lighter that ignores emissive quads. Preserve every other flag.
        quad.setTag(BBEChestCompatibility.emissiveFlags(quad.getTag()));
        quad.setEmissive(true);
        quad.setAmbientOcclusion(TriState.FALSE);
    }
}
