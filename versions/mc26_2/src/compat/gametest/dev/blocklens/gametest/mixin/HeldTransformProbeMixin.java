package dev.blocklens.gametest.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.gametest.ComfortTransformProbe;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
abstract class HeldTransformProbeMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void blocklens$probe(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            PoseStack poses, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (!stack.isEmpty()) ComfortTransformProbe.held(context, poses);
    }
}
