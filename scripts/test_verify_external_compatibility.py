"""Regression checks against falsely accepting external rendering evidence."""

from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

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


if __name__ == "__main__":
    unittest.main()
