# Workflow Renderer Skill

Renders GitHub Actions workflow templates for the target repo's stack. Writes
`.yml` files to `/tmp/generated-workflows/`. Appends one entry to the manifest.

## Step 1 — Load intake

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
TEMPLATE_DIR=".agents/templates/workflows"
OUTPUT_DIR="/tmp/generated-workflows"
mkdir -p "$OUTPUT_DIR"

REPO_URL=$(jq -r '.REPO_URL'    "$INTAKE")
REPO_NAME=$(jq -r '.REPO_NAME'  "$INTAKE")
REPO_SLUG=$(jq -r '.REPO_SLUG'  "$INTAKE")
STACK=$(jq -r '.STACK'          "$INTAKE")
DOCS_SITE=$(jq -r '.DOCS_SITE'  "$INTAKE")
SMOKE_TYPE=$(jq -r '.SMOKE_TYPE' "$INTAKE")
```

## Step 2 — Render CI workflow (stack-specific)

```bash
RENDERED_FILES=()

render_workflow() {
  local TPL="$1"
  local OUT_NAME="$2"

  sed \
    -e "s|{{REPO_NAME}}|${REPO_NAME}|g" \
    -e "s|{{REPO_URL}}|${REPO_URL}|g" \
    -e "s|{{REPO_SLUG}}|${REPO_SLUG}|g" \
    -e "s|{{STACK}}|${STACK}|g" \
    -e "s|{{SMOKE_TYPE}}|${SMOKE_TYPE}|g" \
    "$TPL" > "${OUTPUT_DIR}/${OUT_NAME}"

  echo "  rendered: ${OUT_NAME}"
  RENDERED_FILES+=("${OUT_NAME}")
}

case "$STACK" in
  android-kotlin)
    render_workflow "$TEMPLATE_DIR/android-ci.tpl.yml" "android-ci.yml"
    ;;
  node-shopify)
    render_workflow "$TEMPLATE_DIR/node-ci.tpl.yml" "node-ci.yml"
    ;;
  *)
    echo "⚠️  No CI template for stack '$STACK'. Skipping CI workflow."
    ;;
esac

# docs deploy workflow — any stack with a docs site
if [ "$DOCS_SITE" = "yes" ]; then
  render_workflow "$TEMPLATE_DIR/deploy-docs.tpl.yml" "deploy-docs.yml"
fi

echo ""
echo "Total workflow files rendered: ${#RENDERED_FILES[@]}"
ls -1 "$OUTPUT_DIR"
```

## Step 3 — Append to manifest

```bash
FILES_JSON=$(printf '%s\n' "${RENDERED_FILES[@]}" | jq -R . | jq -s .)

jq --argjson files "$FILES_JSON" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"workflow-renderer","status":"done","timestamp":$ts,
          "output_dir":"/tmp/generated-workflows","files":$files}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ workflow-renderer complete. Manifest updated."
echo "Manifest entries so far: $(jq 'length' "$MANIFEST")"
```
