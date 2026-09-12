# BlockLens M1 Verified Baseline

Status: **authoritative M1 baseline**

Captured from GitHub Actions run **34683630501** on commit `9d814080536d59c3f7b6dc6603b7576f7e1f70f0`.

## Toolchain

- Java: **25**
- Gradle: **9.5.1**
- Fabric Loom: **1.17.19**
- Fabric Loader: **0.19.3**
- Minecraft targets: **26.1.2**, **26.2**

## Automated quality result

All jobs passed:

- common JUnit 5: PASS
- selected JaCoCo gate: PASS
- selected PIT gate: PASS
- repository/version parity contracts: PASS
- 26.1.2 build: PASS
- 26.2 build: PASS
- runtime artifact structure/privacy/residue audit: PASS
- deterministic clean rebuild SHA-256 comparison: PASS on both versions
- Fabric Client GameTest: PASS on both versions
- singleplayer world join/leave smoke: PASS on both versions
- native config create/read/round-trip smoke: PASS on both versions

The startup Client GameTest also exercised Minecraft's normal initial `ResourceManager` reload path on both supported versions.

## Runtime artifact baseline

| Minecraft | Runtime JAR | SHA-256 | Required gate | Stretch gate |
| --- | ---: | --- | ---: | ---: |
| 26.1.2 | **14,481 bytes** | `fcf6d66b961e1cba3dd1b7846fe354bca7a2e03d638ff791d56f2964a86e8f93` | < 1,183,433 B | <= 716,800 B |
| 26.2 | **14,476 bytes** | `07c2bcb885e70d9fef4c2418848d4fd4d7bd07cfe92dac9bde387c486887a4cd` | < 1,183,433 B | <= 716,800 B |

These are M1 scaffold artifacts before feature-rendering expansion. Future milestones must treat them as a no-surprise reference, not as a promise that the final artifact remains exactly this small.

## BlockLens initialization baseline

Measured inside the client entrypoint from immediately before native config load through publication of common runtime state:

| Minecraft | BlockLens initialization |
| --- | ---: |
| 26.1.2 | **7,512,771 ns** (~7.513 ms) |
| 26.2 | **2,683,997 ns** (~2.684 ms) |

This is **BlockLens-only initialization**, not total Minecraft startup time.

The values were captured on GitHub-hosted Ubuntu runners under CI virtualization and must not be presented as representative gaming-hardware frame/load performance. They are regression baselines for later BlockLens changes.

## Client GameTest scope

The shared GameTest source is compiled and executed against both supported Minecraft lines. It verifies:

1. the compiled catalog still contains exactly 37 capabilities;
2. the running Minecraft version matches the version-specific BlockLens runtime adapter;
3. `blocklens.properties` is created through the native config path;
4. the persisted config decodes to the same values published by the runtime;
5. the runtime remains valid after a singleplayer world is created, ticked, and closed.

Rendering parity is intentionally not claimed by M1. Visual/state parity begins with M2/M3 and is added to this test path as renderer functionality lands.

## CI-environment noise observed

The headless Linux Client GameTests can log missing narrator/audio devices, Realms/profile authorization failures for the synthetic test identity, and X11 cursor warnings. These did not fail either GameTest and are not BlockLens runtime failures.

## M1 exit decision

**M1 PASS.**

The project now has a verified dual-version Java 25 foundation with a shared 37-capability contract, native config persistence, deterministic quality gates, reproducible artifacts, runtime privacy/residue checks, size budgets, and actual client/world lifecycle smoke coverage.
