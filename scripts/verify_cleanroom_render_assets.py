#!/usr/bin/env python3
"""Reproduce the original ore-border PNG and audit its precise render contract."""
import json
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
TEXTURE = ROOT / "common/src/main/resources/assets/blocklens/textures/block/lightweight/ore_border.png"
MODEL = ROOT / "common/src/main/resources/assets/blocklens/models/block/lightweight/ore_border.json"

def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))

def generated_texture():
    rows = []
    for y in range(16):
        row = bytearray((0,))
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y) < 2
            row.extend((255, 255, 255, 255 if edge else 0))
        rows.append(row)
    ihdr = struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + chunk(b"IEND", b""))

def verify():
    assert TEXTURE.read_bytes() == generated_texture(), "ore-border PNG differs from generator"
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    assert model["ambientocclusion"] is False
    assert model["textures"]["cue"] == "blocklens:block/lightweight/ore_border"
    assert len(model["elements"]) == 1
    element = model["elements"][0]
    assert element["shade"] is False
    assert element["from"] == [-0.02] * 3
    assert element["to"] == [16.02] * 3
    expected = {"north", "south", "east", "west", "up", "down"}
    assert set(element["faces"]) == expected
    for face, details in element["faces"].items():
        assert details["cullface"] == face
        assert details["texture"] == "#cue"
        assert details["uv"] == [0, 0, 16, 16]

if __name__ == "__main__":
    verify()
    print("BLOCKLENS_CLEANROOM_BORDER exactPixels=true culling=true faceCount=6")
