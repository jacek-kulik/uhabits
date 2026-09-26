---
name: review
description: Review staged, unstaged, or recent changes in the Loop Habit Tracker fork before commit. Use for a read-only precommit quality check, not a full PR review or automatic fixes.
---

# Precommit review

Review the changes the user names. Default to staged changes; if none exist, review unstaged changes; if the working tree is clean, review the latest commit. Say which target you selected. Include relevant untracked files when reviewing the working tree. Do not edit, stage, commit, push, or post comments during this review.

Read the complete diff and enough surrounding code, callers, tests, and storage paths to check actual behavior. Follow `AGENTS.md` and consult `docs/BUILD.md`, `docs/TEST.md`, and `docs/GUIDELINES.md` where relevant. Read `LEARNINGS.md` only if it exists. Judge new problems introduced by the change; do not report generic checklist failures without a concrete impact.

## Checks to apply where relevant

- **Core and Android boundaries:** Keep platform-independent habit rules in `uhabits-core/src/commonMain`; check JVM and JS adapters when shared behavior changes. Trace Android entry points through the existing view-based UI, dependency injection, core commands, and persistence. Check lifecycle, thread use, cancellation, and error handling for UI work; avoid blocking the main thread.
- **Data and offline behavior:** Preserve existing habit data, backups, and offline operation. For stored-data changes, inspect `uhabits-core/assets/main/migrations` and look for an appropriate migration and regression test. Check date, timezone, score, streak, and reminder boundaries when those paths change.
- **Scale and coupling:** Check for repeated database or score work during list rendering, unbounded reads, unnecessary allocations, hidden ordering assumptions, and accidental coupling between Android UI and core. Raise a concern only when the changed path gives a plausible user-visible cost or failure.
- **Scope and bloat:** Identify duplicate logic, pass-through helpers, or abstractions that obscure a specific call path. Do not assume a helper is unnecessary because it has one caller; consider testability, platform boundaries, and readability. Flag unrelated generated files, debug code, commented-out code, and committed credentials or machine-specific configuration.
- **Tests:** Check whether a focused core, Android unit, or instrumentation test covers changed behavior and meaningful edge cases. Prefer tests that exercise real domain logic and persistence; fakes at external or platform boundaries can be appropriate. A compiled instrumentation APK does not establish that device tests ran.
- **Verification:** Check relevant results for `:uhabits-core:jvmTest`, `:uhabits-android:testDebugUnitTest`, `ktlintCheck`, and `:uhabits-android:assembleDebug` when available. UI or device behavior may require `androidTest`. Distinguish checks actually run from checks merely suggested; do not use `build.sh` without reading `docs/TEST.md` because some commands alter files or emulator state.

## Report

Group actionable findings by **Block**, **Warn**, and **Note**. For each, cite the smallest useful changed-line location, the triggering case, its consequence, and a targeted fix. Reserve **Block** for issues that must be fixed before commit, such as a broken flow, data loss, or exposed secret. Use **Warn** for concrete lower-impact defects or maintainability costs. Use **Note** for optional improvements. Omit empty groups. State the review target and any material verification gap.

End with `SHIP IT` if no Block or Warn findings remain; otherwise end with `NEEDS FIXES`. If there is bloat or a weak test, name the exact simplification or missing behavior in the finding instead of recommending an unavailable command.
