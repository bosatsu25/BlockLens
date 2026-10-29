#!/usr/bin/env python3
"""Validate and summarize one pack-vs-mod cost comparison run (PackCostComparisonOracle).

Conditions: ``baseline`` (BlockLens installed, every capability off, no pack), ``pack`` (baseline
plus the source pack or its structural twin) and ``mod`` (the 37 pack-equivalent capabilities on,
no pack). A difference is reported only when every paired round agrees on its sign: with six
rounds a chance agreement has probability 2/64 (two-sided sign test, p = 0.031).
"""
import argparse
import csv
import json
import collections
import shutil
import statistics
import subprocess
import sys
from pathlib import Path

CONDITIONS = ("baseline", "pack", "mod")
ROUNDS = 6
COLUMNS = ("round,position,condition,reloadNanos,heapBytes,atlasWidth,atlasHeight,twinMarker,sceneCompiles,"
           "sceneCpuNanos,sceneWallNanos,sceneAllocatedBytes,otherCompiles,otherCpuNanos,incompleteCompiles,"
           "rebuildNanos,frames,frameMedianNanos,frameP95Nanos").split(",")
# (key, label, unit, value of one sample); per-compile values remove differences in compile counts.
METRICS = (
    ("reload", "Resource reload (startup resource load)", "ms", lambda s: s["reloadNanos"] / 1e6),
    ("heap", "Java heap after reload + GC", "MiB", lambda s: s["heapBytes"] / 2 ** 20),
    ("compileCpu", "Scene section compile CPU per section", "ms", lambda s: s["sceneCpuNanos"] / s["sceneCompiles"] / 1e6),
    ("compileAllocated", "Scene section compile allocation per section", "KiB",
     lambda s: s["sceneAllocatedBytes"] / s["sceneCompiles"] / 1024),
    ("rebuild", "Whole-view rebuild until the last compile returns", "ms", lambda s: s["rebuildNanos"] / 1e6),
    ("frame", "Main render pass median", "ms", lambda s: s["frameMedianNanos"] / 1e6),
)


# Compile-thread profile (unmeasured JFR passes): each sample belongs to the innermost BlockLens
# code path on its stack, so a decal quad's lighting counts as decal cost, not as base geometry.
COMPILE_FRAME = "net.minecraft.client.renderer.chunk.SectionCompiler.compile"
FABRIC_PREFIX = "dev.blocklens.fabric."
# Each overlay layer is its own class; the model keeps the tint transform and the lightweight overlay.
LAYERS = {"MinecraftDecalLayer": "decals", "MinecraftOreBorderLayer": "oreBorder",
          "MinecraftNetherLayer": "netherOverlay"}
MODEL = "MinecraftDecorationModel"
MODEL_PATHS = {
    "emitLightweightOverlay": "lightweightOverlay",
    "applyNonNetherInstructions": "baseTransform", "applyInstruction": "baseTransform",
}
CATEGORIES = ("decals", "oreBorder", "netherOverlay", "lightweightOverlay", "baseTransform",
              "wrappedEmission", "blocklensOther", "otherCompile")


class InvalidRun(ValueError):
    pass


def read_manifest(path):
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            values[key] = value
    if values.get("mode") != "pack-cost-comparison" or values.get("conditions") != ",".join(CONDITIONS):
        raise InvalidRun("not a pack-cost comparison manifest")
    return values


def read_samples(path):
    with path.open(encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream)
        if reader.fieldnames != list(COLUMNS):
            raise InvalidRun("unexpected sample columns")
        samples = []
        for row in reader:
            sample = {key: (value if key in ("condition", "twinMarker") else int(value)) for key, value in row.items()}
            if sample["condition"] not in CONDITIONS or sample["sceneCompiles"] <= 0:
                raise InvalidRun(f"invalid sample: {row}")
            samples.append(sample)
    seen = sorted((s["round"], s["condition"]) for s in samples)
    if seen != sorted((r, c) for r in range(ROUNDS) for c in CONDITIONS):
        raise InvalidRun("every round must measure every condition exactly once")
    for condition in CONDITIONS:
        positions = sorted(s["position"] for s in samples if s["condition"] == condition)
        if positions != [0, 0, 1, 1, 2, 2]:
            raise InvalidRun(f"{condition} must take every position twice")
    return samples


def atlas_bytes(width, height, mip_levels):
    """RGBA bytes of the block atlas texture including its mip chain."""
    return sum(max(1, width >> level) * max(1, height >> level) * 4 for level in range(mip_levels + 1))


def paired(samples, metric, first, second):
    """Per-round differences second - first for one metric."""
    value = {(s["round"], s["condition"]): metric(s) for s in samples}
    return [value[(r, second)] - value[(r, first)] for r in range(ROUNDS)]


def verdict(deltas, lower_name, higher_name):
    if all(d < 0 for d in deltas):
        return f"{lower_name} lower in every round"
    if all(d > 0 for d in deltas):
        return f"{higher_name} lower in every round"
    return "no consistent difference"


def summarize(manifest, samples):
    mips = int(manifest["mipmapLevels"])
    result = {"minecraft": manifest["minecraft"], "source": manifest["source"], "rounds": ROUNDS, "metrics": {}}
    for key, label, unit, metric in METRICS:
        medians = {c: statistics.median(metric(s) for s in samples if s["condition"] == c) for c in CONDITIONS}
        pack_cost = paired(samples, metric, "baseline", "pack")
        mod_cost = paired(samples, metric, "baseline", "mod")
        mod_vs_pack = paired(samples, metric, "pack", "mod")
        result["metrics"][key] = {
            "label": label, "unit": unit, "median": medians,
            "packMinusBaseline": statistics.median(pack_cost),
            "modMinusBaseline": statistics.median(mod_cost),
            "modMinusPack": statistics.median(mod_vs_pack),
            "modMinusPackRounds": mod_vs_pack,
            "verdict": verdict(mod_vs_pack, "mod", "pack"),
        }
    atlas = {}
    for condition in CONDITIONS:
        sizes = {(s["atlasWidth"], s["atlasHeight"]) for s in samples if s["condition"] == condition}
        if len(sizes) != 1:
            raise InvalidRun(f"{condition} block atlas size changed between rounds")
        width, height = sizes.pop()
        atlas[condition] = {"width": width, "height": height, "bytes": atlas_bytes(width, height, mips)}
    if atlas["pack"]["bytes"] <= atlas["baseline"]["bytes"] or atlas["mod"]["bytes"] != atlas["baseline"]["bytes"]:
        raise InvalidRun("atlas invariants failed: the pack must grow it and BlockLens must not")
    result["atlas"] = atlas
    result["modSpriteBytes"] = int(manifest["modSpritePixels"]) * 4
    return result


def parse_samples(lines):
    """Yields (thread, frames leaf-first) for each jdk.ExecutionSample in `jfr print` text."""
    thread, frames, in_stack = None, [], False
    for raw in lines:
        line = raw.strip()
        if line == "jdk.ExecutionSample {":
            thread, frames, in_stack = None, [], False
        elif line.startswith("sampledThread = "):
            thread = line.split('"')[1] if '"' in line else line
        elif line == "stackTrace = [":
            in_stack = True
        elif in_stack and line == "]":
            in_stack = False
        elif in_stack and line != "...":
            frames.append(line.split("(", 1)[0])
        elif line == "}" and thread is not None:
            yield thread, frames
            thread = None


def classify(frames):
    """The innermost BlockLens code path of one compile sample, or None outside section compiles.

    Helper frames (face lookups, DecorationDecals math) are skipped: the sample belongs to the
    nearest enclosing path, or to the wrapper's own emission when no overlay path encloses it.
    """
    if COMPILE_FRAME not in frames:
        return None
    blocklens = False
    for frame in frames:
        if frame.startswith(FABRIC_PREFIX):
            owner, _, method = frame[len(FABRIC_PREFIX):].partition(".")
            owner = owner.split("$", 1)[0]
            if owner in LAYERS:
                return LAYERS[owner]
            if owner == MODEL:
                core = method.split("$")[1] if method.startswith("lambda$") else method
                if method.startswith("lambda$emitQuads$"):
                    return "baseTransform"
                if core in MODEL_PATHS:
                    return MODEL_PATHS[core]
                if method == "emitQuads":
                    return "wrappedEmission"
        blocklens = blocklens or frame.startswith("dev.blocklens.")
    return "blocklensOther" if blocklens else "otherCompile"


def profile(directory, jfr):
    """Per condition: compile samples, path shares and the hottest leaf methods."""
    result = {}
    for condition in CONDITIONS:
        files = sorted(directory.glob(f"profile-{condition}-*.jfr"))
        if not files:
            raise InvalidRun(f"missing JFR profile for {condition}")
        counts, leaves, total = collections.Counter(), collections.Counter(), 0
        for path in files:
            with subprocess.Popen([jfr, "print", "--events", "jdk.ExecutionSample", "--stack-depth", "256",
                                   str(path)], stdout=subprocess.PIPE, text=True) as process:
                for _, frames in parse_samples(process.stdout):
                    total += 1
                    category = classify(frames)
                    if category is not None:
                        counts[category] += 1
                        leaves[frames[0] if frames else "?"] += 1
            if process.returncode != 0:
                raise InvalidRun(f"jfr could not read {path.name}")
        compile_samples = sum(counts.values())
        if compile_samples == 0:
            raise InvalidRun(f"no section compile samples for {condition}")
        result[condition] = {"samples": total, "compileSamples": compile_samples,
                             "paths": {c: counts[c] for c in CATEGORIES}, "hottest": leaves.most_common(8)}
    return result


def profile_markdown(minecraft, result):
    lines = [f"### Compile-thread profile — Minecraft {minecraft}", "",
             "Share of section-compile samples by innermost BlockLens path (unmeasured JFR passes, 1 ms).", "",
             "| Condition | Compile samples | " + " | ".join(CATEGORIES) + " |",
             "| --- | ---: |" + " ---: |" * len(CATEGORIES)]
    for condition, item in result.items():
        total = item["compileSamples"]
        shares = " | ".join(f"{100 * item['paths'][c] / total:.1f} %" for c in CATEGORIES)
        lines.append(f"| {condition} | {total:,} | {shares} |")
    for condition, item in result.items():
        lines += ["", f"Hottest leaf methods ({condition}): "
                  + "; ".join(f"`{name.rsplit('.', 2)[-2]}.{name.rsplit('.', 1)[-1]}` {count}"
                              for name, count in item["hottest"])]
    return "\n".join(lines) + "\n"


def markdown(summary):
    fmt = lambda value: f"{value:,.2f}"
    lines = [f"### Pack vs BlockLens cost — Minecraft {summary['minecraft']} ({summary['source']})", "",
             f"{summary['rounds']} rotated paired rounds. A difference counts only if every round agrees.", "",
             "| Metric | Baseline | Pack | BlockLens (37 on) | Pack − baseline | BlockLens − baseline | Verdict |",
             "| --- | ---: | ---: | ---: | ---: | ---: | --- |"]
    for key, item in summary["metrics"].items():
        median = item["median"]
        lines.append(f"| {item['label']} ({item['unit']}) | {fmt(median['baseline'])} | {fmt(median['pack'])} | "
                     f"{fmt(median['mod'])} | {fmt(item['packMinusBaseline'])} | {fmt(item['modMinusBaseline'])} | "
                     f"{item['verdict']} |")
    atlas = summary["atlas"]
    mib = lambda value: f"{value / 2 ** 20:,.2f}"
    lines += ["", "| Block atlas (deterministic) | Baseline | Pack | BlockLens (37 on) |", "| --- | ---: | ---: | ---: |",
              "| Size | " + " | ".join(f"{atlas[c]['width']}×{atlas[c]['height']}" for c in CONDITIONS) + " |",
              "| RGBA bytes with mips (MiB) | " + " | ".join(mib(atlas[c]["bytes"]) for c in CONDITIONS) + " |",
              "", f"BlockLens' own sprites add {summary['modSpriteBytes']:,} B (mip level 0) in every condition."]
    return "\n".join(lines) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--directory", required=True, type=Path)
    parser.add_argument("--minecraft", required=True)
    parser.add_argument("--destination", required=True, type=Path)
    parser.add_argument("--step-summary", type=Path)
    parser.add_argument("--jfr", help="JDK jfr tool; summarizes the compile-thread profile when given")
    args = parser.parse_args(argv)
    try:
        manifest = read_manifest(args.directory / "pack-cost-comparison.txt")
        if manifest["minecraft"] != args.minecraft:
            raise InvalidRun("manifest is for a different Minecraft version")
        summary = summarize(manifest, read_samples(args.directory / "pack-cost-samples.csv"))
        if args.jfr:
            summary["profile"] = profile(args.directory, args.jfr)
    except (InvalidRun, OSError, KeyError, ValueError, ZeroDivisionError) as error:
        print(f"invalid pack-cost run: {error}", file=sys.stderr)
        return 1
    args.destination.mkdir(parents=True, exist_ok=True)
    for name in ("pack-cost-comparison.txt", "pack-cost-samples.csv"):
        shutil.copyfile(args.directory / name, args.destination / name)
    text = markdown(summary)
    if "profile" in summary:
        text += "\n" + profile_markdown(summary["minecraft"], summary["profile"])
        for path in sorted(args.directory.glob("profile-*.jfr")):
            shutil.copyfile(path, args.destination / path.name)
    (args.destination / "summary.md").write_text(text, encoding="utf-8")
    (args.destination / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    if args.step_summary:
        with args.step_summary.open("a", encoding="utf-8") as stream:
            stream.write(text)
    print(text, end="")
    return 0


if __name__ == "__main__":
    sys.exit(main())
