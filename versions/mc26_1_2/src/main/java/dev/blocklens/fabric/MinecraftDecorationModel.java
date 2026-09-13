package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.DecorationQuadCuePolicy;
import dev.blocklens.core.render.DecorationQuadFace;
import dev.blocklens.core.render.DecorationQuadInstruction;
import dev.blocklens.core.render.DecorationRenderDescriptor;
import dev.blocklens.core.render.NetherTweaksVisualCue;
import dev.blocklens.core.render.ResourceHighlightCue;
import dev.blocklens.core.render.VisibilityQuadCuePolicy;
import dev.blocklens.core.render.VisibilityRenderDescriptor;
import dev.blocklens.core.render.VisibilityRenderPolicy;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.VisibilityStateKind;
import java.util.Objects;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Minecraft 26.1.2 baked-model wrapper shared by the M3-M7 rendering families. */
final class MinecraftDecorationModel extends WrapperBlockStateModel {
    private static final long NETHER_TWEAKS_BIT = 1L << CapabilityId.NETHER_TWEAKS.ordinal();

    private final DecorationRenderDescriptor[] decorationDescriptors;
    private final VisibilityRenderDescriptor[] visibilityDescriptors;
    private final ResourceHighlightCue[] resourceCues;
    private final long[] descriptorBits;
    private final long representedRenderableMask;
    private final @Nullable NetherTweaksVisualCue netherCue;
    private final SemanticState netherState;

    private volatile @Nullable BlockStateModel netherInteriorOverlay;
    private volatile @Nullable BlockStateModel netherBandOverlay;
    private volatile boolean interiorLookupAttempted;
    private volatile boolean bandLookupAttempted;

    MinecraftDecorationModel(
            BlockStateModel wrapped,
            CapabilityId[] capabilities,
            SemanticState[] semanticStates,
            @Nullable NetherTweaksVisualCue netherCue) {
        super(Objects.requireNonNull(wrapped, "wrapped"));
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(semanticStates, "semanticStates");
        if (capabilities.length == 0 || capabilities.length != semanticStates.length) {
            throw new IllegalArgumentException("capability/state arrays must be non-empty and have equal length");
        }

        int count = capabilities.length;
        this.decorationDescriptors = new DecorationRenderDescriptor[count];
        this.visibilityDescriptors = new VisibilityRenderDescriptor[count];
        this.resourceCues = new ResourceHighlightCue[count];
        this.descriptorBits = new long[count];
        long represented = 0L;
        boolean foundNether = false;
        SemanticState capturedNetherState = SemanticState.empty();

        for (int i = 0; i < count; i++) {
            CapabilityId capability = Objects.requireNonNull(capabilities[i], "capability");
            SemanticState state = Objects.requireNonNull(semanticStates[i], "semanticState");
            long bit = 1L << capability.ordinal();

            if (capability == CapabilityId.NETHER_TWEAKS) {
                if (netherCue == null) {
                    throw new IllegalArgumentException("Nether Tweaks capability requires a visual cue");
                }
                capturedNetherState = state;
                foundNether = true;
                descriptorBits[i] = bit;
                represented |= bit;
                continue;
            }
            if (DecorationStateKind.isDecorationCapability(capability)) {
                decorationDescriptors[i] = DecorationRenderDescriptor.of(capability, state);
            } else if (VisibilityStateKind.isVisibilityCapability(capability)) {
                visibilityDescriptors[i] = VisibilityRenderPolicy.describeEnabled(capability, state);
            } else if (capability.category() == CapabilityId.Category.RESOURCE) {
                resourceCues[i] = ResourceHighlightCue.forCapability(capability);
            } else {
                continue;
            }
            descriptorBits[i] = bit;
            represented |= bit;
        }
        if (netherCue != null && !foundNether) {
            throw new IllegalArgumentException("Nether Tweaks cue supplied without target capability");
        }
        this.netherCue = netherCue;
        this.netherState = capturedNetherState;
        this.representedRenderableMask = represented;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        long enabledCapabilities = BlockLensRuntime.config().enabledMask() & representedRenderableMask;
        if (enabledCapabilities == 0L) {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        boolean netherEnabled = netherCue != null && (enabledCapabilities & NETHER_TWEAKS_BIT) != 0L;
        emitter.pushTransform(quad -> {
            if (netherEnabled) {
                quad.multiplyColor(netherCue.frameArgb());
            }
            applyNonNetherInstructions(quad, enabledCapabilities);
            return true;
        });
        try {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }

        if (netherEnabled) {
            emitNetherOverlay(emitter, level, pos, state, random, cullTest, enabledCapabilities);
        }
    }

    private void emitNetherOverlay(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest,
            long enabledCapabilities) {
        BlockStateModel interior = lookupInteriorOverlay();
        if (interior != null) {
            emitter.pushTransform(quad -> {
                DecorationQuadFace face = face(quad.nominalFace());
                quad.multiplyColor(fillColor(face));
                applyNonNetherInstructions(quad, enabledCapabilities);
                return true;
            });
            try {
                interior.emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
        }

        if (netherCue.hasUpperBand()) {
            BlockStateModel band = lookupBandOverlay();
            if (band != null) {
                emitter.pushTransform(quad -> {
                    quad.multiplyColor(netherCue.upperBandArgb());
                    applyNonNetherInstructions(quad, enabledCapabilities);
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

    private int fillColor(DecorationQuadFace face) {
        SemanticState.Axis axis = netherState.axis();
        if (axis != SemanticState.Axis.NONE) {
            boolean endFace = switch (axis) {
                case X -> face == DecorationQuadFace.EAST || face == DecorationQuadFace.WEST;
                case Y -> face == DecorationQuadFace.UP || face == DecorationQuadFace.DOWN;
                case Z -> face == DecorationQuadFace.NORTH || face == DecorationQuadFace.SOUTH;
                case NONE -> false;
            };
            return endFace ? netherCue.upFillArgb() : netherCue.sideFillArgb();
        }
        return switch (face) {
            case UP -> netherCue.upFillArgb();
            case DOWN -> netherCue.downFillArgb();
            default -> netherCue.sideFillArgb();
        };
    }

    private void applyNonNetherInstructions(MutableQuadView quad, long enabledCapabilities) {
        DecorationQuadFace face = face(quad.nominalFace());
        for (int i = 0; i < descriptorBits.length; i++) {
            long bit = descriptorBits[i];
            if (bit == 0L || bit == NETHER_TWEAKS_BIT || (enabledCapabilities & bit) == 0L) {
                continue;
            }

            DecorationQuadInstruction instruction;
            DecorationRenderDescriptor decoration = decorationDescriptors[i];
            if (decoration != null) {
                instruction = DecorationQuadCuePolicy.instruction(decoration, face);
            } else {
                VisibilityRenderDescriptor visibility = visibilityDescriptors[i];
                if (visibility != null) {
                    instruction = VisibilityQuadCuePolicy.instruction(visibility);
                } else {
                    ResourceHighlightCue resource = resourceCues[i];
                    instruction = resource.instruction();
                }
            }
            applyInstruction(quad, instruction);
        }
    }

    private static void applyInstruction(MutableQuadView quad, DecorationQuadInstruction instruction) {
        if (instruction.forceSolid()) {
            quad.chunkLayer(ChunkSectionLayer.SOLID);
        }
        if (instruction.changesColor()) {
            quad.multiplyColor(instruction.multiplyArgb());
        }
        if (instruction.emissive()) {
            quad.emissive(true);
            quad.diffuseShade(false);
            quad.ambientOcclusion(TriState.FALSE);
        }
    }

    private @Nullable BlockStateModel lookupInteriorOverlay() {
        BlockStateModel cached = netherInteriorOverlay;
        if (cached != null || interiorLookupAttempted) {
            return cached;
        }
        synchronized (this) {
            cached = netherInteriorOverlay;
            if (cached != null || interiorLookupAttempted) {
                return cached;
            }
            interiorLookupAttempted = true;
            cached = lookup(NetherTweaksOverlayModels.INTERIOR);
            netherInteriorOverlay = cached;
            return cached;
        }
    }

    private @Nullable BlockStateModel lookupBandOverlay() {
        BlockStateModel cached = netherBandOverlay;
        if (cached != null || bandLookupAttempted) {
            return cached;
        }
        synchronized (this) {
            cached = netherBandOverlay;
            if (cached != null || bandLookupAttempted) {
                return cached;
            }
            bandLookupAttempted = true;
            cached = lookup(NetherTweaksOverlayModels.UPPER_BAND);
            netherBandOverlay = cached;
            return cached;
        }
    }

    private static @Nullable BlockStateModel lookup(ExtraModelKey<BlockStateModel> key) {
        try {
            return Minecraft.getInstance().getModelManager().getModel(key);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    @Override
    @Nullable
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        Object wrappedKey = wrapped.createGeometryKey(level, pos, state, random);
        if (wrappedKey == null) {
            return null;
        }
        long enabledCapabilities = BlockLensRuntime.config().enabledMask() & representedRenderableMask;
        if (enabledCapabilities == 0L) {
            return wrappedKey;
        }
        return new GeometryKey(wrappedKey, enabledCapabilities);
    }

    private static DecorationQuadFace face(@Nullable Direction direction) {
        if (direction == null) {
            return DecorationQuadFace.NONE;
        }
        return switch (direction) {
            case NORTH -> DecorationQuadFace.NORTH;
            case EAST -> DecorationQuadFace.EAST;
            case SOUTH -> DecorationQuadFace.SOUTH;
            case WEST -> DecorationQuadFace.WEST;
            case UP -> DecorationQuadFace.UP;
            case DOWN -> DecorationQuadFace.DOWN;
        };
    }

    private record GeometryKey(Object wrappedKey, long enabledCapabilities) {
    }
}
