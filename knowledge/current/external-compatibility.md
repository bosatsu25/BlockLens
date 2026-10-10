# External rendering compatibility

Status: **partial external coverage** for [Issue #31](https://github.com/bosatsu25/BlockLens/issues/31).
The pinned matrix below defines the experiments, with attempted-run evidence retained below.
[PR #53](https://github.com/bosatsu25/BlockLens/pull/53) records the initial accepted matrix and
merge. [The delivery record](remaining-delivery.md) and #31 also retain subsequent main failures
and follow-up verification. The uploaded-pack and known Sodium boundaries keep #31 open.

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

### Known Sodium boundary

The pinned Sodium 0.9.2 terrain mesher bypasses vanilla `SectionCompiler.compile`
on all three targets. This was confirmed from the published JAR call paths during
independent review. BlockLens's new explicit/tagged ore-extension hook and existing
terrain block-filter hook are attached to that vanilla compiler and have no Sodium
adapter. Those two paths are therefore unsupported with this Sodium stack. Entity
filter behavior is a separate path and is not proved by this code-level finding.
The M3/M5 shader fixtures exercise existing model-bake wrappers, so passing those
fixtures must not be presented as all-57-capability shader support. Issue #31 retains
this renderer boundary alongside the pending uploaded-pack matrix.

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

### 26.1.2 shader-OFF capture lifecycle exception

At commit `835de37d07a3464fe2b227e7bd2afe865accf356`, external run
[`38069788830`](https://github.com/bosatsu25/BlockLens/actions/runs/38069788830)
reached the first M3 screenshot on 26.1.2 shader-OFF and failed with Sodium's
`Global terrain uniforms have not been updated` exception. Fabric's 26.1.2 screenshot helper
invokes `GameRenderer.extract/render` outside the complete native frame. Sodium 0.9.2 clears
its terrain uniforms at frame end, while its update flag can still reject the extra update.
The test-only exception therefore reads the framebuffer after two complete native frames,
without issuing another direct renderer invocation. It retains 640 by 360 dimensions, the
existing GUI/FOV options, and every original M3/M5 image and geometry assertion; it restores
the previous window and render-target dimensions in `finally`. The evidence records
`captureMode=completed-native-frame`. Other rows keep Fabric's screenshot path.

The same run passed both shader profiles on 26.2/26.3, all three Masa profiles and both Vulkan
profiles. The separate 26.1.2 shader-ON M5 failure measured 862/1000 base retention against the
unchanged 950/1000 minimum. That result remains a failed run in the evidence record;
the shader-OFF lifecycle exception does not establish a fix for it.

The next [external run](https://github.com/bosatsu25/BlockLens/actions/runs/38070878498),
at `71f6d9a5cbe1f9b9bc35ca9c3415dbf54840e05f`, passed Shader-OFF on 26.1.2
and the other nine previously passing rows. Shader-ON on 26.1.2 again failed,
at 780/1000 retention. The hash-verified failure frames show that 445 of 487
unmatched OFF-mask pixels are in the unchanged grass below the fixture board
(rows 290–314). Its dynamic rendered background was contaminating the target
subtraction mask. Both failures remain in the record; the correction must
retain the original three models, full measurement region, 950/1000 minimum,
two-pixel neighborhood and exact shader settings.

The external M5 fixture therefore extends its smooth-quartz floor to cover the
entire original measurement region. A bounded native guard projects all four
region corners from the actual camera height/FOV and checks both the fixed floor
bounds and the loaded quartz cells. The normal full-graph floor is unchanged.
The three target positions, screenshot path, original ROI, color threshold,
two-pixel tolerance, 95% retention minimum and LOW profile are unchanged. This
repairs a test background; it adds no product renderer adapter or support claim.

The first guarded-floor attempt, at `c60f57de5914304a6ec472f32a115e56825ad051`
([external run #4](https://github.com/bosatsu25/BlockLens/actions/runs/38071918467)),
passed the three Masa rows but failed all eight rendering rows at the new camera
assertion, before M5 capture. The test had incorrectly treated the existing
`tp @a 0 -54 13 180 18` command as X=0, Z=13. Direct inspection of all three
official clients confirms that `Vec3Argument.vec3()` centers absolute integer X/Z
tokens, so the actual unchanged camera is **X=0.5, Z=13.5**. The guard and projection
now use those actual coordinates. The teleport command and native camera framing
remain unchanged, and the guard reports actual coordinates/angles on failure.
This failed attempt provides no native acceptance of the new background.

### Isolating M5 from the preceding M3 scene

The accepted PR head `2120ccdb006b262eccb96fd888d7f07db5b413c3` passed all eleven
[external #6 jobs](https://github.com/bosatsu25/BlockLens/actions/runs/38074634660).
Its identical merged tree at `9609c4dd70fb162ba8479d6e606baab2fe8eadad` then produced a
26.2 Shader-ON failure in [main external #7](https://github.com/bosatsu25/BlockLens/actions/runs/38075582520):
retention **939/1000 (1486/1581)**, required 950. Ten other rows passed. The original
log and digest-verified failure artifact `11677459914` remain preserved.

M3 and M5 share that focused test world. M3's thirteen blocks had remained in the M5
comparison, including opaque blocks in front of its three target models. Diagnostic
projection of the original model elements and occluders placed **94 of the 95** unmatched
pixels outside the visible target probes, even with a two-pixel diagnostic halo. The
probe-area result of 387/388 (**997/1000**) is localization evidence, not a replacement
acceptance mask. This projection is inferred from source; the actual native projection
matrix was not recorded. The temporal shader cause of individual color changes remains
unproven. Equalizing only five known unchanged M3 surface strips in memory makes
the original global metric 979; removing the individual target models from that control
still fails at 940 / 938 / 860. The original native images and 939 failure are unchanged.

Before placing the same three M5 targets, the external-only setup now clears exactly
**52 cells**, X=-6..6, Z=-1..2, Y=-59. After the existing twenty-tick synchronization,
a native guard requires the three original block types at their original positions and
AIR in the other 49 cells. This physically isolates the fixtures and removes prior
occlusion; it does not crop or alter screenshot pixels. Successful M5 evidence records
`externalSceneIsolated=true`, which the public shader/Vulkan artifact verifier requires.
The separate Masa and private-pack profiles are unchanged.

The normal full graph and M8 scene, quartz floor, camera, target models, screenshot
sequence, complete ROI, color delta 24, two-pixel tolerance, 950/1000 retention minimum,
LOW options and deadlines remain unchanged. Acceptance requires fresh three-version
native evidence for this scene revision; no previous successful run supplies that gate.

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

Failed public client rows also retain a bounded partial manifest and the six named M3/M5 PNGs,
two metric manifests and the fixed external lifecycle diagnostic file, when created. This
explicit ten-file allowlist rejects symlinks,
limits images to 4 MiB at 640 by 360 and text to 64 KiB, and writes SHA-256 hashes plus
`DIAGNOSTIC_STATUS.txt` with `status=failed-or-incomplete`. The acceptance verifier rejects
that diagnostic directory even if a copied native manifest says `passed`. This artifact is
for diagnosis and cannot turn a failed row into passing evidence.

The optional Masa test records progress around schematic assertions, client restoration,
evidence copying and world closure. A test-only daemon watchdog records one bounded thread
snapshot if a phase remains unchanged for 60 seconds. Its fixed output is
`external-compatibility/external-lifecycle-diagnostics.txt`, written by atomic replacement
with at most 16 selected thread stacks, 24 frames per thread and 64 KiB of ASCII text.
It observes the wait without interrupting the client, server or test thread. Closing the
watchdog cancels its scheduled work without waiting for a thread to join. Existing native
assertions, the twelve-minute step deadline and the success manifest remain authoritative.
The watchdog is restricted to the focused Masa profile; it is not a runtime MOD feature or
an M8 measurement condition.

## Evidence state

Python provenance/isolation/false-pass regression tests are part of the common gate. Normal
[CI #343](https://github.com/bosatsu25/BlockLens/actions/runs/38070878467) passed
all four jobs at `71f6d9a5cbe1f9b9bc35ca9c3415dbf54840e05f`. Its paired
[external run](https://github.com/bosatsu25/BlockLens/actions/runs/38070878498)
passed all three exact Masa pairs, actual Vulkan on 26.2/26.3, both shader profiles
on 26.2/26.3 and Shader-OFF on 26.1.2. The separate Shader-ON failure and subsequent
fixture correction are documented above. Final current-head acceptance requires
every public matrix row and the normal graph; its immutable run and merge links
are retained in PR #53 and the delivery issues. None of these observations validates
the uploaded-pack matrix or the unsupported Sodium terrain paths.

At `05e782e4c399e5ae569ef542c7fc7737297b5a74`, normal
[CI #345](https://github.com/bosatsu25/BlockLens/actions/runs/38072553708) passed
the complete three-version graph. [External run #5](https://github.com/bosatsu25/BlockLens/actions/runs/38072553687)
passed all eight rendering rows and Masa on 26.1.2 / 26.3. The native 26.1.2
Shader-ON retention was 996/1000 with the static background and unchanged 950
minimum. All ten successful artifacts were digest-checked and strictly validated.
The 26.2 Masa row reached all typed schematic assertions and then stalled before
the normal disconnect logs, until the twelve-minute timeout. Its original log and
failure artifact `11677452307` are preserved. The precise blocking operation is
unproven; lifecycle instrumentation supplies the missing observation without
claiming the failed attempt passed or changing product behavior. Subsequent exact
head and merge results are recorded in PR #53 and the delivery issues.

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
