"""Release validation regression tests; no network or repository mutations."""
import importlib.util
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "validate_release", Path(__file__).with_name("validate-release.py")
)
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseValidationTest(unittest.TestCase):
    def test_versions_with_and_without_prefix(self):
        for version in ("1.0.0", "1.1.0-rc.1", "1.0.0-rc4"):
            for prefix in ("", "v"):
                with self.subTest(tag=prefix + version):
                    self.assertEqual(release.validate(prefix + version, version), version)

    def test_version_mismatch_still_fails(self):
        for tag in ("1.0.1", "v1.0.1"):
            with self.assertRaisesRegex(ValueError, "does not match"):
                release.validate(tag, "1.0.0")

    def test_unsafe_and_malformed_tags_still_fail(self):
        for tag in ("", "vv1.0.0", "1.0", "01.0.0", "1.0.0;echo", "1.0.0\n", "../1.0.0", "$(id)"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                release.validate(tag, tag)

    def test_reported_tag_uses_prefixed_notes_and_keeps_master_guard(self):
        with tempfile.TemporaryDirectory() as directory:
            previous = Path.cwd()
            try:
                os.chdir(directory)
                Path("gradle.properties").write_text("version=1.0.0\n")
                Path("docs/releases").mkdir(parents=True)
                Path("docs/releases/v1.0.0.md").write_text("# Release\n")
                with patch.dict(os.environ, {"RELEASE_TAG": "1.0.0"}), patch.object(release.subprocess, "run") as run:
                    release.main()
                    self.assertEqual(run.call_args_list[-1].args[0], ["git", "merge-base", "--is-ancestor", "HEAD", "refs/remotes/origin/master"])
                    self.assertTrue(run.call_args_list[-1].kwargs["check"])
                Path("docs/releases/v1.0.0.md").rename("docs/releases/1.0.0.md")
                self.assertEqual(release.find_release_notes("1.0.0"), Path("docs/releases/1.0.0.md"))
                Path("docs/releases/1.0.0.md").write_text(" \n")
                with self.assertRaisesRegex(ValueError, "Missing non-empty"):
                    release.find_release_notes("1.0.0")
            finally:
                os.chdir(previous)


if __name__ == "__main__":
    unittest.main()
