# Rendered pale-oak object evidence

Issue #64 requires actual rendering beyond texture decoding. Preserve the palette control, frozen keys/defaults and all three supported targets. Native scenes cover boat, chest boat, standing sign, hanging sign and both blank sign editors. Every scene captures AIR, OFF, ON, restored and all-enabled frames. A test-only scoped GUI hook hides only the sign background for AIR and resets in finally. The hanging support remains identical in AIR/OFF/ON. Fixtures contain no user text or inventory.

Select visible OFF foreground against paired AIR independent of illumination color. Require at least80 samples,95-percent retention, warm-chroma reduction>=2 and brightness-normalized reduction>=4 permille, non-flat grain with variance0.5..2 and correlation>=0.9, and mean brightness difference<=48. Restoration retains99 percent, correlation>=0.99 and mean absolute RGB difference<=4. Missing/black/flat/shuffled/unchanged/uniform-dimming and support-only recoloring negative controls remain required.

World camera is Z9.5/playerY-56.5/pitch25. The hanging-world scene uses Z7.5/playerY-60/pitch6 to show the full wooden face without support occlusion. Require absent objects and exact camera readiness, then settle20 client frames. Only the six auxiliary Shader ON scenes use40 completed native frames after capture-size changes, through typed render-target bridges. Existing base-palette, terrain and Sodium Shader capture routes stay intact; the26.1.2 Sodium OFF exception remains two frames. All16 LOW_STATIC options are verified through actual Iris. This validates static settings, not arbitrary animated Shader configurations.

The opt-in Vulkan restoration watchdog records at most16 selected stacks,24 frames and64KiB after a60-second delay. It changes no test result and creates no default-client watcher. The historical intermittent cleanup stall remains undiagnosed.

Fresh all-enabled native clients pass the six scenes and exit cleanly in2m26s/2m27s/2m27s
on26.1.2/26.2/26.3. Each strict collector accepts30 images plus the native manifest (31 files
before SHA256SUMS). Shared JaCoCo/PIT,105 Python contracts and all three builds/native compilations
pass. The earlier10m22s local26.3 static Shader graph used the previous24-object-image schema
and157 result files; it is historical evidence, not validation of the expanded30-image schema.
Fresh full external evidence must contain163 files. No input-pack material hashes are published.
Require final-head full hosted normal/Shader/Vulkan/BBE CI; normal steps remain22 minutes/30-minute
jobs, while expanded external steps use28 minutes/36-minute jobs. Durations measure test workload,
not runtime performance improvement.

Each of the six object/editor roles now receives a saved, reloaded all-capabilities-enabled pass. The active mask is checked before capture, and the same foreground, chroma and spatial-grain oracle applies. Other flags are restored before the original OFF restoration. The strict native manifest requiresallFeatures=true and all six additional frames. Fresh three-version native validation has passed; current-head full hosted CI remains required.

Hosted CI38197558645 at81e67fb passes13/14 jobs; only26.3 Shader ON reaches22 minutes. Preserved SHA256/CRC-verified diagnostics show all six object scenes and fourteen terrain roles complete; the Sodium manifest has successful tagged-restoration/reload checks and stage=stone-hidden,status=started. This is unfinished graph work, not a passing row or evidence of a repaired cleanup stall. Give the expanded six-all-enabled-scene graph a bounded28-minute external step and36-minute job. Preserve every image/detail/runtime-performance threshold and require new-head full CI.

## Blank-editor time control

A same-tree post-merge Shader ON replay failed OFF restoration in the26.2 standing-sign and26.3 hanging-sign editors. Digest/CRC-verified synthetic frames showed20 changed foreground pixels from the native blank-text blinking caret; wood/hardware colors and geometry restored, but grain correlation fell below the unchanged.99 threshold. The test-only AbstractSignEditScreen text callback is suppressed only inside the scoped blank-fixture oracle, with scope reset in finally. Actual native sign background, texture, lighting, editor layout/buttons and the AIR negative control remain rendered. No production mixin or text handling changes; user text is outside this empty fixture. A20-pixel caret regression continues to reject moving foreground in the strict image oracle. No chroma, retention, grain or OFF-restoration thresholds are relaxed. Fresh native/external evidence is required for the updated test head.

## Base-palette framebuffer signal

The26.3 static Shader ON replay at `97be68d` in [CI 38207924264](https://github.com/bosatsu25/BlockLens/actions/runs/38207924264) rejected the raw RGB-change count (52 against greater than80). Independently checked native images pass both primary wood color/grain checks: warmth drops5.14/3.40 and spatial correlations0.9994/0.9996. The former combined error message incorrectly implicated those wood checks; separate assertions now identify the failing signal accurately. Texture provenance, saved configuration and both wood metrics remain required.

Shader ON base-palette captures now use the existing typed native render-target route with40 completed frames after framebuffer resize; non-Shader capture behavior is preserved. The world fixture adds actual vanilla pale-oak end-grain blocks in the gap and above the original boards. This supplies a stronger color reference using an already-verified palette role, without moving the original plank/stripped-log measurement areas. No copied artwork, randomized motion, retries, shader-setting change, RGB threshold change or tolerance increase is introduced. The raw count still uses x220..410/y180..255, RGB distance15 and more than80 pixels. Both original wood regions and their chroma, variance, correlation and restoration thresholds remain unchanged.

An intermediate expanded-region experiment completed the full26.3 static Shader graph in12m38s. It is experimental evidence, not final-source acceptance: its wider raw-count region has been reverted. Independently measuring the same native pair in the original region yielded204 changed pixels. Require a fresh final-source native graph and all three normal targets, followed by exact-head hosted CI. Input-pack material hashes remain private.

## Vulkan world-close diagnosis

The same `97be68d` replay completed palette/object, compact and simplified native Vulkan checks on26.3, then timed out while disconnecting the fixture world. Its SHA256/CRC-verified artifact contains147 partial diagnostic files and no passed external manifest. No root cause or repair is inferred from those completed subgraphs. The existing test-only bounded lifecycle observer is now armed for the world_close phase in Vulkan profiles only. Long visual scenes cannot consume its single snapshot before closure. A regression requires no snapshot during world_ready and one at a stalled world_close. It retains16 selected threads/24 stack frames/64KiB,60-second delay, daemon cancellation and no joins/interrupts/forced exits. No test outcome, visual threshold or CI deadline changes. Fresh final-head Vulkan CI is required; a passing replay does not establish that the earlier intermittent stall is repaired.

Fresh final-source normal palette/object clients pass on26.1.2/26.2/26.3 in2m58s/2m58s/2m49s and exit normally. Each preserves the six base-palette images and30 boat/sign/editor images with bounded manifests and checksums. All original RGB/color/grain/restoration conditions pass. Shared JUnit367 tests, selected JaCoCo/PIT (610 generated/592 killed),115 Python contracts and all three Java25 builds/GameTest compilations pass. Final-source full static Shader verification is continuing serially; final-head hosted normal/external/Vulkan/BBE CI remains the publication gate.
