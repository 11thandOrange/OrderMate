# Agent Template Renderer Skill

Reads `/tmp/intake-${REPO_SLUG}.json`, iterates the agent templates in
`.agents/templates/agents/`, substitutes all `{{TOKEN}}` placeholders, and
writes output files to `/tmp/generated-agents/`. Appends one entry to the
pipeline manifest.

## Step 1 — Load intake

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
TEMPLATE_DIR=".agents/templates/agents"
OUTPUT_DIR="/tmp/generated-agents"
mkdir -p "$OUTPUT_DIR"

REPO_URL=$(jq -r '.REPO_URL'                   "$INTAKE")
REPO_NAME=$(jq -r '.REPO_NAME'                 "$INTAKE")
REPO_SLUG=$(jq -r '.REPO_SLUG'                 "$INTAKE")
STACK=$(jq -r '.STACK'                          "$INTAKE")
DOMAIN_A=$(jq -r '.DOMAIN_A'                   "$INTAKE")
DOMAIN_B=$(jq -r '.DOMAIN_B'                   "$INTAKE")
SECONDARY=$(jq -r '.SECONDARY_IMPLEMENTER'     "$INTAKE")
SEC_NAME=$(jq -r '.SECONDARY_IMPLEMENTER_NAME' "$INTAKE")
BUILD_CMD=$(jq -r '.BUILD_CMD'                  "$INTAKE")
UNIT_TEST=$(jq -r '.UNIT_TEST_CMD'              "$INTAKE")
SMOKE_TYPE=$(jq -r '.SMOKE_TYPE'               "$INTAKE")
DOCS_SITE=$(jq -r '.DOCS_SITE'                 "$INTAKE")
API_EXT=$(jq -r '.API_EXTRACTION'               "$INTAKE")
```

## Step 2 — Determine which templates to render

```bash
SKIP_SECONDARY="false"
SKIP_DOCS="false"

[ "$SECONDARY" != "yes" ] && SKIP_SECONDARY="true"
[ "$DOCS_SITE" != "yes" ]  && SKIP_DOCS="true"

echo "SKIP_SECONDARY=$SKIP_SECONDARY  SKIP_DOCS=$SKIP_DOCS"
```

Docs agents to skip when `DOCS_SITE=no`:
`docs-agent.tpl.md`, `site-deployer.tpl.md`, `api-spec-generator.tpl.md`,
`changelog-agent.tpl.md`, `docs-writer.tpl.md`

## Step 3 — Render each template

```bash
RENDERED_FILES=()

render_template() {
  local TPL="$1"
  local OUT_NAME="$2"

  sed \
    -e "s|{{REPO_NAME}}|${REPO_NAME}|g" \
    -e "s|{{REPO_URL}}|${REPO_URL}|g" \
    -e "s|{{REPO_SLUG}}|${REPO_SLUG}|g" \
    -e "s|{{STACK}}|${STACK}|g" \
    -e "s|{{DOMAIN_A}}|${DOMAIN_A}|g" \
    -e "s|{{DOMAIN_B}}|${DOMAIN_B}|g" \
    -e "s|{{SECONDARY_IMPLEMENTER_NAME}}|${SEC_NAME}|g" \
    -e "s|{{BUILD_CMD}}|${BUILD_CMD}|g" \
    -e "s|{{UNIT_TEST_CMD}}|${UNIT_TEST}|g" \
    -e "s|{{SMOKE_TYPE}}|${SMOKE_TYPE}|g" \
    "$TPL" > "${OUTPUT_DIR}/${OUT_NAME}"

  echo "  rendered: ${OUT_NAME}"
  RENDERED_FILES+=("${OUT_NAME}")
}

# Always render
render_template "$TEMPLATE_DIR/orchestrator.tpl.md"         "orchestrator.md"
render_template "$TEMPLATE_DIR/ticket-planner.tpl.md"       "ticket-planner.md"
render_template "$TEMPLATE_DIR/implementer.tpl.md"          "implementer.md"
render_template "$TEMPLATE_DIR/tester.tpl.md"               "tester.md"
render_template "$TEMPLATE_DIR/pr-reviewer.tpl.md"          "pr-reviewer.md"
render_template "$TEMPLATE_DIR/smoke-tester.tpl.md"         "smoke-tester.md"
render_template "$TEMPLATE_DIR/build-release.tpl.md"        "build-release.md"
render_template "$TEMPLATE_DIR/code-auditor.tpl.md"         "code-auditor.md"
render_template "$TEMPLATE_DIR/postman-manager.tpl.md"      "postman-manager.md"

# Conditional: secondary implementer
if [ "$SKIP_SECONDARY" = "false" ]; then
  # Rename output to match the stack-specific agent name
  render_template "$TEMPLATE_DIR/secondary-implementer.tpl.md" "${SEC_NAME}.md"
fi

# Conditional: docs agents
if [ "$SKIP_DOCS" = "false" ]; then
  render_template "$TEMPLATE_DIR/docs-agent.tpl.md"           "docs-agent.md"
  render_template "$TEMPLATE_DIR/site-deployer.tpl.md"        "site-deployer.md"
  render_template "$TEMPLATE_DIR/changelog-agent.tpl.md"      "changelog-agent.md"
  render_template "$TEMPLATE_DIR/docs-writer.tpl.md"          "docs-writer.md"

  if [ "$API_EXT" = "yes" ]; then
    render_template "$TEMPLATE_DIR/api-spec-generator.tpl.md" "api-spec-generator.md"
  fi
fi

echo ""
echo "Total agent files rendered: ${#RENDERED_FILES[@]}"
ls -1 "$OUTPUT_DIR"
```

## Step 4 — Append to manifest

```bash
FILES_JSON=$(printf '%s\n' "${RENDERED_FILES[@]}" | jq -R . | jq -s .)

jq --argjson files "$FILES_JSON" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"agent-template-renderer","status":"done","timestamp":$ts,
          "output_dir":"/tmp/generated-agents","files":$files}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ agent-template-renderer complete. Manifest updated."
echo "Manifest entries so far: $(jq 'length' "$MANIFEST")"
```
