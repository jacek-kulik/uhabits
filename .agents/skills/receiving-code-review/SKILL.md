---
name: receiving-code-review
description: Evaluate code review feedback on this repository before applying suggested changes.
---

# Receiving code review

Treat each comment as a claim to check against the current code and the requested behavior.

1. Read the full review and identify the file, call path, and scenario behind each comment. Check whether the concern is already handled elsewhere or depends on a project constraint.
2. Fix supported defects with the smallest change that addresses them. Test affected behavior after each distinct fix and check for regressions before closing the work.
3. When feedback is mistaken or would break an existing requirement, explain the evidence and propose the narrower correction. Ask about a genuinely blocking ambiguity while continuing independent items that are clear.
4. Summarize what changed and how it was checked. Keep unresolved comments explicit. Do not post replies or approvals to an external review without the user's authorization.

Use `AGENTS.md` for this repository's Git and test workflow. For a request to perform a review rather than respond to one, use the existing `code-review-agent` skill.

Adapted for this repository from [Superpowers receiving code review](https://github.com/obra/superpowers/tree/main/skills/receiving-code-review).
