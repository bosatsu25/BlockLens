package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.BBEChestCompatibility;
import dev.blocklens.fabric.BBEBrightChestMaterials;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Keep BBE's geometry and scheduling; replace only ordinary chest material selection. */
@Pseudo
@Mixin(targets = "betterblockentities.client.model.texture.SpriteSelector", remap = false)
abstract class BBEBrightChestSpriteMixin {
    @ModifyExpressionValue(method = "getChestSprite", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/Sheets;chooseSprite(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState$ChestMaterialType;Lnet/minecraft/world/level/block/state/properties/ChestType;)Lnet/minecraft/client/resources/model/sprite/SpriteId;"),
            require = 1, remap = false)
    private static SpriteId blocklens$brightMaterial(SpriteId original, BlockState state,
            BlockEntity entity, boolean christmas) {
        if (!BBEChestCompatibility.useBrightMaterial(
                BlockLensRuntime.config().isEnabled(CapabilityId.BRIGHT_CHEST), state.is(Blocks.CHEST), christmas)) {
            return original;
        }
        return BBEBrightChestMaterials.forType(state.getValue(ChestBlock.TYPE));
    }
}
