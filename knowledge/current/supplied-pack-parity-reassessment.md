# Supplied-pack parity reassessment — 2026-10-12

Status: Issue #64 remains OPEN. A preset, resource lookup or passing build does not establish
complete source behavior. Preserve all current 55 controls, original 37 source identities,
configuration keys/defaults, three supported versions and the 204800-byte runtime budgets.

## Immediate corrections

- PR #67 fixes the exact 26.3 optimized BBE regular-chest path. Its current-head CI passed all
  14 mandatory jobs; same-commit external and BBE jobs now gate automatic publication.
- Consolidate the two identical terrain wrappers into one shared source plus three lighting
  adapters. Keep active-base geometry, OFF/reload behavior, independent cue composition and
  shader/backend gates unchanged. The new source contract first failed, then all common
  316 tests, JaCoCo/PIT and three builds/native compilations passed locally.
- Include the previously omitted server Chise archive in the private six-pack tests. Preserve
  its original format declaration, and require both orders, 29 exact asset probes, six changed
  winners, ON/OFF/air controls and restoration. Python regression suite: 77 passing tests. Actual Windows packaged six-pack clients pass
  on 26.1.2, 26.2 and 26.3; each independent collector accepts all 12 result files.
- Use neutral project/fixture labels. The supplied filenames remain private input data;
  no asset bytes, machine paths or copied third-party artwork enter the public runtime.

## Behavior gaps found by inspecting actual inputs

| Input | Established behavior | Remaining implementation / proof |
| --- | --- | --- |
| Source reference UI | Frozen 37 keys/323 target bindings; current category settings and existing visual cues | New source hash is independently pinned; mapping is semantic parity, not original-pixel duplication |
| Client Chise | Bright concrete; three regular chest parts | Actual closed-chest behavior and external priority need the corresponding native renderer evidence |
| Server Chise | Same concrete/chest affordance | Eight pale-oak block textures, four entity textures, five item textures and one GUI texture are not native BlockLens palette behavior |
| LowOnFire | Existing independent first-person low-fire control | Source also changes three animated world-fire textures and five model texture selections; lowering first-person geometry does not implement these |
| Small Handhelds | Existing configurable first-person category scales | Source contains 44 model JSON files with item/state/hand/display-context exceptions. Its absolute model transforms are not equivalent to multiplying the current active transform by 40/40/50% |
| Ore Border | Native emissive edge overlays and bounded modded ore tags/API | Source has 32 CTM compact families, inner seams and emissive art; API/tag membership alone does not implement every modded family or OptiFine CTM texture behavior |
| Earlier Eater input in Issue #64 | Existing opt-in hide preset over 196 source filenames | `clear.png` is fully opaque dark gray, alpha 255 on all 256 texels. Its RGB values are approximately 35–49. The preset hides meshes and is not equivalent to this simplified opaque rendering. Eleven specialized state roots and three sparse plant textures also require separate decisions and visual proof |

The Eater finding corrects an earlier false transparent-texture description. Keep the existing hide
preset available; do not silently change configured filters. Any faithful simplified appearance
must be independently controlled and verified against actual rendered frames.

## Completion gate

Each missing behavior requires shared bounded policy, thin typed adapters, independent saved
controls, clean-room assets or procedural rendering, active external-resource priority and meaningful
OFF/ON/reload controls. Reserve removed capability bits 46 and 56 if new identities are appended.

Run all three native clients, the current-head common/coverage/mutation gates, reproducible artifact
and size checks, eight shader/backend rows and both exact BBE rows. Review the exact resulting
commit independently. Keep Issue #64 open until its required implementations and evidence exist.

The six-pack resource experiment is explicitly forced-load outside the declared formats for some
26.2/26.3 inputs. Resource-manager resolution alone does not establish item/entity/GUI rendering,
CTM compatibility, full-modpack coverage or an FPS improvement.

## Actual six-pack native result (2026-10-12 JST)

| Minecraft | Forward foreground / retained / permille | Reverse foreground / retained / permille | Changed cue pixels per order |
| --- | --- | --- | --- |
| 26.1.2 | 2438 / 2404 / 986 | 2374 / 2374 / 1000 | 605 |
| 26.2 | 2442 / 2406 / 985 | 2381 / 2381 / 1000 | 605 |
| 26.3 | 2441 / 2405 / 985 | 2380 / 2380 / 1000 | 605 |

Every row resolves exactly six pack inputs and 29 asset probes in each order, observes six
changed source winners, and restores the original selected packs and enabled mask. PNGs and
bounded, SHA-256-recorded manifests remain private test outputs; original archive bytes are not
published. The 26.3 forward ON screenshot was inspected. These focused tests do not replace full
normal/BBE/shader/backend current-head CI, which remains required before merging this refactor.

## Private material-hash boundary

Individual asset SHA-256 values remain local. The public lock contains only the 29 logical
resource names and their candidate pack aliases; the six whole-archive hashes were already public.
Use `--pack-input` with the private prepared directory when verifying `packs` results. The verifier
checks each archive's pinned bytes before deriving the expected material hashes, preserving exact
resource/priority validation. Collection removes the third material-hash column from resource TSVs;
raw native proof remains private and is not overwritten. A collected two-column resource report
cannot substitute for the raw private acceptance proof. Other shader/backend rows need no private
inputs and keep their existing verification contract.

Privacy follow-up: 81 Python tests pass, including private expected-byte derivation, tamper rejection and collector redaction. All three raw native results were reverified from the private pinned inputs; no material hashes remain in the public source or collected resource reports.
