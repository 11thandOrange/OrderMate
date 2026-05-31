# WhatsApp Notifier Skill

Send a WhatsApp message to request a human code review after a PR is ready.

## Required Secrets

Register these in OpenHands → Settings → Secrets before using this skill:

| Secret | Description |
|--------|-------------|
| `WHATSAPP_PHONE` | Your WhatsApp number in international format, no `+` (e.g. `447911123456`) |
| `WHATSAPP_API_KEY` | callmebot API key (see One-Time Setup below) |

---

## One-Time Setup (callmebot — free, no SDK)

1. Open WhatsApp and send this message to **+34 644 76 60 71**:
   ```
   I allow callmebot to send me messages
   ```
2. You'll receive a reply with your personal API key.
3. Save that key as the `WHATSAPP_API_KEY` secret in OpenHands.

---

## Send a PR Review Request

```bash
# Build the message
PR_NUMBER="$1"
PR_URL="$2"
MESSAGE="OrderMate PR #${PR_NUMBER} is ready for your review. All checks passed.%0A${PR_URL}"

# Send via callmebot
curl -s "https://api.callmebot.com/whatsapp.php?phone=${WHATSAPP_PHONE}&text=${MESSAGE}&apikey=${WHATSAPP_API_KEY}" \
  | grep -o '"status":"[^"]*"'
```

### Usage in Pipeline

```bash
# After CI goes green and pr-reviewer has self-reviewed:
PR_NUMBER=$(gh pr view --json number -q '.number')
PR_URL=$(gh pr view --json url -q '.url')

MESSAGE="OrderMate+PR+%23${PR_NUMBER}+is+ready+for+your+review.+All+checks+passed.%0A${PR_URL}"

curl -s "https://api.callmebot.com/whatsapp.php?phone=${WHATSAPP_PHONE}&text=${MESSAGE}&apikey=${WHATSAPP_API_KEY}"
```

### Custom Message Templates

```bash
# PR ready for review
send_whatsapp() {
  local raw_message="$1"
  local encoded
  encoded=$(python3 -c "import urllib.parse, sys; print(urllib.parse.quote(sys.argv[1]))" "$raw_message")
  curl -s "https://api.callmebot.com/whatsapp.php?phone=${WHATSAPP_PHONE}&text=${encoded}&apikey=${WHATSAPP_API_KEY}"
}

# PR ready
send_whatsapp "✅ OrderMate PR #${PR_NUMBER} is ready for your review. All CI checks passed.
Branch: ${BRANCH}
Link: ${PR_URL}"

# CI failed — needs attention
send_whatsapp "❌ OrderMate PR #${PR_NUMBER} has failing CI checks and needs your input.
Link: ${PR_URL}"

# Build failed
send_whatsapp "🔴 OrderMate build failed on PR #${PR_NUMBER}. Automated retry exhausted.
Link: ${PR_URL}"
```

---

## Alternative: Twilio (if you already have a Twilio account)

### Additional Secrets Needed

| Secret | Description |
|--------|-------------|
| `TWILIO_ACCOUNT_SID` | From Twilio console dashboard |
| `TWILIO_AUTH_TOKEN` | From Twilio console dashboard |
| `TWILIO_FROM_NUMBER` | Your Twilio WhatsApp sender (`whatsapp:+14155238886`) |

### Send via Twilio

```bash
TO_NUMBER="whatsapp:+${WHATSAPP_PHONE}"
FROM_NUMBER="${TWILIO_FROM_NUMBER}"
MESSAGE="OrderMate PR #${PR_NUMBER} is ready for your review. All checks passed.\n${PR_URL}"

curl -s -X POST "https://api.twilio.com/2010-04-01/Accounts/${TWILIO_ACCOUNT_SID}/Messages.json" \
  --data-urlencode "From=${FROM_NUMBER}" \
  --data-urlencode "To=${TO_NUMBER}" \
  --data-urlencode "Body=${MESSAGE}" \
  -u "${TWILIO_ACCOUNT_SID}:${TWILIO_AUTH_TOKEN}" \
  | jq '{sid: .sid, status: .status, error: .error_message}'
```

> **Twilio note:** Your Twilio number must be joined to the WhatsApp Sandbox first, or you need an approved WhatsApp sender. See https://www.twilio.com/docs/whatsapp

---

## Verify a Test Message

```bash
# Send a test ping before using in the pipeline
curl -s "https://api.callmebot.com/whatsapp.php?phone=${WHATSAPP_PHONE}&text=OrderMate+agent+test+ping&apikey=${WHATSAPP_API_KEY}"
# Expected response contains: "status":"Message sent"
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Message not delivered` | Re-send activation message to callmebot number |
| `Wrong API key` | Check `WHATSAPP_API_KEY` secret value has no extra spaces |
| `Phone format error` | Ensure `WHATSAPP_PHONE` has no `+`, spaces, or dashes |
| Twilio 401 | Regenerate auth token in Twilio console and update secret |
| Twilio 21608 | Join the Sandbox: send `join <your-sandbox-word>` to the Twilio number |
