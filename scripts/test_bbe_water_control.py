import hashlib
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zlib
from bbe_water_control import write_water_control


class WaterControlTest(unittest.TestCase):
    def test_original_static_transparent_water_is_deterministic(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            first = write_water_control(root / 'first')
            second = write_water_control(root / 'second')
            self.assertEqual(first, second)
            for name, digest in first.items():
                a = (root / 'first' / name).read_bytes()
                b = (root / 'second' / name).read_bytes()
                self.assertEqual(a, b)
                self.assertEqual(digest, hashlib.sha256(a).hexdigest())

    def test_control_freezes_only_water_and_never_replaces_kelp(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            files = write_water_control(root)
            self.assertEqual(len(files), 5)
            self.assertFalse(any('kelp' in name for name in files))
            for name in ('water_still', 'water_flow'):
                meta = root / 'assets/minecraft/textures/block' / (name + '.png.mcmeta')
                self.assertEqual({}, json.loads(meta.read_text()))

    def test_png_chunks_crc_single_frame_and_transparency(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_water_control(root)
            for name, width in (('water_still', 16), ('water_flow', 32)):
                data = (root / 'assets/minecraft/textures/block' / (name + '.png')).read_bytes()
                self.assertTrue(data.startswith(b'\x89PNG\r\n\x1a\n'))
                offset, kinds, compressed = 8, [], b''
                while offset < len(data):
                    length = struct.unpack('>I', data[offset:offset + 4])[0]
                    kind = data[offset + 4:offset + 8]
                    body = data[offset + 8:offset + 8 + length]
                    crc = struct.unpack('>I', data[offset + 8 + length:offset + 12 + length])[0]
                    self.assertEqual(crc, zlib.crc32(kind + body) & 0xffffffff)
                    kinds.append(kind)
                    if kind == b'IHDR':
                        self.assertEqual((width, width, 8, 6, 0, 0, 0), struct.unpack('>IIBBBBB', body))
                    if kind == b'IDAT':
                        compressed += body
                    offset += length + 12
                self.assertEqual([b'IHDR', b'IDAT', b'IEND'], kinds)
                self.assertEqual((b'\0' + bytes((120, 120, 120, 96)) * width) * width,
                                 zlib.decompress(compressed))

    def test_existing_pack_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            write_water_control(root)
            with self.assertRaises(FileExistsError):
                write_water_control(root)


if __name__ == '__main__':
    unittest.main()
