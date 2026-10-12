import tempfile
import unittest
from pathlib import Path
import test_connected_evidence
import verify_connected_external as verifier
from verify_external_compatibility import verify as verify_full
class ConnectedExternalTest(unittest.TestCase):
    def fixture(self,root,version,profile):
        test_connected_evidence.ConnectedEvidenceTest().fixture(root,version)
        lock=verifier.read_lock()
        fields={"schema":"1","minecraft":version,"profile":profile,"status":"passed","fullGraph":"false",
                "packagedRuntime":"true","backendVerified":"true","shaderVerified":"true","backend":"vulkan" if profile=="vulkan" else "opengl",
                "shaderInUse":str(profile=="shader-on").lower(),"connectedOreOnly":"true","connectedOres":"true",
                "captureMode":"completed-native-frame" if (profile=="shader-on" or (version=="26.1.2" and profile=="shader-off")) else "fabric-test-capture",
                "backendDevice":"fixture","backendDriver":"fixture"}
        if profile.startswith("shader-"):
            fields["shaderPackSha256"]=lock["shader"]["sha256"]
            for mod in ("iris","sodium"):
                fields[f"mod.{mod}.sha256"]=lock["targets"][version][mod]["sha256"]
                fields[f"mod.{mod}.version"]="fixture"
        if profile=="shader-on":fields.update(shaderOptionsVerified="true",shaderPack=lock["shader"]["filename"],shaderProfile=lock["shader"]["profile"])
        (root/"external-compatibility-manifest.properties").write_text("\n".join(key+"="+value for key,value in fields.items())+"\n",encoding="utf-8")
    def test_all_eight_actual_renderer_rows_and_full_suite_isolation(self):
        for version in ("26.1.2","26.2","26.3"):
            for profile in ("shader-off","shader-on","vulkan"):
                if version=="26.1.2" and profile=="vulkan":continue
                with self.subTest(version=version,profile=profile),tempfile.TemporaryDirectory() as tmp:
                    root=Path(tmp).resolve();self.fixture(root,version,profile)
                    self.assertEqual(179,len(verifier.verify(root,version,profile)))
                    with self.assertRaises(ValueError):verify_full(root,version,profile)
    def test_missing_wrong_backend_bytes_shader_and_private_fields_rejected(self):
        for key in ("status","backend","backendVerified","shaderInUse","shaderOptionsVerified","mod.iris.sha256",
                    "mod.sodium.sha256","shaderPackSha256","connectedOreOnly","connectedOres","captureMode"):
            with self.subTest(key=key),tempfile.TemporaryDirectory() as tmp:
                root=Path(tmp).resolve();self.fixture(root,"26.3","shader-on")
                path=root/"external-compatibility-manifest.properties"
                lines=path.read_text().splitlines();path.write_text("\n".join(line for line in lines if not line.startswith(key+"="))+"\n")
                with self.assertRaises(ValueError):verifier.verify(root,"26.3","shader-on")
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp).resolve();self.fixture(root,"26.3","shader-on")
            path=root/"external-compatibility-manifest.properties";path.write_text(path.read_text()+"materialHash=private\n")
            with self.assertRaises(ValueError):verifier.verify(root,"26.3","shader-on")
