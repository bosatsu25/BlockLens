# BlockLens Current Roadmap

Status: **authoritative current implementation order**

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

- every source capability has an evidence-backed target/behavior description,
- no capability is defined only by guesswork,
- baseline measurements are reproducible.

## M1 — Fabric/Java 25 scaffold

Goal: establish a minimal, buildable client-only mod with all product contracts represented.

Tasks:

- [ ] Fabric project for Minecraft 26.1.2 / Java 25
- [ ] `fabric.mod.json`
- [ ] client entrypoint
- [ ] 37-capability catalog
- [ ] native configuration model
- [ ] English/Japanese translation skeleton
- [ ] JUnit test setup
- [ ] CI build/test/artifact audit
- [ ] runtime JAR size report

Exit criteria:

- clean build,
- 37/37 capability contract PASS,
- no server dependency,
- initial config round-trip PASS.

## M2 — Shared state engine

Goal: represent block orientation/state once and reuse it.

Initial semantic set:

- facing
- axis
- half
- shape
- open/closed
- connections
- powered/attached when required by source evidence

Exit criteria:

- pure Java state tests cover relevant combinations,
- state engine does not depend on one source feature's UI name,
- no duplicate state parsing across capability implementations without justification.

## M3 — Decoration/orientation parity

Goal: implement all 13 decoration/orientation capabilities.

Target list:

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

- 13/13 capabilities implemented,
- source-state mapping verified,
- independent toggles,
- representative visual parity PASS,
- all 13 ON simultaneously PASS.

## M4 — Outline and fine visibility parity

Goal: implement the four outline capabilities plus String Tweaks.

Targets:

- Blue Ice
- Dead Coral
- Powder Snow
- Sculk Catalyst
- String Tweaks

Exit criteria:

- 5/5 implemented,
- connection/state-sensitive behavior correct where applicable,
- no unrelated render-state leakage when toggled OFF,
- simultaneous use with M3 PASS.

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

- 18/18 independently controllable,
- active resource-pack texture preserved where intended,
- dark-area visibility behavior verified,
- shader OFF PASS,
- supported shader ON paths PASS.

## M6 — Nether Tweaks parity

Goal: implement `nethertweaks` only after exact source behavior is captured.

Exit criteria:

- affected targets/states documented,
- no guessed behavior,
- source parity evidence exists,
- independent toggle and regression tests.

## M7 — Full parity and interaction hardening

Goal: validate the product as a whole rather than feature-by-feature only.

Required scenarios:

- [ ] all 37 capabilities supported
- [ ] all capabilities ON simultaneously
- [ ] supplied current preset reproduced
- [ ] config save/reload
- [ ] resource reload
- [ ] world join/leave
- [ ] dimension changes
- [ ] active third-party resource pack
- [ ] representative shader packs
- [ ] no server-side BlockLens
- [ ] no custom gameplay/network requirement

Exit criteria:

- Functional Parity Gate PASS,
- visual regression PASS,
- compatibility matrix documented.

## M8 — Performance and size hardening

Goal: reduce static duplication and runtime overhead without changing M7 behavior.

Required measurements:

- JAR size and top entries
- resource reload duration
- allocation rate
- median/P95/P99 frame time in representative scenes
- cache/retained geometry size

Release size gates:

```text
required: < 1,183,433 bytes
stretch:  <= 716,800 bytes
```

Exit criteria:

- hard size gate PASS,
- no M7 regression,
- performance is at least neutral vs the pre-optimization BlockLens baseline in tested scenarios.

## M9 — Release readiness

Goal: produce a distributable BlockLens release.

Tasks:

- [ ] licensing/redistribution audit
- [ ] clean release build
- [ ] artifact privacy/security audit
- [ ] README user guide
- [ ] compatibility notes
- [ ] SHA-256
- [ ] release notes
- [ ] install/smoke test in clean Prism/Fabric instance

Exit criteria:

- release artifact only contains intended runtime content,
- all quality gates green,
- supported environment clearly documented.
