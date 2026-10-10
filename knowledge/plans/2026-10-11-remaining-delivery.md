# Remaining Builder and Compatibility Delivery Plan

> **For agentic workers:** Use superpowers:subagent-driven-development for implementation and independent review. Each owner works in its assigned files; the coordinator integrates shared entrypoints and verification gates.

**Goal:** Implement Issues #36 and #37, exercise the available external compatibility matrix in #31, then merge reviewed changes with exact-head and merged-main evidence.

**Architecture:** Retain the shared pure policy / thin Fabric adapter split. Builder tools consume bounded local read models. Ore extensions compile bounded registrations and synchronized conventional tags into retained models. External integrations remain typed, optional and explicitly versioned.

**Tech Stack:** Java 25, Fabric for Minecraft 26.1.2 / 26.2 / 26.3, JUnit 5, JaCoCo, PIT, Fabric Client GameTest, GitHub Actions.

**Spec:** `knowledge/current/builder-assist.md`, `knowledge/current/extension-compatibility.md`, `knowledge/current/external-compatibility.md`, and the unchanged cross-cutting contracts in `engineering-loop.md` and `performance-strategy.md`.

## Global Constraints

- Support Minecraft 26.1.2, 26.2 and 26.3 with Java 25.
- Preserve all original 37 identities and the existing 53 capability positions, defaults and behavior.
- Builder tools are advisory/read-only: no world writes, placement/break actions, custom packets, server companion or automation.
- Four builder controls append default-OFF in Other: `builder.block_inspector`, `builder.placement_preview`, `builder.pattern_consistency`, `builder.schematic_comparison`.
- Pattern scope is an explicit reference-centered 17 × 9 × 17 domain; at most 256 read/cursor attempts per tick, including validation. Unknown/unloaded input remains unknown.
- Integrations use exact identifiers/typed APIs; no reflection or classpath discovery, no per-frame registry scan.
- All M8 thresholds, samples, warmup and windows remain unchanged; full conformance retains test-only `c2-batch` and requested 60 FPS.
- The user's 2026-10-11 follow-up explicitly replaces the 150 KiB condition with 200 KiB (204,800 B). Development and release artifact gates use this accepted ceiling; exact artifact bytes and growth are still reported. All other quality, runtime and absolute-size guards remain intact.
- Preserve failure evidence; a requested backend or enabled option is insufficient proof that it actually ran.

## Review Focus

- Unsaved settings must not become runtime state when opening the builder panel: config/draft tests and native discard/save test.
- World, reference, target or model replacement must clear stale observations: Pattern tests and native lifecycle checks.
- Placement prediction must preserve world and inventory and report unsupported inputs: native stateful-block and negative controls.
- Conflicting/oversized/missing tag registrations must isolate extension failures without changing vanilla targets: pure bounds tests and native tag reload/removal checks.
- Optional dependencies and Vulkan may silently fall back: absent/unsupported integration tests and actual-device/backend evidence.

## Task 1: Builder Assist / Inspector (#36)

**Owner:** builder_assist; coordinator owns edits to existing catalog/settings/entrypoints/translations.

**Files:** new `common/.../core/builder/`, shared `BuilderAssistClient`, `BuilderAssistScreen`, isolated typed optional adapter, `gametest/.../BuilderAssistOracle.java`, and builder specification.

- [ ] Document exact source-derived behavior, controls, bounds and version pins before production edits.
- [ ] Add failing behavioral tests for state comparison, bounded scans, unknown input, reference replacement, all-OFF work and session cleanup.
- [ ] Implement pure read models and native read-only adapters, retaining deterministic bounds.
- [ ] Add four independent default-OFF keys, EN/JA names, an unbound shortcut and a settings Open entry. The panel reads saved runtime settings only.
- [ ] Exercise representative stateful placement families, block/entity inspection, Pattern, optional schematic comparison, persistence and compact EN/JA UI on all three clients.

## Task 2: Bounded Ore Extensions (#37)

**Owner:** extension_contract; coordinator owns existing metadata, smoke and workflow wiring.

**Files:** common extension policy/API and tests, shared adapter/mixin/model helpers, both native target indexes, test-only ore registration/resources/oracle, and extension specification.

- [ ] Document API v1 and fixed conventional ore tag mappings before integrations.
- [ ] Add failing tests for validation, bounds, duplicate/conflicting registrations, freeze semantics and immutable snapshots.
- [ ] Implement exact non-vanilla registrations and bounded synchronized-tag compilation. Limit identifiers, targets and total wrapped states; preserve the active base model.
- [ ] Verify tag add/remove, explicit registration coexistence, unsupported/absent integrations, reload/disconnect and Scene Filter composition on all three clients.
- [ ] Record the exact supported contract and measured runtime byte delta without blanket named-mod/renderer claims.

## Task 3: External Compatibility (#31)

**Owner:** external_compat; coordinator owns CI/Gradle integration.

- [ ] Pin and hash available Iris/Sodium, shader, Litematica/MaLiLib and uploaded/reference pack inputs; retain third-party pack bytes outside runtime/repository when redistribution is not established.
- [ ] Add a dedicated reproducible compatibility runner and evidence contract for shader ON/OFF, representative pack combinations/order, and actual Vulkan on 26.2/26.3.
- [ ] Capture Resource Highlight and non-Resource visuals, actual active dependencies/packs/backend, restoration and reload results.
- [ ] Fix concrete observed incompatibilities with regression evidence. Record genuinely unavailable configurations as blocked, never passed.

## Task 4: Review, CI and Merge

**Owner:** coordinator plus independent reviewers.

- [ ] Run common behavioral/config tests; compile all native APIs; inspect artifact growth before adjusting a development baseline.
- [ ] Review task diffs for specification compliance, lifecycle/compatibility risks, performance and test quality; repair findings and recheck affected behavior.
- [ ] Open focused PR(s), pass current-head common and all-three-version full native gates, and verify evidence artifact hashes and manifests.
- [ ] Merge with expected-head protection, verify merged-main CI/artifacts, and update issues/README/current roadmap to the evidenced completion boundary.

## Starting Evidence

Main starts clean at `6c355e3831a6df4dded2bc46ac641911480da31d`; CI #340 passed common and all three native jobs on attempt 1. Runtime JARs are 141,298 / 141,566 / 141,628 B. PR #52 and Issue #43 are complete. No other PR was open at discovery.
