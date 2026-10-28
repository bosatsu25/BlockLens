"""Keeps the Gradle cache small enough to survive GitHub's per-repository limit, and downloads patient.

Every job used to save its own ~1 GB Gradle User Home, about 28 GB per full run against a 10 GB
limit, so the shared dependency entry was evicted and each job downloaded everything again
(knowledge/current/ci-release.md).
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github" / "workflows"
# Jobs that build every Minecraft target; only they save the cache.
WRITERS = {("ci.yml", "version-build"), ("ci.yml", "release-build")}
# Gradle 9.5.1 defaults: 30 s connect/read timeouts, 3 tentatives, 1 s initial backoff.
NETWORK_MINIMUMS = {
    "org.gradle.internal.http.connectionTimeout": 30000,
    "org.gradle.internal.http.socketTimeout": 30000,
    "org.gradle.internal.repository.max.tentatives": 3,
    "org.gradle.internal.repository.initial.backoff": 1000,
}


def setup_gradle_steps():
    """Yields (workflow, job, with-block lines) for every setup-gradle step."""
    for path in sorted(WORKFLOWS.glob("*.yml")):
        job = None
        lines = path.read_text(encoding="utf-8").splitlines()
        for index, line in enumerate(lines):
            found = re.match(r"^  ([a-z][a-z0-9_-]*):$", line)
            if found and not line.startswith("   "):
                job = found.group(1)
            if "uses: gradle/actions/setup-gradle@" in line:
                block = []
                for following in lines[index + 1:]:
                    if re.match(r"^\s*- ", following) or re.match(r"^  \S", following):
                        break
                    block.append(following.strip())
                yield path.name, job, block


class GradleCacheWritersTest(unittest.TestCase):
    def test_only_the_build_jobs_save_the_gradle_cache(self):
        steps = list(setup_gradle_steps())
        self.assertGreaterEqual(len(steps), 7, "every Gradle job must be inspected")
        seen = set()
        for workflow, job, block in steps:
            seen.add((workflow, job))
            read_only = [line for line in block if line.startswith("cache-read-only:")]
            if (workflow, job) in WRITERS:
                self.assertNotIn("cache-read-only: true", read_only, f"{workflow}:{job} must write")
            else:
                self.assertEqual(["cache-read-only: true"], read_only, f"{workflow}:{job} must only read")
            self.assertFalse(any(line.startswith("cache-write-only:") for line in block), f"{workflow}:{job}")
        self.assertTrue(WRITERS <= seen, "a cache writer was renamed or removed")


class GradleNetworkSettingsTest(unittest.TestCase):
    def test_downloads_are_more_patient_than_gradle_defaults(self):
        properties = {}
        for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
            if line.startswith("systemProp.") and "=" in line:
                key, value = line[len("systemProp."):].split("=", 1)
                properties[key.strip()] = int(value.strip())
        for key, default in NETWORK_MINIMUMS.items():
            self.assertIn(key, properties)
            self.assertGreater(properties[key], default, key)
        self.assertLessEqual(properties["org.gradle.internal.http.socketTimeout"], 120000,
                             "a dead host must still fail a job well inside its timeout")


if __name__ == "__main__":
    unittest.main()
