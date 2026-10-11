# Cele rendering-stack regression — BlockLens 0.4.1

Status: native RED reproduced; corrective implementation awaiting exact-head CI. Do not infer compatibility success from this document.

## Observed failures and causes

The supplied Cele-Modpack-v1.1 enables BBE's master and chest optimization. Closed chests use BBE terrain emission, which does not call the vanilla ChestRenderer.submit material hook. Opening a chest temporarily uses the immediate renderer, explaining the reported open-dependent white texture. Calling vanilla terrain invalidation alone cannot add a material change to the bypassed pipeline.

Native baseline: commit `89dc1c6b7bf433dd71a09d6967c5f2ddbca78b47`, [Cele rendering compatibility run 38147784374](https://github.com/bosatsu25/BlockLens/actions/runs/38147784374), BBE/Sodium job 114494658606. The actual optimized emission counters recorded enabled=6 / disabled=3 and the unchanged framebuffer assertion failed with `BRIGHT_CHEST has no visible isolated change: 0`. Glass, panes, kelp animation and bright concrete ran before this failure. The test did not disable BBE to manufacture a passing vanilla path.

The supplied `BlockLens-26.3-v0.4.0.jar` has SHA-256 `2c9d582650b7e3a6c32cd09b558b4818e1c895c9fce9fb1232b7731bc5008920`, unlike the main `ebe0b1d3` CI artifact (`1eee2d76ed1911a215adb608fe8a6a688e6d32474f6eac2012c9eaa232dc59e0`). Its outer resources lack the ore-border model/texture and its owned runtime lacks FaceInstructionTable and ResourceBorderPolicy. Their same visible version is not proof of identical implementation. Existing vanilla chest bytecode is identical, so replacing that older JAR alone cannot repair BBE rendering.

## Repair boundary

The optional adapter is applied only with Minecraft 26.3, BBE 1.3.9+mc26.3 and Sodium 0.9.2+mc26.3. It replaces regular single/left/right material identities at BBE selection, registers BlockLens's existing original chest artwork in the terrain atlas, and sets final optimized quads emissive. Only BBE flag 2 (its non-emissive custom lighting route) is cleared for those owned materials; other flags and optimized geometry/scheduling remain intact. Sprite identities, not atlas objects, are retained so reload resolves fresh atlas resources.

Trapped, ender, copper and Christmas materials are not substituted. The base mod does not require BBE/Sodium. Unsupported versions do not activate this adapter. There is no automatic configuration edit or mod disabling. Existing 55 capabilities, source keys, defaults, original AMATERAS controls and removed Masa/debris-analyzer decisions are unchanged.

## Automated evidence

The two added native profiles use exactly hash-verified public dependencies and synthetic settings:

| Profile | Exact dependencies | Settings |
| --- | --- | --- |
| bbe-sodium | BBE 1.3.9+mc26.3, Sodium 0.9.2+mc26.3 | Master/chest optimization and chest animation ON |
| cele-render | Above plus Iris 1.11.7+mc26.3, Continuity 3.0.1+26.3 | Shaders OFF; connected/emissive textures ON |

Tests cover actual optimized emission, closed chest UI Save/Apply and OFF restoration without opening, all three regular parts, non-regular negatives, animation, resource reload, working glass/panes/kelp, first-person comfort transforms and ore borders. The normal three-version graph, 8 existing shader/Vulkan profiles, JaCoCo/PIT thresholds, reproducibility and 204800-byte ceiling remain required. This is rendering-stack evidence, not an execution of every mod/world/server combination in the private modpack. It does not measure an FPS improvement.

## Reference-pack scope and upgrade

Replace the old BlockLens JAR with the verified 0.4.1 artifact matching the Minecraft version; keep only one BlockLens JAR. Preserve existing configuration and BBE optimization. Ore highlighting and first-person fire/handheld controls retain their existing independent switches and profile options.

The Chise reference's pale-oak item/entity/GUI palette and LowOnFire's placed-world fire models are not implemented by this chest compatibility patch. Existing orientation cues or first-person low-fire transforms are not equivalent to those additional visual assets. Original source resource packs and third-party artwork are not redistributed by this change.
