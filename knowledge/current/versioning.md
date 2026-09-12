# BlockLens Versioning and Multi-Version Strategy

Status: **authoritative current version-support strategy**

## 1. Supported Minecraft versions

BlockLens initial supported lines are:

- Minecraft Java Edition **26.1.2**
- Minecraft Java Edition **26.2**

Both are first-class release targets. A feature is not considered complete if it only works on one supported line unless the current specification explicitly marks it version-specific.

Java baseline remains **Java 25** unless a supported Minecraft/Fabric toolchain requires a documented change.

## 2. Release model

Produce one runtime JAR per supported Minecraft line rather than one ambiguous universal JAR.

Example naming:

```text
blocklens-<mod-version>+mc26.1.2.jar
blocklens-<mod-version>+mc26.2.jar
```

The two artifacts should expose the same BlockLens product capability set and config schema unless Minecraft itself makes exact parity impossible. Any intentional divergence must be documented and contract-tested.

## 3. Gradle architecture

Use a small multi-project layout instead of cloning the whole mod twice.

Target shape:

```text
BlockLens/
├─ common/
│  └─ pure Java product logic
├─ versions/
│  ├─ mc26_1_2/
│  │  └─ Fabric/Minecraft 26.1.2 glue
│  └─ mc26_2/
│     └─ Fabric/Minecraft 26.2 glue
└─ test-support/
   └─ shared fixtures/oracles when justified
```

### `common`

Must contain as much reusable behavior as practical:

- 37-capability catalog
- config schema and migration policy
- functional-parity contracts
- state/semantic model independent of mapped Minecraft classes
- render descriptors
- composition/precedence rules
- performance budget policy
- artifact/version policy utilities where appropriate

`common` should avoid Fabric and mapped Minecraft API imports so it can be tested quickly and identically for both targets.

### `versions/mc26_1_2`

Own only 26.1.2-specific concerns such as:

- Minecraft/Fabric dependencies
- mapped block-state extraction
- render hooks/mixins/events
- registry adapters
- optional compatibility adapters
- version-specific resource metadata if required

### `versions/mc26_2`

Own the equivalent 26.2-specific boundary.

Do not copy product policy into this project merely because an API signature differs.

## 4. Adapter boundary

Preferred direction:

```text
Minecraft/Fabric API
       ↓
version adapter
       ↓
BlockLens common semantic model
       ↓
render descriptor / feature policy
       ↓
version render adapter
```

The adapter may translate mapped Minecraft state into stable BlockLens values such as:

- facing
- axis
- half
- shape
- connection mask
- powered/attached/open state

The common layer should not need to know whether a mapped class or method name changed between 26.1.2 and 26.2.

## 5. Version parity contract

CI must compare both supported targets and fail if one target accidentally loses:

- a BlockLens capability
- config key/default
- translation key
- client-only metadata
- required renderer semantic
- test fixture/oracle
- release artifact

Minecraft-native differences are permitted only when explicitly represented as a version exception with evidence.

## 6. 26.2 rendering consideration

Minecraft 26.2 introduced an experimental Vulkan rendering backend while retaining OpenGL fallback/default behavior in the released line. BlockLens must therefore avoid assuming that successful OpenGL rendering alone proves all 26.2 rendering paths safe.

Release verification for 26.2 should distinguish:

```text
26.2 + OpenGL
26.2 + Vulkan (supported/experimental verification track)
```

Do not claim Vulkan support until the relevant BlockLens rendering paths have been exercised successfully.

## 7. Resource-format differences

BlockLens should minimize dependence on Minecraft resource-pack internals because those formats can change between game versions.

When minimal runtime assets are required:

- keep common assets in one shared source where compatible
- isolate version-specific metadata/models only when required
- contract-test that both release artifacts contain only the intended assets

## 8. Adding future versions

A third Minecraft line should be added by:

1. creating a new version adapter project,
2. compiling the unchanged common contracts against it,
3. documenting actual incompatibilities,
4. adding the target to the CI/release matrix,
5. passing functional parity and performance gates.

Do not broaden supported-version ranges in `fabric.mod.json` without actually building and testing those versions.
