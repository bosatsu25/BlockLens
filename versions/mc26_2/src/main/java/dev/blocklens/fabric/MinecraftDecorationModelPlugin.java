package dev.blocklens.fabric;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.TargetCapabilityIndex;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/** Zero-scan model-bake integration for the 26.2 all-capability BlockLens pipeline. */
public final class MinecraftDecorationModelPlugin {
    private static final CapabilityId[] CAPABILITIES = CapabilityId.values();
    private static final AtomicInteger WRAPPED_MODELS = new AtomicInteger();
    private static volatile boolean modelPipelineReady;

    private MinecraftDecorationModelPlugin() {
    }

    public static void register() {
        ModelLoadingPlugin.register(pluginContext -> {
            TargetCapabilityIndex targetIndex = MinecraftDecorationTargetIndex.build();
            WRAPPED_MODELS.set(0);
            pluginContext.modifyBlockModelAfterBake().register(
                    ModelModifier.WRAP_PHASE,
                    (model, context) -> wrap(model, context.state(), targetIndex));
            modelPipelineReady = true;
        });
    }

    public static boolean isModelPipelineReady() {
        return modelPipelineReady;
    }

    public static int wrappedModelCount() {
        return WRAPPED_MODELS.get();
    }

    private static BlockStateModel wrap(
            BlockStateModel model,
            BlockState state,
            TargetCapabilityIndex targetIndex) {
        if (state == null) {
            return model;
        }
        int rawId = BuiltInRegistries.BLOCK.getId(state.getBlock());
        long targetMask = targetIndex.mask(rawId);
        if (targetMask == 0L) {
            return model;
        }

        int count = Long.bitCount(targetMask);
        CapabilityId[] capabilities = new CapabilityId[count];
        SemanticState[] semanticStates = new SemanticState[count];
        int cursor = 0;
        for (CapabilityId capability : CAPABILITIES) {
            long bit = 1L << capability.ordinal();
            if ((targetMask & bit) == 0L) {
                continue;
            }
            capabilities[cursor] = capability;
            semanticStates[cursor] = MinecraftStateAdapter.interpret(capability, state);
            cursor++;
        }
        if (cursor != count) {
            throw new IllegalStateException("BlockLens target mask could not be fully decoded");
        }
        WRAPPED_MODELS.incrementAndGet();
        return new MinecraftDecorationModel(model, capabilities, semanticStates);
    }
}
