"""Rules for CI scope and automatic release versions."""
import unittest

from plan_ci import COMPARE_FILE_LIMIT, docs_only, next_version, plan

CODE = ["common/src/main/java/dev/blocklens/core/BlockLensConfig.java", "README.md"]
DOCS = ["README.md", "README_ja.md", "knowledge/current/roadmap.md", "LICENSE"]
MAIN = "refs/heads/main"


class DocsOnlyTest(unittest.TestCase):
    def test_markdown_knowledge_and_license_are_documentation(self):
        self.assertTrue(docs_only(DOCS))

    def test_any_other_path_needs_verification(self):
        self.assertFalse(docs_only(CODE))
        self.assertFalse(docs_only(["README.md", ".github/workflows/ci.yml"]))
        self.assertFalse(docs_only(["gradle.properties"]))

    def test_empty_or_truncated_lists_are_never_documentation(self):
        self.assertFalse(docs_only([]))
        self.assertFalse(docs_only(["README.md"] * COMPARE_FILE_LIMIT))


class NextVersionTest(unittest.TestCase):
    def test_unused_base_is_published_as_is(self):
        self.assertEqual("0.5.0", next_version("0.5.0", ["v0.4.7", "v0.4.6"]))

    def test_taken_base_moves_to_the_next_free_patch(self):
        self.assertEqual("0.4.8", next_version("0.4.7", ["v0.4.7", "v0.4.6", "v0.3.0"]))
        self.assertEqual("0.4.10", next_version("0.4.7", ["v0.4.9", "v0.4.8", "v0.4.7"]))

    def test_never_publishes_below_base_or_reuses_a_tag(self):
        self.assertEqual("0.4.7", next_version("0.4.7", ["v0.4.2"]))
        self.assertEqual("0.4.8", next_version("0.4.3", ["v0.4.7"]))

    def test_other_lines_and_odd_tags_are_ignored(self):
        self.assertEqual("0.4.7", next_version("0.4.7", ["v1.4.9", "v0.3.9", "nightly", "v0.4.x"]))

    def test_base_must_be_a_plain_version(self):
        with self.assertRaises(ValueError):
            next_version("0.4", [])


class PlanTest(unittest.TestCase):
    def test_ready_pull_request_runs_everything_and_never_releases(self):
        self.assertEqual((True, False, ""), plan("pull_request", "refs/pull/9/merge", False, CODE, False, "0.4.7", []))

    def test_draft_and_documentation_pull_requests_skip_the_client_matrices(self):
        self.assertEqual((False, False, ""), plan("pull_request", "refs/pull/9/merge", True, CODE, False, "0.4.7", []))
        self.assertEqual((False, False, ""), plan("pull_request", "refs/pull/9/merge", False, DOCS, False, "0.4.7", []))

    def test_merge_of_a_verified_pull_request_only_builds_and_releases(self):
        self.assertEqual((False, True, "0.4.8"), plan("push", MAIN, False, CODE, True, "0.4.7", ["v0.4.7"]))

    def test_unverified_push_to_main_runs_everything_before_release(self):
        self.assertEqual((True, True, "0.4.8"), plan("push", MAIN, False, CODE, False, "0.4.7", ["v0.4.7"]))

    def test_documentation_merge_publishes_nothing(self):
        self.assertEqual((False, False, ""), plan("push", MAIN, False, DOCS, False, "0.4.7", ["v0.4.7"]))

    def test_manual_run_is_always_full_and_never_releases(self):
        self.assertEqual((True, False, ""), plan("workflow_dispatch", MAIN, False, [], False, "0.4.7", []))


if __name__ == "__main__":
    unittest.main()
