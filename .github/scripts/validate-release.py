"""Validate the checked-out public release before running the Halo workflows."""
import os
import re
import subprocess
from pathlib import Path


def validate(tag: str, version: str) -> None:
    # Keep tags safe for the upstream shell-based build/upload steps.
    if not re.fullmatch(r"v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?", tag):
        raise ValueError("Expected a vMAJOR.MINOR.PATCH tag, optionally with a prerelease suffix")
    if tag[1:] != version:
        raise ValueError(f"Release tag {tag} does not match gradle.properties version {version}")


def main() -> None:
    tag = os.environ["RELEASE_TAG"]
    versions = re.findall(r"^version\s*=\s*(\S+)\s*$", Path("gradle.properties").read_text(), re.MULTILINE)
    if len(versions) != 1:
        raise ValueError("gradle.properties must define exactly one version")
    validate(tag, versions[0])
    subprocess.run(["git", "fetch", "--no-tags", "origin", "+refs/heads/master:refs/remotes/origin/master"], check=True)
    subprocess.run(["git", "merge-base", "--is-ancestor", "HEAD", "refs/remotes/origin/master"], check=True)
    notes = Path("docs/releases") / f"{tag}.md"
    if not notes.is_file() or not notes.read_text().strip():
        raise ValueError(f"Missing release notes: {notes}")
    print(f"Validated {tag}: version matches, commit belongs to master, release notes exist")


if __name__ == "__main__":
    main()
