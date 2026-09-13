# BlockLens Current Roadmap

Status: **authoritative current implementation order**

## Cross-cutting release rule

Every implementation milestone after M1 must preserve:

- Minecraft **26.1.2** build/test support
- Minecraft **26.2** build/test support
- the same **37-capability** product contract
- client-only behavior
- per-version artifact/load/performance reporting
- current quality thresholds without silent relaxation

Performance and size are continuous gates, not tasks postponed until the end.

## M0 — Baseline freeze ✅

Completed:

- [x] pinned source ZIP/RPO hashes
- [x] 37 RPO-key capability contract
- [x] exact Minecraft target/state mapping
- [x] duplicate/generated/unreachable asset analysis
- [x] source-derived golden references
- [x] Nether Tweaks behavior clarification
- [x] retained third-party asset/licensing classification

Authoritative evidence: `source-baseline.md`, `capability-map.md`, `capability-contract.tsv`.

## M1 — Dual-version Fabric / Java 25 scaffold ✅

Completed:

- [x] `common`, `mc26_1_2`, `mc26_2` multi-project layout
- [x] client-only Fabric metadata/artifacts
- [x] 37-capability shared catalog/config
- [x] English/Japanese localization foundation
- [x] JUnit + JaCoCo + PIT quality gates
- [x] dual-version GitHub Actions + Client GameTest
- [x] reproducible artifact, privacy/residue and JAR-budget gates
- [x] startup/load baseline

## M2 — Shared state engine and version adapters ✅

Completed:

- [x] common `SemanticState`
- [x] thin 26.1.2 / 26.2 adapters
- [x] real-client mapped-state oracle
- [x] bounded target-to-capability lookup
- [x] intentional overlap through capability bitsets

## M3 — Decoration / orientation parity ✅

All 13 decoration/orientation capabilities are implemented and rendered through the unified model pipeline.

Completed:

- [x] exact target catalog
- [x] shared procedural quad grammar
- [x] Stained Glass solid-layer behavior
- [x] model-bake semantic interpretation
- [x] dual-version `WrapperBlockStateModel`
- [x] zero-world-scan bounded target lookup
- [x] primitive enabled mask / direct OFF fast path
- [x] rendered all-13, reload and OFF-restoration evidence

Authoritative contract: [`m3-visual-semantics.md`](m3-visual-semantics.md).

## M4–M7 — Full behavior / compatibility train 🔧

Core product behavior is green. Compatibility items that require external renderer/resource-pack combinations remain intentionally separate from release readiness.

Authoritative contract: [`m4-m7-parity.md`](m4-m7-parity.md).

### M4 — Outline / fine visibility ✅ core parity

- [x] Blue Ice
- [x] Dead Coral
- [x] Powder Snow
- [x] Sculk Catalyst including `bloom`
- [x] String Tweaks / all 64 Tripwire semantic states
- [x] dual-version runtime rendering and OFF restoration
- [x] resource-reload / dimension-transition regression evidence

### M5 — Resource highlighting 🔧 core/rendered parity complete

- [x] 18/18 exact target bindings and independent toggles
- [x] active baked model/texture preservation
- [x] dedicated dark-area framebuffer evidence
- [x] shader-OFF/default OpenGL CI path on both versions
- [x] deterministic active-resource-pack fixture
- [x] performance/load/retention evidence completed by M8
- [ ] representative supported shader-ON path before shader support is claimed
- [ ] broader representative third-party resource-pack matrix

Issue #5 remains the compatibility authority for the unchecked items. They are outside the advertised v0.1.0 support boundary and therefore are not release blockers.

### M6 — Nether Tweaks ✅ core parity

- [x] exact 27-target source-derived behavior
- [x] 14×14 interior + 1px frame grammar / palette
- [x] dual-version rendering
- [x] overlap interaction
- [x] Nether-only framebuffer regression
- [x] BlockLens-owned extra-model packaging/resource-resolution gate

### M7 — Automated full-product core gate ✅

Per supported Minecraft line:

- [x] all 37 capabilities represented / all ON
- [x] native config save/reload
- [x] config codec/mask round-trip
- [x] resource reload
- [x] Overworld → Nether → Overworld
- [x] Nether-only scenario
- [x] frozen five-feature reference preset
- [x] M5 dark-area scenario
- [x] all-OFF active-base restoration
- [x] bounded zero-world-scan lookup
- [x] M3/M5/M7 artifact evidence
- [x] no server-side BlockLens component/gameplay protocol
- [ ] representative supported shader-ON matrix
- [ ] broader third-party resource-pack compatibility matrix
- [ ] Minecraft 26.2 Vulkan experimental verification

A green M7 core gate does not imply the unchecked external compatibility combinations are supported.

## M8 — Performance, load, retention, and size hardening ✅

**Completed in PR #17 + PR #18.** Final implementation verification before documentation closeout: GitHub Actions **`34738345474` (run #223)**.

### Artifact-size contract

```text
pinned source pack:            2,366,865 B
absolute <50% max:             1,183,432 B
release budget:                  102,400 B (100 KiB)
PR #17 pre-optimization:          93,068 B
final frozen M8 baseline:         88,973 B
```

Final M8 JAR is **4,095 B / 4.4% smaller** than the PR #17 baseline and **96.24% smaller** than the pinned source ZIP while retaining all 37 capabilities.

### Completed tasks

- [x] freeze a strict no-growth baseline per version
- [x] replace the historical 700 KiB stretch target with a **100 KiB release budget**
- [x] retain the absolute source-pack **<50%** rule
- [x] deterministic runtime-JAR exact-size/category/top-entry report
- [x] reject unexplained JAR growth
- [x] remove repeated M4/M5 immutable instruction construction
- [x] remove duplicate retained M5 color state
- [x] remove runtime-unreachable policy bytecode
- [x] explicitly reject retired policy classes from packaged runtime JARs
- [x] repeat real-client observations before freezing coarse performance tolerances
- [x] freeze median-based gross-regression guards
- [x] collect main render-pass median/P95/P99 evidence
- [x] measure retained model/capability structure across repeated resource reloads
- [x] verify retained structure is stable across three measured reloads
- [x] preserve lazy bounded Nether overlay caching without render-hot-path telemetry
- [x] verify reproducible compact JAR packaging
- [x] keep M3/M5/M7 behavior green on both Minecraft versions
- [x] update English/Japanese README and authoritative M8 evidence

### Coarse regression guards

Based on repeated dual-version observations from runs #190/#192 and finalized in M8:

```text
resource-reload median:  <= 6.0 s
terrain-rebuild median:  <= 2.5 s
allocation median:       <= 32 MiB
```

These are gross-regression guards, not performance-win claims.

### Final run #223 evidence

| Metric | 26.1.2 | 26.2 |
| --- | ---: | ---: |
| runtime JAR | **88,973 B** | **88,973 B** |
| reload median | **3.842 s** | **4.069 s** |
| OFF / resource-ON rebuild median | **1.493 / 1.510 s** | **1.532 / 1.512 s** |
| OFF frame-main median | **5.026 ms** | **5.054 ms** |
| resource-ON frame-main median | **5.078 ms** | **5.092 ms** |
| resource-ON frame-main P95 | **7.969 ms** | **11.441 ms** |
| resource-ON frame-main P99 | **12.009 ms** | **12.538 ms** |
| wrapped models | **7,820** | **7,820** |
| retained capability slots | **7,827** | **7,827** |
| max capabilities/model | **2** | **2** |
| Nether wrapped models | **33** | **33** |
| retention stable across 3 reloads | PASS | PASS |

Frame values are test-only **main render-pass durations**, not full present-to-present FPS measurements. Tail values remain evidence rather than a claimed optimization percentage.

Authoritative evidence: [`m8-performance.md`](m8-performance.md).

### M8 exit criteria

- [x] hard size gate PASS on both artifacts
- [x] final 88,973 B no-growth baseline frozen
- [x] 100 KiB release budget PASS
- [x] M7 behavior unchanged
- [x] reload/rebuild/allocation gross-regression guards PASS
- [x] frame percentile evidence captured
- [x] retained-state/reload stability captured
- [x] lazy cache contract protected
- [x] common JUnit/JaCoCo/PIT remain GREEN without threshold reduction

**M8 terminal state: DONE.**

## M9 — Release readiness 🚀

M9 turns the M0-M8 verified product into a repeatable release process. Authoritative contract: [`release-readiness.md`](release-readiness.md).

### Icon-inclusive artifact contract

The old 88,973 B M8 baseline was deliberately kept for the first icon build. Run **`34766720469` (#232)** correctly failed the no-growth gate and measured the new product-asset cost:

```text
26.1.2 icon-inclusive JAR:          95,332 B
26.2 icon-inclusive JAR:            95,333 B
new shared no-growth baseline:      95,333 B
release budget:                    102,400 B (unchanged)
absolute <50% max:               1,183,432 B (unchanged)
```

### Completed implementation tasks

- [x] repository-owned BlockLens icon added
- [x] both Fabric metadata files reference the packaged icon
- [x] intentional icon growth measured before rebaseline
- [x] no-growth baseline explicitly updated to **95,333 B**
- [x] 100 KiB release budget retained without relaxation
- [x] automated release workflow triggered only by successful `main` push CI
- [x] release consumes exact verified CI JARs instead of rebuilding
- [x] release idempotency through `mod_version` / existing tag detection
- [x] release-time Minecraft/mod/client/icon/size revalidation
- [x] SHA-256 checksum generation
- [x] evidence-bounded release notes generation
- [x] licensing / redistribution posture documented
- [x] per-version artifact privacy/security gates retained
- [x] README English technology/release guide rewritten
- [x] README Japanese technology/release guide rewritten
- [x] Java 25 / Gradle / Fabric / JUnit / JaCoCo / PIT / GameTest / GitHub Actions stack documented
- [x] supported shader/resource-pack/Vulkan scope explicitly bounded
- [x] M9 repository/release contracts added to JUnit

### Operational completion gates

- [ ] latest PR-head JUnit/JaCoCo/PIT GREEN
- [ ] latest PR-head 26.1.2 real-client/artifact gate GREEN
- [ ] latest PR-head 26.2 real-client/artifact gate GREEN
- [ ] squash merge PR #19
- [ ] post-merge `main` CI GREEN
- [ ] automatic `v0.1.0` GitHub Release publication GREEN
- [ ] published release assets/checksums verified

M9 is not marked terminal DONE until the live publish path is verified. The remaining Issue #5 shader-ON/resource-pack compatibility track is explicitly outside the v0.1.0 advertised support boundary.
