# Pipeline Verifier Skill

Reads the manifest, verifies every expected file is present on the repo's
default branch, confirms both automations are registered and enabled, confirms
labels exist, and writes a final report. On full pass, calls `whatsapp-notifier`.

## Step 1 — Load intake and manifest

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
REPO_URL=$(jq -r '.REPO_URL'   "$INTAKE")
REPO_NAME=$(jq -r '.REPO_NAME' "$INTAKE")
DOCS_SITE=$(jq -r '.DOCS_SITE' "$INTAKE")
SMOKE_TYPE=$(jq -r '.SMOKE_TYPE' "$INTAKE")

PASS=0
FAIL=0
RESULTS=()

check() {
  local LABEL="$1"
  local CMD="$2"
  if eval "$CMD" > /dev/null 2>&1; then
    echo "  ✅ $LABEL"
    RESULTS+=("{\"check\":\"${LABEL}\",\"status\":\"pass\"}")
    PASS=$((PASS+1))
  else
    echo "  ❌ $LABEL"
    RESULTS+=("{\"check\":\"${LABEL}\",\"status\":\"fail\"}")
    FAIL=$((FAIL+1))
  fi
}
```

## Step 2 — Verify .agents/ files on repo default branch

```bash
echo "── .agents/agents/ ──"
for f in orchestrator ticket-planner implementer tester pr-reviewer \
          smoke-tester build-release code-auditor postman-manager; do
  check "$f.md" \
    "gh api repos/${REPO_URL}/contents/.agents/agents/${f}.md --silent"
done

# Docs agents conditional
if [ "$DOCS_SITE" = "yes" ]; then
  for f in docs-agent site-deployer changelog-agent docs-writer api-spec-generator; do
    check "docs: $f.md" \
      "gh api repos/${REPO_URL}/contents/.agents/agents/${f}.md --silent"
  done
fi

# Secondary implementer — name from manifest
SEC_NAME=$(jq -r '.[] | select(.step=="agent-template-renderer") | .files[] | select(endswith(".md")) | select(test("implementer|extension"))' "$MANIFEST" | grep -v "^implementer.md$" | head -1)
if [ -n "$SEC_NAME" ]; then
  check "secondary: $SEC_NAME" \
    "gh api repos/${REPO_URL}/contents/.agents/agents/${SEC_NAME} --silent"
fi

echo ""
echo "── .agents/skills/ ──"
for f in env-setup build-check dev-server; do
  check "$f.md" \
    "gh api repos/${REPO_URL}/contents/.agents/skills/${f}.md --silent"
done

if [ "$SMOKE_TYPE" != "none" ]; then
  check "smoke-runner.md" \
    "gh api repos/${REPO_URL}/contents/.agents/skills/smoke-runner.md --silent"
fi

echo ""
echo "── .agents/automations/ ──"
for f in autonomous-dev-pipeline complex-logic-pipeline; do
  check "$f.md" \
    "gh api repos/${REPO_URL}/contents/.agents/automations/${f}.md --silent"
done
if [ "$DOCS_SITE" = "yes" ]; then
  check "docs-agent-automation.md" \
    "gh api repos/${REPO_URL}/contents/.agents/automations/docs-agent-automation.md --silent"
fi

echo ""
echo "── .github/workflows/ ──"
case "$(jq -r '.STACK' "$INTAKE")" in
  android-kotlin)
    check "android-ci.yml" \
      "gh api repos/${REPO_URL}/contents/.github/workflows/android-ci.yml --silent"
    ;;
  node-shopify)
    check "node-ci.yml" \
      "gh api repos/${REPO_URL}/contents/.github/workflows/node-ci.yml --silent"
    ;;
esac
if [ "$DOCS_SITE" = "yes" ]; then
  check "deploy-docs.yml" \
    "gh api repos/${REPO_URL}/contents/.github/workflows/deploy-docs.yml --silent"
fi
```

## Step 3 — Verify automations registered and enabled

```bash
echo ""
echo "── OpenHands Automations ──"

AUTOMATIONS=$(curl -s "https://app.all-hands.dev/api/automation/v1" \
  -H "Authorization: Bearer ${OPENHANDS_API_KEY}" \
  | jq -c "[.automations[] | select(.name | contains(\"${REPO_NAME}\"))]")

for name in "Autonomous Dev Pipeline" "Complex Logic Pipeline"; do
  FOUND=$(echo "$AUTOMATIONS" | jq -r --arg n "$name" \
    '.[] | select(.name | contains($n)) | .enabled')
  if [ "$FOUND" = "true" ]; then
    check "${REPO_NAME} — $name (enabled)" "true"
  else
    check "${REPO_NAME} — $name (enabled)" "false"
  fi
done

if [ "$DOCS_SITE" = "yes" ]; then
  FOUND=$(echo "$AUTOMATIONS" | jq -r '.[] | select(.name | contains("Docs Agent")) | .enabled')
  if [ "$FOUND" = "true" ]; then
    check "${REPO_NAME} — Docs Agent (enabled)" "true"
  else
    check "${REPO_NAME} — Docs Agent (enabled)" "false"
  fi
fi
```

## Step 4 — Verify labels

```bash
echo ""
echo "── GitHub Labels ──"
check "ready-to-implement label" \
  "gh label list --repo ${REPO_URL} | grep -q ready-to-implement"
check "complex-logic label" \
  "gh label list --repo ${REPO_URL} | grep -q complex-logic"
```

## Step 5 — Write final report and append to manifest

```bash
REPORT="/tmp/pipeline-report-${REPO_SLUG}.md"

cat > "$REPORT" << REPORT
# Pipeline Verification Report — ${REPO_NAME}

**Date:** $(date -u +%Y-%m-%dT%H:%M:%SZ)
**Repo:** https://github.com/${REPO_URL}
**Result:** $([ "$FAIL" -eq 0 ] && echo "✅ ALL CHECKS PASSED" || echo "❌ $FAIL CHECK(S) FAILED")

## Checks

| Check | Status |
|-------|--------|
$(for r in "${RESULTS[@]}"; do
  LABEL=$(echo "$r" | python3 -c "import json,sys; print(json.load(sys.stdin)['check'])")
  STATUS=$(echo "$r" | python3 -c "import json,sys; print('✅' if json.load(sys.stdin)['status']=='pass' else '❌')")
  echo "| $LABEL | $STATUS |"
done)

## Pipeline Manifest

$(cat "$MANIFEST" | jq -r '.[] | "- **\(.step)** — \(.status) @ \(.timestamp // "n/a")"')

## Steps: $PASS passed, $FAIL failed
REPORT

cat "$REPORT"

RESULTS_JSON=$(printf '%s\n' "${RESULTS[@]}" | jq -s '.')

jq --argjson results "$RESULTS_JSON" \
   --arg pass "$PASS" --arg fail "$FAIL" \
   --arg report "$REPORT" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"pipeline-verifier","status":"done","timestamp":$ts,
          "pass":($pass|tonumber),"fail":($fail|tonumber),
          "report_path":$report,"results":$results}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo ""
echo "✅ pipeline-verifier complete. Manifest updated."
echo "Manifest entries: $(jq 'length' "$MANIFEST")"
```

## Step 6 — WhatsApp notification

```bash
if [ "$FAIL" -eq 0 ]; then
  MSG="✅ Pipeline for ${REPO_NAME} is live. Label an issue ready-to-implement to fire it. https://github.com/${REPO_URL}"
else
  MSG="⚠️ Pipeline for ${REPO_NAME} set up with ${FAIL} verification failure(s). Check /tmp/pipeline-report-${REPO_SLUG}.md"
fi

# Delegate to user-level whatsapp-notifier
echo "Sending WhatsApp notification..."
echo "Message: $MSG"
# Follow HeyItsChloe/.agents/skills/whatsapp-notifier.md with the above message
```
