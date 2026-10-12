# External rendering compatibility

Status: **completed external coverage** for [Issue #31](https://github.com/bosatsu25/BlockLens/issues/31).
The pinned matrix below defines the experiments, with verified native evidence retained below.
[PR #53](https://github.com/bosatsu25/BlockLens/pull/53) records the initial accepted matrix and
merge. [The delivery record](remaining-delivery.md) and #31 also retain subsequent main failures,
follow-up verification, and the native execution of the private reference-pack matrix.

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

### Bounded Sodium terrain integration

The pinned Sodium 0.9.2 terrain mesher bypasses vanilla `SectionCompiler.compile`
on all three targets. This was confirmed from the published JAR call paths during
independent review and was the unsupported boundary delivered in PR #53.
The [optional Sodium terrain adapter](sodium-terrain-compatibility.md) now connects
the existing explicit/tagged ore model selection and terrain block filtering at
Sodium's model-rendering entry. Only exact matching `0.9.2+mc26.1.2`,
`0.9.2+mc26.2` and `0.9.2+mc26.3` metadata pairs enable it. The six shader rows
require separate native ore/filter images, actual emitted-model identity and
restoration evidence. Entity filter behavior is a separate path and is not proved
by the original compiler-bypass finding or by these terrain probes.
The M3/M5 shader fixtures exercise existing model-bake wrappers, so passing those
fixtures must not be presented as all-57-capability shader support. Issue #31 retains
the exact version/rendering boundary alongside the pending uploaded-pack matrix.

## Pinned shader matrix

Primary release metadata was retrieved on 2026-10-10 UTC from the author's Modrinth projects.
The complete download URLs, lengths, SHA-512 and SHA-256 values are checked into
[`external_compatibility_dependencies.json`](../../scripts/external_compatibility_dependencies.json).
The setup verifies both hashes before loading an artifact; it never selects a floating latest version.

| Minecraft | Iris / release ID | Sodium / release ID | Shader | Profiles |
| --- | --- | --- | --- | --- |
| 26.1.2 | 1.11.4 / `sZbVsl2Q` | 0.9.2 / `tZQ3jqnf` | Complementary Reimagined r5.9.3 / `Bqen1mJX` | OpenGL shader-OFF and shader-ON LOW_STATIC |
| 26.2 | 1.11.4 / `gxZWWnKH` | 0.9.2 / `xJZxADzI` | Same exact shader ZIP | OpenGL shader-OFF and shader-ON LOW_STATIC |
| 26.3 | 1.11.7 / `vTN4NRGW` | 0.9.2 / `bAZQdGpg` | Same exact shader ZIP | OpenGL shader-OFF and shader-ON LOW_STATIC |

These Sodium versions are the exact dependencies declared by the selected Iris releases.
Complementary r5.9.3 declares all three Minecraft targets. Its LOW profile is a representative
bounded software-rendering configuration, not a claim about its other presets or frame rate.
The original 14 shader options come directly from that ZIP's `profile.LOW` declaration.
The current `LOW_STATIC` fixture adds `WAVING_SPEED=0.00` and `TAA_JITTER=0` for deterministic restoration;
all 16 locked values are read back through the exact Iris API. See the static-motion boundary below.

Shader acceptance requires:

- the loaded Iris and Sodium JAR origins match the pinned SHA-256 values;
- the installed shader ZIP matches its pinned SHA-256;
- the initialized device reports OpenGL;
- `IrisApi.getInstance().isShaderPackInUse()` is true in the shader-ON world;
- the active pack name, all 14 LOW options and both static-fixture overrides match, including after reload;
- the shader-OFF control reports false with the same Iris/Sodium pair;
- M3 rendered ON/reloaded/OFF and M5 active-pack OFF/ON/air controls pass;
- the separate Sodium fixture proves explicit/tagged ore emission, unchanged active
  base models, terrain filtering, restoration and reload with its bounded observer
  and image controls.

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
| `reference-ui` | Source reference pack 1.21.7 v2 | `070bc338936f89320408e7b78bb6f74856917f5944deecc1a1b52390f5363616` | 64–84 |
| `chise` | Chise texture pack for 26.1.2 | `4d68bc024c8e92d2cca56815c9fab3509d7a21bfbbd580ec70458b23a0887d60` | 84 |
| `chise-server` | Chise server texture pack for 26.1.2 | `53d456fa9396a362d8f408a7f17f70372ef8df15e4fc9e2439db31fbd23f1109` | 84 |
| `lowfire` | LowOnFire v26.2 | `e0eaf94bbc15f4c7d40727ecaf7a3b9eaf72ec2bf5fee53898613981159326a3` | 15–200 |
| `glowing` | NewGlowingOres Border v2.0 | `f8464293c2abe4bbb1b424fdec6e122a89b79f1936a02699cc832cafe7d9bb7f` | 8–84 |
| `small` | Small Handhelds | `3ae36207dae0a1c72b0be5b52b9975f706be936e205d20395a0b490887cbd4e0` | 46–1000 |

Official client `version.json` reports resource versions 84.0, 88.0 and 97.1 for the three targets.
The source reference, both Chise packs and NewGlowingOres therefore declare 26.1.2's format but do not declare the newer
formats. On 26.2/26.3 their row is explicitly an **out-of-range forced-load experiment**. Successful
rendering must not be reported as an author's claim of version support.

For each target, test the six-pack order above and its exact reverse. The live resource manager
must resolve all **29** pinned probes to the exact selected source and asset SHA-256. This includes
the original eight probes, all three regular chest textures, and the server pack's 18 pale-oak
block/item/boat/sign/UI textures. Reversing order must change exactly **six** winners: two Nether
ore textures, white concrete and the three regular chest textures. The public lock records resource names and candidate aliases only. Expected
material hashes are computed from the validated private input archives at verification time; the independent collector rejects missing/duplicate rows, wrong winners,
wrong bytes and failed native geometry metrics. Retention is recomputed from foreground/retained
counts within the unchanged 60,200-pixel region, 24-color-delta/two-pixel tolerance and 950-permille gate.

Resolving a texture does not prove that a version's item/entity/UI renderer uses that legacy path.
This is six-pack resource priority and scoped base-model preservation evidence, not implementation
or rendered parity of the pale-oak palette, world-fire style, CTM or every handheld transform.

Each order captures an OFF/ON/air triplet for nether gold ore, an anvil and a log. The test requires
a substantial base foreground, visible BlockLens cue changes, and at least 95% containment of the
OFF foreground by the ON foreground with the recorded two-pixel tolerance. It then restores the
original selected packs and BlockLens configuration. This is base-model preservation evidence;
it does not validate NewGlowingOres' optional OptiFine CTM/emissive integration or every handheld
transform merely because those resources resolved.

## Historical optional schematic integration inputs (retired by PR #60)

The former #36 test profile used the following pinned inputs. The current downloader does not
accept `masa`; the integration and comparison capability were removed by PR #60. This table is
historical evidence only and is not a current launch procedure:

| Minecraft | Litematica | MaLiLib |
| --- | --- | --- |
| 26.1.2 | 0.27.14 / `w8P6TokG` | 0.28.12 / `FItuNokS` |
| 26.2 | 0.28.8 / `CuniXtbo` | 0.29.6 / `KvjmGjAV` |
| 26.3 | 0.29.1 / `ZqV316KY` | 0.30.2 / `JX9bESec` |

The three former pinned dependency JARs exposed the typed `SchematicWorldHandler.getSchematicWorld()`
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
the original six archives. The script verifies content hashes instead of relying on filenames.
The `vulkan` profile rejects 26.1.2. Current profiles are `shader-off`, `shader-on`, `vulkan` and `packs`.

```sh
python scripts/verify_external_compatibility.py --directory versions/mc26_3/build/run/clientGameTest/external-compatibility --minecraft 26.3 --profile shader-on --destination build/ci/external-26.3-shader-on
```

The result verifier rejects wrong-version, partial, fallback-backend and inactive-shader manifests.
Rendering profiles retain the six original M3/M5 PNGs and now require six native palette PNGs
plus their manifest. Each Shader OFF/ON row also requires the fifteen Sodium PNGs and its
native manifest: 27 PNGs and 32 result files before SHA256SUMS. Vulkan rows require 12 PNGs
and 16 result files; private-pack rows preserve their six-image/private-input boundary.
The new palette rows remain pending current-head hosted evidence. The verifier
checks every required PNG header, dimensions and named result file.
Every current profile writes
SHA256SUMS. Its collector copies an explicit allowlist only; original packs,
mod JARs, configuration directories and arbitrary logs are not result artifacts.

Failed public client rows also retain a bounded partial manifest and the six named M3/M5 PNGs,
two metric manifests and the fixed external lifecycle diagnostic file, when created. The
fifteen named Sodium PNGs and its manifest extend that fixed allowlist to 26 possible
inputs. The collector rejects symlinks, limits images to 4 MiB at 640 by 360,
the Sodium manifest to 16 KiB and other text to 64 KiB, and writes SHA-256 hashes plus
`DIAGNOSTIC_STATUS.txt` with `status=failed-or-incomplete`. The acceptance verifier rejects
that diagnostic directory even if a copied native manifest says `passed`. This artifact is
for diagnosis and cannot turn a failed row into passing evidence.

## Historical lifecycle and five-pack acceptance

The retired optional Masa test recorded progress around schematic assertions, client restoration,
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

The historical five-pack/eight-probe reference attempt on 2026-10-10 reached a display limitation under headless Xvfb on Linux.
On 2026-10-11, native client execution was performed on an authorized local Windows runtime with
Adoptium JDK 25 and Gradle 9.5.1 using the original hash-verified private pack archives.
All three supported Minecraft targets (26.1.2, 26.2, and 26.3) executed the full forward/reverse
pack loading sequence, validated live resource manager resolution and model pipeline restoration,
and passed visual retention contracts:
- 26.1.2: forward retention 981/1000 (base foreground 2449, changed pixels 604), reverse retention 1000/1000 (base foreground 2374, changed pixels 604).
- 26.2: forward retention 985/1000 (base foreground 2443, changed pixels 604), reverse retention 1000/1000 (base foreground 2381, changed pixels 604).
- 26.3: forward retention 985/1000 (base foreground 2441, changed pixels 604), reverse retention 1000/1000 (base foreground 2380, changed pixels 604).
In all three versions, all eight probe resources resolved with exact matching SHA-256 digests,
the two overlapping ore textures shifted between source reference and NewGlowingOres as priority reversed,
and selected packs/config were strictly restored. All twelve result files per version were verified
by `scripts/verify_external_compatibility.py`. The original archives remain outside git and public artifacts.

## Primary references

- [Iris official download page](https://www.irisshaders.dev/download/), which directs users to the author-managed Modrinth project.
- [Iris 26.1.2 release metadata](https://api.modrinth.com/v2/version/sZbVsl2Q), [26.2](https://api.modrinth.com/v2/version/gxZWWnKH), [26.3](https://api.modrinth.com/v2/version/vTN4NRGW).
- [Complementary Reimagined r5.9.3 metadata](https://api.modrinth.com/v2/version/Bqen1mJX).
- [Modrinth Maven Litematica artifact metadata](https://api.modrinth.com/maven/maven/modrinth/litematica/ZqV316KY/litematica-ZqV316KY.pom).
- [Official 26.3 launch metadata](https://piston-meta.mojang.com/v1/packages/702fe59163c6ee6578607daa85811d9bc9c7cc40/26.3.json), including the client artifact used for direct API inspection.

## Private material-hash boundary

Individual asset SHA-256 values remain local. The public lock contains only the 29 logical
resource names and their candidate pack aliases; the six whole-archive hashes were already public.
Use `--pack-input` with the private prepared directory when verifying `packs` results. The verifier
checks each archive's pinned bytes before deriving the expected material hashes, preserving exact
resource/priority validation. Collection removes the third material-hash column from resource TSVs;
raw native proof remains private and is not overwritten. A collected two-column resource report
cannot substitute for the raw private acceptance proof. Other shader/backend rows need no private
inputs and keep their existing verification contract.

## Static restoration fixture motion

The original 14 LOW options remain unchanged. The current screenshot-restoration harness additionally pins `WAVING_SPEED=0.00`; the actual loaded Iris option must be verified alongside all 14 LOW values. This freezes shader vertex-animation time without disabling foliage/leaves, SSAO, lighting or shader activation. The pinned shader ZIP remains unmodified. Its `wavingBlocks.glsl` multiplies `frameTimeCounter` by `WAVING_SPEED`, whereas freezing the Minecraft server does not freeze that shader clock. On the three latest failed native Shader ON runs, oak-leaf grain correlation varied from 0.50 to 0.68 despite stable foreground and near-original variance. Raw frames and logs remain preserved. A regression contract first failed on the absent motion-time option. The existing RGB spatial-noise, detail-preservation and missing/flattened/shuffled negative gates remain unchanged. This profile verifies static restoration; animated shader appearance and arbitrary shader settings are separate unverified scope.

The first local frozen-wind 26.3 Shader ON run still failed leaf restoration. Its frames/logs are preserved separately. The shader also varies subpixel projection by `framemod8` in `jitter.glsl`; `TAA_JITTER=0` skips that coordinate shift while leaving TAA itself enabled. Add and verify this second explicit static-fixture option instead of relaxing the spatial/detail gates. The frozen-wind-only attempt is a failed diagnostic experiment, not completion evidence.
