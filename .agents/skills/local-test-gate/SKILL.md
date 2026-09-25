---
name: local-test-gate
description: Verify a Loop Habit Tracker task branch locally before it is proposed for dev, including Git freshness, JVM checks, and emulator tests. Use when preparing a completed code change for dev or diagnosing a failed local gate.
---

# Local test gate

Work from a dedicated task worktree based on freshly fetched `origin/dev`.
After implementation and focused tests are committed, run
`tools/local-test-gate.sh` from the worktree. It rejects dirty or stale branches
and requires one ready emulator for the full Android suite. Read `docs/TEST.md`
when preparing that emulator; `build.sh android-setup` deletes its named AVD.

When a check fails, fix the issue on the task branch and rerun the gate on its
new commit. Report the commit ID, exact checks, device-test result, and any
unavailable check. A compiled test APK does not establish a device-test pass.
Review any proposed lint baseline or screenshot golden update against the
actual behavior before accepting it.

Review the changed behavior and its test assertions against the request before
calling the branch ready. Do not update local `dev`; follow `AGENTS.md` for Git
and push boundaries.
