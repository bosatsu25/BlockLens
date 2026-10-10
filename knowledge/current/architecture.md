# BlockLens Current Architecture

Status: **authoritative current architecture direction**

## 1. Architecture objective

Replace repeated static resource-pack state definitions with a small number of reusable runtime concepts while preserving independently controllable user-facing capabilities across **Minecraft 26.1.2, 26.2 and 26.3**.

The architecture must optimize for:

- functional parity,
- predictable rendering behavior,
- active resource-pack compatibility,
- low startup/reload/runtime overhead,
- minimal static asset duplication,
- testable state interpretation,
- and thin Minecraft-version adapters.

## 2. High-level model

```text
Minecraft / active resource pack
             |
             v
     version-specific adapter
             |
             v
      Block / BlockState semantic input
             |
             v
      State Interpreter (common)
             |
   +---------+----------+
   |                    |
   v                    v
Visual semantics     Feature policy
(direction, shape,   (enabled, style,
 half, connection)    intensity, etc.)
   |                    |
   +---------+----------+
             v
       Render Descriptor
             |
             v
     version render adapter
             |
             v
       Shared Renderers
   +---------+----------+
   |         |          |
 Overlay   Outline   State marker
   |         |          |
   +---------+----------+
             v
         Minecraft
```

## 3. Multi-project boundary

Use:

```text
common/
versions/mc26_1_2/
versions/mc26_2/
test-support/   (only if justified)
```

### `common`

Pure/testable product logic shared by all three versions:

- 37-capability catalog
- config schema/migration policy
- functional-parity contracts
- stable state semantic model
- render descriptors
- composition/precedence rules
- performance budget policy

Avoid Fabric and mapped Minecraft imports here.

### `versions/mc26_1_2`

Only 26.1.2-specific Fabric/Minecraft glue:

- state extraction
- mixins/events/hooks
- registry adapters
- render API bridge
- optional compatibility integration
- metadata/resources required only by this version

### `versions/mc26_2`

Equivalent boundary for 26.2. Do not duplicate product policy merely because mapped APIs differ.

The mc26_3 module reuses the modern mc26_2 adapters and test bridge, with independent dependency/metadata pins and a tiny version-specific MinecraftQuadLighting implementation. Product policy and the complete renderer are shared. See [26.3 integration](minecraft-26-3.md).

## 4. Core module boundaries

### `core/state`

Pure state interpretation:

- orientation/facing
- slab half/type
- stair facing/half/shape
- trapdoor open/half/facing
- fence gate state
- axis for log/wood-like blocks
- connection booleans
- powered/attached-like state where relevant

No rendering API calls.

### `core/catalog`

Defines all 37 capability contracts:

- capability ID
- source RPO key
- category
- default/preset mapping
- target matcher family
- render semantic type

No runtime reflection/classpath discovery. The catalog is compiled/static and contract-tested.

### `config`

- persistent enable/disable state
- shared schema across all three Minecraft versions
- config migration
- corruption-safe fallback

Normal startup does not parse the legacy RPO.

### `render`

Shared semantic renderer families:

1. orientation/state marker
2. highlight overlay
3. outline
4. fine-line

Do not create one renderer per source toggle without a real behavior difference.

### `compat`

Optional, fail-soft boundaries:

- Iris/shader state where required
- active resource-pack interaction
- 26.2 OpenGL/Vulkan backend-sensitive behavior where evidence requires it

### `ui`

Organize around user intent, preserve independent control of all 37 capabilities, and keep the same user-facing contract across supported Minecraft versions.

## 5. Startup/load ownership

BlockLens startup should be deliberately small:

- one client entrypoint per version artifact
- static capability catalog
- no startup network/update checks
- no telemetry
- no classpath/reflection feature scan
- no eager geometry generation for disabled features
- lazy renderer/compatibility initialization when safe

Any expensive initialization must be measurable and justified.

## 6. Static assets policy

Use static assets only when they carry information that shared runtime rendering cannot represent reliably and efficiently.

Prefer:

```text
many state-specific full textures
        ->
one reusable marker/overlay asset + code-driven placement
```

and:

```text
full replacement ore texture
        ->
current ore texture + BlockLens overlay
```

Keep common assets shared when the format is compatible; isolate version-specific assets only when Minecraft requires it.

## 7. Rendering-state ownership

BlockLens owns only the extra visual information it creates.

- feature OFF must restore normal rendering
- world/disconnect/resource-reload transitions must clear retained state safely
- compatibility failure disables/isolate only the affected path when possible

## 8. Simultaneous feature behavior

All capabilities are assumed compatible unless explicitly documented otherwise.

Composition candidate:

```text
base texture/model
  -> orientation/state cue
  -> resource/highlight cue
  -> outline/fine-line cue
```

Conflicts require deterministic precedence and regression tests, not default mutual exclusion.

## 9. Shader/backend strategy

Do not claim compatibility merely because vanilla rendering works.

```text
Render semantic
      |
      +-> standard OpenGL path
      |
      +-> shader-compatible path (only if required)
      |
      +-> 26.2 Vulkan verification/adaptation path (only if required)
```

Avoid one implementation per shader pack or backend unless evidence forces it.

## 10. Performance constraints

- no whole-world scan
- no unbounded loaded-chunk scan each tick
- no per-frame rebuild of unchanged static geometry
- explicit invalidation for caches
- render-hot-path allocations near zero where practical
- bounded queues/collections
- O(1) or bounded target-to-capability lookup where practical
- no runtime reflection discovery
- no startup networking

Detailed budgets are in `performance-strategy.md`.

## 11. Artifact-size strategy

Priority:

1. collapse repeated JSON/state definitions into code
2. eliminate duplicate textures/models where equivalent
3. minimize startup/reload metadata work
4. losslessly optimize remaining PNGs
5. use reproducible JAR compression
6. inspect class-level size only if materially useful

Never trade away diagnostics, correctness, or capability parity for byte count.
