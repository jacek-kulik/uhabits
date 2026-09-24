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
- Install the local guard with `tools/install-dev-guard.sh`. The only permitted
  update is from a clean `dev` worktree with `git pull --ff-only origin dev`.
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

The outline of this guide was informed by the Android team's
[`nowinandroid/AGENTS.md`](https://github.com/android/nowinandroid/blob/main/AGENTS.md);
all project details and commands above come from this repository.
