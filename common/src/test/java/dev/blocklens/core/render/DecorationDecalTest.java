package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DecorationDecalTest {
    @Test
    void absentDecalCarriesNothing() {
        assertFalse(DecorationDecal.NONE.present());
        assertNull(DecorationDecal.NONE.glyph());
        assertEquals(0, DecorationDecal.NONE.quarterTurns());
        assertEquals(0, DecorationDecal.NONE.argb());
        assertThrows(IllegalArgumentException.class, () -> new DecorationDecal(null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DecorationDecal(null, 0, 0xFFFFFFFF));
    }

    @Test
    void glyphTexturePathsAreStableBlockAtlasSprites() {
        assertEquals("block/cue/inset_frame", DecorationGlyph.INSET_FRAME.texturePath());
        assertEquals("block/cue/edge_frame", DecorationGlyph.EDGE_FRAME.texturePath());
        assertEquals("block/cue/rails", DecorationGlyph.RAILS.texturePath());
        assertEquals("block/cue/arrow", DecorationGlyph.ARROW.texturePath());
        assertEquals("block/cue/seam_frame", DecorationGlyph.SEAM_FRAME.texturePath());
        assertEquals("block/cue/double_frame", DecorationGlyph.DOUBLE_FRAME.texturePath());
        assertEquals(32, DecorationGlyph.PIXELS);
    }

    @Test
    void presentDecalValidatesRotationAndOpacity() {
        for (int turns = 0; turns < 4; turns++) {
            var decal = DecorationDecal.of(DecorationGlyph.ARROW, turns, 0xFF102030);
            assertTrue(decal.present());
            assertEquals(turns, decal.quarterTurns());
            assertEquals(0xFF102030, decal.argb());
        }
        assertThrows(IllegalArgumentException.class, () -> DecorationDecal.of(DecorationGlyph.ARROW, -1, 0xFFFFFFFF));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecal.of(DecorationGlyph.ARROW, 4, 0xFFFFFFFF));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecal.of(DecorationGlyph.RAILS, 0, 0x80FFFFFF));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecal.of(DecorationGlyph.RAILS, 0, 0x00FFFFFF));
        assertThrows(NullPointerException.class, () -> DecorationDecal.of(null, 0, 0xFFFFFFFF));
    }
}
