# Bounded world analyzers — implementation contract

This contract precedes #34 implementation. Add five independent, default-OFF capabilities after the existing 46, within the four existing categories. Model targets and the original 37/40/46 identities remain unchanged. Analyze only already loaded client state, without chunk creation, custom packets, server dependencies, automation, or persistent coordinates.

## Source interpretation

Behavior references are ChiseTweaks 9fc977c5fdc0b23040d152216f60d46724cc65d8 and, for the retired debris analyzer, historical 49985425c7a805f94414ee89d557b0c8c214b40a. No source code or artwork is copied. Re-adopting the retired debris behavior is an explicit BlockLens decision; it does not imply it remains in current ChiseTweaks.

- Ancient debris: a 3D sphere from eye to block center, default radius 64, nearest 64 markers. Palette-negative or unavailable 16-cube sections skip individual block reads. Occluded orange frames are separate from ordinary ore models.
- Lava: default horizontal radius 5, vertical 3, nearest 12 markers, default minimum update interval 10 ticks (configurable 5–100). A source lava cell qualifies only if at least one known neighbor among six is not source lava. Unknown neighbors are skipped, never treated as air. All known neighbors source plus unknown neighbors gives no marker.
- Beacon: default H16/V8, nearest eight, interval 20 ticks. Count consecutive fully known beacon-base layers 1–4; incomplete higher layers preserve complete lower layers. Radii 20/30/40/50 are horizontal square outlines centered on the beacon, at block Y+1.05. Beam obstruction and actual effects are not inferred.
- Lightning rod: default H16/V8, nearest eight, interval 20 ticks; horizontal square radius 128, with a target frame. This is not a sphere, weather prediction or a ground contour.
- Villager job site: default H16/V8, nearest 12 among a bounded candidate sample, interval 20 ticks. Only actual JOB_SITE memory in the current dimension, with a loaded endpoint inside the configured query box, can produce a line from villager Y+height*0.65 to site center. This deliberate loaded/bounded endpoint rule is stricter than upstream. Ordinary vanilla clients often have no synchronized memory; explain this limitation in UI/docs. Never infer a nearest workstation.

## Work and memory limits

One shared tick budget, enforced before reads: at most 512 cell reads (including lava neighbors and beacon bases), 64 section/palette probes, and 4096 cursor operations. Round-robin enabled block analyzers share work; a candidate whose maximum read reservation cannot fit pauses until a later tick. Beacon reserves 165 reads, lava seven, others one. Reserving work does not count unused reads as observations. No chunk-load callback bypasses these limits.

A debris scan walks bounded section coordinates, then at most 4096 cells of palette-positive sections over multiple ticks. Other block scans walk the explicit rectangular radius. Each pass captures its eye reference on its first step; candidates are nearest among the bounded observations to that fixed reference, not a live global nearest guarantee. Movement inside the same anchor block can make this ordering approximate until the next pass. After scanning, candidates are published incrementally under the same shared budget: each new marker rechecks its full predicate (including lava neighbors and all required beacon layers) and current radius in its publication tick. Old results for that block analyzer are removed when incremental publication begins. Remaining candidates may take further ticks, so the snapshot is bounded and immutable but is not an atomic whole-pass result. Existing markers recheck source identity/range each tick; dependent neighbor/base changes may remain visible until the next completed pass. Each completed publication cycle exposes a bounded marker snapshot; minimum intervals are delays between completed passes, not a promise to finish dense scans in that interval. Moving out of the scan anchor block restarts the pass; markers outside the current radius, unloaded chunks or replaced targets must not survive validation.

Village sampling visits at most 125 nearby entity sections and 64 entities per update, with no global entity enumeration. Excess candidates are intentionally omitted; dense scenes do not guarantee global nearest selection. Only 12 (configurable up to 24) candidate villagers proceed to memory lookup, preserving upstream truncate-before-memory semantics. Each endpoint read consumes the same cell budget. Section iteration and entities have separate counters.

Published marker capacity equals configured per-feature limits: debris ≤128, lava ≤24, beacon/rod ≤16 each, villager ≤24 (208 total). In-progress block passes retain at most another 184 candidate markers (128+24+16+16); published snapshots plus candidate buffers are bounded to 392 markers, with additional temporary bounded lists during publication. Marker geometry is at most 12 frame lines plus four square lines or one link per marker; no text/NBT/entity identifiers enter evidence. All OFF means zero world, palette and entity queries, empty snapshots, and no geometry. Snapshots contain finite coordinates and are immutable for rendering. World identity changes, disconnect, configuration changes and OFF discard cursors/results; no UUID or coordinate cache is persisted.

Numeric limits: debris radius 16–256, markers 8–128, interval 5–100 (vertical radius equals sphere radius); lava H1–8/V1–5, markers 1–24, interval 5–100; beacon/rod H1–32/V1–16, markers 1–16, interval 5–100; villager H1–32/V1–16, markers 1–24, interval 5–100. UI edits share the existing Save/Apply draft, cancellation and save-failure semantics.

## Required evidence before completion

Pure JUnit budget/geometry/unknown-data/nearest-selection/lifecycle/configuration contracts, selected coverage/PIT, all-three builds, actual rendered OFF/single/all evidence, loaded/missing job memory controls, dimension/disconnect cleanup, reload and numeric UI persistence, OFF/single/all operation/time/allocation observations, reproducible audited runtime JARs ≤153600 bytes, independent review and exact-head hosted CI. No compatibility claim for third-party shaders/Vulkan is made by these tests; #31 remains separate.

## Local verification record

Implementation adds five independent default-OFF analyzers and 19 numeric fields; the runtime catalog contains 51 capabilities. The original 46 identities, seven enabled defaults and model binding/target counts remain unchanged.

Clean `ciGate` passes 201 common JUnit tests with no failures, selected JaCoCo 941/956 covered lines and PIT 310/320 killed mutations. All three compiled/audited runtime JARs remain under the unchanged 143360-byte development guard and 153600-byte release ceiling. Outer and owned inner archive hashes match before/after clean rebuilds; compiled class-byte equality and all 25 archive-negative controls pass.

| Minecraft | Outer bytes | Outer SHA-256 | Packaged native suite |
| --- | ---: | --- | --- |
| 26.1.2 | 123176 | `b1ae443ae62e73fd99c1cba01a40c66c83b73298fe654642d51c2712c97d6dfc` | PASS, 5m16s |
| 26.2 | 123431 | `1bdc0e5074412c3c5ca61e18c111f5f0986f8eda5fd5e499b34bdfe89c75962e` | PASS, 5m19s |
| 26.3 | 123477 | `f0df49de1c8ae7c3a48deb46759da4acec434ded5cc99974f54e0930e117aecf` | PASS, 5m21s |

Each real client loads the outer/nested runtime (native and common class-origin proof), captures ten complete physical 320x240 English/Japanese detail screens and fifteen visual frames, and verifies all 19 numeric draft/save/discard/Escape edits. Brightness-normalized hue controls reject the other four marker colors and require at least twelve added cue pixels. Controlled scenes prove five individual cues, absent/loaded/wrong-dimension/erased client job memories, all-51 coexistence, resource reload and dimension-return cues. Actual disconnect begins with nonempty markers and ends empty. Legacy settings/model/46-visual evidence and unchanged M8 guards also pass. Independent source review found no outstanding required fixes after bounded revalidation, viewpoint, lifecycle and full-viewport repairs.

The test-only END_CLIENT_TICK span records forty ticks per OFF/single/all case. OFF has zero cell/palette/cursor/entity work and zero median allocated bytes in each case. The largest observed work remains within 512 cells, 64 palette sections, 4096 cursor steps, 125 entity sections and 64 entities. Fixed controlled scenes observe these median spans; this includes callback/sampler context and does not attribute whole-client costs to BlockLens or establish general performance improvement.

| Minecraft | OFF median ns (debris case) | Single medians ns: debris / lava / beacon / rod / villager | Single median allocated B | All-51 median ns / allocated B |
| --- | ---: | --- | ---: | --- |
| 26.1.2 | 900 | 29100 / 28700 / 13200 / 12900 / 17700 | 312 each | 39700 / 41368 |
| 26.2 | 1000 | 29300 / 23900 / 13900 / 15500 / 12300 | 312 each | 35300 / 21088 |
| 26.3 | 1000 | 31100 / 24300 / 12800 / 15700 / 12500 | 312 each | 35800 / 21088 |

These observations refer to visible packaged-content Fabric development GameTests on Windows, with stationary controlled scenes and requested M8 FPS 60. Shader/pack/backend compatibility remains #31; measurement variability remains #43. Exact-head hosted common/three-client jobs and the 25-image analyzer artifacts must also pass before merging the associated PR. No release is published by these changes.
