package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.VisualGeometryKey;
import dev.blocklens.core.render.DecorationDecals;
import dev.blocklens.core.render.DecorationLines;
import dev.blocklens.core.render.DecorationQuadFace;
import dev.blocklens.core.render.DecorationQuadInstruction;
import dev.blocklens.core.render.DecorationRenderDescriptor;
import dev.blocklens.core.render.NetherTweaksVisualCue;
import dev.blocklens.core.render.ResourceHighlightCue;
import dev.blocklens.core.render.ResourceBorderPolicy;
import dev.blocklens.core.render.FaceInstructionTable;
import dev.blocklens.core.render.RetainedValue;
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
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.IronBarsBlock;
import org.jspecify.annotations.Nullable;

/**
 * Shared baked-model wrapper of the M3-M7 rendering families: it applies the tint cues to the
 * wrapped geometry and composes the overlay layers. Each layer exists only on wrappers whose
 * capabilities draw it, so most wrappers carry no decal, Nether or ore-border state at all.
 */
final class MinecraftDecorationModel extends WrapperBlockStateModel {
    private static final long NETHER_TWEAKS_BIT = CapabilityId.NETHER_TWEAKS.renderBit();

    private static final long LIGHTWEIGHT_OVERLAY_MASK = CapabilityId.GLASS_HIGHLIGHT.renderBit()
            | CapabilityId.KELP_HIGHLIGHT.renderBit();

    private final Object[] descriptors;
    private final long[] descriptorBits;
    private final long representedRenderableMask;
    private final @Nullable RetainedValue<BlockStateModel> lightweightOverlay;
    private final @Nullable MinecraftDecalLayer decalLayer;
    private final @Nullable MinecraftNetherLayer netherLayer;
    private final @Nullable MinecraftOreBorderLayer oreBorderLayer;

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
        this.descriptors = new Object[count];
        this.descriptorBits = new long[count];
        long represented = 0L;
        long borders = 0L;
        int borderTint = 0;
        SemanticState netherState = null;
        // Decals re-emit the wrapped geometry; only a single fixed variant guarantees the second
        // emission matches the first. Weighted, multipart and other wrappers keep the tint cue.
        boolean decalCapable = wrapped.getClass() == SingleVariant.class;
        long[][] decals = null;
        DecorationLines.Line[][] lines = null;
        long decalBits = 0L;

        for (int i = 0; i < count; i++) {
            CapabilityId capability = Objects.requireNonNull(capabilities[i], "capability");
            SemanticState state = Objects.requireNonNull(semanticStates[i], "semanticState");
            long bit = capability.renderBit();

            if (capability == CapabilityId.NETHER_TWEAKS) {
                if (netherCue == null) {
                    throw new IllegalArgumentException("Nether Tweaks capability requires a visual cue");
                }
                netherState = state;
                descriptorBits[i] = bit;
                represented |= bit;
                continue;
            }
            if (capability == CapabilityId.GLASS_HIGHLIGHT || capability == CapabilityId.KELP_HIGHLIGHT) {
                descriptors[i] = DecorationQuadInstruction.IDENTITY;
            } else if (capability == CapabilityId.BRIGHT_CONCRETE) {
                descriptors[i] = DecorationQuadInstruction.emissiveTint(0xFFFFFFFF);
            } else if (DecorationStateKind.isDecorationCapability(capability)) {
                var descriptor = DecorationRenderDescriptor.of(capability, state);
                descriptors[i] = FaceInstructionTable.compile(descriptor);
                long[] decalTable = decalCapable ? DecorationDecals.table(descriptor) : null;
                DecorationLines.Line[] lineSet = decalCapable ? DecorationLines.lines(descriptor) : null;
                if (decalTable != null && (!DecorationDecals.isEmpty(decalTable) || lineSet.length > 0)) {
                    if (decals == null) {
                        decals = new long[count][];
                        lines = new DecorationLines.Line[count][];
                    }
                    decals[i] = decalTable;
                    lines[i] = lineSet;
                    decalBits |= bit;
                }
            } else if (VisibilityStateKind.isVisibilityCapability(capability)) {
                descriptors[i] = VisibilityRenderPolicy.describeEnabled(capability, state).cue().instruction();
            } else if (capability.isResourceHighlight()) {
                var cue = ResourceHighlightCue.forCapability(capability);
                descriptors[i] = cue.instruction();
                if (ResourceBorderPolicy.supports(capability)) {
                    borders |= bit;
                    borderTint = cue.accentArgb();
                }
            } else {
                continue;
            }
            descriptorBits[i] = bit;
            represented |= bit;
        }
        if (netherCue != null && netherState == null) {
            throw new IllegalArgumentException("Nether Tweaks cue supplied without target capability");
        }
        this.representedRenderableMask = represented;
        this.lightweightOverlay = (represented & LIGHTWEIGHT_OVERLAY_MASK) == 0L ? null : new RetainedValue<>();
        this.decalLayer = decals == null ? null : new MinecraftDecalLayer(descriptorBits, decals, lines, decalBits);
        this.netherLayer = netherCue == null ? null : new MinecraftNetherLayer(netherCue, netherState);
        this.oreBorderLayer = borders == 0L ? null : new MinecraftOreBorderLayer(wrapped, borders, borderTint);
    }

    boolean wraps(BlockStateModel model) {
        return wrapped == model;
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

        boolean bordersEnabled = oreBorderLayer != null && (enabledCapabilities & oreBorderLayer.mask()) != 0L;
        boolean connected = bordersEnabled && oreBorderLayer.connectable()
                && BlockLensRuntime.config().isEnabled(CapabilityId.CONNECTED_ORE_EDGES);
        int[] cubeFaces = connected ? new int[2] : null;
        boolean netherEnabled = netherLayer != null && (enabledCapabilities & NETHER_TWEAKS_BIT) != 0L;
        // A decoration drawn as a decal keeps the active texture's colors instead of tinting it.
        long decalCapabilities = decalLayer == null ? 0L : enabledCapabilities & decalLayer.mask();
        long tintCapabilities = enabledCapabilities & ~decalCapabilities;
        if (cubeFaces == null && !netherEnabled && tintCapabilities == 0L) {
            // Decal-only cues leave the base geometry untouched: no per-quad transform at all.
            super.emitQuads(emitter, level, pos, state, random, cullTest);
        } else {
            emitter.pushTransform(quad -> {
                if (cubeFaces != null) {
                    MinecraftOreBorderLayer.recordCubeFace(quad, cubeFaces);
                }
                if (netherEnabled) {
                    quad.multiplyColor(netherLayer.frameArgb());
                }
                applyNonNetherInstructions(quad, tintCapabilities);
                return true;
            });
            try {
                super.emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
        }

        if (netherEnabled) {
            netherLayer.emit(this, emitter, level, pos, state, random, cullTest, tintCapabilities);
        }
        if ((enabledCapabilities & LIGHTWEIGHT_OVERLAY_MASK) != 0L) {
            emitLightweightOverlay(emitter, level, pos, state, random, cullTest, enabledCapabilities);
        }
        if (bordersEnabled) {
            oreBorderLayer.emit(emitter, level, pos, state, random, cullTest,
                    cubeFaces == null ? -1 : cubeFaces[0] & ~cubeFaces[1]);
        }
        if (decalCapabilities != 0L) {
            decalLayer.emit(this, emitter, level, pos, state, random, cullTest, decalCapabilities);
        }
    }

    /** The wrapped model's own quads, for layers that re-emit the base geometry. */
    void emitWrapped(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, Predicate<@Nullable Direction> cullTest) {
        super.emitQuads(emitter, level, pos, state, random, cullTest);
    }

    /** The baked part that marks a same-ore neighbor for connected borders, or null. */
    @Nullable BlockStateModelPart fixedOrePart() {
        return oreBorderLayer == null ? null : oreBorderLayer.fixedPart();
    }

    private void emitLightweightOverlay(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest, long enabled) {
        ExtraModelKey<BlockStateModel> key = (enabled & CapabilityId.KELP_HIGHLIGHT.renderBit()) != 0L
                ? LightweightOverlayModels.KELP : state.getBlock() instanceof IronBarsBlock
                ? LightweightOverlayModels.PANE : LightweightOverlayModels.GLASS;
        BlockStateModel overlay = lightweightOverlay == null ? null
                : lightweightOverlay.get(key, MinecraftDecorationModel::lookup);
        if (overlay == null) return;
        emitter.pushTransform(quad -> {
            quad.chunkLayer(ChunkSectionLayer.CUTOUT);
            quad.emissive(true);
            MinecraftQuadLighting.disableDirectionalShade(quad);
            quad.ambientOcclusion(TriState.FALSE);
            return true;
        });
        try {
            overlay.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    void applyNonNetherInstructions(MutableQuadView quad, long enabledCapabilities) {
        DecorationQuadFace face = face(quad.nominalFace());
        for (int i = 0; i < descriptorBits.length; i++) {
            long bit = descriptorBits[i];
            if (bit == 0L || bit == NETHER_TWEAKS_BIT || (enabledCapabilities & bit) == 0L) {
                continue;
            }

            DecorationQuadInstruction instruction;
            Object descriptor = descriptors[i];
            if (descriptor instanceof FaceInstructionTable table) {
                instruction = table.instruction(face);
            } else {
                instruction = (DecorationQuadInstruction) descriptor;
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
            MinecraftQuadLighting.disableDirectionalShade(quad);
            quad.ambientOcclusion(TriState.FALSE);
        }
    }

    /** An extra model of the current bake, or null when it failed to load. */
    static @Nullable BlockStateModel lookup(ExtraModelKey<BlockStateModel> key) {
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
        // Connected borders depend on the neighbors, so no geometry key may share the section's quads.
        if (oreBorderLayer != null && (BlockLensRuntime.config().enabledMask() & oreBorderLayer.mask()) != 0L
                && BlockLensRuntime.config().isEnabled(CapabilityId.CONNECTED_ORE_EDGES)) {
            return null;
        }
        Object wrappedKey = wrapped.createGeometryKey(level, pos, state, random);
        if (wrappedKey == null) {
            return null;
        }
        long enabledCapabilities = BlockLensRuntime.config().enabledMask() & representedRenderableMask;
        if (enabledCapabilities == 0L) {
            return wrappedKey;
        }
        return new VisualGeometryKey(wrappedKey, enabledCapabilities);
    }

    static DecorationQuadFace face(@Nullable Direction direction) {
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
}
