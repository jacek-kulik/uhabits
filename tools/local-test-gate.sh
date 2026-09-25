#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
"$repo_dir/tools/check-local-gate-state.sh"
source "$repo_dir/tools/dev-env.sh"

head="$(git rev-parse HEAD)"
log_dir="$repo_dir/build/local-test-gate/$head"
mkdir -p "$log_dir"
printf 'Testing commit %s; logs: %s\n' "$head" "$log_dir"

bash -n build.sh tools/check-local-gate-state.sh tools/local-test-gate.sh
python3 -m unittest discover -s tools/tests -p 'test_*.py' -v \
    2>&1 | tee "$log_dir/tool-tests.log"
./gradlew :uhabits-core:jvmTest :uhabits-android:testDebugUnitTest \
    --rerun-tasks --console=plain 2>&1 | tee "$log_dir/jvm-tests.log"
./gradlew ktlintCheck :uhabits-android:lintDebug :uhabits-android:assembleDebug \
    :uhabits-android:assembleAndroidTest --console=plain 2>&1 | tee "$log_dir/build.log"

adb="$ANDROID_HOME/platform-tools/adb"
mapfile -t devices < <("$adb" devices | awk 'NR > 1 && NF {print $1 " " $2}')
if [[ "${#devices[@]}" -ne 1 || ! "${devices[0]}" =~ ^emulator-[0-9]+[[:space:]]device$ ]]; then
    printf '%s\n' 'Local test gate: connect exactly one ready emulator; physical devices are excluded.' >&2
    exit 1
fi
serial="${devices[0]%% *}"
[[ "$("$adb" -s "$serial" shell getprop sys.boot_completed | tr -d '\r')" == 1 ]] || {
    printf '%s\n' 'Local test gate: emulator has not completed booting.' >&2
    exit 1
}
"$adb" -s "$serial" shell wm size | tr -d '\r' | grep -Eq 'size: 768x1280$' || {
    printf '%s\n' 'Local test gate: emulator must use a 768x1280 screen.' >&2
    exit 1
}
"$adb" -s "$serial" shell wm density | tr -d '\r' | grep -Eq 'density: 320$' || {
    printf '%s\n' 'Local test gate: emulator must use density 320.' >&2
    exit 1
}
locale="$("$adb" -s "$serial" shell getprop persist.sys.locale | tr -d '\r')"
if [[ -z "$locale" ]]; then
    locale="$("$adb" -s "$serial" shell getprop ro.product.locale | tr -d '\r')"
fi
[[ "$locale" == en-US ]] || {
    printf 'Local test gate: emulator locale must be en-US; found %s.\n' "$locale" >&2
    exit 1
}
for package in org.isoron.uhabits.dev org.isoron.uhabits.dev.test; do
    if "$adb" -s "$serial" shell pm list packages "$package" |
        grep -Fxq "package:$package"; then
        "$adb" -s "$serial" uninstall "$package"
    fi
done
for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
    "$adb" -s "$serial" shell settings put global "$setting" 0
done

./gradlew :uhabits-android:connectedDebugAndroidTest \
    --rerun-tasks --console=plain 2>&1 | tee "$log_dir/device-tests.log"
"$repo_dir/tools/check-local-gate-state.sh"
[[ "$(git rev-parse HEAD)" == "$head" ]] || {
    printf '%s\n' 'Local test gate: HEAD changed during testing.' >&2
    exit 1
}
printf 'Local test gate passed for %s\n' "$head"
