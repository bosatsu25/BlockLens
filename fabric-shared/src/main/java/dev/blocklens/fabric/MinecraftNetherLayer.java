package dev.blocklens.fabric;

import dev.blocklens.core.render.NetherTweaksVisualCue;
import dev.blocklens.core.render.RetainedValue;
import dev.blocklens.core.state.SemanticState;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Nether Tweaks for one wrapped target: the frame tint on the base geometry plus the interior fill
 * and the optional upper band, two shared extra models looked up once per wrapper on first use.
 */
final class MinecraftNetherLayer {
    private final NetherTweaksVisualCue netherCue;
    private final SemanticState netherState;
    // A failed lookup is retained too, so a missing overlay model is not looked up again per block.
    private final RetainedValue<BlockStateModel> interiorOverlay = new RetainedValue<>();
    private final @Nullable RetainedValue<BlockStateModel> bandOverlay;

    MinecraftNetherLayer(NetherTweaksVisualCue netherCue, SemanticState netherState) {
        this.netherCue = netherCue;
        this.netherState = netherState;
        this.bandOverlay = netherCue.hasUpperBand() ? new RetainedValue<>() : null;
    }

    int frameArgb() {
        return netherCue.frameArgb();
    }

    /** Emits both overlays; their quads also take the model's other enabled tint cues. */
    void emit(MinecraftDecorationModel model, QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest,
            long enabledCapabilities) {
        BlockStateModel interior = interiorOverlay.get(NetherTweaksOverlayModels.INTERIOR,
                MinecraftDecorationModel::lookup);
        if (interior != null) {
            emitter.pushTransform(quad -> {
                quad.multiplyColor(netherCue.fillArgb(netherState.axis(),
                        MinecraftDecorationModel.face(quad.nominalFace())));
                model.applyNonNetherInstructions(quad, enabledCapabilities);
                return true;
            });
            try {
                interior.emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
        }

        BlockStateModel band = bandOverlay == null ? null
                : bandOverlay.get(NetherTweaksOverlayModels.UPPER_BAND, MinecraftDecorationModel::lookup);
        if (band != null) {
            emitter.pushTransform(quad -> {
                quad.multiplyColor(netherCue.upperBandArgb());
                model.applyNonNetherInstructions(quad, enabledCapabilities);
                return true;
            });
            try {
                band.emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
        }
    }
}
