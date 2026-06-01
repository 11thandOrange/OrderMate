# Complex Logic Pipeline — OrderMate

For GitHub Issues labelled `complex-logic`, generates three distinct Kotlin
implementations on separate branches, reviews them, and opens a PR from the best one.

## Pipeline

```
Issue labelled "complex-logic"
        ↓
  approach-planner       reads issue + Kotlin codebase, posts 3 approach comments
        ↓
 approach-implementer    implements approach 1 on feat/issue-N-approach-1
        ↓
 approach-implementer    implements approach 2 on feat/issue-N-approach-2
        ↓
 approach-implementer    implements approach 3 on feat/issue-N-approach-3
        ↓
  approach-reviewer      checks out all 3 branches, scores, posts decision comment
        ↓
submit-winning-approach  opens PR from winning branch with decision doc
        ↓
 whatsapp-notifier       ✅ PR #N is ready for your review. Link: PR_URL  [USER LEVEL]
        ↓
  mark-pr-ready          removes draft status, triggers smoke CI           [USER LEVEL]
```

## Approach Plans

Plans are stored as GitHub Issue comments so they persist across restarts and
are visible before implementation starts. The approach-reviewer reads them back
via `gh issue view --json comments`.

## Pipeline Agent Sources

The pipeline agents are user-level and live in `HeyItsChloe/.agents`:

| Agent | Source |
|-------|--------|
| `approach-planner` | `HeyItsChloe/.agents/agents/approach-planner.md` |
| `approach-implementer` | `HeyItsChloe/.agents/agents/approach-implementer.md` |
| `approach-reviewer` | `HeyItsChloe/.agents/agents/approach-reviewer.md` |
| `submit-winning-approach` | `HeyItsChloe/.agents/skills/submit-winning-approach.md` |

## Required Label

```bash
gh label create "complex-logic" \
  --color "e4e669" \
  --description "Ticket requires three approaches before implementation" \
  --repo 11thandOrange/OrderMate
```

## Register the Automation

```bash
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OrderMate — Complex Logic Pipeline",
    "prompt": "You are the complex-logic pipeline for OrderMate (https://github.com/11thandOrange/OrderMate).\n\nA GitHub Issue has been labelled complex-logic. Find it: gh issue list --repo 11thandOrange/OrderMate --label complex-logic --state open --json number,title,body --limit 1\n\nExecute each step. On unrecoverable failure post the error as an issue comment and go to STEP 8.\n\nSTEP 1 - approach-planner: Follow HeyItsChloe/.agents/agents/approach-planner.md. Read the issue, explore the Kotlin codebase (MVVM/Hilt/Coroutines patterns), post 3 approach comments on the issue. Each comment must include branch name, files to change, key design decision, complexity, and trade-offs.\n\nSTEP 2 - approach-implementer (approach 1): Follow HeyItsChloe/.agents/agents/approach-implementer.md. Implement on feat/issue-NUMBER-approach-1 using .agents/agents/implementer.md patterns. Run ./gradlew testDebugUnitTest before every commit. Post completion comment.\n\nSTEP 3 - approach-implementer (approach 2): Implement on feat/issue-NUMBER-approach-2.\n\nSTEP 4 - approach-implementer (approach 3): Implement on feat/issue-NUMBER-approach-3.\n\nSTEP 5 - approach-reviewer: Follow HeyItsChloe/.agents/agents/approach-reviewer.md. Check out all 3 branches, run tests on each, score approaches (correctness, Kotlin idiom, testability, complexity). Post decision comment naming the winner with justification.\n\nSTEP 6 - submit-winning-approach: Follow HeyItsChloe/.agents/skills/submit-winning-approach.md. Open PR from winning branch. Link to issue. Record PR number and URL.\n\nSTEP 7 - whatsapp-notifier: Follow HeyItsChloe/.agents/skills/whatsapp-notifier.md. Message: ✅ PR #NUMBER is ready for your review. Link: PR_URL\n\nSTEP 8 - mark-pr-ready: Follow HeyItsChloe/.agents/skills/mark-pr-ready.md. Remove draft status from the winning PR.",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "event.label.name == '\''complex-logic'\'' && glob(repository.full_name, '\''11thandOrange/OrderMate'\'')"
    },
    "timeout": 3600,
    "repos": [
      {"url": "https://github.com/11thandOrange/OrderMate", "ref": "main"}
    ]
  }'
```

## Verify

```bash
curl -s "https://app.all-hands.dev/api/automation/v1" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  | python3 -c "import json,sys; [print(a['id'], a['name'], a['enabled']) for a in json.load(sys.stdin)['automations']]"
```
