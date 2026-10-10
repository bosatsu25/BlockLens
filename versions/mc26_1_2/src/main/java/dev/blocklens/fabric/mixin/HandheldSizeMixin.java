package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.ComfortRenderPolicy;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;

/** Item model scope only: vanilla arms, maps and hand animation stay outside this method. */
@Mixin(ItemInHandRenderer.class)
abstract class HandheldSizeMixin {
    @WrapMethod(method = "renderItem")
    private void blocklens$renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            PoseStack poses, SubmitNodeCollector collector, int light, Operation<Void> original) {
        var config = BlockLensRuntime.config();
        boolean firstPerson = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        if (!firstPerson || !config.isEnabled(CapabilityId.HANDHELD_SIZE) || stack.isEmpty()) {
            original.call(entity, stack, context, poses, collector, light);
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
            original.call(entity, stack, context, poses, collector, light);
        } finally {
            poses.popPose();
        }
    }
}
