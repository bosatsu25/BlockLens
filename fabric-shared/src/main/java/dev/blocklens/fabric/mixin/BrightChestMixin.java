package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve vanilla geometry, open animation and all non-regular chest materials. */
@Mixin(ChestRenderer.class)
abstract class BrightChestMixin {
    @Unique private static final SpriteId BLOCKLENS_SINGLE = blocklens$sprite("single");
    @Unique private static final SpriteId BLOCKLENS_LEFT = blocklens$sprite("left");
    @Unique private static final SpriteId BLOCKLENS_RIGHT = blocklens$sprite("right");

    @Unique private static SpriteId blocklens$sprite(String type) {
        return new SpriteId(Sheets.CHEST_SHEET,
                Identifier.fromNamespaceAndPath("blocklens", "entity/chest/bright_" + type));
    }
    @Unique private static boolean blocklens$enabled(ChestRenderState state) {
        return state.material == ChestRenderState.ChestMaterialType.REGULAR
                && BlockLensRuntime.config().isEnabled(CapabilityId.BRIGHT_CHEST);
    }
    @ModifyExpressionValue(method = "submit", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/Sheets;chooseSprite(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState$ChestMaterialType;Lnet/minecraft/world/level/block/state/properties/ChestType;)Lnet/minecraft/client/resources/model/sprite/SpriteId;"))
    private SpriteId blocklens$sprite(SpriteId original, ChestRenderState state) {
        if (!blocklens$enabled(state)) return original;
        return switch (state.type) {
            case SINGLE -> BLOCKLENS_SINGLE;
            case LEFT -> BLOCKLENS_LEFT;
            case RIGHT -> BLOCKLENS_RIGHT;
        };
    }
    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("RETURN"))
    private void blocklens$light(BlockEntity entity, ChestRenderState state, float delta,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking, CallbackInfo ci) {
        if (blocklens$enabled(state)) state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
    }
}
