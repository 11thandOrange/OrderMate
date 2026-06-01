# Autonomous Dev Pipeline — OrderMate

End-to-end automation: GitHub Issue labelled `ready-to-implement` →
planned → implemented → tested → PR → reviewed.

## Pipeline

```
Issue labelled "ready-to-implement"
        ↓
  ticket-planner        reads issue, maps to Kotlin codebase, posts plan to issue
        ↓
   implementer          creates branch, writes Kotlin/Android code (MVVM/Hilt)
        ↓
      tester            runs unit tests (./gradlew testDebugUnitTest)
        ↓
  build-check (skill)   ./gradlew assembleDebug lint
        ↓
  ticket-manager        gh pr create linking to the issue (draft)
        ↓
   pr-reviewer          self-review, inline comments, iterate
        ↓
  [HUMAN marks PR ready]
```

If the issue involves `docs/frontend/` changes, `docs-frontend-implementer`
is invoked instead of (or alongside) `implementer`.

## OpenHands Automation Setup

```bash
OPENHANDS_HOST="https://app.all-hands.dev"

curl -X POST "${OPENHANDS_HOST}/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OrderMate Autonomous Dev Pipeline",
    "prompt": "A GitHub Issue has been labelled ready-to-implement on OrderMate.\n\n1. Run ticket-planner for the issue number from the event payload\n2. Run implementer (or docs-frontend-implementer if the issue involves docs/)\n3. Run tester: ./gradlew testDebugUnitTest\n4. Run build-check skill: ./gradlew assembleDebug lint\n5. Open a draft PR linking the issue via ticket-manager\n6. Run pr-reviewer for a self-review pass\n\nDo not mark the PR as ready. Report the PR URL.",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "contains(issue.labels[].name, \'ready-to-implement\') && glob(repository.full_name, \'11thandOrange/OrderMate\')"
    },
    "timeout": 1800,
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
| Filter | `contains(issue.labels[].name, 'ready-to-implement') && glob(repository.full_name, '11thandOrange/OrderMate')` |
| Timeout | 1800 seconds (30 min) |
