package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

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
    /**
     * Apply brightness when submitting the model, not only when extracting its render state.
     * A closed chest may reuse an extracted snapshot until a lid/section update; changing
     * settings must work without interacting with the block. Restore the original light even
     * if another renderer short-circuits or throws, and leave trapped/ender chests untouched.
     */
    @WrapMethod(method = "submit")
    private void blocklens$submitWithCurrentLight(ChestRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
        if (!blocklens$enabled(state)) {
            original.call(state, poseStack, collector, camera);
            return;
        }
        int vanillaLight = state.lightCoords;
        try {
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            original.call(state, poseStack, collector, camera);
        } finally {
            state.lightCoords = vanillaLight;
        }
    }
}
