#!/usr/bin/env bash
# Runs one Gradle command and, only when it failed in dependency resolution, runs it once more with
# --refresh-dependencies after a pause. Maven Central has answered 404 for valid artifacts to single
# runners while sibling jobs resolved the same modules (knowledge/current/ci-release.md); Gradle
# reports those as "Could not find" and keeps the negative result in the restored cache, so the
# second try must bypass it. Any other failure, and a second resolution failure, end the step as
# the Gradle exit code did.
#
# Usage: scripts/ci_gradle.sh [wrapper ...] gradle <arguments>
set -uo pipefail

pause="${CI_GRADLE_RETRY_PAUSE_SECONDS:-60}"
log="$(mktemp)"
trap 'rm -f "$log"' EXIT

"$@" 2>&1 | tee "$log"
status="${PIPESTATUS[0]}"
if [[ "$status" -eq 0 ]]; then
  exit 0
fi
if ! grep -Eq 'Could not (find|resolve|GET|HEAD|download) [^ ]|Could not resolve all' "$log"; then
  exit "$status"
fi

command=()
inserted=0
for argument in "$@"; do
  command+=("$argument")
  if [[ "$inserted" -eq 0 && "$argument" == "gradle" ]]; then
    command+=("--refresh-dependencies")
    inserted=1
  fi
done
if [[ "$inserted" -eq 0 ]]; then
  exit "$status"
fi
echo "::warning::Gradle failed in dependency resolution (exit $status); retrying once with --refresh-dependencies after ${pause}s"
sleep "$pause"
exec "${command[@]}"
