---
name: code-review-agent
description: Review a specified diff, commit, pull request, or set of changed files, and automatically fix clear defects on writable review branches. Use when asked to review code, not when asked only to implement a change or explain existing code.
---

# Code Review Agent

Review the change critically, then resolve clear defects when the review target is writable. A review request authorizes posting findings to the specified pull request and implementing, committing, and pushing obvious low-risk fixes to its writable head branch; it does not authorize approving, merging, closing, or opening a pull request. Preserve the user's current checkout and use the repository's worktree workflow for edits.

## Establish the review target

- Follow the user's specified base, commit range, files, and review format. Read applicable `AGENTS.md` instructions and the change's stated requirements.
- For a branch review, resolve the base ref and use its merge base with the review head, so the diff contains what would merge. For a PR, inspect its actual base/head and diff. For working-tree changes, include staged, unstaged, and relevant untracked files.
- Read the complete diff, then the surrounding code, callers, tests, and data flow needed to judge each changed behavior. Do not infer behavior from a diff fragment alone.

## Find and verify defects

Look for introduced correctness, security, performance, and maintainability problems where they matter to the change. In particular, check boundaries, error paths, state and data persistence, compatibility, and whether tests exercise the affected behavior.

Report a finding only if it is discrete, introduced by the change, supported by a concrete scenario or call path, and likely worth fixing. Check apparent issues against existing guards and tests. Do not pad the review with speculative risks, style preferences, or generic requests for more tests. Continue through the whole diff after finding an issue.

Use focused, safe diagnostics when they would resolve uncertainty; say which checks ran. If verification is unavailable, distinguish an unverified concern from a confirmed defect.

## Resolve findings

- Fix a finding automatically when the defect is supported by evidence, the intended behavior is clear, and one small low-risk correction follows from it. Add or update focused tests when behavior changes, then run the relevant checks.
- Ask for the user's judgment only when the finding or fix is genuinely uncertain, complicated, risky, offers materially different options, or requires a product or design choice. Explain the evidence and tradeoffs, then continue any independent fixes that are clear.
- Do not change code for speculative concerns. Investigate them far enough to confirm a defect or present the remaining uncertainty to the user.
- For an uncommitted working-tree review that cannot be fixed safely in an isolated worktree, report the concrete fix instead of rewriting the user's existing changes.

## Report

- Put unresolved actionable findings first, most severe first. For each, give a short title, the smallest useful changed-line reference, the triggering scenario, the consequence, and a concise suggested fix. Summarize automatically fixed findings with their commit and verification results.
- Use the user's requested severity labels and output format. Otherwise use `[P1]` for urgent defects and `[P2]` for ordinary defects; do not inflate severity.
- If the review UI supports inline comments, attach unresolved findings to changed lines with `::code-comment{...}` while keeping the visible response in normal Markdown.
- For a pull request, post one concise comment containing unresolved findings and any fixes made, with their commits and checks. No separate authorization is required. Update an existing review comment instead of posting a duplicate, and do not post when there are no findings or fixes.
- If there are no actionable findings, say so plainly. Mention material verification gaps briefly; omit boilerplate praise, empty sections, and a forced approval verdict.

Adapted from the review dimensions in [Anthropic's code-review skill](https://github.com/anthropics/knowledge-work-plugins/blob/main/engineering/skills/code-review/SKILL.md); the targeting and finding thresholds above are tailored for this repository's review-and-fix workflow.
