# BlockLens

**English** | [日本語](README_ja.md)

BlockLens is a **client-side visual inspection mod for Minecraft Java Edition**. It rebuilds the useful visual ideas of the AMATERAS resource-pack baseline as compact, testable code instead of shipping thousands of state-specific JSON/PNG files.

The goal is not "put a resource pack in a JAR." The goal is to preserve the **37-capability visual contract** while improving maintainability, cross-version behavior, configuration, testability, performance evidence, and artifact size.

> **Current state:** M0, M1, and M2 are complete. M3 now has the shared render descriptors, exact 254-target catalog, procedural visual grammar, and zero-scan baked-model pipeline implemented for Minecraft 26.1.2 and 26.2. M3 remains **in progress** until representative rendered visual parity and all-13 simultaneous in-game verification pass. BlockLens does **not** yet claim full 37-feature visual parity.

## Why BlockLens?

The source baseline is useful but structurally expensive: thousands of tiny blockstate/model files encode combinations that can often be represented once as semantics and rendered dynamically.

```mermaid
flowchart LR
    A[AMATERAS baseline\n4,535 ZIP entries] --> B[Freeze behavior\n37 capability contract]
    B --> C[Semantic state engine\nfacing / axis / half / shape / connections]
    C --> D[Shared visual semantics\noverlay / outline / marker]
    D --> E[Thin Minecraft adapters\n26.1.2 + 26.2]
    E --> F[BlockLens\nsmall client-only runtime]
```

```text
many state-specific JSON / PNG variants
                ↓
shared state interpretation + shared render semantics + minimal unique assets
```

## Supported platforms

| Item | Current contract |
| --- | --- |
| Minecraft | **26.1.2** and **26.2** |
| Loader | Fabric |
| Java | **25** |
| Side | **Client only** |
| Server mod required | No |
| Product policy | Shared across both Minecraft versions |
| Version-specific code | Thin Minecraft/Fabric adapters only |

A feature is not considered complete when it works on only one supported Minecraft line unless a current specification explicitly records an exception.

## 37-capability map

The frozen source baseline contains **37 independently controllable capabilities**.

```mermaid
flowchart TB
    BL[BlockLens · 37 capabilities]
    BL --> D[Decoration / Orientation · 13]
    BL --> R[Resource Highlighting · 18]
    BL --> O[Outline / Visibility · 4]
    BL --> X[Other Visual Tweaks · 2]

    D --> D1[Anvil · Beehive · Campfire · Glazed Terracotta]
    D --> D2[Grindstone · Fence Gate · Froglight · Slabs]
    D --> D3[Stained Glass · Stairs · Trapdoor · Wood · Log]

    R --> R1[Obsidian · Ancient Debris]
    R --> R2[Diamond · Gold · Emerald · Coal]
    R --> R3[Iron · Copper · Lapis · Redstone]
    R2 --> R4[normal + deepslate variants]
    R3 --> R5[normal + deepslate variants]

    O --> O1[Blue Ice · Dead Coral · Powder Snow · Sculk Catalyst]
    X --> X1[Nether Tweaks · String Tweaks]
```

The supplied reference RPO preset enables five of the 37 capabilities: Blue Ice, Dead Coral, Powder Snow, Sculk Catalyst, and String Tweaks. That preset is only a preset; the other 32 capabilities remain part of the product contract.

## Runtime architecture

BlockLens keeps Minecraft mapping/API differences at the edge and product semantics in shared code.

```mermaid
flowchart TD
    MC[Minecraft BlockState\n26.1.2 or 26.2] --> VA[Version State Adapter]
    VA --> SS[Common SemanticState]
    SS --> FP[Feature Policy / Config]
    SS --> RD[Render Descriptor]
    FP --> RD
    RD --> VR[Version Render Adapter]
    VR --> SR[Shared Renderer Families]
    SR --> BASE[Vanilla or active resource-pack texture]
    SR --> EXTRA[BlockLens visual information]
```

### Shared semantic state

M2 represents only the information BlockLens needs instead of retaining mapped Minecraft objects in common product policy:

- horizontal facing
- axis
- top/bottom half
- stair shape
- mount face
- slab type
- north/east/south/west connections
- honey level
- open / lit / in-wall / powered / attached flags

Mapped `BlockState` values are translated at the Minecraft-version boundary. The shared state is compact and Minecraft-independent.

## M3 procedural decoration pipeline

M3 does **not** copy AMATERAS textures into BlockLens. The pinned source is behavioral/visual evidence; BlockLens applies original procedural cues to the model already baked by Minecraft or the user's active resource pack.

```mermaid
flowchart LR
    A[Exact M0 target catalog\n254 bindings] --> B[Raw-ID target index\nmodel-load time]
    B --> C[BlockState semantic adapter\nmodel-bake time]
    C --> D[Common render descriptor]
    D --> E[WrapperBlockStateModel]
    E --> F{represented feature ON?}
    F -- no --> G[emit original baked model]
    F -- yes --> H[apply common quad cue]
    H --> I[base model + BlockLens visual information]
```

Important properties:

- exact M0 scope; no suffix-based automatic expansion
- all 13 controls remain independent
- X/Y/Z orientation uses a shared red/green/blue visual grammar derived from source intent
- Stained Glass is modeled as **opaque/solid-layer behavior**, matching the source meaning
- semantic interpretation occurs at model bake, not every frame
- no whole-world scan and no per-frame registry scan
- runtime config uses a primitive capability bitmask for the OFF fast path
- when no represented M3 feature is enabled, the original baked model is emitted directly
- model reload naturally wraps newly baked active-resource-pack models rather than owning copied base textures

Authoritative M3 visual contract: [`knowledge/current/m3-visual-semantics.md`](knowledge/current/m3-visual-semantics.md).

## Development status

| Milestone | Status | Result |
| --- | --- | --- |
| M0 — source baseline freeze | ✅ Complete | 37/37 capability evidence and machine-readable contract |
| M1 — dual-version quality scaffold | ✅ Complete | Java 25, config, CI, GameTest, reproducible artifacts, size/load baseline |
| M2 — shared state engine | ✅ Complete | semantic model, both adapters, real-client cross-version oracle, composable bounded lookup |
| M3 — 13 decoration/orientation visuals | 🔧 In progress | exact targets, render policy, procedural quad grammar, dual-version model wrappers; rendered parity evidence remains |
| M4–M7 | Planned | outline, resource highlights, Nether Tweaks, full interaction hardening |
| M8 | Planned | final performance/load/size hardening |
| M9 | Planned | release-readiness and public artifact audit |

Authoritative implementation order: [`knowledge/current/roadmap.md`](knowledge/current/roadmap.md).

## M1 verified baseline

The M1 scaffold was intentionally tiny before visual rendering code expanded.

| Minecraft | Runtime JAR | BlockLens init | Client GameTest | Reproducible artifact |
| --- | ---: | ---: | --- | --- |
| 26.1.2 | **14,481 B** | ~**7.513 ms** | PASS | PASS |
| 26.2 | **14,476 B** | ~**2.684 ms** | PASS | PASS |

These are **M1 baselines**, not promises that later feature-complete artifacts remain 14 KiB.

```text
Source resource-pack ZIP   2,366,865 B  ████████████████████████████████████████
M1 26.1.2 runtime JAR         14,481 B  ▏
M1 26.2 runtime JAR           14,476 B  ▏
Hard release maximum       1,183,432 B  ████████████████████
Stretch target               716,800 B  ████████████
```

Per release artifact:

- required: **< 1,183,433 bytes**
- stretch: **<= 716,800 bytes (700 KiB)**

Functional parity and correctness have higher priority than byte count.

## Quality graph

Minecraft integration changes must pass both version branches.

```mermaid
flowchart TD
    C[Common JUnit / contracts] --> Q[JaCoCo + PIT selected policy gates]
    Q --> B1[26.1.2 build]
    Q --> B2[26.2 build]
    B1 --> G1[26.1.2 Client GameTest]
    B2 --> G2[26.2 Client GameTest]
    G1 --> A1[JAR audit + size + reproducibility]
    G2 --> A2[JAR audit + size + reproducibility]
    A1 --> P[Cross-version parity decision]
    A2 --> P
    P --> GREEN([CI GREEN])
```

Current automated quality includes JUnit 5, exact 37-capability contracts, cross-version semantic/config parity, selected JaCoCo and PIT gates, Fabric Client GameTest on both supported versions, exact M3 target/model-pipeline oracles, config persistence, runtime JAR structure/privacy/residue audit, reproducible SHA-256 rebuilds, JAR byte budgets, and startup/load structural contracts.

## Engineering Graph Loop

BlockLens uses an explicit development graph. Code written is not equivalent to work completed.

```mermaid
flowchart LR
    D[DISCOVER] --> P[PLAN]
    P --> I[IMPLEMENT]
    I --> V[VERIFY]
    V --> R[SELF REVIEW]
    R --> K[DOCUMENT]
    K --> PR[PR]
    PR --> CI[CI]
    CI --> U[ISSUE UPDATE]
    U --> DONE([DONE])

    V -- fail --> X[DIAGNOSE]
    R -- defect --> X
    CI -- fail --> X
    X --> F[FIX]
    F --> V
```

A failure always loops through **DIAGNOSE → FIX → VERIFY**. It does not skip directly to completion.

The authoritative rules, exit criteria, and `DONE / BLOCKED / PARTIAL` terminal states are defined in [`knowledge/current/engineering-loop.md`](knowledge/current/engineering-loop.md).

## Repository layout

```text
BlockLens/
├─ common/                  # shared product/config/state/render policy + contracts
├─ versions/
│  ├─ mc26_1_2/            # thin 26.1.2 Minecraft/Fabric adapter
│  └─ mc26_2/              # thin 26.2 Minecraft/Fabric adapter
├─ gametest/                # shared real-client integration oracles
├─ gradle/                  # version-module convention and build rules
├─ knowledge/
│  ├─ index.md
│  └─ current/              # authoritative current specification
├─ AGENTS.md                # engineering rules
└─ README_ja.md             # Japanese README
```

## Build and verify

Java 25 is required.

```bash
./gradlew qualityGate
./gradlew ciGate
```

Per-version client integration checks are also executed by GitHub Actions. A Minecraft/Fabric integration change is not complete until **both** supported versions are green for the current head SHA.

## Performance rules

BlockLens intentionally avoids work that does not belong on startup or the render hot path:

- no runtime classpath/reflection feature discovery
- no startup update/network check
- no telemetry initialization
- no normal-path legacy RPO parsing
- no eager geometry generation for disabled features
- no whole-world or unbounded loaded-chunk scan each frame
- no per-frame registry scan or BlockState reinterpretation in M3 wrappers
- no per-frame rebuilding of unchanged retained geometry
- bounded/raw-ID target lookup where practical
- primitive config mask for frequent render enable checks
- explicit cache/state ownership around reload/world transitions

Performance claims require evidence. Startup/load, resource reload, runtime frame behavior, allocations/memory, and JAR bytes are separate budgets.

## Resource-pack and shader compatibility direction

BlockLens prefers to preserve the model/texture supplied by Minecraft or the user's active resource pack, then apply only the extra BlockLens visual information where technically safe.

Shader/backend compatibility is verified rather than assumed. Minecraft 26.2 OpenGL is a required track; Vulkan is tracked separately and is not claimed merely because OpenGL passes.

## Non-goals

- embedding the original resource pack unchanged inside the JAR
- removing capabilities merely to meet a size target
- duplicating product policy between Minecraft version directories
- requiring a BlockLens server component for visual features
- adding unrelated gameplay automation
- treating the currently enabled five RPO options as the complete product
- claiming shader/backend compatibility without verification
- claiming M3 visual parity merely because model-wrapper code compiles

## Source and licensing note

The AMATERAS resource pack is used as a **behavioral/visual reference baseline**. Its internal structure is not the target architecture for BlockLens.

The baseline contains attribution to other creators and no redistribution permission should be assumed from this repository. Before public release, every third-party texture, model, text, or other retained asset must have its redistribution/license status verified. BlockLens currently prefers original procedural code for M3 instead of copying the source binary assets.

## Project documentation

- Engineering rules: [`AGENTS.md`](AGENTS.md)
- Knowledge index: [`knowledge/index.md`](knowledge/index.md)
- Product contract: [`knowledge/current/product-spec.md`](knowledge/current/product-spec.md)
- Architecture: [`knowledge/current/architecture.md`](knowledge/current/architecture.md)
- Quality strategy: [`knowledge/current/quality-strategy.md`](knowledge/current/quality-strategy.md)
- Performance strategy: [`knowledge/current/performance-strategy.md`](knowledge/current/performance-strategy.md)
- Roadmap: [`knowledge/current/roadmap.md`](knowledge/current/roadmap.md)
- M3 visual semantics: [`knowledge/current/m3-visual-semantics.md`](knowledge/current/m3-visual-semantics.md)
- Engineering Graph Loop: [`knowledge/current/engineering-loop.md`](knowledge/current/engineering-loop.md)

`knowledge/current/` is the source of truth. README files are user-facing summaries and must be corrected when they fall behind current specifications.
