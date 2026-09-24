# Loop Habit Tracker — personal fork

This is my personal fork of [Loop Habit Tracker](https://github.com/iSoron/uhabits),
developed with AI for my own use. It is an Android habit tracker with a Kotlin
Multiplatform core. The original app's store listings and releases do not contain
the changes in this fork.

## What this fork adds

- Automatic database backups about once a day. The app keeps the five newest
  automatic copies. Backups use app storage by default; in **Settings → Database**,
  a public backup folder can be selected to keep copies after uninstalling.
- A **Save to device** choice for database and CSV exports, alongside sharing.
- A debug build named **Habits Dev** with its own app ID (`org.isoron.uhabits.dev`),
  which can be installed alongside the original app. Their app data is separate.
- More useful **Hide entered** filtering for numerical habits: partial progress
  toward an at-least target remains visible, while skipped entries are hidden.

The app also retains Loop's habit schedules, reminders, widgets, statistics,
search, and offline operation. See the [changelog](CHANGELOG.md) for the full
history, including changes inherited from upstream.

## Build and install

Install JDK 17 and Android SDK platform 36, then use the checked-in Gradle
wrapper. From this repository's root:

```sh
source tools/dev-env.sh
./gradlew :uhabits-android:assembleDebug
adb install uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk
```

The last command requires a connected Android device or emulator. For toolchain
setup and worktree instructions, see [docs/BUILD.md](docs/BUILD.md). For tests,
see [docs/TEST.md](docs/TEST.md).

Automatic backups in app storage are removed when the app is uninstalled. For
backups you want to keep, select a public folder in **Settings → Database** and
copy important files off the device. Use a separate backup folder for this debug
app and the original app. See [the backup notes](docs/BUILD.md#automatic-backups-in-this-fork)
for restore instructions.

## Screenshots

[![Main screen][screen1th]][screen1]
[![Edit habit][screen2th]][screen2]
[![Habit strength][screen3th]][screen3]
[![Habit history and streaks][screen4th]][screen4]
[![Widgets][screen5th]][screen5]
[![Night mode][screen6th]][screen6]

## License and upstream

Loop Habit Tracker was created by Álinson Santos Xavier and contributors.
Copyright (C) 2016-2025 Álinson Santos Xavier. This fork remains under the
[GNU General Public License v3 or later](LICENSE.txt). Third-party copyright
and license notices are in [NOTICE.md](NOTICE.md).

[screen1]: screenshots/1.png
[screen2]: screenshots/2.png
[screen3]: screenshots/3.png
[screen4]: screenshots/4.png
[screen5]: screenshots/5.png
[screen6]: screenshots/6.png
[screen1th]: screenshots/1.thumb.png
[screen2th]: screenshots/2.thumb.png
[screen3th]: screenshots/3.thumb.png
[screen4th]: screenshots/4.thumb.png
[screen5th]: screenshots/5.thumb.png
[screen6th]: screenshots/6.thumb.png
