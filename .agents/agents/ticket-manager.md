---
name: ticket-manager
description: >
  MOVED TO USER LEVEL — this is now a user-level agent.
  See HeyItsChloe/.agents/agents/ticket-manager.md
  <example>Use the user-level ticket-manager for all issue operations</example>
tools:
  - terminal
model: inherit
permission_mode: never_confirm
---

# Ticket Manager — MOVED TO USER LEVEL

This agent has been moved to the user-level agent library.

**Use `HeyItsChloe/.agents/agents/ticket-manager.md` instead.**

The user-level version works identically across all repositories, with the
repo passed as a parameter rather than hardcoded.

## Why User Level?

`ticket-manager` performs GitHub issue CRUD (create, update, label, comment, close).
These operations require no codebase knowledge and are identical across all repos,
making it a natural fit for the user-level agent library.

## Usage

Invoke the user-level `ticket-manager` with:

```
task: [create | update | comment | label | close | search]
repo: 11thandOrange/OrderMate
issue: <ISSUE_NUMBER>   # for existing issues
...
```

## Local Usage (ticket-planner delegation)

`ticket-planner` (this repo) delegates write operations to the user-level
`ticket-manager` after completing its codebase analysis. It does not call
the GitHub API directly.
