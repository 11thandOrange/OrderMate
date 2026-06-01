# Autonomous Dev Pipeline — OrderMate

End-to-end automation: GitHub Issue labelled `ready-to-implement` →
planned → implemented → tested → PR → reviewed → CI green → WhatsApp review request.

## Pipeline

```
Issue labelled "ready-to-implement"
        ↓
  ticket-planner              reads issue, maps to Kotlin codebase, posts plan
        ↓
   implementer                creates branch, writes Kotlin/Android code (MVVM/Hilt)
        ↓ (or docs-frontend-implementer if issue involves docs/frontend/)
      tester                  runs unit tests (./gradlew testDebugUnitTest)
        ↓
  build-check (skill)         ./gradlew assembleDebug lint
        ↓
  ticket-manager              gh pr create linking to the issue (draft)   [USER LEVEL]
        ↓
   pr-reviewer                self-review, inline comments, iterate (max 2)
        ↓
   ci-monitor (skill)         polls gh pr checks until green — max 3 retries [USER LEVEL]
        ↓
 mark-pr-ready (skill)        removes draft status, triggers smoke CI      [USER LEVEL]
        ↓
 whatsapp-notifier (skill)    ✅ PR #N ready for your review. Link: PR_URL [USER LEVEL]
```

If the issue involves `docs/frontend/` changes, `docs-frontend-implementer`
is invoked instead of (or alongside) `implementer`.

## Required Secrets

| Secret | Used by |
|--------|---------|
| `GITHUB_TOKEN` | All GitHub operations |
| `ANDROID_SDK_ROOT` | env-setup → build-check |
| `CLOVER_API_KEY` | env-setup |
| `CLOVER_MERCHANT_ID` | env-setup |
| `WHATSAPP_PHONE` | whatsapp-notifier |
| `WHATSAPP_API_KEY` | whatsapp-notifier |

## Setup: Register the Automation

```bash
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OrderMate — Autonomous Dev Pipeline",
    "prompt": "You are the autonomous development pipeline for the OrderMate Android app (https://github.com/11thandOrange/OrderMate).\n\nA GitHub Issue has been labelled ready-to-implement. Find it: gh issue list --repo 11thandOrange/OrderMate --label ready-to-implement --state open --json number,title,body,labels --limit 1\n\nExecute each step. On unrecoverable failure go to Step 9 with failure message.\n\nSTEP 1 - ticket-planner: Follow .agents/agents/ticket-planner.md. Fetch issue, explore Kotlin codebase, produce plan, save to /tmp/plan-NUMBER.md. Delegate plan comment + planned label to user-level ticket-manager.\n\nSTEP 2 - implementer: Follow .agents/agents/implementer.md. Check if issue touches docs/frontend/ — if so use docs-frontend-implementer instead or in addition. Create branch, write Kotlin code (MVVM/Hilt/Coroutines), run ./gradlew testDebugUnitTest, fix failures, commit.\n\nSTEP 3 - tester: Follow .agents/agents/tester.md. Write missing unit tests for new code. Run ./gradlew testDebugUnitTest. Commit new test files.\n\nSTEP 4 - build-check: Follow .agents/skills/build-check.md. Run ./gradlew assembleDebug lint. Fix any errors.\n\nSTEP 5 - ticket-manager: Push branch (git push -u origin BRANCH), create PR: gh pr create --repo 11thandOrange/OrderMate --title ISSUE_TITLE --body Closes #NUMBER. --base main. Record PR number and URL.\n\nSTEP 6 - pr-reviewer: Follow .agents/agents/pr-reviewer.md. Review diff for Kotlin patterns (MVVM/Hilt), post inline comments, iterate on critical issues (max 2 iterations). Do NOT merge.\n\nSTEP 7 - ci-monitor: Follow HeyItsChloe/.agents/skills/ci-monitor.md. Poll gh pr checks PR_NUMBER. On failure fetch logs, fix, push, re-poll (max 3 retries).\n\nSTEP 8 - mark-pr-ready then whatsapp-notifier: If CI passed: follow HeyItsChloe/.agents/skills/mark-pr-ready.md to remove draft status (triggers smoke CI), then follow HeyItsChloe/.agents/skills/whatsapp-notifier.md — message: ✅ PR #NUMBER is ready for your review. Link: PR_URL. If CI failed or earlier failure: follow HeyItsChloe/.agents/skills/whatsapp-notifier.md only — message: ❌ OrderMate pipeline failed. Manual action required. Link: PR_URL.",
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

## Verify

```bash
curl -s "https://app.all-hands.dev/api/automation/v1" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  | python3 -c "import json,sys; [print(a['id'], a['name'], a['enabled']) for a in json.load(sys.stdin)['automations']]"
```

## Trigger the Pipeline

```bash
# Create the label if it doesn't exist
gh label create "ready-to-implement" \
  --repo 11thandOrange/OrderMate \
  --color "0075ca" \
  --description "Queued for autonomous implementation"

# Label an issue to fire the pipeline
gh issue edit <ISSUE_NUMBER> \
  --repo 11thandOrange/OrderMate \
  --add-label "ready-to-implement"
```

## What the Pipeline Will Never Do

- Merge to `main`
- Run `./gradlew assembleRelease` without confirmation
- Upload to the Play Store
- Modify `local.properties`

## Related Files

```
.agents/agents/ticket-planner.md
.agents/agents/implementer.md
.agents/agents/docs-frontend-implementer.md
.agents/agents/tester.md
.agents/agents/pr-reviewer.md
.agents/skills/env-setup.md
.agents/skills/build-check.md
.agents/skills/dev-server.md

User-level (HeyItsChloe/.agents):
agents/ticket-manager.md
skills/ci-monitor.md
skills/mark-pr-ready.md
skills/whatsapp-notifier.md
```
