package dev.blocklens.fabric;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.NetherTweaksVisualCue;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.TargetCapabilityIndex;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/** Zero-scan model-bake integration for the 26.1.2 all-capability BlockLens pipeline. */
public final class MinecraftDecorationModelPlugin {
    private static final CapabilityId[] CAPABILITIES = CapabilityId.values();
    private static final long NETHER_TWEAKS_BIT = 1L << CapabilityId.NETHER_TWEAKS.ordinal();
    private static final AtomicInteger WRAPPED_MODELS = new AtomicInteger();
    private static final AtomicInteger RETAINED_CAPABILITY_SLOTS = new AtomicInteger();
    private static final AtomicInteger MAX_CAPABILITIES_PER_MODEL = new AtomicInteger();
    private static final AtomicInteger NETHER_WRAPPED_MODELS = new AtomicInteger();
    private static volatile boolean modelPipelineReady;

    private MinecraftDecorationModelPlugin() {
    }

    public static void register() {
        ModelLoadingPlugin.register(pluginContext -> {
            TargetCapabilityIndex targetIndex = MinecraftDecorationTargetIndex.build();
            NetherTweaksOverlayModels.register(pluginContext);
            WRAPPED_MODELS.set(0);
            RETAINED_CAPABILITY_SLOTS.set(0);
            MAX_CAPABILITIES_PER_MODEL.set(0);
            NETHER_WRAPPED_MODELS.set(0);
            MinecraftDecorationModel.resetObservationCounters();
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

    public static int retainedCapabilitySlotCount() {
        return RETAINED_CAPABILITY_SLOTS.get();
    }

    public static int maxCapabilitiesPerWrappedModel() {
        return MAX_CAPABILITIES_PER_MODEL.get();
    }

    public static int netherWrappedModelCount() {
        return NETHER_WRAPPED_MODELS.get();
    }

    public static int netherInteriorLookupCount() {
        return MinecraftDecorationModel.interiorLookupCount();
    }

    public static int netherBandLookupCount() {
        return MinecraftDecorationModel.bandLookupCount();
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

        NetherTweaksVisualCue netherCue = null;
        if ((targetMask & NETHER_TWEAKS_BIT) != 0L) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (id == null) {
                throw new IllegalStateException("Nether Tweaks target has no registry id: " + state.getBlock());
            }
            netherCue = NetherTweaksVisualCue.forTarget(id.getPath());
            if (netherCue == null) {
                throw new IllegalStateException("Missing Nether Tweaks visual cue for " + id);
            }
        }

        WRAPPED_MODELS.incrementAndGet();
        RETAINED_CAPABILITY_SLOTS.addAndGet(count);
        MAX_CAPABILITIES_PER_MODEL.accumulateAndGet(count, Math::max);
        if (netherCue != null) {
            NETHER_WRAPPED_MODELS.incrementAndGet();
        }
        return new MinecraftDecorationModel(model, capabilities, semanticStates, netherCue);
    }
}
