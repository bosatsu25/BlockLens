#!/usr/bin/env python3
"""Generate or verify the original decoration decal glyph PNGs (no third-party pixels)."""
import argparse
from pathlib import Path
import struct
import sys
import zlib

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "common/src/main/resources/assets/blocklens/textures/block/cue"
SIZE = 32
WHITE = (255, 255, 255, 255)
CONTOUR = (32, 32, 32, 255)
CLEAR = (0, 0, 0, 0)


def ring(x, y):
    return min(x, y, SIZE - 1 - x, SIZE - 1 - y)


def arrow_fill(x, y):
    head = 3 <= y <= 15 and abs(x + 0.5 - SIZE / 2) <= (y - 3) + 0.5
    shaft = 12 <= x <= 19 and 15 <= y <= 28
    return head or shaft


def arrow(x, y):
    if arrow_fill(x, y):
        return WHITE
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if 0 <= x + dx < SIZE and 0 <= y + dy < SIZE and arrow_fill(x + dx, y + dy):
                return CONTOUR
    return CLEAR


GLYPHS = {
    "inset_frame": lambda x, y: WHITE if ring(x, y) in (2, 3) else CLEAR,
    "edge_frame": lambda x, y: WHITE if ring(x, y) <= 1 else CLEAR,
    "rails": lambda x, y: WHITE if x <= 1 or x >= SIZE - 2 else CLEAR,
    "arrow": arrow,
    "seam_frame": lambda x, y: WHITE if ring(x, y) <= 1 or y in (15, 16) else CLEAR,
    "double_frame": lambda x, y: WHITE if ring(x, y) <= 1 or ring(x, y) in (4, 5) else CLEAR,
}


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))


def png(pixel):
    rows = bytearray()
    for y in range(SIZE):
        rows.append(0)
        for x in range(SIZE):
            rows.extend(pixel(x, y))
    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(bytes(rows), 9)) + chunk(b"IEND", b""))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if committed PNGs differ")
    args = parser.parse_args()
    expected = {f"{name}.png": png(pixel) for name, pixel in GLYPHS.items()}
    if args.check:
        present = {path.name for path in OUTPUT.glob("*.png")}
        if present != set(expected):
            sys.exit(f"glyph set differs: {sorted(present)} != {sorted(expected)}")
        for name, data in expected.items():
            if (OUTPUT / name).read_bytes() != data:
                sys.exit(f"{name} differs from generator")
        print(f"BLOCKLENS_DECORATION_GLYPHS exactPixels=true glyphs={len(expected)}")
        return
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name, data in expected.items():
        (OUTPUT / name).write_bytes(data)


if __name__ == "__main__":
    main()
