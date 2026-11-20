"""Keeps every Gradle job on one shared cache entry that already holds the Fabric build dependencies.

With setup-gradle's enhanced provider every job saved its own ~1 GB Gradle User Home, about 28 GB per
full run against GitHub's 10 GB limit, so the shared dependency entry was evicted, and a read-only job
still restored its own stale entry first. Jobs then downloaded everything from maven.fabricmc.net,
where mercury-0.4.3.jar kept timing out (knowledge/current/ci-release.md).
"""
import os
import re
import subprocess
import tempfile
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
            # Only the writer replaces its entry, so a gate that failed in dependency resolution and
            # saved a near-empty entry is healed by the next green gate instead of staying until the
            # build files change.
            overwrite = [line for line in block if line.startswith("cache-overwrite-existing:")]
            self.assertEqual(["cache-overwrite-existing: true"] if (workflow, job) == WRITER else [],
                             overwrite, f"{workflow}:{job}")

    def test_every_gradle_invocation_retries_dependency_resolution_once(self):
        # Maven Central answered 404 for valid artifacts to single runners while sibling jobs resolved
        # the same modules; scripts/ci_gradle.sh retries that one failure class with
        # --refresh-dependencies and nothing else.
        invocations = []
        for path in sorted(WORKFLOWS.glob("*.yml")):
            for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                code = line.split("#", 1)[0]
                if re.search(r"(^|\s)(xvfb-run -a )?gradle\s", code):
                    invocations.append((path.name, number, code.strip()))
                    self.assertIn("scripts/ci_gradle.sh ", code, f"{path.name}:{number}: {code.strip()}")
                    self.assertRegex(code, r"scripts/ci_gradle\.sh (xvfb-run -a )?gradle\s", f"{path.name}:{number}")
        self.assertGreaterEqual(len(invocations), 10, invocations)

    def test_every_gradle_job_starts_after_the_writer_saved(self):
        jobs = ci_jobs()
        gradle_jobs = {"version-build", "release-build", "external-compatibility", "bbe-compatibility",
                       "connected-compatibility", "connected-native", "pack-cost"}
        self.assertTrue(gradle_jobs <= set(jobs), "a Gradle job was renamed")
        for job in gradle_jobs:
            self.assertIn(WRITER[1], jobs[job], job)

    def test_wrapper_retries_only_a_resolution_failure_and_only_once(self):
        with tempfile.TemporaryDirectory() as directory:
            fake = Path(directory) / "gradle"
            calls = Path(directory) / "calls"
            fake.write_text(
                "#!/usr/bin/env bash\n"
                f"echo \"$*\" >> '{calls}'\n"
                "case \"$FAKE_GRADLE\" in\n"
                "  ok) echo BUILD SUCCESSFUL; exit 0;;\n"
                "  resolution) for a in \"$@\"; do [ \"$a\" = --refresh-dependencies ] && exit 0; done;\n"
                "    echo '   > Could not find com.google.code.gson:gson:2.14.0.' >&2; exit 1;;\n"
                "  resolution-twice) echo 'Could not resolve all artifacts for configuration classpath.'; exit 1;;\n"
                "  compile) echo 'error: cannot find symbol'; exit 3;;\n"
                "esac\n", encoding="utf-8")
            fake.chmod(0o755)
            env = dict(os.environ, PATH=f"{directory}:{os.environ['PATH']}", CI_GRADLE_RETRY_PAUSE_SECONDS="0")

            def run(mode, *command):
                calls.write_text("", encoding="utf-8")
                result = subprocess.run([str(ROOT / "scripts" / "ci_gradle.sh"), *command],
                                        env=dict(env, FAKE_GRADLE=mode), capture_output=True, text=True)
                return result.returncode, calls.read_text(encoding="utf-8").splitlines(), result.stdout

            code, calls_made, _ = run("ok", "gradle", "--stacktrace", ":common:qualityGate")
            self.assertEqual((0, ["--stacktrace :common:qualityGate"]), (code, calls_made))

            code, calls_made, output = run("resolution", "gradle", "--stacktrace", ":common:qualityGate")
            self.assertEqual(0, code)
            self.assertEqual(["--stacktrace :common:qualityGate",
                              "--refresh-dependencies --stacktrace :common:qualityGate"], calls_made)
            self.assertIn("::warning::", output)

            code, calls_made, _ = run("resolution", "env", "DISPLAY=:1", "gradle", "-Pflag=true", ":x:runClientGameTest")
            self.assertEqual(0, code, "the flag goes after the gradle token, not after the wrapper")
            self.assertEqual(["-Pflag=true :x:runClientGameTest",
                              "--refresh-dependencies -Pflag=true :x:runClientGameTest"], calls_made)

            code, calls_made, _ = run("resolution-twice", "gradle", ":x")
            self.assertEqual((1, 2), (code, len(calls_made)), "a second resolution failure is final")

            code, calls_made, output = run("compile", "gradle", ":x")
            self.assertEqual((3, 1), (code, len(calls_made)), "any other failure keeps its exit code, no retry")
            self.assertNotIn("::warning::", output)

    def test_downloads_keep_gradle_default_timeouts(self):
        # Longer timeouts only delayed the same mercury-0.4.3.jar failure (21 min instead of 8).
        self.assertNotIn("org.gradle.internal.http", (ROOT / "gradle.properties").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
