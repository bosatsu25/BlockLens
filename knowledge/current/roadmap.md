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

Evidence: Issue #1 completed; `source-baseline.md`, `capability-map.md`, and `capability-contract.tsv` are authoritative.

## M1 — Dual-version Fabric/Java 25 scaffold + quality foundation ✅

Goal: establish a minimal buildable client-only mod for **26.1.2 and 26.2** with the quality contract in place before feature implementation expands.

Completed:

- [x] Gradle multi-project layout: `common`, `versions/mc26_1_2`, `versions/mc26_2`
- [x] Fabric metadata/runtime artifact per version
- [x] one client entrypoint per artifact
- [x] 37-capability common catalog
- [x] native shared configuration schema
- [x] English/Japanese translation skeleton
- [x] JUnit 5 + selected JaCoCo/PIT gates
- [x] `qualityGate` and `ciGate`
- [x] dual-version GitHub Actions + Client GameTest
- [x] functional/version parity contracts
- [x] reproducible artifact audit
- [x] JAR byte budget
- [x] startup/load baseline
- [x] runtime artifact privacy/residue audit

Verified baseline: [`m1-baseline.md`](m1-baseline.md). GitHub Actions `34683630501` passed common quality plus both version jobs.

## M2 — Shared state engine and version adapters ✅

Goal: represent orientation/state once and keep mapped Minecraft API differences at thin adapter boundaries.

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

Completed:

- [x] pure Java semantic model in `common`
- [x] 26.1.2 and 26.2 state adapters
- [x] cross-version semantic oracle
- [x] bounded composable target-to-capability lookup
- [x] real-client mapped-state oracle
- [x] target overlap through capability bitsets

Evidence: PR #9 merged as `67ac8f18347a6c6343d0aea6bd51a418dac0d632`; Actions `34685138049` green.

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

Completed:

- [x] Minecraft-independent render descriptor / cue policy
- [x] exact 254-binding M0 target catalog
- [x] both raw registry target indices
- [x] shared procedural quad grammar
- [x] Stained Glass opaque/solid-layer behavior
- [x] independent toggles and all-13 simultaneous policy
- [x] model-bake semantic interpretation
- [x] dual-version `WrapperBlockStateModel`
- [x] zero-world-scan bounded target lookup
- [x] direct active-base OFF path
- [x] primitive enabled mask
- [x] real-client target/model pipeline oracle
- [x] representative rendered parity
- [x] all 13 simultaneously rendered on both versions
- [x] resource reload / active-base restoration evidence

Current contract: [`m3-visual-semantics.md`](m3-visual-semantics.md).

Evidence: PR #11 merged as `03bc5213125b96456682f9f58ab02b84bcd95cdb`.

## M4–M7 — Full parity implementation train 🔧

Goal: finish the remaining 24 capabilities and harden the complete 37-capability product. M4, M5, M6, and M7 are acceptance subtracks rather than separate implementation stops.

Current implementation/evidence contract: [`m4-m7-parity.md`](m4-m7-parity.md).

### Track A — Outline / fine visibility (former M4) ✅ core parity

Targets:

- Blue Ice
- Dead Coral
- Powder Snow
- Sculk Catalyst
- String Tweaks

Acceptance:

- [x] source-backed target/state/cue contract frozen in common code
- [x] 24 exact target bindings
- [x] Sculk `bloom` retained
- [x] all 64 tripwire semantic states represented
- [x] selected JaCoCo/PIT gate includes M4 policy
- [x] 5/5 runtime rendering on 26.1.2 and 26.2
- [x] OFF restoration
- [x] combined M3+M4 rendered evidence
- [x] resource reload and dimension-transition regression evidence

Issue #4 is complete.

### Track B — Resource highlighting (former M5) 🔧 core/rendered parity complete; compatibility verification remains

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

- [x] 18/18 exact target bindings and independent toggles
- [x] active baked base model/texture preserved by wrapper architecture
- [x] shared highlight policy; no per-version product-policy duplication
- [x] dedicated dark-area/high-visibility framebuffer evidence on both versions
- [x] shader-OFF/default OpenGL CI path PASS on both versions
- [x] 26.2 OpenGL CI framebuffer path PASS
- [ ] representative supported shader path verified before shader support is claimed
- [ ] representative third-party active resource-pack matrix
- [x] Vulkan remains a separate experimental track and is not implied by OpenGL success

Dark-area evidence: GitHub Actions **`34731643950` (run #168)**. The dedicated resource-only scene produced 5,041 / 5,051 changed pixels and 4,758 / 4,769 materially brighter pixels on 26.1.2 / 26.2 respectively. OFF average luminance was 1 on both versions; ON average luminance was 5. Both archived ON/OFF pairs were manually reviewed.

Issue #5 remains open for shader/resource-pack compatibility and performance evidence.

### Track C — Nether Tweaks (former M6) ✅ core parity

Target scope is the M0 exact 27-block Nether-oriented set.

Acceptance:

- [x] exact source behavior documented from pinned evidence
- [x] no guessed behavior: source-derived 14×14 interior + 1px frame grammar and per-target palette
- [x] independent toggle
- [x] 26.1.2 and 26.2 adapters/rendering
- [x] overlap interaction preserved
- [x] Nether-Tweaks-only framebuffer regression on both versions
- [x] BlockLens-owned extra models packaged and resolved
- [x] CI rejects missing models and incomplete texture references

Issue #6 is complete.

### Track D — Full-product hardening (former M7) 🔧 automated core gate green; compatibility matrix incomplete

Verified scenarios per supported Minecraft line:

- [x] all 37 capabilities represented by the unified pipeline
- [x] all 37 capabilities ON simultaneously
- [x] supplied five-feature reference preset reproduced and framebuffer-captured
- [x] **real native config-file save/reload acceptance through Fabric config directory**
- [x] config codec/mask round-trip
- [x] resource reload
- [x] world join/leave smoke lifecycle
- [x] dimension changes: Overworld → Nether → Overworld
- [x] dedicated M5 resource-only dark-area framebuffer regression
- [ ] representative third-party active resource pack
- [x] shader OFF/default CI renderer path
- [ ] supported representative shader path(s) with shader ON
- [x] no server-side BlockLens entrypoint/component
- [x] no custom gameplay/network requirement
- [x] target lookup remains bounded / zero-world-scan
- [x] OFF fast path remains intact
- [x] M7 five-screenshot artifact + manifest + SHA-256 list per Minecraft line
- [x] M5 two-screenshot dark-area artifact + manifest + SHA-256 list per Minecraft line

Additional 26.2 tracks:

- [x] OpenGL CI framebuffer path
- [ ] Vulkan experimental verification where available

Latest verified implementation run: **GitHub Actions `34731643950` (run #168)**.

M7 evidence:

| Evidence | 26.1.2 | 26.2 |
| --- | ---: | ---: |
| all-37 ON vs OFF | **3,908 px** | **3,914 px** |
| after resource reload vs OFF | **3,934 px** | **3,909 px** |
| Nether Tweaks only vs OFF | **834 px** | **765 px** |
| reference preset vs OFF | **1,090 px** | **1,072 px** |
| dimension round-trip | PASS | PASS |
| config codec round-trip | PASS | PASS |
| native config-file save/reload | PASS | PASS |
| M5 dark-area evidence | PASS | PASS |

Runtime JAR size in this run: **92,988 bytes** on each version. The M5 oracle/CI evidence adds no production runtime assets.

Full-train exit criteria still pending:

- representative third-party resource-pack preservation matrix
- representative supported shader-ON path
- accurate OpenGL/Vulkan compatibility matrix

Implementation rule: do **not** treat the green automated core gate or M5 dark-area evidence as proof of the still-unverified compatibility items above.

## M8 — Performance, load, and size hardening

Goal: optimize only after full behavior is stable, while continuous budgets protect earlier milestones.

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
- [ ] verify bounded lookup/caches under representative load
- [ ] losslessly optimize retained assets/code where evidence supports it
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
- [ ] README user guide finalized
- [ ] compatibility/version notes
- [ ] SHA-256 for both artifacts
- [ ] release notes
- [ ] clean Prism/Fabric smoke test for 26.1.2 and 26.2

Exit criteria:

- exactly intended release artifacts
- both supported lines green
- supported graphics/shader/resource-pack scope accurately documented
