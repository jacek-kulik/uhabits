#!/usr/bin/env bash
# Source this file from any directory before Gradle.

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    printf '%s\n' 'Source this file: source tools/dev-env.sh' >&2
    exit 1
fi

_uhabits_set_dev_env() {
    local repo_dir shared_dev_dir common_git_dir java_bin java_home java_major sdk_dir
    repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)" || return 1
    shared_dev_dir="$repo_dir/.local-dev"
    if common_git_dir="$(git -C "$repo_dir" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)" &&
        [[ -n "$common_git_dir" ]]; then
        # Linked worktrees share the main checkout's Git directory and local tools.
        shared_dev_dir="$(dirname -- "$common_git_dir")/.local-dev"
    fi

    if [[ -x "$repo_dir/.local-dev/jdk17/bin/java" ]]; then
        java_home="$repo_dir/.local-dev/jdk17"
        java_bin="$java_home/bin/java"
    elif [[ -x "$shared_dev_dir/jdk17/bin/java" ]]; then
        java_home="$shared_dev_dir/jdk17"
        java_bin="$java_home/bin/java"
    elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
        java_home="$JAVA_HOME"
        java_bin="$java_home/bin/java"
    else
        java_home=""
        java_bin="$(command -v java || true)"
    fi

    if [[ -z "$java_bin" ]]; then
        printf '%s\n' 'JDK 17 is missing; see docs/BUILD.md.' >&2
        return 1
    fi
    java_major="$("$java_bin" -version 2>&1 | sed -n '1s/^[^"]*"\([0-9][0-9]*\).*/\1/p')"
    if [[ "$java_major" != 17 ]]; then
        printf '%s\n' 'JDK 17 is required; see docs/BUILD.md.' >&2
        return 1
    fi

    if [[ -f "$repo_dir/.local-dev/android-sdk/platforms/android-36/android.jar" ]]; then
        sdk_dir="$repo_dir/.local-dev/android-sdk"
    elif [[ -f "$shared_dev_dir/android-sdk/platforms/android-36/android.jar" ]]; then
        sdk_dir="$shared_dev_dir/android-sdk"
    elif [[ -n "${ANDROID_HOME:-}" && -f "$ANDROID_HOME/platforms/android-36/android.jar" ]]; then
        sdk_dir="$ANDROID_HOME"
    elif [[ -n "${ANDROID_SDK_ROOT:-}" && -f "$ANDROID_SDK_ROOT/platforms/android-36/android.jar" ]]; then
        sdk_dir="$ANDROID_SDK_ROOT"
    else
        printf '%s\n' 'Android SDK platform 36 is missing; see docs/BUILD.md.' >&2
        return 1
    fi

    if [[ -n "$java_home" ]]; then
        export JAVA_HOME="$java_home"
        PATH="$JAVA_HOME/bin:$PATH"
    else
        unset JAVA_HOME
    fi
    export ANDROID_HOME="$sdk_dir"
    export ANDROID_SDK_ROOT="$sdk_dir"
    export PATH="$sdk_dir/cmdline-tools/latest/bin:$sdk_dir/platform-tools:$PATH"
}

if ! _uhabits_set_dev_env; then
    unset -f _uhabits_set_dev_env
    return 1
fi
unset -f _uhabits_set_dev_env
