# Optional Sodium terrain adapter

## Scope and version boundary

The adapter connects the existing bounded ore-extension models and terrain block
filter to Sodium's terrain mesher. It adds no setting, target inference, renderer
dependency, cache, fluid filtering or occlusion change. The original 57 capability
identities and defaults remain unchanged.

Only these exact loaded metadata pairs enable the adapter:

| Minecraft | Sodium version |
| --- | --- |
| 26.1.2 | `0.9.2+mc26.1.2` |
| 26.2 | `0.9.2+mc26.2` |
| 26.3 | `0.9.2+mc26.3` |

An absent Sodium mod, unknown version, mismatched Minecraft suffix or unsupported
Minecraft target skips this optional adapter. Standard Fabric hooks remain
independent. No runtime classpath scan, reflective method discovery or download is
used. The mixin plugin reads only the two loaded mod versions during mixin
selection; it does not initialize the client runtime or inspect a world.

Exact tested Iris, Sodium and shader artifact hashes are pinned in
[the external dependency lock](../../scripts/external_compatibility_dependencies.json).
The [external matrix](external-compatibility.md) and [Issue #31](https://github.com/bosatsu25/BlockLens/issues/31)
record current immutable native results and their actual graphics configuration.
This boundary does not establish other Sodium versions, all 57 capabilities under
shaders, third-party GPU drivers or uploaded private-pack combinations.

## Evidence for the integration point

The three pinned Sodium JARs bypass vanilla `SectionCompiler.compile` when their
`ChunkBuilderMeshingTask` obtains a model and calls Sodium's
`BlockRenderer.renderModel(BlockStateModel, BlockState, BlockPos, BlockPos)`.
The published JARs contain byte-identical `BlockRenderer` and `BlockRenderContext`
classes at this boundary. Its model emitter invokes the supplied model's
`emitQuads`; model substitution therefore reuses BlockLens's existing Fabric
model wrapper without copying Sodium's mutable quad API.

One shared string-target `@Pseudo` mixin uses only Minecraft argument types. Before
Sodium emits terrain, it cancels the model call when the published block-filter
generation hides that block. Otherwise it selects
`MinecraftOreExtensions.model(original, state)`. That lookup retains the original
active-base identity guard and returns the existing bounded wrapper only for an
exact supported extension state. No tags or registries are enumerated during
meshing, and no wrapper is allocated there. Hidden geometry never reaches the
model emitter, so filtering also suppresses an enabled ore highlight.

The pre-existing invalidation adapters already reach Sodium. On 26.1.2,
`LevelRenderer.allChanged` reaches Sodium's renderer reload. On 26.2 and 26.3,
`LevelExtractor.allChanged` marks the existing geometry invalidation which reaches
Sodium reload during extraction. Configuration changes, synchronized tag changes,
resource reload and disconnect therefore use the same publication and cleanup
contract as the standard renderer. No additional reload listener is installed.

The existing scene-filter boundary still preserves fluids, world state, collision,
lighting and solid occlusion. The adapter only suppresses terrain model emission;
it does not reveal faces that the physical world has already occluded. Entity and
block-entity extraction remain separate integration paths.

## Native regression contract

All six pinned Shader OFF/ON rows run a separate isolated close-up fixture after
their original M3/M5 checks. The original M3/M5 scene guards, images, full ROI,
24-unit RGB distance, two-pixel tolerance and 950/1000 retention minimum are
unchanged.

The Sodium fixture records actual explicit registration, synchronized tag add and
remove, active non-cube base preservation, unsupported-registration isolation,
tagged-ore filtering, vanilla stone filtering, OFF restoration and resource
reload. Fifteen bounded 640 by 360 frames include AIR, unsupported and restoration
controls. Image differences use the declared 24-unit RGB distance and 80-pixel
minimum; active-base retention must reach 950/1000.

A test-only observer at the actual Sodium model-emitter call additionally compares
the emitted model with the precomputed retained model and records whether a hidden
target reaches emission. A paired observation at the mesher's call site records
completion after the matching target's model-rendering call returns, including a
filtered call. Every non-AIR stage requires this actual completion; positive visible and
restored stages also require model emission. This prevents unrelated temporal
shader changes from proving a missing model adapter, and prevents an unfinished
rebuild or unloaded fixture from proving filtering. Counter snapshots are atomic
and saturation is rejected. The observer and its bounded state are excluded from
runtime JARs.

The regression harness can be committed before the runtime adapter: it records
both missing-path outcomes, preserves its bounded manifest and images, then fails.
A later passing run does not erase that original failure. Strict artifact
verification requires the named native checks and all original files/hashes.

Normal three-version builds, JUnit, selected JaCoCo/PIT, native regressions, M8,
reproducible packaging and both **204800-byte** ceilings remain required. Exact PR
and resulting main evidence is recorded in the delivery issues. Published v0.2.2
is unchanged by this development adapter.
