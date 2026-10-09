# BlockLens Current Roadmap

Status: **M0-M9 complete / v0.2.0 published / post-release stability hardening locally verified**

## Cross-cutting product rules

Every future change must preserve, unless deliberately revised with evidence:

- Minecraft **26.1.2** and **26.2** support
- the frozen original **37-capability source contract** as a permanent regression boundary
- additive post-v0.1 capabilities may expand the current runtime without rewriting that historical source contract
- client-only behavior
- Java **25+**
- dual-version real-client verification
- reproducible runtime artifacts
- privacy/residue checks
- current JUnit / JaCoCo / PIT thresholds without silent relaxation
- v0.1.0 historical icon-inclusive baseline: **95,333 B**
- published v0.2.0 cross-platform baseline: **96,257 B**
- current reviewed source no-growth baseline: **101,913 B**
- **100 KiB** release budget
- source-pack **<50%** absolute requirement

Performance and artifact size remain continuous gates rather than deferred cleanup tasks.

## Milestone summary

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

Remaining external compatibility work is deliberately separate and remains tracked in Issue #5:

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

The release-era **96,257 B** baseline remains historical v0.2.0 evidence. The 100 KiB release ceiling is unchanged.

## Post-v0.2.0 stability and performance hardening

The current source adds the bounded responsive settings screen, crash-safe config persistence, safe lazy publication, hot-path instruction/enum reuse, compact retained descriptor arrays, all-40 M8 evidence, and durable failure artifacts. Local dual-version Client GameTest is GREEN.

Measured local runtime artifacts are **101,877 B (26.1.2) / 101,913 B (26.2)**. The reviewed source no-growth baseline is **101,913 B** and the hard **102,400 B / 100 KiB** ceiling remains unchanged. Full evidence and measurement limitations are recorded in [`stability-performance-review.md`](stability-performance-review.md).

## P1-P5 — Next consolidation phases ⏳

Planned in Issue #22 after the published P0 baseline:

- **P1:** Glass Highlight, Kelp Highlight, Bright Concrete; evaluate Bright Chest separately
- **P2:** Low Fire, Handheld Size
- **P3:** bounded world-overlay/analyzer engine: Ancient Debris, Lava Source, Beacon Range, Lightning Rod Range, Villager Job Site Links
- **P4:** Block/Entity Filter, Inspector, Placement State Preview, Pattern Consistency, selected Litematica integration
- **P5:** Modded Ore API, renderer compatibility and individually justified Masa integrations

Automation remains outside product scope unless deliberately reconsidered later.

## Compatibility track

The following remain non-release-blocking relative to the verified v0.2.0 support boundary and are tracked separately:

- representative shader-ON verification
- broader third-party resource-pack compatibility evidence
- Minecraft 26.2 Vulkan investigation before stable support is claimed

Do not reinterpret those unchecked external combinations as regressions in the verified default/shader-OFF OpenGL path.
