# Builder Assist, ore extensions and external compatibility delivery

## Scope and authority

The user requested implementation, review and merge of the remaining #36, #37 and
#31 work. The original delivery starts at main commit
`6c355e3831a6df4dded2bc46ac641911480da31d`, verified by
[main CI #340](https://github.com/bosatsu25/BlockLens/actions/runs/38065290177).
That baseline contains 53 controls and measured runtime JARs of
141298 / 141566 / 141628 bytes for 26.1.2 / 26.2 / 26.3.

On **2026-10-10 UTC (2026-10-11 JST)**, the user explicitly replaced the 150 KiB requirement with
**200 KiB (204800 bytes)** and requested completion through merge. Both development
and release size gates now enforce that accepted ceiling. The source-pack absolute
ceiling remains 1183432 bytes. Historical 140/150 KiB measurements remain intact;
this change is an explicit product decision, not a performance improvement.

No additional compression library, modified class format, removed feature or stripped
debug information is introduced. The existing audited owned-runtime container and
ordinary Gradle ZIP compression remain the delivery format. Each target still reports
its actual artifact size, SHA-256, clean-rebuild reproducibility and source-class equality.

## Implemented contracts

- [Builder Assist](builder-assist.md): four independent default-OFF controls, client-local
  inspector, readonly native placement prediction, fixed-domain pattern comparison,
  exact optional Litematica/MaLiLib pairs and a bilingual session panel.
- [Ore extensions](extension-compatibility.md): versioned bounded declarations,
  synchronized conventional tags, deterministic precedence/isolation and retained
  active-base terrain models composed with existing Resource controls and Scene Filter.
- [External compatibility](external-compatibility.md): byte-pinned Shader ON/OFF,
  Masa and Vulkan profiles, actual backend/shader checks and strict result collection.

The compiled catalog is 57 controls, preserving the prior 53 identities and defaults
and the frozen original 37-capability source contract. No custom server protocol,
automatic placement/breaking or forced Masa dependency is added.

## Verification and completion boundary

The normal workflow runs the full common JUnit, selected JaCoCo/PIT, reproducibility,
packaged-runtime audit and all-three-version client graph. It adds four Builder panel
screenshots per target and the explicit/tagged ore rendering/lifecycle evidence.
M8 still uses its reviewed test-only C2 foreground condition, requested 60 FPS,
32 MiB allocation threshold, 30-tick windows and three measurements. The remaining
delivery does not reopen or reinterpret its historical root-cause uncertainty.

The external workflow has eleven isolated rows: Shader ON, same-stack Shader OFF and
Masa for each supported Minecraft release, plus Vulkan for 26.2 and 26.3. These are
focused compatibility observations; they do not replace the full normal graph or prove
M8 performance. A requested backend or installed shader is insufficient evidence.

Original uploaded resource packs have been recovered and hash-checked. Their private
native execution remains blocked by the current environment's unavailable X11 sockets;
the public repository and public artifacts do not receive their archive bytes. The
prepared all-three-version forward/reverse-order harness is documented in the external
specification. Issue #31 remains open until that missing real-client evidence exists.

Final completion requires the exact PR head's normal and advertised external CI results,
independent review, merge at that head, and verification of the resulting main workflow.
Issue/PR delivery records carry those exact immutable commit and CI links. The published
release remains v0.2.2 until a separate release decision changes its version.

## Initial hosted verification and corrections

[PR #53](https://github.com/bosatsu25/BlockLens/pull/53) first ran at
`d9d0c8a208ee94e27ebf0e2e9d9e1adc783fde04`.
[CI #341](https://github.com/bosatsu25/BlockLens/actions/runs/38069083325) passed
the common JUnit/JaCoCo/PIT gate (414/429 killed mutations, 97%) and all three
build, runtime audit and clean-rebuild reproducibility steps. Initial runtime sizes
were 168534 / 168863 / 168930 bytes in target order. The real-client graph stopped
in all three versions at the responsive settings oracle's obsolete action-button
count. The oracle now includes the four Builder actions and retains its actual
keyboard reachability and viewport-bound assertions. The all-capabilities Scene
Filter fixture and its evidence names also move from 53 to the current 57 controls;
the original capability identities remain unchanged.

[External CI #1](https://github.com/bosatsu25/BlockLens/actions/runs/38069083342)
passed all three actual Masa integrations and both supported Vulkan rows. Its six
Shader rows failed before visual testing because prepared files were copied to
JavaExec's preliminary working directory, before Loom assigned the client run
directory in its task action. Preparation now targets the configured Loom run
directory directly. Actual shader activation, dependency hashes and rendered visual
assertions remain required. These initial failures are retained as failed evidence;
they are not passing completion results for the full delivery.

At follow-up head `835de37d07a3464fe2b227e7bd2afe865accf356`,
[CI #342](https://github.com/bosatsu25/BlockLens/actions/runs/38069788885) again
passed common quality and every build/artifact step. All three native clients then
completed Builder Assist and all fourteen ore-extension images with their required
behavioral success markers. The new oracles took 64–66 seconds after the Scene
fixture. Ore verification finished only about 14–16 seconds before the eight-minute step
deadline, and Actions cancelled all three jobs while the following M8 phase was
reloading resources. No preceding native assertion or M8 measurement failure was
reported, and no M8 completion was recorded. These runs remain failures.

The previous main CI #340 full-client steps already took 404 / 434 / 402 seconds.
The expanded graph now receives a **twelve-minute test-step execution window**
within the existing twenty-minute job. This change accommodates the added native
coverage; it changes none of the M8 scene, warmup, sample, allocation, timing, FPS
or compilation requirements. Current-head completion still requires that entire
graph and its evidence collectors to pass.

[External CI #2](https://github.com/bosatsu25/BlockLens/actions/runs/38069788830)
passed nine rows, including Shader ON/OFF on 26.2 and 26.3. The remaining 26.1.2
Shader-OFF row failed when Fabric's extra screenshot render called Sodium after
its per-frame terrain uniforms had been released. The 26.1.2 Shader-ON row reached
M5 but measured 862/1000 base retention against the unchanged 950/1000 minimum.
Its first failed run did not archive the rendered M5 frames, so bounded diagnostic
frame collection is required to investigate that failure without accepting it or
weakening the visual assertion.

At head `71f6d9a5cbe1f9b9bc35ca9c3415dbf54840e05f`,
[External CI #3](https://github.com/bosatsu25/BlockLens/actions/runs/38070878498)
passed ten of eleven rows, including the repaired 26.1.2 Shader-OFF capture.
The 26.1.2 Shader-ON row again failed M5, this time at 780/1000 retention.
Its failure artifact `11677081534` preserves the actual OFF/ON/air frames,
their hashes and explicitly failed diagnostic status. Of 487 OFF-mask pixels
without a matching ON neighbor, 445 (91.4%) are in the unchanged grass below
the quartz board, at image rows 290–314. The metric was counting changing
background samples as removed target geometry. This diagnosis does not turn
either failed run into a pass: the external fixture needs a static background
covering the original measurement region, followed by the same native gate.

The same head's [normal CI #343](https://github.com/bosatsu25/BlockLens/actions/runs/38070878467)
passed all four jobs. The three native steps completed in **514 / 473 / 470 seconds**,
including M8 and the final Builder/ore disconnect cleanup. The 26.1.2 step exceeded
the old 480-second execution cap while passing its actual assertions, validating
the expanded test window. Each version produced four bilingual Builder screenshots
and fourteen ore-extension images plus success manifests. Common JUnit, selected
JaCoCo/PIT, packaged-runtime audits and clean-rebuild reproducibility also passed.

| Minecraft | Runtime JAR | Growth from starting main | Headroom below 204800 B |
| --- | ---: | ---: | ---: |
| 26.1.2 | 168534 B | +27236 B | 36266 B |
| 26.2 | 168863 B | +27297 B | 35937 B |
| 26.3 | 168930 B | +27302 B | 35870 B |

This is complete normal-graph evidence for `71f6d9a5cbe1f9b9bc35ca9c3415dbf54840e05f`.
The subsequent external-fixture correction still requires its own current-head CI;
the successful normal run alone does not complete the external matrix or merge.

The guarded external-floor attempt at `c60f57de5914304a6ec472f32a115e56825ad051`
([External CI #4](https://github.com/bosatsu25/BlockLens/actions/runs/38071918467))
passed Masa on all three targets but failed the eight rendering rows before M5
capture. The added guard assumed X=0/Z=13 for the pre-existing teleport command.
All three official client parsers actually center those absolute integer tokens
at X=0.5/Z=13.5. The correction changes the test guard and its projection origin,
with a regression against the actual parsed coordinates; it preserves the
teleport command, native framing, complete image region and retention gate.
The original assertion failures remain recorded, without claiming a native pass
for that attempted background revision.

At `05e782e4c399e5ae569ef542c7fc7737297b5a74`,
[normal CI #345](https://github.com/bosatsu25/BlockLens/actions/runs/38072553708)
passed all four jobs. Its three downloaded runtime JARs match the sizes and
SHA-256 hashes from #343 exactly. Current M8 manifests and raw samples pass every
guard, and all three native logs reach the final Builder, ore, analyzer and Scene
Filter disconnect assertions. Native steps took 515 / 487 / 482 seconds.

The paired [external run #5](https://github.com/bosatsu25/BlockLens/actions/runs/38072553687)
passed ten rows. All ten successful artifacts were independently downloaded,
digest-checked and validated. The corrected 26.1.2 Shader-ON native images retain
1309 of 1313 base pixels, or **996/1000**, above the unchanged 950 minimum. The
previous grass contamination is absent; all three models and original framing
remain present. Shader-ON retention on 26.2 / 26.3 is 995 / 992; Shader-OFF and
Vulkan retention is 1000 on their supported rows.

The remaining 26.2 Masa row timed out after twelve minutes. Its typed schematic
assertions, including missing-chunk isolation, unload cleanup and readonly
behavior, reached the success marker at 17:40:50 UTC. No later native log appeared
before the timeout at 17:52:26. The original log and hash-verified failure artifact
`11677452307` retain this failed attempt. Earlier identical Masa inputs passed,
but that does not establish the cause of this stall or turn it into a pass.
The available evidence places the stop after schematic assertions and before
the observed disconnect path; it cannot distinguish client restoration from
world-close waiting. Test-only lifecycle diagnostics are required to investigate
this uncertainty without changing the timeout, product code or acceptance
assertions. Exact subsequent outcomes remain in PR #53 and the delivery issues.

## Merge and the external scene-isolation follow-up

The final PR #53 head `2120ccdb006b262eccb96fd888d7f07db5b413c3` passed
[normal CI #346](https://github.com/bosatsu25/BlockLens/actions/runs/38074634731)
and all eleven [external #6 jobs](https://github.com/bosatsu25/BlockLens/actions/runs/38074634660).
All current M8/JAR artifacts and the eleven external artifacts were independently
downloaded and hash-verified. Normal native durations were 512 / 494 / 470 seconds;
runtime bytes remained 168534 / 168863 / 168930. All three Masa profiles completed
restoration, evidence copy and world closure. The earlier Masa stall did not reproduce,
and its precise cause remains unproven.

PR #53 was squash-merged to signed main commit
`9609c4dd70fb162ba8479d6e606baab2fe8eadad`, with exactly the accepted tree
`137bedc042e470c35ab857e4ccea4a800171a92e`. The actual main
[external #7 run](https://github.com/bosatsu25/BlockLens/actions/runs/38075582520)
passed ten rows but failed 26.2 Shader ON at **939/1000** retention
(1486/1581, required 950). Its original artifact `11677459914` has SHA-256
`d21062dcb0f42965f35c21a5c9eb7655903c804dd94726c045817a8c402abbca`;
its ten internal hashes were verified and its incomplete manifest was correctly rejected.
Successful 26.1.2/26.3 Shader-ON retention was 967/979, and all Masa lifecycles completed.

The remaining failure exposed a separate fixture-isolation defect: M5 inherited
M3's thirteen blocks, including front-row occluders. A source-derived diagnostic
projection places **94/95** unmatched pixels outside the three visible target probes
with a two-pixel halo; the actual native projection matrix was not recorded.
The probe-only 997/1000 result and controlled-image 979 positive with
940/938/860 individual-target-removal negatives are causal analysis only;
the original global 939 result remains a failed native run.

The [external specification](external-compatibility.md#isolating-m5-from-the-preceding-m3-scene)
now requires an external-only 52-cell clear and a native check for exactly the three
original targets plus 49 AIR cells before capture. Strict artifact verification
requires the resulting isolation flag. Real temporary-artifact regressions first
reproduced acceptance of missing/false isolation flags, then passed after the verifier
change. All **55** local Python tests and all three exact Minecraft API compilations pass.
The runtime code, normal/M8 scene, capture path, shader settings, metric and 200 KiB
ceilings are unchanged. Fresh follow-up PR and resulting main gates remain required;
their immutable workflow links and final Issue states record completion.
