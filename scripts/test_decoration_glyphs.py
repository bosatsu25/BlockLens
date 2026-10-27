"""Regression guard for the clean-room decoration decal glyphs."""
import re
import unittest

from generate_decoration_glyphs import GLYPHS, OUTPUT, ROOT, SIZE, png


class DecorationGlyphsTest(unittest.TestCase):
    def test_committed_glyphs_are_reproducible_from_the_generator(self):
        for name, pixel in GLYPHS.items():
            self.assertEqual(png(pixel), (OUTPUT / f"{name}.png").read_bytes(), name)

    def test_generator_matches_the_java_glyph_catalog(self):
        source = (ROOT / "common/src/main/java/dev/blocklens/core/render/DecorationGlyph.java").read_text()
        body = source[source.index("public enum DecorationGlyph {"):source.index("public static final int PIXELS")]
        names = {name.lower() for name in re.findall(r"^\s+([A-Z_]+)[,;]", body, re.MULTILINE)}
        self.assertEqual(set(GLYPHS), names)
        self.assertIn(f"PIXELS = {SIZE};", source)

    def test_arrow_points_to_row_zero_and_is_mirror_symmetric(self):
        arrow = GLYPHS["arrow"]
        tip = min(y for y in range(SIZE) for x in range(SIZE) if arrow(x, y)[3] and arrow(x, y)[0] == 255)
        self.assertLess(tip, 6)
        for y in range(SIZE):
            for x in range(SIZE // 2):
                self.assertEqual(arrow(x, y), arrow(SIZE - 1 - x, y))


if __name__ == "__main__":
    unittest.main()
