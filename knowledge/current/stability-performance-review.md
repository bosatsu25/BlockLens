# Stability, Performance, and Responsive-UI Review

Status: **historical v0.2.1 implementation and measurement record**

This document preserves the v0.2.1-era UI, two-version measurements and 100 KiB release ceiling as historical evidence. Current category-based UI is specified in [settings-ui.md](settings-ui.md); current three-version support and the reviewed 140 KiB development / 150 KiB release ceilings are in [minecraft-26-3.md](minecraft-26-3.md) and [roadmap.md](roadmap.md). Its old layout and numeric budgets are not current requirements. Diagnostic improvements were completed in #43 / PR #45; the original historical failure cause remains unknown. See [m8-diagnostics.md](m8-diagnostics.md) and [lightweight-visuals.md](lightweight-visuals.md) for the additive development scope.

This review hardens the post-v0.2.0 source tree without adding a second rendering engine, a world scan, telemetry, network access, or an external UI dependency. The frozen 37-capability source contract and the current 40-capability product contract remain unchanged.

## Runtime changes

### Render hot path and retained memory

- `SemanticState` caches enum lookup tables instead of calling `Enum.values()` for each relevant quad.
- `DecorationQuadCuePolicy` returns bounded immutable instruction instances instead of allocating equivalent records repeatedly.
- each `MinecraftDecorationModel` wrapper retains one descriptor object array and one packed-bit array instead of four parallel descriptor arrays;
- lazy overlay publication writes the cached value before publishing the volatile attempted flag, so another thread cannot observe “attempted” before the result is visible.

The model-bake architecture remains the same: exact target classification happens during bake, the render path uses primitive masks, and the all-OFF path emits the active baked base directly.

### Configuration and failure handling

- config reads stop at **64 KiB + 1 byte** and reject oversized input before unbounded decoding;
- saves use a unique sibling temporary file, synchronous file writes, atomic replacement where the filesystem supports it, a safe replacement fallback otherwise, and best-effort temporary cleanup;
- startup falls back to defaults for both I/O and security failures;
- settings publication installs one immutable runtime snapshot and triggers one terrain invalidation only after a successful save;
- error messages do not include local filesystem paths or config content.

### Responsive settings surface

The native settings screen opens with **B** and exposes all 40 capability toggles. It uses a bounded registry of 40 toggle buttons plus one Done button. Layout behavior is deterministic:

- one column below 520 px, two columns at and above 520 px;
- content width capped at 620 px;
- compact layouts scroll without creating additional widgets;
- resize reflows and clamps scroll state;
- the real-client oracle exercises **320×240, 640×360, 854×480, and 1920×1080** on both Minecraft versions.

Draft changes are not applied to the running renderer until the config file is saved successfully. A failed save leaves the screen open with a localized error.

## Regression and diagnostic changes

- the M5 active-resource-pack oracle keeps its **950‰** preservation requirement and tolerates only a two-pixel raster-edge neighborhood when comparing the same base geometry; exact intersection and IoU remain recorded separately;
- failed Client GameTest jobs upload the client log and available M3/M5/M7/M8 evidence;
- M8 now measures OFF, default, and all-40 configurations and includes representative Decoration, Resource, Visibility, and Other targets;
- the 64-bit capability-mask capacity is an executable test invariant;
- source contracts require both version adapters to retain the same responsive surface and compact descriptor storage.

## Local verification evidence

| Gate | Minecraft 26.1.2 | Minecraft 26.2 |
| --- | ---: | ---: |
| real Client GameTest | PASS | PASS |
| responsive sizes | 4/4 PASS | 4/4 PASS |
| M5 active-pack retention | 1000‰ | 1000‰ |
| reload median | 2.967 s | 2.974 s |
| terrain rebuild median, OFF | 1.500 s | 1.504 s |
| terrain rebuild median, default | 1.503 s | 1.501 s |
| terrain rebuild median, all-40 | 1.502 s | 1.502 s |
| relevant allocation delta, all-40 minus OFF | −27,672 B | +199,512 B |
| wrapped models | 7,839 | 7,839 |
| retained capability slots | 7,849 | 7,849 |

Common quality evidence:

```text
JaCoCo line coverage for mutated classes: 466/481 (97%)
PIT generated/killed:                    239/231 (97%)
PIT test strength:                       97%
```

The observed allocation and timing values are coarse regression evidence, not an FPS claim. The important result is that default/all-40 operation remains within the existing guards and does not introduce a material rebuild or allocation jump relative to OFF.

## Linux pull-request verification

PR **#26** implementation commit `ee3a0b49ef044d9780e3a19fa06ffd617175978b` passed CI **#273 / 34820789320** on Linux before the release-version metadata bump.

- common JUnit + JaCoCo + PIT: PASS
- Minecraft 26.1.2 build, reproducibility, Client GameTest, visual evidence, performance evidence, and artifact upload: PASS
- Minecraft 26.2 build, reproducibility, Client GameTest, visual evidence, performance evidence, and artifact upload: PASS
- responsive settings marker: 320×240, 640×360, 854×480, 1920×1080; 41 bounded widgets
- all-ON wrapped models: 7,839

The raw Linux runtime artifacts measured:

```text
Minecraft 26.1.2: 101,854 B
SHA-256: 2ab6c469385d1f583de6c31ea01eb1bbf722367a67bbf2c9e2a2941ffe43cec8

Minecraft 26.2:   101,890 B
SHA-256: 4e56b40953596f7714005642405276c63d453d8bde3639627281aca7032c4699
```

These measurements verify the implementation itself. Because changing `mod_version` changes packaged metadata and can slightly change compressed bytes, the **v0.2.1** candidate is intentionally re-run through the same Linux gates before merge; the post-bump run is the final release-candidate authority.

## Artifact-size decision

Published v0.2.0 remains immutable historical evidence at **96,248 B** per supported Minecraft artifact, with the release-era cross-platform no-growth baseline of **96,257 B**.

The reviewed source adds a complete settings surface and durability/performance hardening. Local Gradle artifacts measured:

```text
Minecraft 26.1.2: 101,877 B
Minecraft 26.2:   101,913 B
new no-growth baseline: 101,913 B
hard release ceiling:   102,400 B (unchanged)
remaining local headroom:   487 B
```

Linux CI #273 measured 101,854 B / 101,890 B, leaving 546 B / 510 B respectively under the same hard ceiling. The baseline remains the larger reviewed local value, **101,913 B**, so the release ceiling is not relaxed.

This is an explicit rebaseline for reviewed behavior, not a relaxed ceiling. Further runtime growth must recover space or make a new architecture/size decision; tests, diagnostics, and the hard 100 KiB product budget must not be deleted or weakened opportunistically.

## Remaining validation before publication

- final v0.2.1 metadata candidate must pass the post-bump Linux PR CI;
- merge is allowed only after common + both Minecraft jobs are GREEN and both final JARs remain <= 102,400 B;
- after merge, main CI must pass and the Release workflow must promote the exact verified CI JAR bytes with `SHA256SUMS.txt`;
- repository branch/ruleset protection is a GitHub administration setting rather than a source change and must be enabled separately by an authorized repository administrator.
