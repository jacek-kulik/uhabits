# Loop Habit Tracker fork

This is a Kotlin Android app with a Kotlin Multiplatform core. Read the existing
code and the project guides before changing behavior: `docs/BUILD.md`,
`docs/TEST.md`, and `docs/GUIDELINES.md`. The upstream guides describe the
original project; follow the fork owner's request when it differs.

## Code map

- `uhabits-core/src/commonMain` contains habit models, scores, streaks,
  commands, and other shared logic. Platform adapters are in `jvmMain` and
  `jsMain`; core tests are in `commonTest` and `jvmTest`.
- `uhabits-android/src/main` contains the Android UI, reminders, widgets,
  storage integration, resources, and manifest. Local tests are in `src/test`;
  emulator tests are in `src/androidTest`.
- `gradle/libs.versions.toml` and the module build files define versions and
  build settings. Check them instead of assuming current Android defaults.

## Development environment

- Use JDK 17 and Android SDK platform 36. On a fresh clone, install JDK 17 and
  the SDK command-line tools as described in `docs/BUILD.md`, set
  `ANDROID_HOME` or `ANDROID_SDK_ROOT`, then run
  `sdkmanager "platforms;android-36"`.
  Before Gradle commands, run `source tools/dev-env.sh`. It prefers this
  checkout's ignored `.local-dev/` toolchain, then the main checkout's toolchain
  from a linked Git worktree. It also accepts an installed JDK 17 and SDK 36.
  The script leaves `GRADLE_USER_HOME` unchanged. `local.properties` is
  machine-specific and optional.
- Use the checked-in Gradle wrapper, not a system Gradle installation.
- Do not put signing keys, credentials, or generated build outputs in Git.

## Updating dev

- Codex must never merge into local `dev`, including a manual fast-forward merge.
  Do not commit on `dev`, rebase it, cherry-pick onto it, reset it, update its ref
  directly, or push it.
- Install the local guards with `tools/install-git-hooks.sh`. They protect `dev`
  and local branches with work missing from `origin`; see `docs/GIT_HOOKS.md`.
  The only permitted `dev` update is from a clean worktree with
  `git pull --ff-only origin dev`.
  If that pull cannot fast-forward, leave `dev` unchanged and report the
  divergence instead of repairing it with another Git operation.

## Change and verification workflow

- Trace a feature from the Android entry point into core logic and persistence
  before editing. Keep new domain behavior in core when it does not depend on
  Android APIs. Match the existing view-based UI and dependency injection
  patterns; this app is not a Compose project.
- Preserve offline behavior and existing user data. If changing stored data,
  inspect the migration sequence in `uhabits-core/assets/main/migrations` and
  add a migration and regression test as needed.
- Add or update a focused test for behavior changes. Run the relevant checks:
  `./gradlew :uhabits-core:jvmTest`,
  `./gradlew :uhabits-android:testDebugUnitTest`,
  `./gradlew ktlintCheck`, and
  `./gradlew :uhabits-android:assembleDebug`.
- UI and device behavior may need `androidTest` on an emulator. Read
  `docs/TEST.md` before using `build.sh`: its emulator setup deletes a named
  AVD, and its core build formats source files.
- Report exactly which checks ran and any environment blockers. Review the
  diff and Git status before committing or pushing. Never push without a
  request for that change.

## Local gate before dev

- For behavior changes, add a focused test that would fail for the reported bug
  or missing behavior when practical. Review test assertions against the user
  requirement; an AI-authored test can reproduce an AI-authored mistake.
- Once a task branch is committed, run `tools/local-test-gate.sh` before calling
  it ready for `dev`. The script verifies the remote `dev` tip, requires a clean
  branch containing that tip, reruns JVM tests, checks formatting and lint,
  builds both debug APKs, and runs the full Android suite on one dedicated
  emulator.
- A build-only result or missing emulator is an incomplete gate. Report the
  failing check and tested commit, and leave `dev` untouched. Follow
  `docs/TEST.md` for emulator setup and device-test limitations.
- Give every concrete `androidTest` class a `MediumTest` or `LargeTest` size
  annotation. The gate runs those groups in isolation to prevent shared device
  state from leaking between incompatible test categories.
- Fix new lint findings. Do not regenerate `uhabits-android/lint-baseline.xml`
  or accept screenshot goldens just to make a failing gate pass; review the
  specific change to either baseline first.

## AI development review rules

- Provide English and Polish entries in `uhabits-android/src/main/res/values/strings.xml` and `uhabits-android/src/main/res/values-pl-rPL/strings.xml` for every new or changed user-facing app string. Add translations retroactively when modifying or extending an existing feature; do not rely on the English fallback for Polish users.
- Before completing a user-visible feature or fix, check the release rules in `docs/GUIDELINES.md` and update the app version and changelog when required. Use the established patch increment for bug fixes, minor increment for new features, and major increment for major features; update both `versionName` and `versionCode` consistently.
- Before editing, turn the request into a short list of observable outcomes.
  After editing, check each outcome against the implementation and test
  evidence; do not infer correctness from a successful build alone.
- Review the complete diff before committing. Look for unrelated changes,
  weakened or deleted assertions, skipped tests, broad filters, and generated
  files. Passing tests do not replace this review. For risky changes, request a
  second review when available and resolve any actionable findings.
- Treat test changes as behavior changes. Explain why changed expectations,
  screenshot goldens, lint baselines, test filters, or skips are correct, and
  inspect the relevant output or behavior before accepting them. Never weaken a
  test only to make the gate pass.
- Choose checks by risk. Changes to persistence, migrations, backup or restore,
  reminders, widgets, permissions, or lifecycle behavior need focused
  regression coverage and emulator testing when device behavior is involved.
- Keep behavior changes and unrelated refactors separate where practical.
  Commit coherent, reviewable changes with clear messages.
- In the completion report, list the exact checks run and mark each as passed,
  failed, skipped, or blocked. Distinguish compilation from test execution and
  state any relevant coverage gap. Do not call the task ready while a required
  check is failing or unrun.

The outline of this guide was informed by the Android team's
[`nowinandroid/AGENTS.md`](https://github.com/android/nowinandroid/blob/main/AGENTS.md);
all project details and commands above come from this repository.
