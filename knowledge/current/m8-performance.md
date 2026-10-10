# M8 Performance, Allocation, Retention, and Artifact-Size Hardening

Status: **M8 complete / historical original-product evidence**.

The measurements, original 37-capability count and 100 KiB release budget below describe the original M8 release era. The development tree has 46 capabilities and three supported Minecraft versions; current reviewed size policy is 140 KiB development / 150 KiB release / 1,183,432 B absolute. See [roadmap.md](roadmap.md) and [minecraft-26-3.md](minecraft-26-3.md). Coarse 6.0 s reload / 2.5 s rebuild / 32 MiB allocation guards remain active. Diagnostic publication and controlled sampling were completed in #43 / PR #45; evidence and the unresolved original failure cause are in [m8-diagnostics.md](m8-diagnostics.md); this historical record is not evidence of current measurement stability.

This document records what BlockLens can prove on Minecraft **26.1.2** and **26.2**. It deliberately distinguishes deterministic structural improvements from noisy whole-client timing observations; M8 does **not** claim an FPS or percentage speedup that the measurements cannot support.

## Goal and final result

M8 kept the full **37-capability** product contract while adding real-client performance evidence, bounded-retention evidence, coarse regression guards, and strict artifact-size gates.

Final verification run: **GitHub Actions `34738345474` (run #223)**.

| Result | Minecraft 26.1.2 | Minecraft 26.2 |
| --- | ---: | ---: |
| Runtime JAR | **88,973 B** | **88,973 B** |
| Client GameTest | PASS | PASS |
| Reproducible JAR | PASS | PASS |
| M3/M5/M7 regression evidence | PASS | PASS |
| M8 performance manifest | PASS | PASS |
| reload retention stable across 3 measured reloads | PASS | PASS |

The PR #17 frozen pre-optimization baseline was **93,068 B**. The completed M8 artifact is therefore **4,095 B smaller (4.4%)**. Relative to the pinned source resource pack (**2,366,865 B**), the final runtime artifact is **3.76%** of the source ZIP, a **96.24% reduction** while retaining all 37 capabilities.

The final no-growth baseline is now **88,973 B** on each supported Minecraft line.

## Size contract

```text
pinned source pack:             2,366,865 B
absolute <50% maximum:          1,183,432 B
BlockLens release budget:         102,400 B (100 KiB)
final frozen M8 baseline:           88,973 B
final/source ratio:                    3.76%
source-size reduction:                96.24%
```

Per-version Gradle verification fails when the runtime JAR exceeds any applicable limit. Intentional future growth requires an explicit reviewed rebaseline; the release budget and source-pack half-size requirement remain independent upper bounds.

`writeRuntimeJarSizeReport` also writes deterministic evidence to `build/reports/blocklens/runtime-jar-size.txt`, containing exact bytes, entry count, compressed category totals, and the 20 largest compressed entries.

Final 26.1.2 runtime inspection contained **55 files** and no retired policy bytecode. Compressed category totals were:

| Category | Compressed bytes |
| --- | ---: |
| core rendering | 30,993 |
| Minecraft adapter | 20,667 |
| core state | 13,535 |
| core other | 9,347 |
| language assets | 1,265 |
| model assets | 434 |
| metadata / other | 424 |

Largest compressed entries included `MinecraftDecorationModel.class` (5,986 B), `DecorationTargetCatalog.class` (4,550 B), `MinecraftStateAdapter.class` (3,877 B), and `SemanticState.class` (3,568 B). These numbers are diagnostic evidence, not targets for unsafe code golf.

## Structural runtime hardening

### M5 Resource Highlight instruction reuse

Resource Highlight quad emission previously created an equivalent immutable `DecorationQuadInstruction` repeatedly. Each `ResourceHighlightCue` now owns one immutable emissive instruction and both adapters use:

```java
instruction = resource.instruction();
```

The duplicate retained `accentArgb` field was also removed; the immutable instruction is the single source of truth for the color.

### M4 Visibility instruction reuse

The same bounded pattern is used for visibility cues. Each `VisibilityVisualCue` owns one immutable instruction and both adapters use:

```java
instruction = visibility.cue().instruction();
```

The former `VisibilityQuadCuePolicy` runtime class is removed. Regression tests verify value semantics, object identity reuse, and direct dual-version adapter wiring.

### Unreachable production bytecode removal

Dependency reachability from the real `BlockLensClient` entry point identified packaged policy wrappers that were not part of the actual runtime graph. M8 removed:

- `VisibilityQuadCuePolicy`
- `ResourceHighlightPolicy`
- `DecorationRenderPolicy`
- generated `DecorationRenderPolicy$1`

Their product contracts remain tested against the actual runtime config/descriptor/cue paths. `versionSmokeContract` now explicitly rejects these retired class entries if they reappear in the runtime JAR.

The first strict-size CI attempt demonstrated that the gate is effective: an intermediate artifact of **93,141 B** exceeded the then-frozen 93,068 B baseline by 73 B and failed. The threshold was not relaxed; the implementation was simplified instead.

## Real-client measurement design

The M8 oracle runs in the Fabric Client GameTest for both supported Minecraft versions using a deterministic **72-block Resource Highlight scene**.

It records:

- one resource-reload warmup + three measured reloads;
- reload median / nearest-rank P95 / P99;
- alternating all-OFF and all-18-Resource-Highlight-ON terrain rebuilds;
- total and render-relevant JVM allocation medians using `com.sun.management.ThreadMXBean`;
- main render-pass timing via test-only `LevelRenderEvents.START_MAIN` / `END_MAIN` sampling;
- median / P95 / P99 main render-pass duration;
- wrapped-model and retained-capability-slot counts after every measured reload;
- BlockLens initialization time.

The frame metric is **main render-pass duration**, not end-to-end present-to-present frame time. It is useful as reproducible CI evidence but must not be marketed as a complete FPS benchmark.

## Why the regression guards are coarse

Runs #190 and #192 provided repeated dual-version observations before an absolute performance tolerance was frozen. Across those runs, reload medians were roughly **3.86–4.45 s** and rebuild medians roughly **1.49–1.53 s**. Allocation medians were around **8–10.5 MB**, but raw samples contained large 100–200 MB outliers caused by shared JVM/client work.

M8 therefore freezes only broad **median-based gross-regression guards**:

```text
reload median:       <= 6.0 s
rebuild median:      <= 2.5 s
allocation median:   <= 32 MiB
```

These limits are intentionally not used to claim a speedup. Raw allocation outliers, ON-minus-OFF deltas, and frame percentiles remain evidence fields rather than pass/fail optimization claims.

## Final run #223 performance evidence

### Minecraft 26.1.2

| Metric | Result |
| --- | ---: |
| reload median | **3.842 s** |
| reload P95/P99 | **4.230 s / 4.230 s** |
| OFF / Resource-ON total allocation median | **9,340,144 B / 9,330,424 B** |
| OFF / Resource-ON render-relevant median | **8,318,328 B / 8,380,072 B** |
| OFF / Resource-ON rebuild median | **1.493 s / 1.510 s** |
| OFF frame-main median / P95 / P99 | **5.026 / 6.909 / 7.517 ms** |
| Resource-ON frame-main median / P95 / P99 | **5.078 / 7.969 / 12.009 ms** |
| frame-main median delta | **+0.053 ms** |
| initialization | **3.722 ms** |

### Minecraft 26.2

| Metric | Result |
| --- | ---: |
| reload median | **4.069 s** |
| reload P95/P99 | **4.290 s / 4.290 s** |
| OFF / Resource-ON total allocation median | **10,375,528 B / 10,325,776 B** |
| OFF / Resource-ON render-relevant median | **9,447,240 B / 9,356,680 B** |
| OFF / Resource-ON rebuild median | **1.532 s / 1.512 s** |
| OFF frame-main median / P95 / P99 | **5.054 / 6.144 / 8.229 ms** |
| Resource-ON frame-main median / P95 / P99 | **5.092 / 11.441 / 12.538 ms** |
| frame-main median delta | **+0.038 ms** |
| initialization | **3.771 ms** |

The ON P95/P99 tails are visibly noisier than the medians. M8 therefore records them but does not convert them into an unsupported performance-win claim.

## Bounded retained-state / reload evidence

After each of the three measured resource reloads, both supported versions reported exactly:

```text
wrapped models:                  7,820 / 7,820 / 7,820
retained capability slots:       7,827 / 7,827 / 7,827
max capabilities per model:          2 /     2 /     2
Nether wrapped models:              33 /    33 /    33
reloadRetentionStable=true
```

This is direct evidence that the model-bake retained-capability structure is bounded and does not grow across the measured reload sequence.

Retention counters are updated only during model bake; M8 deliberately does not add Atomic counters to the render hot path.

## Lazy initialization / cache contract

Nether overlay models remain lazy and bounded:

- `netherInteriorOverlay` and `netherBandOverlay` are per-wrapper cached references;
- `interiorLookupAttempted` / `bandLookupAttempted` stop repeated failed lookups;
- overlay lookup only occurs on the Nether-enabled path;
- no registry scan, world scan, or measurement counter was added to quad emission;
- existing M6/M7 rendered evidence verifies the enabled path continues to render correctly.

The code contract is protected by repository tests, while retained reload stability is measured in the real Client GameTest.

## Quality / behavior authority

Run #223 passed:

- common JUnit;
- JaCoCo;
- PIT without lowering thresholds;
- Minecraft 26.1.2 build and reproducibility;
- Minecraft 26.2 build and reproducibility;
- both Client GameTests;
- M3 rendered evidence;
- M5 dark-area evidence;
- M5 active-resource-pack deterministic fixture;
- M7 full parity evidence;
- M8 performance/retention manifests;
- privacy/residue/runtime-entry audit;
- final 88,973-byte JAR gate.

## M8 exit decision

**M8 is complete.**

All M8 tasks are satisfied:

- [x] strict dual-version no-growth baseline
- [x] 100 KiB release budget and <50% source-pack absolute rule
- [x] deterministic JAR size/category/top-entry evidence
- [x] bounded instruction reuse and unreachable production-bytecode removal
- [x] repeated observations before freezing coarse performance guards
- [x] main render-pass median/P95/P99 evidence
- [x] retained model/capability evidence across repeated reloads
- [x] lazy/bounded overlay cache contract
- [x] dual-version real-client verification
- [x] M3/M5/M7 behavior unchanged

Compatibility work such as representative shader-ON, broader third-party resource-pack testing, and experimental Vulkan verification belongs to the M4–M7 compatibility/release tracks, not to M8 performance/size acceptance.

The continuing rule is: **do not trade capability parity for size, and do not turn noisy measurements into a performance claim.**
