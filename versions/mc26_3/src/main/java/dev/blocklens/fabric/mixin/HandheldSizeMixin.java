package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.ComfortRenderPolicy;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.3 submits pre-extracted item states; wrap the item submission, never the arm scope. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class HandheldSizeMixin {
    @WrapOperation(method = "submitArmWithItem", require = 2, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void blocklens$item(ItemStackRenderState item, PoseStack poses, SubmitNodeCollector collector,
            int light, int overlay, int outline, Operation<Void> original,
            @Local(argsOnly = true) ItemStack stack) {
        var config = BlockLensRuntime.config();
        if (!config.isEnabled(CapabilityId.HANDHELD_SIZE) || stack.isEmpty()) {
            original.call(item, poses, collector, light, overlay, outline);
            return;
        }
        boolean tool = stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON)
                || stack.has(DataComponents.PIERCING_WEAPON) || stack.has(DataComponents.KINETIC_WEAPON)
                || stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.FISHING_ROD);
        float scale = ComfortRenderPolicy.heldScale(config.comfortOptions(),
                stack.getItem() instanceof BlockItem, tool, stack.is(Items.SHIELD));
        poses.pushPose();
        try {
            poses.scale(scale, scale, scale);
            original.call(item, poses, collector, light, overlay, outline);
        } finally {
            poses.popPose();
        }
    }
}
