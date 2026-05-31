# CI Monitor Skill

Poll GitHub Actions checks on a PR until all pass or a failure is detected.
Surfaces failure logs back to the pipeline so `android-implementer` can fix and retry.

## Prerequisites

```bash
[ -n "$GITHUB_TOKEN" ] && echo "GITHUB_TOKEN is set" || echo "GITHUB_TOKEN is NOT set"
gh auth status
```

---

## Required Android CI Workflow

The repo currently only has `deploy-docs.yml`. The autonomous pipeline needs an Android
CI workflow. Create `.github/workflows/android-ci.yml` if it does not exist:

```yaml
name: Android CI

on:
  pull_request:
    branches: [main]

jobs:
  test-and-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Run unit tests
        run: ./gradlew test --no-daemon

      - name: Build debug APK
        run: ./gradlew assembleDebug --no-daemon

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: app/build/reports/tests/
```

Check if this file exists before starting the monitor loop:
```bash
[ -f ".github/workflows/android-ci.yml" ] && echo "CI workflow present" || echo "WARNING: android-ci.yml missing — create it first"
```

---

## Core Monitor Loop

```bash
#!/bin/bash
# ci-monitor: wait for all PR checks to complete, return pass/fail

PR_NUMBER="${1}"
MAX_WAIT_MINUTES="${2:-20}"   # default 20 minute timeout
POLL_INTERVAL=30              # seconds between polls
MAX_POLLS=$(( MAX_WAIT_MINUTES * 60 / POLL_INTERVAL ))
REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner')

echo "Monitoring CI for PR #${PR_NUMBER} on ${REPO}"
echo "Timeout: ${MAX_WAIT_MINUTES} minutes | Polling every ${POLL_INTERVAL}s"

for i in $(seq 1 $MAX_POLLS); do
  STATUS=$(gh pr checks "${PR_NUMBER}" --json name,state,conclusion 2>/dev/null)

  if [ -z "$STATUS" ]; then
    echo "[${i}/${MAX_POLLS}] No checks found yet — waiting..."
    sleep $POLL_INTERVAL
    continue
  fi

  PENDING=$(echo "$STATUS" | jq '[.[] | select(.state == "pending" or .state == "queued" or .conclusion == null)] | length')
  FAILED=$(echo "$STATUS"  | jq '[.[] | select(.conclusion == "failure" or .conclusion == "cancelled" or .conclusion == "timed_out")] | length')
  PASSED=$(echo "$STATUS"  | jq '[.[] | select(.conclusion == "success" or .conclusion == "skipped")] | length')
  TOTAL=$(echo "$STATUS"   | jq 'length')

  echo "[${i}/${MAX_POLLS}] Total: ${TOTAL} | Passed: ${PASSED} | Pending: ${PENDING} | Failed: ${FAILED}"

  if [ "$FAILED" -gt 0 ]; then
    echo "❌ CI FAILED"
    echo "$STATUS" | jq '[.[] | select(.conclusion == "failure" or .conclusion == "cancelled" or .conclusion == "timed_out") | {name, conclusion}]'
    exit 1
  fi

  if [ "$PENDING" -eq 0 ] && [ "$TOTAL" -gt 0 ]; then
    echo "✅ All CI checks passed"
    exit 0
  fi

  sleep $POLL_INTERVAL
done

echo "⏰ Timeout: CI did not complete within ${MAX_WAIT_MINUTES} minutes"
exit 2
```

---

## Fetch Failure Logs (call when exit code is 1)

```bash
# Get the run ID of the failed check
FAILED_RUN=$(gh run list --repo "$REPO" --branch "$(git branch --show-current)" \
  --json databaseId,conclusion,name \
  --jq '[.[] | select(.conclusion == "failure")] | first | .databaseId')

echo "Failed run ID: ${FAILED_RUN}"

# Download and display failed job logs
gh run view "$FAILED_RUN" --log-failed 2>&1 | tail -100
```

---

## Full Usage in the Pipeline

```bash
# 1. After android-implementer pushes the branch and creates a PR:
PR_NUMBER=$(gh pr view --json number -q '.number')

# 2. Run the monitor (20 min timeout)
bash .agents/skills/ci-monitor.sh "${PR_NUMBER}" 20
CI_EXIT=$?

# 3. Branch on result
if [ $CI_EXIT -eq 0 ]; then
  echo "CI green — proceed to whatsapp-notifier"

elif [ $CI_EXIT -eq 1 ]; then
  echo "CI failed — fetching logs for android-implementer to fix"
  FAILED_RUN=$(gh run list --branch "$(git branch --show-current)" \
    --json databaseId,conclusion --jq '[.[] | select(.conclusion=="failure")] | first | .databaseId')
  gh run view "$FAILED_RUN" --log-failed 2>&1 | tail -150
  echo "Feed the above logs to android-implementer, fix, push, and re-run ci-monitor"

elif [ $CI_EXIT -eq 2 ]; then
  echo "CI timed out — notify via whatsapp-notifier with a warning"
fi
```

---

## Retry Limit

The autonomous pipeline should cap CI fix-retry cycles to avoid infinite loops:

```bash
MAX_CI_RETRIES=3
CI_ATTEMPT=0

while [ $CI_ATTEMPT -lt $MAX_CI_RETRIES ]; do
  CI_ATTEMPT=$((CI_ATTEMPT + 1))
  echo "CI attempt ${CI_ATTEMPT}/${MAX_CI_RETRIES}"

  bash .agents/skills/ci-monitor.sh "${PR_NUMBER}" 20
  CI_EXIT=$?

  [ $CI_EXIT -eq 0 ] && break   # success — exit loop

  if [ $CI_ATTEMPT -eq $MAX_CI_RETRIES ]; then
    echo "Max retries reached — escalating to human via WhatsApp"
    # call whatsapp-notifier with a failure message
    exit 1
  fi

  # Feed logs to android-implementer for a fix, then re-push
done
```

---

## Interpreting Check Conclusions

| Conclusion | Meaning | Action |
|------------|---------|--------|
| `success` | Passed | Continue |
| `skipped` | Not applicable | Continue |
| `failure` | Job failed | Fetch logs → fix → re-push |
| `cancelled` | Manually cancelled | Re-trigger or alert human |
| `timed_out` | Job hit timeout | Check for hanging tests |
| `null` | Still running | Keep polling |
