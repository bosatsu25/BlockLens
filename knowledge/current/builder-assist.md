# Builder Assist and Inspector

Status: **implemented with three-version native evidence** in
[PR #53](https://github.com/bosatsu25/BlockLens/pull/53). The exact tested commits,
quality results and artifact sizes are recorded in [remaining-delivery.md](remaining-delivery.md).
[Issue #36](https://github.com/bosatsu25/BlockLens/issues/36) records final merge completion.

## Source and accepted user interactions

Behavior is reviewed against ChiseTweaks `9fc977c5fdc0b23040d152216f60d46724cc65d8`:
[crosshair inspector](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/gui/CrosshairInspector.java),
[placement](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/gui/PlacementInspector.java),
[pattern comparison](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/gui/PatternConsistencyInspector.java), and
[inspector interaction](https://github.com/bosatsu25/ChiseTweaks/blob/9fc977c5fdc0b23040d152216f60d46724cc65d8/src/main/java/dev/chise/chisetweaks/gui/InspectorSettingsRows.java).
These are behavioral references; their source implementation, reflection framework and
artwork are not copied.

Four independent, default-OFF controls are appended to the existing Other category:

| Capability | Accepted behavior |
| --- | --- |
| Block Inspector | Show the existing crosshair hit's registered block ID and sorted state properties, or entity type ID. No new ray scan, entity name/UUID, NBT, inventory or server-only state is inspected. |
| Placement State Preview | Explain the current main-hand vanilla stateful block's predicted placement state and clicked face/upper-half input. This is a prediction from available client state, not a server acceptance guarantee. |
| Pattern Consistency | The user explicitly captures the current block as a reference, then compares nearby blocks with that same block ID. Show compared/matching/mismatching/unknown counts and per-property mismatch totals. Clear is explicit; reference state and position are session-only. |
| Schematic Comparison | Compare one locally available schematic state with the current predicted placement state through an optional, pinned typed Litematica adapter. Absence, unsupported versions or missing schematic data produce an unavailable result. Compatibility is claimed only after native evidence for the exact integration pins below. |

The native Builder Assist panel is opened explicitly from the in-world settings surface
or an initially unbound key. It does not pause the world. It consumes saved runtime
toggles only; opening it never installs or saves a settings draft. Enable tools and use
the main screen's Save and Apply before inspecting. Its reference/clear buttons affect
only session inspection state. It has bounded scrolling and supports the same compact
320-pixel viewport as the settings UI.

The accepted placement families are vanilla trapdoors, slabs, stairs, glazed terracotta,
fence gates, grindstones, beehives/nests, campfires, log/wood axes and froglight axes.
The adapter uses Minecraft's native placement-state calculation, including the held
stack's block-state component, after checking the target neighborhood is loaded. It
does not call item use, block placement/breaking, packet sending or inventory selection.
Vanilla registry ownership and supported implementation classes bound this prediction
path; arbitrary modded placement callbacks are not executed. Entity collision and
server-side policy are not inferred by this advisory state preview.

The optional read boundary compiles against these exact official releases. Both the
Minecraft version and the two installed mod versions must match the same row:

| Minecraft | Litematica | MaLiLib |
| --- | --- | --- |
| 26.1.2 | 0.27.14 | 0.28.12 |
| 26.2 | 0.28.8 | 0.29.6 |
| 26.3 | 0.29.1 | 0.30.2 |

The isolated adapter calls the public `SchematicWorldHandler.getSchematicWorld()` and
`WorldSchematic.getChunkSource().getChunkIfExists(...)` APIs. It reads one non-air state
only when both the ordinary client position and the schematic chunk are already
available. It does not create a schematic chunk or retain the schematic world. These
dependencies are compile-only and never packaged. The normal native suite must prove
the absent-mod path, and the separate `masa` profile must prove match, state difference,
block difference, missing chunk and unload cleanup with the pinned installed pair.

Intentional differences from ChiseTweaks: there is no interaction-history log, no
post-placement observation window, no automatic material replacement and no reflection
adapter. They are separate responsibilities and are not necessary for the accepted
read-only state preview. The native panel shows raw registered state tokens in its
English/Japanese explanatory UI, keeping exact technical state values visible.

## Work, memory and lifecycle contract

Inspector and preview read only the current hit and its bounded placement neighborhood.
The immutable read model accepts IDs up to 256 characters and at most 32 properties,
with each name/value at most 64 characters. Oversized or invalid state input is
unavailable rather than silently declaring a truncated comparison equal.
The native adapter retains at most 64 immutable decoded block states in an identity
cache. It clears this cache with inspection lifecycle state. Native placement checks
the at-most-four chunks intersecting a radius-three neighborhood before calling the
supported vanilla state calculation, and rejects coordinate arithmetic overflow.

Pattern work has fixed horizontal radius 8 and vertical radius 4: a 17 x 17 x 9 box of
2601 cursor positions around the explicitly selected reference. The reference itself
is excluded from comparisons. One tick spends at most 256 cursor/read operations,
including its reference validation. Missing chunks and out-of-height positions are
unknown, never air, and never trigger chunk creation. Only the reference block type's
state properties are decoded. A completed pass waits 20 ticks before its next pass.
The panel labels in-progress results; these are bounded observations over time, not
an atomic world snapshot or a whole-world consistency claim.

Changing the reference block state, unloading the reference, changing world identity,
disconnecting, resource reload, disabling Pattern Consistency or clearing the reference
discards the reference and partial/result counts. Target MISS and unloaded targets
clear their inspector/preview outputs. All four tools OFF means zero world queries and
no retained inspection state. No position, world identity or Minecraft object is saved
to configuration or diagnostic evidence.

## Verification before completion

Required evidence is common behavioral JUnit for state validation/comparison, unknown
data, exact domain/budgets, reference lifecycle, OFF behavior and deterministic counts;
selected coverage/PIT; three-version native stateful-block prediction and read-only
checks; rendered panel evidence and reference button interaction; combined existing
capabilities, reload and disconnect cleanup; reproducible runtime JAR audit and the
user-authorized 204800-byte release ceiling. Third-party Litematica compatibility is claimed
only for the exact versions independently exercised in native clients.
