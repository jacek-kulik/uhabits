#!/usr/bin/env bash
# Exercise device selection and install failure without touching a real phone.
set -euo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
test_root="$(mktemp -d)"
trap 'rm -rf -- "$test_root"' EXIT
export TEST_ROOT="$test_root"
mkdir -p "$test_root/tools" "$test_root/sdk/platform-tools"
cp "$repo_dir/tools/install-on-phone.sh" "$test_root/tools/"

cat > "$test_root/tools/dev-env.sh" <<'EOF'
export ANDROID_HOME="$TEST_ROOT/sdk"
EOF
cat > "$test_root/gradlew" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$TEST_ROOT/builds"
mkdir -p "$TEST_ROOT/uhabits-android/build/outputs/apk/debug"
touch "$TEST_ROOT/uhabits-android/build/outputs/apk/debug/uhabits-android-debug.apk"
EOF
cat > "$test_root/sdk/platform-tools/adb" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$TEST_ROOT/adb-calls"
if [[ "$1" == devices ]]; then
    cat "$TEST_ROOT/devices"
elif [[ "${3:-}" == install ]]; then
    exit "${INSTALL_EXIT:-0}"
else
    exit 1
fi
EOF
chmod +x "$test_root/gradlew" "$test_root/sdk/platform-tools/adb"

script="$test_root/tools/install-on-phone.sh"
printf 'List of devices attached\n\n' > "$test_root/devices"
if "$script" > "$test_root/output" 2>&1; then
    printf 'Expected no-device failure\n' >&2
    exit 1
fi
[[ ! -e "$test_root/builds" ]]

printf 'List of devices attached\nUSB1 device usb:1-1 product:phone\nemulator-5554 device product:emulator\n' > "$test_root/devices"
"$script" > "$test_root/output" 2>&1
grep -q '^:uhabits-android:assembleDebug$' "$test_root/builds"
grep -q '^-s USB1 install -r ' "$test_root/adb-calls"

rm -f "$test_root/builds" "$test_root/adb-calls"
printf 'List of devices attached\nUSB1 device usb:1-1\nUSB2 device usb:1-2\n' > "$test_root/devices"
if "$script" > "$test_root/output" 2>&1; then
    printf 'Expected multiple-device failure\n' >&2
    exit 1
fi
[[ ! -e "$test_root/builds" ]]
"$script" USB2 > "$test_root/output" 2>&1
grep -q '^-s USB2 install -r ' "$test_root/adb-calls"

rm -f "$test_root/adb-calls"
if INSTALL_EXIT=1 "$script" USB2 > "$test_root/output" 2>&1; then
    printf 'Expected install failure\n' >&2
    exit 1
fi
grep -q 'this script will not remove its data' "$test_root/output"
if grep -q 'uninstall' "$test_root/adb-calls"; then
    printf 'The script attempted to uninstall the app\n' >&2
    exit 1
fi
printf 'USB install script checks passed.\n'
