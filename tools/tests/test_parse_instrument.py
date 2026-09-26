import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "parseInstrument.py"


class ParseInstrumentTest(unittest.TestCase):
    def parse(self, contents):
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8") as log:
            log.write(contents)
            log.flush()
            return subprocess.run(
                ["python3", str(SCRIPT), log.name],
                text=True,
                capture_output=True,
            )

    def test_accepts_untimestamped_successful_gradle_output(self):
        result = self.parse("INSTRUMENTATION_CODE: -1\nOK (1 test)\n")

        self.assertEqual(0, result.returncode)
        self.assertEqual("", result.stdout)

    def test_rejects_empty_test_run(self):
        result = self.parse("INSTRUMENTATION_CODE: -1\nOK (0 tests)\n")

        self.assertNotEqual(0, result.returncode)

    def test_reports_failed_test_as_instrumentation_argument(self):
        result = self.parse(
            "100.0 INSTRUMENTATION_STATUS: class=example.HabitsTest\n"
            "100.0 INSTRUMENTATION_STATUS: test=testSearch\n"
            "100.0 INSTRUMENTATION_STATUS_CODE: 1\n"
            "100.5 INSTRUMENTATION_STATUS_CODE: -2\n"
        )

        self.assertNotEqual(0, result.returncode)
        self.assertEqual(
            "-e class example.HabitsTest#testSearch\n",
            result.stdout,
        )
        self.assertIn("FAIL example.HabitsTest#testSearch", result.stderr)


if __name__ == "__main__":
    unittest.main()
