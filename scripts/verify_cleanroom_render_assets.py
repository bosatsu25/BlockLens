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

def connected_pixels():
    pixels=bytearray(256*256*4)
    for mask in range(256):
        for y in range(16):
            for x in range(16):
                sides=(y<2,x>=14,y>=14,x<2)
                visible=any(edge and not mask&(1<<side) for side,edge in enumerate(sides))
                for side in range(4):
                    if sides[side] and sides[(side+1)%4]:
                        complete=(1<<side)|(1<<((side+1)%4))|(1<<(side+4))
                        visible |= mask&complete!=complete
                index=(((mask//16)*16+y)*256+(mask%16)*16+x)*4
                pixels[index:index+4]=bytes((255,255,255,255 if visible else 0))
    return bytes(pixels)

def generated_connected_texture():
    pixels=connected_pixels()
    rows=[]
    for y in range(256):
        row=bytearray((0,))
        for x in range(0,256,8):
            value=0
            for bit in range(8):
                if pixels[(y*256+x+bit)*4+3]:value|=1<<(7-bit)
            row.append(value)
        rows.append(row)
    ihdr=struct.pack(">IIBBBBB",256,256,1,3,0,0,0)
    return (b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",ihdr)
            +chunk(b"PLTE",bytes((255,255,255,255,255,255)))+chunk(b"tRNS",bytes((0,255)))
            +chunk(b"IDAT",zlib.compress(b"".join(rows),9))+chunk(b"IEND",b""))

def verify():
    assert TEXTURE.read_bytes() == generated_texture(), "ore-border PNG differs from generator"
    connected=TEXTURE.with_name("connected_ore_border.png")
    assert connected.read_bytes()==generated_connected_texture(), "connected border differs from generator"
    model = json.loads(MODEL.read_text(encoding="utf-8"))
    joined=json.loads(MODEL.with_name("connected_ore_border.json").read_text(encoding="utf-8"))
    expected=json.loads(json.dumps(model))
    expected["textures"]={key:"blocklens:block/lightweight/connected_ore_border" for key in ("cue","particle")}
    for face in expected["elements"][0]["faces"].values():face["uv"]=[0,0,1,1]
    assert joined==expected, "connected border model contract differs"
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
