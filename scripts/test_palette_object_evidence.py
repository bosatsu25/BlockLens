import tempfile
import unittest
from pathlib import Path
import verify_palette_objects as verifier

class PaletteObjectEvidenceTest(unittest.TestCase):
    def fixture(self, root, version="26.3"):
        fields={key:"true" for key in verifier.CHECKS}
        fields.update(minecraft=version,roles="6")
        (root/"palette-objects-manifest.txt").write_text("\n".join(key+"="+value for key,value in fields.items())+"\n",encoding="utf-8")
        header=b"\x89PNG\r\n\x1a\n"+b"\x00\x00\x00\x0dIHDR"+(640).to_bytes(4,"big")+(360).to_bytes(4,"big")+b"x"*1100
        for name in verifier.IMAGES:(root/name).write_bytes(header)
    def test_supported_versions_and_exact_frames(self):
        for version in ("26.1.2","26.2","26.3"):
            with self.subTest(version=version),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root,version)
                self.assertEqual(25,len(verifier.verify(root,version)))
    def test_each_missing_or_failed_control(self):
        for control in verifier.CHECKS:
            with self.subTest(control=control),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root)
                path=root/"palette-objects-manifest.txt";path.write_text(path.read_text().replace(control+"=true",control+"=false"))
                with self.assertRaises(ValueError):verifier.verify(root,"26.3")
    def test_each_missing_frame_and_invalid_dimensions(self):
        for name in verifier.IMAGES:
            with self.subTest(frame=name),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root);(root/name).unlink()
                with self.assertRaises(ValueError):verifier.verify(root,"26.3")
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp).resolve();self.fixture(root);(root/verifier.IMAGES[0]).write_bytes(b"bad")
            with self.assertRaises(ValueError):verifier.verify(root,"26.3")
    def test_partial_duplicate_private_wrong_version_and_diagnostics(self):
        for text in ("status=started\n","materialHash=private\n","minecraft=26.2\n","roles=5\n","woodChroma=false\n"):
            with self.subTest(text=text),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root);path=root/"palette-objects-manifest.txt";path.write_text(path.read_text()+text)
                with self.assertRaises(ValueError):verifier.verify(root,"26.3")
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp).resolve();self.fixture(root);(root/"DIAGNOSTIC_STATUS.txt").write_text("failed")
            with self.assertRaises(ValueError):verifier.verify(root,"26.3")
    def test_new_output_contains_only_synthetic_allowlist(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp).resolve();source=root/"source";source.mkdir();self.fixture(source)
            (source/"private-material-hashes.txt").write_text("private")
            output=root/"output";self.assertEqual(25,verifier.collect(source,"26.3",output))
            self.assertEqual(set(verifier.IMAGES)|{"palette-objects-manifest.txt","SHA256SUMS.txt"},{p.name for p in output.iterdir()})
            with self.assertRaises(ValueError):verifier.collect(source,"26.3",output)
