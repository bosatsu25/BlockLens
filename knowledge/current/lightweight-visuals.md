# Lightweight visual and comfort features

Issue #42 adds six independent capabilities after the existing 40, preserving their keys, ordinal order, defaults and rendering. All three Minecraft targets use the same pure policies and settings draft, with narrow version adapters.

| Capability | Default | Contract |
| --- | --- | --- |
| Glass Highlight | Off | Add a cyan bracket/diamond cue to 35 exact glass targets; panes use an amber central cue. Keep active-pack base geometry and glass connections. |
| Kelp Highlight | Off | Add crossed magenta/orange ribbons to kelp and kelp plants; eight animation frames, three ticks per frame with interpolation. |
| Bright Concrete | On | Make the active-pack white concrete model emissive without replacing its geometry or texture. |
| Low Fire | Off | Change only the first-person fire overlay. Large/medium/small use vertical scales 0.78/0.56/0.34 and offsets -0.14/-0.24/-0.34. Medium is the default preset. |
| Handheld Size | Off | Change only first-person item models, keeping arm movement and inventory/third-person rendering. Blocks/items/tools default to 70/60/75 percent, bounded to 40–100. Shield uses 95 percent. |
| Bright Chest | On | Use BlockLens-owned white textures and full light for regular single/left/right chests. Preserve models, lid animation and other chest materials. |

Numeric options are edited in the parent settings draft. Only Save and Apply writes them; discard/Esc restores the baseline. Invalid individual values retain defaults or the preceding valid value. Existing boolean configuration remains compatible.

Source behavior is a reference. Runtime textures and models are original procedural BlockLens assets; no third-party source or artwork is redistributed. Normal startup performs no network calls, reflection discovery or registry scans.

Completion requires pure policy/config/draft tests, original-capability regression contracts, all three builds, actual visible client UI and rendering GameTests, resource reload, all-on interaction, artifact/size audits, independent review and exact-head CI. Shader, Vulkan and exact third-party pack combinations remain tracked separately in #31 until exercised.

## Reviewed size baseline

Six capabilities add numeric configuration and shared settings controls, three narrow render hooks, lazy overlay infrastructure, bilingual descriptions and original tiny assets. Final Windows Java 25 artifacts measure 141,994 / 142,696 / 142,779 bytes for 26.1.2 / 26.2 / 26.3. The 111 KiB no-growth baseline belongs to the preceding 40-capability product and cannot describe this additive scope. The new reviewed baseline is 140 KiB (143,360 bytes); the 150 KiB release budget and source-pack half-size hard maximum stay unchanged. No capability is removed to reduce size. All three local builds and artifact-residue audits passed; exact-head hosted CI remains required. These development artifacts are not release assets.

Local verification passed 167 JUnit tests, selected JaCoCo and PIT 265/274 (97%), plus actual visible client UI/rendering GameTests on 26.1.2, 26.2 and 26.3. All 32 additional visual and 10 bilingual settings captures per version were retained. Independent review findings for stale widget focus, cache publication, missing pane cues and weak animated-water controls were reproduced and repaired. Exact-head hosted CI and merge remain pending.

## Rendered verification protocol

The shared client oracle captures 32 PNGs per version, separate from the existing 10 bilingual settings and five original all-40 regression screenshots. Plain and stained panes have separate left/right amber-color controls in a pane-only fixture, tested with the existing Stained Glass control both OFF and ON; the rails lie just outside the existing post and retain its connections.

Kelp motion uses the BlockLens magenta/orange cue shape after six ticks. A higher-priority original static-control texture must win resource selection and retain at least 80 visible cue pixels. Animated change must reach 80 pixels, static residual must remain at most 12, and the animated change must exceed 8 times that residual. Fixed-camera static samples have a small raster/fringe displacement; water/base color changes alone cannot pass this paired protocol.

Chest evidence verifies white-color gains separately for regular single, left and right variants, with unchanged white counts for trapped and ender chests. Controlled client lid events demonstrate opening with the feature OFF, preservation while enabling it, changed rendered open silhouette and subsequent closure. These events exist only in the client test fixture.

Fire evidence samples actual submitted pose matrices for all three presets. Both first-person hands are measured for block, item, tool and shield categories. The all-46 fixture enables every control, reloads resources and keeps fire and both hand submissions active. M8 uses representative added world targets and retains its original coarse guards; noisy whole-client observations do not establish an FPS improvement.

The preceding PR #45 Windows artifacts measure 112,628 / 113,335 / 113,379 bytes. The final additive artifacts therefore grew by 29,366 / 29,361 / 29,400 bytes. This change keeps the 150 KiB ceiling, uses no copied artwork and does not change the published 0.2.2 release.

| Local target | Additional images | Kelp animated/static shape pixels | Reload median (ns) |
| --- | ---: | ---: | ---: |
| 26.1.2 | 32 | 156 / 3 | 2987527400 |
| 26.2 | 32 | 189 / 6 | 2978699000 |
| 26.3 | 32 | 245 / 6 | 2969815800 |

Each local manifest passed the unchanged 6.0 s reload, 2.5 s rebuild and 32 MiB allocation guards. These are representative coarse observations, not statistical confidence intervals or an FPS improvement.
