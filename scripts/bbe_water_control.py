"""Original, test-only background control: isolate cue motion from water and vanilla kelp animation."""
import hashlib
import json
from pathlib import Path
import struct
import zlib


def _chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)


def _water_png(width: int) -> bytes:
    # Authored constant RGBA input, not copied Minecraft/third-party artwork.
    # Transparent water retains the fluid geometry while its color is constant in both controls.
    rows = (b'\0' + bytes((120, 120, 120, 96)) * width) * width
    return (b'\x89PNG\r\n\x1a\n'
            + _chunk(b'IHDR', struct.pack('>IIBBBBB', width, width, 8, 6, 0, 0, 0))
            + _chunk(b'IDAT', zlib.compress(rows, 9)) + _chunk(b'IEND', b''))


def write_water_control(root: Path) -> dict[str, str]:
    if root.exists() and any(root.iterdir()):
        raise FileExistsError('test water control destination must be empty')
    root.mkdir(parents=True, exist_ok=True)
    files = {'pack.mcmeta': (json.dumps({'pack': {
        'description': 'BlockLens test-only stationary water background for kelp animation controls',
        'min_format': 71, 'max_format': 2048}}, sort_keys=True) + '\n').encode('utf-8')}
    for name, width in (('water_still', 16), ('water_flow', 32)):
        prefix = 'assets/minecraft/textures/block/' + name + '.png'
        files[prefix] = _water_png(width)
        files[prefix + '.mcmeta'] = b'{}\n'
    # Metadata-only overrides retain the vanilla PNGs/models and alpha-cutoff contract.
    # Animated base alpha otherwise changes occlusion even when the overlay is static.
    # Repeat the same frame: 26.3 single-frame strip upload otherwise uses the full PNG height.
    metadata = {'animation': {'frames': [0, 0], 'frametime': 2, 'interpolate': False},
                'texture': {'alpha_cutoff_bias': 0.1}}
    for name in ('kelp', 'kelp_plant'):
        files['assets/minecraft/textures/block/' + name + '.png.mcmeta'] = (
            json.dumps(metadata, sort_keys=True) + '\n').encode('utf-8')
    hashes = {}
    for name, data in files.items():
        destination = root / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        with destination.open('xb') as stream:
            stream.write(data)
        hashes[name] = hashlib.sha256(data).hexdigest()
    return hashes
