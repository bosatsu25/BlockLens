#!/usr/bin/env python3
"""Validate actual external-renderer evidence and collect only approved result files."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import shutil
import struct
import zipfile

from prepare_external_compatibility import read_lock, validate_pack
from verify_simplified_evidence import IMAGES as SIMPLIFIED_IMAGES, CHECKS as SIMPLIFIED_CHECKS, verify as verify_simplified
from verify_palette_objects import IMAGES as OBJECT_IMAGES, CHECKS as OBJECT_CHECKS, verify as verify_objects

RENDER_PROFILES = ("shader-off", "shader-on", "vulkan", "packs")
PALETTE_IMAGES = tuple("palette-" + name + ".png" for name in (
    "off", "on", "on-reloaded", "off-restored", "all-features", "external-priority"))
PALETTE_CHECKS = ("actualAtlas", "standaloneDecode", "uiSave", "uiDiscard", "failedSave",
                  "allFeatures", "reload", "offRestored", "externalPriority", "stableWoodChroma")
COMPACT_ITEMS = ("white_concrete", "apple", "shield", "white_bed", "white_banner", "trident")
COMPACT_IMAGES = tuple(f"{item}-{stage}.png" for item in COMPACT_ITEMS for stage in ("off", "on", "restored")) + (
    "shield-using.png", "trident-using.png", "white_concrete-third-person.png", "white_concrete-gui.png",
    "apple-third-person.png", "apple-gui.png", "all-features.png", "all-features-reloaded.png",
    "external-priority.png", "inherited-external-ground.png")
COMPACT_CHECKS = ("actualModelParser", "bothHands", "thirdPerson", "gui", "inheritedExternal", "independentGroups",
                  "stateExceptions", "uiSave", "uiDiscard", "failedSave", "allFeatures", "reload", "offRestored", "externalPriority")
DEFAULT_IMAGES = (
    "m3-all13-on.png", "m3-all13-reloaded.png", "m3-all13-off-active-pack.png",
    "m5-pack-off.png", "m5-pack-on.png", "m5-pack-air-control.png",
) + PALETTE_IMAGES + COMPACT_IMAGES + SIMPLIFIED_IMAGES + OBJECT_IMAGES
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
    *(f"pale-oak-visual/{name}" for name in (*PALETTE_IMAGES, "palette-manifest.txt")),
    *(f"pale-oak-objects/{name}" for name in (*OBJECT_IMAGES, "palette-objects-manifest.txt")),
    *(f"compact-model-visual/{name}" for name in (*COMPACT_IMAGES, "compact-manifest.txt")),
    *(f"simplified-visual/{name}" for name in (*SIMPLIFIED_IMAGES, "simplified-manifest.txt")),
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
        "minimumDifferencePixels": "80", "minimumRetentionPermille": "950",
        "minimumRestorationOverlapPermille": "990",
        "maximumShaderRestorationDifferentPixels": "200", "colorDelta": "24",
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
                "terrainHiddenAir", "terrainRestoration"):
        if bounded_number(native, key + "DifferentPixels", 101200) >= 80:
            raise ValueError(f"Sodium {key} image control differs")
    color_diff = bounded_number(native, "tagFilterRestorationDifferentPixels", 101200)
    before = bounded_number(native, "tagFilterRestorationExpectedForeground", 101200)
    after = bounded_number(native, "tagFilterRestorationRestoredForeground", 101200)
    overlap = bounded_number(native, "tagFilterRestorationSharedForeground", min(before, after))
    before_ratio = bounded_number(native, "tagFilterRestorationExpectedOverlapPermille", 1000)
    after_ratio = bounded_number(native, "tagFilterRestorationRestoredOverlapPermille", 1000)
    if (color_diff > 200 or before < 80 or after < 80 or
            before_ratio != overlap * 1000 // before or
            after_ratio != overlap * 1000 // after or
            min(before_ratio, after_ratio) < 990):
        raise ValueError("Sodium tag-filter restoration failed the bounded shader-aware geometry check")
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


def private_pack_probe_hashes(pack_input: Path | None, lock: dict) -> dict:
    """Read expected asset hashes locally; never add them to the public dependency lock."""
    if pack_input is None:
        raise ValueError("Pack evidence requires the private prepared pack input")
    if pack_input.absolute() != pack_input.resolve() or not pack_input.is_dir():
        raise ValueError("Private pack input must be a regular directory without symlinks")
    result = {path: {} for path in lock["pack_probes"]}
    for alias, expected in lock["packs"].items():
        archive = pack_input / "packs" / expected["filename"]
        if archive.absolute() != archive.resolve() or not archive.is_file():
            raise ValueError("Private pack input is missing or contains a symlink")
        validate_pack(archive, expected)
        with zipfile.ZipFile(archive) as source:
            for path, candidates in lock["pack_probes"].items():
                if alias not in candidates:
                    continue
                entry = source.getinfo("assets/minecraft/" + path)
                if entry.file_size > 4 * 1024 * 1024:
                    raise ValueError("Private pack resource exceeds the inspection bound")
                with source.open(entry) as stream:
                    data = stream.read(4 * 1024 * 1024 + 1)
                if len(data) != entry.file_size:
                    raise ValueError("Private pack resource size differs")
                result[path][alias] = hashlib.sha256(data).hexdigest()
    return result


def verify_pack_order(directory: Path, order: str, lock: dict, expected_probes: dict) -> list[Path]:
    """Require the byte-pinned complete resource set and actual native geometry metrics."""
    if order not in ("forward", "reverse"):
        raise ValueError("Unknown pack resource order")
    resource_file = directory / f"external-packs-{order}-resources.tsv"
    if resource_file.is_symlink() or not resource_file.is_file() or resource_file.stat().st_size > 16384:
        raise ValueError("The bounded pack resource evidence is missing")
    rows = {}
    for line in resource_file.read_text(encoding="utf-8").splitlines():
        fields = line.split("\t")
        if len(fields) != 3 or fields[0] in rows:
            raise ValueError("Malformed or duplicate pack resource evidence")
        rows[fields[0]] = fields[1:]
    if set(rows) != set(lock["pack_probes"]):
        raise ValueError("Incomplete pack resource evidence")
    aliases = list(lock["packs"])
    if order == "reverse":
        aliases.reverse()
    for path, candidates in lock["pack_probes"].items():
        winner = next(alias for alias in reversed(aliases) if alias in candidates)
        if rows[path] != [winner, expected_probes[path][winner]]:
            raise ValueError("The pack resource bytes or priority differ")
    visual_file = directory / f"external-packs-{order}-visual.txt"
    visual = properties(visual_file)
    for key, expected in {"maskThreshold": "24", "maskToleranceRadius": "2",
                          "minimumRetentionPermille": "950"}.items():
        require_value(visual, key, expected)
    foreground = bounded_number(visual, "baseForeground", 60200)
    retained = bounded_number(visual, "baseRetained", foreground)
    changed = bounded_number(visual, "changedPixels", 60200)
    ratio = bounded_number(visual, "retentionPermille", 1000)
    if foreground < 120 or changed < 80 or ratio < 950 or ratio != retained * 1000 // foreground:
        raise ValueError("The pack visual evidence fails geometry retention")
    return [resource_file, visual_file]


def verify_compact(directory: Path, minecraft: str) -> list[Path]:
    if (directory / "DIAGNOSTIC_STATUS.txt").exists():
        raise ValueError("Failure diagnostics are not acceptance evidence")
    if minecraft not in ("26.1.2", "26.2", "26.3") or directory.absolute() != directory.resolve() or not directory.is_dir():
        raise ValueError("Compact evidence requires a supported version and regular directory")
    manifest = directory / "compact-manifest.txt"
    native = properties(manifest, 16384)
    if set(native) != set(COMPACT_CHECKS) | {"minecraft", "modelRoles", "changedPixels"}:
        raise ValueError("Compact evidence fields differ from the approved bounded schema")
    require_value(native, "minecraft", minecraft)
    require_value(native, "modelRoles", "40" if minecraft == "26.1.2" else "41")
    for key in COMPACT_CHECKS:
        require_value(native, key, "true")
    if bounded_number(native, "changedPixels", 1075200) < 186:
        raise ValueError("Compact held frames lack the required rendered difference")
    files = [manifest]
    for name in COMPACT_IMAGES:
        path = directory / name
        if path.is_symlink() or not path.is_file() or not 1000 < path.stat().st_size <= 4 * 1024 * 1024:
            raise ValueError("Compact rendered image is missing or exceeds bounds")
        with path.open("rb") as stream:
            header = stream.read(24)
        if (len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR"
                or struct.unpack(">II", header[16:24]) != (640, 360)):
            raise ValueError("Compact image is not a 640 by 360 PNG")
        files.append(path)
    return files


def verify(directory: Path, minecraft: str, profile: str, pack_input: Path | None = None) -> list[Path]:
    if profile == "compact":
        return verify_compact(directory, minecraft)
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
    if profile == "packs":
        expected_probes = private_pack_probe_hashes(pack_input, lock)
        for order in ("Forward", "Reverse"):
            require_value(evidence, "packOrder" + order, "true")
        for alias, pack in lock["packs"].items():
            require_value(evidence, f"pack.{alias}.sha256", pack["sha256"])
            major = int(lock["resource_format"][minecraft].split(".")[0])
            require_value(evidence, f"pack.{alias}.formatDeclared",
                          str(pack["min_format"] <= major <= pack["max_format"]).lower())
        images = [f"external-packs-{order}-{state}.png" for order in ("forward", "reverse")
                  for state in ("off", "on", "air")]
        for order in ("forward", "reverse"):
            files += verify_pack_order(directory, order, lock, expected_probes)
        restoration = directory / "external-packs-restoration.txt"
        restored = properties(restoration)
        for key, expected in {"packCount": "6", "probesPerOrder": "29", "priorityChangedSources": "6",
                              "selectedPacksRestored": "true", "configRestored": "true"}.items():
            require_value(restored, key, expected)
        files.append(restoration)
    else:
        require_value(evidence, "m3Visual", "true")
        require_value(evidence, "m5ActivePackVisual", "true")
        require_value(properties(directory / "m5-pack-visual-manifest.txt"), "externalSceneIsolated", "true")
        require_value(evidence, "paleOakPalette", "true")
        palette_file = directory / "palette-manifest.txt"
        palette = properties(palette_file, 16384)
        require_value(palette, "minecraft", minecraft)
        for check in PALETTE_CHECKS:
            require_value(palette, check, "true")
        if bounded_number(palette, "changedPixels", 14250) <= 80:
            raise ValueError("Palette native wood ROI lacks a rendered difference")
        require_value(evidence, "paletteObjects", "true")
        object_files = verify_objects(directory, minecraft)
        files.append(object_files[0])
        require_value(evidence, "compactModels", "true")
        compact_files = verify_compact(directory, minecraft)
        require_value(evidence, "simplifiedTerrain", "true")
        simplified_files = verify_simplified(directory, minecraft)
        images = DEFAULT_IMAGES
        files.append(simplified_files[0])
        files.append(compact_files[0])
        files += [directory / "m3-visual-manifest.txt", directory / "m5-pack-visual-manifest.txt", palette_file]
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


def collect(directory: Path, minecraft: str, profile: str, destination: Path,
            pack_input: Path | None = None) -> int:
    files = verify(directory, minecraft, profile, pack_input)
    destination.mkdir(parents=True, exist_ok=True)
    expected_names = {path.name for path in files} | {"SHA256SUMS.txt"}
    if any(path.name not in expected_names for path in destination.iterdir()):
        raise ValueError("Use a dedicated result directory; extra files must not be published")
    sums = []
    for source in files:
        target = destination / source.name
        if target.is_symlink():
            raise ValueError("Result destination must not be a symlink")
        if profile == "packs" and source.name.endswith("-resources.tsv"):
            if source.resolve() == target.resolve():
                raise ValueError("Private pack proof and collected result must be separate directories")
            rows = source.read_text(encoding="utf-8").splitlines()
            target.write_text("".join("\t".join(row.split("\t")[:2]) + "\n" for row in rows),
                              encoding="utf-8")
        elif source.resolve() != target.resolve():
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
    parser.add_argument("--profile", required=True, choices=(*RENDER_PROFILES, "compact"))
    parser.add_argument("--destination", type=Path, required=True)
    parser.add_argument("--pack-input", type=Path,
                        help="Private prepared pack input; required only for packs acceptance")
    parser.add_argument("--failed-run", action="store_true",
                        help="Collect bounded partial frames for diagnosis; never accept this row")
    args = parser.parse_args()
    if args.failed_run:
        count = collect_failure(args.directory, args.destination)
        print(f"Collected failure diagnostics minecraft={args.minecraft} profile={args.profile} files={count}")
        return
    count = collect(args.directory, args.minecraft, args.profile, args.destination, args.pack_input)
    print(f"Verified external row minecraft={args.minecraft} profile={args.profile} files={count}")


if __name__ == "__main__":
    main()
