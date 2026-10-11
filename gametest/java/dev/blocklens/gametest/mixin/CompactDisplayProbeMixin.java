package dev.blocklens.gametest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.gametest.CompactDisplayProbe;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemTransform.class)
abstract class CompactDisplayProbeMixin {
    @Shadow public abstract Vector3fc rotation();
    @Shadow public abstract Vector3fc translation();
    @Shadow public abstract Vector3fc scale();
    @Inject(method = "apply", at = @At("RETURN"))
    private void blocklens$draw(boolean mirrored, PoseStack.Pose poses, CallbackInfo callback) {
        CompactDisplayProbe.record(mirrored, rotation(), translation(), scale());
    }
}
