# Testing the project

Loop Habit Tracker has a fairly large number of automated tests to reduce the chance of bugs being silently introduced in our code base. The tests are divided into three categories:

- **Unit tests:** These tests run very quickly on the developer's computer, inside a JVM, and do not need an Android emulator or device. They typically test the correctness of core functions of the application, such as the computation of scores and streaks.
- **Instrumented tests:** These tests require an Android emulator or device. _Medium_ instrumented tests are still quite fast to run, since only individual classes are tested. The app itself does not need to be launched. Examples include _view tests_, which render our custom views on the device and compare them against prerendered images. _Large_ instrumented tests launch the application on an Android emulator and interact with it by touching the screen, much like a regular user.

## Running unit tests

Unit tests can be launched by running `./gradlew test` or by right-clicking a particular class/method in Android Studio and selecting "Run testMethod()" or "Run ClassTest". An alternative way is to use `build.sh`, the script used by our continuous integration server. By running `./build.sh build`, the script will automatically build and run all small tests.

## Running instrumented tests

To run medium tests, it is recommended to use the `build.sh` script.

1. Run `./build.sh android-setup API` to create the emulator, where `API` is the desired API level.
2. Run `./build.sh android-tests API` to run the tests on a single API.
3. Run `./build.sh android-tests-parallel API API...` to run the tests on multiple APIs in parallel.

Note that instrumented tests are designed to run on a clean install, inside an emulator. They will not work on actual devices. All tests are also designed for a particular screen size, namely the Nexus 4 configuration (4.7" 768x1280 xhdpi), and a particular locale, namely English (US). Furthermore:

- No additional apps should be installed on the device;
- The homescreen must look exactly like it was when the emulator was originally created, with no additional icons or widgets;
- All animations must be manually disabled.

If there are failing view tests (that is, if some custom views do not render exactly like the prerendered images we have), then both the actual and expected images will be automatically downloaded from the device to the folder `uhabits-android/build/outputs`. After verifying the differences, if you feel that the actual images are actually fine and should replace the prerendered ones, then run `./build.sh android-accept-images`.

## Local testing in this fork

There is no GitHub CI gate. Work on a task branch from a freshly fetched
`origin/dev`, add focused regression tests for changed behavior, and use the
checked-in Gradle wrapper after `source tools/dev-env.sh`. During development,
run the relevant JVM tests, `ktlintCheck`, and `assembleDebug`.

Before proposing a committed branch for `dev`, run `tools/local-test-gate.sh`.
It requires a clean task branch containing the current remote `dev` tip. It
reruns the gate's regression tests plus the core and Android JVM tests, checks
style and lint, builds the app and test APKs, then runs
`connectedDebugAndroidTest` on exactly one ready emulator. It checks Git state
again afterward. Logs are under `build/local-test-gate/<commit>`.
If `dev` advances, update the task branch and rerun the gate; never merge into
local `dev` as a workaround.

The full gate needs a dedicated emulator. Install the SDK emulator and an
Android system image if they are missing, then create an AVD with the Nexus 4
screen configuration used by the image tests: 768x1280 at density 320. Set the
locale to English (US), keep the home screen clean, and disable animations.
Check `adb devices` before running the gate; a physical phone is not an
acceptable substitute. The gate verifies the screen, density, locale, and boot
state, then removes only the Dev app and its test package before running tests.
`build.sh android-setup` deletes the named AVD, so inspect it before using
that command. Do not accept new golden images merely to make the gate green.

The older `build.sh build` command runs `ktlintFormat` and
`kotlinUpgradeYarnLock`, which can edit source or lock files. Use the local
gate for pre-`dev` verification. `build.sh android-tests` remains available for
the legacy medium/large split after building matching app and test APKs.

The checked-in `uhabits-android/lint-baseline.xml` records existing errors;
warnings remain visible and new lint errors fail the gate. Review a finding
before changing that baseline.
