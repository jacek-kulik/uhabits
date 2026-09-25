#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
git_at_repo() { git -C "$repo_dir" "$@"; }
fail() { printf 'Local test gate: %s\n' "$1" >&2; exit 1; }

branch="$(git_at_repo symbolic-ref --quiet --short HEAD)" || fail 'use a named task branch'
[[ "$branch" != dev ]] || fail 'never run a task gate on dev'
[[ -z "$(git_at_repo status --porcelain --untracked-files=normal)" ]] ||
    fail 'commit or remove tracked and untracked changes before the promotion gate'

local_dev="$(git_at_repo rev-parse --verify 'refs/remotes/origin/dev^{commit}')" ||
    fail 'origin/dev is missing; fetch origin dev'
remote_dev="$(git_at_repo ls-remote --exit-code origin refs/heads/dev)" ||
    fail 'cannot verify the current remote dev tip'
remote_dev="${remote_dev%%[[:space:]]*}"
[[ "$local_dev" == "$remote_dev" ]] || fail 'origin/dev is stale; fetch origin dev'
head="$(git_at_repo rev-parse HEAD)"
[[ "$head" != "$local_dev" ]] || fail 'task branch has no commits beyond dev'
git_at_repo merge-base --is-ancestor "$local_dev" "$head" ||
    fail 'task branch does not contain the current dev tip; update the task branch and retest'

printf 'Local test gate: %s at %s contains origin/dev %s\n' "$branch" "$head" "$local_dev"
