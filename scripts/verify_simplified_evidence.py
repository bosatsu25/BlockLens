#!/usr/bin/env python3
"""Collect synthetic native frames only; input-pack material metadata stays private."""
from __future__ import annotations
import argparse
import hashlib
from pathlib import Path
import shutil
import struct

ROLES = ("stone", "oak_slab", "oak_leaves", "big_dripleaf", "big_dripleaf_stem", "small_dripleaf",
         "pointed_dripstone", "vine", "glow_lichen", "moss_carpet", "leaf_litter", "short_grass", "tall_grass", "bush")
IMAGES = tuple(f"{role}-{stage}.png" for role in ROLES for stage in ("off", "on", "air", "restored")) + (
    "simplified-reload-before.png", "simplified-reloaded.png", "simplified-filter-visible.png", "simplified-filtered.png", "simplified-air.png",
    "simplified-cue-before.png", "simplified-cue-on.png", "simplified-all-features.png", "simplified-external-off.png", "simplified-external-on.png",
    "simplified-external-model-off.png", "simplified-external-model-on.png",
    "simplified-external-texture-off.png", "simplified-external-texture-on.png")
CHECKS = ("actualEmission", "opaque", "sparse", "statesPreserved", "uiSave", "uiDiscard", "failedSave",
          "reload", "offRestored", "filterComposition", "cueComposition", "allFeatures", "externalPriority")

def verify(directory: Path, minecraft: str) -> list[Path]:
    from verify_external_compatibility import properties, require_value, bounded_number
    if (directory / "DIAGNOSTIC_STATUS.txt").exists():
        raise ValueError("Failure diagnostics are not acceptance evidence")
    if minecraft not in ("26.1.2", "26.2", "26.3") or directory.absolute() != directory.resolve() or not directory.is_dir():
        raise ValueError("Simplified evidence requires a supported version and regular directory")
    manifest = directory / "simplified-manifest.txt"
    fields = properties(manifest, 16384)
    if set(fields) != set(CHECKS) | {"minecraft", "roles", "changedPixels"}:
        raise ValueError("Simplified fields differ from the approved bounded schema")
    require_value(fields, "minecraft", minecraft)
    require_value(fields, "roles", "14")
    for key in CHECKS:
        require_value(fields, key, "true")
    if bounded_number(fields, "changedPixels", 145600) < 294:
        raise ValueError("Target frames lack the required rendered difference")
    files = [manifest]
    for name in IMAGES:
        path = directory / name
        if path.is_symlink() or not path.is_file() or not 1000 < path.stat().st_size <= 4 * 1024 * 1024:
            raise ValueError("Simplified rendered image is missing or exceeds bounds")
        with path.open("rb") as stream:
            header = stream.read(24)
        if (len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR"
                or struct.unpack(">II", header[16:24]) != (640, 360)):
            raise ValueError("Simplified image is not a 640 by 360 PNG")
        files.append(path)
    return files

def collect(directory: Path, minecraft: str, destination: Path) -> int:
    files = verify(directory, minecraft)
    if destination.exists() or destination.is_symlink() or destination.absolute() != destination.resolve():
        raise ValueError("Use a new dedicated output directory without symlinks")
    destination.mkdir(parents=True)
    sums = []
    for source in files:
        target = destination / source.name
        with source.open("rb") as incoming, target.open("xb") as outgoing:
            shutil.copyfileobj(incoming, outgoing)
        sums.append(hashlib.sha256(target.read_bytes()).hexdigest() + "  " + target.name)
    (destination / "SHA256SUMS.txt").write_text("\n".join(sorted(sums)) + "\n", encoding="utf-8")
    return len(files)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--minecraft", required=True)
    parser.add_argument("--destination", type=Path, required=True)
    args = parser.parse_args()
    print(f"Verified simplified row minecraft={args.minecraft} files={collect(args.directory, args.minecraft, args.destination)}")
