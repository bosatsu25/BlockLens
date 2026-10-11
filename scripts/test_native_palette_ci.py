"""Execute the actual CI Bash collector against malformed native evidence."""

from pathlib import Path
import os
import shutil
import subprocess
import tempfile
import textwrap
import unittest

from verify_external_compatibility import PALETTE_CHECKS, PALETTE_IMAGES


class NativePaletteCollectorTest(unittest.TestCase):
    def collect(self, pixels):
        source = Path(__file__).resolve().parents[1] / ".github/workflows/ci.yml"
        section = source.read_text(encoding="utf-8").split(
            "      - name: Collect native pale-oak palette evidence\n", 1)[1]
        script = textwrap.dedent(section.split("        run: |\n", 1)[1].split(
            "      - name: Upload native pale-oak palette evidence\n", 1)[0])
        script = script.replace("${{ matrix.module }}", "mc26_3").replace(
            "${{ matrix.minecraft }}", "26.3")
        if os.name == "nt":
            bash = str(Path(os.environ["ProgramFiles"]) / "Git/bin/bash.exe")
        else:
            bash = shutil.which("bash")
        self.assertTrue(bash and Path(bash).is_file(), "Bash is required for the CI collector regression")
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            output = root / "versions/mc26_3/build/run/clientGameTest/pale-oak-visual"
            output.mkdir(parents=True)
            for image in PALETTE_IMAGES:
                (output / image).write_bytes(b"frame")
            content = "minecraft=26.3\n" + "".join(f"{key}=true\n" for key in PALETTE_CHECKS)
            if pixels is not None:
                content += "changedPixels=" + pixels + "\n"
            (output / "palette-manifest.txt").write_text(content, encoding="utf-8")
            env = dict(os.environ, GITHUB_OUTPUT="github-output.txt")
            result = subprocess.run([bash, "-c", script], cwd=root, env=env,
                                    capture_output=True, check=False)
            return result.returncode, (root / "build/ci/pale-oak-26.3/SHA256SUMS.txt").is_file()

    def test_malformed_or_outside_roi_pixels_cannot_be_published(self):
        for value in (None, "", "missing", "80", "14251", "-1", "0100", "100\nchangedPixels=100"):
            with self.subTest(value=value):
                code, published = self.collect(value)
                self.assertNotEqual(0, code, "CI silently accepted malformed native evidence")
                self.assertFalse(published)

    def test_both_valid_roi_bounds_are_accepted(self):
        for value in ("81", "14250"):
            with self.subTest(value=value):
                code, published = self.collect(value)
                self.assertEqual(0, code)
                self.assertTrue(published)


if __name__ == "__main__":
    unittest.main()
