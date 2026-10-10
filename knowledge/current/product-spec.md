# BlockLens Current Product Specification

Status: **authoritative current specification**

## 1. Product definition

BlockLens is a **client-side Minecraft Java Edition visual inspection mod**.

Its purpose is to make block orientation, block state, resources, outlines, and other hard-to-see visual information easier to inspect without replacing the user's world, automating gameplay, or requiring a server-side BlockLens component.

The initial product is a clean reimplementation of the user value represented by the supplied resource pack and its RPO configuration.

## 2. Supported platform targets

BlockLens launches with three first-class Minecraft targets:

- Minecraft Java Edition: **26.1.2**
- Minecraft Java Edition: **26.2**
- Minecraft Java Edition: **26.3**
- Mod loader: **Fabric**
- Java baseline: **25**
- Side: **client only**
- Server-side BlockLens: **not required**

Each supported Minecraft line receives its own verified runtime JAR. A capability is not considered complete if it exists only on one supported version unless the current specification explicitly documents a Minecraft-native limitation.

The multi-version implementation rules are defined in `knowledge/current/versioning.md`.

## 3. Functional-parity baseline

The source RPO exposes **37 independently addressable capabilities**. BlockLens must preserve the ability to represent every one of them on all three supported Minecraft targets.

### 3.1 Decoration / orientation — 13

| Source key | User-facing capability |
|---|---|
| `anvil` | Anvil orientation/state visibility |
| `beehive` | Beehive orientation/state visibility |
| `campfire` | Campfire state visibility |
| `glazedterracotta` | Glazed terracotta orientation visibility |
| `grindstone` | Grindstone orientation/state visibility |
| `fence_gate` | Fence gate orientation/state visibility |
| `froglight` | Froglight orientation/state visibility |
| `slabs` | Slab placement/state visibility |
| `stainedglass` | Stained glass visibility assistance |
| `stairs` | Stair orientation/shape/half visibility |
| `trapdoor` | Trapdoor orientation/state visibility |
| `wood` | Wood orientation visibility |
| `log` | Log orientation visibility |

### 3.2 Resource highlighting — 18

| Source key | User-facing capability |
|---|---|
| `obsidian` | Obsidian visibility/highlight |
| `ancient_debris` | Ancient Debris visibility/highlight |
| `diamond_ore` | Diamond Ore visibility/highlight |
| `deepslate_diamond_ore` | Deepslate Diamond Ore visibility/highlight |
| `gold_ore` | Gold Ore visibility/highlight |
| `deepslate_gold_ore` | Deepslate Gold Ore visibility/highlight |
| `emerald_ore` | Emerald Ore visibility/highlight |
| `deepslate_emerald_ore` | Deepslate Emerald Ore visibility/highlight |
| `coal_ore` | Coal Ore visibility/highlight |
| `deepslate_coal_ore` | Deepslate Coal Ore visibility/highlight |
| `iron_ore` | Iron Ore visibility/highlight |
| `deepslate_iron_ore` | Deepslate Iron Ore visibility/highlight |
| `copper_ore` | Copper Ore visibility/highlight |
| `deepslate_copper_ore` | Deepslate Copper Ore visibility/highlight |
| `lapis_ore` | Lapis Ore visibility/highlight |
| `deepslate_lapis_ore` | Deepslate Lapis Ore visibility/highlight |
| `redstone_ore` | Redstone Ore visibility/highlight |
| `deepslate_redstone_ore` | Deepslate Redstone Ore visibility/highlight |

### 3.3 Outline / visibility — 4

| Source key | User-facing capability |
|---|---|
| `blueice` | Blue Ice outline/visibility assistance |
| `deadcoral` | Dead Coral outline/visibility assistance |
| `powdersnow` | Powder Snow outline/visibility assistance |
| `sculk_catalyst` | Sculk Catalyst outline/visibility assistance |

### 3.4 Other visual tweaks — 2

| Source key | User-facing capability |
|---|---|
| `nethertweaks` | Nether-oriented visibility adjustments |
| `stringtweaks` | String / tripwire visibility adjustments |

## 4. Current RPO preset

The supplied RPO preset enables exactly five source capabilities:

```text
blueice = true
deadcoral = true
powdersnow = true
sculk_catalyst = true
stringtweaks = true
```

All other source keys are currently `false`.

Important distinction:

- **Capability baseline:** all 37 capabilities must remain supported.
- **Current preset:** only the five entries above are enabled in the supplied RPO.

Do not mistake the current preset for the complete product scope.

## 5. User-facing configuration principle

Capabilities with different use cases should remain independently controllable even when they share one internal rendering engine.

Example:

```text
Shared highlight renderer
├─ Powder Snow toggle
├─ Blue Ice toggle
├─ Dead Coral toggle
└─ Sculk Catalyst toggle
```

Internal reuse must not force unrelated capabilities behind one switch.

The same config schema and defaults should be shared across 26.1.2, 26.2 and 26.3 unless a documented Minecraft-native difference requires an exception.

## 6. Rendering principle

BlockLens should prefer:

> **active Minecraft/resource-pack texture + BlockLens-generated visual information**

rather than replacing every block with a BlockLens-owned full texture.

This is intended to improve:

- compatibility with the user's active resource pack,
- runtime artifact size,
- startup/resource-reload cost,
- maintainability,
- multi-version migration,
- and accessibility customization.

When exact source appearance requires a unique asset, retain only the minimal asset necessary and verify its redistribution rights before public release.

Minecraft 26.2 introduces a distinct rendering-backend compatibility concern because Vulkan is available as an experimental backend in addition to OpenGL. BlockLens must not claim Vulkan compatibility without separate verification.

## 7. Size requirement

Measured source ZIP:

```text
2,366,865 bytes
```

Hard acceptance threshold for **each supported BlockLens runtime JAR**:

```text
< 1,183,433 bytes
```

Stretch target for **each supported runtime JAR**:

```text
<= 700 KiB (716,800 bytes)
```

The stretch target is subordinate to functional parity, correctness, compatibility, diagnosability, and measured runtime performance.

## 8. Non-functional requirements

### Correctness

- Block-state interpretation must match Minecraft state.
- Orientation/shape indicators must not report a false state.
- Per-feature configuration must persist correctly.
- Features must tolerate simultaneous enablement.
- 26.1.2, 26.2 and 26.3 must preserve the same BlockLens capability contract unless an explicit version exception exists.

### Compatibility

- BlockLens must remain client-only.
- No server-side BlockLens installation is required.
- Missing optional integrations must fail soft.
- Active third-party resource packs should remain usable whenever BlockLens can overlay rather than replace.
- Shader compatibility must be verified for rendering paths that can be affected by shader packs.
- 26.2 OpenGL and Vulkan verification results must be tracked separately.

### Performance

- No unbounded per-tick world scan.
- Prefer event/state-driven updates and retained rendering where practical.
- Avoid repeated allocations in hot render paths.
- Static visual state should not be recomputed every tick without evidence that it is necessary.
- Avoid runtime reflection/classpath scanning for feature discovery.
- Avoid startup networking, telemetry, or legacy-RPO parsing in the normal path.
- Track startup/load, resource-reload, runtime/render, memory, and artifact-size budgets separately.

Detailed performance rules live in `knowledge/current/performance-strategy.md`.

### Testability

- Source capability count and keys must be contract-tested.
- Functional-parity checks must detect accidental deletion/renaming.
- Render/state logic should be separated from Minecraft glue where possible so it can be unit-tested.
- The same common contracts must be executed for 26.1.2, 26.2 and 26.3.
- Minecraft-dependent smoke/GameTests must run against each supported line.

## 9. Explicit non-goals

BlockLens is not intended to become:

- a gameplay automation mod,
- an auto-placement mod,
- a server management mod,
- a generic X-ray mod,
- a source code/asset dump inside a JAR,
- or a feature-count competition with other Tweaks mods.

The product value is **visual clarity and state inspection**.

## 10. Open items requiring source-equivalence capture

The RPO keys identify the capability set but do not, by themselves, fully describe the exact visual contract for every option.

Before implementing each capability, capture its source behavior from the supplied resource pack:

- affected blocks/states,
- exact visual cue,
- texture/model dependency,
- whether orientation, shape, connection, powered/attached state, or outline is encoded,
- interactions with neighboring blocks,
- and screenshots/golden references where practical.

Do not invent missing source behavior and call it parity.
