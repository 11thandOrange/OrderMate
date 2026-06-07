# Complex Logic Pipeline — User Level

For GitHub Issues labelled `complex-logic`, the pipeline generates three
distinct implementations on separate branches, reviews them, and opens a PR
from the best one. Works for any repository owned by HeyItsChloe or any
connected org.

## Pipeline

```
Issue labelled "complex-logic"
        ↓
  approach-planner       reads issue + codebase, posts 3 approach comments on issue
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
  mark-pr-ready          removes draft status, triggers smoke CI
        ↓
  whatsapp-notifier      ✅ PR #N is ready for your review. Link: PR_URL
```

## Approach Plans

Plans are stored as GitHub Issue comments (not `/tmp/`) so they persist across
restarts and are visible before implementation starts. The approach-reviewer
reads them back via `gh issue view --json comments`.

## All Dependencies Are User-Level

Every agent and skill this pipeline uses lives in `HeyItsChloe/.agents`:

```
agents/approach-planner.md          ✅
agents/approach-implementer.md      ✅
agents/approach-reviewer.md         ✅
skills/submit-winning-approach.md   ✅
skills/mark-pr-ready.md             ✅
skills/whatsapp-notifier.md         ✅
```

No repo-level files are required. Drop a `complex-logic` label in any repo and
register the automation against it.

## Required Label (per repo)

```bash
gh label create "complex-logic" \
  --color "e4e669" \
  --description "Ticket requires three approaches before implementation" \
  --repo <OWNER>/<REPO>
```

## Required Secrets

| Secret | Used by |
|--------|---------|
| `GITHUB_TOKEN` | All GitHub operations |
| `WHATSAPP_PHONE` | whatsapp-notifier |
| `WHATSAPP_API_KEY` | whatsapp-notifier |

---

## Register the Automation

### Option A — Single Wildcard Registration (recommended)

One automation covers all repos you own. The prompt reads the triggering repo
from the cloned environment at runtime.

```bash
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Complex Logic Pipeline",
    "prompt": "You are the complex-logic pipeline.\n\nDetermine which repository triggered this run:\nREPO=$(gh repo view --json nameWithOwner -q '\''.nameWithOwner'\'')\necho \"Running for: $REPO\"\n\nA GitHub Issue has been labelled complex-logic. Find it:\ngh issue list --repo \"$REPO\" --label complex-logic --state open --json number,title,body --limit 1\n\nExecute each step. On unrecoverable failure post the error as an issue comment and go to STEP 8.\n\nSTEP 1 - approach-planner: Follow agents/approach-planner.md. Read the issue, explore the codebase, post 3 approach comments on the issue. Each comment must include branch name, files to change, key design decision, complexity, and trade-offs.\n\nSTEP 2 - approach-implementer (approach 1): Follow agents/approach-implementer.md. Implement approach 1 on feat/issue-NUMBER-approach-1. Run tests before every commit. Post completion comment on issue.\n\nSTEP 3 - approach-implementer (approach 2): Follow agents/approach-implementer.md. Implement approach 2 on feat/issue-NUMBER-approach-2. Run tests before every commit. Post completion comment on issue.\n\nSTEP 4 - approach-implementer (approach 3): Follow agents/approach-implementer.md. Implement approach 3 on feat/issue-NUMBER-approach-3. Run tests before every commit. Post completion comment on issue.\n\nSTEP 5 - approach-reviewer: Follow agents/approach-reviewer.md. Check out all 3 branches, score each on simplicity/pattern-fit/AC-coverage/test-quality, post decision comment with scores and winner on the issue.\n\nSTEP 6 - submit-winning-approach: Follow skills/submit-winning-approach.md. Open PR from the winning branch. Record PR URL as issue comment.\n\nSTEP 7 - mark-pr-ready then whatsapp-notifier: Follow skills/mark-pr-ready.md then skills/whatsapp-notifier.md. Message: PR #NUMBER is ready for your review. Link: PR_URL.\n\nSTEP 8 - failure: Post issue comment with failure details. Do not open a PR.",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "event.label.name == '\''complex-logic'\'' && (glob(repository.full_name, '\''HeyItsChloe/*'\'') || glob(repository.full_name, '\''11thandOrange/*'\''))"
    },
    "repos": [
      {"url": "https://github.com/HeyItsChloe/.agents", "ref": "main"}
    ]
  }'
```

> **Note on `repos`:** The `.agents` repo is cloned so all user-level agents and
> skills are available. The triggering repo is determined at runtime via
> `gh repo view`. For repos that need their own `.agents/` skills loaded, add
> them as a second entry in `repos` using the dynamic pattern above.

### Option B — Per-Repo Registration

Use this when a specific repo needs its own registration (e.g. different timeout,
or to keep automations scoped per project).

```bash
# Replace OWNER/REPO and REPO_URL with the target repository
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "<REPO_NAME> — Complex Logic Pipeline",
    "prompt": "You are the complex-logic pipeline for <OWNER>/<REPO> (https://github.com/<OWNER>/<REPO>).\n\nA GitHub Issue has been labelled complex-logic. Find it:\ngh issue list --repo <OWNER>/<REPO> --label complex-logic --state open --json number,title,body --limit 1\n\nExecute each step. On unrecoverable failure post the error as an issue comment and go to STEP 8.\n\nSTEP 1 - approach-planner: Follow agents/approach-planner.md.\nSTEP 2-4 - approach-implementer: Follow agents/approach-implementer.md for each approach.\nSTEP 5 - approach-reviewer: Follow agents/approach-reviewer.md.\nSTEP 6 - submit-winning-approach: Follow skills/submit-winning-approach.md.\nSTEP 7 - mark-pr-ready then whatsapp-notifier: Follow skills/mark-pr-ready.md then skills/whatsapp-notifier.md.\nSTEP 8 - failure: Post issue comment. Do not open a PR.",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "event.label.name == '\''complex-logic'\'' && repository.full_name == '\''<OWNER>/<REPO>'\''"
    },
    "repos": [
      {"url": "https://github.com/HeyItsChloe/.agents", "ref": "main"},
      {"url": "https://github.com/<OWNER>/<REPO>", "ref": "main"}
    ]
  }'
```

## Live Automation

| Field | Value |
|-------|-------|
| **Automation ID** | `2541e763-d4ac-4665-bd9e-4f726de211b6` |
| **Status** | ✅ Enabled |
| **Registered** | 2026-06-01 |
| **Trigger** | `issues.labeled` → `complex-logic` on `HeyItsChloe/*` + `11thandOrange/*` |

## Verify Registration

```bash
curl -s "https://app.all-hands.dev/api/automation/v1" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  | python3 -c "import json,sys; [print(a['id'], a['name'], a['enabled']) for a in json.load(sys.stdin)['automations']]"
```

## Trigger the Pipeline

```bash
# Create the label if it doesn't exist
gh label create "complex-logic" \
  --repo <OWNER>/<REPO> \
  --color "e4e669" \
  --description "Ticket requires three approaches before implementation"

# Label an issue to fire the pipeline
gh issue edit <ISSUE_NUMBER> \
  --repo <OWNER>/<REPO> \
  --add-label "complex-logic"
```

## Both Pipelines at a Glance

| Label | Pipeline | Branches |
|-------|----------|---------|
| `ready-to-implement` | Standard pipeline — single implementation | 1 |
| `complex-logic` | This pipeline — three approaches, reviewer picks best | 3 + 1 PR |

## What the Pipeline Will Never Do

- Merge to `main`
- Push directly to `main`
- Delete the losing approach branches
