---
name: systematic-debugging
description: Diagnose bugs, failed tests, and unexpected behavior in this Android and Kotlin project, and implement evidence-backed fixes when requested.
---

# Systematic debugging

Use the reported symptom to find a cause that the available evidence supports.

1. Reproduce the problem with the smallest safe command or interaction. Read the full error, stack trace, and relevant logs. If it does not reproduce, record the conditions and gather more evidence before editing.
2. Trace the affected path through the Android entry point, shared core, and persistence where applicable. Check recent changes, configuration, and a comparable working path. For build failures, distinguish source failures from JDK, SDK, or dependency setup problems.
3. State one likely cause and the evidence for it. Use a focused experiment or diagnostic when the evidence is incomplete. Change one cause at a time so the result is interpretable.
4. If the request is diagnosis-only, report the supported cause without editing code. When a fix is requested and the intended behavior is clear, make the smallest correction that addresses the cause. Add or update a focused regression test for behavior changes, following `AGENTS.md`; keep Android-only behavior in the Android module and shared behavior in core.
5. Ask for the user's judgment only when the cause or fix is genuinely uncertain, complicated, offers materially different options, or requires a design choice. Otherwise, re-run the original reproduction and the relevant tests, then report what was verified and what remains uncertain.

For Gradle commands, load `tools/dev-env.sh` and use the checked-in wrapper. Consult `docs/TEST.md` before emulator or `build.sh` workflows.

Adapted for this repository from [Superpowers systematic debugging](https://github.com/obra/superpowers/tree/main/skills/systematic-debugging).
