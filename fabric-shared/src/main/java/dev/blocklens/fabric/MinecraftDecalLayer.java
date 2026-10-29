package dev.blocklens.fabric;

import dev.blocklens.core.render.DecorationDecals;
import dev.blocklens.core.render.DecorationQuadFace;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
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
 * Decal quads of one single-variant decoration model: each quad of the wrapped geometry gets the
 * face's clean-room glyph mapped onto its own bounds, lifted just above the surface. The quads are
 * encoded once per (enabled decal cues, glyph sprite) and grouped by cull face, so culled faces
 * stay culled exactly as when the model itself is emitted with the same cull test.
 */
final class MinecraftDecalLayer {
    private static final Identifier DECAL_SPRITE = Identifier.fromNamespaceAndPath("blocklens", DecorationDecals.SPRITE);
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int UNCULLED = DIRECTIONS.length;

    // Indexed like the owning model's capabilities; tables are interned per (capability, state).
    private final long[] capabilityBits;
    private final long[] @Nullable [] tables;
    private final long mask;

    // Decal quads depend only on the fixed geometry, the enabled decal cues and the glyph sprite, so
    // they are encoded once and replayed per block instead of re-emitting and re-mapping the model.
    private volatile @Nullable DecalMeshes decalMeshes;

    MinecraftDecalLayer(long[] capabilityBits, long[] @Nullable [] tables, long mask) {
        this.capabilityBits = capabilityBits;
        this.tables = tables;
        this.mask = mask;
    }

    /** Render bits of the capabilities drawn as decals instead of tints. */
    long mask() {
        return mask;
    }

    void emit(MinecraftDecorationModel model, QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest, long decalCapabilities) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(DECAL_SPRITE);
        if (!DECAL_SPRITE.equals(sprite.contents().name())) {
            return; // Never draw the missing-texture checkerboard over a block.
        }
        DecalMeshes meshes = decalMeshes;
        // The sprite identity check rebuilds after any atlas change instead of keeping stale UVs.
        if (meshes == null || meshes.capabilities() != decalCapabilities || meshes.sprite() != sprite) {
            meshes = encode(model, level, pos, state, random, sprite, decalCapabilities);
            decalMeshes = meshes;
        }
        Mesh[] byCullFace = meshes.byCullFace();
        for (int i = 0; i < byCullFace.length; i++) {
            Mesh mesh = byCullFace[i];
            if (mesh != null && !cullTest.test(i == UNCULLED ? null : DIRECTIONS[i])) {
                mesh.outputTo(emitter);
            }
        }
    }

    private DecalMeshes encode(MinecraftDecorationModel model, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, TextureAtlasSprite sprite, long decalCapabilities) {
        MutableMesh[] groups = new MutableMesh[UNCULLED + 1];
        QuadEmitter[] emitters = new QuadEmitter[UNCULLED + 1];
        QuadEmitter source = Renderer.get().mutableMesh().emitter();
        float[] positions = new float[12];
        float[] uvs = new float[8];
        source.pushTransform(quad -> {
            if (applyDecal(quad, sprite, decalCapabilities, positions, uvs)) {
                Direction cullFace = quad.cullFace();
                int group = cullFace == null ? UNCULLED : cullFace.ordinal();
                if (groups[group] == null) {
                    groups[group] = Renderer.get().mutableMesh();
                    emitters[group] = groups[group].emitter();
                }
                emitters[group].copyFrom(quad).emit();
            }
            return false; // Only the grouped copies are kept.
        });
        try {
            model.emitWrapped(source, level, pos, state, random, direction -> false);
        } finally {
            source.popTransform();
        }
        Mesh[] byCullFace = new Mesh[UNCULLED + 1];
        for (int i = 0; i < groups.length; i++) {
            byCullFace[i] = groups[i] == null ? null : groups[i].immutableCopy();
        }
        return new DecalMeshes(decalCapabilities, sprite, byCullFace);
    }

    private record DecalMeshes(long capabilities, TextureAtlasSprite sprite, @Nullable Mesh[] byCullFace) {
    }

    private boolean applyDecal(MutableQuadView quad, TextureAtlasSprite sprite, long decalCapabilities,
            float[] positions, float[] uvs) {
        DecorationQuadFace face = MinecraftDecorationModel.face(quad.nominalFace());
        if (face == DecorationQuadFace.NONE) {
            return false;
        }
        int f = face.ordinal();
        long decal = DecorationDecals.NONE;
        for (int i = 0; i < tables.length && decal == DecorationDecals.NONE; i++) {
            if (tables[i] != null && (decalCapabilities & capabilityBits[i]) != 0L) {
                decal = tables[i][f];
            }
        }
        if (decal == DecorationDecals.NONE) {
            return false;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            positions[3 * vertex] = quad.x(vertex);
            positions[3 * vertex + 1] = quad.y(vertex);
            positions[3 * vertex + 2] = quad.z(vertex);
        }
        if (!DecorationDecals.layout(f, decal, positions, sprite.getU0(), sprite.getU1(), sprite.getV0(),
                sprite.getV1(), uvs)) {
            return false;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            quad.uv(vertex, uvs[2 * vertex], uvs[2 * vertex + 1]);
            quad.pos(vertex, positions[3 * vertex], positions[3 * vertex + 1], positions[3 * vertex + 2]);
        }
        int argb = DecorationDecals.argb(decal);
        quad.color(argb, argb, argb, argb);
        quad.tintIndex(-1);
        quad.animated(false);
        quad.chunkLayer(ChunkSectionLayer.CUTOUT);
        // Keep the base face's lighting (block/sky light, directional shade, AO), as a texture in the
        // source pack would be lit: the decal darkens in darkness instead of glowing.
        return true;
    }
}
