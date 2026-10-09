# BlockLens M0 Capability Map

Status: **authoritative source-to-product baseline**

This document was generated from the pinned source ZIP/RPO baseline. It maps all 37 RPO conditions to the files and Minecraft states that actually implement them.

## Evidence summary

- RPO conditions: **37 / 37**
- RPO rule files parsed successfully: **336 / 336**
- Source assets reachable from gated files: **4101 / 4196 (97.74%)**
- Candidate unreferenced/source-residue assets: **95**
- Shared transitive dependencies across capabilities: **6**
- Explicit source-pack license file: **not found**

No source binary asset is approved for redistribution by this mapping. Until rights are confirmed, BlockLens uses the pack as behavior/reference evidence and prefers independently generated overlays/geometry.

## Complete capability mapping

| Capability | Preset | Direct gated files | Dependency closure | Targets | State properties / source meaning |
|---|---:|---:|---:|---|---|
| `deco.anvil` — Anvil | OFF | 1 | 3 | `anvil` | Show anvil orientation<br>facing=east,north,south,west |
| `deco.beehive` — Beehive | OFF | 1 | 10 | `beehive` | Show beehive orientation<br>facing=east,north,south,west; honey_level=0,1,2,3,4,5 |
| `deco.campfire` — Campfire | OFF | 2 | 9 | `campfire`, `soul_campfire` | Show campfires orientation<br>facing=east,north,south,west; lit=false,true |
| `deco.fence_gate` — Fence Gate | OFF | 12 | 39 | 12 blocks; see machine baseline | Show fence gates orientation<br>facing=east,north,south,west; in_wall=false,true; open=false,true |
| `deco.froglight` — Flog Light | OFF | 3 | 15 | `ochre_froglight`, `pearlescent_froglight`, `verdant_froglight` | Show flog lights orientation<br>axis=x,y,z |
| `deco.glazedterracotta` — Glazed Terracotta | OFF | 16 | 49 | 16 blocks; see machine baseline | Show glazed terracottas orientation<br>facing=east,north,south,west |
| `deco.grindstone` — Grindstone | OFF | 1 | 4 | `grindstone` | Show grindstone orientation<br>face=ceiling,floor,wall; facing=east,north,south,west |
| `deco.log` — Log | OFF | 24 | 251 | 24 blocks; see machine baseline | Show logs orientation<br>axis=x,y,z |
| `deco.slabs` — Slabs | OFF | 61 | 298 | 61 blocks; see machine baseline | Show slabs states<br>type=bottom,double,top |
| `deco.stainedglass` — Stained Glass | OFF | 32 | 160 | 32 blocks; see machine baseline | Make stained glasses opaque<br>east=false,true; north=false,true; south=false,true; west=false,true |
| `deco.stairs` — Stairs | OFF | 58 | 2758 | 58 blocks; see machine baseline | Show stairs states<br>facing=east,north,south,west; half=bottom,top; shape=inner_left,inner_right,outer_left,outer_right,straight |
| `deco.trapdoor` — Trapdoor | OFF | 32 | 120 | 21 blocks; see machine baseline | Show trapdoors orientation<br>facing=east,north,south,west; half=bottom,top; open=false,true |
| `deco.wood` — Wood | OFF | 22 | 237 | 22 blocks; see machine baseline | Show woods orientation<br>axis=x,y,z |
| `gaming.ancient_debris` — Ancient Debris | OFF | 1 | 6 | `ancient_debris` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.coal_ore` — Coal Ore | OFF | 1 | 4 | `coal_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.copper_ore` — Copper Ore | OFF | 1 | 4 | `copper_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.deepslate_coal_ore` — Deepslate Coal Ore | OFF | 1 | 4 | `deepslate_coal_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.deepslate_copper_ore` — Deepslate Copper Ore | OFF | 1 | 4 | `deepslate_copper_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.deepslate_diamond_ore` — Deepslate Diamond Ore | OFF | 1 | 4 | `deepslate_diamond_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.deepslate_emerald_ore` — Deepslate Emerald Ore | OFF | 1 | 4 | `deepslate_emerald_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.deepslate_gold_ore` — Deepslate Gold Ore | OFF | 1 | 4 | `deepslate_gold_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.deepslate_iron_ore` — Deepslate Iron Ore | OFF | 1 | 4 | `deepslate_iron_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.deepslate_lapis_ore` — Deepslate Lapis Ore | OFF | 1 | 4 | `deepslate_lapis_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.deepslate_redstone_ore` — Deepslate Redstone Ore | OFF | 1 | 4 | `deepslate_redstone_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.diamond_ore` — Diamond Ore | OFF | 1 | 4 | `diamond_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.emerald_ore` — Emerald Ore | OFF | 1 | 4 | `emerald_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.gold_ore` — Gold Ore | OFF | 1 | 4 | `gold_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.iron_ore` — Iron Ore | OFF | 1 | 4 | `iron_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.lapis_ore` — Lapis Ore | OFF | 1 | 4 | `lapis_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.obsidian` — Obsidian | OFF | 1 | 4 | `obsidian` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `gaming.redstone_ore` — Redstone Ore | OFF | 1 | 4 | `redstone_ore` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `others.nethertweaks` — Nether Tweaks | OFF | 34 | 34 | 27 blocks; see machine baseline | Simplified view of blocks in nether |
| `others.stringtweaks` — String Tweaks | ON | 1 | 23 | `tripwire` | Make the strings easier to see.<br>attached=false,true; east=false,true; north=false,true; powered=false,true; south=false,true; west=false,true |
| `outline.blueice` — Blue Ice | ON | 1 | 1 | `blue_ice` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `outline.deadcoral` — Dead Coral | ON | 15 | 15 | 20 blocks; see machine baseline | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `outline.powdersnow` — Powder Snow | ON | 1 | 1 | `powder_snow` | Source asset replacement/highlight; exact pixels frozen by source ZIP SHA-256. |
| `outline.sculk_catalyst` — Sculk Catalyst | ON | 1 | 9 | `sculk_catalyst` | bloom=false,true |

## Source-defined semantic findings

- `stairs`: 58 gated blockstate files, 2,320 source variants. Semantics encode `facing`, `half`, and five `shape` values.
- `slabs`: 61 gated blockstate files. Semantics encode `bottom`, `top`, and `double`.
- `trapdoor`: 32 gated blockstate files. Semantics encode `facing`, `half`, and `open`.
- `fence_gate`: 12 gated blockstate files. Semantics encode `facing`, `in_wall`, and `open`.
- `log` / `wood` / `froglight`: axis is explicitly encoded as `x/y/z`.
- `stainedglass`: source tooltip is **Make stained glasses opaque**; this is not merely an outline feature.
- `stringtweaks`: the gated `tripwire.json` encodes attached/powered/connectivity state; 64 source variants are present.
- `nethertweaks`: source tooltip is **Simplified view of blocks in nether**. The gate controls 33 textures plus the magma-block model and targets 27 Nether-oriented blocks/materials.
- `gaming.*`: each source blockstate redirects to a dedicated animated texture/model (`frametime: 2` on the custom `.mcmeta` files). BlockLens must preserve the visibility/highlight value, not necessarily the exact static asset mechanism.

## Golden-reference policy

The pinned source ZIP/RPO SHA-256 values plus the machine-readable capability contract and source-gate map are the M0 golden source references without redistributing source binary assets into this repository.

Visual in-game screenshots remain useful during renderer implementation, but they are not required to identify the source capability contract: the RPO gate, source tooltip, target/state mapping, exact source ZIP hash, and direct source-file paths are frozen here.

## Unreferenced/source-residue candidates

Automated dependency tracing found **95** asset files that are not reachable from any of the 37 gated roots. They must not be copied into BlockLens merely because they exist in the source ZIP. See `source-unreferenced-assets.txt`.

## M0 exit decision

**PASS for implementation start.** All 37 conditions are mapped to evidence-backed source roots, targets/state semantics are captured where encoded by the source, ambiguous `nethertweaks` scope is now explicit, and source binary redistribution is fail-closed pending rights confirmation.
