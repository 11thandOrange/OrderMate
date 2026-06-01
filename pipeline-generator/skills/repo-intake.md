# Repo Intake Skill

Collect all facts needed to generate the pipeline ecosystem for a new repo.
Writes `/tmp/intake-${REPO_SLUG}.json` and appends the first entry to
`/tmp/pipeline-manifest-${REPO_SLUG}.json`.

## Inputs required from caller

- `REPO_SLUG` — already set by `pipeline-generator` Step 0

## Collect values

Ask the user each question in sequence. Do not proceed until every
required field has a non-empty answer.

```
1. REPO_URL          — GitHub full name (e.g. org/repo-name)
2. STACK             — android-kotlin | node-shopify | react-next | python-fastapi | other
3. DOMAIN_A          — primary code directory (e.g. app/ or web/)
4. DOMAIN_B          — secondary code directory (e.g. docs/frontend/ or extensions/)
5. SECONDARY_IMPLEMENTER — yes / no — does the repo need a second implementer for DOMAIN_B?
6. SECONDARY_IMPLEMENTER_NAME — name for secondary agent (e.g. shopify-extension-implementer)
7. BUILD_CMD         — command that verifies the build (e.g. ./gradlew assembleDebug lint)
8. UNIT_TEST_CMD     — command that runs unit tests (e.g. ./gradlew testDebugUnitTest)
9. SMOKE_TYPE        — espresso | playwright | cypress | none
10. SECRETS_NEEDED   — comma-separated list of env var names (e.g. DB_CONNECTION,SHOPIFY_API_KEY)
11. DOCS_SITE        — yes / no — does the repo have a docs/ site
12. API_EXTRACTION   — yes / no — does the repo expose an HTTP API to extract to OpenAPI
```

## Write intake JSON

```bash
REPO_SLUG="${REPO_SLUG}"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"

cat > "/tmp/intake-${REPO_SLUG}.json" << INTAKE
{
  "REPO_URL":                   "${REPO_URL}",
  "REPO_NAME":                  "$(echo "${REPO_URL}" | sed 's|.*/||')",
  "REPO_SLUG":                  "${REPO_SLUG}",
  "STACK":                      "${STACK}",
  "DOMAIN_A":                   "${DOMAIN_A}",
  "DOMAIN_B":                   "${DOMAIN_B}",
  "SECONDARY_IMPLEMENTER":      "${SECONDARY_IMPLEMENTER}",
  "SECONDARY_IMPLEMENTER_NAME": "${SECONDARY_IMPLEMENTER_NAME}",
  "BUILD_CMD":                  "${BUILD_CMD}",
  "UNIT_TEST_CMD":              "${UNIT_TEST_CMD}",
  "SMOKE_TYPE":                 "${SMOKE_TYPE}",
  "SECRETS_NEEDED":             "${SECRETS_NEEDED}",
  "DOCS_SITE":                  "${DOCS_SITE}",
  "API_EXTRACTION":             "${API_EXTRACTION}"
}
INTAKE

echo "Intake written to /tmp/intake-${REPO_SLUG}.json"
cat "/tmp/intake-${REPO_SLUG}.json" | jq .
```

## Print summary table

```bash
echo "| Field | Value |"
echo "|-------|-------|"
jq -r 'to_entries[] | "| \(.key) | \(.value) |"' "/tmp/intake-${REPO_SLUG}.json"
```

## Append to manifest

```bash
SUMMARY=$(jq -r '"REPO_URL=\(.REPO_URL) STACK=\(.STACK) SMOKE_TYPE=\(.SMOKE_TYPE) DOCS_SITE=\(.DOCS_SITE)"' \
  "/tmp/intake-${REPO_SLUG}.json")

jq --arg summary "$SUMMARY" \
   --argjson values "$(cat /tmp/intake-${REPO_SLUG}.json)" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"repo-intake","status":"done","timestamp":$ts,"summary":$summary,"values":$values}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ repo-intake complete. Manifest updated."
cat "$MANIFEST" | jq 'length'
```
