---
name: agent-name
description: >
  One-paragraph description of what this agent does and when to invoke it.
  Include 2-4 trigger examples:
  <example>Short phrase that would invoke this agent</example>
  <example>Another trigger phrase</example>
tools:
  - file_editor
  - terminal
model: inherit
permission_mode: never_confirm   # or always_confirm — see permission policy
---

# Agent Title — OrderMate

One sentence: what this agent does for OrderMate and what it does NOT do.

Stack context: Kotlin/Android app (`app/`), MVVM/Hilt/Coroutines,
Retrofit + Clover REST API, docs site in `docs/frontend/` (React/TypeScript).

## Prerequisites

```bash
[ -n "$GITHUB_TOKEN" ] && echo "set" || echo "GITHUB_TOKEN missing"
```

## Inputs

| Input | Required | Description |
|-------|----------|-------------|
| `ISSUE_NUMBER` | Yes | GitHub Issue number |

## Steps

### Step 1 — [First action]

```bash
# concrete commands
```

### Step N — Handoff / Output Report

```markdown
## [Agent Name] Complete: #<ISSUE> — <TITLE>
### Next Steps
Hand off to `next-agent-name`.
```

## What You Must Never Do

- Never push to `main` or merge a PR
- Never touch `local.properties` — only Gradle files read it
- Never expose credentials in logs or committed files
