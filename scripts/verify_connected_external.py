#!/usr/bin/env python3
"""Validate the focused connected ore suite plus actual pinned backend/shader identity."""
from __future__ import annotations
import argparse
import hashlib
from pathlib import Path
import shutil
from verify_connected_evidence import verify as verify_connected
from verify_external_compatibility import properties, require_value
from prepare_external_compatibility import read_lock

def verify(directory: Path,minecraft: str,profile: str)->list[Path]:
    lock=read_lock()
    if minecraft not in lock["targets"] or profile not in ("shader-off","shader-on","vulkan") or (minecraft=="26.1.2" and profile=="vulkan"):
        raise ValueError("Unsupported connected renderer matrix row")
    manifest=directory/"external-compatibility-manifest.properties"
    fields=properties(manifest,16384)
    expected={"schema":"1","minecraft":minecraft,"profile":profile,"status":"passed","fullGraph":"false",
              "packagedRuntime":"true","backendVerified":"true","shaderVerified":"true",
              "backend":"vulkan" if profile=="vulkan" else "opengl","shaderInUse":str(profile=="shader-on").lower(),
              "connectedOreOnly":"true","connectedOres":"true",
              "captureMode":"completed-native-frame" if (profile=="shader-on" or (minecraft=="26.1.2" and profile=="shader-off")) else "fabric-test-capture"}
    allowed=set(expected)|{"backendDevice","backendDriver"}
    if profile.startswith("shader-"):
        expected["shaderPackSha256"]=lock["shader"]["sha256"]
        for mod in ("iris","sodium"):
            expected[f"mod.{mod}.sha256"]=lock["targets"][minecraft][mod]["sha256"]
            # Friendly version is diagnostic; exact artifact bytes and runtime presence are authoritative.
            allowed.add(f"mod.{mod}.version")
    if profile=="shader-on":
        expected.update(shaderOptionsVerified="true",shaderPack=lock["shader"]["filename"],shaderProfile=lock["shader"]["profile"])
    allowed|=set(expected)
    if set(fields)!=allowed:raise ValueError("Connected renderer manifest contains missing or unapproved fields")
    for key,value in expected.items():require_value(fields,key,value)
    for key in allowed-set(expected):
        if not fields[key] or len(fields[key])>160:raise ValueError("Unbounded native diagnostic")
    return [manifest,*verify_connected(directory,minecraft)]

def collect(directory: Path,minecraft: str,profile: str,destination: Path)->int:
    files=verify(directory,minecraft,profile)
    if destination.exists() or destination.is_symlink() or destination.absolute()!=destination.resolve():
        raise ValueError("Use a new dedicated result directory")
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
    parser.add_argument("--profile",required=True);parser.add_argument("--destination",type=Path,required=True);args=parser.parse_args()
    print(f"Verified connected renderer minecraft={args.minecraft} profile={args.profile} files={collect(args.directory,args.minecraft,args.profile,args.destination)}")
