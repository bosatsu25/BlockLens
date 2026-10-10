# BlockLens Quality Strategy

Status: **authoritative current quality strategy**

## 1. Quality objective

BlockLens must become smaller and faster to load than the source resource-pack approach **without losing or silently changing the visual capabilities being migrated**, and must preserve that contract on all three targets: **Minecraft 26.1.2, 26.2 and 26.3**.

Priority:

1. correctness
2. functional parity
3. rendering stability
4. cross-version compatibility
5. configuration persistence
6. startup/reload/runtime performance and memory
7. maintainability/testability
8. artifact size

## 2. Functional Parity Gate

CI must detect accidental changes to:

- frozen original source capability count: **37**; current additive runtime count: **40**
- exact source RPO keys
- category membership
- supported target/block-state mappings once captured
- native config keys/defaults/preset mapping
- feature independence
- required rendering semantics
- client-only metadata
- Fabric entrypoints
- optional compatibility boundaries

The same contract must pass for 26.1.2, 26.2 and 26.3.

A smaller or faster artifact with a missing capability is a failed build.

## 3. Automated test stack

BlockLens should adopt the strongest reusable parts of the ChiseTweaks quality model while keeping the implementation proportional to this smaller product.

### Layer A — JUnit 5 pure Java tests

Target deterministic common code:

- block-state interpretation
- orientation/axis mapping
- stair/slab/trapdoor semantics
- capability catalog
- config policy/migration
- composition/precedence
- source-preset conversion
- performance budget policy
- version-parity policy

Use parameterized tests, equivalence partitioning, boundary-value analysis, and state-transition tests where appropriate.

### Layer B — Repository/contract tests

Verify:

- preserve the original 37 capability IDs/source keys, plus current additive identities (40 runtime capabilities)
- no duplicate IDs
- every capability has translations/config representation
- reference preset contains exactly the expected five enabled keys
- client-only metadata on all three version artifacts
- all three version projects exist and are wired into CI
- config schema is identical across versions unless an explicit exception fixture exists
- no accidental startup networking/telemetry contract

### Layer C — Render-descriptor/state tests

Verify semantic output independently of Minecraft rendering where possible.

Examples:

```text
stairs[facing=north,half=top,shape=outer_left]
 -> expected descriptor
```

```text
log[axis=x]
 -> expected axis marker
```

Prefer golden semantic descriptors over assertions tied to internal class structure.

### Layer D — JaCoCo coverage gate

Use JaCoCo on selected deterministic product logic rather than chasing repository-wide vanity coverage.

Coverage targets should include:

- capability catalog/policy
- state interpretation
- config migration
- composition rules
- performance/version budget policy

Freeze numeric thresholds only after the first stable scaffold is measured.

### Layer E — PIT mutation gate

Use PIT for semantic policy classes where a surviving mutation would represent a meaningful defect.

Do not dilute mutation scores with mapping glue, rendering coordinates, or Fabric wrappers that are better validated by boundary/integration tests.

### Layer F — Minecraft client GameTest / smoke tests

Run against **all three** supported Minecraft lines.

At minimum verify:

- game/client launches with BlockLens
- no missing mixin/entrypoint/class errors
- config loads/saves
- representative feature can initialize/toggle
- resource reload completes
- world join/leave clears state
- no server-side BlockLens requirement

Version-specific smoke tasks must fail independently so one supported line cannot hide the other's failure.

### Layer G — Visual regression

Maintain representative golden evidence for source-parity features.

Verify:

- correct target/cue
- correct direction/state
- OFF leaves base rendering unchanged by BlockLens
- simultaneous features compose correctly
- representative resource-pack compatibility

Start manual only where automation is not yet reliable; promote high-risk cases toward machine-comparable screenshot/descriptor evidence.

### Layer H — Compatibility matrix

At minimum:

- 26.1.2 vanilla/resource-pack baseline
- 26.2 OpenGL baseline
- 26.2 Vulkan experimental verification track
- representative third-party resource pack
- shader OFF
- supported representative shader ON
- all current capabilities ON simultaneously while preserving the frozen original 37-capability contract

Do not advertise untested compatibility.

### Layer I — Performance/memory

Measure separately per Minecraft version:

- startup duration
- BlockLens initialization where measurable
- resource reload duration
- median/P95/P99 frame time
- allocation rate
- heap/cache/retained geometry behavior
- runtime JAR bytes

For 26.2, do not combine OpenGL and Vulkan results into one number.

## 4. CI entry points

Target Gradle entry points modeled after ChiseTweaks:

```text
qualityGate
ciGate
```

`qualityGate` should aggregate, as appropriate:

- JUnit
- JaCoCo report/verification
- PIT
- functional-parity contracts
- version-parity contracts
- artifact-size budget checks

`ciGate` should add:

- reproducible builds
- all three version runtime artifacts
- source artifact where retained
- per-version artifact audit

GitHub Actions should run isolated version matrix jobs or equivalent per-version tasks so failures are attributable to 26.1.2, 26.2 or 26.3.

## 5. Fast CI without weakening gates

Heavy tests should not be repeated unnecessarily, but verified artifacts must never be promoted from unrelated code.

Allowed optimizations include:

- Gradle dependency/build cache
- path-based classification for documentation-only changes
- reuse/promotion only when source tree identity/provenance is proven
- common pure-Java tests once plus version-specific smoke/build jobs for each target

Do not skip functional parity or version build verification merely to shorten CI.

## 6. Artifact size gate

Source baseline:

```text
2,366,865 bytes
```

For **each** supported runtime JAR:

```text
hard maximum: < 1,183,433 bytes
stretch goal: <= 716,800 bytes (700 KiB)
```

CI reports per version:

- current bytes
- delta from frozen version baseline
- largest compressed entries
- class/assets/metadata contribution

After the first verified implementation, freeze a per-version no-growth budget similar to ChiseTweaks: baseline + explicitly allowed growth, capped by the hard maximum.

## 7. Startup/resource-load gates

CI and local performance tooling must distinguish byte size from load cost.

Structural CI gates should reject regressions such as:

- runtime reflection/classpath feature discovery
- startup network/update checks
- normal-path legacy RPO parsing
- eager geometry creation for disabled features
- unbounded registry/resource scanning
- repeated immutable catalog creation in hot paths

Numeric startup/reload thresholds will be frozen only after reproducible M1 baselines exist.

## 8. Source baseline integrity

Pin source ZIP/RPO SHA-256. Intentional source-pack updates require:

1. new baseline version
2. capability/asset diff
3. parity fixture update
4. full migration reassessment

## 9. Configuration persistence

Test:

- defaults
- reference preset
- save/reload round trip
- unknown/missing keys
- corrupt fallback
- schema upgrades
- same persisted semantics on all three Minecraft versions

Configuration failure must not corrupt the game instance.

## 10. Fail-soft policy

Examples:

- Iris missing -> standard path works
- shader/backend query fails -> isolate only that optional path
- one capability initialization failure -> isolate/report it when safe rather than crashing all BlockLens

Errors remain actionable but must not spam every frame/tick.

## 11. Security/privacy/release artifact checks

Release artifacts/repository must not accidentally contain:

- local paths
- server IPs
- secrets/tokens
- account identifiers
- logs/crash reports
- saves/caches
- unrelated user files

Baseline BlockLens has no telemetry, arbitrary process execution, or silent network updater.

## 12. Licensing gate

Inventory retained non-code assets before public release. If rights are unclear, recreate the visual cue independently, generate it at runtime, or require the original pack separately.

## 13. Definition of Done

A capability is done only after applicable:

```text
spec
 -> implementation in common/version adapter
 -> unit/contracts
 -> three-version build
 -> integration/render regression
 -> compatibility/performance sanity
 -> artifact audit
 -> knowledge update
```
