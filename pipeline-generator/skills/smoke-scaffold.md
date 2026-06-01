# Smoke Scaffold Skill

Creates the smoke test infrastructure files that the CI smoke job and
`smoke-tester` agent depend on. Output goes to `/tmp/generated-smoke/`
(Espresso) or patches `web/frontend/` directly (Playwright).
Appends one entry to the manifest.

## Step 1 — Load intake

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
OUTPUT_DIR="/tmp/generated-smoke"
mkdir -p "$OUTPUT_DIR"

REPO_NAME=$(jq -r '.REPO_NAME'   "$INTAKE")
SMOKE_TYPE=$(jq -r '.SMOKE_TYPE' "$INTAKE")
STACK=$(jq -r '.STACK'           "$INTAKE")
```

## Step 2 — Scaffold by smoke type

### Espresso (android-kotlin)

```bash
if [ "$SMOKE_TYPE" = "espresso" ]; then

  PACKAGE_PATH="app/src/androidTest/java/com/$(echo "$REPO_NAME" | tr '[:upper:]' '[:lower:]')/smoke"
  mkdir -p "${OUTPUT_DIR}/${PACKAGE_PATH}"
  PACKAGE_NAME="com.$(echo "$REPO_NAME" | tr '[:upper:]' '[:lower:]').smoke"

  cat > "${OUTPUT_DIR}/${PACKAGE_PATH}/BaseSmokeTest.kt" << KOTLIN
package ${PACKAGE_NAME}

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Base class for ${REPO_NAME} smoke tests.
 * Provides screenshot capture at each AC checkpoint.
 */
abstract class BaseSmokeTest {

    protected fun takeScreenshot(name: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .takeScreenshot()
            ?.let { bitmap ->
                val file = File("/sdcard/screenshots/\${name}.png")
                file.parentFile?.mkdirs()
                FileOutputStream(file).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
    }
}
KOTLIN

  echo "  created: ${PACKAGE_PATH}/BaseSmokeTest.kt"
  CREATED_FILES=("${PACKAGE_PATH}/BaseSmokeTest.kt")

fi
```

### Playwright (node-shopify)

```bash
if [ "$SMOKE_TYPE" = "playwright" ]; then

  SMOKE_DIR="${OUTPUT_DIR}/web/frontend/tests/smoke"
  mkdir -p "$SMOKE_DIR"

  # Playwright config
  cat > "${OUTPUT_DIR}/web/frontend/playwright.config.js" << 'JS'
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir:   './tests/smoke',
  timeout:   30000,
  use: {
    baseURL:    'http://localhost:4000',
    viewport:   { width: 1280, height: 800 },
    screenshot: 'only-on-failure',
  },
  webServer: {
    command:            'SHOPIFY_API_KEY=smoke-test-key npm run dev',
    url:                'http://localhost:4000',
    reuseExistingServer: !process.env.CI,
    timeout:             30000,
  },
});
JS

  # Base smoke test
  cat > "${SMOKE_DIR}/base.smoke.js" << 'JS'
import { test, expect } from '@playwright/test';

test.describe('base smoke', () => {

  test('app shell loads without crash', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('body')).toBeVisible();
    await page.screenshot({ path: '.smoke-results/base-home.png', fullPage: true });
  });

});
JS

  # package.json smoke scripts patch — printed as instruction since we can't
  # modify the existing package.json without cloning the repo
  cat > "${OUTPUT_DIR}/PATCH-web-frontend-package.json.txt" << 'TXT'
Add the following scripts to web/frontend/package.json:

  "smoke":         "playwright test --config=playwright.config.js",
  "smoke:install": "playwright install chromium --with-deps"
TXT

  echo "  created: web/frontend/playwright.config.js"
  echo "  created: web/frontend/tests/smoke/base.smoke.js"
  echo "  created: PATCH-web-frontend-package.json.txt (apply manually after clone)"
  CREATED_FILES=(
    "web/frontend/playwright.config.js"
    "web/frontend/tests/smoke/base.smoke.js"
    "PATCH-web-frontend-package.json.txt"
  )

fi
```

## Step 3 — Append to manifest

```bash
FILES_JSON=$(printf '%s\n' "${CREATED_FILES[@]}" | jq -R . | jq -s .)

jq --argjson files "$FILES_JSON" \
   --arg smoke "$SMOKE_TYPE" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"smoke-scaffold","status":"done","timestamp":$ts,
          "smoke_type":$smoke,"output_dir":"/tmp/generated-smoke","files":$files}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ smoke-scaffold complete. Manifest updated."
echo "Manifest entries so far: $(jq 'length' "$MANIFEST")"
```
