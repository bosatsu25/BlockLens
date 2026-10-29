package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.render.DecorationDecals;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
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
        String layer = Files.readString(SupportedVersionFixtures.sharedSource(root(), "MinecraftDecalLayer.java"));
        assertTrue(model.contains("boolean decalCapable = wrapped.getClass() == SingleVariant.class;"));
        assertTrue(model.contains("DecorationDecals.table(DecorationRenderDescriptor.of(capability, state))"));
        assertTrue(model.contains("long decalCapabilities = decalLayer == null ? 0L : enabledCapabilities & decalLayer.mask();"));
        assertTrue(model.contains("long tintCapabilities = enabledCapabilities & ~decalCapabilities;"));
        assertTrue(model.contains("applyNonNetherInstructions(quad, tintCapabilities)"));
        assertTrue(model.contains("decalLayer.emit(this, emitter, level, pos, state, random, cullTest, decalCapabilities)"));
        assertTrue(layer.contains("if (!DECAL_SPRITE.equals(sprite.contents().name()))"),
                "a missing glyph sprite must never draw the missing-texture checkerboard");
        assertTrue(layer.contains("quad.chunkLayer(ChunkSectionLayer.CUTOUT);"));
        // The glyph mapping and the surface lift are DecorationDecals.layout, unit-tested in common.
        assertTrue(layer.contains("DecorationDecals.layout(f, decal, positions,"));
        // The decal geometry is the wrapped model's own, emitted once with nothing culled.
        assertTrue(layer.contains("model.emitWrapped(source, level, pos, state, random, direction -> false);"));
        assertFalse(layer.contains("getBlockState("), "decals never inspect world state");
        assertFalse(layer.contains("emissive(") || layer.contains("disableDirectionalShade")
                || layer.contains("ambientOcclusion("), "decals keep the base face's lighting, as pack textures do");
    }

    @Test
    void glyphsAreGeneratedCleanRoomArtCheckedInCi() throws IOException {
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));
        String generator = Files.readString(root().resolve("scripts/generate_decoration_glyphs.py"));
        assertTrue(ci.contains("python3 scripts/generate_decoration_glyphs.py --check"));
        assertTrue(generator.contains("no third-party pixels"));
        String convention = Files.readString(root().resolve("gradle/version-module.gradle"));
        assertTrue(convention.contains("'assets/blocklens/textures/block/cue/decal_glyphs.png'"));
        assertTrue(convention.contains("'dev/blocklens/core/render/DecorationDecals.class'"));
    }

    @Test
    void glyphStripIsOneOriginalTintableSpriteWithTheExpectedCells() throws IOException {
        Path directory = root().resolve("common/src/main/resources/assets/blocklens/textures/block/cue");
        try (var files = Files.list(directory)) {
            assertEquals(List.of("decal_glyphs.png"), files.map(path -> path.getFileName().toString()).toList());
        }
        BufferedImage strip = ImageIO.read(directory.resolve("decal_glyphs.png").toFile());
        int cell = DecorationDecals.GLYPH_PIXELS;
        assertEquals(cell, strip.getWidth());
        assertEquals(cell * DecorationDecals.GLYPHS, strip.getHeight());
        for (int y = 0; y < strip.getHeight(); y++) {
            for (int x = 0; x < cell; x++) {
                int argb = strip.getRGB(x, y);
                boolean arrow = y / cell == DecorationDecals.ARROW;
                assertTrue(argb == 0xFFFFFFFF || (argb >>> 24) == 0 || (arrow && argb == 0xFF202020),
                        "only tintable white, transparency and the arrow contour at " + x + "," + y);
                assertEquals(strip.getRGB(x, y), strip.getRGB(cell - 1 - x, y), "glyphs are mirror symmetric");
            }
        }
        int rails = DecorationDecals.RAILS * cell;
        for (int y = 0; y < cell; y++) {
            for (int x = 0; x < cell; x++) {
                assertEquals(x <= 1 || x >= cell - 2, (strip.getRGB(x, rails + y) >>> 24) != 0, "rails run along glyph up");
            }
        }
        int arrow = DecorationDecals.ARROW * cell;
        assertTrue(white(strip, arrow + 14) > 2 * white(strip, arrow + 24), "arrow head is wider than its shaft");
        assertTrue(white(strip, arrow + 3) > 0 && white(strip, arrow + 1) == 0, "arrow tip sits near glyph up");
        int seam = DecorationDecals.SEAM_FRAME * cell;
        assertTrue(white(strip, seam + 15) == cell && white(strip, seam + 13) == 4, "seam crosses the middle");
    }

    private static int white(BufferedImage image, int y) {
        int count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            if (image.getRGB(x, y) == 0xFFFFFFFF) count++;
        }
        return count;
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
