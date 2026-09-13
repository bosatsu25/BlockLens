package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.DecorationQuadCuePolicy;
import dev.blocklens.core.render.DecorationQuadFace;
import dev.blocklens.core.render.DecorationQuadInstruction;
import dev.blocklens.core.render.DecorationRenderDescriptor;
import dev.blocklens.core.render.ResourceHighlightCue;
import dev.blocklens.core.render.VisibilityQuadCuePolicy;
import dev.blocklens.core.render.VisibilityRenderDescriptor;
import dev.blocklens.core.render.VisibilityRenderPolicy;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.VisibilityStateKind;
import java.util.Objects;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
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
    private final DecorationRenderDescriptor[] decorationDescriptors;
    private final VisibilityRenderDescriptor[] visibilityDescriptors;
    private final ResourceHighlightCue[] resourceCues;
    private final long[] descriptorBits;
    private final long representedRenderableMask;

    MinecraftDecorationModel(
            BlockStateModel wrapped,
            CapabilityId[] capabilities,
            SemanticState[] semanticStates) {
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

        for (int i = 0; i < count; i++) {
            CapabilityId capability = Objects.requireNonNull(capabilities[i], "capability");
            SemanticState state = Objects.requireNonNull(semanticStates[i], "semanticState");
            long bit = 1L << capability.ordinal();

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

        emitter.pushTransform(quad -> {
            DecorationQuadFace face = face(quad.nominalFace());
            for (int i = 0; i < descriptorBits.length; i++) {
                long bit = descriptorBits[i];
                if (bit == 0L || (enabledCapabilities & bit) == 0L) {
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
                        instruction = DecorationQuadInstruction.emissiveTint(resource.accentArgb());
                    }
                }

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
            return true;
        });
        try {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
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
