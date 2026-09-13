# M5 Active Resource-Pack Preservation Evidence

Status: **verified deterministic compatibility evidence** for Minecraft 26.1.2 and 26.2.

## Goal

Prove that Resource Highlight wraps the already-active baked Minecraft/resource-pack model instead of replacing it with BlockLens-owned base geometry.

This evidence intentionally uses a deterministic non-vanilla GameTest resource pack. It verifies the compatibility mechanism and reload behavior; it is **not** a claim that arbitrary third-party packs, shaders, or custom rendering backends have all been tested.

## Fixture

The test-only pack `blocklens-gametest:m5-active-pack` overrides three representative M5 targets:

- `diamond_ore`: half-height geometry with a magenta-concrete marker token
- `deepslate_redstone_ore`: narrow full-height geometry with a lime-concrete marker token
- `obsidian`: inset geometry with a yellow-concrete marker token

The fixture lives under `gametest/resources` only and adds no production runtime asset.

## Oracle

For each supported Minecraft line the real Client GameTest captures:

1. active-pack models with every BlockLens capability OFF
2. the same scene with exactly the three representative Resource Highlight capabilities ON
3. an air-control frame with the three target blocks removed

Acceptance requires:

- all three model resources resolve from the active fixture pack
- each resolved model payload contains its expected marker texture token
- M5 ON differs materially from OFF
- OFF and ON both retain a substantial target foreground mask relative to air control
- ON retains at least 95% of the OFF/base foreground
- the model pipeline remains ready and bounded after resource reload
- the original runtime configuration is restored

Exact framebuffer marker RGB counts are diagnostic only. Lighting and render transforms can change source texture RGB values before capture, so marker identity is established from the active `ResourceManager` source and model payload rather than screenshot color equality.

## Verified CI evidence

GitHub Actions `34734428301` on commit `4846411f1af3d4dfb84fbb26cd40695d5f55b1a5` passed common JUnit/JaCoCo/PIT and both real-client version jobs.

| Metric | Minecraft 26.1.2 | Minecraft 26.2 |
| --- | ---: | ---: |
| fixture models | 3 | 3 |
| resolved from fixture pack | PASS | PASS |
| marker payload tokens | PASS | PASS |
| wrapped models after reload | 7,820 | 7,820 |
| ON vs OFF different pixels | 2,097 | 2,100 |
| OFF foreground pixels | 904 | 906 |
| ON foreground pixels | 2,308 | 2,311 |
| retained OFF/base pixels | 904 | 906 |
| base retention | **100.0%** | **100.0%** |
| manual screenshot review | PASS | PASS |

The archived screenshot triplets were manually reviewed. On both versions, OFF preserves the non-vanilla fixture geometry, ON preserves that same base geometry while adding the Resource Highlight cue, and the air control removes only the three fixture targets.

## Artifacts

Run `34734428301` archives:

- `blocklens-m5-pack-visual-26.1.2`
- `blocklens-m5-pack-visual-26.2`

Each artifact contains:

- `m5-pack-off.png`
- `m5-pack-on.png`
- `m5-pack-air-control.png`
- `m5-pack-visual-manifest.txt`
- `SHA256SUMS.txt`

## Compatibility boundary

This gate now proves the active baked-model preservation mechanism across both supported Minecraft lines with a representative non-vanilla resource pack and an actual resource reload.

Still not claimed by this evidence:

- arbitrary third-party resource-pack compatibility
- shader-ON compatibility
- Minecraft 26.2 Vulkan compatibility
- M8 resource-reload timing or render-allocation performance budgets
