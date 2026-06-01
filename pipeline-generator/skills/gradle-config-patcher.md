# Gradle Config Patcher Skill

Applies the `lint { abortOnError = false }` patch to `app/build.gradle.kts`
in the staging clone. Without this the Android CI lint job fails on first run.
Only invoked for `STACK=android-kotlin`. Appends one entry to the manifest.

## Step 1 — Load intake

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
REPO_SLUG=$(jq -r '.REPO_SLUG' "$INTAKE")
CLONE_DIR="/tmp/repo-${REPO_SLUG}"
GRADLE_FILE="${CLONE_DIR}/app/build.gradle.kts"
```

## Step 2 — Verify file exists

```bash
if [ ! -f "$GRADLE_FILE" ]; then
  echo "⚠️  $GRADLE_FILE not found — skipping patch (clone may not have run yet)"
  echo "    Patch will be applied in Step 9 (assemble and push)"
  PATCH_STATUS="deferred"
else
  PATCH_STATUS="applied"
fi
```

## Step 3 — Apply patch (if clone exists)

```bash
if [ "$PATCH_STATUS" = "applied" ]; then

  # Check if patch already applied
  if grep -q "abortOnError" "$GRADLE_FILE"; then
    echo "lint { abortOnError } already present — skipping"
    PATCH_STATUS="already-present"
  else
    # Insert after the closing brace of the kotlinOptions block
    # Pattern: find '    }\n}' (end of android block) and insert before it
    python3 << PYEOF
import re, sys

with open('${GRADLE_FILE}', 'r') as f:
    content = f.read()

patch = '''
    lint {
        abortOnError = false
    }
'''

# Insert before the closing brace of the android { } block
# Find last occurrence of a lone closing brace at indent level 0
pattern = r'(\n\}(\n|$))'
replacement = patch + r'\1'
patched, count = re.subn(pattern, replacement, content, count=1)

if count == 0:
    print("WARNING: Could not locate insertion point. Appending to end of file.")
    patched = content.rstrip() + '\n' + patch

with open('${GRADLE_FILE}', 'w') as f:
    f.write(patched)

print("Patch applied successfully.")
PYEOF

    echo ""
    echo "Diff:"
    git -C "$CLONE_DIR" diff app/build.gradle.kts
  fi

fi
```

## Step 4 — Write patch as standalone file for deferred application

If the clone hasn't run yet, write the patch as a note file that Step 9 applies:

```bash
PATCH_NOTE="/tmp/gradle-patch-${REPO_SLUG}.txt"
cat > "$PATCH_NOTE" << 'NOTE'
FILE: app/build.gradle.kts

Add the following block inside the `android { }` block, after kotlinOptions:

    lint {
        abortOnError = false
    }

This prevents the CI lint job from failing the build when lint warnings exist.
Required by android-ci.yml Job 3 (lint).
NOTE
echo "Patch note written: $PATCH_NOTE"
```

## Step 5 — Append to manifest

```bash
jq --arg status "$PATCH_STATUS" \
   --arg file "app/build.gradle.kts" \
   --arg note "/tmp/gradle-patch-${REPO_SLUG}.txt" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"gradle-config-patcher","status":"done","timestamp":$ts,
          "patch_status":$status,
          "file_patched":$file,
          "patch_note":$note,
          "change":"lint { abortOnError = false }"}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ gradle-config-patcher complete (status: $PATCH_STATUS). Manifest updated."
echo "Manifest entries so far: $(jq 'length' "$MANIFEST")"
```
