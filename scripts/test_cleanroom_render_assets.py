"""Regression guard for clean-room generated art and the six-face culling contract."""
import hashlib
import json
import struct
import unittest
import zlib

from verify_cleanroom_render_assets import MODEL, TEXTURE, generated_texture, verify


class CleanRoomRenderAssetsTest(unittest.TestCase):
    def test_checked_in_asset_is_reproducible_and_not_third_party_art(self):
        verify()
        self.assertEqual(generated_texture(), TEXTURE.read_bytes())
        self.assertEqual("e6b4a350f4733dc9e846f4a841f61c5c22d5fc7de0c6846247c7b2a3b4f07a7a",
                         hashlib.sha256(generated_texture()).hexdigest())

    def test_border_texture_has_transparent_center_and_emissive_white_edges(self):
        data = TEXTURE.read_bytes()
        self.assertEqual(bytes((137, 80, 78, 71, 13, 10, 26, 10)), data[:8])
        self.assertEqual((16, 16), struct.unpack(">II", data[16:24]))
        off = 8
        samples = bytearray()
        while off < len(data):
            length = struct.unpack_from(">I", data, off)[0]
            kind = data[off + 4:off + 8]
            chunk = data[off + 8:off + 8 + length]
            self.assertEqual(struct.unpack_from(">I", data, off + 8 + length)[0],
                             zlib.crc32(kind + chunk))
            if kind == b"IDAT":
                samples.extend(zlib.decompress(chunk))
            off += 12 + length
        width = 16 * 4 + 1
        def alpha(x, y):
            return samples[y * width + 1 + x * 4 + 3]
        self.assertEqual(255, alpha(0, 8))
        self.assertEqual(255, alpha(15, 8))
        self.assertEqual(255, alpha(8, 0))
        self.assertEqual(255, alpha(8, 15))
        self.assertEqual(0, alpha(2, 8))
        self.assertEqual(0, alpha(8, 8))

    def test_model_has_one_cullable_face_per_direction(self):
        model = json.loads(MODEL.read_text())
        self.assertEqual(1, len(model["elements"]))
        self.assertEqual(6, len(model["elements"][0]["faces"]))
        for face, options in model["elements"][0]["faces"].items():
            self.assertEqual(face, options["cullface"])
            self.assertEqual("#cue", options["texture"])
