# Automation Builder Skill

Renders the three automation `.md` files and writes
`/tmp/register-automations-${REPO_SLUG}.sh` — a ready-to-run script of three
`curl` commands that register the automations via the OpenHands API.
Appends one entry to the manifest.

## Step 1 — Load intake

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
TEMPLATE_DIR=".agents/templates/automations"
OUTPUT_DIR="/tmp/generated-automations"
mkdir -p "$OUTPUT_DIR"

REPO_URL=$(jq -r '.REPO_URL'                   "$INTAKE")
REPO_NAME=$(jq -r '.REPO_NAME'                 "$INTAKE")
REPO_SLUG=$(jq -r '.REPO_SLUG'                 "$INTAKE")
STACK=$(jq -r '.STACK'                          "$INTAKE")
DOMAIN_A=$(jq -r '.DOMAIN_A'                   "$INTAKE")
DOMAIN_B=$(jq -r '.DOMAIN_B'                   "$INTAKE")
BUILD_CMD=$(jq -r '.BUILD_CMD'                  "$INTAKE")
UNIT_TEST=$(jq -r '.UNIT_TEST_CMD'              "$INTAKE")
SEC_NAME=$(jq -r '.SECONDARY_IMPLEMENTER_NAME'  "$INTAKE")
SECONDARY=$(jq -r '.SECONDARY_IMPLEMENTER'     "$INTAKE")
DOCS_SITE=$(jq -r '.DOCS_SITE'                 "$INTAKE")
SECRETS=$(jq -r '.SECRETS_NEEDED'              "$INTAKE")
```

## Step 2 — Render automation markdown files

```bash
render_automation() {
  local TPL="$1"
  local OUT="$2"
  sed \
    -e "s|{{REPO_NAME}}|${REPO_NAME}|g" \
    -e "s|{{REPO_URL}}|${REPO_URL}|g" \
    -e "s|{{REPO_SLUG}}|${REPO_SLUG}|g" \
    -e "s|{{STACK}}|${STACK}|g" \
    -e "s|{{DOMAIN_A}}|${DOMAIN_A}|g" \
    -e "s|{{DOMAIN_B}}|${DOMAIN_B}|g" \
    -e "s|{{BUILD_CMD}}|${BUILD_CMD}|g" \
    -e "s|{{UNIT_TEST_CMD}}|${UNIT_TEST}|g" \
    -e "s|{{SECONDARY_IMPLEMENTER_NAME}}|${SEC_NAME}|g" \
    -e "s|{{SECRETS_NEEDED}}|${SECRETS}|g" \
    "$TPL" > "$OUT"
  echo "  rendered: $(basename "$OUT")"
}

render_automation \
  "$TEMPLATE_DIR/autonomous-dev-pipeline.tpl.md" \
  "$OUTPUT_DIR/autonomous-dev-pipeline.md"

render_automation \
  "$TEMPLATE_DIR/complex-logic-pipeline.tpl.md" \
  "$OUTPUT_DIR/complex-logic-pipeline.md"

# docs-agent-automation only for repos with a docs site
if [ "$DOCS_SITE" = "yes" ]; then
  render_automation \
    "$TEMPLATE_DIR/docs-agent-automation.tpl.md" \
    "$OUTPUT_DIR/docs-agent-automation.md"
fi
```

## Step 3 — Build step-by-step pipeline prompt strings

```bash
# autonomous-dev-pipeline — 8-step prompt
SECONDARY_STEP=""
if [ "$SECONDARY" = "yes" ]; then
  SECONDARY_STEP="\\\\nSTEP 3 - ${SEC_NAME}: Follow .agents/agents/${SEC_NAME}.md ONLY IF the plan flags ${DOMAIN_B} changes. Skip otherwise."
fi

PIPELINE_PROMPT="You are the autonomous development pipeline for the ${REPO_NAME} repo (https://github.com/${REPO_URL}).\\n\\nA GitHub Issue has been labelled ready-to-implement. Find it: gh issue list --repo ${REPO_URL} --label ready-to-implement --state open --json number,title,body,labels --limit 1\\n\\nExecute each step. On unrecoverable failure go to final step with failure message.\\n\\nSTEP 1 - ticket-planner: Follow .agents/agents/ticket-planner.md. Fetch issue, explore codebase, produce plan, save to /tmp/plan-NUMBER.md. Delegate plan comment + planned label to user-level ticket-manager.\\n\\nSTEP 2 - implementer: Follow .agents/agents/implementer.md. Create branch, write code for ${DOMAIN_A}, run ${UNIT_TEST}, fix failures, commit.${SECONDARY_STEP}\\n\\nSTEP 4 - tester: Follow .agents/agents/tester.md. Write missing unit tests for new code. Run all test suites. Commit new test files.\\n\\nSTEP 5 - build-check: Follow .agents/skills/build-check.md. Run ${BUILD_CMD}. Fix any errors.\\n\\nSTEP 6 - ticket-manager: Push branch (git push -u origin BRANCH), create PR: gh pr create --repo ${REPO_URL} --title ISSUE_TITLE --body 'Closes #NUMBER.' --base main. Record PR number and URL.\\n\\nSTEP 7 - pr-reviewer: Follow .agents/agents/pr-reviewer.md. Review diff, post inline comments, iterate on critical issues (max 2 iterations). Do NOT merge.\\n\\nSTEP 8 - ci-monitor then mark-pr-ready then whatsapp-notifier: Follow HeyItsChloe/.agents/skills/ci-monitor.md. Poll gh pr checks PR_NUMBER. On failure fetch logs, fix, push, re-poll (max 3 retries). If CI passed: follow HeyItsChloe/.agents/skills/mark-pr-ready.md then HeyItsChloe/.agents/skills/whatsapp-notifier.md — message: ✅ PR #NUMBER is ready for your review. Link: PR_URL. If failed: whatsapp-notifier only — message: ❌ ${REPO_NAME} pipeline failed. Manual action required. Link: PR_URL."

# complex-logic-pipeline prompt
COMPLEX_PROMPT="You are the complex-logic pipeline for ${REPO_NAME} (https://github.com/${REPO_URL}).\\n\\nA GitHub Issue has been labelled complex-logic. Find it: gh issue list --repo ${REPO_URL} --label complex-logic --state open --json number,title,body --limit 1\\n\\nExecute each step. On unrecoverable failure post error as issue comment and go to final step.\\n\\nSTEP 1 - approach-planner: Follow HeyItsChloe/.agents/agents/approach-planner.md. Read issue, explore codebase, post 3 approach comments. Each must include branch name, files to change, key design decision, complexity, trade-offs.\\n\\nSTEP 2 - approach-implementer (approach 1): Follow HeyItsChloe/.agents/agents/approach-implementer.md. Implement on feat/issue-NUMBER-approach-1. Run ${UNIT_TEST} before every commit. Post completion comment.\\n\\nSTEP 3 - approach-implementer (approach 2): Implement on feat/issue-NUMBER-approach-2.\\n\\nSTEP 4 - approach-implementer (approach 3): Implement on feat/issue-NUMBER-approach-3.\\n\\nSTEP 5 - approach-reviewer: Follow HeyItsChloe/.agents/agents/approach-reviewer.md. Checkout all 3 branches, run tests, score approaches, post decision comment with winner.\\n\\nSTEP 6 - submit-winning-approach: Follow HeyItsChloe/.agents/skills/submit-winning-approach.md. Open PR from winning branch. Record PR URL.\\n\\nSTEP 7 - mark-pr-ready then whatsapp-notifier: Follow HeyItsChloe/.agents/skills/mark-pr-ready.md then HeyItsChloe/.agents/skills/whatsapp-notifier.md. Message: ✅ PR #NUMBER is ready for your review. Link: PR_URL.\\n\\nSTEP 8 - failure: Post issue comment with failure details. Do not open a PR."

# docs-agent-automation prompt
if [ "$DOCS_SITE" = "yes" ]; then
  DOCS_PROMPT="You are the docs-agent for ${REPO_NAME}. A push to main has occurred.\\n\\n1. Check what files changed: git diff HEAD~1 --name-only\\n\\n2. If route/controller files changed:\\n   - Run api-spec-generator to extract updated endpoints\\n   - Update docs/frontend/src/data/endpoints.ts\\n\\n3. If this looks like a release (tagged commit or version bump):\\n   - Run changelog-agent to generate changelog entries\\n   - Update CHANGELOG.md and docs/frontend/src/pages/Changelog.tsx\\n\\n4. If docs/ files changed:\\n   - Verify build: cd docs/frontend && npm ci && npm run build\\n\\n5. If any docs/ updates were made:\\n   - Commit: git add docs/ CHANGELOG.md && git commit -m 'docs: auto-update [skip ci]'\\n   - Push to main\\n\\nReport all actions taken."
fi
```

## Step 4 — Write registration script

```bash
REGISTER_SCRIPT="/tmp/register-automations-${REPO_SLUG}.sh"

cat > "$REGISTER_SCRIPT" << SCRIPT
#!/usr/bin/env bash
# Auto-generated by automation-builder for ${REPO_NAME}
# Run after merging the .agents/ PR. Requires OPENHANDS_API_KEY.

set -e

[ -n "\$OPENHANDS_API_KEY" ] || { echo "ERROR: OPENHANDS_API_KEY not set"; exit 1; }

OPENHANDS_HOST="https://app.all-hands.dev"

echo "=== Registering: ${REPO_NAME} — Autonomous Dev Pipeline ==="
RESULT_1=\$(curl -s -X POST "\${OPENHANDS_HOST}/api/automation/v1/preset/prompt" \\
  -H "Authorization: Bearer \${OPENHANDS_API_KEY}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "${REPO_NAME} — Autonomous Dev Pipeline",
    "prompt": "${PIPELINE_PROMPT}",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "event.label.name == '\''ready-to-implement'\'' && glob(repository.full_name, '\''${REPO_URL}'\'')"
    },
    "timeout": 3600,
    "repos": [{"url": "https://github.com/${REPO_URL}", "ref": "main"}]
  }')
echo "\$RESULT_1" | python3 -c "import json,sys; d=json.load(sys.stdin); print('  ID:', d.get('id','?'), 'Name:', d.get('name','?'))"

echo ""
echo "=== Registering: ${REPO_NAME} — Complex Logic Pipeline ==="
RESULT_2=\$(curl -s -X POST "\${OPENHANDS_HOST}/api/automation/v1/preset/prompt" \\
  -H "Authorization: Bearer \${OPENHANDS_API_KEY}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "${REPO_NAME} — Complex Logic Pipeline",
    "prompt": "${COMPLEX_PROMPT}",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "issues.labeled",
      "filter": "event.label.name == '\''complex-logic'\'' && glob(repository.full_name, '\''${REPO_URL}'\'')"
    },
    "timeout": 3600,
    "repos": [{"url": "https://github.com/${REPO_URL}", "ref": "main"}]
  }')
echo "\$RESULT_2" | python3 -c "import json,sys; d=json.load(sys.stdin); print('  ID:', d.get('id','?'), 'Name:', d.get('name','?'))"

SCRIPT

if [ "$DOCS_SITE" = "yes" ]; then
cat >> "$REGISTER_SCRIPT" << DOCS_SCRIPT

echo ""
echo "=== Registering: ${REPO_NAME} — Docs Agent ==="
RESULT_3=\$(curl -s -X POST "\${OPENHANDS_HOST}/api/automation/v1/preset/prompt" \\
  -H "Authorization: Bearer \${OPENHANDS_API_KEY}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "name": "${REPO_NAME} — Docs Agent",
    "prompt": "${DOCS_PROMPT}",
    "trigger": {
      "type": "event",
      "source": "github",
      "on": "push",
      "filter": "ref == '\''refs/heads/main'\'' && glob(repository.full_name, '\''${REPO_URL}'\'')"
    },
    "timeout": 600,
    "repos": [{"url": "https://github.com/${REPO_URL}", "ref": "main"}]
  }')
echo "\$RESULT_3" | python3 -c "import json,sys; d=json.load(sys.stdin); print('  ID:', d.get('id','?'), 'Name:', d.get('name','?'))"

DOCS_SCRIPT
fi

cat >> "$REGISTER_SCRIPT" << SCRIPT_FOOTER

echo ""
echo "=== Updating manifest with automation IDs ==="
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"

ID_1=\$(echo "\$RESULT_1" | python3 -c "import json,sys; print(json.load(sys.stdin).get('id','unknown'))")
ID_2=\$(echo "\$RESULT_2" | python3 -c "import json,sys; print(json.load(sys.stdin).get('id','unknown'))")

jq --arg id1 "\$ID_1" --arg id2 "\$ID_2" \\
   --arg ts "\$(date -u +%Y-%m-%dT%H:%M:%SZ)" \\
   '. + [{"step":"register-automations","status":"done","timestamp":\$ts,
          "automation_ids":[\$id1,\$id2]}]' \\
   "\$MANIFEST" > "\${MANIFEST}.tmp" && mv "\${MANIFEST}.tmp" "\$MANIFEST"

echo "Manifest updated with automation IDs."
echo "✅ All automations registered."
SCRIPT_FOOTER

chmod +x "$REGISTER_SCRIPT"
echo "Registration script written: $REGISTER_SCRIPT"
```

## Step 5 — Append to manifest

```bash
AUTO_FILES=("autonomous-dev-pipeline.md" "complex-logic-pipeline.md")
[ "$DOCS_SITE" = "yes" ] && AUTO_FILES+=("docs-agent-automation.md")
FILES_JSON=$(printf '%s\n' "${AUTO_FILES[@]}" | jq -R . | jq -s .)

AUTOMATION_NAMES=("${REPO_NAME} — Autonomous Dev Pipeline" "${REPO_NAME} — Complex Logic Pipeline")
[ "$DOCS_SITE" = "yes" ] && AUTOMATION_NAMES+=("${REPO_NAME} — Docs Agent")
NAMES_JSON=$(printf '%s\n' "${AUTOMATION_NAMES[@]}" | jq -R . | jq -s .)

jq --argjson files "$FILES_JSON" \
   --argjson names "$NAMES_JSON" \
   --arg script "$REGISTER_SCRIPT" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"automation-builder","status":"done","timestamp":$ts,
          "output_dir":"/tmp/generated-automations",
          "files":$files,"automation_names":$names,
          "register_script":$script}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ automation-builder complete. Manifest updated."
echo "Manifest entries so far: $(jq 'length' "$MANIFEST")"
```
