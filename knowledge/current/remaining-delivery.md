# Builder Assist, ore extensions and external compatibility delivery

## Scope and authority

The user requested implementation, review and merge of the remaining #36, #37 and
#31 work. The original delivery starts at main commit
`6c355e3831a6df4dded2bc46ac641911480da31d`, verified by
[main CI #340](https://github.com/bosatsu25/BlockLens/actions/runs/38065290177).
That baseline contains 53 controls and measured runtime JARs of
141298 / 141566 / 141628 bytes for 26.1.2 / 26.2 / 26.3.

On **2026-10-11**, the user explicitly replaced the 150 KiB requirement with
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
