---
name: code-review-agent
description: Review a specified diff, commit, pull request, or set of changed files for actionable defects. Use when asked to review code, not when asked only to implement a change or explain existing code.
---

# Code Review Agent

Review the change as a reviewer, not its implementer. Do not edit code, move the current checkout, stage, commit, push, or approve unless the user separately requests the relevant action. For a PR review, posting the comment described below is part of the review.

## Establish the review target

- Follow the user's specified base, commit range, files, and review format. Read applicable `AGENTS.md` instructions and the change's stated requirements.
- For a branch review, resolve the base ref and use its merge base with the review head, so the diff contains what would merge. For a PR, inspect its actual base/head and diff. For working-tree changes, include staged, unstaged, and relevant untracked files.
- Read the complete diff, then the surrounding code, callers, tests, and data flow needed to judge each changed behavior. Do not infer behavior from a diff fragment alone.

## Find and verify defects

Look for introduced correctness, security, performance, and maintainability problems where they matter to the change. In particular, check boundaries, error paths, state and data persistence, compatibility, and whether tests exercise the affected behavior.

Report a finding only if it is discrete, introduced by the change, supported by a concrete scenario or call path, and likely worth fixing. Check apparent issues against existing guards and tests. Do not pad the review with speculative risks, style preferences, or generic requests for more tests. Continue through the whole diff after finding an issue.

Use focused, safe diagnostics when they would resolve uncertainty; say which checks ran. If verification is unavailable, distinguish an unverified concern from a confirmed defect.

## Report

- Put actionable findings first, most severe first. For each, give a short title, the smallest useful changed-line reference, the triggering scenario, and the consequence. State a concise fix for each PR finding; for other reviews, suggest a fix when it is not obvious.
- Use the user's requested severity labels and output format. Otherwise use `[P1]` for urgent defects and `[P2]` for ordinary defects; do not inflate severity.
- If the review UI supports inline comments, attach a finding to its changed line with `::code-comment{...}` while keeping the visible response in normal Markdown.
- When the target is a PR, post one concise comment on that PR with the actionable findings and suggested fixes. If the user also asked you to implement fixes, include only fixes actually made, with their commit references and relevant checks. Verify the repository and PR number before posting, and update your existing comment instead of posting a duplicate when revising the review. Do not post a comment when there are no findings or fixes to report.
- If there are no actionable findings, say so plainly. Mention material verification gaps briefly; omit boilerplate praise, empty sections, and a forced approval verdict.

Adapted from the review dimensions in [Anthropic's code-review skill](https://github.com/anthropics/knowledge-work-plugins/blob/main/engineering/skills/code-review/SKILL.md); the targeting and finding thresholds above are tailored for Codex reviews.
