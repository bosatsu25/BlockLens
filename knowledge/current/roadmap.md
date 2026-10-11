# BlockLens Current Roadmap

Status: **M0-M9 and P0-P5 complete / v0.3.0 release candidate / three-version main verified**

## Current release and development boundary

Target release candidate: **v0.3.0**, shipping **57 independently configurable capabilities** across **Minecraft 26.1.2, 26.2, and 26.3**. It encompasses all P0-P5 migrations (lightweight visuals, world analyzers, scene filters, builder assist, ore extension APIs, synchronized conventional tags, pinned Sodium terrain integration, private reference-pack validation, and comprehensive codebase hardening).

Historical published release: [v0.2.2](https://github.com/bosatsu25/BlockLens/releases/tag/v0.2.2), target `46ae5ccb1e60c92acd796acb0e2b10e740f9dcfc`, shipping 40 capabilities for 26.1.2 / 26.2.

Current size policy is **204,800 B (200 KiB)** for both development and release gates, explicitly authorized by the user on 2026-10-11. The **1,183,432 B** absolute ceiling and runtime/quality guards are unchanged. Older 100/140/150 KiB limits and measurements below are historical evidence. See [remaining-delivery.md](remaining-delivery.md).

## Remaining implementation graph

| Track | Issue | Dependency and completion boundary |
| --- | --- | --- |
| Lightweight visual / comfort migration | [#42](https://github.com/bosatsu25/BlockLens/issues/42) | Completed by PR #46: CI #320 passed all four jobs for PR head `d4f313491ada47788bc46459e4f34a8f8396ef5a`, then merged as `5f992800cde79236d2d47fea304dbfe5831307dc`. That merge's main CI #321 allocation failure remains #43. Six controls, numeric details and three-version UI/render evidence preserve the superseded #32/#33 requirements. |
| Bounded Analyzer | [#34](https://github.com/bosatsu25/BlockLens/issues/34) | Completed by PR #50, head `3df8117983c60db2baf1b9b194cccc305cce3039`, merged as `90105fb702b30a7619300cd668836a1314f4815b`. PR CI #328 and main CI #329 passed all four jobs on their first attempts. The three hosted analyzer artifacts contain 75 checksum-verified UI/render images and completion manifests. Five default-OFF analyzers and 19 numeric fields; no release claimed. See world-analyzers.md. |
| Scene Filter | [#35](https://github.com/bosatsu25/BlockLens/issues/35) | Completed by PR #51, latest head `8b1121044010d9a29a007b75ca2dd5964126549b` and CI #332, then merged as `fd9eefb2ce6ef06fd091e649dde9ace3b094a2ba`. The PR passed all four jobs; the merge's CI #333 allocation failure remains recorded under #43. Two default-OFF controls bring the development runtime to 53 capabilities. See scene-filter.md. |
| Builder Assist / Inspector | [#36](https://github.com/bosatsu25/BlockLens/issues/36) | Completed by PR #53 / CI #343: four advisory tools and exact optional Litematica/MaLiLib pairs across all three Minecraft releases. See [builder-assist.md](builder-assist.md). |
| Extension compatibility | [#37](https://github.com/bosatsu25/BlockLens/issues/37) | Completed by PR #53 / CI #343 & PR #55: versioned registration, synchronized conventional tags, and pinned Sodium terrain integration. See [extension-compatibility.md](extension-compatibility.md). |
| Masa / deprecation decisions | [#38](https://github.com/bosatsu25/BlockLens/issues/38) | Completed evaluation by PR #49 / CI #326: eight explicit scope exclusions, separate read-only #36 candidate, ChiseTweaks retained without deprecation or archival. |
| External compatibility | [#31](https://github.com/bosatsu25/BlockLens/issues/31) | Completed by PR #54, PR #55, and PR #56: Pinned Shader ON/OFF, Masa and Vulkan workflows, plus verified native execution of all five private reference packs across all three Minecraft targets. See [external-compatibility.md](external-compatibility.md). |

Diagnostic implementation: [PR #45](https://github.com/bosatsu25/BlockLens/pull/45) and [PR #47](https://github.com/bosatsu25/BlockLens/pull/47) are merged. #43 was reopened after main CI #321 reproduced the 26.1.2 allocation failure. PR #47 adds bounded role/main-pass attribution and preserves failing samples; exact-head CI #322 and main CI #323 passed all four jobs on their first attempts. These passing runs do not identify or resolve the cause; guard values are unchanged.

[PR #52](https://github.com/bosatsu25/BlockLens/pull/52) defines the measured,
test-only C2/foreground conformance condition while preserving all M8 thresholds
and historical failures. Its CI #336 analyzer timeout exposed a separate
test-fixture lifetime issue, repaired with identity-scoped replacement inputs
and native regression controls; see [world-analyzers.md](world-analyzers.md#dimension-return-fixture-repair--pr-52).
Final PR CI #339 and merged-main CI #340 passed all four jobs on their first attempts.
Issue #43 is completed under its explicitly permitted measured test-environment exception:
Java 25 C2 foreground compilation, requested 60 FPS, unchanged 32 MiB / 30-tick / three-sample
conditions. The historical allocation cause remains unproven and failed samples remain recorded.
Main commit `6c355e3831a6df4dded2bc46ac641911480da31d` and
[CI #340](https://github.com/bosatsu25/BlockLens/actions/runs/38065290177) are the starting
baseline for this remaining delivery; they do not prove its new functionality.


Issue #22 records delivered P0 only. Planned capabilities remain unimplemented until their own contracts, all-three-version evidence and PRs are complete. [#39](https://github.com/bosatsu25/BlockLens/issues/39) synchronizes these repository-owned status documents.

## Cross-cutting product rules

Every future change must preserve, unless deliberately revised with evidence:

- Minecraft **26.1.2**, **26.2** and **26.3** support in current main
- the frozen original **37-capability source contract** as a permanent regression boundary
- additive post-v0.1 capabilities may expand the current runtime without rewriting that historical source contract
- client-only behavior
- Java **25+**
- three-version real-client verification
- reproducible runtime artifacts
- privacy/residue checks
- current JUnit / JaCoCo / PIT thresholds without silent relaxation
- v0.1.0 historical icon-inclusive baseline: **95,333 B**
- published v0.2.0 cross-platform baseline: **96,257 B**
- current user-authorized development ceiling: **204,800 B**
- **200 KiB** release ceiling
- source-pack **<50%** absolute requirement

Performance and artifact size remain continuous gates rather than deferred cleanup tasks.

## Historical milestone summary

| Milestone / phase | Status | Result |
| --- | --- | --- |
| M0 | ✅ DONE | pinned source baseline and 37-capability contract |
| M1 | ✅ DONE | Java 25, dual-version Fabric scaffold, CI and quality gates |
| M2 | ✅ DONE | shared semantic-state engine and thin version adapters |
| M3 | ✅ DONE | 13 Decoration / Orientation capabilities |
| M4 | ✅ core parity | 5 Outline / Fine Visibility capabilities |
| M5 | ✅ core/rendered parity | original 18 Resource Highlights; external shader matrix remains separate |
| M6 | ✅ DONE | source-derived Nether Tweaks behavior on exact original 27 targets |
| M7 | ✅ DONE | automated all-37 dual-version real-client integration gate |
| M8 | ✅ DONE | performance/load/retention/size hardening |
| M9 | ✅ DONE | verified automated release pipeline and live **v0.1.0** publication |
| P0 | ✅ DONE | Chise material/hidden/fine-line/Nether overlap shipped in v0.2.0 |
| P1-P5 | ⏳ PLANNED | selected ChiseTweaks visual, comfort, analyzer, builder and compatibility capabilities |

## M0 — Baseline freeze ✅

Completed:

- pinned source ZIP/RPO hashes
- exact 37-key product contract
- state/target mapping
- source-derived visual references
- duplicate/generated/unreachable asset analysis
- redistribution classification
- later raw-source audit confirms **337 observed sidecars / 336 effective gated roots**, **4,101 reachable raw assets**, and **95 source-residue candidates**

Evidence: `source-baseline.md`, `capability-map.md`, `capability-contract.tsv`, `source-raw-parity-audit.md`.

The first 37 source keys remain immutable historical evidence. New functionality is additive and must not silently alter their config identity, defaults, or source-derived target coverage.

## M1-M3 — Foundation and Orientation / State ✅

Completed:

- `common`, `mc26_1_2`, `mc26_2` multi-project layout
- Fabric client-only metadata
- Java 25
- JUnit Jupiter / JUnit Platform
- JaCoCo and PIT quality gates
- GitHub Actions and Fabric Client GameTest
- reproducible artifact, privacy/residue and size gates
- common `SemanticState`
- thin Minecraft-version adapters
- exact raw-ID target index
- bounded capability lookup
- intentional overlap through capability bitsets
- all **13** Decoration / Orientation capabilities through the unified wrapped-model pipeline

Authority: [`m3-visual-semantics.md`](m3-visual-semantics.md).

## M4-M7 — Original full-product behavior ✅

### M4 — Outline / Fine Visibility

Original source contract:

- Blue Ice
- Dead Coral
- Powder Snow
- Sculk Catalyst including `bloom`
- String Tweaks / all 64 Tripwire semantic states

### M5 — Resource Highlighting

Original v0.1 source contract:

- **18/18** exact Resource capabilities
- independent toggles
- active baked-base preservation
- shader-OFF/default OpenGL path verified
- dark-area framebuffer evidence
- deterministic active-resource-pack preservation fixture

Remaining external compatibility work is deliberately separate and tracked in Issue #31 (the successor to completed #5):

- representative shader-ON support before claiming shader support
- broader third-party resource-pack matrix

### M6 — Nether Tweaks

Original v0.1 source contract:

- exact **27-target** source-derived behavior
- procedural visual grammar
- BlockLens-owned small overlay models
- overlap interaction
- Nether-only framebuffer regression
- resource-resolution gate

### M7 — Automated original product gate

Both supported Minecraft lines verify the frozen 37-capability product, config save/reload, resource reload, dimension transitions, reference preset, visual evidence, all-OFF restoration and bounded zero-world-scan lookup.

Authority: [`m4-m7-parity.md`](m4-m7-parity.md).

## M8 — Performance / Load / Retention / Size ✅

Frozen coarse regression guards:

```text
resource-reload median guard: <= 6.0 s
terrain-rebuild median guard: <= 2.5 s
allocation median guard:      <= 32 MiB
M8 pre-icon JAR baseline:       88,973 B
```

M8 removed redundant runtime policy bytecode, reused bounded immutable render instructions, protected lazy overlay caching, captured frame-main percentiles, and verified stable retained model/capability structure across repeated reloads.

Authority: [`m8-performance.md`](m8-performance.md).

## M9 — Release readiness ✅

Final v0.1.0 release identity:

```text
release:                 v0.1.0
release target:          a4b63087004d687c3c8223d0d95c6c95fb2c5156
final main CI:           #245 / 34768792311 / GREEN
final Release workflow:  #3 / 34769093202 / GREEN
26.1.2 JAR:              95,332 B
26.2 JAR:                95,333 B
v0.1.0 no-growth limit:  95,333 B
release budget:         102,400 B (100 KiB)
```

The release workflow publishes exact successful-main-CI artifacts without rebuilding, revalidates metadata/icon/size, generates SHA-256 checksums, and performs duplicate-tag successful no-op handling.

Authority: [`release-readiness.md`](release-readiness.md).

## P0 — ChiseTweaks visual-overlap convergence ✅

Authority: Issue **#22**, PR **#23**, and [`chisetweaks-migration.md`](chisetweaks-migration.md).

### Product direction

BlockLens remains the product and visual-engine foundation. ChiseTweaks is a source of selected capabilities and behavior only; it is **not** a runtime dependency and its heavier feature framework is not copied into BlockLens.

P0 absorbs the near-duplicate visual responsibilities into the existing compiled catalog/model-bake engine:

- Chise material highlighting → BlockLens Resource capabilities
- Chise hidden-surface behavior → existing BlockLens visibility responsibilities
- Chise fine-line behavior → String Tweaks
- Chise Nether palette behavior → Nether Tweaks

### Additive P0 runtime contract

P0 adds:

- Crying Obsidian as an independent Resource capability
- Nether Gold Ore as an independent Resource capability
- Nether Quartz Ore as an independent Resource capability
- Tripwire Hook under String Tweaks
- Polished Basalt under Nether Tweaks

Published v0.2.0 contract:

```text
capabilities:                    40
capability-to-target bindings:  328
unique block targets:           322
original frozen capabilities:    37
original frozen bindings:       323
original frozen unique targets: 320
P0 no-growth baseline:       96,257 B
release budget:             102,400 B
```

The first 37 capability IDs/config keys/defaults remain stable. The three new independent capabilities are appended; no original capability is repurposed.

### P0 quality gates

- [x] explicit ChiseTweaks → BlockLens P0 mapping
- [x] 40-capability common contract
- [x] 328-binding / 322-target current runtime contract
- [x] Tripwire Hook state-adapter coverage
- [x] Polished Basalt state-adapter coverage
- [x] Resource-only performance scenario updated from 18 historical to 21 current Resource capabilities
- [x] dark-area real-client scene updated to exercise all 21 current Resource capabilities
- [x] raw source audit documented
- [x] executable raw-source parity contract protects 37 keys / 336 effective roots / 323 original bindings / 320 original unique targets
- [x] only reviewed additive target drift is allowed on original capabilities (`tripwire_hook`, `polished_basalt`)
- [x] P0 JAR growth measured and 100 KiB ceiling preserved
- [x] README English/Japanese updated
- [x] latest common JUnit / JaCoCo / PIT GREEN
- [x] latest 26.1.2 real-client gate GREEN
- [x] latest 26.2 real-client gate GREEN
- [x] PR #23 final self-review / ready-for-review
- [x] merge after all required gates are GREEN
- [x] publish the additive release as v0.2.0 without mutating v0.1.0

### P0 size evidence

Published v0.2.0 artifacts:

- 26.1.2: **96,248 B**
- 26.2: **96,248 B**
- release-era cross-platform no-growth baseline: **96,257 B**
- main CI: **#270 / `34801429999` — GREEN**
- Release: **#31 / `34802548055` — GREEN**
- release target: `b440d43904e2a93236549efc571b7cc127622352`

The release-era **96,257 B** baseline and 100 KiB release ceiling remain historical v0.2.0 evidence; current policy is stated above.

## Historical post-v0.2.0 stability and performance hardening

The current source adds the bounded responsive settings screen, crash-safe config persistence, safe lazy publication, hot-path instruction/enum reuse, compact retained descriptor arrays, all-40 M8 evidence, and durable failure artifacts. Local dual-version Client GameTest is GREEN.

Measured local runtime artifacts are **101,877 B (26.1.2) / 101,913 B (26.2)**. The reviewed source no-growth baseline is **101,913 B** and the hard **102,400 B / 100 KiB** ceiling remains unchanged. Full evidence and measurement limitations are recorded in [`stability-performance-review.md`](stability-performance-review.md).

## P1-P5 — Next consolidation phases ⏳

The historical P1-P5 scope is now tracked by #42 and #34-#38; #22 is closed for delivered P0:

- **P1:** Glass Highlight, Kelp Highlight, Bright Concrete and Bright Chest (#42)
- **P2:** Low Fire, Handheld Size
- **P3:** bounded world-overlay/analyzer engine: Ancient Debris, Lava Source, Beacon Range, Lightning Rod Range, Villager Job Site Links
- **P4:** Block/Entity Filter, Inspector, Placement State Preview, Pattern Consistency, selected Litematica integration
- **P5:** Modded Ore API, renderer compatibility and individually justified Masa integrations

Automation remains outside product scope unless deliberately reconsidered later.

## Compatibility track

The following remain outside the verified default OpenGL support boundary and are tracked in #31:

- representative shader-ON verification
- broader third-party resource-pack compatibility evidence
- Minecraft 26.2 / 26.3 Vulkan investigation before support is claimed

Do not reinterpret those unchecked external combinations as regressions in the verified default/shader-OFF OpenGL path.

The six additive controls and their historical 140 KiB development baseline are specified in [lightweight-visuals.md](lightweight-visuals.md). The current accepted ceiling is 200 KiB; see [remaining-delivery.md](remaining-delivery.md). No new release is implied by implementation or a merged PR.
