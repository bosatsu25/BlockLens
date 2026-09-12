# BlockLens Current Roadmap

Status: **authoritative current implementation order**

## Cross-cutting release rule

Every implementation milestone after M1 must preserve:

- Minecraft **26.1.2** build/test support
- Minecraft **26.2** build/test support
- the same 37-capability product contract
- client-only behavior
- per-version artifact/load/performance reporting

Performance and size are continuous gates, not tasks postponed until the end.

## M0 — Baseline freeze ✅

Goal: make the source resource pack reproducible as a specification before coding.

Tasks:

- [x] Record SHA-256 of supplied ZIP and RPO
- [x] Map 37 RPO keys to source files
- [x] Map source files to Minecraft block IDs/states
- [x] Identify duplicate/generated/unreachable assets
- [x] Capture machine golden references from pinned source hashes, path sets, tooltips, target/state mappings, and dependency counts
- [x] Document ambiguous capabilities, especially `nethertweaks`
- [x] Classify retained third-party assets and licensing risk

Exit criteria:

- every source capability has evidence-backed behavior
- no capability is defined only by guesswork
- baseline measurements are reproducible

Evidence: Issue #1 completed; `source-baseline.md`, `capability-map.md`, and `capability-contract.tsv` are authoritative.

## M1 — Dual-version Fabric/Java 25 scaffold + quality foundation ✅

Goal: establish a minimal buildable client-only mod for **26.1.2 and 26.2** with the full quality contract in place before feature implementation expands.

Tasks:

- [x] Gradle multi-project layout: `common`, `versions/mc26_1_2`, `versions/mc26_2`
- [x] Fabric metadata/runtime artifact per version
- [x] one client entrypoint per artifact
- [x] 37-capability common catalog
- [x] native shared configuration schema
- [x] English/Japanese translation skeleton
- [x] JUnit 5
- [x] JaCoCo selected-policy coverage gate
- [x] PIT selected-policy mutation gate
- [x] `qualityGate` and `ciGate`
- [x] GitHub Actions version matrix / isolated per-version jobs
- [x] client GameTest/smoke path for both versions
- [x] functional-parity/version-parity contracts
- [x] reproducible artifact audit
- [x] per-version JAR byte report/budget
- [x] startup/resource-load performance evidence path and recorded BlockLens initialization baseline
- [x] runtime artifact privacy/residue audit

Exit criteria:

- both version artifacts build cleanly on Java 25
- 37/37 common capability contract PASS
- config round trip PASS
- no server dependency
- automated quality entry points PASS
- first per-version load/size baselines recorded

Verified baseline: [`m1-baseline.md`](m1-baseline.md). GitHub Actions run `34683630501` passed common quality plus both version build/reproducibility/Client GameTest jobs.

## M2 — Shared state engine and version adapters ✅

Goal: represent block orientation/state once and keep mapped Minecraft API differences at thin adapter boundaries.

Semantic set:

- facing
- axis
- half
- shape
- open/closed
- connections
- honey level
- mount face
- powered/attached when required

Tasks:

- [x] pure Java semantic model in `common`
- [x] 26.1.2 state adapter
- [x] 26.2 state adapter
- [x] cross-version oracle tests proving equivalent semantic output
- [x] bounded composable target-to-capability lookup design
- [x] real-client mapped-state oracle after Minecraft registry bootstrap
- [x] simultaneous target overlap support through capability bitsets

Exit criteria:

- pure Java state tests cover relevant combinations
- no duplicated product policy across version projects
- both adapters produce equivalent semantics for shared Minecraft states
- no reflection/classpath feature discovery

Evidence: PR #9 merged as `67ac8f18347a6c6343d0aea6bd51a418dac0d632`; GitHub Actions run `34685138049` passed common JUnit/JaCoCo/PIT plus both 26.1.2 and 26.2 build/reproducibility/Client GameTest/artifact jobs.

## M3 — Decoration/orientation parity ✅

Goal: implement all 13 decoration/orientation capabilities on both versions without copying the source pack's binary assets.

Targets:

- Anvil
- Beehive
- Campfire
- Glazed Terracotta
- Grindstone
- Fence Gate
- Froglight
- Slabs
- Stained Glass
- Stairs
- Trapdoor
- Wood
- Log

Implemented and verified:

- [x] Minecraft-independent render descriptor and visual-cue policy
- [x] exact M0 target catalog: 254 bindings, machine-checked against `capability-contract.tsv`
- [x] 26.1.2 raw registry target index
- [x] 26.2 raw registry target index
- [x] shared procedural quad grammar derived from source visual intent
- [x] Stained Glass modeled as opaque/solid-layer behavior rather than a generic orientation marker
- [x] independent toggle policy for all 13 capabilities
- [x] all 13 enabled simultaneously at pure common-policy level
- [x] model-bake semantic interpretation; no per-frame BlockState reinterpretation
- [x] `WrapperBlockStateModel` integration for 26.1.2
- [x] `WrapperBlockStateModel` integration for 26.2
- [x] model pipeline is zero-world-scan and bounded by exact raw-ID target lookup
- [x] OFF fast path emits the original baked active-resource-pack model directly
- [x] primitive 64-bit enabled mask for render hot-path checks
- [x] Client GameTest target oracle verifies exact target scope and model-pipeline execution
- [x] representative rendered visual parity evidence
- [x] all 13 simultaneously verified in rendered in-game scenarios on 26.1.2
- [x] all 13 simultaneously verified in rendered in-game scenarios on 26.2
- [x] resource reload / active resource-pack visual regression evidence

Current visual/runtime contract: [`m3-visual-semantics.md`](m3-visual-semantics.md).

Exit criteria:

- 13/13 on 26.1.2 and 26.2
- independent toggles
- representative visual parity PASS
- all 13 simultaneously PASS
- startup/reload/runtime no-regression evidence retained

Evidence: PR #11 validation run `34712828259` passed common JUnit/JaCoCo/PIT plus both 26.1.2 and 26.2 build/reproducibility/Client GameTest/artifact jobs on the finalized visual oracle before documentation-only completion commits. Deterministic framebuffer evidence verifies all-13 ON, resource reload, fixed target-ROI differences, active resource-pack preservation, and OFF restoration. The current namespaced time/weather gamerules are verified by both client logs. CI evidence is OpenGL/llvmpipe; 26.2 Vulkan remains a separate later track.

## M4–M7 — Full parity implementation train 🔧

Goal: finish the remaining 24 capabilities and immediately harden the complete 37-capability product. **M4, M5, M6, and M7 are acceptance subtracks, not separate implementation stops.** Work continues on one integration branch/PR until the full-product gates pass.

### Track A — Outline / fine visibility (former M4)

Targets:

- Blue Ice
- Dead Coral
- Powder Snow
- Sculk Catalyst
- String Tweaks

Acceptance:

- [x] source-backed target/state/cue contract frozen in common code
- [x] 24 exact target bindings
- [x] Sculk `bloom` semantic retained
- [x] all 64 tripwire source states represented
- [x] selected JaCoCo/PIT policy gate includes M4 common code
- [ ] 5/5 runtime rendering on 26.1.2
- [ ] 5/5 runtime rendering on 26.2
- [ ] OFF restores normal rendering
- [ ] combined M3+M4 rendered evidence
- [ ] reload/world-transition state cleanup

### Track B — Resource highlighting (former M5)

Targets:

- Obsidian
- Ancient Debris
- Diamond / Deepslate Diamond
- Gold / Deepslate Gold
- Emerald / Deepslate Emerald
- Coal / Deepslate Coal
- Iron / Deepslate Iron
- Copper / Deepslate Copper
- Lapis / Deepslate Lapis
- Redstone / Deepslate Redstone

Acceptance:

- [ ] 18/18 exact target bindings and independent toggles
- [ ] active base resource-pack texture/model preserved where intended
- [ ] shared highlight policy; no per-version product-policy duplication
- [ ] dark-area/high-visibility behavior verified
- [ ] shader OFF PASS on both versions
- [ ] 26.2 OpenGL PASS
- [ ] representative supported shader path verified before support is claimed
- [ ] Vulkan remains a separate experimental track and is not implied by OpenGL success

### Track C — Nether Tweaks (former M6)

Target scope is the M0 exact 27-block Nether-oriented set.

Acceptance:

- [ ] exact source behavior documented from pinned evidence
- [ ] no guessed behavior
- [ ] independent toggle
- [ ] 26.1.2 and 26.2 adapters/rendering
- [ ] interaction with orientation, outline/string, and resource highlighting

### Track D — Full-product hardening (former M7)

Required scenarios per supported Minecraft line:

- [ ] all 37 capabilities supported
- [ ] all 37 capabilities ON simultaneously
- [ ] supplied reference preset reproduced
- [ ] config save/reload
- [ ] resource reload
- [ ] world join/leave
- [ ] dimension changes
- [ ] active third-party resource pack
- [ ] shader OFF
- [ ] supported representative shader path(s)
- [ ] no server-side BlockLens
- [ ] no custom gameplay/network requirement
- [ ] target lookup remains bounded / zero-world-scan
- [ ] OFF fast path remains intact

Additional 26.2 tracks:

- [ ] OpenGL
- [ ] Vulkan experimental verification where available

Full-train exit criteria:

- Functional Parity Gate PASS for all 37 capabilities
- Version Parity Gate PASS on 26.1.2 and 26.2
- all-37 simultaneous visual/integration regression PASS
- reload/world/dimension transitions do not retain stale render state
- active resource-pack preservation PASS
- compatibility matrix accurately documents verified graphics paths

Implementation rule: do **not** stop merely because Track A/B/C individually passes. Continue directly to the full-product Track D gate unless a concrete source ambiguity or failing regression requires diagnosis.

## M8 — Performance, load, and size hardening

Goal: optimize only after full behavior is stable, while continuous budgets have been protecting earlier milestones.

Measurements per version:

- runtime JAR size/top entries
- startup/load duration
- BlockLens initialization where measurable
- resource reload duration
- allocation rate
- median/P95/P99 frame time
- cache/retained geometry size

Release size gates per artifact:

```text
required: < 1,183,433 bytes
stretch:  <= 716,800 bytes
```

Tasks:

- [ ] freeze no-growth baseline per version
- [ ] remove remaining repeated assets/state expansion
- [ ] verify lazy initialization
- [ ] verify bounded lookup/caches
- [ ] losslessly optimize retained assets
- [ ] verify reproducible compact JAR packaging

Exit criteria:

- hard size gate PASS on both artifacts
- M7 behavior unchanged
- startup/reload/runtime performance is at least neutral vs frozen pre-optimization BlockLens baseline in tested scenarios

## M9 — Release readiness

Tasks:

- [ ] licensing/redistribution audit
- [ ] clean Java 25 release build
- [ ] all automated quality gates
- [ ] per-version artifact privacy/security audit
- [ ] README user guide
- [ ] compatibility/version notes
- [ ] SHA-256 for both artifacts
- [ ] release notes
- [ ] clean Prism/Fabric smoke test for 26.1.2 and 26.2

Exit criteria:

- exactly intended release artifacts
- both supported lines green
- supported graphics/shader/resource-pack scope accurately documented
