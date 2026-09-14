# BlockLens Performance and Lightweight Strategy

Status: **authoritative current performance strategy**

## 1. Goal

BlockLens must be lightweight in three different senses:

1. **startup/load cost** — adding BlockLens should not materially slow client startup or initialization
2. **resource reload cost** — BlockLens must avoid recreating the source pack's thousands of tiny static resources and should keep reload work bounded
3. **runtime/render cost** — visual inspection features must avoid unbounded scans, repeated allocations, and unnecessary rebuilds

JAR byte size is a separate artifact budget and must not be confused with runtime performance.

## 2. Startup/load design rules

Prefer:

- one client entrypoint per release target
- static/compiled capability catalog rather than runtime classpath discovery
- no reflection-based feature scanning on startup
- no network/update check during startup
- no telemetry initialization
- no runtime parsing of the legacy RPO for normal operation
- lazy initialization of renderer-specific state until a feature/path actually needs it
- prevalidated config schema with bounded migration work

Avoid:

- walking large resource trees at startup
- scanning every registered block repeatedly to discover features when a deterministic mapping can be built once
- constructing shader/resource-pack compatibility state every frame
- eager generation of geometry for disabled capabilities

## 3. Resource reload rules

The migration exists partly to remove the cost and maintenance burden of thousands of resource-pack files.

During reload:

- load only minimal BlockLens-owned assets
- rebuild target lookup tables once when registry/resource state actually changes
- cache immutable lookup results with explicit invalidation
- do not regenerate unchanged static geometry per frame
- do not parse legacy RPO data unless an explicit import tool is later implemented

Measure reload duration before and after major renderer/asset changes.

## 4. Runtime rules

- no whole-world scans
- no unbounded loaded-chunk scan every tick
- prefer event/change-driven invalidation
- retain static geometry where useful and invalidate it explicitly
- keep render-hot-path allocations near zero
- use O(1) or bounded lookup for `block/state -> applicable capabilities` where practical
- precompute target/capability mappings rather than iterating over every current capability for each rendered block when evidence shows the lookup is hot
- bound every cache, queue, retained collection, and rebuild batch
- world/disconnect/resource-reload transitions must clear version/render state cleanly

## 5. Performance evidence model

Do not claim a speedup without measurement.

Track at minimum:

- process/client startup duration attributable to the tested scenario
- BlockLens initialization duration where measurable
- resource reload duration
- median frame time
- P95 frame time
- P99 frame time
- allocation rate in BlockLens update/render paths
- retained cache/geometry count and estimated memory
- runtime JAR bytes

Recommended scenarios for each supported Minecraft version:

1. vanilla/Fabric baseline without BlockLens
2. BlockLens installed, all capabilities OFF
3. representative orientation capability ON
4. representative outline/fine-line capability ON
5. resource highlights ON
6. all supported capabilities ON

## 6. No-regression gates

Absolute performance thresholds should be frozen only after a reproducible baseline exists.

Until then CI must enforce structural performance contracts, for example:

- no unbounded per-tick scan implementation
- no feature discovery through runtime reflection/classpath scanning
- no accidental startup networking
- no per-frame construction of immutable catalogs
- no unbounded collection growth
- no duplicate full-size replacement assets when shared/generated cues satisfy the visual contract

After M1 establishes repeatable evidence, record baseline values and define explicit no-regression tolerances in `knowledge/current/` rather than guessing numeric budgets.

## 7. Artifact-size budget

Source resource-pack baseline:

```text
2,366,865 bytes
```

Each supported BlockLens runtime JAR must independently satisfy:

```text
hard release maximum: < 1,183,433 bytes
stretch goal:         <= 716,800 bytes (700 KiB)
```

A passing 26.1.2 JAR does not excuse an oversized 26.2 JAR, or vice versa.

CI should report per-version:

- JAR bytes
- delta from that version's last frozen baseline
- largest compressed entries
- class/assets/metadata contribution

Once a verified BlockLens baseline exists, introduce a no-growth ceiling analogous to ChiseTweaks: current frozen baseline + explicitly permitted growth, always capped by the hard release maximum.

## 8. Optimization order

Use this order:

1. eliminate repeated JSON/model state expansion
2. eliminate duplicate/full replacement textures where runtime cues are equivalent
3. minimize eager startup/reload work
4. remove repeated runtime state interpretation with stable lookup/caching
5. losslessly optimize remaining image assets
6. compact reproducible JAR packaging
7. inspect class-level size only if still materially useful

Do not strip diagnostics, tests, source readability, or functional capability merely to hit a byte target.

## 9. Version-aware performance

Performance evidence must be recorded separately for 26.1.2 and 26.2 because rendering/resource internals can change between Minecraft releases.

For 26.2, OpenGL and Vulkan results must not be mixed into one number. Vulkan is a distinct experimental verification track until BlockLens explicitly declares it supported.
