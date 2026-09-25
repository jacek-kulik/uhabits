import unittest
from pathlib import Path


ANDROID_TESTS = (
    Path(__file__).resolve().parents[2]
    / "uhabits-android"
    / "src"
    / "androidTest"
)


class AndroidTestClassificationTest(unittest.TestCase):
    def test_every_concrete_test_class_has_a_size(self):
        unclassified = []
        for path in ANDROID_TESTS.rglob("*Test.kt"):
            source = path.read_text(encoding="utf-8")
            if "@Test" not in source:
                continue
            if not any(
                annotation in source for annotation in ("@MediumTest", "@LargeTest")
            ):
                unclassified.append(str(path.relative_to(ANDROID_TESTS)))

        self.assertEqual([], unclassified)


if __name__ == "__main__":
    unittest.main()
