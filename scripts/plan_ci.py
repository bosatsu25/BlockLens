#!/usr/bin/env python3
"""Decide how much CI a change needs and which version a main merge publishes.

Pure decision logic; the workflow gathers the inputs (changed paths, tags, verification of the
merged pull request) with the GitHub API and passes them here, so the rules are unit-tested.
"""
import argparse
import re
import sys
from pathlib import Path

# Paths that cannot change the runtime JAR or any client behavior. The common quality gate still
# runs for them, because repository contracts read README and knowledge/current.
DOCS_PREFIXES = ("knowledge/",)
DOCS_SUFFIXES = (".md",)
DOCS_NAMES = ("LICENSE",)
# The compare API truncates at this many files; a truncated list is never treated as docs-only.
COMPARE_FILE_LIMIT = 300
VERSION = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")


def docs_only(paths):
    paths = [path for path in paths if path]
    if not paths or len(paths) >= COMPARE_FILE_LIMIT:
        return False
    return all(path.startswith(DOCS_PREFIXES) or path.endswith(DOCS_SUFFIXES)
               or path.split("/")[-1].startswith(DOCS_NAMES) for path in paths)


def next_version(base, tags):
    """The first free patch version on the base line, never below base."""
    match = VERSION.match(base)
    if not match:
        raise ValueError(f"mod_version must be MAJOR.MINOR.PATCH: {base!r}")
    major, minor, patch = (int(part) for part in match.groups())
    used = []
    for tag in tags:
        found = VERSION.match(tag[1:] if tag.startswith("v") else tag)
        if found and int(found.group(1)) == major and int(found.group(2)) == minor:
            used.append(int(found.group(3)))
    return f"{major}.{minor}.{max([patch] + [value + 1 for value in used])}"


def plan(event, ref, draft, paths, pre_verified, base, tags, released=False):
    """Returns (full, release, version) for one CI run.

    For a main push, paths are the changes since the latest release, not since the previous push, so
    a merge whose run was replaced while pending is published by the next run; released means the
    pushed commit is already part of the latest release (for example a re-run after publishing).
    """
    on_main_push = event == "push" and ref == "refs/heads/main"
    documentation = docs_only(paths)
    if event == "workflow_dispatch":
        full = True
    elif event == "pull_request":
        full = not draft and not documentation
    elif on_main_push:
        full = not documentation and not pre_verified and not released
    else:
        full = True
    release = on_main_push and not documentation and not released
    return full, release, next_version(base, tags) if release else ""


def read_lines(path):
    if not path:
        return []
    return [line.strip() for line in Path(path).read_text(encoding="utf-8").splitlines() if line.strip()]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--event", required=True)
    parser.add_argument("--ref", required=True)
    parser.add_argument("--draft", default="false")
    parser.add_argument("--paths-file", help="one changed path per line (main push: since the latest release)")
    parser.add_argument("--pre-verified", default="false")
    parser.add_argument("--base-version", required=True)
    parser.add_argument("--tags-file", help="one tag per line")
    parser.add_argument("--released", default="false", help="the pushed commit is in the latest release")
    args = parser.parse_args()
    full, release, version = plan(args.event, args.ref, args.draft == "true", read_lines(args.paths_file),
                                  args.pre_verified == "true", args.base_version, read_lines(args.tags_file),
                                  args.released == "true")
    print(f"full={'true' if full else 'false'}")
    print(f"release={'true' if release else 'false'}")
    print(f"version={version}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
