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

Before implementation, translate the request into observable outcomes. After
implementation, verify each against the diff and test results. Review the full
diff for unrelated edits, weakened assertions, skips, broad filters, and
generated files. Use a second review for risky changes when available, then
resolve its findings against the code and request.

Treat edits to tests, screenshot goldens, lint baselines, filters, and skips as
behavior changes: inspect the actual behavior and explain why the updated
expectation is correct. Never weaken a check solely to make the gate pass. Add
focused regression coverage based on risk, especially for persistence,
migrations, backup and restore, reminders, widgets, permissions, and lifecycle
behavior; use emulator coverage when device behavior is involved.

Keep unrelated refactors separate where practical. In the final report, list
exact commands and label each check passed, failed, skipped, or blocked. Make
clear whether tests ran or only compiled, state meaningful coverage gaps, and
do not call the branch ready with required checks failing or unrun.
