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
            self.assertEqual({"iris", "sodium"}, set(target))
            for artifact in target.values():
                self.assertIn("/versions/" + artifact["version_id"] + "/", artifact["url"])
                self.assertEqual(128, len(bytes.fromhex(artifact["sha512"]).hex()))
                self.assertEqual(64, len(bytes.fromhex(artifact["sha256"]).hex()))
        self.assertEqual("LOW_STATIC", lock["shader"]["profile"])
        self.assertEqual(16, len(lock["shader"]["options"]))

    def test_shader_restoration_fixture_freezes_only_vertex_animation_time(self):
        shader = compatibility.read_lock()["shader"]
        self.assertEqual("0.00", shader["options"].get("WAVING_SPEED"))
        self.assertEqual("2", shader["options"]["SSAO_QUALI_DEFINE"])
        self.assertNotIn("WAVING_LEAVES", shader["options"])
        self.assertNotIn("WAVING_FOLIAGE", shader["options"])

    def test_static_shader_fixture_freezes_subpixel_phase_without_disabling_taa(self):
        options = compatibility.read_lock()["shader"]["options"]
        self.assertEqual("0", options.get("TAA_JITTER"))
        self.assertNotIn("TAA_DEFINE", options)

    def test_all_six_supplied_pack_inputs_are_byte_pinned(self):
        packs = compatibility.read_lock()["packs"]
        self.assertEqual({"reference-ui", "chise", "chise-server", "lowfire", "glowing", "small"}, set(packs))
        self.assertEqual("53d456fa9396a362d8f408a7f17f70372ef8df15e4fc9e2439db31fbd23f1109",
                         packs["chise-server"]["sha256"])
        self.assertEqual(71634, packs["chise-server"]["size"])
        self.assertEqual(84, packs["chise-server"]["min_format"])
        self.assertEqual(84, packs["chise-server"]["max_format"])
        self.assertEqual(len(packs), len({entry["filename"] for entry in packs.values()}))

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
            (root / "reference-ui.zip").write_bytes(b"not the expected pack")
            with self.assertRaisesRegex(ValueError, "reference-ui.*unavailable"):
                compatibility.locate_packs(root, compatibility.read_lock())

    def test_native_vulkan_cannot_be_silently_claimed_for_26_1_2(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError, "26.2 and 26.3"):
                compatibility.prepare("26.1.2", "vulkan", Path(tmp))


if __name__ == "__main__":
    unittest.main()
