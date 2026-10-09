# M3 Decoration / Orientation Visual Semantics

Status: **authoritative current M3 visual contract — DONE**

This document separates two things that must not be conflated:

1. behavior and visual intent derived from the pinned source baseline,
2. BlockLens-owned procedural rendering choices used to reproduce that value without redistributing source binary assets.

The pinned source remains:

```text
reference-source-mc26.1.2.zip (neutral documentation label)
SHA-256: 36e5c5bba4e77f05b8c22d549593dfb95e8aebc7bfebdba57e229257b46640ef
```

## 1. Scope

M3 owns exactly 13 independently controllable capabilities and 254 exact source target bindings:

| Capability | M0 target count | State semantics |
| --- | ---: | --- |
| Anvil | 1 | facing |
| Beehive | 1 | facing + honey level 0..5 |
| Campfire | 2 | facing + lit |
| Fence Gate | 12 | facing + open + in-wall |
| Froglight | 3 | axis |
| Glazed Terracotta | 16 | facing |
| Grindstone | 1 | mount face + facing |
| Log | 24 | axis |
| Slabs | 61 | bottom / top / double |
| Stained Glass | 32 | opaque behavior + pane connections |
| Stairs | 58 | facing + half + five shapes |
| Trapdoor | 21 | facing + half + open |
| Wood | 22 | axis |

The exact target names remain machine-checked against `capability-contract.tsv`. M3 must not silently broaden scope using suffix matching such as every future `*_stairs` block.

## 2. Source-derived visual findings

The following is evidence from the pinned source pack, not a claim that BlockLens must copy source pixels:

- Log/Wood orientation uses a consistent axis-color grammar. Representative source textures encode X with red, Y with green, and Z with blue cues.
- Slab source variants add high-contrast red state/boundary information.
- Stair source variants encode facing/half/shape with high-contrast state markings rather than relying on the base texture alone.
- Glazed Terracotta source variants make facing visibly obvious with a directional graphic.
- Campfire and Beehive source variants encode facing while preserving their block identity; Beehive also has source states for all six honey levels.
- Froglight variants encode X/Y/Z axis state.
- Grindstone variants encode mount face and horizontal facing.
- Trapdoor variants encode open/closed, half, and facing.
- Stained Glass has an explicit source meaning: **Make stained glasses opaque**. It is not merely an orientation/highlight feature.

The authoritative state/target evidence remains `capability-map.md` and `capability-contract.tsv`.

## 3. BlockLens procedural visual grammar

BlockLens does not copy the source pack's textures into the runtime JAR. It keeps the currently baked Minecraft/active-resource-pack model as the base and applies procedural render information to it.

Current shared cue policy:

| Semantic cue | BlockLens procedural behavior |
| --- | --- |
| Axis X | red cue on X end faces |
| Axis Y | green cue on Y end faces |
| Axis Z | blue cue on Z end faces |
| Basic facing | stable high-contrast facing cue |
| Beehive honey=5 | distinct full-honey facing cue |
| Campfire lit/unlit | facing cue changes with lit state |
| Grindstone | mount surface cue + facing cue |
| Fence Gate | facing cue distinguishes open/closed |
| Slab | visible top/bottom state boundary |
| Stained Glass | force solid rendering layer while enabled; base color is not procedurally replaced |
| Stairs | facing cue distinguishes shape; separate half boundary cue |
| Trapdoor | closed uses panel surface cue; open uses facing-plane cue |

These colors and mappings are common product policy in `common`, not duplicated in Minecraft-version adapters.

## 4. Runtime pipeline

```mermaid
flowchart LR
    A[Exact M0 target catalog\n254 bindings] --> B[Registry raw-id index\nmodel-load time]
    B --> C[BlockState semantic adapter\nmodel-bake time]
    C --> D[Render descriptor\nMinecraft-independent]
    D --> E[WrapperBlockStateModel]
    E --> F{feature enabled?}
    F -- no --> G[emit original baked model]
    F -- yes --> H[apply procedural quad cue]
    H --> I[emit active resource-pack base + BlockLens cue]
```

### Ownership rules

- No whole-world scan.
- No per-frame registry scan.
- Minecraft `BlockState` interpretation occurs during model bake, not for every rendered frame.
- The render hot path uses immutable descriptors and primitive capability masks.
- If none of the capabilities represented by a wrapper are enabled, the wrapped model is emitted directly with no BlockLens quad transform.
- BlockLens owns only its additional render transformation. It does not replace the user's base resource-pack model with copied source models/textures.

## 5. Configuration behavior

All 13 capabilities remain independent.

A target may participate in multiple BlockLens capabilities elsewhere in the product. Target indexing therefore stores a capability bit-set rather than a mutually exclusive single value.

The native config remains the source of enable/disable truth. `BlockLensConfig` also exposes an immutable 64-bit enabled mask for hot-path checks; this is a runtime optimization only and does not change config-file semantics.

## 6. Compatibility / rendering-state contract

- Feature OFF uses normal baked Minecraft/active-resource-pack rendering.
- Enabling one M3 feature does not implicitly disable another.
- All 13 enabled simultaneously is a required and verified test condition.
- Stained Glass solid-layer behavior is isolated to that capability and must not leak to unrelated models.
- Geometry changes are followed by terrain invalidation before rendered evidence is sampled.
- Resource reload rebuilds wrapped models from newly baked active-resource-pack models rather than retaining stale model instances.
- Minecraft-version rendering invalidation stays at thin adapter boundaries: 26.1.2 uses `LevelRenderer.allChanged()`; 26.2 uses `LevelExtractor.allChanged()`.

## 7. Verification layers

M3 is verified in layers:

```mermaid
flowchart TD
    P[Pure semantic state tests] --> R[Render policy + quad grammar tests]
    R --> T[Exact 254-target machine contract]
    T --> V1[26.1.2 registry + model pipeline Client GameTest]
    T --> V2[26.2 registry + model pipeline Client GameTest]
    V1 --> A[Runtime JAR / reproducibility / size audit]
    V2 --> A
    A --> VIS[Rendered visual parity evidence]
    VIS --> ALL[All 13 simultaneously + reload + OFF restoration]
```

### Final rendered evidence

GitHub Actions run `34712828259` on PR #11 is the evidence run for the finalized M3 visual oracle before documentation-only completion commits. Common JUnit/JaCoCo/PIT and both version jobs passed. Each version produced three real 640x360 client framebuffer captures plus a manifest and screenshot SHA-256 file.

The oracle is intentionally deterministic:

- all 13 representative capabilities are present in one scene,
- time and weather progression are disabled with the current namespaced gamerules `minecraft:advance_time=false` and `minecraft:advance_weather=false`,
- the representative campfire is unlit to remove smoke-particle image noise,
- resource-reload completion is followed by LoadingOverlay fade time before capture,
- changed terrain is explicitly invalidated before capture,
- visual comparison is restricted to the fixed target ROI `90,205-550,285`, not the whole framebuffer,
- a GameTest-only white-glazed-terracotta override to vanilla magenta concrete proves the active baked resource-pack base survives with BlockLens ON and OFF.

| Minecraft | Wrapped models | Marker pixels ON/OFF | ON vs OFF ROI pixels | Reloaded ON vs OFF ROI pixels |
| --- | ---: | ---: | ---: | ---: |
| 26.1.2 | 7,577 | 270 / 270 | 345 | 335 |
| 26.2 | 7,577 | 270 / 270 | 368 | 335 |

The required ROI difference is 150 pixels, so both first-ON and reload-ON captures remain measurably distinct from OFF while the active resource-pack marker remains unchanged. OFF restoration therefore has rendered evidence rather than only structural/model-pipeline evidence.

Runtime artifacts in the same run remained compact and reproducible:

- 26.1.2: 63,337 bytes
- 26.2: 63,335 bytes

The CI renderer for this evidence is OpenGL/llvmpipe. This M3 evidence **does not claim 26.2 Vulkan verification**; Vulkan remains a separate later compatibility track.

## 8. Licensing boundary

The reference pack contains attribution to other creators and no redistribution permission is assumed.

Therefore M3 follows a fail-closed asset policy:

- source textures/models are reference evidence,
- source binary assets are not copied into BlockLens by default,
- procedural cues and original code are preferred,
- any future retained third-party binary asset requires explicit redistribution/license verification before public release.
