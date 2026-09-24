#!/usr/bin/env bash
# Build and install the debug app on a USB-connected Android device.
set -euo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ $# -gt 1 || ( $# -eq 1 && ( "$1" == "-h" || "$1" == "--help" ) ) ]]; then
    printf 'Usage: %s [USB_DEVICE_SERIAL]\n' "$0"
    if [[ $# -gt 1 ]]; then
        exit 2
    fi
    exit 0
fi

source "$repo_dir/tools/dev-env.sh"
adb="$ANDROID_HOME/platform-tools/adb"
if [[ ! -x "$adb" ]]; then
    printf 'ADB is missing: %s\n' "$adb" >&2
    exit 1
fi

devices="$("$adb" devices -l)" || exit 1
mapfile -t usb_serials < <(printf '%s\n' "$devices" | awk '$2 == "device" { for (i = 3; i <= NF; i++) if ($i ~ /^usb:/) print $1 }')

if [[ $# -eq 1 ]]; then
    serial="$1"
    if [[ ! " ${usb_serials[*]} " == *" $serial "* ]]; then
        printf 'No authorized USB device with serial %s was found.\n%s\n' "$serial" "$devices" >&2
        exit 1
    fi
elif [[ ${#usb_serials[@]} -eq 1 ]]; then
    serial="${usb_serials[0]}"
else
    printf 'Connect and authorize one USB phone, or pass its serial.\n%s\n' "$devices" >&2
    exit 1
fi

cd "$repo_dir"
./gradlew :uhabits-android:assembleDebug
apk="$repo_dir/uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk"
if [[ ! -f "$apk" ]]; then
    printf 'Built APK was not found: %s\n' "$apk" >&2
    exit 1
fi

printf 'Installing Habits Dev on %s...\n' "$serial"
if ! "$adb" -s "$serial" install -r "$apk"; then
    printf 'Install failed. If Android reports a signature mismatch, the installed debug app was signed with a different key; this script will not remove its data.\n' >&2
    exit 1
fi
printf 'Habits Dev installed on %s.\n' "$serial"
