#!/usr/bin/env python3
"""Collect synthetic connected-border frames; source packs and material hashes stay private."""
from __future__ import annotations
import argparse
import hashlib
from pathlib import Path
import shutil
import struct

VANILLA = tuple(name for material in ("coal", "copper", "diamond", "emerald", "gold", "iron", "lapis", "redstone")
                for name in (material+"_ore", "deepslate_"+material+"_ore")) + ("nether_gold_ore", "nether_quartz_ore")
ROLES = tuple("geometry-"+str(i) for i in range(6)) + tuple("geometry-reverse-"+str(i) for i in range(1,5)) + tuple(
    "topology-"+name for name in ("isolated", "pair", "square", "l", "diagonal", "concave", "ring")) + ("covered-neighbor",) + tuple(
    "vanilla-"+name for name in VANILLA) + ("lit-redstone_ore", "lit-deepslate_redstone_ore", "chunk-boundary", "external-model")
IMAGES = tuple(f"{role}-{stage}.png" for role in ROLES for stage in ("air", "off", "on", "restored")) + tuple(
    name+".png" for name in ("ui-connection-draft", "ui-connection-applied", "ui-connection-save-failed", "chunk-neighbor-before",
    "chunk-neighbor-removed", "chunk-neighbor-added", "neighbor-before", "neighbor-removed", "neighbor-added", "connected-reloaded",
    "independent-off", "independent-connection-only", "filter-visible", "filter-hidden", "all-features", "all-features-air", "all-features-off"))
CHECKS = ("actualEmission", "actualUv", "geometryFallback", "collisionSymmetry", "topology", "coveredNeighbor", "litStates",
          "chunkBoundary", "neighborUpdates", "independent", "reload", "offRestored", "uiSave", "uiDiscard", "failedSave",
          "filterComposition", "allFeatures", "externalModel")

def verify(directory: Path, minecraft: str) -> list[Path]:
    from verify_external_compatibility import properties, require_value
    if (directory/"DIAGNOSTIC_STATUS.txt").exists():
        raise ValueError("Failure diagnostics are not acceptance evidence")
    if minecraft not in ("26.1.2", "26.2", "26.3") or directory.absolute()!=directory.resolve() or not directory.is_dir():
        raise ValueError("Connected evidence requires a supported version and regular directory")
    manifest=directory/"connected-manifest.txt"
    fields=properties(manifest,16384)
    if set(fields)!=set(CHECKS)|{"minecraft","roles","vanillaVariants"}:
        raise ValueError("Connected fields differ from the bounded approved schema")
    for key,value in {"minecraft":minecraft,"roles":"40","vanillaVariants":"18",**dict.fromkeys(CHECKS,"true")}.items():
        require_value(fields,key,value)
    files=[manifest]
    for name in IMAGES:
        path=directory/name
        if path.is_symlink() or not path.is_file() or not 1000<path.stat().st_size<=4*1024*1024:
            raise ValueError("Connected frame is missing or exceeds bounds")
        with path.open("rb") as stream:header=stream.read(24)
        if len(header)!=24 or header[:8]!=b"\x89PNG\r\n\x1a\n" or header[12:16]!=b"IHDR" or struct.unpack(">II",header[16:24])!=(640,360):
            raise ValueError("Connected frame is not a640x360 PNG")
        files.append(path)
    return files

def collect(directory: Path,minecraft: str,destination: Path)->int:
    files=verify(directory,minecraft)
    if destination.exists() or destination.is_symlink() or destination.absolute()!=destination.resolve():
        raise ValueError("Use a new dedicated output directory without symlinks")
    destination.mkdir(parents=True)
    sums=[]
    for source in files:
        target=destination/source.name
        with source.open("rb") as incoming,target.open("xb") as outgoing:shutil.copyfileobj(incoming,outgoing)
        sums.append(hashlib.sha256(target.read_bytes()).hexdigest()+"  "+target.name)
    (destination/"SHA256SUMS.txt").write_text("\n".join(sorted(sums))+"\n",encoding="utf-8")
    return len(files)

if __name__=="__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory",type=Path,required=True);parser.add_argument("--minecraft",required=True)
    parser.add_argument("--destination",type=Path,required=True);args=parser.parse_args()
    print(f"Verified connected row minecraft={args.minecraft} files={collect(args.directory,args.minecraft,args.destination)}")
