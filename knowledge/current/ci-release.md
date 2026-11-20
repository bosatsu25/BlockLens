# CI scope and automatic release

Status: current (2026-10-27; Gradle cache 2026-10-28; resolution retry 2026-11-20). Supersedes the `workflow_run` Release workflow described in
`release-readiness.md`, which skipped every merge after v0.4.7 because the version never changed.

## Verification scope

`plan / verification scope` (`scripts/plan_ci.py`, unit-tested by `scripts/test_plan_ci.py`) decides
per run:

| Event | Client matrices | Release |
| --- | --- | --- |
| Draft pull request | skipped | no |
| Documentation-only change (`*.md`, `knowledge/**`, `LICENSE*`) | skipped | no |
| Ready pull request, `ready_for_review`, manual run | full | no |
| Push to main whose tree equals a merged PR head with a successful `verified / full matrix` | reused, not repeated | yes |
| Any other push to main | full | yes, after `verified / full matrix` |
| Push to main already contained in the latest release (for example a re-run) | skipped | no |

For a push to main, "documentation-only" is judged on the changes since the latest release, not
since the previous push. Main runs are serialized (`blocklens-ci-refs/heads/main`), and GitHub
replaces an older pending run with a newer one; the newer run still publishes the replaced merge's
code because it compares against the release. Manual runs use their own group and never replace a
pending push.

The common gate (JUnit, JaCoCo, PIT, script tests, clean-room asset checks) always runs, because
repository contracts read README and `knowledge/current`. Superseded pull request runs are
cancelled; a main run is never cancelled while it may publish.

`verified / full matrix` succeeds only when the common gate, the three client GameTest jobs and the
shader/Vulkan, BBE/Cele, connected-ore and native connected-ore matrices all succeed. The check run
on a pull request head is what a later main merge of the same tree reuses.

## Gradle cache and dependency downloads

Every Gradle job uses one shared Gradle cache entry from setup-gradle's basic provider: `~/.gradle/caches`
and `~/.gradle/wrapper`, keyed only by the hash of the build files (`**/*.gradle*`), with no restore
keys.

- `common-quality` is the only job that saves it (`cache-read-only: false`). Every other Gradle job in
  `ci.yml` needs `common-quality`, so it starts after the entry exists and restores it with
  `cache-read-only: true`. Every Gradle invocation first resolves the Fabric Loom plugin classpath,
  including `mercury`, where every observed timeout occurred, and the common gate also configures the
  three version modules, so the entry holds what each job used to download.
- An unchanged build restores the entry, so no job downloads again what it holds. A build-file
  change starts one new entry, which only `common-quality` downloads. A dependency bumped
  only in `gradle.properties` keeps the old entry, and each job downloads the new artifact itself.
- Gradle's default download timeouts stay. Longer ones (60 s, 5 tentatives) were tried and did not
  prevent the same failure; it only arrived after 21 minutes instead of 7–17.
- Every Gradle command in the workflows runs through `scripts/ci_gradle.sh`. When Gradle fails with
  `Could not find`, `Could not resolve`, `Could not GET/HEAD` or `Could not download`, the script waits
  60 s and runs the same command once more with `--refresh-dependencies`; any other failure, and a
  second resolution failure, end the step with Gradle's exit code. Why (2026-11-20): Maven Central
  answered 404 for valid artifacts (the Minecraft libraries' parent POMs, `asm-analysis`, and on a cold
  main gate the Loom plugin's own dependencies) to single runners, within a second and for the whole
  batch, while sibling jobs restoring the same cache entry resolved the same modules in the same
  minutes. Gradle records such a 404 as a missing module in the cache, so the retry has to bypass it.
- `common-quality` saves with `cache-overwrite-existing: true`. A gate that failed in resolution still
  saves its entry, which then holds almost nothing; the next green gate replaces it, instead of every
  job downloading everything from `maven.fabricmc.net` until the build files change.
- `scripts/test_ci_gradle_cache.py` fails if a job leaves the basic provider, if another job saves or
  overwrites, if a Gradle job stops needing `common-quality`, or if a Gradle command bypasses the
  wrapper; it also runs the wrapper against a fake `gradle` for the four outcomes.

Why (2026-10-28): with the enhanced provider every job saved its own Gradle User Home entry of about
1 GB, keyed by job, matrix and commit, so one full run wrote about 28 GB against GitHub's 10 GB
per-repository limit. Least-recently-used eviction removed the shared dependency entries. A job made
read-only still restored its own older entry first, whose dependencies were gone. Jobs then fetched
everything from `maven.fabricmc.net`, where `mercury-0.4.3.jar` timed out before any test ran in at
least nine jobs.

## Release

- Version: the first free patch on the `mod_version` line, never below `mod_version`
  (v0.4.8 after v0.4.7). Raising `mod_version` starts a new line.
- `release-build` rebuilds each Minecraft target with `-Pmod_version=<version>` and runs
  `versionSmokeContract` and `verifyRuntimeJarBudget`. The deterministic build keeps classes
  byte-identical to the verified tree; only version metadata differs. This replaces the earlier rule
  of promoting the exact CI bytes, which could not carry a new version.
- `release` revalidates baseline, release and hard size budgets, `fabric.mod.json` version/Minecraft/
  environment and the packaged icon, writes `SHA256SUMS.txt`, refuses an existing tag, publishes
  with generated notes, and checks the exact asset set.
- Only the latest release keeps JARs: after publishing, JARs and `SHA256SUMS.txt` are removed from
  every older release. Tags and release notes stay.
- Only the `release` job has `contents: write`.
