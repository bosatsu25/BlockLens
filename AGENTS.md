# BlockLens Engineering Rules

This repository is used to design, implement, review, test, and release BlockLens.

## Source of truth

- `knowledge/current/` is the authoritative current specification.
- `archive/`, `deprecated/`, and `superseded/` material, if added later, is historical only.
- Do not revive an older behavior because it existed in the source resource pack if the current specification intentionally replaces it.

## Supported platform contract

BlockLens currently has two first-class Minecraft targets:

- Minecraft Java Edition 26.1.2
- Minecraft Java Edition 26.2

Both use Fabric and the Java 25 baseline unless the current specification explicitly changes it.

A feature is not complete if it only works on one supported target without an explicit documented version exception.

Prefer shared pure product logic plus thin Minecraft-version adapters. Do not copy the complete implementation into separate version trees.

## Core principles

- Preserve existing user-visible capability unless a current specification explicitly changes it.
- Do not delete features to meet the JAR-size target.
- Respect unrelated existing changes.
- Features are not mutually exclusive unless the specification explicitly says so.
- Assume all visual features can be enabled simultaneously and verify their interaction.
- Prefer shared internal rendering/state infrastructure when multiple features solve the same technical problem, while keeping independent user controls when their use cases differ.
- Client-only behavior must remain client-only unless a future current specification explicitly changes that contract.
- Keep config semantics and the 37-capability contract aligned across supported Minecraft versions.

## Quality priorities

Review and verify at least:

- correctness
- regression risk
- Minecraft/Fabric compatibility
- cross-version parity
- rendering state
- resource-pack compatibility
- shader/backend compatibility where relevant
- startup/load performance
- resource-reload performance
- runtime/render performance
- memory/allocation behavior
- configuration persistence
- maintainability
- testability
- release artifact size

## Automated quality rule

BlockLens should provide deterministic automated gates comparable in intent to ChiseTweaks, adapted to BlockLens scope:

- JUnit 5 unit/contract tests
- functional-parity contracts
- version-parity contracts
- selected JaCoCo coverage verification
- selected PIT mutation testing
- per-version client smoke/GameTests
- reproducible artifact audit
- per-version JAR-size budgets
- structural performance/load contracts

Changes that affect Minecraft integration must build/test against both supported version targets before completion.

## Optimization rule

Artifact size is subordinate to functional parity and correctness.

A smaller JAR is not an improvement if it:

- removes a capability,
- changes a visual contract unintentionally,
- breaks either supported Minecraft version,
- harms diagnosis/debuggability,
- increases startup/reload/runtime cost without evidence,
- or makes configuration harder to understand.

Performance claims require measurement. Keep startup/load, resource reload, runtime/render, memory, and JAR byte size as separate budgets.

## Load-performance rules

Normal BlockLens startup must avoid unnecessary work:

- no runtime classpath/reflection feature discovery
- no startup network/update check
- no telemetry initialization
- no normal-path legacy RPO parsing
- no eager geometry construction for disabled features
- no unbounded registry/resource scans

Prefer static catalogs, bounded lookup tables, lazy initialization, event/change-driven invalidation, and retained rendering with explicit cleanup.

## Implementation workflow

For meaningful changes, aim for:

1. establish or update the current specification,
2. implement shared logic first where applicable,
3. implement/adjust both version adapters,
4. build both targets,
5. automated tests/quality gates,
6. regression/functional-parity/version-parity checks,
7. performance or rendering verification where relevant,
8. per-version artifact audit,
9. update `knowledge/current/` if behavior or architecture changed.

For bug fixes, add a regression test whenever the failure can be represented mechanically.

## Resource-pack migration rule

The source AMATERAS resource pack is a behavioral and visual reference, not an instruction to copy its internal structure into the mod.

Preferred transformation:

```text
static state-specific JSON/PNG duplication
            ->
shared state interpretation + shared renderer + minimal unique assets
```

Before public distribution, confirm redistribution/licensing rights for any third-party texture, model, text, or other asset that remains in the runtime JAR.
