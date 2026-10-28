"""Regression guard for the structural twin used by the pack-vs-mod cost comparison."""
import gzip
import io
import json
import struct
import tempfile
import unittest
import zipfile
import zlib
from pathlib import Path

import pack_structure_twin as twin

ROOT = Path(__file__).resolve().parents[1]
PROFILE = ROOT / "scripts/source_pack_structure.jsonl.gz"
SOURCE_SHA256 = "070bc338936f89320408e7b78bb6f74856917f5944deecc1a1b52390f5363616"


def png(width, height, color=6, depth=8, palette=0, transparency=0, metadata=0):
    channels = {3: 1, 6: 4}[color]
    row = (width * channels * depth + 7) // 8
    pixels = b"".join(b"\x00" + bytes((x * 7 + y) % 3 for x in range(row)) for y in range(height))
    body = b"\x89PNG\r\n\x1a\n" + twin.png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, depth, color, 0, 0, 0))
    if palette:
        body += twin.png_chunk(b"PLTE", bytes(palette * 3))
    if transparency:
        body += twin.png_chunk(b"tRNS", bytes(transparency))
    if metadata:
        body += twin.png_chunk(b"iTXt", b"x" * metadata)
    return body + twin.png_chunk(b"IDAT", zlib.compress(pixels)) + twin.png_chunk(b"IEND", b"")


def source_pack():
    """A small pack with every construct the real source uses, including text that must not leak."""
    files = [
        ("assets/", None),
        ("assets/minecraft/", None),
        ("assets/minecraft/blockstates/", None),
        ("assets/minecraft/blockstates/oak_log.json", {"variants": {
            "axis=x": {"model": "minecraft:block/log/oak_x", "x": 90, "y": 90},
            "axis=y": {"model": "block/log/oak_y"},
            "axis=z": {"model": "minecraft:block/oak_log_horizontal", "x": 90}}}),
        ("assets/minecraft/blockstates/oak_log.json.rpo", {"condition": "deco.log"}),
        ("assets/minecraft/blockstates/tripwire.json", {"multipart": [
            {"apply": {"model": "minecraft:block/tripwire"}},
            {"when": {"north": "true"}, "apply": {"model": "minecraft:block/log/oak_y", "y": 90}}]}),
        ("assets/minecraft/models/block/log/", None),
        ("assets/minecraft/models/block/log/template.json", {
            "credit": "Made by Somebody Special", "format_version": "1.9.0", "ambientocclusion": False,
            "textures": {"side": "minecraft:block/log/stripe", "particle": "#side"},
            "elements": [
                {"name": "Body part", "from": [1, 2, 3], "to": [15, 14, 13], "shade": False,
                 "rotation": {"angle": 22.5, "axis": "y", "origin": [8, 4, 8], "rescale": True},
                 "faces": {"north": {"uv": [1, 2, 3, 4], "texture": "#side", "cullface": "north", "rotation": 90},
                           "up": {"uv": [0, 0, 8, 8], "texture": "#side", "tintindex": 0}}}]}),
        ("assets/minecraft/models/block/log/oak_x.json", {
            "credit": "Made by Somebody", "parent": "minecraft:block/log/template",
            "textures": {"side": "minecraft:block/log/stripe", "end": "minecraft:block/oak_log_top"}}),
        ("assets/minecraft/models/block/log/oak_x.json.rpo", {"condition": "deco.log"}),
        ("assets/minecraft/models/block/log/oak_y.json", {
            "parent": "block/cube_column", "textures": {"side": "block/log/animated", "end": "block/log/stripe"}}),
        ("assets/minecraft/models/item/oak_log.json", {
            "parent": "minecraft:block/log/oak_y",
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.6, 0.6, 0.6]}}}),
        ("assets/minecraft/textures/block/log/", None),
        ("assets/minecraft/textures/block/log/stripe.png", png(32, 32, metadata=40)),
        ("assets/minecraft/textures/block/log/stripe.png.rpo", {"condition": "deco.log"}),
        ("assets/minecraft/textures/block/log/animated.png", png(16, 48, color=3, depth=4, palette=5, transparency=2)),
        ("assets/minecraft/textures/block/log/animated.png.mcmeta",
         {"animation": {"interpolate": True, "frametime": 10, "frames": [0, {"index": 1, "time": 4}, 2]}}),
        ("assets/minecraft/textures/block/log/desktop.ini", b"[LocalizedFileNames]\nstripe.png=Pretty name\n"),
        # Flat names replace the vanilla resource of the same name instead of adding one.
        ("assets/minecraft/textures/block/obsidian.png", png(16, 16)),
        ("assets/minecraft/textures/block/obsidian.png.rpo", {"condition": "gaming.obsidian"}),
        ("assets/minecraft/models/block/magma_block.json", {"parent": "block/cube_all",
                                                             "textures": {"all": "block/obsidian"}}),
        ("assets/minecraft/lang/en_us.json", {"rpo.pack.title": "Somebody's Tweaks", "rpo.pack.log": "Log"}),
        ("pack.mcmeta", {"pack": {"description": "Made by Somebody", "pack_format": 84,
                                  "supported_formats": {"min_inclusive": 64, "max_inclusive": 84}}}),
        ("pack.png", png(16, 16)),
        ("respackopts.json5", b'{ id: "somebodystweaks", conf: { deco: { log: false } } }'),
    ]
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, body in files:
            if body is None:
                archive.writestr(zipfile.ZipInfo(name), b"", compress_type=zipfile.ZIP_STORED)
            else:
                # Tab-indented like the source pack's editor output, so byte totals stay comparable.
                data = body if isinstance(body, bytes) else json.dumps(body, indent="\t").encode("utf-8")
                archive.writestr(name, data)
    return buffer.getvalue()


def masked(records):
    """Records without the fields a twin only approximates (IDAT bytes and translation sizes)."""
    result = []
    for record in records:
        record = json.loads(json.dumps(record))
        if "png" in record:
            record["png"][4] = None
        if record["k"] == "l":
            record["s"] = None
        result.append(record)
    return result


class PackStructureTwinTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        self.source = self.root / "source.zip"
        self.source.write_bytes(source_pack())

    def tearDown(self):
        self.directory.cleanup()

    def round_trip(self):
        header, records = twin.extract(self.source)
        profile = self.root / "profile.jsonl.gz"
        twin.write_profile(header, records, profile)
        out = self.root / "twin.zip"
        self.assertEqual(twin.main(["generate", "--profile", str(profile), "--out", str(out)]), 0)
        return header, records, profile, out

    def test_twin_reproduces_the_recorded_structure(self):
        header, records, profile, out = self.round_trip()
        twin_header, twin_records = twin.extract(out)
        self.assertEqual(masked(twin_records), masked(records))
        rows = twin.compare(header["signature"], twin_header["signature"])
        self.assertTrue(all(ok for *_, ok in rows), [row for row in rows if not row[3]])
        self.assertEqual(twin.main(["compare", "--profile", str(profile), "--pack", str(out)]), 0)

    def test_profile_keeps_structure_and_drops_names_text_and_coordinates(self):
        _, records = twin.extract(self.source)
        text = json.dumps(records)
        for leaked in ("Somebody", "stripe", "template", "animated", "Pretty", "1.9.0", "Body part", "22.5x"):
            self.assertNotIn(leaked, text)
        self.assertEqual(twin.unsafe_strings(records), [])
        template = next(r for r in records if r["k"] == "m" and "elements" in r["j"])["j"]
        element = template["elements"][0]
        self.assertEqual((element["from"], element["to"]), ([0, 0, 0], [16, 16, 16]))
        self.assertEqual(element["rotation"], {"angle": 22.5, "axis": "y", "origin": [8, 8, 8], "rescale": True})
        self.assertEqual(element["faces"]["north"], {"uv": [0, 0, 16, 16], "texture": "#side",
                                                     "cullface": "north", "rotation": 90})
        self.assertIs(template["ambientocclusion"], False)
        self.assertIs(element["shade"], False)
        state = next(r for r in records if r.get("n") == "oak_log")["j"]["variants"]
        self.assertEqual(state["axis=x"], {"model": "@m1", "x": 90, "y": 90})
        self.assertEqual(state["axis=y"], {"model": "@m2"})
        self.assertEqual(state["axis=z"], {"model": "minecraft:block/oak_log_horizontal", "x": 90})
        child = next(r for r in records if r["k"] == "m" and r["i"] == 1)["j"]
        self.assertEqual(child["parent"], "@m0")
        self.assertEqual(child["textures"], {"side": "@t0", "end": "minecraft:block/oak_log_top"})
        flat_texture = next(r for r in records if r["k"] == "t" and "n" in r)
        self.assertEqual((flat_texture["c"], flat_texture["n"]), ("block", "obsidian"))
        flat_model = next(r for r in records if r["k"] == "m" and r.get("n") == "magma_block")["j"]
        self.assertEqual(flat_model["textures"], {"all": "minecraft:block/obsidian"})
        item = next(r for r in records if r["k"] == "m" and r.get("n") == "oak_log")
        self.assertEqual((item["c"], item["j"]["parent"]), ("item", "@m2"))
        self.assertIn({"k": "r", "t": "f:assets/minecraft/textures/block/obsidian.png",
                       "j": {"condition": "gaming.obsidian"}, "z": 8}, records)
        animation = next(r for r in records if r["k"] == "a")
        self.assertEqual(animation["j"]["animation"]["frames"], [0, {"index": 1, "time": 4}, 2])

    def test_free_text_is_refused(self):
        self.assertEqual(twin.unsafe_strings({"textures": {"all": "minecraft:block/Fancy Name"}}),
                         [("/textures/all", "minecraft:block/Fancy Name")])
        self.assertEqual(twin.main(["extract", "--pack", str(self.source), "--profile", str(self.root / "p.gz"),
                                    "--sha256", "0" * 64]), 1)

    def test_synthetic_png_matches_geometry_and_is_well_formed(self):
        for layout in ([32, 32, 6, 8, 900, 40, 0, 0], [16, 48, 3, 4, 120, 0, 15, 2], [16, 16, 3, 8, 200, 3, 768, 0]):
            body = twin.synthetic_png(layout, 7)
            width, height, color, depth, idat, other, plte, trns = twin.png_layout(body)
            self.assertEqual((width, height, color, depth, plte, trns), tuple(layout[:4] + layout[6:]))
            self.assertLessEqual(abs(idat - layout[4]), max(64, layout[4] // 5))
            offset = 8
            pixels = b""
            while offset < len(body):
                length, tag = struct.unpack(">I4s", body[offset:offset + 8])
                chunk = body[offset + 8:offset + 8 + length]
                self.assertEqual(struct.unpack(">I", body[offset + 8 + length:offset + 12 + length])[0],
                                 zlib.crc32(tag + chunk))
                pixels += zlib.decompress(chunk) if tag == b"IDAT" else b""
                offset += 12 + length
            channels = {3: 1, 6: 4}[color]
            self.assertEqual(len(pixels), ((width * channels * depth + 7) // 8 + 1) * height)
            if color == 3:
                limit = min(plte // 3, 1 << depth)
                indices = [b >> s & ((1 << depth) - 1) for b in pixels for s in range(8 - depth, -1, -depth)]
                rows = [pixels[i:i + 1] for i in range(0, len(pixels), (width * depth + 7) // 8 + 1)]
                self.assertTrue(all(row == b"\x00" for row in rows))
                self.assertTrue(all(i < limit for i in indices[2:]))

    def test_generation_is_deterministic(self):
        _, _, profile, first = self.round_trip()
        second = self.root / "again.zip"
        twin.main(["generate", "--profile", str(profile), "--out", str(second)])
        self.assertEqual(first.read_bytes(), second.read_bytes())


class CommittedProfileTest(unittest.TestCase):
    def test_profile_is_the_pinned_source_without_text(self):
        header, records = twin.read_profile(PROFILE)
        self.assertEqual(header["sha256"], SOURCE_SHA256)
        self.assertEqual(twin.unsafe_strings(records), [])
        signature = header["signature"]
        self.assertEqual((signature["entries"], signature["directories"]), (4744, 207))
        self.assertEqual((signature["models"], signature["blockstates"], signature["textures"]), (2860, 274, 1019))
        self.assertEqual(signature["variantEntries"], 3398)
        with gzip.open(PROFILE, "rb") as stream:
            self.assertNotIn(b"credit\":\"M", stream.read())

    def test_twin_of_the_committed_profile_matches_the_source_structure(self):
        header, records = twin.read_profile(PROFILE)
        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory) / "twin.zip"
            twin.generate(header, records, out)
            with zipfile.ZipFile(out) as archive:
                rows = twin.compare(header["signature"], twin.signature_of(archive))
                names = archive.namelist()
        self.assertTrue(all(ok for *_, ok in rows), [row for row in rows if not row[3]])
        self.assertIn("assets/minecraft/textures/block/bltwin/t0.png", names)
        self.assertIn("pack.mcmeta", names)


if __name__ == "__main__":
    unittest.main()
