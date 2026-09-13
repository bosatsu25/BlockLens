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

Target overlap is intentional. Examples include Obsidian (`gaming.obsidian` + `others.nethertweaks`) and Crimson/Warped Stem (`deco.log` + `others.nethertweaks`). All features must remain composable when enabled together.

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

Core implementation and all-on interaction are verified on Minecraft 26.1.2 and 26.2. Representative shader-ON and third-party resource-pack compatibility remain separate M7/M9 verification work and are **not claimed here**.

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
- no copied AMATERAS texture binaries
- overlap cues from M3/M5 remain composable on the same state

The two BlockLens-owned models are part of the actual Fabric resource pack in development, Client GameTest, and packaged runtime JARs. CI fails if either model cannot resolve or has incomplete texture references.

## M7 — Automated full-product integration gate

The shared Client GameTest runs on **both Minecraft 26.1.2 and 26.2**.

The automated scenario currently verifies:

1. all 37 capabilities enabled simultaneously
2. config encode/decode mask round-trip
3. resource reload while all 37 remain enabled
4. framebuffer capture after reload
5. Overworld → Nether → Overworld dimension round-trip
6. Nether Tweaks enabled **alone**
7. frozen five-feature reference preset
8. all 37 disabled and active base rendering restored
9. target-model index remains bounded and zero-world-scan
10. runtime model resources resolve without BlockLens missing-model / missing-texture warnings

### Latest verified run

GitHub Actions run: `34729441969` on the implementation head before documentation-only updates.

| Evidence | Minecraft 26.1.2 | Minecraft 26.2 |
| --- | ---: | ---: |
| all-37 ON vs OFF ROI difference | 3,917 px | 3,932 px |
| reload-after-all-37 ON vs OFF | 3,911 px | 3,904 px |
| Nether Tweaks only vs OFF | 834 px | 765 px |
| five-feature preset vs OFF | 1,067 px | 1,063 px |
| dimension round-trip | PASS | PASS |
| config codec round-trip | PASS | PASS |
| Client GameTest | PASS | PASS |
| BlockLens model resolution | PASS | PASS |

Each version archives five deterministic M7 screenshots plus a manifest and SHA-256 screenshot list:

- `m7-all37-on.png`
- `m7-all37-reloaded.png`
- `m7-nether-only.png`
- `m7-reference-preset.png`
- `m7-all37-off-active-pack.png`

## Runtime artifact size

The run above produced **92,790-byte** runtime JARs for both Minecraft lines, far below the 700 KiB stretch target and 1,183,432-byte hard maximum.

These sizes are implementation evidence for this revision, not a guarantee that later M8/M9 builds remain byte-identical.

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

## What is still not claimed

The automated M7 core gate being green does **not** complete every compatibility and release-readiness item. The following remain explicit future verification/hardening work:

- representative third-party resource-pack matrix
- supported shader path with shader **ON**
- explicit Minecraft 26.2 OpenGL compatibility matrix beyond the CI framebuffer path
- Vulkan experimental backend validation and documented separation from required OpenGL support
- real config-file save/reload acceptance beyond codec/persistence smoke coverage
- final M8 startup/reload/frame/allocation performance evidence
- M9 release-readiness, licensing, attribution, and public artifact audit

Those items must remain unchecked in the roadmap/issues until evidence exists.
