#!/usr/bin/env python3
"""Deterministic resource-pack ZIP metadata audit; never copies third-party assets."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import zipfile

SUFFIXES = (".rpo", ".png", ".json", ".properties", ".mcmeta")


def audit(path: Path) -> dict:
    if not path.is_file():
        raise FileNotFoundError(path)
    counts = {suffix: 0 for suffix in SUFFIXES}
    seen, duplicates = set(), set()
    entries = 0
    with zipfile.ZipFile(path) as archive:
        for info in archive.infolist():
            name = info.filename.replace("\\", "/")
            logical = PurePosixPath(name)
            if name.startswith("/") or ".." in logical.parts:
                raise ValueError(f"unsafe zip member: {name}")
            if info.is_dir():
                continue
            entries += 1
            if name in seen:
                duplicates.add(name)
            seen.add(name)
            for suffix in counts:
                if name.lower().endswith(suffix):
                    counts[suffix] += 1
                    break
    with path.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    return {"filename": path.name, "sha256": digest, "entries": entries,
            "types": counts, "duplicate_paths": sorted(duplicates),
            "note": "Metadata only: visual parity and redistribution rights not established"}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("packs", nargs="+", type=Path)
    args = parser.parse_args()
    print(json.dumps([audit(p) for p in args.packs], ensure_ascii=False, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
