"""Regression guard for the pack-vs-mod cost comparison summary and its validity rules."""
import json
import re
import tempfile
import unittest
from pathlib import Path

import summarize_pack_cost as summary

ROOT = Path(__file__).resolve().parents[1]
ORDERS = ((0, 1, 2), (1, 2, 0), (2, 0, 1), (0, 2, 1), (2, 1, 0), (1, 0, 2))
MANIFEST = ("minecraft=26.3\nmode=pack-cost-comparison\nsource=twin\nconditions=baseline,pack,mod\n"
            "mipmapLevels=4\nmodSpritePixels=8192\n")


def sample(round_, position, condition, reload_ms, cpu_ms=2.0, atlas=(2048, 1024)):
    values = {"round": round_, "position": position, "condition": condition,
              "reloadNanos": int(reload_ms * 1e6), "heapBytes": 300 << 20, "atlasWidth": atlas[0],
              "atlasHeight": atlas[1], "twinMarker": str(condition == "pack").lower(), "sceneCompiles": 16,
              "sceneCpuNanos": int(cpu_ms * 16e6), "sceneWallNanos": int(cpu_ms * 17e6),
              "sceneAllocatedBytes": 16 << 20, "otherCompiles": 400, "otherCpuNanos": 10 ** 8,
              "incompleteCompiles": 0, "rebuildNanos": 9 * 10 ** 8, "frames": 120,
              "frameMedianNanos": 5 * 10 ** 6, "frameP95Nanos": 7 * 10 ** 6}
    return ",".join(str(values[column]) for column in summary.COLUMNS)


def run(rows, manifest=MANIFEST):
    directory = Path(tempfile.mkdtemp())
    (directory / "pack-cost-comparison.txt").write_text(manifest, encoding="utf-8")
    (directory / "pack-cost-samples.csv").write_text(",".join(summary.COLUMNS) + "\n" + "\n".join(rows) + "\n",
                                                     encoding="utf-8")
    destination = directory / "out"
    code = summary.main(["--directory", str(directory), "--minecraft", "26.3", "--destination", str(destination)])
    return code, destination


def rows(reload=lambda r, c: {"baseline": 4000, "pack": 4300 + r, "mod": 4010 + r}[c],
         cpu=lambda r, c: {"baseline": 2.0, "pack": 2.1, "mod": 2.4 if r % 2 else 1.9}[c]):
    result = []
    for round_, order in enumerate(ORDERS):
        for position, index in enumerate(order):
            condition = summary.CONDITIONS[index]
            atlas = (2048, 2048) if condition == "pack" else (2048, 1024)
            result.append(sample(round_, position, condition, reload(round_, condition), cpu(round_, condition), atlas))
    return result


class SummarizePackCostTest(unittest.TestCase):
    def test_consistent_and_inconsistent_differences(self):
        code, destination = run(rows())
        self.assertEqual(code, 0)
        result = json.loads((destination / "summary.json").read_text())
        reload = result["metrics"]["reload"]
        self.assertEqual(reload["verdict"], "mod lower in every round")
        self.assertAlmostEqual(reload["packMinusBaseline"], 302.5)
        self.assertAlmostEqual(reload["modMinusBaseline"], 12.5)
        self.assertEqual(result["metrics"]["compileCpu"]["verdict"], "no consistent difference")
        self.assertEqual(result["metrics"]["frame"]["verdict"], "no consistent difference")
        self.assertEqual(result["atlas"]["pack"]["bytes"], summary.atlas_bytes(2048, 2048, 4))
        self.assertEqual(result["modSpriteBytes"], 32768)
        self.assertIn("Pack − baseline", (destination / "summary.md").read_text())

    def test_pack_lower_verdict(self):
        code, destination = run(rows(cpu=lambda r, c: {"baseline": 2.0, "pack": 2.0, "mod": 2.5}[c]))
        self.assertEqual(code, 0)
        result = json.loads((destination / "summary.json").read_text())
        self.assertEqual(result["metrics"]["compileCpu"]["verdict"], "pack lower in every round")

    def test_atlas_bytes_include_the_mip_chain(self):
        self.assertEqual(summary.atlas_bytes(16, 8, 0), 512)
        self.assertEqual(summary.atlas_bytes(16, 8, 4), (128 + 32 + 8 + 2 + 1) * 4)

    def test_invalid_runs_are_rejected(self):
        valid = rows()
        self.assertEqual(run(valid[:-1])[0], 1, "a missing sample")
        self.assertEqual(run(valid[:3] + valid[:3] + valid[6:])[0], 1, "a duplicated round")
        swapped = [row.replace(",0,baseline,", ",9,baseline,") for row in valid]
        self.assertEqual(run(swapped)[0], 1, "positions must be balanced")
        self.assertEqual(run(valid, MANIFEST.replace("26.3", "26.2"))[0], 1, "wrong version")
        self.assertEqual(run(valid, MANIFEST.replace("pack-cost-comparison", "m8"))[0], 1, "wrong manifest")
        no_growth = [row.replace(",2048,2048,", ",2048,1024,") for row in valid]
        self.assertEqual(run(no_growth)[0], 1, "the pack must grow the atlas")
        uncompiled = [re.sub(r",(true|false),16,", r",\1,0,", row) for row in valid]
        self.assertEqual(run(uncompiled)[0], 1, "scene sections must compile")

    def test_columns_match_the_oracle(self):
        oracle = (ROOT / "gametest/java/dev/blocklens/gametest/PackCostComparisonOracle.java").read_text()
        header = re.search(r'HEADER = "([^;]+);', oracle, re.S).group(1)
        self.assertEqual(",".join(summary.COLUMNS), re.sub(r'"\s*\+\s*"', "", header).rstrip('"'))
        self.assertIn('{"baseline", "pack", "mod"}', oracle)
        self.assertEqual(oracle.count("{0, 1, 2}, {1, 2, 0}, {2, 0, 1}, {0, 2, 1}, {2, 1, 0}, {1, 0, 2}"), 1)


SAMPLE_TEXT = """jdk.ExecutionSample {
  startTime = 07:16:50.945 (2026-10-28)
  sampledThread = "Worker-Main-1" (javaThreadId = 30)
  state = "STATE_RUNNABLE"
  stackTrace = [
    net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator.compute(MutableQuadViewImpl) line: 3
    dev.blocklens.fabric.MinecraftDecorationModel.emitDecals(QuadEmitter, BlockPos) line: 234
    dev.blocklens.fabric.MinecraftDecorationModel.emitQuads(QuadEmitter, BlockPos) line: 216
    net.minecraft.client.renderer.chunk.SectionCompiler.compile(SectionPos) line: 90
    ...
  ]
}

jdk.ExecutionSample {
  startTime = 07:16:50.946 (2026-10-28)
  sampledThread = "Worker-Main-2" (javaThreadId = 31)
  state = "STATE_RUNNABLE"
  stackTrace = [
    dev.blocklens.fabric.MinecraftDecorationModel.applyNonNetherInstructions(MutableQuadView, long) line: 456
    dev.blocklens.fabric.MinecraftDecorationModel.lambda$emitQuads$0(int[], MutableQuadView) line: 196
    dev.blocklens.fabric.MinecraftDecorationModel.emitQuads(QuadEmitter, BlockPos) line: 200
    net.minecraft.client.renderer.chunk.SectionCompiler.compile(SectionPos) line: 90
  ]
}

jdk.ExecutionSample {
  startTime = 07:16:50.947 (2026-10-28)
  sampledThread = "Render thread" (javaThreadId = 1)
  state = "STATE_RUNNABLE"
  stackTrace = [
    java.util.HashMap.get(Object) line: 556
  ]
}
"""


class CompileProfileTest(unittest.TestCase):
    def test_parses_jfr_print_text(self):
        samples = list(summary.parse_samples(SAMPLE_TEXT.splitlines()))
        self.assertEqual([thread for thread, _ in samples], ["Worker-Main-1", "Worker-Main-2", "Render thread"])
        self.assertEqual(samples[0][1][1], "dev.blocklens.fabric.MinecraftDecorationModel.emitDecals")
        self.assertEqual(len(samples[0][1]), 4, "a truncation marker is not a frame")

    def test_innermost_blocklens_path_owns_the_sample(self):
        model = "dev.blocklens.fabric.MinecraftDecorationModel."
        compile_frame = summary.COMPILE_FRAME
        cases = {
            "decals": ["indigo.AoCalculator.compute", model + "lambda$emitDecals$2", model + "emitDecals",
                       model + "emitQuads", compile_frame],
            "baseTransform": [model + "lambda$emitQuads$0", model + "emitQuads", compile_frame],
            "oreBorder": [model + "lambda$emitOreBorder$5", model + "emitOreBorder", compile_frame],
            "netherOverlay": [model + "emitNetherOverlay", model + "emitQuads", compile_frame],
            "wrappedEmission": ["indigo.AoCalculator.compute", model + "emitQuads", compile_frame],
            # Helper frames inside a path belong to that path.
            "decals ": ["dev.blocklens.core.render.DecorationDecals.right", model + "face",
                        model + "applyDecal", model + "lambda$emitDecals$1", compile_frame],
            "blocklensOther": ["dev.blocklens.fabric.MinecraftStateAdapter.state", compile_frame],
            "otherCompile": ["net.minecraft.client.renderer.block.LiquidBlockRenderer.tesselate", compile_frame],
        }
        for expected, frames in cases.items():
            self.assertEqual(summary.classify(frames), expected.strip(), expected)
        self.assertIsNone(summary.classify(["java.util.HashMap.get"]), "outside section compiles")

    def test_profile_reads_every_condition_through_the_jfr_tool(self):
        directory = Path(tempfile.mkdtemp())
        (directory / "fixture.txt").write_text(SAMPLE_TEXT, encoding="utf-8")
        tool = directory / "jfr"
        tool.write_text(f"#!/bin/sh\ncat '{directory / 'fixture.txt'}'\n", encoding="utf-8")
        tool.chmod(0o755)
        for condition in summary.CONDITIONS:
            (directory / f"profile-{condition}-0.jfr").write_bytes(b"")
        result = summary.profile(directory, str(tool))
        self.assertEqual(result["mod"]["samples"], 3)
        self.assertEqual(result["mod"]["compileSamples"], 2)
        self.assertEqual(result["mod"]["paths"]["decals"], 1)
        self.assertEqual(result["mod"]["paths"]["baseTransform"], 1)
        text = summary.profile_markdown("26.3", result)
        self.assertIn("| mod | 2 | 50.0 % | 0.0 % | 0.0 % | 0.0 % | 50.0 % |", text)
        (directory / "profile-pack-0.jfr").unlink()
        with self.assertRaises(summary.InvalidRun):
            summary.profile(directory, str(tool))


if __name__ == "__main__":
    unittest.main()
