# BlockLens Current Roadmap

Status: **M0-M9 complete / v0.1.0 published**

## Cross-cutting product rules

Every future change must preserve, unless deliberately revised with evidence:

- Minecraft **26.1.2** and **26.2** support
- the same **37-capability** product contract
- client-only behavior
- Java **25+**
- dual-version real-client verification
- reproducible runtime artifacts
- privacy/residue checks
- current JUnit / JaCoCo / PIT thresholds without silent relaxation
- **95,333 B** shared no-growth baseline unless explicitly remeasured and reviewed
- **100 KiB** release budget
- source-pack **<50%** absolute requirement

Performance and artifact size remain continuous gates rather than deferred cleanup tasks.

## Milestone summary

| Milestone | Status | Result |
| --- | --- | --- |
| M0 | ✅ DONE | pinned source baseline and 37-capability contract |
| M1 | ✅ DONE | Java 25, dual-version Fabric scaffold, CI and quality gates |
| M2 | ✅ DONE | shared semantic state engine and thin version adapters |
| M3 | ✅ DONE | 13 Decoration / Orientation capabilities |
| M4 | ✅ core parity | 5 Outline / Fine Visibility capabilities |
| M5 | ✅ core/rendered parity | 18 Resource Highlights; external shader matrix remains separate |
| M6 | ✅ DONE | source-derived Nether Tweaks behavior on exact 27 targets |
| M7 | ✅ DONE | automated all-37 dual-version real-client integration gate |
| M8 | ✅ DONE | performance/load/retention/size hardening |
| M9 | ✅ DONE | verified automated release pipeline and live **v0.1.0** publication |

## M0 — Baseline freeze ✅

Completed:

- pinned source ZIP/RPO hashes
- exact 37-key product contract
- state/target mapping
- source-derived visual references
- duplicate/generated/unreachable asset analysis
- redistribution classification

Evidence: `source-baseline.md`, `capability-map.md`, `capability-contract.tsv`.

## M1 — Java 25 / dual-version Fabric foundation ✅

Completed:

- `common`, `mc26_1_2`, `mc26_2` multi-project layout
- Fabric client-only metadata
- Java 25
- JUnit Jupiter / JUnit Platform
- JaCoCo and PIT quality gates
- GitHub Actions
- Fabric Client GameTest
- reproducible artifact gate
- privacy/residue and runtime-size gates

## M2 — Shared state engine ✅

Completed:

- common `SemanticState`
- thin Minecraft-version adapters
- exact raw-ID target index
- bounded capability lookup
- intentional overlap through capability bitsets

## M3 — Decoration / Orientation ✅

All **13** capabilities are implemented through the unified wrapped-model pipeline. Rendered all-13, reload and OFF-restoration evidence remains part of the automated gate.

Authority: [`m3-visual-semantics.md`](m3-visual-semantics.md).

## M4-M7 — Full core behavior ✅

### M4 — Outline / Fine Visibility

Implemented and regression-tested:

- Blue Ice
- Dead Coral
- Powder Snow
- Sculk Catalyst including `bloom`
- String Tweaks / all 64 Tripwire semantic states

### M5 — Resource Highlighting

- 18/18 exact target bindings
- independent toggles
- active baked base preservation
- shader-OFF/default OpenGL path verified
- dark-area framebuffer evidence
- deterministic active-resource-pack preservation fixture

Remaining external compatibility work is deliberately separate:

- [ ] representative shader-ON support before claiming shader support
- [ ] broader third-party resource-pack matrix

Issue #5 remains the authority for these items. They are outside the advertised v0.1.0 support boundary.

### M6 — Nether Tweaks

- exact 27-target source-derived behavior
- procedural visual grammar
- BlockLens-owned small overlay models
- overlap interaction
- Nether-only framebuffer regression
- resource-resolution gate

### M7 — Automated full-product gate

Both supported Minecraft lines verify:

- all 37 capabilities
- native config save/reload and codec round-trip
- resource reload
- Overworld → Nether → Overworld transition
- Nether-only scenario
- fixed five-feature reference preset
- M5 dark-area scenario
- all-OFF restoration
- bounded zero-world-scan lookup
- M3/M5/M7 evidence
- no server-side BlockLens protocol/component

Authority: [`m4-m7-parity.md`](m4-m7-parity.md).

## M8 — Performance / Load / Retention / Size ✅

M8 established evidence-based regression protection without claiming an unsupported FPS improvement.

### Frozen M8 evidence

```text
resource-reload median guard: <= 6.0 s
terrain-rebuild median guard: <= 2.5 s
allocation median guard:      <= 32 MiB
M8 pre-icon JAR baseline:       88,973 B
```

M8 also removed redundant runtime policy bytecode, reused bounded immutable render instructions, protected lazy overlay caching, captured frame-main percentiles, and verified stable retained model/capability structure across repeated reloads.

Authority: [`m8-performance.md`](m8-performance.md).

**M8 terminal state: DONE.**

## M9 — Release readiness ✅

M9 converted the verified product into a live, reproducible publishing chain.

### Final release identity

```text
release:                 v0.1.0
release target:          a4b63087004d687c3c8223d0d95c6c95fb2c5156
final main CI:           #245 / 34768792311 / GREEN
final Release workflow:  #3 / 34769093202 / GREEN
26.1.2 JAR:              95,332 B
26.2 JAR:                95,333 B
shared no-growth limit:  95,333 B
release budget:         102,400 B (100 KiB)
```

### Completed M9 work

- [x] repository-owned BlockLens icon
- [x] icon referenced by both Fabric metadata files
- [x] icon growth measured before rebaseline
- [x] no-growth baseline frozen at **95,333 B**
- [x] 100 KiB budget preserved
- [x] Java / Fabric / Gradle / JUnit / JaCoCo / PIT / GameTest technology foundation documented
- [x] English README updated
- [x] Japanese README updated
- [x] release/security/redistribution posture documented
- [x] evidence-bounded shader/resource-pack/Vulkan support scope documented
- [x] successful-main-CI-only Release workflow
- [x] no rebuild in Release job
- [x] exact CI runtime artifact handoff
- [x] SHA-256 generation
- [x] metadata/icon/size revalidation before publication
- [x] duplicate-tag successful no-op behavior
- [x] exact post-publication asset-set verification
- [x] M9 repository/release JUnit contracts

### Live incident closed through Graph Loop

The first publication attempt failed because CI runtime artifacts are intentionally `archive:false`, while the initial Release implementation used `gh run download`, which expanded the raw JAR as though it were an artifact container.

PR #20 corrected the handoff by resolving artifact ids and downloading the exact raw payload directly through the GitHub Actions artifact API. JUnit contract coverage now prevents restoring the incompatible download path.

The hotfix then passed:

- PR CI #244: all three jobs GREEN
- squash merge PR #20
- main CI #245: all three jobs GREEN
- Release #3: GREEN
- `v0.1.0`: published
- published JAR bytes/SHA-256: exact match to CI #245

Authority: [`release-readiness.md`](release-readiness.md).

**M9 terminal state: DONE.**

## Next work

The core/release milestone train is complete. Future work should be opened as a new milestone only when there is a concrete product goal.

The existing non-release-blocking compatibility track remains:

- representative shader-ON verification
- broader third-party resource-pack compatibility evidence
- Minecraft 26.2 Vulkan investigation before stable support is claimed

Do not reinterpret those unchecked external combinations as regressions in the verified v0.1.0 support scope.
