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
3. invalidate all terrain and time every section compile until none has returned for 1 s of wall
   time (wall, thread CPU and allocation of each compile, inside and outside the scene box;
   `SectionCompileTimingProbe`). GameTest ticks are not real-time, so quiescence is not counted in ticks;
4. sample the main render pass for 60 ticks at a 60 FPS limit;
5. read Java heap use after two `System.gc()` calls.

After the measured rounds, three unmeasured rebuilds per condition run under a JFR execution-sample
recording (1 ms, full stacks). The summary assigns every section-compile sample to the innermost
BlockLens code path on its stack (decals, ore border, Nether overlay, lightweight overlay, base
transform, the wrapper's own emission, other compile work), so the lighting of a decal quad counts
as decal cost. The recordings are part of the artifact; no timing in §4 is taken while JFR runs.

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

Run [40658200803](https://github.com/bosatsu25/BlockLens/actions/runs/40658200803) on `6fe5f21`:
structural twin, hosted `ubuntu-24.04` runners, Mesa llvmpipe software rendering, 60 FPS limit.
Medians of six rounds; Δ is the condition minus `baseline`. "Lower" verdicts held in all six rounds.

| Metric | 26.1.2 pack Δ / BlockLens Δ | 26.2 pack Δ / BlockLens Δ | 26.3 pack Δ / BlockLens Δ | Verdict |
| --- | ---: | ---: | ---: | --- |
| Resource reload (ms) | +273.0 / −40.7 | +25.5 / −20.0 | +235.5 / +21.1 | BlockLens lower on 26.1.2 and 26.3; 26.2 no consistent difference |
| Java heap after reload + GC (MiB) | +5.2 / −0.3 | +5.7 / −0.1 | +5.6 / −0.0 | BlockLens lower on all three |
| Block atlas RGBA with mips (MiB) | +21.31 / 0 | +21.31 / 0 | +21.31 / 0 | deterministic: 2048×2048 → 4096×2048 with the pack |
| Scene compile CPU per section (ms) | +1.44 / +1.60 | +0.87 / +0.87 | +1.02 / +0.98 | no consistent difference on all three |
| Scene compile allocation per section (KiB) | +165.9 / +47.8 | +165.9 / +47.8 | +165.9 / +47.8 | BlockLens lower on all three |
| Whole-view rebuild (ms) | +9.1 / +13.0 | +16.4 / +7.5 | +8.6 / +7.3 | no consistent difference on all three |
| Main render pass median (ms) | +31.5 / +13.5 | +17.8 / +8.8 | +35.2 / +10.0 | BlockLens lower on all three (software rendering) |

Baselines: reload 1.15 / 1.10 / 1.26 s; compile CPU 10.55 / 7.04 / 7.22 ms per section; main pass
47.2 / 29.0 / 29.9 ms. BlockLens' own sprites take 302,080 B of the atlas at mip 0 in every condition.
Baselines move by up to a third between runs on hosted runners, which is why only paired verdicts
within one run count.

### Meshing cost by head

The first measurement found BlockLens' meshing heavier than the pack's. Each change below was measured
by the full `pack-cost` job on its head; compile CPU is pack Δ / BlockLens Δ per section on
26.1.2; 26.2; 26.3.

| Head | Change | Compile CPU (ms) | CPU verdict | BlockLens allocation Δ |
| --- | --- | --- | --- | ---: |
| `f1bd5d4` | first measurement | +0.75/+1.59; +1.50/+2.98; +1.20/+2.73 | pack lower on all three | +115.8 KiB |
| `c7afeb6` | no identity base transform for decal-only blocks | +1.32/+2.30; +0.89/+1.66; +1.31/+3.01 | pack lower on all three | +81.8 KiB |
| `6fe5f21` | decal quads encoded once per model and replayed | +1.44/+1.60; +0.87/+0.87; +1.02/+0.98 | no consistent difference on all three | +47.8 KiB |

The compile-thread profile (`mod` condition, share of all section-compile samples on
26.1.2 / 26.2 / 26.3) shows where the change acted:

| Path | `c7afeb6` | `6fe5f21` |
| --- | --- | --- |
| Decals (encoding, lighting and buffering of decal quads) | 7.8 / 5.8 / 9.9 % | 2.5 / 3.3 / 3.5 % |
| Nether overlay | 1.7 / 1.2 / 1.7 % | 2.7 / 0.8 / 1.5 % |
| Ore border | 0.8 / 0.2 / 0.3 % | 0.4 / 0.3 / 0.4 % |
| Base per-quad transform | 0.3 / 0.2 / 0.5 % | 0.2 / 0.2 / 0.4 % |

### What the claim rules allow

- **Loading and memory: BlockLens is lighter.** The pack doubles the block atlas (+21.3 MiB of RGBA
  texture with mips; atlas packing depends only on texture dimensions, which the twin reproduces
  exactly, so this holds for the source archive) and keeps 4.9–7.5 MiB more heap, in every round on
  every line on all three measured heads. On 26.1.2 and 26.3 it adds 60–370 ms to every resource
  reload, one of which happens at startup; on 26.2 no head has shown a consistent reload difference.
  Enabling the 37 capabilities adds no measurable reload time or heap and leaves the atlas unchanged.
- **Chunk meshing: no measurable difference from the pack.** Since `6fe5f21`, compile CPU per
  decorated section is +0.87–1.60 ms for BlockLens against +0.87–1.44 ms for the pack, and no line
  shows a consistent sign. This is parity, not "faster": six paired rounds cannot separate the two.
  Before the decal quads were cached, BlockLens cost about twice the pack's overhead in every round.
  BlockLens allocates 118 KiB less per section than the pack. What remains of the decal layer
  (2.5–3.5 % of compile samples) is lighting and buffering the decal quads per block; a resource pack
  bakes the same information into textures and pays nothing at mesh time.
- **Frame time:** on software rendering the pack's main-pass cost is 2.0–3.5 times BlockLens' in this
  run (on `c7afeb6`, 26.2 showed no consistent difference). Twin elements are canonical full cubes, so
  a fill-rate-bound rasterizer may overstate the pack here, and llvmpipe is not a player GPU. This is
  relative runner evidence, not an FPS claim.
- Respackopts is not included (§1): the real pack's load cost is at least the measured `pack` value.
