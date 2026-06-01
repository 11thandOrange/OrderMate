# Complex Logic Pipeline — {{REPO_NAME}}

For issues labelled `complex-logic`: generates three distinct implementations
on separate branches, reviews them, and opens a PR from the best one.

## Pipeline

```
Issue labelled "complex-logic"
        ↓
  approach-planner       posts 3 approach comments on the issue     [USER LEVEL]
        ↓
 approach-implementer    implements approach 1 on feat/N-approach-1  [USER LEVEL]
        ↓
 approach-implementer    implements approach 2 on feat/N-approach-2  [USER LEVEL]
        ↓
 approach-implementer    implements approach 3 on feat/N-approach-3  [USER LEVEL]
        ↓
  approach-reviewer      scores all 3, posts decision comment        [USER LEVEL]
        ↓
submit-winning-approach  opens PR from winning branch                [USER LEVEL]
        ↓
 mark-pr-ready           removes draft status                        [USER LEVEL]
        ↓
 whatsapp-notifier       ✅ PR #N ready for your review              [USER LEVEL]
```

## Register the Automation

```bash
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "{{REPO_NAME}} — Complex Logic Pipeline",
    "prompt": "<rendered by automation-builder>",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "event.label.name == '\''complex-logic'\'' && glob(repository.full_name, '\''{{REPO_URL}}'\'')"
    },
    "timeout": 3600,
    "repos": [{"url": "https://github.com/{{REPO_URL}}", "ref": "main"}]
  }'
```

## Trigger

```bash
gh label create "complex-logic" --repo {{REPO_URL}} \
  --color "e4e669" --description "Requires three approaches before implementation"

gh issue edit <ISSUE_NUMBER> --repo {{REPO_URL}} --add-label "complex-logic"
```

## User-Level Agents and Skills Referenced

| Component | Location |
|-----------|----------|
| `approach-planner` | `HeyItsChloe/.agents/agents/approach-planner.md` |
| `approach-implementer` | `HeyItsChloe/.agents/agents/approach-implementer.md` |
| `approach-reviewer` | `HeyItsChloe/.agents/agents/approach-reviewer.md` |
| `submit-winning-approach` | `HeyItsChloe/.agents/skills/submit-winning-approach.md` |
| `mark-pr-ready` | `HeyItsChloe/.agents/skills/mark-pr-ready.md` |
| `whatsapp-notifier` | `HeyItsChloe/.agents/skills/whatsapp-notifier.md` |
