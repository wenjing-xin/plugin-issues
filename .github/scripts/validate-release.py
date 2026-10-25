"""Validate the checked-out public release before running the Halo workflows."""
import os
import re
import subprocess
from pathlib import Path


def validate(tag: str, version: str) -> str:
    # Keep tags safe for the upstream shell-based build/upload steps.
    if not re.fullmatch(r"v?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?", tag):
        raise ValueError("Expected MAJOR.MINOR.PATCH or vMAJOR.MINOR.PATCH, optionally with a prerelease suffix")
    normalized = tag.removeprefix("v")
    if normalized != version:
        raise ValueError(f"Release tag {tag} does not match gradle.properties version {version}")
    return normalized


def find_release_notes(version: str) -> Path:
    directory = Path("docs/releases")
    candidates = [directory / f"v{version}.md", directory / f"{version}.md"]
    for path in candidates:
        if path.is_file() and path.read_text(encoding="utf-8").strip():
            return path
    raise ValueError(f"Missing non-empty release notes: {candidates[0]} or {candidates[1]}")


def main() -> None:
    tag = os.environ["RELEASE_TAG"]
    versions = re.findall(r"^version\s*=\s*(\S+)\s*$", Path("gradle.properties").read_text(), re.MULTILINE)
    if len(versions) != 1:
        raise ValueError("gradle.properties must define exactly one version")
    version = validate(tag, versions[0])
    subprocess.run(["git", "fetch", "--no-tags", "origin", "+refs/heads/master:refs/remotes/origin/master"], check=True)
    subprocess.run(["git", "merge-base", "--is-ancestor", "HEAD", "refs/remotes/origin/master"], check=True)
    notes = find_release_notes(version)
    print(f"Validated {tag}: version matches, commit belongs to master, release notes: {notes}")


if __name__ == "__main__":
    main()
