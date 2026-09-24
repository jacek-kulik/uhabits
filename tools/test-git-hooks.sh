#!/bin/sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
test_dir=$(mktemp -d "${TMPDIR:-/tmp}/uhabits-hook-test.XXXXXXXX")
trap 'rm -rf "$test_dir"' EXIT HUP INT TERM

git init --bare -q "$test_dir/origin.git"
git init -q -b main "$test_dir/repo"
git -C "$test_dir/repo" config user.name 'Hook Test'
git -C "$test_dir/repo" config user.email 'hook-test@example.invalid'
git -C "$test_dir/repo" remote add origin "$test_dir/origin.git"
git -C "$test_dir/repo" commit -q --allow-empty -m initial
git -C "$test_dir/repo" branch dev
git -C "$test_dir/repo" branch legacy-protected
git -C "$test_dir/repo" push -q origin main

mkdir -p "$test_dir/repo/tools/git-hooks"
cp "$repo_dir/tools/install-git-hooks.sh" "$repo_dir/tools/install-dev-guard.sh" \
    "$test_dir/repo/tools/"
cp "$repo_dir/tools/git-hooks/"* "$test_dir/repo/tools/git-hooks/"
cat > "$test_dir/repo/.git/hooks/reference-transaction" <<'EOF'
#!/bin/sh
[ "$1" = prepared ] || exit 0
printf 'called\n' >> "$HOOK_MARKER"
while read -r old new ref; do
    [ "$ref" = refs/heads/legacy-protected ] && exit 1
done
exit 0
EOF
chmod +x "$test_dir/repo/.git/hooks/reference-transaction"
HOOK_MARKER="$test_dir/previous-hook-called"
export HOOK_MARKER
sh "$test_dir/repo/tools/install-git-hooks.sh" >/dev/null
printf '%s\n' '# test managed update' >> \
    "$test_dir/repo/tools/git-hooks/reference-transaction"
sh "$test_dir/repo/tools/install-git-hooks.sh" >/dev/null
sh "$test_dir/repo/tools/install-dev-guard.sh" >/dev/null
grep -Fqx '# test managed update' \
    "$test_dir/repo/.git/hooks/reference-transaction"

must_block() {
    if "$@" > "$test_dir/output" 2>&1; then
        printf 'Expected command to fail: %s\n' "$*" >&2
        exit 1
    fi
}

git -C "$test_dir/repo" branch unpushed
must_block git -C "$test_dir/repo" branch -D unpushed
git -C "$test_dir/repo" show-ref --verify --quiet refs/heads/unpushed
git -C "$test_dir/repo" push -q origin unpushed
git -C "$test_dir/repo" branch -D unpushed >/dev/null

git -C "$test_dir/repo" branch merged-but-unpushed
must_block git -C "$test_dir/repo" branch -d merged-but-unpushed
git -C "$test_dir/repo" push -q origin merged-but-unpushed
git -C "$test_dir/repo" branch -d merged-but-unpushed >/dev/null

git -C "$test_dir/repo" switch -q -c diverged
git -C "$test_dir/repo" push -q origin diverged
git -C "$test_dir/repo" commit -q --allow-empty -m local
git -C "$test_dir/repo" switch -q main
must_block git -C "$test_dir/repo" branch -D diverged
git -C "$test_dir/repo" push -q origin diverged
git -C "$test_dir/repo" branch -D diverged >/dev/null

git -C "$test_dir/repo" branch removed-from-origin
git -C "$test_dir/repo" push -q origin removed-from-origin
git -C "$test_dir/repo" push -q origin --delete removed-from-origin
must_block git -C "$test_dir/repo" branch -D removed-from-origin

git -C "$test_dir/repo" branch direct-update-ref
must_block git -C "$test_dir/repo" update-ref -d refs/heads/direct-update-ref
git -C "$test_dir/repo" remote set-url origin "$test_dir/missing-origin.git"
must_block git -C "$test_dir/repo" branch -D direct-update-ref
git -C "$test_dir/repo" remote set-url origin "$test_dir/origin.git"
git -C "$test_dir/repo" tag temporary-tag
git -C "$test_dir/repo" tag -d temporary-tag >/dev/null

must_block git -C "$test_dir/repo" update-ref -d refs/heads/legacy-protected
git -C "$test_dir/repo" commit -q --allow-empty -m next
must_block git -C "$test_dir/repo" update-ref refs/heads/dev HEAD
test -s "$HOOK_MARKER"

git init -q -b main "$test_dir/legacy-repo"
git -C "$test_dir/legacy-repo" config user.name 'Hook Test'
git -C "$test_dir/legacy-repo" config user.email 'hook-test@example.invalid'
git -C "$test_dir/legacy-repo" commit -q --allow-empty -m initial
git -C "$test_dir/legacy-repo" branch dev
mkdir -p "$test_dir/legacy-repo/tools/git-hooks"
cp "$repo_dir/tools/install-git-hooks.sh" "$test_dir/legacy-repo/tools/"
cp "$repo_dir/tools/git-hooks/"* "$test_dir/legacy-repo/tools/git-hooks/"
git -C "$repo_dir" cat-file blob \
    76ecbf79534289753b0f3e8c6826ee4e4485c489 > \
    "$test_dir/legacy-repo/.git/hooks/reference-transaction"
chmod +x "$test_dir/legacy-repo/.git/hooks/reference-transaction"
sh "$test_dir/legacy-repo/tools/install-git-hooks.sh" >/dev/null
test ! -e "$test_dir/legacy-repo/.git/hooks/reference-transaction.previous"
cmp -s "$repo_dir/tools/git-hooks/reference-transaction" \
    "$test_dir/legacy-repo/.git/hooks/reference-transaction"
git -C "$test_dir/legacy-repo" pack-refs --all

printf '%s\n' 'Git hook integration tests passed.'
