# OrderMate Autonomous Dev Pipeline

End-to-end automation: GitHub Issue labelled `ready-to-implement` →
implemented → PR → reviewed → CI green → WhatsApp review request.

## Pipeline

```
Issue labelled "ready-to-implement"
        ↓
  ordermate-ticket-planner      reads issue, maps to Android codebase, writes plan
        ↓
  env-setup (skill)             verifies Gradle, Android SDK, dependencies
        ↓
  ordermate-implementer         creates branch, writes Kotlin code, tests, commits
        ↓
  tester (user-level)           fills missing test coverage, all tests pass
        ↓
  build-check (skill)           ./gradlew compileDebugKotlin + assembleDebug
        ↓
  ticket-manager (user-level)   gh pr create linking to the issue
        ↓
  pr-reviewer (user-level)      self-review, inline comments, iterate (max 2)
        ↓
  ci-monitor (user-level)       waits for GitHub Actions to pass (max 3 retries)
        ↓
  mark-pr-ready (user-level)    removes draft status, triggers smoke CI
        ↓
  whatsapp-notifier (user-level) sends review request to your phone
```

## Required Context Parameters

When calling user-level agents, pass these:

| Parameter | Value |
|-----------|-------|
| `{{REPO_URL}}` | `11thandOrange/OrderMate` |
| `{{REPO_NAME}}` | `ordermate` |
| `{{SOURCE_DIR}}` | `app/src/main/java/com/orderMate/` |
| `{{TEST_DIR}}` | `app/src/test/java/com/orderMate/` |
| `{{BUILD_CMD}}` | `./gradlew assembleDebug` |
| `{{TEST_CMD}}` | `./gradlew test` |
| `{{LANGUAGE}}` | `Kotlin` |
| `{{FRAMEWORK}}` | `Android` |
| `{{CHECKLIST_FILE}}` | `.agents/checklists/ordermate-review.md` |

## Register the Automation

### Label Trigger

```bash
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OrderMate — Autonomous Dev Pipeline",
    "prompt": "You are the autonomous development pipeline for the OrderMate Android app (https://github.com/11thandOrange/OrderMate).\n\nA GitHub Issue has been labelled ready-to-implement. Find it:\ngh issue list --repo 11thandOrange/OrderMate --label ready-to-implement --state open --json number,title,body,labels --limit 1\n\nContext parameters:\nREPO_URL=11thandOrange/OrderMate\nSOURCE_DIR=app/src/main/java/com/orderMate/\nTEST_DIR=app/src/test/java/com/orderMate/\nBUILD_CMD=./gradlew assembleDebug\nTEST_CMD=./gradlew test\nLANGUAGE=Kotlin\nFRAMEWORK=Android\nCHECKLIST_FILE=.agents/checklists/ordermate-review.md\n\nExecute each step. On unrecoverable failure go to STEP 9 with failure message.\n\nSTEP 1 - ordermate-ticket-planner: Follow .agents/agents/ordermate-ticket-planner.md. Fetch issue, explore codebase, produce plan, save to /tmp/plan-NUMBER.md.\n\nSTEP 2 - env-setup: Follow .agents/skills/env-setup.md. Verify Gradle, Android SDK, dependencies. Check ./gradlew --version.\n\nSTEP 3 - ordermate-implementer: Follow .agents/agents/ordermate-implementer.md. Create branch feat/issue-NUMBER-slug. Read plan, implement Kotlin changes. Run ./gradlew compileDebugKotlin and ./gradlew test before each commit. Fix failures. Push branch.\n\nSTEP 4 - tester (user-level): Follow agents/ticket-manager.md from user-level. Write missing tests in app/src/test/. Run ./gradlew test. Fix failures. Commit.\n\nSTEP 5 - build-check: Follow .agents/skills/build-check.md. Run ./gradlew assembleDebug. Fix any errors.\n\nSTEP 6 - ticket-manager (user-level): Follow agents/ticket-manager.md from user-level. Create PR: gh pr create --repo 11thandOrange/OrderMate --title ISSUE_TITLE --body Closes #NUMBER --base main --draft.\n\nSTEP 7 - pr-reviewer (user-level): Follow agents/pr-reviewer.md from user-level. Check out branch, run tests, review diff against Android/Kotlin checklist, post comments. Iterate max 2 rounds. Do NOT merge.\n\nSTEP 8 - ci-monitor (user-level): Follow skills/ci-monitor.md from user-level. Poll gh pr checks PR_NUMBER. On failure: fetch logs, fix, push, re-poll. Max 3 retries.\n\nSTEP 9 - mark-pr-ready then whatsapp-notifier (user-level): If CI passed: follow skills/mark-pr-ready.md to remove draft status. Then follow skills/whatsapp-notifier.md — message: PR #NUMBER is ready for your review. All checks passed. Link: PR_URL. If pipeline failed: follow skills/whatsapp-notifier.md — message: OrderMate pipeline failed at Step N for issue #NUMBER. Manual action required. Repo: https://github.com/11thandOrange/OrderMate",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "event.label.name == '\''ready-to-implement'\'' && repository.full_name == '\''11thandOrange/OrderMate'\''"
    },
    "timeout": 3600,
    "repos": [
      {"url": "https://github.com/11thandOrange/OrderMate", "ref": "main"},
      {"url": "https://github.com/HeyItsChloe/.agents", "ref": "main"}
    ]
  }'
```

### Cron Trigger (4AM weekdays)

```bash
curl -X POST "https://app.all-hands.dev/api/automation/v1/preset/prompt" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OrderMate — Daily Issue Processor (4AM)",
    "prompt": "You are the daily issue processor for OrderMate.\n\nAt 4AM, batch process all open issues NOT in the admin project.\n\nSTEP 1: Get all open issues with ready-to-implement label:\ngh issue list --repo 11thandOrange/OrderMate --label ready-to-implement --state open --json number,title,body --limit 10\n\nSTEP 2: For each issue, check if in admin project (skip if so).\n\nSTEP 3: For each non-admin issue, trigger the autonomous pipeline.\n\nSTEP 4: Report summary of processed issues.\n\nContext parameters:\nREPO_URL=11thandOrange/OrderMate\nSOURCE_DIR=app/src/main/java/com/orderMate/\nLANGUAGE=Kotlin\nFRAMEWORK=Android",
    "trigger": {
      "type": "cron",
      "schedule": "0 4 * * 1-5",
      "timezone": "UTC"
    },
    "timeout": 7200,
    "repos": [
      {"url": "https://github.com/11thandOrange/OrderMate", "ref": "main"},
      {"url": "https://github.com/HeyItsChloe/.agents", "ref": "main"}
    ]
  }'
```

## Required Secrets

| Secret | Used by |
|--------|---------|
| `GITHUB_TOKEN` | All GitHub operations |
| `WHATSAPP_PHONE` | whatsapp-notifier |
| `WHATSAPP_API_KEY` | whatsapp-notifier |
| `FIREBASE_CONFIG` | env-setup (if Firebase needed) |

## Agent + Skill Map

```
Repo-level (.agents/ in OrderMate):
  agents/ordermate-ticket-planner.md      STEP 1
  skills/env-setup.md                     STEP 2
  agents/ordermate-implementer.md         STEP 3
  skills/build-check.md                    STEP 5

User-level (HeyItsChloe/.agents/):
  agents/tester.md                         STEP 4
  agents/ticket-manager.md                 STEP 6
  agents/pr-reviewer.md                    STEP 7
  skills/ci-monitor.md                     STEP 8
  skills/mark-pr-ready.md                  STEP 9
  skills/whatsapp-notifier.md              STEP 9
```

## What the Pipeline Will Never Do

- Merge to `main`
- Use `var` instead of `val` without justification
- Skip tests
- Push directly to `main`
- Commit google-services.json with real keys

## Related Files

```
.agents/agents/ordermate-ticket-planner.md
.agents/agents/ordermate-implementer.md
.agents/skills/env-setup.md
.agents/skills/build-check.md
.github/workflows/android-ci.yml (assumed)
```