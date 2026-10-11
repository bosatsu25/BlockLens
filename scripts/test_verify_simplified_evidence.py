import tempfile
import unittest
from pathlib import Path
import verify_simplified_evidence as verifier

class SimplifiedEvidenceTest(unittest.TestCase):
    def fixture(self,root,version="26.3"):
        fields={key:"true" for key in verifier.CHECKS}
        fields.update(minecraft=version,roles="14",changedPixels="294")
        (root/"simplified-manifest.txt").write_text("\n".join(key+"="+value for key,value in fields.items())+"\n",encoding="utf-8")
        header=b"\x89PNG\r\n\x1a\n"+b"\x00\x00\x00\x0dIHDR"+(640).to_bytes(4,"big")+(360).to_bytes(4,"big")+b"x"*1100
        for name in verifier.IMAGES:(root/name).write_bytes(header)
    def test_three_supported_rows_and_exact_frame_set(self):
        for version in ("26.1.2","26.2","26.3"):
            with self.subTest(version=version),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root,version);self.assertEqual(71,len(verifier.verify(root,version)))
    def test_each_missing_or_failed_native_control_is_rejected(self):
        for control in verifier.CHECKS:
            with self.subTest(control=control),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root)
                path=root/"simplified-manifest.txt";text=path.read_text().replace(control+"=true",control+"=false");path.write_text(text)
                with self.assertRaises(ValueError):verifier.verify(root,"26.3")
    def test_each_missing_or_invalid_frame_is_rejected(self):
        for name in verifier.IMAGES:
            with self.subTest(name=name),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root);(root/name).unlink()
                with self.assertRaises(ValueError):verifier.verify(root,"26.3")
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp).resolve();self.fixture(root);(root/verifier.IMAGES[0]).write_bytes(b"bad")
            with self.assertRaises(ValueError):verifier.verify(root,"26.3")
    def test_started_diagnostic_private_extra_and_wrong_version_are_rejected(self):
        for text in ("status=started\n","privateAssetHash=hidden\n","sourceArchive=private\n","minecraft=26.2\n","changedPixels=+294\n","roles=13\n"):
            with self.subTest(text=text),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root);path=root/"simplified-manifest.txt";path.write_text(path.read_text()+text)
                with self.assertRaises(ValueError):verifier.verify(root,"26.3")
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp).resolve();self.fixture(root);(root/"DIAGNOSTIC_STATUS.txt").write_text("failed")
            with self.assertRaises(ValueError):verifier.verify(root,"26.3")
    def test_collector_refuses_existing_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp).resolve();source=root/"source";source.mkdir();self.fixture(source);output=root/"output";output.mkdir();(output/"keep").write_text("keep")
            with self.assertRaises(ValueError):verifier.collect(source,"26.3",output)
            self.assertEqual("keep",(output/"keep").read_text())