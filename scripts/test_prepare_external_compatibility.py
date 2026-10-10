"""Fail-closed provenance and isolation tests for the optional client harness."""

import hashlib
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import prepare_external_compatibility as compatibility


class ExternalPreparationTest(unittest.TestCase):
    def test_official_lock_pins_every_dependency_and_target(self):
        lock = compatibility.read_lock()
        self.assertEqual({"26.1.2", "26.2", "26.3"}, set(lock["targets"]))
        for target in lock["targets"].values():
            self.assertEqual({"iris", "sodium", "litematica", "malilib"}, set(target))
            for artifact in target.values():
                self.assertIn("/versions/" + artifact["version_id"] + "/", artifact["url"])
                self.assertEqual(128, len(bytes.fromhex(artifact["sha512"]).hex()))
                self.assertEqual(64, len(bytes.fromhex(artifact["sha256"]).hex()))
        self.assertEqual("LOW", lock["shader"]["profile"])
        self.assertEqual(14, len(lock["shader"]["options"]))

    def test_existing_same_size_tampered_dependency_is_rejected_without_redownload(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "fixture.jar"
            path.write_bytes(b"bad")
            expected = {"size": 3, "sha256": hashlib.sha256(b"yes").hexdigest()}
            with patch("urllib.request.urlopen") as network:
                with self.assertRaisesRegex(ValueError, "sha256"):
                    compatibility.download(expected, path)
                network.assert_not_called()

    def test_oversized_response_does_not_create_a_dependency(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "fixture.jar"
            expected = {"size": 3, "url": "https://cdn.modrinth.com/data/test/file.jar",
                        "sha256": hashlib.sha256(b"yes").hexdigest()}
            with patch("urllib.request.urlopen") as network:
                network.return_value.__enter__.return_value.read.return_value = b"yes!"
                with self.assertRaisesRegex(ValueError, "size"):
                    compatibility.download(expected, path)
            self.assertFalse(path.exists())

    def test_dependency_destination_symlink_cannot_modify_another_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "user-file"
            target.write_bytes(b"user")
            path = Path(tmp) / "fixture.jar"
            path.symlink_to(target)
            with self.assertRaisesRegex(ValueError, "symlink"):
                compatibility.download({"size": 4}, path)
            self.assertEqual(b"user", target.read_bytes())

    def test_native_profile_only_prepares_the_requested_fixture_directory(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            destination = root / "fixture"
            result = compatibility.prepare("26.3", "vulkan", destination)
            self.assertEqual(str(destination), result["directory"])
            self.assertEqual([destination], list(root.iterdir()))
            self.assertEqual("vulkan", result["profile"])
            self.assertTrue((destination / "manifest.properties").is_file())

    def test_wrong_pack_bytes_cannot_be_selected_by_filename(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "amateras.zip").write_bytes(b"not the expected pack")
            with self.assertRaisesRegex(ValueError, "amateras.*unavailable"):
                compatibility.locate_packs(root, compatibility.read_lock())

    def test_native_vulkan_cannot_be_silently_claimed_for_26_1_2(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError, "26.2 and 26.3"):
                compatibility.prepare("26.1.2", "vulkan", Path(tmp))


if __name__ == "__main__":
    unittest.main()
