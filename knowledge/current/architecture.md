# BlockLens Current Architecture

Status: **authoritative current architecture direction**

## 1. Architecture objective

Replace repeated static resource-pack state definitions with a small number of reusable runtime concepts while preserving independently controllable user-facing capabilities.

The architecture must optimize for:

- functional parity,
- predictable rendering behavior,
- active resource-pack compatibility,
- low runtime overhead,
- minimal static asset duplication,
- and testable state interpretation.

## 2. High-level model

```text
Minecraft / active resource pack
             |
             v
      Block / BlockState
             |
             v
      State Interpreter
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
       Shared Renderers
   +---------+----------+
   |         |          |
 Overlay   Outline   State marker
   |         |          |
   +---------+----------+
             v
         Minecraft
```

## 3. Suggested module boundaries

Names are conceptual until implementation begins.

### `core/state`

Pure state interpretation.

Responsibilities:

- orientation/facing
- slab half/type
- stair facing/half/shape
- trapdoor open/half/facing
- fence gate state
- axis for log/wood-like blocks
- connection booleans
- powered/attached-like state where relevant

Rules:

- no rendering API calls,
- deterministic input/output,
- strongly unit-testable.

### `core/catalog`

Defines the 37 capability contracts.

Each entry should carry only stable product information such as:

- capability ID,
- source RPO key,
- category,
- default/preset mapping,
- target matcher or matcher family,
- render semantic type.

The catalog is the primary functional-parity contract.

### `config`

Responsibilities:

- persistent enable/disable state,
- style/accessibility options when added,
- migration of older BlockLens configs,
- current preset import if an RPO-import feature is later justified.

The first release does **not** need runtime RPO parsing if the native config already represents all capabilities cleanly.

### `render`

Shared rendering infrastructure.

Candidate renderer families:

1. **Orientation/state marker renderer**
   - arrows, axis marks, top/bottom markers, shape hints

2. **Highlight overlay renderer**
   - ore/resource overlays
   - color/pattern cues

3. **Outline renderer**
   - powder snow, blue ice, dead coral, sculk catalyst and similar visibility aids

4. **Fine-line renderer**
   - string/tripwire-like thin geometry visibility

Do not create one renderer class per source RPO toggle unless the behavior is actually unique.

### `compat`

Optional compatibility boundaries.

Initial concerns:

- Iris/shader enabled state where necessary
- third-party resource-pack interaction
- Fabric/Minecraft-version abstraction if future version support is added

All optional compatibility must fail soft.

### `ui`

The UI should be organized around user intent, not source-file organization.

Current product grouping may evolve, but must preserve independent control of all 37 capabilities.

Avoid exposing implementation jargon such as model JSON count, RPO rule filenames, or renderer class names.

## 4. Static assets policy

Use static assets only when they carry information that cannot be represented reliably and efficiently by shared runtime rendering.

Prefer:

```text
many state-specific full textures
        ->
one reusable marker/overlay asset + code-driven placement
```

Prefer:

```text
full replacement ore texture
        ->
current ore texture + BlockLens overlay
```

Benefits:

- smaller JAR,
- better compatibility with active packs,
- less duplication,
- fewer version-specific resource files.

## 5. Rendering-state ownership

BlockLens should own only the additional visual information it creates.

It should not silently take ownership of unrelated vanilla/resource-pack rendering state.

Examples:

- If a feature is OFF, BlockLens must not alter that feature's target rendering.
- Disabling BlockLens should restore normal rendering without requiring a resource reload unless technically unavoidable.
- World/disconnect/resource-reload transitions must clear retained BlockLens state safely.

## 6. Simultaneous feature behavior

All capabilities are assumed compatible unless explicitly documented otherwise.

When multiple features affect one target, define deterministic composition rather than arbitrary priority.

Candidate composition rule:

```text
base texture/model
  -> orientation/state cue
  -> resource/highlight cue
  -> outline/fine-line cue
```

If two features genuinely conflict visually, document and test the chosen precedence instead of making them mutually exclusive by default.

## 7. Shader strategy

Do not claim shader compatibility merely because vanilla rendering works.

Architecture should permit:

```text
Render semantic
      |
      +-> Standard path
      |
      +-> Shader-compatible path (only if required by evidence)
```

Avoid one implementation per shader pack. Prefer one compatibility abstraction with targeted exceptions only when measured/verified.

## 8. Performance constraints

- No whole-world scan.
- No unbounded loaded-chunk scan each tick.
- No per-frame rebuilding of static geometry without state change.
- Cache only what has a clear invalidation path.
- Keep hot-path allocations near zero where practical.
- Prefer change-driven rebuilds.
- Bound all queues and retained collections.

## 9. Artifact-size strategy

Largest expected savings come from removing duplication, not from stripping debug information or reducing code readability.

Priority:

1. collapse repeated JSON/state definitions into code,
2. eliminate duplicate textures/models where shared runtime markers are equivalent,
3. losslessly optimize remaining PNGs,
4. minimize generated metadata/resources,
5. use normal JAR compression efficiently,
6. only then inspect class-level size if still necessary.

Do not use artifact-size pressure as justification for unsafe obfuscation, loss of diagnostics, or feature deletion.
