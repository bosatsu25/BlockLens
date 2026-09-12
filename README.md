# BlockLens

BlockLens is a Minecraft Java Edition client-side visual inspection mod project.

The project starts from the visual utility provided by the supplied AMATERAS resource pack, but the goal is **not** to wrap the resource pack inside a JAR. BlockLens will re-design the behavior as code-driven rendering and state inspection so that the same user value can be delivered with fewer static assets, clearer configuration, better maintainability, and measurable performance characteristics.

## Project goal

Preserve the complete feature capability of the source resource pack while rebuilding it as a standalone mod.

Priority order:

1. Functional equivalence
2. Correctness and rendering-state accuracy
3. Compatibility and fail-soft behavior
4. Performance and memory efficiency
5. Maintainability and testability
6. Runtime artifact size

The initial hard size target is:

- Source resource-pack ZIP: **2,366,865 bytes**
- 50% threshold: **1,183,433 bytes**
- BlockLens runtime JAR acceptance target: **< 1,183,433 bytes**
- Stretch target: **<= 700 KiB**, only if functional parity and quality gates remain green

## Source baseline

The supplied resource pack currently contains:

- 4,535 ZIP entries
- 3,135 JSON files
- 1,020 PNG files
- 336 `.rpo` rule files
- 2,209,147 bytes uncompressed data
- 1,279,563 bytes compressed payload
- approximately 1,087,302 bytes of ZIP/container overhead

This means a major part of the current size is caused by thousands of small static files. BlockLens should replace repeated state/model/resource definitions with shared code where doing so preserves appearance and behavior.

## Capability baseline

The source configuration exposes **37 capabilities** across four groups:

- Decoration / orientation: 13
- Resource highlighting: 18
- Outline / visibility: 4
- Other visual tweaks: 2

All 37 capabilities are part of the functional-parity baseline even when the current RPO preset has them disabled.

The supplied RPO preset currently enables five capabilities:

- Blue Ice
- Dead Coral
- Powder Snow
- Sculk Catalyst
- String Tweaks

See [`knowledge/current/product-spec.md`](knowledge/current/product-spec.md) for the authoritative current specification.

## Core design direction

```text
Minecraft block state / world state
            |
            v
     BlockLens state model
            |
      +-----+-------------------+
      |                         |
      v                         v
Orientation / shape       Highlight / outline
      |                         |
      +-----------+-------------+
                  v
          Shared render layer
                  |
                  v
      Vanilla or active pack texture
            + BlockLens overlay
```

BlockLens should prefer the texture already supplied by Minecraft or the user's active resource pack, then add dynamic visual information on top. It should not duplicate thousands of state-specific assets when the same result can be represented safely in code.

## Non-goals

- Embedding the original resource pack unchanged inside a mod JAR
- Cutting features merely to hit a size number
- Requiring a server-side BlockLens component
- Sending custom gameplay packets solely for visual features
- Making unrelated gameplay automation part of this project
- Treating the currently enabled five RPO options as the only supported features

## Repository rules

Project-specific engineering rules are in [`AGENTS.md`](AGENTS.md).

Current specifications and decisions live under [`knowledge/current/`](knowledge/current/). Historical or superseded material must not override `current`.

## Status

**M0 — Baseline/specification capture.**

No production implementation should begin until the resource-pack capability map and functional-parity contracts are fixed.
