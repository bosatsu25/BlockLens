# BBE fixture: isolate kelp animation from the water background

This is a test-fixture change, not a change to the shipped kelp or water renderer.

## Observed repeat

The original four-mod RED job in run 38147784374 measured static kelp-control shape residual 13 against an unchanged limit of 12 (animated 249). Both stacks then passed in run 38148643149 with residual 8. A documentation-only commit 19a0aa72 again failed the minimal stack in run 38149338461: animated=252, static=13, static visible cue=234. The failure occurs before the chest scene and does not negate the separate reproduced chest defect or its corrective run.

The paired screenshots contain water-texture animation behind/in front of a static kelp cue. Pixel inspection placed the mismatches at the submerged cue fringes, not a new kelp animation. The existing one-pixel shape-neighborhood matcher already tolerates raster fringe displacement; enlarging it or raising the 12-pixel gate would change acceptance without controlling the other animated input.

## Controlled experiment

The focused BBE job therefore generates original single-frame transparent water textures and explicitly selects them in an isolated resource pack. The same constant water background is used for BOTH positive animated-kelp and negative static-kelp samples. The real kelp renderer, animated frames, static kelp texture, six-tick interval, 80-pixel motion requirement, 12-pixel negative limit and 8x separation requirement are unchanged. Water geometry and fluid state remain intact. A positive motion result can no longer be supplied by water-texture animation.

The generator writes only water_still/water_flow PNGs, empty animation metadata and pack metadata. Unit tests independently verify PNG chunks/CRCs, RGBA dimensions, transparency, one-frame layout, deterministic hashes, absence of kelp replacement assets and refusing to overwrite an existing pack. The native fixture verifies both water resources' active pack identity before and after the visual oracle and restores the prior pack selection on exit. Generated inputs are hash-recorded in the artifact manifest.

This control applies only to the additional BBE profile. Normal three-version and existing external workflows keep their original resource-pack inputs. It is not a claim of whole-modpack pixel parity or an excuse to change the user's water textures. The distribution JAR is unaffected by this fixture.
