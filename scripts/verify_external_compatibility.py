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
SODIUM_STAGES = (
    "air-control", "explicit-off", "explicit-on", "explicit-restored", "unsupported-off",
    "unsupported-on", "tagged-off", "tagged-on", "tag-removed", "tagged-filtered",
    "tagged-restored", "tagged-reloaded", "stone-off", "stone-hidden", "stone-restored",
)
SODIUM_IMAGES = tuple(f"sodium-{stage}.png" for stage in SODIUM_STAGES)
SODIUM_MANIFEST = "sodium-terrain-manifest.properties"
SODIUM_CHECKS = (
    "originalIndexUnchanged", "activeNonCubeResource", "explicitBaselineEmission",
    "explicitRetainedModelEmission", "explicitHighlight", "explicitRestoration", "explicitBaseGeometry",
    "unsupportedBaseEmission", "unsupportedControl", "tagAbsentBaseline", "tagRetainedModelEmission",
    "tagHighlight", "tagBaseGeometry", "tagRemovedBaseEmission", "tagRemoval", "tagPreHideEmission",
    "tagFilterEmissionSuppressed", "tagFilterAir", "tagPhysicalStatePreserved", "tagRestoredEmission",
    "tagFilterRestoration", "resourceReloadEmission", "resourceReloadHighlight", "terrainBaselineEmission",
    "terrainVisible", "terrainEmissionSuppressed", "terrainHiddenAir", "terrainPhysicalStatePreserved",
    "terrainRestoredEmission", "terrainRestoration",
)
FAILURE_FILES = (
    "external-compatibility/external-compatibility-manifest.properties",
    "external-compatibility/external-lifecycle-diagnostics.txt",
    "m3-visual/m3-all13-on.png", "m3-visual/m3-all13-reloaded.png",
    "m3-visual/m3-all13-off-active-pack.png", "m3-visual/m3-visual-manifest.txt",
    "m5-pack-visual/m5-pack-off.png", "m5-pack-visual/m5-pack-on.png",
    "m5-pack-visual/m5-pack-air-control.png", "m5-pack-visual/m5-pack-visual-manifest.txt",
    *(f"sodium-terrain/{name}" for name in (*SODIUM_IMAGES, SODIUM_MANIFEST)),
)


def properties(path: Path, maximum_bytes: int = 65536) -> dict[str, str]:
    if path.is_symlink() or not path.is_file() or path.stat().st_size > maximum_bytes:
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


def bounded_number(evidence: dict[str, str], key: str, maximum: int) -> int:
    value = evidence.get(key, "")
    try:
        number = int(value)
    except ValueError as exception:
        raise ValueError(f"Evidence {key} is not a bounded integer") from exception
    if str(number) != value or not 0 <= number <= maximum:
        raise ValueError(f"Evidence {key} is not a bounded integer")
    return number


def verify_sodium(directory: Path, minecraft: str, profile: str) -> Path:
    manifest = directory / SODIUM_MANIFEST
    native = properties(manifest, 16384)
    expected = {
        "schema": "1", "minecraft": minecraft, "status": "passed", "scope": "sodium-terrain",
        "sodiumVersion": "0.9.2+mc" + minecraft, "imageCount": "15", "failedChecks": "",
        "minimumDifferencePixels": "80", "minimumRetentionPermille": "950", "colorDelta": "24",
        "maskToleranceRadius": "2", "roiMinX": "90", "roiMaxX": "550", "roiMinY": "90",
        "roiMaxY": "310", "observationLimit": "4096", "stage": "stone-restored",
        "captureMode": "completed-native-frame" if minecraft == "26.1.2" and profile == "shader-off"
                       else "fabric-test-capture",
    }
    expected.update({key: "true" for key in SODIUM_CHECKS})
    for key, value in expected.items():
        require_value(native, key, value)
    wrapped = {"explicit-off", "explicit-on", "explicit-restored", "tagged-on", "tagged-filtered",
               "tagged-restored", "tagged-reloaded", "tag-pre-hide"}
    hidden = {"tagged-filtered", "stone-hidden"}
    for stage in (*SODIUM_STAGES, "tag-pre-hide"):
        for key, value in {"saturated": "false", "observationComplete": "true", "expectedEmission": "true",
                           "preparedReplacement": str(stage in wrapped).lower()}.items():
            require_value(native, stage + "." + key, value)
        visited, completed, emitted, selected = [bounded_number(native, stage + "." + key, 4095)
                for key in ("visited", "completed", "emitted", "expectedModel")]
        if completed != visited or not selected <= emitted <= completed:
            raise ValueError(f"Sodium {stage} observation is incomplete or inconsistent")
        if stage == "air-control":
            valid = visited == emitted == selected == 0
        elif stage in hidden:
            valid = completed > 0 and emitted == selected == 0
        else:
            valid = completed > 0 and emitted > 0 and selected == emitted
        if not valid:
            raise ValueError(f"Sodium {stage} lacks completed native model emission evidence")
    for key in ("explicitHighlight", "tagHighlight", "resourceReloadHighlight", "terrainVisible"):
        if bounded_number(native, key + "DifferentPixels", 101200) < 80:
            raise ValueError(f"Sodium {key} lacks the required image difference")
    for key in ("explicitRestoration", "unsupportedControl", "tagRemoval", "tagFilterAir",
                "tagFilterRestoration", "terrainHiddenAir", "terrainRestoration"):
        if bounded_number(native, key + "DifferentPixels", 101200) >= 80:
            raise ValueError(f"Sodium {key} image control differs")
    for key in ("explicitBase", "tagBase"):
        foreground = bounded_number(native, key + "Foreground", 101200)
        retained = bounded_number(native, key + "Retained", foreground)
        ratio = bounded_number(native, key + "RetentionPermille", 1000)
        if foreground < 80 or ratio < 950 or ratio != retained * 1000 // foreground:
            raise ValueError(f"Sodium {key} does not preserve base geometry")
    for name in SODIUM_IMAGES:
        image = directory / name
        if image.is_symlink() or not image.is_file() or not 1000 < image.stat().st_size <= 4 * 1024 * 1024:
            raise ValueError("Sodium rendered image is missing or exceeds bounds")
        require_value(native, name + ".sha256", hashlib.sha256(image.read_bytes()).hexdigest())
    return manifest


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
    if (directory / "DIAGNOSTIC_STATUS.txt").exists():
        raise ValueError("Failure diagnostics are not acceptance evidence")
    files = [manifest]
    if profile.startswith("shader-"):
        for mod in ("iris", "sodium"):
            require_value(evidence, f"mod.{mod}.sha256", lock["targets"][minecraft][mod]["sha256"])
        require_value(evidence, "shaderPackSha256", lock["shader"]["sha256"])
        require_value(evidence, "sodiumTerrain", "true")
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
        require_value(properties(directory / "m5-pack-visual-manifest.txt"), "externalSceneIsolated", "true")
        images = DEFAULT_IMAGES
        files += [directory / "m3-visual-manifest.txt", directory / "m5-pack-visual-manifest.txt"]
        if profile.startswith("shader-"):
            files.append(verify_sodium(directory, minecraft, profile))
            images += SODIUM_IMAGES
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


def collect_failure(game_directory: Path, destination: Path) -> int:
    """Keep partial oracle frames for diagnosis without validating or accepting the row."""
    files = []
    for name in FAILURE_FILES:
        source = game_directory / name
        if source.absolute() != source.resolve():
            raise ValueError("Failure diagnostic input must not contain a symlink")
        if not source.exists():
            continue
        limit = (4 * 1024 * 1024 if source.suffix == ".png"
                 else 16384 if source.name == SODIUM_MANIFEST else 65536)
        if not source.is_file() or source.stat().st_size > limit:
            raise ValueError("Failure diagnostic input is not a bounded regular file")
        if source.suffix == ".png":
            with source.open("rb") as stream:
                header = stream.read(24)
            if (len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n"
                    or header[12:16] != b"IHDR" or struct.unpack(">II", header[16:24]) != (640, 360)):
                raise ValueError("Failure diagnostic image is not a 640 by 360 PNG")
        files.append(source)
    if destination.absolute() != destination.resolve():
        raise ValueError("Failure diagnostic destination must not contain a symlink")
    destination.mkdir(parents=True, exist_ok=True)
    expected_names = {path.name for path in files} | {"SHA256SUMS.txt", "DIAGNOSTIC_STATUS.txt"}
    for path in destination.iterdir():
        if path.name not in expected_names:
            raise ValueError("Use a dedicated diagnostic directory; extra files must not be published")
        if path.is_symlink() or not path.is_file():
            raise ValueError("Failure diagnostic destination must be regular files, not symlinks")
    sums = []
    for source in files:
        target = destination / source.name
        if source.resolve() != target.resolve():
            shutil.copyfile(source, target)
        sums.append(hashlib.sha256(target.read_bytes()).hexdigest() + "  " + target.name)
    status = destination / "DIAGNOSTIC_STATUS.txt"
    status.write_text("status=failed-or-incomplete\nPartial failure diagnostics; not acceptance evidence.\n",
                      encoding="utf-8")
    sums.append(hashlib.sha256(status.read_bytes()).hexdigest() + "  " + status.name)
    (destination / "SHA256SUMS.txt").write_text("\n".join(sorted(sums)) + "\n", encoding="utf-8")
    return len(files)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", type=Path, required=True)
    parser.add_argument("--minecraft", required=True, choices=("26.1.2", "26.2", "26.3"))
    parser.add_argument("--profile", required=True, choices=RENDER_PROFILES)
    parser.add_argument("--destination", type=Path, required=True)
    parser.add_argument("--failed-run", action="store_true",
                        help="Collect bounded partial frames for diagnosis; never accept this row")
    args = parser.parse_args()
    if args.failed_run:
        count = collect_failure(args.directory, args.destination)
        print(f"Collected failure diagnostics minecraft={args.minecraft} profile={args.profile} files={count}")
        return
    count = collect(args.directory, args.minecraft, args.profile, args.destination)
    print(f"Verified external row minecraft={args.minecraft} profile={args.profile} files={count}")


if __name__ == "__main__":
    main()
