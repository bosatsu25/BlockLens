"""Regression checks against falsely accepting external rendering evidence."""

from pathlib import Path
import hashlib
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
        actual.update(m3Visual="true", m5ActivePackVisual="true")
        (directory / "external-compatibility-manifest.properties").write_text(
            "".join(f"{key}={value}\n" for key, value in actual.items()))
        (directory / "m3-visual-manifest.txt").write_text("minecraft=26.3\n")
        (directory / "m5-pack-visual-manifest.txt").write_text(
            "minecraft=26.3\n" + ("" if isolation is None else f"externalSceneIsolated={isolation}\n"))

        def chunk(name, payload):
            return struct.pack(">I", len(payload)) + name + payload + struct.pack(">I", zlib.crc32(name + payload))

        row = b"\0" + bytes(range(256)) * 7 + bytes(range(128))
        png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 640, 360, 8, 2, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(row * 360)) + chunk(b"IEND", b""))
        for name in evidence.DEFAULT_IMAGES:
            (directory / name).write_bytes(png)

    def test_nonisolated_or_unrecorded_m5_scene_is_rejected(self):
        for isolation in ("false", None):
            with self.subTest(isolation=isolation), tempfile.TemporaryDirectory() as tmp:
                directory = Path(tmp)
                self.rendering_fixture(directory, isolation)
                with self.assertRaisesRegex(ValueError, "externalSceneIsolated.*true"):
                    evidence.verify(directory, "26.3", "vulkan")

    def test_isolated_m5_scene_acceptance_keeps_all_six_images(self):
        with tempfile.TemporaryDirectory() as tmp:
            directory = Path(tmp)
            self.rendering_fixture(directory, "true")
            files = evidence.verify(directory, "26.3", "vulkan")
            self.assertEqual(9, len(files))
            self.assertTrue(set(evidence.DEFAULT_IMAGES).issubset(path.name for path in files))

    def test_masa_metadata_without_typed_schematic_read_is_rejected(self):
        actual = self.base("masa")
        actual.update(backend="opengl", schematicRead="true")
        lock = evidence.read_lock()
        for mod in ("litematica", "malilib"):
            actual[f"mod.{mod}.sha256"] = lock["targets"]["26.3"][mod]["sha256"]
        schematic = {"minecraft": "26.3", "pinnedVersions": "true", "typedRead": "false"}
        with patch.object(evidence, "properties", side_effect=[actual, schematic]):
            with self.assertRaisesRegex(ValueError, "typedRead.*true"):
                evidence.verify(Path("unused"), "26.3", "masa")

    def test_masa_wrong_release_bytes_are_rejected(self):
        actual = self.base("masa")
        actual.update(backend="opengl", schematicRead="true")
        lock = evidence.read_lock()
        for mod in ("litematica", "malilib"):
            actual[f"mod.{mod}.sha256"] = lock["targets"]["26.2"][mod]["sha256"]
        with patch.object(evidence, "properties", return_value=actual):
            with self.assertRaisesRegex(ValueError, "mod.litematica.sha256"):
                evidence.verify(Path("unused"), "26.3", "masa")

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

    def test_failed_masa_lifecycle_trace_is_hashed_and_remains_incomplete(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "game" / "external-compatibility"
            source.mkdir(parents=True)
            trace = source / "external-lifecycle-diagnostics.txt"
            trace.write_text("status=diagnostic-only\nstalledPhase=world_close\n")
            manifest = source / "external-compatibility-manifest.properties"
            actual = self.base("masa")
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
                evidence.verify(destination, "26.3", "masa")

    def test_failed_masa_lifecycle_trace_rejects_oversized_or_symlink_input(self):
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


if __name__ == "__main__":
    unittest.main()
