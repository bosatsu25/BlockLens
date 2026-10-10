#!/usr/bin/env python3
"""Prepare byte-pinned optional dependencies for isolated, opt-in client tests.

No resource pack or third-party mod is copied into the BlockLens runtime JAR.
Uploaded packs are discovered by their recorded content hash, never uploaded.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request
import zipfile

LOCK_PATH = Path(__file__).with_name("external_compatibility_dependencies.json")
PROFILES = ("shader-off", "shader-on", "vulkan", "packs", "masa")
MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024
MAX_PACK_EXPANDED_BYTES = 64 * 1024 * 1024


def read_lock() -> dict:
    lock = json.loads(LOCK_PATH.read_text(encoding="utf-8"))
    if lock.get("schema") != 1:
        raise ValueError("Unsupported dependency lock schema")
    return lock


def check_bytes(data: bytes, expected: dict) -> None:
    if len(data) != expected["size"]:
        raise ValueError("Dependency size differs from the pinned release")
    for algorithm in ("sha256", "sha512"):
        if algorithm in expected:
            actual = hashlib.new(algorithm, data).hexdigest()
            if actual != expected[algorithm]:
                raise ValueError(f"Dependency {algorithm} differs from the pinned release")


def download(expected: dict, destination: Path) -> Path:
    if destination.is_symlink():
        raise ValueError("A dependency destination must not be a symlink")
    if destination.is_file():
        check_bytes(destination.read_bytes(), expected)
        return destination
    if not expected["url"].startswith("https://cdn.modrinth.com/data/"):
        raise ValueError("Only the pinned official Modrinth CDN is allowed")
    if not 0 < expected["size"] <= MAX_DOWNLOAD_BYTES:
        raise ValueError("Dependency exceeds the bounded download size")
    request = urllib.request.Request(
        expected["url"], headers={"User-Agent": "BlockLens-Compatibility/1.0"}
    )
    with urllib.request.urlopen(request, timeout=45) as response:
        data = response.read(expected["size"] + 1)
    check_bytes(data, expected)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


def validate_pack(path: Path, expected: dict) -> None:
    if path.is_symlink() or path.stat().st_size > MAX_DOWNLOAD_BYTES:
        raise ValueError("Pack input must be a bounded regular file")
    check_bytes(path.read_bytes(), expected)
    with zipfile.ZipFile(path) as archive:
        entries = archive.infolist()
        if len(entries) > 10000 or sum(e.file_size for e in entries) > MAX_PACK_EXPANDED_BYTES:
            raise ValueError("Pack archive exceeds the bounded inspection size")
        if archive.getinfo("pack.mcmeta").file_size > 65536:
            raise ValueError("Pack metadata is unexpectedly large")


def locate_packs(pack_directory: Path, lock: dict) -> dict[str, Path]:
    files = sorted(pack_directory.glob("*.zip"))
    if len(files) > 64:
        raise ValueError("Use a dedicated directory containing the reference packs")
    by_hash = {}
    for path in files:
        if path.is_file() and not path.is_symlink() and path.stat().st_size <= MAX_DOWNLOAD_BYTES:
            by_hash[hashlib.sha256(path.read_bytes()).hexdigest()] = path
    result = {}
    for alias, expected in lock["packs"].items():
        path = by_hash.get(expected["sha256"])
        if path is None:
            raise ValueError(f"The byte-pinned {alias} reference pack is unavailable")
        validate_pack(path, expected)
        result[alias] = path
    return result


def write_properties(path: Path, values: dict) -> None:
    # All keys/values come from the checked-in lock or fixed enumerations.
    text = "".join(f"{key}={value}\n" for key, value in values.items())
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.is_symlink():
        raise ValueError("An isolated configuration must not be a symlink")
    path.write_text(text, encoding="utf-8")


def prepare(minecraft: str, profile: str, destination: Path,
            pack_directory: Path | None = None) -> dict:
    lock = read_lock()
    if minecraft not in lock["targets"] or profile not in PROFILES:
        raise ValueError("Unknown Minecraft version or compatibility profile")
    if profile == "vulkan" and minecraft == "26.1.2":
        raise ValueError("The native Vulkan track covers Minecraft 26.2 and 26.3")
    destination = destination.resolve()
    destination.mkdir(parents=True, exist_ok=True)
    manifest = {
        "schema": "1", "minecraft": minecraft, "profile": profile,
        "expectedBackend": "vulkan" if profile == "vulkan" else "opengl",
        "shaderExpected": str(profile == "shader-on").lower(),
        "resourceFormat": lock["resource_format"][minecraft],
    }
    selected_mods = ("iris", "sodium") if profile.startswith("shader-") else (
        ("litematica", "malilib") if profile == "masa" else ()
    )
    mods_dir = destination / "mods"
    mods_dir.mkdir(exist_ok=True)
    expected_names = {lock["targets"][minecraft][key]["filename"] for key in selected_mods}
    if any(path.name not in expected_names for path in mods_dir.glob("*.jar")):
        raise ValueError("Dependency directory contains another profile; use a fresh destination")
    for key in selected_mods:
        artifact = lock["targets"][minecraft][key]
        download(artifact, mods_dir / artifact["filename"])
        manifest[f"mod.{key}.versionId"] = artifact["version_id"]
        manifest[f"mod.{key}.sha256"] = artifact["sha256"]
    if profile.startswith("shader-"):
        shader = lock["shader"]
        target = download(shader, destination / "shaderpacks" / shader["filename"])
        write_properties(target.with_name(target.name + ".txt"), shader["options"])
        write_properties(destination / "config" / "iris.properties", {
            "enableShaders": str(profile == "shader-on").lower(),
            "shaderPack": shader["filename"],
            "enableDebugOptions": "false", "disableUpdateMessage": "true",
        })
        manifest["shaderPack"] = shader["filename"]
        manifest["shaderProfile"] = shader["profile"]
        manifest["shaderSha256"] = shader["sha256"]
        for name, value in shader["options"].items():
            manifest[f"shaderOption.{name}"] = value
    if profile == "packs":
        if pack_directory is None:
            raise ValueError("The packs profile requires a private reference-pack directory")
        packs = locate_packs(pack_directory, lock)
        target_dir = destination / "packs"
        target_dir.mkdir(exist_ok=True)
        manifest["packs"] = ",".join(packs)
        major = int(manifest["resourceFormat"].split(".")[0])
        for alias, source in packs.items():
            expected = lock["packs"][alias]
            target = target_dir / expected["filename"]
            if target.is_symlink():
                raise ValueError("An isolated pack destination must not be a symlink")
            shutil.copyfile(source, target)
            manifest[f"pack.{alias}.sha256"] = expected["sha256"]
            manifest[f"pack.{alias}.formatDeclared"] = str(
                expected["min_format"] <= major <= expected["max_format"]
            ).lower()
    write_properties(destination / "manifest.properties", manifest)
    return {"directory": str(destination), "minecraft": minecraft, "profile": profile,
            "dependency_count": len(selected_mods)}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft", required=True, choices=("26.1.2", "26.2", "26.3"))
    parser.add_argument("--profile", required=True, choices=PROFILES)
    parser.add_argument("--destination", type=Path, required=True)
    parser.add_argument("--pack-dir", type=Path)
    args = parser.parse_args()
    result = prepare(args.minecraft, args.profile, args.destination, args.pack_dir)
    print(json.dumps(result))


if __name__ == "__main__":
    main()
