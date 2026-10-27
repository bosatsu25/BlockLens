package dev.blocklens.core.render;

import static dev.blocklens.core.render.DecorationQuadFace.DOWN;
import static dev.blocklens.core.render.DecorationQuadFace.EAST;
import static dev.blocklens.core.render.DecorationQuadFace.NONE;
import static dev.blocklens.core.render.DecorationQuadFace.NORTH;
import static dev.blocklens.core.render.DecorationQuadFace.SOUTH;
import static dev.blocklens.core.render.DecorationQuadFace.UP;
import static dev.blocklens.core.render.DecorationQuadFace.WEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class DecalProjectionTest {
    private static final List<DecorationQuadFace> FACES = List.of(NORTH, EAST, SOUTH, WEST, UP, DOWN);

    @Test
    void viewerFramesMatchAnUprightPlayerLookingAtEachFace() {
        assertFrame(UP, NORTH, EAST);
        assertFrame(DOWN, SOUTH, EAST);
        assertFrame(SOUTH, UP, EAST);
        assertFrame(NORTH, UP, WEST);
        assertFrame(EAST, UP, NORTH);
        assertFrame(WEST, UP, SOUTH);
    }

    @Test
    void everyFrameIsRightHandedWithItsOutwardNormal() {
        for (DecorationQuadFace face : FACES) {
            int[] right = vector(DecalProjection.screenRight(face));
            int[] up = vector(DecalProjection.screenUp(face));
            int[] cross = {right[1] * up[2] - right[2] * up[1], right[2] * up[0] - right[0] * up[2],
                    right[0] * up[1] - right[1] * up[0]};
            assertEquals(List.of(cross[0], cross[1], cross[2]),
                    List.of(DecalProjection.normalX(face), DecalProjection.normalY(face), DecalProjection.normalZ(face)),
                    "right x up must be the outward normal (no mirrored glyphs) on " + face);
        }
    }

    @Test
    void coordinatesIncreaseTowardScreenRightAndScreenDown() {
        float x = 0.25F, y = 0.5F, z = 0.75F;
        for (DecorationQuadFace face : FACES) {
            int[] right = vector(DecalProjection.screenRight(face));
            int[] up = vector(DecalProjection.screenUp(face));
            float baseRight = DecalProjection.right(face, x, y, z);
            float baseDown = DecalProjection.down(face, x, y, z);
            assertEquals(baseRight + 1.0F, DecalProjection.right(face, x + right[0], y + right[1], z + right[2]), 1.0E-6F, face.name());
            assertEquals(baseDown, DecalProjection.down(face, x + right[0], y + right[1], z + right[2]), 1.0E-6F, face.name());
            assertEquals(baseDown - 1.0F, DecalProjection.down(face, x + up[0], y + up[1], z + up[2]), 1.0E-6F, face.name());
            assertEquals(baseRight, DecalProjection.right(face, x + up[0], y + up[1], z + up[2]), 1.0E-6F, face.name());
        }
    }

    @Test
    void quarterTurnsPointGlyphUpTowardEachInPlaneDirection() {
        for (DecorationQuadFace face : FACES) {
            DecorationQuadFace up = DecalProjection.screenUp(face);
            DecorationQuadFace right = DecalProjection.screenRight(face);
            assertEquals(0, DecalProjection.quarterTurnsToward(face, up));
            assertEquals(1, DecalProjection.quarterTurnsToward(face, right));
            assertEquals(2, DecalProjection.quarterTurnsToward(face, DecalProjection.opposite(up)));
            assertEquals(3, DecalProjection.quarterTurnsToward(face, DecalProjection.opposite(right)));
            assertEquals(-1, DecalProjection.quarterTurnsToward(face, face));
            assertEquals(-1, DecalProjection.quarterTurnsToward(face, DecalProjection.opposite(face)));
        }
        assertThrows(NullPointerException.class, () -> DecalProjection.quarterTurnsToward(UP, null));
    }

    @Test
    void glyphUpAppearsOnTheRotatedScreenEdge() {
        // Screen point where the glyph's top-center texel lands after k clockwise quarter turns.
        float[][] screen = {{0.5F, 0.0F}, {1.0F, 0.5F}, {0.5F, 1.0F}, {0.0F, 0.5F}};
        for (int turns = 0; turns < 4; turns++) {
            assertEquals(0.5F, DecalProjection.glyphU(turns, screen[turns][0], screen[turns][1]), 1.0E-6F);
            assertEquals(0.0F, DecalProjection.glyphV(turns, screen[turns][0], screen[turns][1]), 1.0E-6F);
        }
        // Glyph left-center lands one quarter turn earlier.
        float[][] left = {{0.0F, 0.5F}, {0.5F, 0.0F}, {1.0F, 0.5F}, {0.5F, 1.0F}};
        for (int turns = 0; turns < 4; turns++) {
            assertEquals(0.0F, DecalProjection.glyphU(turns, left[turns][0], left[turns][1]), 1.0E-6F);
            assertEquals(0.5F, DecalProjection.glyphV(turns, left[turns][0], left[turns][1]), 1.0E-6F);
        }
        assertEquals(0.2F, DecalProjection.glyphU(0, 0.2F, 0.7F), 1.0E-6F);
        assertEquals(0.7F, DecalProjection.glyphV(0, 0.2F, 0.7F), 1.0E-6F);
        assertEquals(0.7F, DecalProjection.glyphU(1, 0.2F, 0.7F), 1.0E-6F);
        assertEquals(0.8F, DecalProjection.glyphV(1, 0.2F, 0.7F), 1.0E-6F);
        assertEquals(0.8F, DecalProjection.glyphU(2, 0.2F, 0.7F), 1.0E-6F);
        assertEquals(0.3F, DecalProjection.glyphV(2, 0.2F, 0.7F), 1.0E-6F);
        assertEquals(0.3F, DecalProjection.glyphU(3, 0.2F, 0.7F), 1.0E-6F);
        assertEquals(0.2F, DecalProjection.glyphV(3, 0.2F, 0.7F), 1.0E-6F);
        for (int invalid : new int[] {-1, 4}) {
            assertThrows(IllegalArgumentException.class, () -> DecalProjection.glyphU(invalid, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> DecalProjection.glyphV(invalid, 0, 0));
        }
    }

    @Test
    void normalsAndOppositesAreExact() {
        assertEquals(List.of(1, 0, 0), normal(EAST));
        assertEquals(List.of(-1, 0, 0), normal(WEST));
        assertEquals(List.of(0, 1, 0), normal(UP));
        assertEquals(List.of(0, -1, 0), normal(DOWN));
        assertEquals(List.of(0, 0, 1), normal(SOUTH));
        assertEquals(List.of(0, 0, -1), normal(NORTH));
        assertEquals(SOUTH, DecalProjection.opposite(NORTH));
        assertEquals(NORTH, DecalProjection.opposite(SOUTH));
        assertEquals(WEST, DecalProjection.opposite(EAST));
        assertEquals(EAST, DecalProjection.opposite(WEST));
        assertEquals(DOWN, DecalProjection.opposite(UP));
        assertEquals(UP, DecalProjection.opposite(DOWN));
    }

    @Test
    void surfaceOffsetSitsAboveExistingOverlaysWithoutAVisibleGap() {
        assertTrue(DecalProjection.SURFACE_OFFSET > 0.03F / 16.0F, "above the Nether upper band");
        assertTrue(DecalProjection.SURFACE_OFFSET > 0.02F / 16.0F, "above ore borders");
        assertTrue(DecalProjection.SURFACE_OFFSET < 1.0F / 64.0F, "well below a quarter texel");
    }

    @Test
    void absentFacesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> DecalProjection.screenUp(NONE));
        assertThrows(IllegalArgumentException.class, () -> DecalProjection.screenRight(NONE));
        assertThrows(IllegalArgumentException.class, () -> DecalProjection.right(NONE, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> DecalProjection.down(NONE, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> DecalProjection.normalX(NONE));
        assertThrows(IllegalArgumentException.class, () -> DecalProjection.normalY(NONE));
        assertThrows(IllegalArgumentException.class, () -> DecalProjection.normalZ(NONE));
        assertThrows(IllegalArgumentException.class, () -> DecalProjection.opposite(NONE));
        assertThrows(NullPointerException.class, () -> DecalProjection.screenUp(null));
    }

    private static void assertFrame(DecorationQuadFace face, DecorationQuadFace up, DecorationQuadFace right) {
        assertEquals(up, DecalProjection.screenUp(face), face + " screen up");
        assertEquals(right, DecalProjection.screenRight(face), face + " screen right");
    }

    private static List<Integer> normal(DecorationQuadFace face) {
        return List.of(DecalProjection.normalX(face), DecalProjection.normalY(face), DecalProjection.normalZ(face));
    }

    static int[] vector(DecorationQuadFace direction) {
        return new int[] {DecalProjection.normalX(direction), DecalProjection.normalY(direction),
                DecalProjection.normalZ(direction)};
    }
}
