"""Reject incomplete or cross-version compact-model rendering evidence."""
from pathlib import Path
import struct
import tempfile
import unittest
import zlib
import verify_external_compatibility as evidence

ITEMS = ("white_concrete", "apple", "shield", "white_bed", "white_banner", "trident")
IMAGES = tuple(f"{item}-{stage}.png" for item in ITEMS for stage in ("off", "on", "restored")) + (
    "shield-using.png", "trident-using.png", "white_concrete-third-person.png", "white_concrete-gui.png",
    "apple-third-person.png", "apple-gui.png", "all-features.png", "all-features-reloaded.png",
    "external-priority.png", "inherited-external-ground.png")
CHECKS = ("actualModelParser", "bothHands", "thirdPerson", "gui", "inheritedExternal", "independentGroups",
          "stateExceptions", "uiSave", "uiDiscard", "failedSave", "allFeatures", "reload", "offRestored", "externalPriority")

class CompactEvidenceTest(unittest.TestCase):
    def fixture(self, root, minecraft="26.3"):
        def chunk(name, payload):
            return struct.pack(">I", len(payload)) + name + payload + struct.pack(">I", zlib.crc32(name + payload))
        row = b"\0" + bytes(range(256)) * 7 + bytes(range(128))
        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 640, 360, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(row * 360)) + chunk(b"IEND", b"")
        for name in IMAGES: (root / name).write_bytes(png)
        text = f"minecraft={minecraft}\nmodelRoles={40 if minecraft == '26.1.2' else 41}\nchangedPixels=186\n" + "".join(f"{key}=true\n" for key in CHECKS)
        (root / "compact-manifest.txt").write_text(text, encoding="utf-8")
        return root / "compact-manifest.txt"

    def test_all_three_versions_retain_every_required_frame(self):
        for version in ("26.1.2", "26.2", "26.3"):
            with self.subTest(version=version), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); self.fixture(root, version)
                files = evidence.verify_compact(root, version)
                self.assertEqual(29, len(files))
                self.assertEqual(set(IMAGES) | {"compact-manifest.txt"}, {p.name for p in files})

    def test_any_failed_native_control_is_rejected(self):
        for key in CHECKS:
            with self.subTest(key=key), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); manifest = self.fixture(root)
                manifest.write_text(manifest.read_text().replace(f"{key}=true", f"{key}=false"))
                with self.assertRaises(ValueError): evidence.verify_compact(root, "26.3")

    def test_wrong_version_missing_roles_and_noncanonical_counts_are_rejected(self):
        for key, value in (("minecraft", "26.2"), ("modelRoles", "29"), ("changedPixels", "185"),
                           ("changedPixels", "1075201"), ("changedPixels", "0186"), ("changedPixels", "-1")):
            with self.subTest(key=key, value=value), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); manifest = self.fixture(root)
                content = manifest.read_text(); lines = content.splitlines()
                manifest.write_text("\n".join(f"{key}={value}" if line.startswith(key + "=") else line for line in lines) + "\n")
                with self.assertRaises(ValueError): evidence.verify_compact(root, "26.3")

    def test_each_missing_frame_is_rejected(self):
        for name in IMAGES:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); self.fixture(root); (root / name).unlink()
                with self.assertRaises(ValueError): evidence.verify_compact(root, "26.3")

    def test_failed_diagnostics_are_never_success_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixture(root)
            (root / "DIAGNOSTIC_STATUS.txt").write_text("status=failed-or-incomplete\n")
            with self.assertRaisesRegex(ValueError, "diagnostic"):
                evidence.verify_compact(root, "26.3")

    def test_unapproved_manifest_fields_are_not_published(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); manifest = self.fixture(root)
            manifest.write_text(manifest.read_text() + "privateMetadata=forbidden\n")
            with self.assertRaises(ValueError): evidence.verify_compact(root, "26.3")

    def test_duplicate_manifest_and_wrong_image_dimensions_are_rejected(self):
        for malformed in ("duplicate", "dimensions"):
            with self.subTest(malformed=malformed), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); manifest = self.fixture(root)
                if malformed == "duplicate": manifest.write_text(manifest.read_text() + "bothHands=true\n")
                else:
                    image = root / IMAGES[0]; data = bytearray(image.read_bytes()); data[16:24] = struct.pack(">II", 640, 480); image.write_bytes(data)
                with self.assertRaises(ValueError): evidence.verify_compact(root, "26.3")

if __name__ == "__main__": unittest.main()
