# Three-version integration

Status: current three-version development contract; not a published release.

## Supported targets

The current development targets are Minecraft 26.1.2, 26.2 and 26.3, each with an exact-version client-only JAR. Java 25 and Fabric Loader 0.19.5 are required. Fabric API pins are respectively 0.155.2+26.1.2, 0.160.0+26.2 and 0.162.0+26.3. All 40 capabilities, persisted keys, bit positions and five enabled defaults remain unchanged.

The 26.3 module compiles the modern mc26_2 adapters and loading-overlay test bridge independently against its own dependencies. The complete renderer is not copied. Shared UI and test input uses InputConstants: 26.3 moved from GLFW to SDL, so raw key/button/modifier values do not preserve behavior.

Fabric's 26.3 renderer API removed diffuseShade. A tiny MinecraftQuadLighting boundary retains diffuseShade(false) for 26.2 and uses shadeDirectionOverride(Direction.UP) for 26.3. Independent bytecode review confirmed that both Indigo paths select cardinalLighting.up(): default brightness 1.0 and Nether brightness 0.9. Emissive and disabled ambient occlusion are retained separately. This does not establish shader or Vulkan compatibility.

## Visible client UI automation

Install Java 25 and Gradle 9.5.1; set JAVA_HOME and make gradle available on PATH. From the repository root on Windows:

```powershell
./scripts/run-ui-tests.ps1
./scripts/run-ui-tests.ps1 -Version 26.3
```

The first command launches the three Minecraft windows sequentially. The second selects only 26.3. These are real Fabric client GameTests with rendered frames, input events, window resizing, save/discard/failure recovery and render/parity checks. They use isolated generated test worlds and exit after testing. A nonzero exit stops the runner; merely opening a window does not count as success. Game startup may fetch public Minecraft/Fabric assets on the first run.

Settings screenshots are written under each module's build/run/clientGameTest/settings-ui directory. CI runs the same client suite under Xvfb and requires ten settings screenshots per target: four categories at 854×480 and a compact Resources view at 320×240, in English and Japanese. The responsive oracle additionally checks 640×360 and 1920×1080; save-failure/retry behavior has separate assertions. Its three version jobs also retain rendering evidence and JAR audit/reproducibility results. Common JUnit/JaCoCo/PIT runs separately. Evidence counts only for the current head SHA.

## Reviewed artifact budget

The development no-growth baseline increases by exactly 1 KiB, from 112640 to 113664 bytes (111 KiB). Initial Windows artifacts measured 112628 / 113335 / 113379 bytes. The largest exceeds the former baseline by 739 bytes. Independent review confirmed that the required lighting bridges contribute 364 / 410 compressed bytes plus archive entries, alongside input/runtime-version adaptation. This bounded compatibility adjustment preserves all capabilities. The release ceiling of 153600 bytes and absolute ceiling of 1183432 bytes remain unchanged. Every build must pass the per-target size gates; these measurements are not a performance claim.

## Verification status and limits

Local ciGate passed: 150 common tests, selected coverage/mutation gates and all three builds, artifact smoke contracts and size budgets. The 26.3 real-client suite completed successfully, including ten settings screenshots and all-40 render tests. Initial 26.3 native startup opened an SDL/OpenGL window but exited with Windows access violation 0xC0000005 during resource loading. This did not reproduce on the unchanged rerun; its cause remains undetermined. A successful rerun does not establish native-startup stability. Per-version client results, current-head CI and screenshots must be checked on the pull request; an earlier SHA does not close these gates.

PR #40's settings redesign was merged at ea488677cbae13ca2f80da685dd9831eace88e7f. Its two-version evidence does not validate this three-version change. Uploaded third-party pack combinations, shaders and Vulkan remain unverified. Release creation and merging are separate actions.

## Official references

- [Fabric for Minecraft 26.3](https://www.fabricmc.net/2026/09/15/263.html)
- [Fabric automated testing](https://docs.fabricmc.net/develop/automatic-testing)
