# Autonomous Dev Pipeline Automation

End-to-end automation that takes a GitHub Issue from open → implemented → PR → reviewed
→ CI green → WhatsApp review request. Triggered when the `ready-to-implement` label is
added to an issue.

## Pipeline Overview

```
GitHub Issue labelled "ready-to-implement"
        │
        ▼
  ticket-planner        — reads issue, maps to codebase, produces plan
        │
        ▼
 android-implementer    — creates branch, writes code, runs local tests
        │
        ▼
     tester             — writes any missing tests, confirms ./gradlew test passes
        │
        ▼
  build-release         — confirms ./gradlew assembleDebug passes (build check only)
        │
        ▼
  ticket-manager        — opens PR linked to the issue
        │
        ▼
   pr-reviewer          — self-reviews the PR, posts inline comments, iterates
        │
        ▼
   ci-monitor           — waits for GitHub Actions to go green (up to 3 retries)
        │
        ▼
 whatsapp-notifier      — sends "PR #N is ready for your review" to your phone
```

## Required Secrets

Register all of these in OpenHands → Settings → Secrets before activating:

| Secret | Used By |
|--------|---------|
| `GITHUB_TOKEN` | All GitHub operations |
| `WHATSAPP_PHONE` | whatsapp-notifier |
| `WHATSAPP_API_KEY` | whatsapp-notifier |

---

## Live Automation

| Field | Value |
|-------|-------|
| **Automation ID** | `185429e5-f1e8-4785-9540-064b15112715` |
| **Status** | ✅ Enabled |
| **Registered** | 2026-05-31 |

> This automation is already registered and live. The setup section below is for reference
> or if you ever need to re-register it from scratch.

## Setup: Create the Automation

Run this once (replace `${OPENHANDS_API_KEY}` with your actual key):

```bash
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OrderMate — Autonomous Dev Pipeline",
    "prompt": "You are the autonomous development pipeline for the OrderMate Android repository.\n\nA GitHub Issue has just been labelled \"ready-to-implement\". The issue number is available in the event payload as {{ event.issue.number }}. The issue title is {{ event.issue.title }}.\n\nExecute the following pipeline in order. Stop and send a WhatsApp message via the whatsapp-notifier skill if any step cannot be completed autonomously.\n\n---\n\n## STEP 1 — ticket-planner\n\nRun the ticket-planner agent on issue #{{ event.issue.number }}.\n\n- Fetch the full issue: gh issue view {{ event.issue.number }} --json number,title,body,labels,comments\n- Explore the OrderMate codebase to understand which files are affected\n- Produce a complete implementation plan in the format defined in .agents/agents/ticket-planner.md\n- Save the plan to /tmp/plan-{{ event.issue.number }}.md\n\n## STEP 2 — android-implementer\n\nRun the android-implementer agent using the plan from Step 1.\n\n- Read the plan from /tmp/plan-{{ event.issue.number }}.md\n- Create the feature branch named as specified in the plan (fix/<number>-<slug> or feat/<number>-<slug>)\n- Implement every file change in the plan following the rules in .agents/agents/android-implementer.md\n- Run ./gradlew test — if tests fail, fix them before proceeding\n- Run ./gradlew assembleDebug — if the build fails, fix it before proceeding\n- Commit all changes with conventional commit messages\n\n## STEP 3 — tester\n\nRun the tester agent to fill any test gaps.\n\n- Check which new functions or classes were added in Step 2\n- Write unit tests for any that are not yet covered\n- Run ./gradlew test to confirm all tests pass\n- Commit new test files\n\n## STEP 4 — ticket-manager (create PR)\n\nCreate the pull request.\n\n- Push the feature branch: git push -u origin <branch-name>\n- Create the PR: gh pr create --title \"<plan title>\" --body \"Closes #{{ event.issue.number }}\\n\\n<brief description of changes>\" --base main\n- Add the label \"ready-for-review\" to the PR\n- Record the PR number and URL for use in subsequent steps\n\n## STEP 5 — pr-reviewer (self-review)\n\nRun the pr-reviewer agent on the newly created PR.\n\n- Fetch the PR diff\n- Review against the checklist in .agents/agents/pr-reviewer.md\n- Post any inline comments or suggestions via GitHub API\n- If critical issues are found: fix them in the branch, push, re-review (max 2 self-review iterations)\n- Do NOT merge or approve the PR — the human will do that\n\n## STEP 6 — ci-monitor\n\nWait for GitHub Actions CI to complete.\n\n- Use the logic in .agents/skills/ci-monitor.md to poll gh pr checks <PR_NUMBER>\n- On success: proceed to Step 7\n- On failure: feed the failure logs back to android-implementer, fix, push, wait for CI again\n- Maximum 3 CI fix-retry cycles before escalating to Step 7 with a failure notification\n\n## STEP 7 — whatsapp-notifier\n\nSend a WhatsApp message to the developer.\n\nIf CI passed:\n  Message: \"✅ OrderMate PR #<PR_NUMBER> is ready for your review. All checks passed.\\nBranch: <branch>\\nLink: <PR_URL>\"\n\nIf CI failed after 3 retries:\n  Message: \"❌ OrderMate PR #<PR_NUMBER> needs your attention. Automated CI fix exhausted after 3 attempts.\\nLink: <PR_URL>\"\n\nUse the curl command from .agents/skills/whatsapp-notifier.md with the WHATSAPP_PHONE and WHATSAPP_API_KEY secrets.\n\n---\n\nReport a brief summary of every step taken and its outcome.",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "event.label.name == '\''ready-to-implement'\'' && glob(repository.full_name, '\''11thandOrange/OrderMate'\'')"
    },
    "timeout": 3600,
    "repos": [
      {"url": "https://github.com/11thandOrange/OrderMate", "ref": "main"}
    ]
  }'
```

---

## Verify the Automation

```bash
# List all automations
curl "https://app.all-hands.dev/api/automation/v1" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  | jq '[.[] | {id, name, enabled}]'

# Check recent runs
curl "https://app.all-hands.dev/api/automation/v1/<AUTOMATION_ID>/runs" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  | jq '[.[] | {id, status, startedAt, completedAt}]'
```

## Manually Trigger a Run (for testing)

```bash
# Trigger manually with a specific issue number
curl -X POST "https://app.all-hands.dev/api/automation/v1/<AUTOMATION_ID>/dispatch" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"variables": {"issue_number": "42"}}'
```

---

## How to Trigger the Pipeline on a Ticket

1. Open the GitHub Issue you want implemented
2. Add the label `ready-to-implement`
3. The automation fires automatically
4. Wait for the WhatsApp message

If the label doesn't exist yet:
```bash
gh label create "ready-to-implement" --color "0075ca" --description "Queued for autonomous implementation"
```

---

## Trigger Settings

| Setting | Value |
|---------|-------|
| Event source | GitHub |
| Event type | `issues.labeled` |
| Filter | `event.label.name == 'ready-to-implement'` |
| Repository | `11thandOrange/OrderMate` |
| Timeout | 3600 seconds (1 hour) |

---

## What the Pipeline Will Never Do

- Merge to `main` — that's always a human action after reviewing the WhatsApp link
- Push release tags or bump version numbers
- Force-push to any branch
- Close the original issue — `ticket-manager` only closes it once the human merges the PR

---

## Related Files

```
.agents/agents/ticket-planner.md        ← Step 1
.agents/agents/android-implementer.md   ← Step 2
.agents/agents/tester.md                ← Step 3
.agents/agents/ticket-manager.md        ← Step 4 (PR creation)
.agents/agents/pr-reviewer.md           ← Step 5
.agents/skills/ci-monitor.md            ← Step 6
.agents/skills/whatsapp-notifier.md     ← Step 7
.github/workflows/android-ci.yml        ← CI workflow (create if missing, see ci-monitor.md)
```
