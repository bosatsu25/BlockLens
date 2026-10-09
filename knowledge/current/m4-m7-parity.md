# M4–M7 Core Parity and Integration Evidence

Status: **authoritative current implementation evidence** for the M4–M7 core rendering train.

This document records what is implemented and automatically verified today. It does **not** turn unverified shader, third-party resource-pack, Vulkan, or performance claims into supported behavior.

## Scope

The current BlockLens product contract contains 37 independently configurable visual capabilities:

- 13 Decoration / Orientation capabilities
- 18 Resource Highlight capabilities
- 4 Outline / Visibility capabilities
- Nether Tweaks
- String Tweaks

The unified target catalog contains:

- **323 capability-to-target bindings**
- **320 unique Minecraft block targets**

Target overlap is intentional. Examples include Obsidian (`gaming.obsidian` + `others.nethertweaks`) and Crimson/Warped Stem (`deco.log` + `others.nethertweaks`). All features remain composable when enabled together.

## M4 — Outline / Fine Visibility

Implemented capabilities:

- Blue Ice
- Dead Coral
- Powder Snow
- Sculk Catalyst
- String Tweaks

Current behavior:

- exact source-backed targets only
- shared high-visibility procedural cues
- Sculk Catalyst preserves the `bloom` semantic
- Tripwire preserves N/E/S/W connections plus `powered` and `attached`
- all **64 tripwire semantic states** are covered by common regression tests
- state extraction is verified after a real Minecraft client bootstrap on both supported lines

## M5 — Resource Highlighting

All 18 frozen resource capabilities are implemented in the unified baked-model wrapper.

BlockLens retains the active baked base model and applies BlockLens-owned full-bright accent information. Full-bright uses the Fabric quad API as a unit:

- emissive = true
- diffuse shade = false
- ambient occlusion = false

This avoids copying the source pack's animated resource textures while preserving high-visibility intent.

### Dedicated dark-area rendered evidence

M5 has a dedicated real-client framebuffer oracle separate from the all-37 M7 scene. The scene contains only the 18 resource-highlight targets inside a sealed, unlit black-concrete room at midnight. All non-resource BlockLens capabilities are OFF for the ON capture; all 37 capabilities are OFF for the control capture.

The gate verifies:

- exactly 18 resource capabilities are enabled
- the OFF control is genuinely dark
- ON and OFF differ materially inside a fixed target ROI
- a material number of pixels become brighter with M5 enabled
- total target-region luminance increases
- the model pipeline is ready and bounded
- the original runtime config is restored after capture

GitHub Actions `34731643950` (run #168) produced:

| Dark-area evidence | Minecraft 26.1.2 | Minecraft 26.2 |
| --- | ---: | ---: |
| resource capabilities enabled | 18 | 18 |
| ON vs OFF different pixels | **5,041 px** | **5,051 px** |
| pixels materially brighter | **4,758 px** | **4,769 px** |
| OFF average luminance | **1** | **1** |
| ON average luminance | **5** | **5** |
| total ROI luminance gain | **361,663** | **362,456** |
| manual screenshot review | PASS | PASS |

The archived ON/OFF screenshots were manually reviewed. In both Minecraft lines the OFF scene leaves the same 18-block arrangement nearly submerged in darkness, while the ON scene makes the resource accents clearly visible without changing camera, layout, or enabling another BlockLens visual family.

This completes the dedicated dark-area/high-visibility framebuffer acceptance item. Representative shader-ON verification and third-party resource-pack compatibility remain separate verification work and are **not claimed here**.

## M6 — Nether Tweaks

The pinned source ZIP was inspected directly rather than guessing the visual rule.

### Source-derived grammar

Most 16×16 Nether Tweaks textures reduce the block surface to:

- a **14×14 flat interior** = 196 pixels
- a **1-pixel frame** = 60 pixels

Nylium additionally uses an upper color band. BlockLens recreates that grammar procedurally instead of redistributing the source PNG files.

### BlockLens implementation

- exact 27-target catalog
- source-derived palette table per target
- existing baked model retained as the base/frame geometry
- reusable BlockLens-owned `interior_fill` extra model
- reusable BlockLens-owned `upper_band` extra model for Nylium
- no copied source texture binaries
- overlap cues from M3/M5 remain composable on the same state

The two BlockLens-owned models are part of the actual Fabric resource pack in development, Client GameTest, and packaged runtime JARs. CI fails if either model cannot resolve or has incomplete texture references.

## M7 — Automated full-product integration gate

The shared Client GameTest runs on **both Minecraft 26.1.2 and 26.2**.

The automated scenario verifies:

1. all 37 capabilities enabled simultaneously
2. config encode/decode mask round-trip
3. **real native `config/blocklens.properties` save → reload → runtime publication → exact restoration**
4. resource reload while all 37 remain enabled
5. framebuffer capture after reload
6. Overworld → Nether → Overworld dimension round-trip
7. Nether Tweaks enabled **alone**
8. frozen five-feature reference preset
9. all 37 disabled and active base rendering restored
10. dedicated M5 resource-only dark-area framebuffer evidence
11. target-model index remains bounded and zero-world-scan
12. runtime model resources resolve without BlockLens missing-model / missing-texture warnings

### Native config-file reload acceptance

The config acceptance does not stop at codec serialization. `ConfigFileReloadOracle` uses the real Fabric config directory and the existing production persistence implementation:

- read the existing `blocklens.properties`
- invert all 37 capability booleans so the changed state cannot pass vacuously
- persist through `BlockLensConfigFiles.save(...)`
- verify canonical file contents and no leaked `.tmp`
- reload through `BlockLensRuntime.reloadConfig(...)`
- verify the runtime enabled mask matches the persisted config
- verify Minecraft version identity and startup timing are unchanged
- restore the exact original config text and runtime config before visual tests continue

The current integration run still produces `BLOCKLENS_CONFIG_FILE_RELOAD ... result=PASS` on both Minecraft lines. `reloadConfig(...)` is an explicit configuration boundary. It performs filesystem I/O and is **not** a render-hot-path operation.

### Latest verified run

GitHub Actions run: **`34731643950` (run #168)**.

| M7 evidence | Minecraft 26.1.2 | Minecraft 26.2 |
| --- | ---: | ---: |
| all-37 ON vs OFF ROI difference | **3,908 px** | **3,914 px** |
| reload-after-all-37 ON vs OFF | **3,934 px** | **3,909 px** |
| Nether Tweaks only vs OFF | **834 px** | **765 px** |
| five-feature preset vs OFF | **1,090 px** | **1,072 px** |
| dimension round-trip | PASS | PASS |
| config codec round-trip | PASS | PASS |
| native config-file save/reload | PASS | PASS |
| M5 dark-area evidence | PASS | PASS |
| Client GameTest | PASS | PASS |
| BlockLens model resolution | PASS | PASS |

Each version archives five deterministic M7 screenshots plus a manifest and SHA-256 screenshot list, and two dedicated M5 dark-area screenshots plus their own manifest and SHA-256 list.

## Runtime artifact size

Run `34731643950` produced **92,988-byte** runtime JARs for both Minecraft lines, far below the 700 KiB stretch target and 1,183,432-byte hard maximum.

Verified SHA-256 remains:

- 26.1.2: `bf07d2f19f4edeb3a4ec630e62449e01ab83dd0010e2105932cabf84a98a2857`
- 26.2: `13af1b1d7ba9ee17f693202862226c43d687540b6f2b94c5cd0e41c1c297f272`

The M5 dark-area test adds no runtime asset or production byte growth; it is test/CI evidence. These sizes are implementation evidence for this revision, not a guarantee that later M8/M9 builds remain byte-identical.

## Architecture invariants retained

- client-only Fabric mod
- one common product policy shared by both Minecraft lines
- thin version adapters
- no runtime RPO parsing
- no classpath/reflection feature discovery
- no whole-world render scan
- raw-ID target bitmask lookup
- semantic interpretation at model bake, not on every frame
- primitive enabled-capability mask on the render hot path
- direct wrapped-model emission when no represented capability is enabled
- active baked model remains the base rather than copied third-party base textures
- native config reload is explicit and outside the render hot path

## What is still not claimed

The automated core gates being green do **not** complete every compatibility and release-readiness item. The following remain explicit future verification/hardening work:

- representative third-party resource-pack matrix
- supported shader path with shader **ON**
- explicit Minecraft 26.2 Vulkan experimental backend validation
- final M8 startup/reload/frame/allocation performance evidence
- M9 release-readiness, licensing, attribution, compatibility notes, clean Prism/Fabric smoke tests, and public artifact audit

Those items remain unchecked in the roadmap/issues until evidence exists.
