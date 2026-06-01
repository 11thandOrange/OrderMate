# Docs Agent Automation — {{REPO_NAME}}

Triggers the `docs-agent` on every push to `main`. Detects route changes,
release commits, and docs file changes, then auto-updates the docs site.

## Overview

The automation detects:
1. Route/controller file changes → runs `api-spec-generator`
2. Release commits → runs `changelog-agent`
3. Any `docs/` changes → verifies the build

## Register the Automation

```bash
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "{{REPO_NAME}} — Docs Agent",
    "prompt": "<rendered by automation-builder>",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "push",
      "filter": "ref == '\''refs/heads/main'\'' && glob(repository.full_name, '\''{{REPO_URL}}'\'')"
    },
    "timeout": 600,
    "repos": [{"url": "https://github.com/{{REPO_URL}}", "ref": "main"}]
  }'
```

## Trigger Configuration

| Setting | Value |
|---------|-------|
| Source | GitHub |
| Event | `push` |
| Filter | `ref == 'refs/heads/main' && glob(repository.full_name, '{{REPO_URL}}')` |
| Timeout | 600 seconds |

## Related Files

- `.agents/agents/docs-agent.md`
- `.agents/agents/api-spec-generator.md`
- `.agents/agents/changelog-agent.md`
- `.agents/agents/site-deployer.md`
- `.github/workflows/deploy-docs.yml`
