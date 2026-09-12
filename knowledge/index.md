# BlockLens Knowledge Index

## Authoritative current documents

Read these before implementation or review:

1. [`current/source-baseline.md`](current/source-baseline.md) — pinned AMATERAS ZIP/RPO hashes, measured structure, current preset, M0 baseline status
2. [`current/capability-map.md`](current/capability-map.md) — completed M0 mapping of all 37 source capabilities, targets, state semantics, dependency counts, and licensing posture
3. [`current/capability-contract.tsv`](current/capability-contract.tsv) — machine-readable 37-capability contract used to bootstrap automated parity tests
4. [`current/m1-baseline.md`](current/m1-baseline.md) — verified dual-version CI, artifact-size/SHA, reproducibility, Client GameTest, and BlockLens initialization baseline
5. [`current/product-spec.md`](current/product-spec.md) — product goal, complete 37-capability baseline, supported Minecraft lines, non-goals
6. [`current/architecture.md`](current/architecture.md) — target runtime architecture and shared rendering/state design
7. [`current/versioning.md`](current/versioning.md) — Minecraft 26.1.2/26.2 multi-version build and adapter strategy
8. [`current/performance-strategy.md`](current/performance-strategy.md) — startup/load, resource reload, runtime, memory, and JAR-size budgets
9. [`current/migration-plan.md`](current/migration-plan.md) — resource-pack-to-mod migration strategy and phases
10. [`current/quality-strategy.md`](current/quality-strategy.md) — automated tests, functional parity, rendering, compatibility, performance, and artifact gates
11. [`current/roadmap.md`](current/roadmap.md) — implementation order and acceptance criteria
12. [`current/engineering-loop.md`](current/engineering-loop.md) — authoritative Graph Loop for discovery, implementation, verification, diagnosis/fix cycles, PR/CI, and completion states

## Rule

`current` is the source of truth.

If future documents are placed under `archive`, `deprecated`, or `superseded`, use them only for historical investigation. They must not override current behavior or architecture.
