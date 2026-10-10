# External rendering compatibility

Status: **verification in progress** for [Issue #31](https://github.com/bosatsu25/BlockLens/issues/31).
The harness and pinned matrix below define the experiments. They are not pass evidence until the
corresponding current-head real-client job and its archived results succeed.

## Scope and completion boundary

Keep the normal packaged-runtime three-version CI and M8 graph unchanged. External tests use
separate client processes, dependency directories and recorded profiles. A focused external result
has `fullGraph=false`; it establishes only its named rendering combination. Never infer shader-ON
from an installed shader mod, Vulkan from a requested setting, or uploaded-pack compatibility from
the deterministic M5 fixture.

The representative matrix was fixed before adding compatibility adapters. Its first probes cover
the 13 original non-Resource decoration capabilities and the active-model preservation mechanism
for three Resource Highlights. The reference-pack profile additionally checks actual resource
priority and rendered base-model retention with the supplied archives. This does not claim that
every shader feature, connected-texture rule, modded block or graphics driver has been tested.

## Pinned shader matrix

Primary release metadata was retrieved on 2026-10-10 UTC from the author's Modrinth projects.
The complete download URLs, lengths, SHA-512 and SHA-256 values are checked into
[`external_compatibility_dependencies.json`](../../scripts/external_compatibility_dependencies.json).
The setup verifies both hashes before loading an artifact; it never selects a floating latest version.

| Minecraft | Iris / release ID | Sodium / release ID | Shader | Profiles |
| --- | --- | --- | --- | --- |
| 26.1.2 | 1.11.4 / `sZbVsl2Q` | 0.9.2 / `tZQ3jqnf` | Complementary Reimagined r5.9.3 / `Bqen1mJX` | OpenGL shader-OFF and shader-ON LOW |
| 26.2 | 1.11.4 / `gxZWWnKH` | 0.9.2 / `xJZxADzI` | Same exact shader ZIP | OpenGL shader-OFF and shader-ON LOW |
| 26.3 | 1.11.7 / `vTN4NRGW` | 0.9.2 / `bAZQdGpg` | Same exact shader ZIP | OpenGL shader-OFF and shader-ON LOW |

These Sodium versions are the exact dependencies declared by the selected Iris releases.
Complementary r5.9.3 declares all three Minecraft targets. Its LOW profile is a representative
bounded software-rendering configuration, not a claim about its other presets or frame rate.
The 14 shader option values come directly from that ZIP's `profile.LOW` declaration and are saved
in the dependency lock. The native test reads the applied values back through the exact Iris API.

Shader acceptance requires:

- the loaded Iris and Sodium JAR origins match the pinned SHA-256 values;
- the installed shader ZIP matches its pinned SHA-256;
- the initialized device reports OpenGL;
- `IrisApi.getInstance().isShaderPackInUse()` is true in the shader-ON world;
- the active pack name and all 14 LOW profile options match, including after reload;
- the shader-OFF control reports false with the same Iris/Sodium pair;
- M3 rendered ON/reloaded/OFF and M5 active-pack OFF/ON/air controls pass.

The Iris probe is a compile-time selected, test-only source. The base source has no Iris dependency.
Neither probe, nor the optional renderer JARs or shader assets, belongs to the product artifact.

## Native Vulkan matrix

| Minecraft | Requested API | Required actual API | Third-party renderer |
| --- | --- | --- | --- |
| 26.2 | `--graphicsBackend VULKAN` | Vulkan | None |
| 26.3 | `--graphicsBackend VULKAN` | Vulkan | None |

The official Minecraft client accepts `--graphicsBackend` through a `PreferredGraphicsApi` enum
converter. Its Vulkan preference can fall back to OpenGL. Consequently, the test reads
`RenderSystem.getDevice().getDeviceInfo().backendName()` and rejects an OpenGL fallback even if
startup and screenshots succeed. The 26.1.2 OpenGL adapter reads the older device's
`getBackendName()` method. No reflective backend detection is used.

Hosted Linux experiments may use Mesa's software Vulkan driver. Record the actual device and
driver from the initialized client. A software-driver pass is evidence for that configuration;
it does not establish performance or compatibility on AMD, Intel or NVIDIA hardware. A failure
to create a Vulkan device remains a blocked or failed Vulkan result, separate from OpenGL.
The normal 26.3 SDL/EGL setup continues to establish OpenGL evidence only.

## Uploaded reference-pack matrix

Original user-supplied bytes remain outside public Git and are never bundled in an uploaded
dependency artifact. The preparation script locates archives by these exact hashes, validates
bounded ZIP metadata, and creates local test aliases. It does not rewrite `pack.mcmeta`, import
RPO sidecars or change an installed user's resources.

| Test alias | Reference | SHA-256 | Declared resource formats |
| --- | --- | --- | --- |
| `amateras` | AMATERAS Resourcepack 1.21.7 v2 | `070bc338936f89320408e7b78bb6f74856917f5944deecc1a1b52390f5363616` | 64–84 |
| `chise` | Chise texture pack for 26.1.2 | `4d68bc024c8e92d2cca56815c9fab3509d7a21bfbbd580ec70458b23a0887d60` | 84 |
| `lowfire` | LowOnFire v26.2 | `e0eaf94bbc15f4c7d40727ecaf7a3b9eaf72ec2bf5fee53898613981159326a3` | 15–200 |
| `glowing` | NewGlowingOres Border v2.0 | `f8464293c2abe4bbb1b424fdec6e122a89b79f1936a02699cc832cafe7d9bb7f` | 8–84 |
| `small` | Small Handhelds | `3ae36207dae0a1c72b0be5b52b9975f706be936e205d20395a0b490887cbd4e0` | 46–1000 |

Official client `version.json` reports resource versions 84.0, 88.0 and 97.1 for the three targets.
AMATERAS, Chise and NewGlowingOres therefore declare 26.1.2's format but do not declare the newer
formats. On 26.2/26.3 their row is explicitly an **out-of-range forced-load experiment**. Successful
rendering must not be reported as an author's claim of version support.

For each target, test the five-pack order above and its exact reverse. Eight known resources are
resolved from the live resource manager and compared with bytes from the selected archives.
The two overlapping vanilla textures, `nether_gold_ore.png` and `nether_quartz_ore.png`, must switch
between AMATERAS and NewGlowingOres with the priority order. Other probes cover the anvil model,
diamond blockstate, white concrete, fire model, diamond texture and inherited handheld model.

Each order captures an OFF/ON/air triplet for nether gold ore, an anvil and a log. The test requires
a substantial base foreground, visible BlockLens cue changes, and at least 95% containment of the
OFF foreground by the ON foreground with the recorded two-pixel tolerance. It then restores the
original selected packs and BlockLens configuration. This is base-model preservation evidence;
it does not validate NewGlowingOres' optional OptiFine CTM/emissive integration or every handheld
transform merely because those resources resolved.

## Optional schematic integration inputs

The same downloader pins the separate #36 optional integration profile, without adding Masa mods
to the shader or Vulkan tests:

| Minecraft | Litematica | MaLiLib |
| --- | --- | --- |
| 26.1.2 | 0.27.14 / `w8P6TokG` | 0.28.12 / `FItuNokS` |
| 26.2 | 0.28.8 / `CuniXtbo` | 0.29.6 / `KvjmGjAV` |
| 26.3 | 0.29.1 / `ZqV316KY` | 0.30.2 / `JX9bESec` |

All three actual release JARs expose the typed `SchematicWorldHandler.getSchematicWorld()`
accessor. Artifact/API availability is not itself runtime verification. The Builder Assist native
oracle owns schematic match/mismatch, missing-chunk, empty/air and lifecycle assertions.

## Running and collecting evidence

Prepare one fresh profile directory. The command only prepares inputs in that directory:

```sh
python scripts/prepare_external_compatibility.py --minecraft 26.3 --profile shader-on --destination build/external/26.3-shader-on
```

The Gradle client task selects the matching test-only shader probe, adds only that profile's runtime
mods, copies the prepared configuration/shader assets into its isolated working directory, and
passes the actual requested graphics backend. Run with packaged runtime verification and the
external focus; never substitute this focused run for the normal CI gate.

The private pack profile additionally requires `--pack-dir` pointing to the directory containing
the original five archives. The script verifies content hashes instead of relying on filenames.
The `vulkan` profile rejects 26.1.2. The `masa` profile is dispatched to the Builder Assist oracle.

```sh
python scripts/verify_external_compatibility.py --directory versions/mc26_3/build/run/clientGameTest/external-compatibility --minecraft 26.3 --profile shader-on --destination build/ci/external-26.3-shader-on
```

The result verifier rejects wrong-version, partial, fallback-backend and inactive-shader manifests.
For rendering profiles it also checks the six PNG headers/dimensions and required result files.
The separate Masa profile requires all typed read/absence/restoration assertions in the schematic
manifest and the two pinned loaded-mod hashes. It has no image artifact. Every profile writes
SHA256SUMS. Its collector copies an explicit allowlist only; original packs,
mod JARs, configuration directories and arbitrary logs are not result artifacts.

## Evidence state

At harness implementation, the 15 Python provenance/isolation/false-pass regression tests pass.
Actual shader, Vulkan and uploaded-pack rendering results remain pending the corresponding native
runs. Record each attempted configuration, current commit, driver/backend, artifact hashes and
failure or pass outcome before changing this section or closing #31.

The local reference-pack attempt on 2026-10-10 reached a concrete display limitation. After
installing the missing X11 libraries from hash-verified Ubuntu packages, the normal authenticated
Xvfb server could not create either its local or Unix listening socket and exited with
`Cannot establish any listening sockets`. There was no game process or rendered result from this
attempt. Local Loom also requires unavailable Unix-domain IPC. These restrictions were not
modified and no transport tunnel was used to produce evidence. The uploaded archives are ready
for the private `packs` profile on an authorized native runtime; they remain pending until that
runtime is available. The public hosted workflow can exercise the author-published shader and
Masa artifacts plus the native Vulkan backend without receiving those private archives.

## Primary references

- [Iris official download page](https://www.irisshaders.dev/download/), which directs users to the author-managed Modrinth project.
- [Iris 26.1.2 release metadata](https://api.modrinth.com/v2/version/sZbVsl2Q), [26.2](https://api.modrinth.com/v2/version/gxZWWnKH), [26.3](https://api.modrinth.com/v2/version/vTN4NRGW).
- [Complementary Reimagined r5.9.3 metadata](https://api.modrinth.com/v2/version/Bqen1mJX).
- [Modrinth Maven Litematica artifact metadata](https://api.modrinth.com/maven/maven/modrinth/litematica/ZqV316KY/litematica-ZqV316KY.pom).
- [Official 26.3 launch metadata](https://piston-meta.mojang.com/v1/packages/702fe59163c6ee6578607daa85811d9bc9c7cc40/26.3.json), including the client artifact used for direct API inspection.
