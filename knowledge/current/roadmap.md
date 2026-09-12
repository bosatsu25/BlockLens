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

## M0 — Baseline freeze

Goal: make the source resource pack reproducible as a specification before coding.

Tasks:

- [ ] Record SHA-256 of supplied ZIP and RPO
- [ ] Map 37 RPO keys to source files
- [ ] Map source files to Minecraft block IDs/states
- [ ] Identify duplicate/generated assets
- [ ] Capture representative screenshots/golden references
- [ ] Document ambiguous capabilities, especially `nethertweaks`
- [ ] Classify retained third-party assets and licensing risk

Exit criteria:

- every source capability has evidence-backed behavior
- no capability is defined only by guesswork
- baseline measurements are reproducible

## M1 — Dual-version Fabric/Java 25 scaffold + quality foundation

Goal: establish a minimal buildable client-only mod for **26.1.2 and 26.2** with the full quality contract in place before feature implementation expands.

Tasks:

- [ ] Gradle multi-project layout: `common`, `versions/mc26_1_2`, `versions/mc26_2`
- [ ] Fabric metadata/runtime artifact per version
- [ ] one client entrypoint per artifact
- [ ] 37-capability common catalog
- [ ] native shared configuration schema
- [ ] English/Japanese translation skeleton
- [ ] JUnit 5
- [ ] JaCoCo selected-policy coverage gate
- [ ] PIT selected-policy mutation gate
- [ ] `qualityGate` and `ciGate`
- [ ] GitHub Actions version matrix / isolated per-version jobs
- [ ] client GameTest/smoke path for both versions
- [ ] functional-parity/version-parity contracts
- [ ] reproducible artifact audit
- [ ] per-version JAR byte report/budget
- [ ] startup/resource-reload performance evidence harness or documented reproducible capture path

Exit criteria:

- both version artifacts build cleanly on Java 25
- 37/37 common capability contract PASS
- config round trip PASS
- no server dependency
- automated quality entry points PASS
- first per-version load/size baselines recorded

## M2 — Shared state engine and version adapters

Goal: represent block orientation/state once and keep mapped Minecraft API differences at thin adapter boundaries.

Initial semantic set:

- facing
- axis
- half
- shape
- open/closed
- connections
- powered/attached when required

Tasks:

- [ ] pure Java semantic model in `common`
- [ ] 26.1.2 state adapter
- [ ] 26.2 state adapter
- [ ] cross-version oracle tests proving equivalent semantic output
- [ ] bounded target-to-capability lookup design

Exit criteria:

- pure Java state tests cover relevant combinations
- no duplicated product policy across version projects
- both adapters produce equivalent semantics for shared Minecraft states
- no reflection/classpath feature discovery

## M3 — Decoration/orientation parity

Goal: implement all 13 decoration/orientation capabilities on both versions.

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

Exit criteria:

- 13/13 on 26.1.2 and 26.2
- independent toggles
- representative visual parity PASS
- all 13 simultaneously PASS
- startup/reload/runtime no-regression evidence retained

## M4 — Outline and fine visibility parity

Goal: implement Blue Ice, Dead Coral, Powder Snow, Sculk Catalyst, and String Tweaks through shared runtime rendering.

Exit criteria:

- 5/5 on both versions
- state/connection-sensitive behavior correct
- OFF restores normal rendering
- combined M3+M4 PASS
- no render-state leakage through reload/world transitions

## M5 — Resource highlighting parity

Goal: implement all 18 resource/highlight capabilities.

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

Exit criteria:

- 18/18 independently controllable on both versions
- active resource-pack texture preserved where intended
- shader OFF PASS
- supported shader ON paths PASS
- 26.2 OpenGL PASS
- 26.2 Vulkan tracked separately and only claimed if verified

## M6 — Nether Tweaks parity

Goal: implement `nethertweaks` only after exact source behavior is captured.

Exit criteria:

- affected targets/states documented
- no guessed behavior
- source parity evidence exists
- independent toggle/regression tests on both versions

## M7 — Full parity and interaction hardening

Required scenarios per supported Minecraft line:

- [ ] all 37 capabilities supported
- [ ] all capabilities ON simultaneously
- [ ] supplied reference preset reproduced
- [ ] config save/reload
- [ ] resource reload
- [ ] world join/leave
- [ ] dimension changes
- [ ] active third-party resource pack
- [ ] representative shader packs
- [ ] no server-side BlockLens
- [ ] no custom gameplay/network requirement

Additional 26.2 tracks:

- [ ] OpenGL
- [ ] Vulkan experimental verification where available

Exit criteria:

- Functional Parity Gate PASS
- Version Parity Gate PASS
- visual regression PASS
- compatibility matrix documented

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
