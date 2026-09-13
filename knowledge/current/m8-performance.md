# M8 Performance Observations and Allocation Hardening

Status: **authoritative current M8 measurement evidence**. This document records repeatable observations and structural hardening; it does not claim a statistically proven speedup.

## Goal

Establish dual-version performance evidence before making renderer optimizations, then keep any optimization claim narrower than the measurements support.

The current M8 oracle runs inside the real Fabric Client GameTest on Minecraft 26.1.2 and 26.2. It uses a deterministic 72-block Resource Highlight scene, one resource-reload warmup, three measured reloads, and alternating all-OFF / all-18-Resource-Highlight-ON terrain rebuild observations.

## Measurement boundary

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

## Structural allocation hardening

Static inspection nevertheless identified a definite avoidable construction site in both version adapters. Resource Highlight quad emission called:

```java
DecorationQuadInstruction.emissiveTint(resource.accentArgb())
```

`DecorationQuadInstruction` is immutable. The resulting value depends only on one of ten `ResourceHighlightCue` enum values, so constructing an equivalent record for every highlighted quad has no semantic benefit.

The implementation now creates exactly one immutable instruction per `ResourceHighlightCue` enum constant and reuses it through:

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

## Post-hardening verified observation

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

The pre/post numbers do **not** establish a measurable speedup. In fact, several post-hardening medians move in the unfavorable direction. Treating only favorable samples as evidence would be invalid.

What is verified is narrower and stronger:

1. the source-level per-resource-quad `DecorationQuadInstruction` construction site has been removed;
2. the replacement reuses ten bounded immutable enum-owned objects;
3. dual-version visual/product behavior remains green;
4. JUnit, JaCoCo, and PIT remain green without lowering thresholds;
5. the broad real-client observation is now archived on every CI version job so future regressions can be studied from raw evidence.

## CI artifacts and size

Run `34736173848` archives:

- `blocklens-m8-performance-26.1.2`
- `blocklens-m8-performance-26.2`

Each contains `m8-performance-baseline.txt` with the raw samples and summary fields.

Runtime JAR artifacts in the same run are **93,068 bytes** for both Minecraft lines, still far below the 700 KiB stretch goal and the 1,183,433-byte hard maximum.

## Remaining M8 work

This is the first M8 evidence/hardening slice, not M8 completion. Remaining work includes:

- repeat observations before freezing a numeric no-regression tolerance
- runtime JAR top-entry/no-growth baseline
- representative frame-time median/P95/P99 measurement if a reproducible harness is available
- cache/retained-geometry size evidence
- lazy-initialization verification
- further optimization only where measured evidence or an unambiguous bounded structural improvement justifies it

The rule remains: do not turn a noisy observation into a performance claim.