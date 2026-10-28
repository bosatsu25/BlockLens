"""Keeps every Gradle job on one shared cache entry that already holds the Fabric build dependencies.

With setup-gradle's enhanced provider every job saved its own ~1 GB Gradle User Home, about 28 GB per
full run against GitHub's 10 GB limit, so the shared dependency entry was evicted, and a read-only job
still restored its own stale entry first. Jobs then downloaded everything from maven.fabricmc.net,
where mercury-0.4.3.jar kept timing out (knowledge/current/ci-release.md).
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github" / "workflows"
# The common gate runs first and every other Gradle job in ci.yml needs it, so it alone saves.
WRITER = ("ci.yml", "common-quality")


def setup_gradle_steps():
    """Yields (workflow, job, with-block lines) for every setup-gradle step."""
    for path in sorted(WORKFLOWS.glob("*.yml")):
        job = None
        lines = path.read_text(encoding="utf-8").splitlines()
        for index, line in enumerate(lines):
            found = re.match(r"^  ([a-z][a-z0-9_-]*):$", line)
            if found:
                job = found.group(1)
            if "uses: gradle/actions/setup-gradle@" in line:
                block = []
                for following in lines[index + 1:]:
                    if re.match(r"^\s*- ", following) or re.match(r"^  \S", following):
                        break
                    if not following.strip().startswith("#"):
                        block.append(following.strip())
                yield path.name, job, block


def ci_jobs():
    """Maps each ci.yml job to its `needs` list."""
    jobs, current = {}, None
    for line in (WORKFLOWS / "ci.yml").read_text(encoding="utf-8").splitlines():
        found = re.match(r"^  ([a-z][a-z0-9_-]*):$", line)
        if found:
            current = found.group(1)
            jobs[current] = []
        needs = re.match(r"^    needs: \[([^\]]*)\]?", line)
        if current and needs:
            jobs[current] = [name.strip() for name in needs.group(1).split(",") if name.strip()]
    return jobs


class GradleCacheTest(unittest.TestCase):
    def test_every_gradle_job_shares_the_basic_cache_and_only_the_common_gate_saves(self):
        steps = list(setup_gradle_steps())
        self.assertGreaterEqual(len(steps), 7, "every Gradle job must be inspected")
        self.assertIn(WRITER, [(workflow, job) for workflow, job, _ in steps])
        for workflow, job, block in steps:
            self.assertIn("cache-provider: basic", block, f"{workflow}:{job}")
            expected = "cache-read-only: false" if (workflow, job) == WRITER else "cache-read-only: true"
            self.assertEqual([expected], [line for line in block if line.startswith("cache-read-only:")],
                             f"{workflow}:{job}")

    def test_every_gradle_job_starts_after_the_writer_saved(self):
        jobs = ci_jobs()
        gradle_jobs = {"version-build", "release-build", "external-compatibility", "bbe-compatibility",
                       "connected-compatibility", "connected-native", "pack-cost"}
        self.assertTrue(gradle_jobs <= set(jobs), "a Gradle job was renamed")
        for job in gradle_jobs:
            self.assertIn(WRITER[1], jobs[job], job)

    def test_downloads_keep_gradle_default_timeouts(self):
        # Longer timeouts only delayed the same mercury-0.4.3.jar failure (21 min instead of 8).
        self.assertNotIn("org.gradle.internal.http", (ROOT / "gradle.properties").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
