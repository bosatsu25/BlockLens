# Current M8 diagnostic contract

This test-only contract covers Minecraft 26.1.2, 26.2 and 26.3. The original measurements in [m8-performance.md](m8-performance.md) remain historical evidence. Issue #43 records a 26.1.2 allocation guard failure and successful retry on the same source; the old failed run did not preserve its raw samples or actual frame counts. Its cause cannot be established retrospectively from medians alone.

## Failure evidence

The real-client oracle publishes `m8-performance-baseline.txt` before checking the unchanged coarse limits: 6.0 s reload median, 2.5 s rebuild median and 32 MiB allocation median. It records all three raw OFF/default/all-on samples, medians, reload retention, timing percentiles and separate passed/failed reload, rebuild and allocation statuses. A failing guard still propagates and fails the test. CI's existing failure collector retains this controlled directory; retries and larger thresholds are not a remedy.

Every client run also deliberately throws a synthetic allocation assertion after saving a separate `m8-failure-order.txt`. It catches only that exact fixture assertion and checks its sample survived. Actual measurement assertions propagate normally. JUnit covers failing guards, successful validation seeing already published evidence and IO failure preventing validation. The helper is compiled exclusively into common tests and version GameTests, not runtime/source JARs.

The stored environment fields are bounded OS family, Java major version and VSync boolean. No paths, usernames, IDs, world data, hardware identifiers or thread names are written into this manifest. JVM thread names are used internally to select render-relevant totals and are not emitted.

## Measurement boundaries

Allocation and rebuild samples observe a 30-tick window after terrain invalidation. They measure live-thread cumulative allocation deltas for the entire client JVM and a render-relevant subset, not bytes attributable solely to BlockLens. Terminated threads may be missed. The window has a varying number of main render passes; each raw sample now records its actual rendered-frame count. Counting uses a primitive counter even when timing capture is disabled, without boxed timing samples during allocation measurement.

Main-pass timing is measured separately for 80 ticks. At most 1,024 timing samples are retained per capture. Percentiles describe those sampled main passes, not complete frame presentation or FPS. A requested FPS limit is an upper bound; VSync, focus, backend and scheduling may yield fewer actual rendered frames. Do not substitute the configured upper bound for measured frame counts.

Minecraft's AFK limiter reduces the requested bound to at most 30 after 60 seconds without input. The test's earlier UI interactions can be more than a minute before its M8 phase, so merely changing the FPS option does not control the effective limit. Each allocation/rebuild or timing window now resets the isolated client's AFK timer through the tracker bookkeeping API and asserts that its throttle reason is NONE and its effective configured limit matches the option. This synthesizes no keyboard, mouse, world or server action. The window must remain visible; minimization fails the measurement-condition check. VSync/backend scheduling may still reduce actual rendered frames, which are recorded.

The isolated test client requests an upper bound of 60 by default. It restores the original option in `finally` and never saves options. Product clients are unaffected. To vary this single condition reproducibly, configure Gradle 9.5.1 and Java 25, then run each version module with one of the accepted test-only bounds:

```sh
gradle --no-daemon --no-parallel -Pm8FpsLimit=30 :versions:mc26_1_2:runClientGameTest
gradle --no-daemon --no-parallel -Pm8FpsLimit=60 :versions:mc26_2:runClientGameTest
gradle --no-daemon --no-parallel -Pm8FpsLimit=120 :versions:mc26_3:runClientGameTest
```

Use the same bound on all three modules when comparing them. Preserve each manifest before the next test run cleans its isolated game directory. Run clients sequentially, keep the same window/focus/VSync conditions, and avoid concurrent builds when collecting comparable final samples. Report raw values and observed variation; these diagnostics establish no percentage speedup or universal timing guarantee.

## Verified observations — 2026-10-10

Each row has three repeated samples per OFF/default/all-on scenario. Windows clients ran sequentially with VSync enabled. The final 30-bound runs and [CI #317](https://github.com/bosatsu25/BlockLens/actions/runs/38015239232) used `7be715ce3b4734d6be841806f733f1901bc38bee`; the Windows 60-bound measurements used the same AFK-controlled measurement routine (the first run preceded a diagnostic stdout-only addition). All rows passed the unchanged guards. These are observations of this scene/environment, not transferable performance guarantees.

| Environment | Minecraft | Requested upper bound | Rendered frames: OFF / default / all-on | Allocation medians B: OFF / default / all-on |
| --- | --- | ---: | --- | --- |
| Windows | 26.1.2 | 30 | 44,45,45 / 45,45,45 / 45,45,44 | 12687672 / 12442968 / 12816856 |
| Windows | 26.1.2 | 60 | 87,90,90 / 90,90,90 / 89,89,89 | 16081720 / 16496336 / 16423928 |
| Linux CI317 | 26.1.2 | 60 | 81,81,82 / 79,84,82 / 81,84,85 | 16223768 / 16625520 / 16469104 |
| Windows | 26.2 | 30 | 46,45,45 / 45,45,45 / 45,44,45 | 12224728 / 12500360 / 12070384 |
| Windows | 26.2 | 60 | 88,90,90 / 90,90,90 / 89,89,89 | 16060152 / 15743136 / 15625152 |
| Linux CI317 | 26.2 | 60 | 74,78,78 / 76,75,72 / 75,76,79 | 14710064 / 14175064 / 14784344 |
| Windows | 26.3 | 30 | 45,45,45 / 45,45,45 / 44,45,45 | 10495896 / 10580248 / 10549744 |
| Windows | 26.3 | 60 | 88,89,89 / 90,90,90 / 90,90,90 | 10744704 / 11312008 / 10979760 |
| Linux CI317 | 26.3 | 60 | 85,84,85 / 82,83,86 / 81,81,87 | 11868728 / 12192032 / 11949456 |

The 81 controlled allocation raw samples span **8,737,552–17,283,256 B**, below the unchanged 33,554,432 B median guard. Raw reload times span **2.920–3.448 s**; raw rebuild windows span **1.452–1.532 s**. These observed ranges describe the current repeated captures; they do not define new thresholds or statistical confidence intervals. ON-minus-OFF changes sign between rows, so these numbers support no runtime improvement claim.

The pre-control [CI #316](https://github.com/bosatsu25/BlockLens/actions/runs/38014738138) requested 60 but its 26.1.2 manifest recorded approximately 45 frames per window and allocation medians 28,663,856 / 28,619,056 / 28,629,896 B. Its artifact SHA-256 `0f69f406fdf897127243a423dd551f019fc6edf5d396a33b0de6a6e82cf5b2ba` was verified. The AFK-controlled CI317 records about 79–85 frames on that version while allocating less. This demonstrates both that the requested option alone did not control rendered work and that frame count alone does not explain cross-run allocation noise. It does not establish the missing conditions in the original CI312 failure, whose retrospective cause remains unknown.

CI317's common quality gate and all three native-client/build/reproducibility/privacy/size jobs passed on the exact source head without retries. Its three M8 ZIP digests were verified directly and contain passed reload/rebuild/allocation statuses, AFK control, Linux/Java25/VSync conditions and all raw arrays. Synthetic guard-failure evidence is verified inside every client before measurement. Product runtime artifacts remain unchanged in size: locally 112,628 / 113,335 / 113,379 B for the three versions, with test diagnostics explicitly rejected by the artifact audit.

No measurement limit or runtime feature has been changed by this diagnostic implementation. The original CI312 cause remains an evidence limitation, not a claimed fix.

The nine controlled observations above belong to the 40-capability PR #45 workload. Issue #42 extends the configured all-on workload to 46 and adds representative glass/pane, kelp, white-concrete and regular-chest targets. Its measurements must be recorded separately; the historical observations do not establish performance for the added scope.

## Recurring main CI321 allocation failure and phase attribution

PR46 CI320 passed on d4f313491ada47788bc46459e4f34a8f8396ef5a, then main CI321 failed the 26.1.2 allocation guard on the identical merged tree (5f992800cde79236d2d47fea304dbfe5831307dc). The verified failure ZIP digest is f1eb14bb0eb29ea5c06eda4505c868e791ef8082d91a30d68223d9aaedc491d8. OFF/default/all-ON medians were 41,098,872 / 40,000,576 / 40,168,632 B; 74–82 frames were rendered. The passing PR observation rendered 85–90 frames while allocating less. Reload and rebuild guards passed. Frame counts and OFF/ON medians alone do not identify a root cause; #43 was reopened for that recurrence.

The follow-up records five fixed live-thread roles: render, chunk, worker, test and other. The first three partition the existing render-relevant predicate. Names and IDs are used transiently to match counters and classify roles; only the five role numbers and three samples per scenario enter the primary manifest. More than 512 live threads fails the test instead of truncating coverage. Existing negative/decreasing counters are excluded; new threads use a zero baseline, and terminated threads can still be missed.

A second primitive probe samples current-thread allocated bytes at START_MAIN and END_MAIN and counts complete main passes in each capture. It starts after the before-snapshot and ends before the after-snapshot. It measures the enclosing Java rendering phase, including observer overhead, rather than classes, methods attributable exclusively to BlockLens, native/GPU allocations or retained heap. The normal time/total-allocation guards remain unchanged. Both probes stop in finally. The same primary manifest contains these numbers before the guards; no separate diagnostic write can prevent primary evidence publication.

No raw JFR recording, event stack, thread name, ID, path or runtime class identity is written by this implementation. A RecordingStream prototype was rejected by independent review because it creates a disk repository; it is absent from the patch. The attribution implementation subsequently passed the shared and all-three-version gates in PR #47's [CI #322](https://github.com/bosatsu25/BlockLens/actions/runs/38024132708) and merged-main [CI #323](https://github.com/bosatsu25/BlockLens/actions/runs/38024826549). These changes do not claim to have fixed the original or recurring allocation cause.

## Current conformance and completion boundary

[performance-strategy.md](performance-strategy.md) records the later compiler-attributed recurrence, 18 controlled compiler/FPS/escape-analysis observations and the explicit test-only C2/foreground conformance condition. Historical passing measurements above do not replace that condition or the current full-client graph.

[Issue #43](https://github.com/bosatsu25/BlockLens/issues/43) retains the failures, final PR-head and merged-main verification records, and current completion state. Closure requires the documented measurement boundary or justified environment exception plus shared and all-three-version full native gates; it does not establish the exact historical allocating method or a performance improvement in ordinary clients.
