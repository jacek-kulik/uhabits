#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source_hook="$repo_dir/tools/git-hooks/reference-transaction"
common_git_dir="$(git -C "$repo_dir" rev-parse --path-format=absolute --git-common-dir)"
target_hook="$common_git_dir/hooks/reference-transaction"

if git -C "$repo_dir" config --get core.hooksPath >/dev/null; then
    printf '%s\n' 'Cannot install dev guard while core.hooksPath is set.' >&2
    exit 1
fi
if [[ -e "$target_hook" || -L "$target_hook" ]] && ! cmp -s "$source_hook" "$target_hook"; then
    printf 'Cannot replace existing hook: %s\n' "$target_hook" >&2
    exit 1
fi

install -m 755 "$source_hook" "$target_hook"
printf 'Installed dev guard at %s\n' "$target_hook"
