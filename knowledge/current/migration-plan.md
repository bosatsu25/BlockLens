# BlockLens Resource-Pack-to-Mod Migration Plan

Status: **authoritative migration strategy**

## Goal

Rebuild the visual value of the supplied AMATERAS resource pack as a Fabric client mod while preserving all 37 source capabilities and reducing the runtime artifact below 50% of the source ZIP size.

This is a **black-box / behavior-first reimplementation strategy**. The source pack is used to establish observable visual behavior and supported block-state combinations. The mod should not simply preserve the source pack's internal file structure.

## M0 — Freeze the source baseline

Before production implementation:

- record the source ZIP hash,
- record ZIP size and file counts,
- map every RPO key to affected files,
- map every RPO key to affected block IDs and block states,
- capture representative screenshots/golden references,
- classify each source asset as:
  - behavior-defining,
  - reusable/minimal unique visual asset,
  - duplicate/generated/static state expansion,
  - or irrelevant packaging overhead.

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

## M1 — Create the minimal mod scaffold

Create a client-only Fabric project for Minecraft 26.1.2 / Java 25 with:

- `fabric.mod.json`,
- a single client entrypoint,
- native BlockLens configuration,
- capability catalog containing all 37 source keys,
- unit-test infrastructure,
- CI build/test/artifact audit.

Acceptance:

- clean Java 25 build,
- client-only metadata,
- 37 capability IDs present and contract-tested,
- all capabilities configurable even before render implementations are complete.

## M2 — State/orientation family

Implement the common state interpreter and marker renderer for the 13 decoration/orientation capabilities.

Prioritize reusable semantics:

- facing,
- axis,
- half,
- shape,
- open/closed,
- connection,
- powered/attached where source behavior requires it.

Do not implement 13 unrelated rendering systems if the visual semantics can be shared.

Acceptance per capability:

- affected block/state set matches source baseline,
- visual meaning is equivalent,
- enable/disable is independent,
- no change when disabled,
- resource-pack compatibility verified for representative packs.

## M3 — Outline/fine-visibility family

Implement:

- Blue Ice,
- Dead Coral,
- Powder Snow,
- Sculk Catalyst,
- String Tweaks.

Use one or more shared outline/fine-line renderers as appropriate.

Acceptance:

- thin/hidden targets remain readable,
- adjacent/connected state remains correct where source behavior depends on it,
- simultaneous enablement has deterministic composition.

## M4 — Resource highlighting family

Implement all 18 resource/highlight capabilities.

Preferred strategy:

```text
active block texture/model
        +
BlockLens resource cue
```

rather than shipping complete replacement textures for every ore/state.

Acceptance:

- each source resource can be independently enabled,
- normal and deepslate variants remain separately addressable because the source configuration exposes them separately,
- dark-area visibility is intentional and tested,
- shader-on and shader-off behavior is verified before compatibility is claimed.

## M5 — Nether Tweaks

Capture exact source behavior first because the single RPO key is broader than a precise contract.

Do not guess the behavior based only on the key name.

Acceptance:

- explicit mapped targets and visuals,
- parity evidence stored in baseline data/tests,
- independent configuration retained.

## M6 — Compatibility and accessibility hardening

After source parity exists:

- test with active third-party resource packs,
- test representative shader packs,
- introduce color/pattern accessibility options only if they do not erase source parity,
- verify all-capabilities-ON mode,
- verify world/resource reload transitions.

Enhancements beyond parity must be labeled as BlockLens-native behavior rather than silently changing the source-equivalence contract.

## M7 — Size and performance optimization

Only after parity gates are green:

1. measure runtime JAR,
2. identify largest entries,
3. remove remaining static duplication,
4. losslessly optimize required PNGs,
5. inspect JSON/model generation,
6. profile render/update paths,
7. repeat parity tests.

Targets:

- required: `< 1,183,433 bytes`,
- stretch: `<= 716,800 bytes`.

A size reduction is rejected if any parity/quality gate regresses.

## M8 — Release readiness

Before public distribution:

- clean-room build from repository,
- full automated test suite,
- functional parity report,
- shader/resource-pack compatibility matrix,
- runtime artifact audit,
- no private/local data,
- license/redistribution review for every retained third-party asset,
- release notes describing supported Minecraft/Fabric/Java versions.
