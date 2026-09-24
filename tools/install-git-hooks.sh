#!/bin/sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
hook_dir=$(git -C "$repo_dir" rev-parse --path-format=absolute --git-common-dir)/hooks
source_dir="$repo_dir/tools/git-hooks"
hook="$hook_dir/reference-transaction"
previous="$hook.previous"
legacy_dev_hook_oid=76ecbf79534289753b0f3e8c6826ee4e4485c489
legacy_combined_hook_oid=29aee6e7dd193186d5b0817bd8bbd346f185fbc7

if git -C "$repo_dir" config --get core.hooksPath >/dev/null; then
    printf '%s\n' 'Cannot install Git guards while core.hooksPath is set.' >&2
    exit 1
fi
mkdir -p "$hook_dir"
if [ -e "$hook" ] && ! cmp -s "$source_dir/reference-transaction" "$hook"; then
    hook_oid=$(git hash-object --no-filters "$hook" 2>/dev/null || true)
    if grep -Fqx '# uhabits-managed-reference-transaction' "$hook"; then
        : # Update a hook installed by this repository.
    elif [ "$hook_oid" = "$legacy_dev_hook_oid" ] ||
        [ "$hook_oid" = "$legacy_combined_hook_oid" ]; then
        : # Replace a legacy hook installed by this repository.
    elif [ -e "$previous" ]; then
        printf '%s\n' 'Cannot install: an existing hook and its backup both exist.' >&2
        exit 1
    else
        mv "$hook" "$previous"
    fi
fi
install -m 755 "$source_dir/prevent-unpushed-branch-delete" "$hook_dir/prevent-unpushed-branch-delete"
install -m 755 "$source_dir/protect-dev" "$hook_dir/protect-dev"
install -m 755 "$source_dir/reference-transaction" "$hook"
printf 'Installed Git safety guards in %s\n' "$hook_dir"
