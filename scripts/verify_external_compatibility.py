#!/usr/bin/env python3
"""Validate actual external-renderer evidence and collect only approved result files."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import shutil
import struct

from prepare_external_compatibility import read_lock

RENDER_PROFILES = ("shader-off", "shader-on", "vulkan", "packs", "masa")
DEFAULT_IMAGES = (
    "m3-all13-on.png", "m3-all13-reloaded.png", "m3-all13-off-active-pack.png",
    "m5-pack-off.png", "m5-pack-on.png", "m5-pack-air-control.png",
)


def properties(path: Path) -> dict[str, str]:
    if not path.is_file() or path.stat().st_size > 65536:
        raise ValueError("The bounded evidence manifest is missing")
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if not separator or key in result:
            raise ValueError("Malformed or duplicate evidence field")
        result[key] = value
    return result


def require_value(evidence: dict, key: str, expected: str) -> None:
    if evidence.get(key) != expected:
        raise ValueError(f"Evidence {key} does not match the required {expected}")


def verify(directory: Path, minecraft: str, profile: str) -> list[Path]:
    lock = read_lock()
    if minecraft not in lock["targets"] or profile not in RENDER_PROFILES:
        raise ValueError("Unknown rendering evidence matrix row")
    if profile == "vulkan" and minecraft == "26.1.2":
        raise ValueError("26.1.2 is not a native Vulkan matrix row")
    manifest = directory / "external-compatibility-manifest.properties"
    evidence = properties(manifest)
    for key, expected in {
        "schema": "1", "minecraft": minecraft, "profile": profile, "status": "passed",
        "fullGraph": "false", "packagedRuntime": "true", "backendVerified": "true",
        "shaderVerified": "true", "backend": "vulkan" if profile == "vulkan" else "opengl",
        "shaderInUse": str(profile == "shader-on").lower(),
    }.items():
        require_value(evidence, key, expected)
    files = [manifest]
    if profile.startswith("shader-"):
        for mod in ("iris", "sodium"):
            require_value(evidence, f"mod.{mod}.sha256", lock["targets"][minecraft][mod]["sha256"])
        require_value(evidence, "shaderPackSha256", lock["shader"]["sha256"])
    if profile == "shader-on":
        require_value(evidence, "shaderOptionsVerified", "true")
        require_value(evidence, "shaderPack", lock["shader"]["filename"])
        require_value(evidence, "shaderProfile", lock["shader"]["profile"])
    if profile == "masa":
        for mod in ("litematica", "malilib"):
            require_value(evidence, f"mod.{mod}.sha256", lock["targets"][minecraft][mod]["sha256"])
        require_value(evidence, "schematicRead", "true")
        schematic_path = directory / "builder-schematic-manifest.txt"
        schematic = properties(schematic_path)
        require_value(schematic, "minecraft", minecraft)
        for key in ("pinnedVersions", "typedRead", "match", "stateDifference", "blockDifference",
                    "airUnavailable", "missingChunk", "unloadCleanup", "readonly"):
            require_value(schematic, key, "true")
        files.append(schematic_path)
        images = ()
    elif profile == "packs":
        for order in ("Forward", "Reverse"):
            require_value(evidence, "packOrder" + order, "true")
        for alias, pack in lock["packs"].items():
            require_value(evidence, f"pack.{alias}.sha256", pack["sha256"])
            major = int(lock["resource_format"][minecraft].split(".")[0])
            require_value(evidence, f"pack.{alias}.formatDeclared",
                          str(pack["min_format"] <= major <= pack["max_format"]).lower())
        images = [f"external-packs-{order}-{state}.png" for order in ("forward", "reverse")
                  for state in ("off", "on", "air")]
        files += [directory / f"external-packs-{order}-{kind}" for order in ("forward", "reverse")
                  for kind in ("resources.tsv", "visual.txt")]
        restoration = directory / "external-packs-restoration.txt"
        restored = properties(restoration)
        for key, expected in {"packCount": "5", "probesPerOrder": "8", "priorityChangedSources": "2",
                              "selectedPacksRestored": "true", "configRestored": "true"}.items():
            require_value(restored, key, expected)
        files.append(restoration)
    else:
        require_value(evidence, "m3Visual", "true")
        require_value(evidence, "m5ActivePackVisual", "true")
        images = DEFAULT_IMAGES
        files += [directory / "m3-visual-manifest.txt", directory / "m5-pack-visual-manifest.txt"]
    for name in images:
        path = directory / name
        if not path.is_file() or not 1000 < path.stat().st_size <= 4 * 1024 * 1024:
            raise ValueError("An expected rendered image is missing or exceeds bounds")
        with path.open("rb") as stream:
            header = stream.read(24)
        if header[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", header[16:24]) != (640, 360):
            raise ValueError("An expected image is not a 640 by 360 PNG")
        files.append(path)
    for path in files:
        if path.is_symlink() or not path.is_file() or path.stat().st_size > 4 * 1024 * 1024:
            raise ValueError("An expected result is missing, a symlink or oversized")
    return files


def collect(directory: Path, minecraft: str, profile: str, destination: Path) -> int:
    files = verify(directory, minecraft, profile)
    destination.mkdir(parents=True, exist_ok=True)
    expected_names = {path.name for path in files} | {"SHA256SUMS.txt"}
    if any(path.name not in expected_names for path in destination.iterdir()):
        raise ValueError("Use a dedicated result directory; extra files must not be published")
    sums = []
    for source in files:
        target = destination / source.name
        if target.is_symlink():
            raise ValueError("Result destination must not be a symlink")
        if source.resolve() != target.resolve():
            shutil.copyfile(source, target)
        sums.append(hashlib.sha256(target.read_bytes()).hexdigest() + "  " + target.name)
    (destination / "SHA256SUMS.txt").write_text("\n".join(sorted(sums)) + "\n", encoding="utf-8")
    return len(files)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--minecraft", required=True, choices=("26.1.2", "26.2", "26.3"))
    parser.add_argument("--profile", required=True, choices=RENDER_PROFILES)
    parser.add_argument("--destination", type=Path, required=True)
    args = parser.parse_args()
    count = collect(args.directory, args.minecraft, args.profile, args.destination)
    print(f"Verified external row minecraft={args.minecraft} profile={args.profile} files={count}")


if __name__ == "__main__":
    main()
