package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.DecorationQuadCuePolicy;
import dev.blocklens.core.render.DecorationQuadFace;
import dev.blocklens.core.render.DecorationQuadInstruction;
import dev.blocklens.core.render.DecorationRenderDescriptor;
import dev.blocklens.core.state.SemanticState;
import java.util.Objects;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Minecraft 26.2 baked-model wrapper for M3 decoration cues. */
final class MinecraftDecorationModel extends WrapperBlockStateModel {
    private final DecorationRenderDescriptor[] descriptors;
    private final long[] descriptorBits;
    private final long representedCapabilityMask;

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
        this.descriptors = new DecorationRenderDescriptor[capabilities.length];
        this.descriptorBits = new long[capabilities.length];
        long represented = 0L;
        for (int i = 0; i < capabilities.length; i++) {
            CapabilityId capability = Objects.requireNonNull(capabilities[i], "capability");
            long bit = 1L << capability.ordinal();
            this.descriptors[i] = DecorationRenderDescriptor.of(capability, semanticStates[i]);
            this.descriptorBits[i] = bit;
            represented |= bit;
        }
        this.representedCapabilityMask = represented;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        long enabledCapabilities = BlockLensRuntime.config().enabledMask() & representedCapabilityMask;
        if (enabledCapabilities == 0L) {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        emitter.pushTransform(quad -> {
            DecorationQuadFace face = face(quad.nominalFace());
            for (int i = 0; i < descriptors.length; i++) {
                if ((enabledCapabilities & descriptorBits[i]) == 0L) {
                    continue;
                }
                DecorationQuadInstruction instruction =
                        DecorationQuadCuePolicy.instruction(descriptors[i], face);
                if (instruction.forceSolid()) {
                    quad.chunkLayer(ChunkSectionLayer.SOLID);
                }
                if (instruction.changesColor()) {
                    quad.multiplyColor(instruction.multiplyArgb());
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
        long enabledCapabilities = BlockLensRuntime.config().enabledMask() & representedCapabilityMask;
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
