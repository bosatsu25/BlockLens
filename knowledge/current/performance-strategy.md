# BlockLens Performance and Lightweight Strategy

Status: **authoritative current performance strategy**

## 1. Goal

BlockLens must be lightweight in three different senses:

1. **startup/load cost** — adding BlockLens should not materially slow client startup or initialization
2. **resource reload cost** — BlockLens must avoid recreating the source pack's thousands of tiny static resources and should keep reload work bounded
3. **runtime/render cost** — visual inspection features must avoid unbounded scans, repeated allocations, and unnecessary rebuilds

JAR byte size is a separate artifact budget and must not be confused with runtime performance.

## 2. Startup/load design rules

Prefer:

- one client entrypoint per release target
- static/compiled capability catalog rather than runtime classpath discovery
- no reflection-based feature scanning on startup
- no network/update check during startup
- no telemetry initialization
- no runtime parsing of the legacy RPO for normal operation
- lazy initialization of renderer-specific state until a feature/path actually needs it
- prevalidated config schema with bounded migration work

Avoid:

- walking large resource trees at startup
- scanning every registered block repeatedly to discover features when a deterministic mapping can be built once
- constructing shader/resource-pack compatibility state every frame
- eager generation of geometry for disabled capabilities

## 3. Resource reload rules

The migration exists partly to remove the cost and maintenance burden of thousands of resource-pack files.

During reload:

- load only minimal BlockLens-owned assets
- rebuild target lookup tables once when registry/resource state actually changes
- cache immutable lookup results with explicit invalidation
- do not regenerate unchanged static geometry per frame
- do not parse legacy RPO data unless an explicit import tool is later implemented

Measure reload duration before and after major renderer/asset changes.

## 4. Runtime rules

- no whole-world scans
- no unbounded loaded-chunk scan every tick
- prefer event/change-driven invalidation
- retain static geometry where useful and invalidate it explicitly
- keep render-hot-path allocations near zero
- use O(1) or bounded lookup for `block/state -> applicable capabilities` where practical
- precompute target/capability mappings rather than iterating over every current capability for each rendered block when evidence shows the lookup is hot
- bound every cache, queue, retained collection, and rebuild batch
- world/disconnect/resource-reload transitions must clear version/render state cleanly

## 5. Performance evidence model

Do not claim a speedup without measurement.

Track at minimum:

- process/client startup duration attributable to the tested scenario
- BlockLens initialization duration where measurable
- resource reload duration
- median frame time
- P95 frame time
- P99 frame time
- allocation rate in BlockLens update/render paths
- retained cache/geometry count and estimated memory
- runtime JAR bytes

Recommended scenarios for each supported Minecraft version:

1. vanilla/Fabric baseline without BlockLens
2. BlockLens installed, all capabilities OFF
3. representative orientation capability ON
4. representative outline/fine-line capability ON
5. resource highlights ON
6. all supported capabilities ON

## 6. No-regression gates

Absolute performance thresholds should be frozen only after a reproducible baseline exists.

Until then CI must enforce structural performance contracts, for example:

- no unbounded per-tick scan implementation
- no feature discovery through runtime reflection/classpath scanning
- no accidental startup networking
- no per-frame construction of immutable catalogs
- no unbounded collection growth
- no duplicate full-size replacement assets when shared/generated cues satisfy the visual contract

After M1 establishes repeatable evidence, record baseline values and define explicit no-regression tolerances in `knowledge/current/` rather than guessing numeric budgets.

## 7. Artifact-size budget

Source resource-pack baseline:

```text
2,366,865 bytes
```

Each supported BlockLens runtime JAR must independently satisfy:

```text
hard release maximum: < 1,183,433 bytes
stretch goal:         <= 716,800 bytes (700 KiB)
```

A passing 26.1.2 JAR does not excuse an oversized 26.2 JAR, or vice versa.

CI should report per-version:

- JAR bytes
- delta from that version's last frozen baseline
- largest compressed entries
- class/assets/metadata contribution

Once a verified BlockLens baseline exists, introduce a no-growth ceiling analogous to ChiseTweaks: current frozen baseline + explicitly permitted growth, always capped by the hard release maximum.

## 8. Optimization order

Use this order:

1. eliminate repeated JSON/model state expansion
2. eliminate duplicate/full replacement textures where runtime cues are equivalent
3. minimize eager startup/reload work
4. remove repeated runtime state interpretation with stable lookup/caching
5. losslessly optimize remaining image assets
6. compact reproducible JAR packaging
7. inspect class-level size only if still materially useful

Do not strip diagnostics, tests, source readability, or functional capability merely to hit a byte target.

## 9. Version-aware performance

Performance evidence must be recorded separately for 26.1.2, 26.2 and 26.3 because rendering/resource internals can change between Minecraft releases.

For 26.2 and 26.3, OpenGL and Vulkan results must not be mixed into one number. Vulkan is a distinct experimental verification track until BlockLens explicitly declares it supported.

## 10. Issue #43 controlled M8 experiments

The isolated client GameTest requires `-PverifyPackagedRuntime=true` together with
`-PclientTestFocus=m8` to verify packaged
runtime origins, create a world, and execute M8 without the preceding visual/UI
oracles. Its console marker and M8 manifest explicitly report `fullGraph=false`.
This changes preceding world/JIT history, so compare focused runs with focused
runs; a focused pass does not close the normal full-graph CI gate.

`-Pm8EscapeAnalysis=default` (the default) adds no VM flag.
`-Pm8EscapeAnalysis=disabled` adds `-XX:-DoEscapeAnalysis` only to the isolated
`runClientGameTest` JVM. Other values fail during Gradle configuration. It never
changes the Gradle daemon, ordinary client launcher, or distributed runtime.
The existing `-Pm8FpsLimit=30|60|120` controls the requested frame-rate ceiling;
actual frame counts remain separate evidence.

`-Pm8Compilation=default|c1|c2-batch` selects a separate, validated experiment
control for the same isolated GameTest JVM. `default` adds no compilation flags.
`c1` adds `-XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=240m`.
`c2-batch` adds `-XX:-TieredCompilation -Xbatch -XX:ReservedCodeCacheSize=240m`.
Both explicit modes keep the reserved code-cache capacity at 240 MiB because
disabling tiered compilation otherwise changes its default capacity to 48 MiB.
`-Xbatch` disables background compilation; it does not establish that every
measured method has compiled. These option semantics are documented in the
[JDK 25 Java launcher specification](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html).
The compilation and escape-analysis controls can be combined, but change one
control at a time in paired comparisons. C1 and escape-analysis-disabled modes remain diagnostic observations. C2-batch
is the explicit JVM condition for the automated conformance graph described
below. None proves which compiler tier ran in an earlier CI job or establishes
release performance on an ordinary client.

For example, run `:versions:mc26_1_2:runClientGameTest` with
`-PverifyPackagedRuntime=true -PclientTestFocus=m8 -Pm8EscapeAnalysis=disabled -Pm8FpsLimit=60`, and repeat
the paired default/disabled comparison separately for `mc26_2` and `mc26_3`.
Keep version, OS, FPS ceiling, scene history, render distance and ambient
occlusion comparable, and preserve every failed manifest as well as passes.
Do not treat an EA-disabled run as a release performance baseline.

The manifest reads the actual `DoEscapeAnalysis` boolean through the standard
`HotSpotDiagnosticMXBean`, and actual `TieredCompilation`, `TieredStopAtLevel`
(0 through 4), `BackgroundCompilation` and reserved code-cache capacity in bytes.
It also records numeric Java feature/interim/update/patch,
available processor count, a fixed OS-family value, render distance and ambient
occlusion. Existing fixed-role allocation, completed section-compile counters,
incomplete/dropped/invalid counts and loaded-chunk counts remain available.
No raw VM arguments, process/thread identifiers or arbitrary OS text are saved.
This diagnostic requires HotSpot's management bean and fails explicitly if it
cannot read an expected boolean or bounded numeric option. Measurement windows, warmups and coarse guards
remain unchanged. The scalar-replacement hypothesis remains unproven until
controlled native evidence supports it; adding these controls is not a fix for
the allocation failure.

## 11. Explicit conformance JVM condition

Automated full Client GameTests in CI and the dedicated `scripts/run-ui-tests.ps1`
launcher select `-Pm8Compilation=c2-batch -Pm8FpsLimit=60`. The UI launcher
accepts `-M8Compilation default` for a separate ordinary tiered-JVM observation.
The Gradle property itself still defaults to `default`, which adds no compilation
flags; the ordinary `runClient`, Gradle daemon and distributed mod receive no
new flags or user-option changes. C1/EA-disabled and other FPS settings are
explicit diagnostic variants and never automatically retried into passing evidence.

This defines the environment of an unchanged coarse conformance guard, not a
product performance optimization. Keep the same 32 MiB limit, 30-tick windows,
warmups, scene, sampling, OFF/default/all-ON scenarios and completed-work/frame
observations. Record actual VM values rather than trusting requested flags.
Focused and full-graph histories remain separate. Test-only SectionCompiler
instrumentation may itself affect JIT and captures normal returns only.

Controlled Windows Java25.0.4.1 observations on all three versions passed at
default and C2-batch, each using three samples per scenario. The C1-only
26.1.2 observation failed at approximately 155–165 MB whole-client allocation,
including roughly 82–107 MB of completed compiler-worker allocation over 48–59
section returns; C1-only 26.2/26.3 remained within the unchanged guard.
The corresponding 26.1.2 default/C2 observations were about 20–21 MB whole-client.
These include OFF and cannot attribute the excess to BlockLens cue rendering.
They demonstrate a version-dependent compiler-optimization boundary, while the
exact allocating method and historical hosted compiler tier remain unproven.
The retained main CI #333 compiler samples and failures are not relabelled as
successful or erased by these comparisons.

### Controlled observations on 2026-10-10

Each row is one fresh focused client, with three samples for each OFF/default/all-ON
scenario, Windows, Java 25.0.4.1, 16 available processors, render distance 5 and AO
enabled. No failed case was retried. Numbers are whole-client allocated-byte
medians, not allocations attributed to BlockLens. Frame triplets are the actual
frames within the unchanged 30-tick windows.

| Minecraft | Condition | OFF bytes | Default bytes | All ON bytes | Allocation guard | Actual frames OFF/default/ON |
| --- | --- | ---: | ---: | ---: | --- | --- |
| 26.1.2 | c1 | 165326720 | 155297624 | 159380296 | failed | 91,90,90/90,90,89/89,89,90 |
| 26.1.2 | c2-batch | 20382928 | 20029584 | 20766696 | passed | 86,89,90/89,90,90/86,89,89 |
| 26.1.2 | default | 20127512 | 20869640 | 20908384 | passed | 87,89,90/90,90,90/90,89,89 |
| 26.2 | c1 | 24880904 | 25277592 | 25485088 | passed | 88,90,90/89,90,90/90,89,89 |
| 26.2 | c2-batch | 18331408 | 17951840 | 18772984 | passed | 88,90,90/89,88,90/89,88,89 |
| 26.2 | default | 17029992 | 17754440 | 16934248 | passed | 88,90,90/90,90,90/89,89,89 |
| 26.3 | c1 | 21090680 | 21384856 | 22372144 | passed | 90,90,90/90,89,89/89,90,90 |
| 26.3 | c2-batch | 16528848 | 15696552 | 16122856 | passed | 83,88,89/87,89,90/88,90,90 |
| 26.3 | default | 16174504 | 15123608 | 15616480 | passed | 88,90,90/89,89,89/90,90,90 |
| 26.1.2 | default-120 | 28245232 | 27047848 | 28792216 | passed | 177,179,178/179,177,178/178,179,178 |
| 26.1.2 | default-30 | 15419752 | 14988992 | 16374576 | passed | 45,44,45/45,45,45/45,45,45 |
| 26.1.2 | disabled-60 | 130959288 | 128610192 | 130844960 | failed | 88,89,89/89,90,90/90,90,90 |
| 26.2 | default-120 | 25532744 | 25485736 | 25819440 | passed | 177,179,178/179,178,178/179,178,178 |
| 26.2 | default-30 | 14582840 | 14368560 | 15356456 | passed | 44,45,45/45,45,45/44,45,45 |
| 26.2 | disabled-60 | 25560696 | 25056928 | 25590632 | passed | 90,90,89/89,89,90/90,90,90 |
| 26.3 | default-120 | 19948616 | 20016200 | 20472944 | passed | 178,179,178/179,178,179/178,178,178 |
| 26.3 | default-30 | 12549256 | 12939200 | 12846216 | passed | 45,45,45/45,45,45/45,44,45 |
| 26.3 | disabled-60 | 20691584 | 21318704 | 21298832 | passed | 89,90,90/90,90,89/89,89,90 |

All 18 observations had zero incomplete, dropped, invalid or saturated compile
counters. Each retained 157 loaded chunks before and after the windows. The
requested 30/60/120 FPS ceilings produced approximately 44–45/83–91/177–179
actual frames. Thus FPS changes affect this tick-window allocation observation
and must be reported separately. They did not reproduce the 26.1.2 excess on
their own.

The EA-disabled 26.1.2 compiler-worker samples ranged from 63,610,408 to
84,029,376 bytes over 48–59 completed sections; its whole-client medians were
128–131 MB. 26.2 and 26.3 stayed below the existing limit. Together with the C1
case, this supports an optimization-sensitive, version-dependent measurement
boundary. It does not identify a specific allocating method or prove the VM
conditions of historical CI #333. No release performance improvement is claimed.

The current-head shared checks and all-three full native/hosted conformance gates
remain required before closing #43. A focused pass alone does not satisfy this
policy. Failure artifacts preserve bounded fixed-role counters and controlled
images. Public diagnostic logs contain only a fixed set of reviewed exception
types and method symbols. Unknown types/frames produce fixed boolean flags, even
if they imitate a library namespace. Raw client logs, paths and arbitrary
messages are excluded.
