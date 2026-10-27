package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** The shipped glyph sprites are exactly the catalog, original and structurally what the policy assumes. */
final class DecorationGlyphAssetTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int CONTOUR = 0xFF202020;
    private static final Path ASSETS = Path.of(System.getProperty("blocklens.repoRoot"))
            .resolve("common/src/main/resources/assets/blocklens/textures");

    @Test
    void everyGlyphShipsAsOneSquareSpriteAndNothingElse() throws IOException {
        Set<String> expected = new TreeSet<>();
        for (DecorationGlyph glyph : DecorationGlyph.values()) {
            expected.add(glyph.texturePath() + ".png");
            BufferedImage image = read(glyph);
            assertEquals(DecorationGlyph.PIXELS, image.getWidth(), glyph.name());
            assertEquals(DecorationGlyph.PIXELS, image.getHeight(), glyph.name());
            assertTrue(Files.notExists(ASSETS.resolve(glyph.texturePath() + ".png.mcmeta")), "glyphs are static");
        }
        Set<String> actual = new TreeSet<>();
        try (var files = Files.list(ASSETS.resolve("block/cue"))) {
            files.forEach(file -> actual.add("block/cue/" + file.getFileName()));
        }
        assertEquals(expected, actual);
    }

    @Test
    void glyphsUseOnlyTintableWhiteAContourAndTransparency() throws IOException {
        for (DecorationGlyph glyph : DecorationGlyph.values()) {
            BufferedImage image = read(glyph);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    boolean allowed = argb == WHITE || (argb >>> 24) == 0
                            || (glyph == DecorationGlyph.ARROW && argb == CONTOUR);
                    assertTrue(allowed, glyph + " pixel " + x + "," + y + "=" + Integer.toHexString(argb));
                }
            }
        }
    }

    @Test
    void glyphShapesMatchThePolicyAssumptions() throws IOException {
        BufferedImage rails = read(DecorationGlyph.RAILS);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                boolean edge = x <= 1 || x >= 30;
                assertEquals(edge, opaque(rails, x, y), "rails run along glyph up at " + x + "," + y);
            }
        }
        BufferedImage inset = read(DecorationGlyph.INSET_FRAME);
        BufferedImage edge = read(DecorationGlyph.EDGE_FRAME);
        assertTrue(!opaque(inset, 0, 16) && opaque(inset, 2, 16) && opaque(inset, 16, 3) && !opaque(inset, 16, 16));
        assertTrue(opaque(edge, 0, 16) && opaque(edge, 31, 0) && !opaque(edge, 2, 16) && !opaque(edge, 16, 16));
        BufferedImage seam = read(DecorationGlyph.SEAM_FRAME);
        assertTrue(opaque(seam, 16, 15) && opaque(seam, 16, 16) && !opaque(seam, 16, 13) && opaque(seam, 0, 5));
        BufferedImage doubled = read(DecorationGlyph.DOUBLE_FRAME);
        assertTrue(opaque(doubled, 0, 16) && opaque(doubled, 4, 16) && !opaque(doubled, 2, 16) && !opaque(doubled, 16, 16));

        BufferedImage arrow = read(DecorationGlyph.ARROW);
        int tipRow = -1;
        for (int y = 0; y < 32 && tipRow < 0; y++) {
            if (whiteCount(arrow, y) > 0) tipRow = y;
        }
        assertTrue(tipRow >= 0 && tipRow < 6, "arrow tip must sit near glyph up");
        assertTrue(whiteCount(arrow, 14) > 2 * whiteCount(arrow, 24), "head must be wider than shaft");
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 16; x++) {
                assertEquals(arrow.getRGB(x, y), arrow.getRGB(31 - x, y), "arrow must be symmetric about its axis");
            }
        }
    }

    private static BufferedImage read(DecorationGlyph glyph) throws IOException {
        BufferedImage image = ImageIO.read(ASSETS.resolve(glyph.texturePath() + ".png").toFile());
        assertTrue(image != null, glyph.name());
        return image;
    }

    private static boolean opaque(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) != 0;
    }

    private static int whiteCount(BufferedImage image, int y) {
        int count = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            if (image.getRGB(x, y) == WHITE) count++;
        }
        return count;
    }
}
