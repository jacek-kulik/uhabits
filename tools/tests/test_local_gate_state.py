import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "check-local-gate-state.sh"


class LocalGateStateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.origin = self.root / "origin.git"
        self.work = self.root / "work"
        self.git(self.root, "init", "--bare", "--initial-branch=dev", str(self.origin))
        self.git(self.root, "clone", str(self.origin), str(self.work))
        self.configure(self.work)
        (self.work / "README").write_text("base\n")
        (self.work / "tools").mkdir()
        shutil.copy2(SCRIPT, self.work / "tools/check-local-gate-state.sh")
        self.git(self.work, "add", "README", "tools")
        self.git(self.work, "commit", "-m", "Base")
        self.git(self.work, "push", "-u", "origin", "dev")
        self.git(self.work, "switch", "-c", "codex/task")
        (self.work / "change").write_text("candidate\n")
        self.git(self.work, "add", "change")
        self.git(self.work, "commit", "-m", "Candidate")

    def git(self, cwd, *args):
        return subprocess.run(
            ["git", *args], cwd=cwd, text=True, capture_output=True, check=True
        ).stdout

    def configure(self, path):
        self.git(path, "config", "user.name", "Local Gate Test")
        self.git(path, "config", "user.email", "gate@example.invalid")

    def gate(self):
        return subprocess.run(
            ["bash", str(self.work / "tools/check-local-gate-state.sh")],
            cwd=self.root,
            text=True,
            capture_output=True,
        )

    def advance_remote_dev(self):
        updater = self.root / "updater"
        self.git(self.root, "clone", str(self.origin), str(updater))
        self.configure(updater)
        (updater / "new-dev").write_text("next\n")
        self.git(updater, "add", "new-dev")
        self.git(updater, "commit", "-m", "Advance dev")
        self.git(updater, "push", "origin", "dev")

    def test_clean_task_branch_at_current_dev_passes(self):
        self.assertEqual(0, self.gate().returncode)

    def test_uncommitted_changes_fail(self):
        (self.work / "change").write_text("modified\n")
        self.assertIn("commit or remove", self.gate().stderr)

    def test_stale_remote_tracking_ref_fails(self):
        self.advance_remote_dev()
        self.assertIn("origin/dev is stale", self.gate().stderr)

    def test_branch_missing_fetched_dev_fails(self):
        self.advance_remote_dev()
        self.git(self.work, "fetch", "origin", "dev")
        self.assertIn("does not contain", self.gate().stderr)

    def test_dev_branch_fails(self):
        self.git(self.work, "switch", "dev")
        self.assertIn("never run a task gate on dev", self.gate().stderr)


if __name__ == "__main__":
    unittest.main()
