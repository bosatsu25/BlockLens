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
13. [`current/m3-visual-semantics.md`](current/m3-visual-semantics.md) — completed M3 source-derived visual intent, procedural cue policy, zero-scan model pipeline, resource-pack restoration rules, and verified dual-version framebuffer evidence
14. [`current/m4-visual-semantics.md`](current/m4-visual-semantics.md) — source-derived outline/fine-line semantics, exact five-capability scope, and tripwire 64-state contract
15. [`current/m4-m7-parity.md`](current/m4-m7-parity.md) — implemented M4–M6 core rendering, unified 37-capability pipeline, dual-version M7 automated integration evidence, and explicit remaining compatibility boundaries
16. [`current/m5-active-resource-pack.md`](current/m5-active-resource-pack.md) — deterministic non-vanilla active resource-pack preservation evidence, dual-version metrics, manual screenshot review, and explicit compatibility boundaries
17. [`current/m8-performance.md`](current/m8-performance.md) — dual-version resource-reload/allocation observations, Resource Highlight instruction-reuse hardening, raw CI evidence, and measurement limitations
18. [`current/release-readiness.md`](current/release-readiness.md) — M9 release contract, technology foundation, icon-inclusive artifact baseline, licensing/security audit, exact-CI-artifact publishing, checksums, and compatibility support boundary

## Rule

`current` is the source of truth.

If future documents are placed under `archive`, `deprecated`, or `superseded`, use them only for historical investigation. They must not override current behavior or architecture.
