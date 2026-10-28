# CI scope and automatic release

Status: current (2026-10-27; Gradle cache and download settings 2026-10-28). Supersedes the `workflow_run` Release workflow described in
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

GitHub keeps at most 10 GB of Actions cache per repository and evicts the least recently used
entries. Each job's Gradle User Home entry is about 1 GB, and setup-gradle keys it by job, matrix
and commit, so when every job saved, one full run wrote about 28 GB. The shared downloaded-dependency
entry was evicted, and every job fetched all Fabric artifacts from `maven.fabricmc.net` again; on
2026-10-28 reads of `mercury-0.4.3.jar` timed out in at least seven client jobs before any test ran.

- Only `version-build` (non-pull-request runs) and `release-build` (main) save the Gradle cache. Every
  other Gradle job, including the common gate and all compatibility, connected-ore and pack-cost
  matrices, sets `cache-read-only: true` and restores the most recent entry of the branch or main.
- `gradle.properties` raises Gradle's download patience: 60 s connect and read timeouts (default 30 s)
  and 5 tentatives from a 2 s backoff (default 3 from 1 s), after which Gradle disables the repository
  for the build. A host that does not answer still fails after about 5.5 minutes per artifact.
- `scripts/test_ci_gradle_cache.py` fails if another job starts saving the cache or a setting falls back
  to Gradle's default.

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
