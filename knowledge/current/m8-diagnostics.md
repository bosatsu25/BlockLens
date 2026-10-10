# Current M8 diagnostic contract

This test-only contract covers Minecraft 26.1.2, 26.2 and 26.3. The original measurements in [m8-performance.md](m8-performance.md) remain historical evidence. Issue #43 records a 26.1.2 allocation guard failure and successful retry on the same source; the old failed run did not preserve its raw samples or actual frame counts. Its cause cannot be established retrospectively from medians alone.

## Failure evidence

The real-client oracle publishes `m8-performance-baseline.txt` before checking the unchanged coarse limits: 6.0 s reload median, 2.5 s rebuild median and 32 MiB allocation median. It records all three raw OFF/default/all-on samples, medians, reload retention, timing percentiles and separate passed/failed reload, rebuild and allocation statuses. A failing guard still propagates and fails the test. CI's existing failure collector retains this controlled directory; retries and larger thresholds are not a remedy.

Every client run also deliberately throws a synthetic allocation assertion after saving a separate `m8-failure-order.txt`. It catches only that exact fixture assertion and checks its sample survived. Actual measurement assertions propagate normally. JUnit covers failing guards, successful validation seeing already published evidence and IO failure preventing validation. The helper is compiled exclusively into common tests and version GameTests, not runtime/source JARs.

The stored environment fields are bounded OS family, Java major version and VSync boolean. No paths, usernames, IDs, world data, hardware identifiers or thread names are written into this manifest. JVM thread names are used internally to select render-relevant totals and are not emitted.

## Measurement boundaries

Allocation and rebuild samples observe a 30-tick window after terrain invalidation. They measure live-thread cumulative allocation deltas for the entire client JVM and a render-relevant subset, not bytes attributable solely to BlockLens. Terminated threads may be missed. The window has a varying number of main render passes; each raw sample now records its actual rendered-frame count. Counting uses a primitive counter even when timing capture is disabled, without boxed timing samples during allocation measurement.

Main-pass timing is measured separately for 80 ticks. At most 1,024 timing samples are retained per capture. Percentiles describe those sampled main passes, not complete frame presentation or FPS. A requested FPS limit is an upper bound; VSync, focus, backend and scheduling may yield fewer actual rendered frames. Do not substitute the configured upper bound for measured frame counts.

The isolated test client requests an upper bound of 60 by default. It restores the original option in `finally` and never saves options. Product clients are unaffected. To vary this single condition reproducibly, configure Gradle 9.5.1 and Java 25, then run each version module with one of the accepted test-only bounds:

```sh
gradle --no-daemon --no-parallel -Pm8FpsLimit=30 :versions:mc26_1_2:runClientGameTest
gradle --no-daemon --no-parallel -Pm8FpsLimit=60 :versions:mc26_2:runClientGameTest
gradle --no-daemon --no-parallel -Pm8FpsLimit=120 :versions:mc26_3:runClientGameTest
```

Use the same bound on all three modules when comparing them. Preserve each manifest before the next test run cleans its isolated game directory. Run clients sequentially, keep the same window/focus/VSync conditions, and avoid concurrent builds when collecting comparable final samples. Report raw values and observed variation; these diagnostics establish no percentage speedup or universal timing guarantee.

Controlled 30/60-bound observations and current-head hosted verification will be recorded here before #43 is closed. No measurement limit or runtime feature has been changed by the diagnostic implementation.
