# CI scope and automatic release

Status: current (2026-10-27). Supersedes the `workflow_run` Release workflow described in
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

The common gate (JUnit, JaCoCo, PIT, script tests, clean-room asset checks) always runs, because
repository contracts read README and `knowledge/current`. Superseded pull request runs are
cancelled; a main run is never cancelled while it may publish.

`verified / full matrix` succeeds only when the common gate, the three client GameTest jobs and the
shader/Vulkan, BBE/Cele, connected-ore and native connected-ore matrices all succeed. The check run
on a pull request head is what a later main merge of the same tree reuses.

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
