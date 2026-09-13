# M8 Performance, Allocation, and Artifact-Size Hardening

Status: **authoritative current M8 evidence**. This document records repeatable observations and structural hardening; it does not claim a statistically proven speedup where the measurements do not support one.

## Goal

Keep the verified **37-capability** product behavior intact while reducing avoidable runtime work and keeping BlockLens dramatically smaller than the pinned AMATERAS source pack on Minecraft 26.1.2 and 26.2.

M8 uses two different kinds of evidence and does not conflate them:

1. **real-client observation** for reload/rebuild/allocation behavior, where runner noise must be reported honestly;
2. **deterministic structural evidence** for bounded allocation-site removal and runtime-JAR size, where exact contracts can be enforced.

## Product size contract

Pinned source resource pack:

```text
source pack:                  2,366,865 B
absolute <50% maximum:        1,183,432 B
BlockLens release budget:       102,400 B (100 KiB)
frozen verified M8 baseline:     93,068 B
baseline/source ratio:              3.93%
source-size reduction:             96.1%
```

The source-pack half-size rule remains an absolute product requirement, but it is no longer useful as the day-to-day engineering threshold because BlockLens is already much smaller. M8 therefore adds a tighter **100 KiB release budget** and freezes **93,068 B** as the no-growth review baseline.

Growth above 93,068 B is treated as a regression unless the baseline is deliberately revised with reviewed justification. A deliberate baseline revision still cannot exceed the 100 KiB release budget or the source-pack half-size absolute requirement.

Per-version Gradle verification now writes `build/reports/blocklens/runtime-jar-size.txt`, including:

- exact runtime JAR bytes
- frozen baseline and release/source limits
- source-pack percentage
- runtime entry count
- compressed bytes by category
- the 20 largest compressed entries

This report is deterministic artifact evidence. It is separate from noisy runtime timing observations.

## Real-client measurement boundary

The M8 oracle runs inside the real Fabric Client GameTest on Minecraft 26.1.2 and 26.2. It uses a deterministic 72-block Resource Highlight scene, one resource-reload warmup, three measured reloads, and alternating all-OFF / all-18-Resource-Highlight-ON terrain rebuild observations.

Resource reload is timed end-to-end around Minecraft's resource-pack reload future.

Allocation observation uses `com.sun.management.ThreadMXBean` cumulative allocated bytes before and after a 30-tick deterministic terrain rebuild window. The manifest records both all live threads and a render/chunk/worker-thread subset.

These allocation values are **scenario-level JVM evidence**, not BlockLens-exclusive allocation accounting. Terminating threads may be missed. Minecraft/Fabric background work, JIT/GC timing, worker scheduling, resource reload effects, and shared CI runner variation can dominate small BlockLens deltas.

Accordingly, the current oracle has no invented absolute performance threshold. A numeric no-regression tolerance must only be frozen after repeated observations establish reproducibility.

## Pre-hardening observation

GitHub Actions `34735108314` measured the implementation before Resource Highlight instruction reuse.

| Metric | Minecraft 26.1.2 | Minecraft 26.2 |
| --- | ---: | ---: |
| reload median | 3,909,121,171 ns | 3,822,403,632 ns |
| reload nearest-rank P95 | 4,360,485,485 ns | 4,504,479,740 ns |
| OFF total allocation median | 8,930,200 B | 10,943,800 B |
| Resource-ON total allocation median | 8,980,904 B | 10,890,872 B |
| ON - OFF total allocation median | +50,704 B | -52,928 B |
| OFF render-relevant allocation median | 7,969,632 B | 10,006,144 B |
| Resource-ON render-relevant allocation median | 7,944,704 B | 9,889,008 B |
| ON - OFF render-relevant median | -24,928 B | -117,136 B |
| OFF rebuild median | 1,514,869,706 ns | 1,503,649,410 ns |
| Resource-ON rebuild median | 1,486,336,181 ns | 1,490,671,856 ns |
| ON - OFF rebuild median | -28,533,525 ns | -12,977,554 ns |
| wrapped models | 7,820 | 7,820 |

The sign already differed between total and render-relevant allocation and between Minecraft versions. This prevented a defensible claim that the broad scenario had isolated Resource Highlight's record construction cost.

## Structural allocation hardening — M5 Resource Highlight

Static inspection identified a definite avoidable construction site in both version adapters. Resource Highlight quad emission previously called:

```java
DecorationQuadInstruction.emissiveTint(resource.accentArgb())
```

`DecorationQuadInstruction` is immutable. The resulting value depends only on one of ten `ResourceHighlightCue` enum values, so constructing an equivalent record for every highlighted quad has no semantic benefit.

The implementation creates exactly one immutable instruction per `ResourceHighlightCue` enum constant and reuses it through:

```java
resource.instruction()
```

This is intentionally narrow:

- ten shared instruction instances globally
- no per-model instruction cache
- no new unbounded collection
- no change to active baked-model preservation
- no change to color/emissive semantics
- identical common policy on 26.1.2 and 26.2
- unit identity/semantic coverage plus a cross-version structural regression contract

The first mutation-test run after this change correctly exposed that the new test was not in the explicit PIT test target list. The fix added `ResourceHighlightCueInstructionTest` to PIT; quality thresholds were not lowered.

## Structural allocation hardening — M4 Visibility

The same bounded pattern applies to M4. The former `VisibilityQuadCuePolicy` created an equivalent emissive `DecorationQuadInstruction` for every emitted quad from one of only seven `VisibilityVisualCue` enum values.

M8 now stores one immutable instruction inside each `VisibilityVisualCue`, and both Minecraft adapters consume it directly:

```java
instruction = visibility.cue().instruction();
```

The thin `VisibilityQuadCuePolicy` runtime class is removed entirely. This removes both the repeated immutable-record construction site and an unnecessary hot-path method/class indirection.

The existing `accentArgb()` contract remains unchanged by reading the color from that immutable instruction. The regression tests verify value semantics, **object identity reuse**, direct adapter wiring on both supported versions, and the absence of the retired policy class. The retained set is bounded to exactly the seven enum values; there is no dynamic map or cache growth.

The first strict-size CI run for this slice proved the no-growth gate was active: an intermediate implementation measured **93,141 B**, which was **73 B above** the frozen 93,068 B baseline, and both version builds failed immediately. The baseline was not relaxed. The implementation was instead simplified by removing the redundant policy class.

As with the M5 change, this is a structural allocation/runtime simplification, not a claim that broad CI rebuild timing moved by a measurable percentage.

## Post-M5-hardening verified observation

GitHub Actions `34736173848` (run #190), commit `27b7eb5e2872b08a73940fecf9541661af1dab0a`, passed common JUnit + JaCoCo + PIT and both real-client version jobs. Existing M3, M5 dark-area, M5 active-resource-pack, and M7 evidence remained green. The M8 manifest is archived per Minecraft line.

| Metric | Minecraft 26.1.2 | Minecraft 26.2 |
| --- | ---: | ---: |
| reload samples | 4,452,367,229 / 3,862,105,487 / 4,649,925,437 ns | 3,552,747,219 / 4,072,455,612 / 3,864,880,623 ns |
| reload median | 4,452,367,229 ns | 3,864,880,623 ns |
| reload nearest-rank P95 | 4,649,925,437 ns | 4,072,455,612 ns |
| OFF total allocation median | 9,140,560 B | 10,233,960 B |
| Resource-ON total allocation median | 9,320,096 B | 10,457,512 B |
| ON - OFF total allocation median | +179,536 B | +223,552 B |
| OFF render-relevant allocation median | 8,171,384 B | 9,304,384 B |
| Resource-ON render-relevant allocation median | 8,295,456 B | 9,450,960 B |
| ON - OFF render-relevant median | +124,072 B | +146,576 B |
| OFF rebuild median | 1,486,856,321 ns | 1,491,444,424 ns |
| Resource-ON rebuild median | 1,515,398,881 ns | 1,502,251,018 ns |
| ON - OFF rebuild median | +28,542,560 ns | +10,806,594 ns |
| wrapped models | 7,820 | 7,820 |
| BlockLens initialization | 7,480,858 ns | 4,557,871 ns |

One 26.1.2 Resource-ON allocation sample was **196,859,224 B** total / **177,953,064 B** render-relevant while the other samples were around 9 MB. That large outlier is additional evidence that this broad rebuild window is noisy and unsuitable for proving a small per-quad allocation improvement by itself.

### Interpretation

The pre/post timing numbers do **not** establish a measurable speedup. Several post-hardening medians move in the unfavorable direction; selecting only favorable samples would be invalid.

What is verified is narrower and stronger:

1. repeated M5 per-resource-quad instruction construction is removed;
2. repeated M4 per-visibility-quad instruction construction is removed in the current hardening slice;
3. replacements use tiny bounded immutable enum-owned sets;
4. dual-version visual/product behavior remains the regression authority;
5. JUnit, JaCoCo, and PIT stay mandatory without threshold reduction;
6. broad real-client observations remain archived so future regressions can be investigated from raw evidence;
7. runtime-JAR size is enforced independently with deterministic byte-level evidence.

## Final PR #17 baseline

GitHub Actions `34736446316` passed the latest PR #17 head on all three jobs:

- common JUnit + JaCoCo + PIT — PASS
- Minecraft 26.1.2 build / reproducibility / Client GameTest / evidence — PASS
- Minecraft 26.2 build / reproducibility / Client GameTest / evidence — PASS

Both runtime artifacts were exactly **93,068 B**. This is the frozen M8 no-growth baseline used by the current size gate.

## Remaining M8 work

M8 is not complete. Remaining work includes:

- verify the current M4 allocation-hardening slice on both Minecraft lines
- verify that the stricter 93,068 B no-growth gate and 100 KiB release budget pass on both artifacts
- inspect generated runtime-JAR category/top-entry reports for further safe reduction candidates
- repeat observations before freezing a numeric runtime-performance tolerance
- representative frame-time median/P95/P99 measurement if a reproducible harness is available
- cache/retained-geometry size evidence
- lazy-initialization and reload-work verification
- further optimization only where measured evidence or an unambiguous bounded structural improvement justifies it

The rule remains: **do not trade capability parity for size, and do not turn a noisy observation into a performance claim.**
