package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Keeps the pack-level decal layer wired the same way on every supported Minecraft line: one
 * shared renderer, single-variant geometry only, tint fallback otherwise, and rendered proof.
 */
final class DecorationDecalContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void sharedRendererDrawsDecalsOnlyOverSingleVariantGeometryWithTintFallback() throws IOException {
        String model = Files.readString(SupportedVersionFixtures.rendererSource(root()));
        assertTrue(model.contains("boolean decalCapable = wrapped.getClass() == SingleVariant.class;"));
        assertTrue(model.contains("FaceDecalTable.compile(DecorationRenderDescriptor.of(capability, state))"));
        assertTrue(model.contains("long decalCapabilities = enabledCapabilities & decalMask;"));
        assertTrue(model.contains("applyNonNetherInstructions(quad, tintCapabilities)"));
        assertTrue(model.contains("emitDecals(emitter, level, pos, state, random, cullTest, decalCapabilities)"));
        assertTrue(model.contains("if (!spriteId.equals(sprite.contents().name()))"),
                "a missing glyph sprite must never draw the missing-texture checkerboard");
        assertTrue(model.contains("quad.chunkLayer(ChunkSectionLayer.CUTOUT);"));
        assertTrue(model.contains("DecalProjection.SURFACE_OFFSET"));
        assertFalse(model.contains("level.getBlockState(pos)"), "decals never inspect world state");
        for (String module : SupportedVersionFixtures.MODULES) {
            assertFalse(Files.exists(SupportedVersionFixtures.sourceRoot(root(), module)
                    .resolve("MinecraftDecorationModel.java")), module);
        }
    }

    @Test
    void glyphsAreGeneratedCleanRoomArtCheckedInCi() throws IOException {
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));
        String generator = Files.readString(root().resolve("scripts/generate_decoration_glyphs.py"));
        assertTrue(ci.contains("python3 scripts/generate_decoration_glyphs.py --check"));
        assertTrue(generator.contains("no third-party pixels"));
        String convention = Files.readString(root().resolve("gradle/version-module.gradle"));
        assertTrue(convention.contains("'assets/blocklens/textures/block/cue/arrow.png'"));
        assertTrue(convention.contains("'dev/blocklens/core/render/DecorationDecalPolicy.class'"));
    }

    @Test
    void everySupportedClientRendersTheDecalOrientationOracle() throws IOException {
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String oracle = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/DecorationDecalOracle.java"));
        assertTrue(smoke.contains("DecorationDecalOracle.verify(context, singleplayer);"));
        assertTrue(smoke.indexOf("M3VisualParityOracle.verify(context, singleplayer);")
                < smoke.indexOf("DecorationDecalOracle.verify(context, singleplayer);"));
        assertTrue(oracle.contains("{{\"north\", \"down\"}, {\"east\", \"left\"}, {\"south\", \"up\"}, {\"west\", \"right\"}}"));
        assertTrue(oracle.contains("bottom slab must not mark its upper face"));
        assertTrue(oracle.contains("dominantChannel()"));
    }
}
