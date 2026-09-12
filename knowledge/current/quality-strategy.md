# BlockLens Quality Strategy

Status: **authoritative current quality strategy**

## 1. Quality objective

BlockLens must become smaller than the source resource pack **without losing or silently changing the visual capabilities being migrated**.

Artifact size is a release gate, but it is not the primary quality goal.

Priority:

1. correctness
2. functional parity
3. rendering stability
4. compatibility
5. configuration persistence
6. performance/memory
7. maintainability/testability
8. artifact size

## 2. Functional Parity Gate

The most important regression gate.

At minimum, CI must detect accidental changes to:

- total capability count: **37**,
- source RPO keys,
- category membership,
- supported target/block-state mapping once captured,
- native config keys/defaults/preset mapping,
- feature enable/disable independence,
- required rendering semantics,
- client-only metadata,
- Fabric entrypoints,
- optional compatibility boundaries.

A smaller JAR with a missing capability is a failed build.

## 3. Test layers

### Layer A — Pure Java unit tests

Target deterministic policy/state code:

- block-state interpretation,
- orientation/axis mapping,
- stair/slab/trapdoor semantics,
- capability catalog,
- config policy,
- composition/precedence rules,
- source-preset conversion.

Use equivalence partitioning and boundary-value analysis for state/property combinations where applicable.

### Layer B — Contract tests

Verify repository/product contracts:

- exactly 37 capability IDs,
- exact source-key set,
- no duplicate capability IDs,
- every capability has user-facing name/description keys,
- every capability has config representation,
- current RPO reference preset contains exactly the expected five enabled keys,
- no feature is accidentally marked server-required.

### Layer C — Resource/render descriptor tests

Without requiring a full Minecraft render, verify that target states produce expected render descriptors.

Examples:

```text
stairs[facing=north,half=top,shape=outer_left]
 -> expected semantic descriptor
```

```text
log[axis=x]
 -> expected axis marker
```

Golden descriptor tests are preferable to brittle internal implementation assertions.

### Layer D — Minecraft integration/smoke tests

Where automation is practical:

- mod loads on Minecraft 26.1.2,
- no missing mixin/entrypoint/class errors,
- config loads/saves,
- resource reload completes,
- world join/leave does not leak retained state,
- toggling features does not require restart unless explicitly documented.

### Layer E — Visual regression

For source-parity features, maintain representative golden references.

Verify:

- correct target is marked,
- direction/state cue is correct,
- OFF means vanilla/resource-pack rendering is not altered by BlockLens,
- all relevant faces/states remain correct,
- combinations do not hide one another unexpectedly.

Visual regression can begin as a documented/manual checklist if automated screenshot comparison is not yet reliable, but high-risk features should move toward machine-comparable evidence.

### Layer F — Compatibility matrix

At minimum verify:

- vanilla/no extra resource pack,
- representative third-party resource pack,
- shader OFF,
- shader ON for supported representative shader packs,
- all 37 capabilities enabled simultaneously.

Do not advertise a compatibility claim that has not been tested.

### Layer G — Performance and memory

Measure before claiming improvement.

Useful metrics:

- median frame time,
- P95/P99 frame time,
- allocation rate in render/update paths,
- heap growth across world transitions,
- resource reload duration,
- startup duration,
- retained geometry/cache counts,
- CPU time attributable to BlockLens.

Required scenarios:

1. BlockLens installed, all features OFF
2. one representative orientation feature ON
3. one representative outline/fine feature ON
4. resource highlights ON
5. all features ON

## 4. Artifact size gate

Source baseline:

```text
2,366,865 bytes
```

Hard BlockLens runtime-JAR maximum:

```text
1,183,432 bytes
```

(`50%` is intentionally strict: the JAR must be **less than** half of 2,366,865 bytes.)

Stretch goal:

```text
716,800 bytes (700 KiB)
```

CI should report:

- current JAR bytes,
- delta from previous baseline,
- largest JAR entries,
- total assets vs classes vs metadata size.

Do not fail normal feature development merely for temporary growth below the hard release ceiling unless a milestone explicitly freezes a tighter no-growth budget.

## 5. Source-pack baseline integrity

Record and pin a SHA-256 for the source ZIP/RPO when those files are added to a private test-fixture location or when hashes are documented.

The source baseline should not silently change during optimization.

If the source pack is updated intentionally:

1. treat it as a new baseline version,
2. diff capability/assets,
3. update parity fixtures,
4. rerun migration assessment.

## 6. Configuration persistence

Test:

- default values,
- current RPO reference preset,
- save/reload round trip,
- unknown key handling,
- missing key migration,
- corrupt config fallback,
- upgrade migration when config schema changes.

Configuration failure must not corrupt the game instance.

## 7. Fail-soft policy

Optional integrations must isolate failure.

Examples:

- Iris missing -> standard render path still works.
- Shader-state query fails -> disable only shader-specific optimization/path and remain usable.
- A single capability cannot initialize -> report/disable that capability rather than crashing all BlockLens if safely possible.

Do not swallow errors silently. Keep actionable logging without spamming every frame/tick.

## 8. Security/privacy/release artifact checks

Before release ensure the JAR/repository contains no accidental:

- local absolute paths,
- server IPs,
- tokens/secrets,
- account identifiers,
- logs/crash reports,
- world saves,
- caches,
- unrelated user files.

BlockLens has no reason to include telemetry, arbitrary external process execution, or silent network update behavior in the baseline product.

## 9. Licensing gate

Before public release, inventory every retained non-code asset and confirm its redistribution/license status.

If rights are unclear:

- recreate the visual cue independently,
- use runtime-generated geometry,
- or require the original pack separately rather than redistributing its asset.

Private development status does not remove the need to resolve this before publication.

## 10. Definition of Done

A capability is not done when code merely exists.

Done means, as applicable:

```text
spec captured
 -> implementation
 -> unit/contract tests
 -> build
 -> visual/state regression
 -> compatibility check
 -> performance sanity check
 -> knowledge update
```
