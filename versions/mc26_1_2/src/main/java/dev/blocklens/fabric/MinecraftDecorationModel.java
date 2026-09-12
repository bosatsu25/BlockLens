package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensConfig;
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

/**
 * Minecraft 26.1.2 baked-model wrapper for M3 decoration cues.
 *
 * <p>Semantic interpretation happens once during model bake. The render hot path only reads the
 * current immutable config snapshot and applies common quad instructions to the already-baked base
 * model, preserving active resource-pack textures.</p>
 */
final class MinecraftDecorationModel extends WrapperBlockStateModel {
    private final DecorationRenderDescriptor[] descriptors;

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
        for (int i = 0; i < capabilities.length; i++) {
            this.descriptors[i] = DecorationRenderDescriptor.of(capabilities[i], semanticStates[i]);
        }
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        BlockLensConfig config = BlockLensRuntime.config();
        long enabledMask = enabledMask(config);
        if (enabledMask == 0L) {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        emitter.pushTransform(quad -> {
            DecorationQuadFace face = face(quad.nominalFace());
            for (int i = 0; i < descriptors.length; i++) {
                if ((enabledMask & (1L << i)) == 0L) {
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
        long enabledMask = enabledMask(BlockLensRuntime.config());
        if (enabledMask == 0L) {
            return wrappedKey;
        }
        return new GeometryKey(wrappedKey, enabledMask);
    }

    private long enabledMask(BlockLensConfig config) {
        long mask = 0L;
        for (int i = 0; i < descriptors.length; i++) {
            if (config.isEnabled(descriptors[i].capability())) {
                mask |= 1L << i;
            }
        }
        return mask;
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

    private record GeometryKey(Object wrappedKey, long enabledMask) {
    }
}
