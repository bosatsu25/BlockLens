# Source raw-source parity audit

Status: **comparison complete; executable regression contract added in P0**

## Purpose

This audit answers a stricter question than “does BlockLens expose 37 toggles?”: **does the compact BlockLens runtime preserve the useful behavior encoded by the complete effective source RPO-gated raw source?**

BlockLens does **not** redistribute the source JSON/PNG files byte-for-byte. The product invariant is semantic/function parity: every effective RPO behavior must map to a BlockLens capability and logical target/state contract, while unreachable/source-residue files are not copied into the runtime JAR.

## Comparison inputs

- Source pack (neutral documentation label): `source-1.21.7-v2.zip` — SHA-256 `174a952ef9d385c8a72b9f8509e51ca59dfa5a4cff618d91a4d8d75fa38aaced`, 3,865,945 B, 4,744 ZIP entries.
- Supplied BlockLens: `BlockLens-26.1.2-release.jar` — metadata `0.1.0+mc26.1.2`, SHA-256 `2e12a92ffadc280f3cf3cdf272a338e93a7fc9ad2fb25b4fde52d28a75cef671`, 94,227 B, 69 JAR entries.

> The supplied JAR is not byte-identical to the published v0.1.0 26.1.2 release artifact documented in README. This report therefore treats behavioral/catalog parity separately from release-byte identity.

## Source graph findings

- **37** distinct RPO condition keys.
- **337** `.rpo` sidecars.
- **336** sidecars gate an existing base file.
- 1 orphan duplicate sidecar: `assets/minecraft/blockstates/pale_oak_slab.rpo`; the valid gate is `pale_oak_slab.json.rpo`. Both declare `deco.slabs`.
- **4,196** non-RPO Minecraft assets.
- **4,101** assets reachable from effective RPO-gated roots.
- **95** unreachable/source-residue candidates.

These effective counts match the established M0 source-evidence shape. The extra 337th sidecar does not create a new capability or logical target.

## Group map

| Source group | RPO keys | Sidecars | Existing gated roots | Reachable raw assets | BlockLens v0.1.0 logical coverage |
| --- | ---: | ---: | ---: | ---: | --- |
| Decoration / Orientation | 13 | 266 | 265 | 3,950 | 254 block targets |
| Resource Highlight | 18 | 18 | 18 | 74 | 18 block targets |
| Outline / Visibility | 4 | 18 | 18 | 26 | Blue Ice + 20 Dead Coral forms + Powder Snow + Sculk Catalyst |
| Other | 2 | 35 | 35 | 57 | String Tweaks: 1; Nether Tweaks: 27 |

## Per-capability mapping

| RPO condition | Sidecars | Effective roots | Reachable raw assets | Logical source targets | JAR targets | Result | Notes |
| --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| `deco.anvil` | 1 | 1 | 3 | 1 | 1 | ✅ mapped | |
| `deco.beehive` | 1 | 1 | 10 | 1 | 1 | ✅ mapped | |
| `deco.campfire` | 2 | 2 | 9 | 2 | 2 | ✅ mapped | |
| `deco.fence_gate` | 12 | 12 | 39 | 12 | 12 | ✅ mapped | |
| `deco.froglight` | 3 | 3 | 15 | 3 | 3 | ✅ mapped | |
| `deco.glazedterracotta` | 16 | 16 | 49 | 16 | 16 | ✅ mapped | |
| `deco.grindstone` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `deco.log` | 24 | 24 | 251 | 24 | 24 | ✅ mapped | |
| `deco.slabs` | 62 | 61 | 298 | 61 | 61 | ✅ mapped | one orphan duplicate `pale_oak_slab.rpo`; 61 effective slab targets |
| `deco.stainedglass` | 32 | 32 | 160 | 32 | 32 | ✅ mapped | |
| `deco.stairs` | 58 | 58 | 2758 | 58 | 58 | ✅ mapped | |
| `deco.trapdoor` | 32 | 32 | 120 | 21 | 21 | ✅ mapped | 21 blockstate gates + 11 model gates; 21 logical block targets |
| `deco.wood` | 22 | 22 | 237 | 22 | 22 | ✅ mapped | |
| `gaming.ancient_debris` | 1 | 1 | 6 | 1 | 1 | ✅ mapped | |
| `gaming.coal_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.copper_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.deepslate_coal_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.deepslate_copper_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.deepslate_diamond_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.deepslate_emerald_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.deepslate_gold_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.deepslate_iron_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.deepslate_lapis_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.deepslate_redstone_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.diamond_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.emerald_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.gold_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.iron_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.lapis_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.obsidian` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `gaming.redstone_ore` | 1 | 1 | 4 | 1 | 1 | ✅ mapped | |
| `outline.blueice` | 1 | 1 | 1 | 1 | 1 | ✅ mapped | one gated texture maps to Blue Ice |
| `outline.deadcoral` | 15 | 15 | 15 | 20 | 20 | ✅ mapped | 15 texture gates cover 20 logical forms because wall-fans reuse fan textures |
| `outline.powdersnow` | 1 | 1 | 1 | 1 | 1 | ✅ mapped | one gated texture maps to Powder Snow |
| `outline.sculk_catalyst` | 1 | 1 | 9 | 1 | 1 | ✅ mapped | state-aware behavior remains represented by BlockLens semantic state |
| `others.nethertweaks` | 34 | 34 | 34 | 27 | 27 | ✅ mapped | 33 texture gates + one magma model gate represent 27 logical Nether targets |
| `others.stringtweaks` | 1 | 1 | 23 | 1 | 1 | ✅ mapped | source gate is Tripwire; P0 later adds Tripwire Hook additively |

## Frozen v0.1.0 conclusions

1. **No RPO capability key is missing:** 37 distinct source conditions map to the 37 compiled v0.1.0 capabilities.
2. **Effective logical target coverage is preserved:** the frozen source contract contains **323 capability-to-target bindings / 320 unique block targets**.
3. Decoration contributes 254 target bindings, Resource 18, Visibility/String 24, and Nether 27.
4. The apparent `337 vs 336` discrepancy is an orphan duplicate sidecar, not missing BlockLens behavior.
5. Thousands of source helper models/textures are intentionally collapsed into semantic state + compiled target catalogs + procedural cues. Literal source-byte inclusion would destroy the size advantage without adding user-visible capability.
6. The 95 unreachable source assets remain excluded unless new evidence shows they are functionally reachable.

## P0 / ChiseTweaks migration relationship

P0 extends BlockLens beyond the frozen 37-capability source contract. The original 37 entries stay stable while ChiseTweaks-derived behavior is additive. The current P0 design is **40 capabilities / 328 bindings / 322 unique targets**.

Reviewed target additions to original source capabilities are limited to:

- `others.stringtweaks` → `tripwire_hook`
- `others.nethertweaks` → `polished_basalt`

The other three P0 additions are independent Resource capabilities: Crying Obsidian, Nether Gold Ore, and Nether Quartz Ore.

## Executable regression gate

`SourceRawParityContractTest` now enforces in CI:

- exactly 37 frozen source rows;
- exactly 336 effective gated roots from the frozen machine-readable contract;
- exactly 323 frozen capability-target bindings;
- exactly 320 frozen unique targets;
- every frozen source key still resolves to a runtime `CapabilityId`;
- every frozen logical target remains present in the compiled runtime catalog;
- only the two reviewed additive target extensions above are allowed on the original capabilities;
- this audit retains the raw-source evidence boundary (337 / 336 / 4,101 / 95).

This keeps raw-source parity separate from additive ChiseTweaks-derived features and makes accidental deletion of a source behavior a hard CI failure.
