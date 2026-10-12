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
