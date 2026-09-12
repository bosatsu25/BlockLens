# BlockLens Resource-Pack-to-Mod Migration Plan

Status: **authoritative migration strategy**

## Goal

Rebuild the visual value of the supplied AMATERAS resource pack as a Fabric client mod while preserving all 37 source capabilities, supporting **Minecraft 26.1.2 and 26.2**, and reducing each runtime artifact below 50% of the source ZIP size.

This is a **black-box / behavior-first reimplementation strategy**. The source pack establishes observable visual behavior and supported block-state combinations. The mod must not preserve the source pack's internal file explosion merely for convenience.

## M0 — Freeze the source baseline

Before production implementation:

- record source ZIP/RPO hashes,
- record ZIP size/file counts,
- map every RPO key to affected files,
- map every RPO key to affected block IDs/states,
- capture representative screenshots/golden references,
- classify source assets as behavior-defining, minimal/reusable visual asset, duplicate/static state expansion, or packaging overhead.

Current measured baseline:

```text
ZIP size:               2,366,865 bytes
ZIP entries:            4,535
JSON files:             3,135
PNG files:              1,020
RPO rule files:           336
uncompressed payload:   2,209,147 bytes
compressed payload:     1,279,563 bytes
container overhead:     1,087,302 bytes
```

## M1 — Create dual-version mod/quality scaffold

Create a client-only Fabric/Java 25 multi-project for:

- Minecraft 26.1.2
- Minecraft 26.2

Shape:

```text
common/
versions/mc26_1_2/
versions/mc26_2/
```

Add:

- per-version `fabric.mod.json`/runtime artifact,
- common 37-capability catalog,
- shared config schema,
- JUnit 5,
- selected JaCoCo/PIT gates,
- `qualityGate` / `ciGate`,
- per-version client smoke/GameTest,
- functional/version parity contracts,
- reproducible artifact audit,
- per-version JAR-size reporting,
- startup/resource-reload evidence path.

Acceptance:

- clean Java 25 builds for both targets,
- 37 capability IDs contract-tested,
- same config semantics on both versions,
- no server dependency,
- first per-version size/load baselines recorded.

## M2 — State/orientation family + version adapters

Implement a stable common semantic model and thin Minecraft adapters.

Shared semantics:

- facing,
- axis,
- half,
- shape,
- open/closed,
- connection,
- powered/attached where required.

Do not duplicate product policy in version directories.

Acceptance:

- same semantic oracle passes for 26.1.2 and 26.2,
- no runtime reflection feature discovery,
- target lookup is bounded/deterministic.

## M3 — Decoration/orientation parity

Implement all 13 decoration/orientation capabilities on both versions with common render semantics and version-specific render glue only where required.

Acceptance per capability:

- affected block/state set matches source baseline,
- visual meaning is equivalent,
- enable/disable independent,
- no change when disabled,
- representative resource-pack compatibility,
- both Minecraft lines PASS.

## M4 — Outline/fine-visibility family

Implement:

- Blue Ice,
- Dead Coral,
- Powder Snow,
- Sculk Catalyst,
- String Tweaks.

Acceptance:

- thin/hidden targets readable,
- state/connection behavior correct,
- deterministic simultaneous composition,
- both versions PASS.

## M5 — Resource highlighting family

Implement all 18 resource/highlight capabilities using:

```text
active block texture/model
        +
BlockLens resource cue
```

Acceptance:

- every source resource independently configurable,
- normal/deepslate variants remain separately addressable,
- dark-area visibility intentional/tested,
- shader OFF verified,
- supported shader paths verified before claims,
- 26.2 OpenGL verified,
- 26.2 Vulkan tracked separately as experimental compatibility evidence.

## M6 — Nether Tweaks

Capture exact source behavior first. Do not infer the feature solely from its name.

Acceptance:

- explicit target/state/visual mapping,
- parity evidence,
- independent config,
- both versions PASS.

## M7 — Compatibility and accessibility hardening

After source parity exists:

- active third-party resource packs,
- representative shader packs,
- all-capabilities-ON,
- world/resource reload transitions,
- config parity across versions,
- 26.2 OpenGL/Vulkan tracks kept distinct.

Enhancements beyond parity must be labeled BlockLens-native rather than silently changing source equivalence.

## M8 — Load/runtime/size optimization

Optimization is continuously guarded from M1, then deliberately hardened here.

Order:

1. compare per-version startup/load baselines,
2. compare resource reload duration,
3. inspect runtime JAR/top entries,
4. remove remaining static duplication,
5. verify lazy initialization,
6. verify bounded lookup/caches,
7. losslessly optimize required PNGs,
8. profile render/update paths,
9. rerun parity/version tests.

Per-version size targets:

- required: `< 1,183,433 bytes`,
- stretch: `<= 716,800 bytes`.

A reduction is rejected if parity, cross-version correctness, diagnostics, startup/reload, or runtime performance regresses.

## M9 — Release readiness

Before public distribution:

- clean-room build,
- full automated quality suite,
- functional/version parity report,
- shader/resource-pack/backend compatibility matrix,
- per-version runtime artifact audit,
- no private/local data,
- license/redistribution review,
- release notes identifying exact Minecraft/Fabric/Java support,
- SHA-256 for both artifacts.
