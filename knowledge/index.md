# BlockLens Knowledge Index

## Authoritative current documents

Read these before implementation or review:

1. [`current/source-baseline.md`](current/source-baseline.md) — pinned AMATERAS ZIP/RPO hashes, measured structure, current preset
2. [`current/product-spec.md`](current/product-spec.md) — product goal, complete 37-capability baseline, supported Minecraft lines, non-goals
3. [`current/architecture.md`](current/architecture.md) — target runtime architecture and shared rendering/state design
4. [`current/versioning.md`](current/versioning.md) — Minecraft 26.1.2/26.2 multi-version build and adapter strategy
5. [`current/performance-strategy.md`](current/performance-strategy.md) — startup/load, resource reload, runtime, memory, and JAR-size budgets
6. [`current/migration-plan.md`](current/migration-plan.md) — resource-pack-to-mod migration strategy and phases
7. [`current/quality-strategy.md`](current/quality-strategy.md) — automated tests, functional parity, rendering, compatibility, performance, and artifact gates
8. [`current/roadmap.md`](current/roadmap.md) — implementation order and acceptance criteria

## Rule

`current` is the source of truth.

If future documents are placed under `archive`, `deprecated`, or `superseded`, use them only for historical investigation. They must not override current behavior or architecture.
