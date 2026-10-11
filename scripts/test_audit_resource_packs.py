import tempfile
import unittest
import warnings
from pathlib import Path
from zipfile import ZipFile
from audit_resource_packs import audit


class ResourcePackAuditTest(unittest.TestCase):
    def test_metadata_and_determinism(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "pack.zip"
            with ZipFile(path, "w") as archive:
                archive.writestr("assets/minecraft/models/block/a.json", "{}")
                archive.writestr("assets/minecraft/textures/block/a.png", "image")
                archive.writestr("options/a.rpo", "")
            result = audit(path)
            self.assertEqual(result["entries"], 3)
            self.assertEqual(result["types"][".json"], 1)
            self.assertEqual(result["types"][".png"], 1)
            self.assertEqual(result["types"][".rpo"], 1)
            self.assertEqual(result, audit(path))

    def test_unsafe_path_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "bad.zip"
            with ZipFile(path, "w") as archive:
                archive.writestr("../a.png", "")
            with self.assertRaises(ValueError):
                audit(path)

    def test_duplicate_member_is_reported(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "dup.zip"
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                with ZipFile(path, "w") as archive:
                    archive.writestr("assets/a.png", "a")
                    archive.writestr("assets/a.png", "b")
            self.assertEqual(audit(path)["duplicate_paths"], ["assets/a.png"])


if __name__ == "__main__":
    unittest.main()
