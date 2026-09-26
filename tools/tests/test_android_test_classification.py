import unittest
from pathlib import Path
from tempfile import TemporaryDirectory


ANDROID_TESTS = (
    Path(__file__).resolve().parents[2]
    / "uhabits-android"
    / "src"
    / "androidTest"
)


def unclassified_tests(android_tests):
    unclassified = []
    for path in android_tests.rglob("*.kt"):
        source = path.read_text(encoding="utf-8")
        if "@Test" not in source:
            continue
        if not any(
            annotation in source for annotation in ("@MediumTest", "@LargeTest")
        ):
            unclassified.append(str(path.relative_to(android_tests)))

    return unclassified


class AndroidTestClassificationTest(unittest.TestCase):
    def test_every_concrete_test_class_has_a_size(self):
        self.assertEqual([], unclassified_tests(ANDROID_TESTS))

    def test_detects_unclassified_test_with_other_filename(self):
        with TemporaryDirectory() as directory:
            source = Path(directory) / "ExampleChecks.kt"
            source.write_text("class ExampleChecks { @Test fun checksBehavior() {} }\n", encoding="utf-8")
            self.assertEqual([source.name], unclassified_tests(Path(directory)))


if __name__ == "__main__":
    unittest.main()
