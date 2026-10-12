#!/usr/bin/env python3
"""Audit independently generated terrain grain and sparse plant masks."""
import json
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
KINDS = ("grain", "short_grass", "tall_grass", "bush")

def pixel(kind, x, y):
    if kind == "grain":
        gray = 35 + (x * 7 + y * 11 + (x ^ y) * 3) % 15
        return (gray, gray, gray, 255)
    visible = ((7 <= x <= 10 and y >= 8) or (5 <= x <= 8 and 6 <= y <= 7)) if kind == "short_grass" else (
        x in (7, 8) if kind == "tall_grass" else ((4 <= x <= 11 and 10 <= y <= 13) or (6 <= x <= 11 and y == 9)))
    return (135, 135, 135, 255) if visible else (0, 0, 0, 0)

def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data))

def generated_texture(kind):
    rows = []
    for y in range(16):
        row = bytearray((0,))
        for x in range(16): row.extend(pixel(kind, x, y))
        rows.append(row)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB",16,16,8,6,0,0,0))
            + chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + chunk(b"IEND",b""))

def generated_model(kind):
    if kind != "grain":
        return {"parent":"minecraft:block/tinted_cross", "textures":{"cross":"blocklens:block/simplified/"+kind}}
    return {"textures":{"all":"blocklens:block/simplified/grain","particle":"blocklens:block/simplified/grain"},
            "elements":[{"from":[0,0,0],"to":[16,16,16],"faces":{
                direction:{"uv":[0,0,16,16],"texture":"#all","cullface":direction}
                for direction in ("down","up","north","south","west","east")}}]}

def verify():
    for kind in KINDS:
        texture=ROOT/"common/src/main/resources/assets/blocklens/textures/block/simplified"/(kind+".png")
        model=ROOT/"common/src/main/resources/assets/blocklens/models/block/simplified"/(("cube" if kind=="grain" else kind)+".json")
        assert texture.read_bytes()==generated_texture(kind), "procedural texture differs: "+kind
        assert json.loads(model.read_text(encoding="utf-8"))==generated_model(kind), "procedural geometry differs: "+kind
    assert [sum(pixel(kind,x,y)[3]>0 for y in range(16) for x in range(16)) for kind in KINDS]==[256,40,32,38]

if __name__=="__main__":
    verify()
    print("BLOCKLENS_SIMPLIFIED_ASSETS opaque=true sparse=true independentArtwork=true")