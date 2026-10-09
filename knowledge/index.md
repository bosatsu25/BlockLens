# BlockLens Knowledge Index

## Authoritative current documents

Read these before implementation or review:

1. [`current/source-baseline.md`](current/source-baseline.md) — pinned AMATERAS ZIP/RPO hashes, measured structure, current preset, M0 baseline status
2. [`current/capability-map.md`](current/capability-map.md) — completed M0 mapping of all 37 source capabilities, targets, state semantics, dependency counts, and licensing posture
3. [`current/capability-contract.tsv`](current/capability-contract.tsv) — machine-readable frozen 37-capability AMATERAS contract used to bootstrap automated parity tests
4. [`current/amateras-raw-parity-audit.md`](current/amateras-raw-parity-audit.md) — raw RPO/source-file graph vs supplied BlockLens JAR audit, including the 337/336 sidecar distinction, 4,101 reachable assets, 95 residue candidates, and per-capability mapping
5. [`current/m1-baseline.md`](current/m1-baseline.md) — verified dual-version CI, artifact-size/SHA, reproducibility, Client GameTest, and BlockLens initialization baseline
6. [`current/product-spec.md`](current/product-spec.md) — original product goal and AMATERAS-derived 37-capability baseline, supported Minecraft lines, non-goals
7. [`current/architecture.md`](current/architecture.md) — target runtime architecture and shared rendering/state design
8. [`current/versioning.md`](current/versioning.md) — Minecraft 26.1.2/26.2 multi-version build and adapter strategy
9. [`current/performance-strategy.md`](current/performance-strategy.md) — startup/load, resource reload, runtime, memory, and JAR-size budgets
10. [`current/migration-plan.md`](current/migration-plan.md) — original resource-pack-to-mod migration strategy and phases
11. [`current/chisetweaks-migration.md`](current/chisetweaks-migration.md) — post-v0.1.0 ChiseTweaks→BlockLens consolidation direction, P0 mapping, additive capability/target contract, and architecture invariants
12. [`current/stability-performance-review.md`](current/stability-performance-review.md) — post-v0.2.0 crash-safety, hot-path allocation, retained-memory, responsive-settings, real-client, and artifact-size evidence
13. [`current/quality-strategy.md`](current/quality-strategy.md) — automated tests, functional parity, rendering, compatibility, performance, and artifact gates
14. [`current/roadmap.md`](current/roadmap.md) — implementation order and acceptance criteria
15. [`current/engineering-loop.md`](current/engineering-loop.md) — authoritative Graph Loop for discovery, implementation, verification, diagnosis/fix cycles, PR/CI, and completion states
16. [`current/m3-visual-semantics.md`](current/m3-visual-semantics.md) — completed M3 source-derived visual intent, procedural cue policy, zero-scan model pipeline, resource-pack restoration rules, and verified dual-version framebuffer evidence
17. [`current/m4-visual-semantics.md`](current/m4-visual-semantics.md) — source-derived outline/fine-line semantics, original five-capability scope, and tripwire state contract
18. [`current/m4-m7-parity.md`](current/m4-m7-parity.md) — implemented M4–M6 core rendering, original unified 37-capability pipeline, dual-version M7 automated integration evidence, and explicit compatibility boundaries
19. [`current/m5-active-resource-pack.md`](current/m5-active-resource-pack.md) — deterministic non-vanilla active resource-pack preservation evidence, dual-version metrics, manual screenshot review, and explicit compatibility boundaries
20. [`current/m8-performance.md`](current/m8-performance.md) — dual-version resource-reload/allocation observations, Resource Highlight instruction-reuse hardening, raw CI evidence, and measurement limitations
21. [`current/release-readiness.md`](current/release-readiness.md) — M9/v0.1.0 and v0.2.0 release contracts, technology foundation, artifact baselines, licensing/security audit, exact-CI-artifact publishing, checksums, and compatibility support boundary

22. [`current/settings-ui.md`](current/settings-ui.md) — AMATERAS-style native settings redesign, editing semantics, shared screen architecture and verification boundaries

## Historical-baseline rule

The AMATERAS M0 documents remain authoritative **historical baseline evidence** for the first 37 capabilities. Post-v0.1.0 additive product work must not rewrite those source facts.

`current/amateras-raw-parity-audit.md` provides an additional raw-file-level cross-check: the effective RPO behavior must remain completely mapped even though BlockLens intentionally does not redistribute the original JSON/PNG graph byte-for-byte.

Current runtime expansion and ChiseTweaks consolidation are tracked separately in `current/chisetweaks-migration.md`, Issue #22, and the later roadmap evidence.

## Rule

`current` is the source of truth.

If future documents are placed under `archive`, `deprecated`, or `superseded`, use them only for historical investigation. They must not override current behavior or architecture.
