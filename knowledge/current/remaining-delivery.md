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
