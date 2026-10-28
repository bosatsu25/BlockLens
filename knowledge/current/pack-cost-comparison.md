# Pack-vs-mod cost comparison

Status: **authoritative current measurement contract** for any claim that BlockLens is lighter or
faster than the source resource pack. Results are recorded in §6 only from CI runs on a named head.

## 1. Question and conditions

The product started from "the pack, but lighter on memory and faster". M8 bounds BlockLens against
its own regressions; it never measured the pack, and its rebuild figure times a fixed 30-tick settle
window rather than compile work. This comparison does, in one client per supported
Minecraft line (26.1.2, 26.2, 26.3):

| Condition | Resource packs | BlockLens |
| --- | --- | --- |
| `baseline` | none added | installed, every capability off |
| `pack` | the source pack (or its structural twin, §2) | installed, every capability off |
| `mod` | none added | the 37 pack-equivalent capabilities on (`CapabilityId` ordinals 0–36, the source's `respackopts` keys) |

`pack − baseline` is the pack's cost; `mod − baseline` is the cost of enabling BlockLens. BlockLens'
installed-but-off cost (its own sprites, bake-time wrapping, initialization) is present in all three
conditions and therefore reported separately (§4), not hidden in either difference.

Without Respackopts every file of the source pack loads, which equals the pack with all 37 options
on. Respackopts itself (option parsing, 337 `.rpo` conditions) is not installed, so `pack` is a
**lower bound** of the real pack's cost. A result "BlockLens costs less" is conservative; a result
"the pack costs less" is not, for the pack as players run it with Respackopts.

## 2. Source pack versus structural twin

The repository is public and the source pack is third-party material, so CI never sees it.
`scripts/pack_structure_twin.py` derives a structure profile from the pinned archive
(`070bc338…`, the same `reference-ui` pack as the private external-pack matrix) and rebuilds a
loadable twin from it:

- kept: ZIP entry order, kinds and compression; vanilla blockstate names, state keys, `x`/`y`/
  `uvlock`, multipart conditions; the model reference graph (parents, texture variables, vanilla
  references); element and face counts with `cullface`, `tintindex`, `shade`, `ambientocclusion`,
  element rotation angle and face rotation; image dimensions, PNG color type and bit depth,
  palette/transparency sizes, IDAT and other chunk byte sizes; animation parameters; `.rpo`
  conditions (capability keys only); names directly inside a category folder, which replace the
  vanilla resource of that name (replacing versus adding decides atlas growth);
- replaced: all pixels (seeded noise tuned to the recorded compressed size), element coordinates
  and UVs (a canonical full cube), credits, Blockbench metadata, translations, the pack
  description and the names of resources the pack adds in its own folders.

`extract` refuses to write a profile containing free text. The committed
`scripts/source_pack_structure.jsonl.gz` (52 KB) contains 123 vanilla identifiers and no source
names or text. Every structural field of the twin matches the source exactly: 4,744 entries,
274 blockstates with 3,398 variants, 2,860 models with 1,064 elements and 4,278 faces
(2,920 culled), 1,019 textures (53 replacing vanilla) with identical dimensions, 42 animations.
Byte totals are within 10 % (JSON 1,630,477 vs 1,696,636 B; PNG 2,050,307 vs 2,051,512 B).
CI regenerates the twin and fails if any structural field differs.

A private local run measures the real archive with the same oracle:

```text
gradlew -PverifyPackagedRuntime=true -PclientTestFocus=pack-cost -PpackCostSource=source \
  -PpackCostPack=<path to the pinned archive> :versions:mc26_3:runClientGameTest
```

The oracle refuses any archive whose SHA-256 differs from the pin. Its outputs contain timings and
sizes only.

## 3. Procedure

`PackCostComparisonOracle` (focus `pack-cost`, packaged runtime) builds a 64 × 8 × 64 flat-world
scene on a non-ticking floor with random ticks off (covered grass would otherwise keep recompiling
it) of pack-equivalent targets (glazed terracotta in four facings, logs/wood/froglights in three
axes, bottom/top/double slabs, stairs, trapdoors, anvil, beehive, campfire, grindstone, fence
gates, stained glass, ores, obsidian, ancient debris, the outline and Nether Tweaks blocks), exactly
16 sections, viewed top-down. One discarded pass per condition warms class loading and JIT. Six
rounds then measure all three conditions in rotated orders, so each condition takes each position
twice. Per condition and round:

1. select the packs and the BlockLens config, then time `reloadResourcePacks()` to completion;
2. read the block atlas size and whether the twin's marker sprite is present;
3. invalidate all terrain and time every section compile until 10 quiet ticks (wall, thread CPU
   and allocation of each compile, inside and outside the scene box; `SectionCompileTimingProbe`);
4. sample the main render pass for 60 ticks at a 60 FPS limit;
5. read Java heap use after two `System.gc()` calls.

Only invalid measurement fails the job: missing scene compiles, too few frames, a block atlas that
changes between rounds of the same condition, a pack that does not grow the atlas, BlockLens
changing the atlas, twin sprites present without the pack, or packs/config not restored.

## 4. Metrics and claim rules

`scripts/summarize_pack_cost.py` validates the raw CSV and reports, per line:

| Metric | Unit | Notes |
| --- | --- | --- |
| Resource reload | ms | the resource-loading part of startup; the client also reloads at launch |
| Java heap after reload + GC | MiB | retained models and sprite metadata; noisy |
| Scene compile CPU per section | ms | meshing work for the decorated scene (pack models versus BlockLens transforms and decals) |
| Scene compile allocation per section | KiB | |
| Whole-view rebuild | ms | until the last compile returns |
| Main render pass median | ms | not an FPS figure |
| Block atlas RGBA bytes with mips | MiB | deterministic, from atlas size and the mipmap setting |

A difference between `mod` and `pack` is stated only when all six paired rounds agree on its sign
(two-sided sign test, p = 0.031). Otherwise the report says "no consistent difference". Hosted
runners use software rendering (Mesa llvmpipe) and shared CPUs: frame and rebuild times are
relative evidence for these runners, not player-hardware figures. BlockLens' own sprites, reported
as `modSpriteBytes`, and `initializationNanos` bound its installed-but-off cost; bake-time wrapping
is inside every condition's reload and is not separated.

## 5. Out of scope

- Respackopts cost (see §1) and pack option subsets.
- BlockLens capabilities beyond the 37 pack-equivalent ones.
- GPU memory other than the block atlas texture; native image memory during loading.
- Player-hardware FPS. The private local run can provide real-GPU timings for one machine.

## 6. Results

Pending the first CI run of this contract.
