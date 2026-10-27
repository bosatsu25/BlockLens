"""Regression guard for the clean-room decoration decal glyph strip."""
import re
import unittest

from generate_decoration_glyphs import GLYPHS, OUTPUT, ROOT, SIZE, STRIP, png, strip_pixel


class DecorationGlyphsTest(unittest.TestCase):
    def test_committed_strip_is_reproducible_from_the_generator(self):
        self.assertEqual(png(strip_pixel, SIZE * len(GLYPHS)), (OUTPUT / STRIP).read_bytes())

    def test_generator_matches_the_java_glyph_ids(self):
        source = (ROOT / "common/src/main/java/dev/blocklens/core/render/DecorationDecals.java").read_text()
        ids = dict(re.findall(r"public static final int ([A-Z_]+) = (\d+);", source))
        for index, name in enumerate(GLYPHS):
            self.assertEqual(str(index), ids[name.upper()], name)
        self.assertEqual(str(len(GLYPHS)), ids["GLYPHS"])
        self.assertEqual(str(SIZE), ids["GLYPH_PIXELS"])
        self.assertIn(f'SPRITE = "block/cue/{STRIP[:-4]}"', source)

    def test_arrow_points_to_row_zero_and_is_mirror_symmetric(self):
        arrow = GLYPHS["arrow"]
        tip = min(y for y in range(SIZE) for x in range(SIZE) if arrow(x, y)[3] and arrow(x, y)[0] == 255)
        self.assertLess(tip, 6)
        for y in range(SIZE):
            for x in range(SIZE // 2):
                self.assertEqual(arrow(x, y), arrow(SIZE - 1 - x, y))


if __name__ == "__main__":
    unittest.main()
