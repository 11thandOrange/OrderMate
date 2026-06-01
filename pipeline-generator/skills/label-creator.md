# Label Creator Skill

Creates `ready-to-implement` and `complex-logic` labels on the target repo.
These labels are the triggers for the two autonomous pipeline automations.
Appends one entry to the manifest.

## Step 1 — Load intake

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
REPO_URL=$(jq -r '.REPO_URL' "$INTAKE")
```

## Step 2 — Create labels

```bash
echo "Creating labels on $REPO_URL..."

gh label create "ready-to-implement" \
  --repo "$REPO_URL" \
  --color "0075ca" \
  --description "Queued for autonomous implementation" 2>/dev/null \
  && echo "  ✅ ready-to-implement created" \
  || echo "  ℹ️  ready-to-implement already exists"

gh label create "complex-logic" \
  --repo "$REPO_URL" \
  --color "e4e669" \
  --description "Requires three approaches before implementation" 2>/dev/null \
  && echo "  ✅ complex-logic created" \
  || echo "  ℹ️  complex-logic already exists"
```

## Step 3 — Verify

```bash
echo ""
echo "Verifying labels:"
gh label list --repo "$REPO_URL" \
  | grep -E "ready-to-implement|complex-logic" \
  | awk '{printf "  %-30s %s\n", $1, $2}'
```

## Step 4 — Append to manifest

```bash
jq --arg repo "$REPO_URL" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"label-creator","status":"done","timestamp":$ts,
          "repo":$repo,
          "labels":["ready-to-implement","complex-logic"]}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ label-creator complete. Manifest updated."
echo "Manifest entries so far: $(jq 'length' "$MANIFEST")"
```
