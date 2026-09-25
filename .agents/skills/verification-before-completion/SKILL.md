---
name: verification-before-completion
description: Verify changed Kotlin or Android work before reporting it complete or committing.
---

# Verification before completion

Base the completion report on checks run against the current files.

1. Identify the affected behavior and the smallest checks that can confirm it. Use the relevant checks listed in `AGENTS.md`; run broader checks when the change spans modules or the result leaves a concrete risk.
2. Before Gradle, run `source tools/dev-env.sh`, then use `./gradlew`. Read `docs/TEST.md` before using an emulator or `build.sh`.
3. Read each command's exit status and failure output. Reproduce the original symptom for a bug fix, and inspect generated artifacts when the task depends on them.
4. Review the final diff and Git status for unrelated, ignored, or generated files. Confirm the requested behavior and scope before committing.
5. Report which checks passed, failed, or could not run. Do not call an unrun check passing or infer a successful build from a passing linter.

This skill does not grant permission to push or open a pull request. Follow `AGENTS.md` and the user's instructions for those actions.

Adapted for this repository from [Superpowers verification before completion](https://github.com/obra/superpowers/tree/main/skills/verification-before-completion).
