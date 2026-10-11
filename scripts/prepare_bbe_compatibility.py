#!/usr/bin/env python3
"""Prepare exact public rendering dependencies, never upload the private source modpack."""
import argparse
import hashlib
import json
from pathlib import Path
import re
from urllib.parse import urlparse
from urllib.request import Request, urlopen

LOCK = Path(__file__).with_name("bbe_compatibility_dependencies.json")


def validated(data: bytes, record: dict) -> bytes:
    if len(data) != record["size"] or hashlib.sha512(data).hexdigest() != record["sha512"]:
        raise ValueError("dependency bytes do not match the pinned size/SHA-512")
    if hashlib.sha256(data).hexdigest() != record["sha256"]:
        raise ValueError("dependency SHA-256 differs from the inspected source JAR")
    return data


def selected_file(metadata: dict, record: dict) -> str:
    candidates = [f for f in metadata.get("files", [])
                  if f.get("hashes", {}).get("sha512") == record["sha512"]
                  and f.get("size") == record["size"]]
    if len(candidates) != 1:
        raise ValueError("public version metadata does not identify the exact pinned artifact")
    url = candidates[0].get("url", "")
    parsed = urlparse(url)
    if parsed.scheme != "https" or parsed.netloc != "cdn.modrinth.com":
        raise ValueError("dependency URL must use the official HTTPS CDN")
    return url


def read_public(url: str, limit: int) -> bytes:
    request = Request(url, headers={"User-Agent": "BlockLens-compatibility-ci/1.0"})
    with urlopen(request, timeout=60) as response:
        final = urlparse(response.geturl())
        if final.scheme != "https" or final.netloc not in {"api.modrinth.com", "cdn.modrinth.com"}:
            raise ValueError("unexpected dependency redirect")
        data = response.read(limit + 1)
    if len(data) > limit:
        raise ValueError("public dependency response exceeds its bounded size")
    return data


def prepare(destination: Path, stack: str, read=read_public) -> dict:
    if stack not in {"bbe-sodium", "cele-render"}:
        raise ValueError("unknown rendering stack")
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    records = [r for r in lock["mods"] if stack == "cele-render" or r["id"] in {"betterblockentities", "sodium"}]
    if destination.exists() and any(destination.iterdir()):
        raise ValueError("fixture destination must be empty; do not mix stale mods/configuration")
    mods = destination / "mods"
    config = destination / "config"
    mods.mkdir(parents=True, exist_ok=True)
    config.mkdir()
    for record in records:
        name = record["filename"]
        if Path(name).name != name or not name.endswith(".jar") or not re.fullmatch(r"[0-9a-f]{128}", record["sha512"]):
            raise ValueError("invalid dependency lock identity")
        metadata = json.loads(read("https://api.modrinth.com/v2/version_file/" + record["sha512"] + "?algorithm=sha512", 262144))
        url = selected_file(metadata, record)
        data = validated(read(url, record["size"]), record)
        (mods / name).write_bytes(data)
    options = {"optimize.master": True, "optimize.chest": True, "misc.shademode": 1,
               "animation.chest": True, "misc.christmas_chest": False, "misc.update_scheduler": 0}
    bbe = {"bbe.config.storage.main": [{"option": k, "value": v} for k, v in options.items()],
           "bbe.config.storage.experimental": []}
    (config / "BBEConfig.json").write_text(json.dumps(bbe, indent=2) + "\n", encoding="utf-8")
    (config / "iris.properties").write_text("enableShaders=false\n", encoding="utf-8")
    (config / "continuity.json").write_text('{"connected_textures":true,"emissive_textures":true}\n', encoding="utf-8")
    manifest = {"schema": 1, "minecraft": "26.3", "stack": stack, "mods": records,
                "private_modpack_uploaded": False, "shaders_enabled": False}
    (destination / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--stack", choices=["bbe-sodium", "cele-render"], required=True)
    args = parser.parse_args()
    prepare(args.destination, args.stack)


if __name__ == "__main__":
    main()
