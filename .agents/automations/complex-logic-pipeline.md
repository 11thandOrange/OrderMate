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
  [HUMAN reviews and merges]
```

## Pipeline Agent Sources

The pipeline agents are user-level and live in `HeyItsChloe/.agents`:

| Agent | Source |
|-------|--------|
| `approach-planner` | `HeyItsChloe/.agents/agents/approach-planner.md` |
| `approach-implementer` | `HeyItsChloe/.agents/agents/approach-implementer.md` |
| `approach-reviewer` | `HeyItsChloe/.agents/agents/approach-reviewer.md` |
| `submit-winning-approach` | `HeyItsChloe/.agents/skills/submit-winning-approach.md` |

## OpenHands Automation Setup

```bash
OPENHANDS_HOST="https://app.all-hands.dev"

curl -X POST "${OPENHANDS_HOST}/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OrderMate Complex Logic Pipeline",
    "prompt": "A GitHub Issue has been labelled complex-logic on OrderMate.\n\n1. Run approach-planner: explore Kotlin codebase, post 3 approach comments on the issue\n2. Run approach-implementer for approach 1 on branch feat/issue-N-approach-1\n3. Run approach-implementer for approach 2 on branch feat/issue-N-approach-2\n4. Run approach-implementer for approach 3 on branch feat/issue-N-approach-3\n5. Run approach-reviewer: score all 3 branches, post decision comment\n6. Run submit-winning-approach skill: open PR from winning branch\n\nReport the winning PR URL.",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "contains(issue.labels[].name, \'complex-logic\') && glob(repository.full_name, \'11thandOrange/OrderMate\')"
    },
    "timeout": 3600,
    "repos": [
      {"url": "https://github.com/11thandOrange/OrderMate", "ref": "main"}
    ]
  }'
```

## Trigger Configuration

| Setting | Value |
|---------|-------|
| Source | GitHub |
| Event | `issues.labeled` |
| Filter | `contains(issue.labels[].name, 'complex-logic') && glob(repository.full_name, '11thandOrange/OrderMate')` |
| Timeout | 3600 seconds (1 hour) |
