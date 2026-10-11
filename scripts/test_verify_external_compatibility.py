"""Regression checks against falsely accepting external rendering evidence."""

from pathlib import Path
import hashlib
import copy
import io
import zipfile
import struct
import tempfile
import unittest
from unittest.mock import patch
import zlib

import verify_external_compatibility as evidence


class ExternalEvidenceTest(unittest.TestCase):
    @staticmethod
    def base(profile="vulkan"):
        return {"schema": "1", "minecraft": "26.3", "profile": profile, "status": "passed",
                "fullGraph": "false", "packagedRuntime": "true", "backendVerified": "true",
                "shaderVerified": "true", "backend": "vulkan", "shaderInUse": "false"}

    def test_successful_opengl_fallback_is_not_vulkan_evidence(self):
        actual = self.base()
        actual["backend"] = "opengl"
        with patch.object(evidence, "properties", return_value=actual):
            with self.assertRaisesRegex(ValueError, "backend.*vulkan"):
                evidence.verify(Path("unused"), "26.3", "vulkan")

    def test_requested_shader_without_actual_activation_is_rejected(self):
        actual = self.base("shader-on")
        actual["backend"] = "opengl"
        with patch.object(evidence, "properties", return_value=actual):
            with self.assertRaisesRegex(ValueError, "shaderInUse.*true"):
                evidence.verify(Path("unused"), "26.3", "shader-on")

    def test_another_minecraft_version_cannot_supply_this_row(self):
        with patch.object(evidence, "properties", return_value=self.base()):
            with self.assertRaisesRegex(ValueError, "minecraft.*26.2"):
                evidence.verify(Path("unused"), "26.2", "vulkan")

    def test_started_partial_manifest_does_not_pass(self):
        actual = self.base()
        actual["status"] = "started"
        with patch.object(evidence, "properties", return_value=actual):
            with self.assertRaisesRegex(ValueError, "status.*passed"):
                evidence.verify(Path("unused"), "26.3", "vulkan")

    def rendering_fixture(self, directory, isolation):
        actual = self.base()
        actual.update(m3Visual="true", m5ActivePackVisual="true", paleOakPalette="true", compactModels="true", simplifiedTerrain="true")
        (directory / "external-compatibility-manifest.properties").write_text(
            "".join(f"{key}={value}\n" for key, value in actual.items()))
        (directory / "m3-visual-manifest.txt").write_text("minecraft=26.3\n")
        (directory / "m5-pack-visual-manifest.txt").write_text(
            "minecraft=26.3\n" + ("" if isolation is None else f"externalSceneIsolated={isolation}\n"))

        (directory / "palette-manifest.txt").write_text(
            "minecraft=26.3\nchangedPixels=100\n" + "".join(key + "=true\n" for key in evidence.PALETTE_CHECKS))

        def chunk(name, payload):
            return struct.pack(">I", len(payload)) + name + payload + struct.pack(">I", zlib.crc32(name + payload))

        row = b"\0" + bytes(range(256)) * 7 + bytes(range(128))
        png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 640, 360, 8, 2, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(row * 360)) + chunk(b"IEND", b""))
        (directory / "compact-manifest.txt").write_text("minecraft=26.3\nmodelRoles=41\nchangedPixels=186\n" + "".join(key + "=true\n" for key in evidence.COMPACT_CHECKS))
        (directory / "simplified-manifest.txt").write_text("minecraft=26.3\nroles=14\nchangedPixels=294\n" + "".join(key + "=true\n" for key in evidence.SIMPLIFIED_CHECKS))
        for name in evidence.DEFAULT_IMAGES:
            (directory / name).write_bytes(png)

    def test_nonisolated_or_unrecorded_m5_scene_is_rejected(self):
        for isolation in ("false", None):
            with self.subTest(isolation=isolation), tempfile.TemporaryDirectory() as tmp:
                directory = Path(tmp)
                self.rendering_fixture(directory, isolation)
                with self.assertRaisesRegex(ValueError, "externalSceneIsolated.*true"):
                    evidence.verify(directory, "26.3", "vulkan")

    def test_isolated_m5_and_palette_acceptance_keep_all_twelve_images(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            self.rendering_fixture(directory, "true")
            files = evidence.verify(directory, "26.3", "vulkan")
            self.assertEqual(116, len(files))
            self.assertTrue(set(evidence.DEFAULT_IMAGES).issubset(path.name for path in files))

    def test_palette_missing_or_failed_native_proof_cannot_pass_a_backend_row(self):
        for state in (None, "false"):
            with self.subTest(state=state), tempfile.TemporaryDirectory() as tmp:
                directory = Path(tmp)
                self.rendering_fixture(directory, "true")
                manifest = directory / "external-compatibility-manifest.properties"
                text = manifest.read_text().replace("paleOakPalette=true\n", "")
                if state is not None:
                    text += "paleOakPalette=" + state + "\n"
                manifest.write_text(text)
                with self.assertRaises(ValueError):
                    evidence.verify(directory, "26.3", "vulkan")

    def test_palette_declared_success_without_native_metrics_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            self.rendering_fixture(directory, "true")
            manifest = directory / "external-compatibility-manifest.properties"
            manifest.write_text(manifest.read_text().replace("paleOakPalette=true\n", "") + "paleOakPalette=true\n")
            (directory / "palette-manifest.txt").write_text("minecraft=26.3\nchangedPixels=0\n")
            with self.assertRaises(ValueError):
                evidence.verify(directory, "26.3", "vulkan")

    def test_palette_false_controls_and_invalid_roi_metrics_are_rejected(self):
        for line in ("uiSave=false", "failedSave=false", "allFeatures=false", "externalPriority=false",
                     "changedPixels=80", "changedPixels=14251", "changedPixels=-1", "changedPixels=0100"):
            with self.subTest(line=line), tempfile.TemporaryDirectory() as tmp:
                directory = Path(tmp)
                self.rendering_fixture(directory, "true")
                manifest = directory / "palette-manifest.txt"
                key, value = line.split("=")
                old = manifest.read_text()
                manifest.write_text("\n".join(line if row.startswith(key + "=") else row for row in old.splitlines()) + "\n")
                with self.assertRaises(ValueError):
                    evidence.verify(directory, "26.3", "vulkan")

    def pack_order_fixture(self, directory, order="forward"):
        lock = evidence.read_lock()
        aliases = list(lock["packs"])
        if order == "reverse":
            aliases.reverse()
        expected = {path: {alias: hashlib.sha256((path + alias).encode()).hexdigest()
                           for alias in candidates}
                    for path, candidates in lock["pack_probes"].items()}
        rows = []
        for path, candidates in lock["pack_probes"].items():
            winner = next(alias for alias in reversed(aliases) if alias in candidates)
            rows.append(f"{path}\t{winner}\t{expected[path][winner]}\n")
        (directory / f"external-packs-{order}-resources.tsv").write_text("".join(rows), encoding="utf-8")
        (directory / f"external-packs-{order}-visual.txt").write_text(
            "changedPixels=100\nbaseForeground=200\nbaseRetained=198\nretentionPermille=990\n"
            "maskThreshold=24\nmaskToleranceRadius=2\nminimumRetentionPermille=950\n", encoding="utf-8")
        return lock, expected

    def test_pack_order_requires_every_pinned_source_and_correct_priority(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            lock, expected = self.pack_order_fixture(directory)
            self.assertEqual(2, len(evidence.verify_pack_order(directory, "forward", lock, expected)))
            path = directory / "external-packs-forward-resources.tsv"
            valid = path.read_text()
            for invalid in ("", "\n".join(valid.splitlines()[:-1]), valid + valid.splitlines()[0] + "\n",
                            valid.replace("chise-server", "chise")):
                path.write_text(invalid)
                with self.assertRaisesRegex(ValueError, "pack resource"):
                    evidence.verify_pack_order(directory, "forward", lock, expected)
            path.write_text(valid)
            self.pack_order_fixture(directory, "reverse")
            self.assertEqual(2, len(evidence.verify_pack_order(directory, "reverse", lock, expected)))
            path.write_text((directory / "external-packs-reverse-resources.tsv").read_text())
            with self.assertRaisesRegex(ValueError, "pack resource"):
                evidence.verify_pack_order(directory, "forward", lock, expected)

    def test_pack_visual_counts_cannot_claim_failed_geometry_passed(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            lock, expected = self.pack_order_fixture(directory)
            path = directory / "external-packs-forward-visual.txt"
            valid = path.read_text()
            for old, new in (("changedPixels=100", "changedPixels=0"),
                             ("baseForeground=200", "baseForeground=0"),
                             ("baseRetained=198", "baseRetained=180"),
                             ("retentionPermille=990", "retentionPermille=949"),
                             ("maskThreshold=24", "maskThreshold=1")):
                path.write_text(valid.replace(old, new))
                with self.assertRaises(ValueError):
                    evidence.verify_pack_order(directory, "forward", lock, expected)

    def test_public_probe_catalog_does_not_contain_material_hashes(self):
        lock = evidence.read_lock()
        self.assertEqual(29, len(lock["pack_probes"]))
        for candidates in lock["pack_probes"].values():
            self.assertIsInstance(candidates, list)
            self.assertTrue(set(candidates).issubset(lock["packs"]))

    def test_private_pack_input_is_required_for_material_validation(self):
        with self.assertRaisesRegex(ValueError, "private prepared pack input"):
            evidence.private_pack_probe_hashes(None, evidence.read_lock())

    def test_private_material_hashes_derive_from_pinned_bytes_and_tampering_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            packs = directory / "packs"
            packs.mkdir()
            lock = copy.deepcopy(evidence.read_lock())
            expected = {path: {} for path in lock["pack_probes"]}
            for alias, pin in lock["packs"].items():
                output = io.BytesIO()
                with zipfile.ZipFile(output, "w") as archive:
                    archive.writestr("pack.mcmeta", '{"pack":{"pack_format":84,"description":"fixture"}}')
                    for path, candidates in lock["pack_probes"].items():
                        if alias in candidates:
                            content = (path + alias).encode()
                            archive.writestr("assets/minecraft/" + path, content)
                            expected[path][alias] = hashlib.sha256(content).hexdigest()
                data = output.getvalue()
                (packs / pin["filename"]).write_bytes(data)
                pin["size"] = len(data)
                pin["sha256"] = hashlib.sha256(data).hexdigest()
            self.assertEqual(expected, evidence.private_pack_probe_hashes(directory, lock))
            victim = packs / lock["packs"]["chise-server"]["filename"]
            victim.write_bytes(victim.read_bytes()[:-1])
            with self.assertRaisesRegex(ValueError, "size"):
                evidence.private_pack_probe_hashes(directory, lock)

    def test_collected_private_material_hashes_are_redacted(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "external-packs-forward-resources.tsv"
            material_hash = hashlib.sha256(b"private reference material").hexdigest()
            source.write_text(f"textures/block/example.png\tchise-server\t{material_hash}\n")
            destination = root / "public-result"
            with patch.object(evidence, "verify", return_value=[source]):
                evidence.collect(root, "26.3", "packs", destination)
            content = (destination / source.name).read_text()
            self.assertEqual("textures/block/example.png\tchise-server\n", content)
            self.assertNotIn(material_hash, content)
            self.assertIn(hashlib.sha256((destination / source.name).read_bytes()).hexdigest(),
                          (destination / "SHA256SUMS.txt").read_text())
            self.assertIn(material_hash, source.read_text())

    def test_duplicate_fields_are_not_last_write_wins(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "manifest"
            path.write_text("backend=vulkan\nbackend=opengl\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate"):
                evidence.properties(path)

    def test_unrelated_files_cannot_leak_into_a_collected_artifact(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "evidence"
            source.write_text("verified")
            destination = root / "output"
            destination.mkdir()
            (destination / "private-pack.zip").write_text("private")
            with patch.object(evidence, "verify", return_value=[source]):
                with self.assertRaisesRegex(ValueError, "extra files"):
                    evidence.collect(root, "26.3", "vulkan", destination)

    def test_failed_render_diagnostics_preserve_frames_without_becoming_pass_evidence(self):
        def chunk(name, payload):
            return struct.pack(">I", len(payload)) + name + payload + struct.pack(">I", zlib.crc32(name + payload))

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            game = root / "game"
            source = game / "m5-pack-visual"
            source.mkdir(parents=True)
            row = b"\0" + bytes(range(256)) * 7 + bytes(range(128))
            png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 640, 360, 8, 2, 0, 0, 0))
                   + chunk(b"IDAT", zlib.compress(row * 360)) + chunk(b"IEND", b""))
            (source / "m5-pack-off.png").write_bytes(png)
            (source / "m5-pack-visual-manifest.txt").write_text("baseRetentionPermille=862\n")
            (source / "private-pack.zip").write_bytes(b"private")
            manifest = game / "external-compatibility" / "external-compatibility-manifest.properties"
            manifest.parent.mkdir()
            actual = self.base("shader-on")
            actual.update(backend="opengl", shaderInUse="true", status="started")
            manifest.write_text("".join(f"{key}={value}\n" for key, value in actual.items()))
            destination = root / "diagnostics"
            self.assertEqual(3, evidence.collect_failure(game, destination))
            self.assertEqual(png, (destination / "m5-pack-off.png").read_bytes())
            self.assertFalse((destination / "private-pack.zip").exists())
            self.assertIn("not acceptance evidence", (destination / "DIAGNOSTIC_STATUS.txt").read_text())
            with self.assertRaisesRegex(ValueError, "status.*passed"):
                evidence.verify(destination, "26.3", "shader-on")

    def test_failed_render_diagnostics_reject_symlinks(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "game" / "m3-visual"
            source.mkdir(parents=True)
            private = root / "private"
            private.write_text("private")
            (source / "m3-visual-manifest.txt").symlink_to(private)
            with self.assertRaisesRegex(ValueError, "symlink"):
                evidence.collect_failure(root / "game", root / "diagnostics")

    def test_diagnostic_directory_cannot_supply_acceptance_even_with_passed_fields(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "DIAGNOSTIC_STATUS.txt").write_text("status=failed-or-incomplete\n")
            with patch.object(evidence, "properties", return_value=self.base()):
                with self.assertRaisesRegex(ValueError, "not acceptance evidence"):
                    evidence.verify(root, "26.3", "vulkan")

    def test_failed_external_lifecycle_trace_is_hashed_and_remains_incomplete(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "game" / "external-compatibility"
            source.mkdir(parents=True)
            trace = source / "external-lifecycle-diagnostics.txt"
            trace.write_text("status=diagnostic-only\nstalledPhase=world_close\n")
            manifest = source / "external-compatibility-manifest.properties"
            actual = self.base("shader-off")
            actual.update(backend="opengl", status="started")
            manifest.write_text("".join(f"{key}={value}\n" for key, value in actual.items()))
            (source / "external-lifecycle-diagnostics.tmp").write_text("partial private input")
            destination = root / "diagnostics"
            self.assertEqual(2, evidence.collect_failure(root / "game", destination))
            self.assertEqual(trace.read_bytes(), (destination / trace.name).read_bytes())
            self.assertFalse((destination / "external-lifecycle-diagnostics.tmp").exists())
            self.assertIn(hashlib.sha256(trace.read_bytes()).hexdigest() + "  " + trace.name,
                          (destination / "SHA256SUMS.txt").read_text())
            with self.assertRaisesRegex(ValueError, "status.*passed"):
                evidence.verify(destination, "26.3", "shader-off")

    def test_failed_external_lifecycle_trace_rejects_oversized_or_symlink_input(self):
        for kind in ("oversized", "symlink"):
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                source = root / "game" / "external-compatibility"
                source.mkdir(parents=True)
                trace = source / "external-lifecycle-diagnostics.txt"
                if kind == "oversized":
                    trace.write_bytes(b"x" * 65537)
                else:
                    private = root / "private"
                    private.write_text("private")
                    trace.symlink_to(private)
                with self.assertRaisesRegex(ValueError, "bounded|symlink"):
                    evidence.collect_failure(root / "game", root / "diagnostics")


    def sodium_fixture(self, directory):
        self.rendering_fixture(directory, "true")
        actual = self.base("shader-on")
        actual.update(backend="opengl", shaderInUse="true", shaderOptionsVerified="true",
                      m3Visual="true", m5ActivePackVisual="true", sodiumTerrain="true", paleOakPalette="true", compactModels="true", simplifiedTerrain="true")
        lock = evidence.read_lock()
        actual.update(shaderPackSha256=lock["shader"]["sha256"], shaderPack=lock["shader"]["filename"],
                      shaderProfile=lock["shader"]["profile"])
        for mod in ("iris", "sodium"):
            actual[f"mod.{mod}.sha256"] = lock["targets"]["26.3"][mod]["sha256"]
        (directory / "external-compatibility-manifest.properties").write_text(
            "".join(f"{key}={value}\n" for key, value in actual.items()))
        native = {
            "schema": "1", "minecraft": "26.3", "status": "passed", "scope": "sodium-terrain",
            "captureMode": "fabric-test-capture", "sodiumVersion": "0.9.2+mc26.3", "imageCount": "15",
            "minimumDifferencePixels": "80", "minimumRetentionPermille": "950",
            "minimumRestorationOverlapPermille": "990",
            "maximumShaderRestorationDifferentPixels": "200", "colorDelta": "24",
            "maskToleranceRadius": "2", "roiMinX": "90", "roiMaxX": "550", "roiMinY": "90",
            "roiMaxY": "310", "observationLimit": "4096", "failedChecks": "", "stage": "stone-restored",
        }
        checks = ("originalIndexUnchanged", "activeNonCubeResource", "explicitBaselineEmission",
                  "explicitRetainedModelEmission", "explicitHighlight", "explicitRestoration", "explicitBaseGeometry",
                  "unsupportedBaseEmission", "unsupportedControl", "tagAbsentBaseline", "tagRetainedModelEmission",
                  "tagHighlight", "tagBaseGeometry", "tagRemovedBaseEmission", "tagRemoval", "tagPreHideEmission",
                  "tagFilterEmissionSuppressed", "tagFilterAir", "tagPhysicalStatePreserved", "tagRestoredEmission",
                  "tagFilterRestoration", "resourceReloadEmission", "resourceReloadHighlight", "terrainBaselineEmission",
                  "terrainVisible", "terrainEmissionSuppressed", "terrainHiddenAir", "terrainPhysicalStatePreserved",
                  "terrainRestoredEmission", "terrainRestoration")
        native.update({key: "true" for key in checks})
        for name in ("explicitHighlight", "tagHighlight", "resourceReloadHighlight", "terrainVisible"):
            native[name + "DifferentPixels"] = "100"
        for name in ("explicitRestoration", "unsupportedControl", "tagRemoval", "tagFilterAir",
                     "terrainHiddenAir", "terrainRestoration"):
            native[name + "DifferentPixels"] = "0"
        native.update({
            "tagFilterRestorationDifferentPixels": "124",
            "tagFilterRestorationExpectedForeground": "100",
            "tagFilterRestorationRestoredForeground": "100",
            "tagFilterRestorationSharedForeground": "100",
            "tagFilterRestorationExpectedOverlapPermille": "1000",
            "tagFilterRestorationRestoredOverlapPermille": "1000",
        })
        for name in ("explicitBase", "tagBase"):
            native.update({name + "Foreground": "100", name + "Retained": "100", name + "RetentionPermille": "1000"})
        stages = ("air-control", "explicit-off", "explicit-on", "explicit-restored", "unsupported-off",
                  "unsupported-on", "tagged-off", "tagged-on", "tag-removed", "tagged-filtered",
                  "tagged-restored", "tagged-reloaded", "stone-off", "stone-hidden", "stone-restored")
        wrapped = {"explicit-off", "explicit-on", "explicit-restored", "tagged-on", "tagged-filtered",
                   "tagged-restored", "tagged-reloaded", "tag-pre-hide"}
        png = (directory / "m5-pack-off.png").read_bytes()
        for name in (*stages, "tag-pre-hide"):
            calls = "0" if name == "air-control" else "2"
            emitted = "0" if name in ("air-control", "tagged-filtered", "stone-hidden") else calls
            native.update({name + ".visited": calls, name + ".completed": calls,
                           name + ".emitted": emitted, name + ".expectedModel": emitted,
                           name + ".saturated": "false", name + ".observationComplete": "true",
                           name + ".expectedEmission": "true", name + ".preparedReplacement": str(name in wrapped).lower()})
            if name != "tag-pre-hide":
                filename = "sodium-" + name + ".png"
                (directory / filename).write_bytes(png)
                native[filename + ".sha256"] = hashlib.sha256(png).hexdigest()
        self.write_sodium_manifest(directory, native)
        return native

    @staticmethod
    def write_sodium_manifest(directory, native):
        (directory / "sodium-terrain-manifest.properties").write_text(
            "".join(f"{key}={value}\n" for key, value in native.items()))

    def test_sodium_evidence_keeps_all_fifteen_new_frames_and_native_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            self.sodium_fixture(directory)
            files = evidence.verify(directory, "26.3", "shader-on")
            self.assertEqual(132, len(files))
            self.assertEqual(15, sum(path.name.startswith("sodium-") and path.suffix == ".png" for path in files))
            self.assertIn(directory / "sodium-terrain-manifest.properties", files)

    def test_shader_rows_cannot_pass_without_sodium_native_assertions(self):
        for key, value in (("status", "failed"), ("explicitRetainedModelEmission", "false"),
                           ("terrainEmissionSuppressed", "false"), ("tagFilterEmissionSuppressed", None)):
            with self.subTest(key=key), tempfile.TemporaryDirectory() as tmp:
                directory = Path(tmp)
                native = self.sodium_fixture(directory)
                if value is None:
                    del native[key]
                else:
                    native[key] = value
                self.write_sodium_manifest(directory, native)
                with self.assertRaises(ValueError):
                    evidence.verify(directory, "26.3", "shader-on")

    def test_sodium_manifest_booleans_do_not_override_missing_model_or_incomplete_filter_work(self):
        cases = (("explicit-on.expectedModel", "0"), ("explicit-on.expectedModel", "1"),
                 ("stone-hidden.completed", "0"), ("stone-hidden.emitted", "1"),
                 ("stone-off.emitted", "0"), ("stone-restored.emitted", "0"),
                 ("tagged-filtered.visited", "0"), ("tagged-on.saturated", "true"),
                 ("explicit-on.visited", "4096"), ("tagged-on.expectedModel", "3"))
        for key, value in cases:
            with self.subTest(key=key, value=value), tempfile.TemporaryDirectory() as tmp:
                directory = Path(tmp)
                native = self.sodium_fixture(directory)
                native[key] = value
                self.write_sodium_manifest(directory, native)
                with self.assertRaises(ValueError):
                    evidence.verify(directory, "26.3", "shader-on")

    def test_sodium_thresholds_native_metrics_and_image_hashes_are_enforced(self):
        cases = (("minimumRetentionPermille", "900"), ("minimumRestorationOverlapPermille", "950"),
                  ("maximumShaderRestorationDifferentPixels", "400"),
                  ("tagFilterRestorationExpectedOverlapPermille", "989"),
                  ("tagFilterRestorationRestoredOverlapPermille", "989"),
                  ("tagFilterRestorationSharedForeground", "0"),
                  ("tagFilterRestorationDifferentPixels", "201"),
                  ("explicitHighlightDifferentPixels", "79"),
                 ("terrainHiddenAirDifferentPixels", "80"), ("tagBaseRetained", "94"),
                 ("explicitBaseRetentionPermille", "999"), ("roiMaxY", "300"),
                 ("sodiumVersion", "0.9.3+mc26.3"), ("captureMode", "completed-native-frame"),
                 ("sodium-explicit-on.png.sha256", "0" * 64))
        for key, value in cases:
            with self.subTest(key=key), tempfile.TemporaryDirectory() as tmp:
                directory = Path(tmp)
                native = self.sodium_fixture(directory)
                native[key] = value
                self.write_sodium_manifest(directory, native)
                with self.assertRaises(ValueError):
                    evidence.verify(directory, "26.3", "shader-on")

    def test_sodium_missing_or_oversized_native_evidence_is_rejected(self):
        for kind in ("manifest", "image", "oversized"):
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as tmp:
                directory = Path(tmp)
                self.sodium_fixture(directory)
                manifest = directory / "sodium-terrain-manifest.properties"
                if kind == "manifest":
                    manifest.unlink()
                elif kind == "image":
                    (directory / "sodium-stone-hidden.png").unlink()
                else:
                    with manifest.open("a") as stream:
                        stream.write("#" + "x" * 16384)
                with self.assertRaises(ValueError):
                    evidence.verify(directory, "26.3", "shader-on")

    def test_failed_sodium_collection_is_fixed_bounded_and_never_acceptance(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "game" / "sodium-terrain"
            source.mkdir(parents=True)
            native = self.sodium_fixture(source)
            native.update(status="failed", terrainEmissionSuppressed="false")
            self.write_sodium_manifest(source, native)
            (source / "private-pack.zip").write_bytes(b"private")
            destination = root / "diagnostics"
            self.assertEqual(16, evidence.collect_failure(root / "game", destination))
            self.assertEqual(15, len(list(destination.glob("sodium-*.png"))))
            self.assertFalse((destination / "private-pack.zip").exists())
            manifest = destination / "sodium-terrain-manifest.properties"
            self.assertEqual("failed", evidence.properties(manifest)["status"])
            self.assertIn(hashlib.sha256(manifest.read_bytes()).hexdigest() + "  " + manifest.name,
                          (destination / "SHA256SUMS.txt").read_text())
            self.assertIn("not acceptance evidence", (destination / "DIAGNOSTIC_STATUS.txt").read_text())

    def test_failed_sodium_manifest_rejects_over_16_kib_and_symlinks(self):
        for kind in ("oversized", "symlink"):
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                source = root / "game" / "sodium-terrain"
                source.mkdir(parents=True)
                manifest = source / "sodium-terrain-manifest.properties"
                if kind == "oversized":
                    manifest.write_bytes(b"x" * 16385)
                else:
                    private = root / "private"
                    private.write_text("private")
                    manifest.symlink_to(private)
                with self.assertRaisesRegex(ValueError, "bounded|symlink"):
                    evidence.collect_failure(root / "game", root / "diagnostics")


if __name__ == "__main__":
    unittest.main()
