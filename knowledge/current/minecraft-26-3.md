# Three-version integration

Status: current three-version development contract; not a published release.

## Supported targets

The current development targets are Minecraft 26.1.2, 26.2 and 26.3, each with an exact-version client-only JAR. Java 25 and Fabric Loader 0.19.5 are required. Fabric API pins are respectively 0.155.2+26.1.2, 0.160.0+26.2 and 0.162.0+26.3. The original 40 capabilities, persisted keys, bit positions and five enabled defaults remain unchanged. The additive #42 development scope has 46 controls and seven enabled defaults, specified in [lightweight-visuals.md](lightweight-visuals.md).

The 26.3 module compiles the modern mc26_2 adapters and loading-overlay test bridge independently against its own dependencies. The complete renderer is not copied. Shared UI and test input uses InputConstants: 26.3 moved from GLFW to SDL, so raw key/button/modifier values do not preserve behavior.

Fabric's 26.3 renderer API removed diffuseShade. A tiny MinecraftQuadLighting boundary retains diffuseShade(false) for 26.2 and uses shadeDirectionOverride(Direction.UP) for 26.3. Independent bytecode review confirmed that both Indigo paths select cardinalLighting.up(): default brightness 1.0 and Nether brightness 0.9. Emissive and disabled ambient occlusion are retained separately. This does not establish shader or Vulkan compatibility.

## Visible client UI automation

Install Java 25 and Gradle 9.5.1; set JAVA_HOME and make gradle available on PATH. From the repository root on Windows:

```powershell
./scripts/run-ui-tests.ps1
./scripts/run-ui-tests.ps1 -Version 26.3
```

The first command launches the three Minecraft windows sequentially. The second selects only 26.3. These are real Fabric client GameTests with rendered frames, input events, window resizing, save/discard/failure recovery and render/parity checks. The runner sets `-PverifyPackagedRuntime=true`: it loads the built outer artifact and its owned runtime container, excludes loose development product classes, and checks actual class origins. See [runtime-packaging.md](runtime-packaging.md) for the packaging contract and development-environment limitation. Tests use isolated generated worlds and exit after testing. A nonzero exit stops the runner; merely opening a window does not count as success. Game startup may fetch public Minecraft/Fabric assets on the first run.

Settings screenshots are written under each module's build/run/clientGameTest/settings-ui directory. CI runs the same client suite under Xvfb and requires ten settings screenshots per target: four categories at 854×480 and a compact Resources view at 320×240, in English and Japanese. The responsive oracle additionally checks 640×360 and 1920×1080; save-failure/retry behavior has separate assertions. Its three version jobs also retain rendering evidence and JAR audit/reproducibility results. Common JUnit/JaCoCo/PIT runs separately. Evidence counts only for the current head SHA.

## Historical #41 artifact budget and current additive scope

For #41, the development no-growth baseline increased by exactly 1 KiB, from 112640 to 113664 bytes (111 KiB). Initial Windows artifacts measured 112628 / 113335 / 113379 bytes. The largest exceeds the former baseline by 739 bytes. Independent review confirmed that the required lighting bridges contribute 364 / 410 compressed bytes plus archive entries, alongside input/runtime-version adaptation. This bounded compatibility adjustment preserves all capabilities. The release ceiling of 153600 bytes and absolute ceiling of 1183432 bytes remain unchanged. Every build must pass the per-target size gates; these measurements are not a performance claim.

The historical #42 development guard was 143360 bytes (140 KiB), with 153600-byte release and 1183432-byte absolute ceilings. The user authorized the current 204800-byte development/release ceiling on 2026-10-11; see [remaining-delivery.md](remaining-delivery.md). Three-version native render/UI tests and 167 common tests passed locally; current-head hosted CI and merge remain required. See [lightweight-visuals.md](lightweight-visuals.md) for final artifacts and delta evidence.

## Historical #41 verification and launch diagnosis

At #41, local ciGate passed: 150 common tests, selected coverage/mutation gates and all three builds, artifact smoke contracts and size budgets. Each version's real-client suite completed successfully, including ten settings screenshots and all-40 render tests. Further 26.3 launch attempts also exposed an intermittent Windows access violation 0xC0000005 during initial resource loading.

Diagnosis found that Mojang's official 26.3 launch metadata requires -XX:StackShadowPages=32, which the generated Loom client launch omitted. The native failure phase matches the upstream report below. The 26.3 module now explicitly applies this official argument to client runs. verifyClientLaunchContract inspects the actual JVM arguments of both runClient and runClientGameTest; it failed before the fix and passed afterwards, and is required by check/build. This changes development/test launch configuration, not runtime mod behavior or JAR content. External launchers must honor Minecraft's version metadata themselves.

Per-version client results after this fix, current-head CI and screenshots must be checked on the pull request; an earlier SHA does not close these gates. Do not infer general native-startup stability from a bounded number of successful test runs.

The first Ubuntu 26.3 CI launch failed before tests with "Couldn't find matching GLX visual"; Vulkan fallback also could not initialize in that virtual display. The CI client step selects SDL_VIDEO_FORCE_EGL=1 only for 26.3 under Xvfb so SDL creates the OpenGL context through EGL. Earlier GLFW targets keep their existing context path. The client step has an eight-minute timeout to preserve failure diagnostics instead of hanging indefinitely in a native startup dialog. This is CI environment configuration and does not enable or establish Vulkan support.

PR #40's settings redesign was merged at ea488677cbae13ca2f80da685dd9831eace88e7f. Its two-version evidence does not validate this three-version change. Uploaded third-party pack combinations, shaders and Vulkan remain unverified. Release creation and merging are separate actions.

The subsequent Ubuntu launch selected EGL but failed with "Could not load EGL library" before tests. SDL's loader falls back to libEGL.so.1. The 26.3 CI job therefore explicitly installs Ubuntu's libegl1 (including its Mesa EGL dependency) and libgl1-mesa-dri before client startup. This dependency setup is restricted to the ephemeral 26.3 CI runner; local machines and earlier version jobs are unchanged. Hosted client evidence is still required after this environment repair.

## Official references

- [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html)
- [Fabric automated testing](https://docs.fabricmc.net/develop/automatic-testing)
- [Official Minecraft 26.3 launch metadata](https://piston-meta.mojang.com/v1/packages/702fe59163c6ee6578607daa85811d9bc9c7cc40/26.3.json)
- [Upstream Windows native startup report and stack-shadow argument](https://github.com/PrismLauncher/PrismLauncher/issues/6073)
- [Official SDL3 EGL context selection](https://wiki.libsdl.org/SDL3/SDL_HINT_VIDEO_FORCE_EGL)
- [Official SDL EGL loader](https://github.com/libsdl-org/SDL/blob/main/src/video/SDL_egl.c)
- [Ubuntu 24.04 EGL runtime and Mesa dependency](https://packages.ubuntu.com/noble/libegl1)
