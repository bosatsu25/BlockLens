package dev.blocklens.fabric;

import dev.blocklens.core.render.ResourceBorderPolicy;
import dev.blocklens.core.render.RetainedValue;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Clean-room emissive ore edges, baked once and emitted only for enabled ore highlights. With
 * Connected Ore Edges each full unit-cube face takes the connected sheet's tile for its same-ore
 * neighbors, so a vein draws one outline instead of one per block.
 */
final class MinecraftOreBorderLayer {
    private static final Identifier CONNECTED_SPRITE =
            Identifier.fromNamespaceAndPath("blocklens", "block/lightweight/connected_ore_border");

    private final BlockStateModel wrapped;
    private final long mask;
    private final int tint;
    private final RetainedValue<BlockStateModel> overlay = new RetainedValue<>();
    private final RetainedValue<BlockStateModel> connectedOverlay = new RetainedValue<>();
    // Only one fixed variant has a single baked part that identifies same-ore neighbors.
    private final @Nullable RetainedValue<BlockStateModelPart> singlePart;

    MinecraftOreBorderLayer(BlockStateModel wrapped, long mask, int tint) {
        this.wrapped = wrapped;
        this.mask = mask;
        this.tint = tint;
        this.singlePart = wrapped.getClass() == SingleVariant.class ? new RetainedValue<>() : null;
    }

    /** Render bits of the ore highlights that draw this border. */
    long mask() {
        return mask;
    }

    boolean connectable() {
        return singlePart != null;
    }

    /**
     * Sorts one base quad by cull face: {@code faces[0]} collects faces a quad covers as a whole
     * unit-cube face, {@code faces[1]} faces with any other quad; an unculled quad marks them all.
     */
    static void recordCubeFace(MutableQuadView quad, int[] faces) {
        Direction direction = quad.cullFace();
        if (direction == null) {
            faces[1] = 63;
        } else if (unitCubeFace(quad, direction)) {
            faces[0] |= 1 << direction.ordinal();
        } else {
            faces[1] |= 1 << direction.ordinal();
        }
    }

    /** {@code connectedFaces} is the bit set of faces that take connected tiles, or -1 when not connected. */
    void emit(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
            Predicate<@Nullable Direction> cullTest, int connectedFaces) {
        boolean connected = connectedFaces >= 0;
        BlockStateModel border = connected
                ? connectedOverlay.get(LightweightOverlayModels.CONNECTED_ORE_BORDER, MinecraftDecorationModel::lookup)
                : overlay.get(LightweightOverlayModels.ORE_BORDER, MinecraftDecorationModel::lookup);
        if (border == null) {
            return;
        }
        TextureAtlasSprite sprite = connected ? Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(CONNECTED_SPRITE) : null;
        BlockStateModelPart part = connected ? fixedPart() : null;
        emitter.pushTransform(quad -> {
            Direction direction = quad.cullFace();
            if (sprite != null && direction != null && (connectedFaces & (1 << direction.ordinal())) != 0) {
                int neighbors = connectedNeighbors(level, pos, state, direction, part);
                float uOffset = (sprite.getU1() - sprite.getU0()) * ResourceBorderPolicy.sheetColumn(neighbors)
                        / ResourceBorderPolicy.SHEET_TILES;
                float vOffset = (sprite.getV1() - sprite.getV0()) * ResourceBorderPolicy.sheetRow(neighbors)
                        / ResourceBorderPolicy.SHEET_TILES;
                for (int vertex = 0; vertex < 4; vertex++) {
                    quad.uv(vertex, quad.u(vertex) + uOffset, quad.v(vertex) + vOffset);
                }
            }
            quad.chunkLayer(ChunkSectionLayer.CUTOUT);
            quad.multiplyColor(tint);
            quad.emissive(true);
            MinecraftQuadLighting.disableDirectionalShade(quad);
            quad.ambientOcclusion(TriState.FALSE);
            return true;
        });
        try {
            border.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    @Nullable BlockStateModelPart fixedPart() {
        return singlePart == null ? null : singlePart.get(wrapped, model -> {
            var parts = new ArrayList<BlockStateModelPart>(1);
            model.collectParts(RandomSource.create(0), parts);
            // The exact SingleVariant class checked above guarantees one immutable baked part.
            return parts.getFirst();
        });
    }

    private static boolean unitCubeFace(MutableQuadView quad, Direction direction) {
        int side = MinecraftDecorationModel.face(direction).ordinal();
        int corners = 0;
        for (int vertex = 0; vertex < 4; vertex++) {
            int corner = ResourceBorderPolicy.unitCubeCorner(side, quad.x(vertex), quad.y(vertex), quad.z(vertex));
            if (corner < 0) {
                return false;
            }
            corners |= 1 << corner;
        }
        return corners == 15;
    }

    /**
     * Neighbor bits of {@code face}: a neighbor counts when it is the same block, its own face is
     * not covered, and its model shares this model's baked part.
     */
    private static int connectedNeighbors(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face,
            @Nullable BlockStateModelPart part) {
        int side = MinecraftDecorationModel.face(face).ordinal();
        var neighbor = new BlockPos.MutableBlockPos();
        var front = new BlockPos.MutableBlockPos();
        int bits = 0;
        for (int bit = 0; bit < 8; bit++) {
            neighbor.set(pos.getX() + ResourceBorderPolicy.neighborOffset(side, bit, 0),
                    pos.getY() + ResourceBorderPolicy.neighborOffset(side, bit, 1),
                    pos.getZ() + ResourceBorderPolicy.neighborOffset(side, bit, 2));
            var adjacent = level.getBlockState(neighbor);
            front.set(neighbor).move(face);
            if (part != null && adjacent.getBlock() == state.getBlock()
                    && !level.getBlockState(front).isSolidRender()) {
                var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
                var candidate = MinecraftOreExtensions.model(models.get(adjacent), adjacent);
                if (candidate instanceof MinecraftDecorationModel decoration && decoration.fixedOrePart() == part) {
                    bits |= 1 << bit;
                }
            }
        }
        return bits;
    }
}
