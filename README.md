# BlockLens

BlockLens is a Minecraft Java Edition client-side visual inspection mod project.

The project starts from the visual utility provided by the supplied AMATERAS resource pack, but the goal is **not** to wrap the resource pack inside a JAR. BlockLens re-designs the behavior as code-driven rendering and state inspection so that the same user value can be delivered with fewer static assets, clearer configuration, better maintainability, automated quality gates, and measurable load/runtime performance.

## Supported Minecraft versions

BlockLens is designed from the start for two first-class Fabric targets:

- **Minecraft Java Edition 26.1.2**
- **Minecraft Java Edition 26.2**
- Java baseline: **25**
- Side: **client only**

Each Minecraft line will receive its own verified runtime JAR. The product/config/capability contract should remain common across both versions, with only thin Minecraft/Fabric adapter code differing where required.

See [`knowledge/current/versioning.md`](knowledge/current/versioning.md).

## Project goal

Preserve the complete feature capability of the source resource pack while rebuilding it as a standalone mod.

Priority order:

1. Functional equivalence
2. Correctness and rendering-state accuracy
3. Cross-version compatibility and fail-soft behavior
4. Startup/resource-reload/runtime performance and memory efficiency
5. Maintainability and automated testability
6. Runtime artifact size

## Automated quality direction

BlockLens adopts the strongest reusable quality ideas from ChiseTweaks, scaled to this project:

- JUnit 5 unit/contract tests
- exact 37-capability Functional Parity Gate
- 26.1.2 / 26.2 Version Parity Gate
- selected JaCoCo coverage verification
- selected PIT mutation testing
- per-version client smoke/GameTests
- reproducible artifact audit
- config persistence/migration tests
- per-version JAR-size budgets
- structural startup/load-performance contracts

Target Gradle entry points:

```text
qualityGate
ciGate
```

## Lightweight and load-performance goals

BlockLens treats these as separate budgets:

1. startup / initialization cost
2. resource reload cost
3. runtime / render cost and allocations
4. memory / retained state
5. runtime JAR byte size

Normal startup should avoid runtime reflection/classpath feature scanning, startup networking, telemetry, legacy RPO parsing, and eager geometry creation for disabled features.

Performance improvements are measured rather than assumed. Baselines are recorded separately for 26.1.2 and 26.2.

Minecraft 26.2 also has a separate experimental Vulkan verification track; OpenGL success is not treated as proof of Vulkan compatibility.

See [`knowledge/current/performance-strategy.md`](knowledge/current/performance-strategy.md).

## Size targets

Source resource-pack ZIP: **2,366,865 bytes**

For **each supported runtime JAR**:

- hard acceptance target: **< 1,183,433 bytes**
- stretch target: **<= 700 KiB (716,800 bytes)**

A smaller JAR fails if functional parity, version parity, rendering correctness, diagnostics, or measured performance regresses.

## Source baseline

The supplied resource pack currently contains:

- 4,535 ZIP entries
- 3,135 JSON files
- 1,020 PNG files
- 336 `.rpo` rule files
- 2,209,147 bytes uncompressed data
- 1,279,563 bytes compressed payload
- approximately 1,087,302 bytes of ZIP/container overhead

A major part of the current size comes from thousands of small static files. BlockLens should replace repeated state/model/resource definitions with shared code where doing so preserves appearance and behavior.

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

See [`knowledge/current/product-spec.md`](knowledge/current/product-spec.md).

## Core design direction

```text
Minecraft/Fabric 26.1.2 or 26.2
            |
            v
     thin version adapter
            |
            v
     BlockLens common state model
            |
      +-----+-------------------+
      |                         |
      v                         v
Orientation / shape       Highlight / outline
      |                         |
      +-----------+-------------+
                  v
          shared render semantics
                  |
                  v
        version render adapter
                  |
                  v
      Vanilla or active pack texture
            + BlockLens overlay
```

BlockLens should prefer the texture already supplied by Minecraft or the user's active resource pack, then add dynamic visual information on top. It should not duplicate thousands of state-specific assets when the same result can be represented safely in code.

## Non-goals

- Embedding the original resource pack unchanged inside a mod JAR
- Cutting features merely to hit a size number
- Duplicating the whole codebase for each Minecraft version
- Requiring a server-side BlockLens component
- Sending custom gameplay packets solely for visual features
- Making unrelated gameplay automation part of this project
- Treating the currently enabled five RPO options as the only supported features

## Repository rules

Project-specific engineering rules are in [`AGENTS.md`](AGENTS.md).

Current specifications and decisions live under [`knowledge/current/`](knowledge/current/). Historical or superseded material must not override `current`.

## Status

**M0 — Baseline/specification capture.**

The supported-version, automated-test, performance, and artifact-size architecture is now fixed in current specs. Production feature implementation should begin only after the source resource-pack capability map and functional-parity baseline are complete.
