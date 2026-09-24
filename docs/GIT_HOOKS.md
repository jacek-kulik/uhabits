# Local Git safety hooks

Install the repository hook once per clone:

```sh
sh tools/install-git-hooks.sh
```

The installer uses the shared Git directory, so the hooks also run from linked
worktrees. It upgrades the repository's older standalone `dev` guard directly.
If another `reference-transaction` hook already exists, the installer keeps it
as `reference-transaction.previous` and runs it first. Running the installer
again updates the guards without replacing that previous hook. The older
`tools/install-dev-guard.sh` command delegates to this installer.

The existing `dev` guard only permits a clean `git pull --ff-only origin dev`
that advances local `dev` to exactly `origin/dev`.

The guard rejects local branch deletion when the branch is missing from `origin`,
the remote cannot be reached, or the local tip has commits absent from the
same-named branch on `origin`. It checks the live remote, so deletion needs a
working connection. A local branch can be deleted once its tip is reachable
from the same branch on `origin`. If that remote branch advanced since your
last fetch, fetch it first so Git can check ancestry locally.

This covers Git ref updates such as `git branch -d`, `git branch -D`, and
`git update-ref -d`. Git hooks can be bypassed explicitly and do not intercept
other destructive commands such as `git reset --hard`, `git clean`, or remote
branch deletion. Check those actions separately.

Run `sh tools/test-git-hooks.sh` to exercise the hook against a temporary local
bare repository.
