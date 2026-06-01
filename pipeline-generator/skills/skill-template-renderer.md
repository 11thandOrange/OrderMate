# Skill Template Renderer Skill

Reads `/tmp/intake-${REPO_SLUG}.json`, renders the skill templates in
`.agents/templates/skills/`, and writes output to `/tmp/generated-skills/`.
Appends one entry to the pipeline manifest.

## Step 1 — Load intake

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
TEMPLATE_DIR=".agents/templates/skills"
OUTPUT_DIR="/tmp/generated-skills"
mkdir -p "$OUTPUT_DIR"

REPO_URL=$(jq -r '.REPO_URL'          "$INTAKE")
REPO_NAME=$(jq -r '.REPO_NAME'        "$INTAKE")
REPO_SLUG=$(jq -r '.REPO_SLUG'        "$INTAKE")
STACK=$(jq -r '.STACK'                "$INTAKE")
BUILD_CMD=$(jq -r '.BUILD_CMD'        "$INTAKE")
UNIT_TEST=$(jq -r '.UNIT_TEST_CMD'    "$INTAKE")
SMOKE_TYPE=$(jq -r '.SMOKE_TYPE'      "$INTAKE")
SECRETS=$(jq -r '.SECRETS_NEEDED'     "$INTAKE")
API_EXT=$(jq -r '.API_EXTRACTION'     "$INTAKE")
DOMAIN_A=$(jq -r '.DOMAIN_A'          "$INTAKE")
```

## Step 2 — Render function

```bash
RENDERED_FILES=()

render_skill() {
  local TPL="$1"
  local OUT_NAME="$2"

  sed \
    -e "s|{{REPO_NAME}}|${REPO_NAME}|g" \
    -e "s|{{REPO_URL}}|${REPO_URL}|g" \
    -e "s|{{REPO_SLUG}}|${REPO_SLUG}|g" \
    -e "s|{{STACK}}|${STACK}|g" \
    -e "s|{{BUILD_CMD}}|${BUILD_CMD}|g" \
    -e "s|{{UNIT_TEST_CMD}}|${UNIT_TEST}|g" \
    -e "s|{{SMOKE_TYPE}}|${SMOKE_TYPE}|g" \
    -e "s|{{SECRETS_NEEDED}}|${SECRETS}|g" \
    -e "s|{{DOMAIN_A}}|${DOMAIN_A}|g" \
    "$TPL" > "${OUTPUT_DIR}/${OUT_NAME}"

  echo "  rendered: ${OUT_NAME}"
  RENDERED_FILES+=("${OUT_NAME}")
}
```

## Step 3 — Render each skill template

```bash
# Always render
render_skill "$TEMPLATE_DIR/env-setup.tpl.md"   "env-setup.md"
render_skill "$TEMPLATE_DIR/build-check.tpl.md" "build-check.md"
render_skill "$TEMPLATE_DIR/dev-server.tpl.md"  "dev-server.md"

# Conditional: smoke runner
if [ "$SMOKE_TYPE" != "none" ]; then
  render_skill "$TEMPLATE_DIR/smoke-runner.tpl.md" "smoke-runner.md"
fi

# Conditional: openapi extractor
if [ "$API_EXT" = "yes" ]; then
  render_skill "$TEMPLATE_DIR/openapi-extractor.tpl.md" "openapi-extractor.md"
fi

echo ""
echo "Total skill files rendered: ${#RENDERED_FILES[@]}"
ls -1 "$OUTPUT_DIR"
```

## Step 4 — Append to manifest

```bash
FILES_JSON=$(printf '%s\n' "${RENDERED_FILES[@]}" | jq -R . | jq -s .)

jq --argjson files "$FILES_JSON" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"skill-template-renderer","status":"done","timestamp":$ts,
          "output_dir":"/tmp/generated-skills","files":$files}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ skill-template-renderer complete. Manifest updated."
echo "Manifest entries so far: $(jq 'length' "$MANIFEST")"
```
