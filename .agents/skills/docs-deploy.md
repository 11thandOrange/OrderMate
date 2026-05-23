# Docs Deploy Skill

Deploy the OrderMate documentation site to GitHub Pages using GitHub Actions.

## Quick Commands

### Build Site
```bash
cd docs-site/frontend && npm ci && npm run build
```

### Deploy via gh-pages
```bash
cd docs-site/frontend && npx gh-pages -d dist
```

### Trigger GitHub Action
```bash
gh workflow run deploy-docs.yml
```

### Check Deployment Status
```bash
gh api repos/11thandOrange/OrderMate/pages --jq '.status'
```

## GitHub Actions Workflow

Create `.github/workflows/deploy-docs.yml`:

```yaml
name: Deploy Docs Site

on:
  push:
    branches: [main]
    paths:
      - 'docs-site/**'
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: false

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: docs-site/frontend/package-lock.json

      - name: Install dependencies
        working-directory: docs-site/frontend
        run: npm ci

      - name: Build
        working-directory: docs-site/frontend
        run: npm run build

      - name: Setup Pages
        uses: actions/configure-pages@v4

      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: docs-site/frontend/dist

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

## Vite Configuration

Ensure `docs-site/frontend/vite.config.ts` has:

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  base: '/OrderMate/',  // Must match repo name
  plugins: [react()],
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
});
```

## GitHub Pages Setup

### Enable GitHub Pages
1. Go to repo Settings → Pages
2. Source: GitHub Actions
3. Save

### Verify Settings via API
```bash
gh api repos/11thandOrange/OrderMate/pages
```

### Expected Response
```json
{
  "url": "https://11thandorange.github.io/OrderMate/",
  "status": "built",
  "source": {
    "branch": "gh-pages",
    "path": "/"
  }
}
```

## Manual Deployment Steps

If GitHub Actions is not available:

```bash
# 1. Build the site
cd docs-site/frontend
npm ci
npm run build

# 2. Create/checkout gh-pages branch
git checkout --orphan gh-pages

# 3. Remove all files
git rm -rf .

# 4. Copy dist contents
cp -r docs-site/frontend/dist/* .

# 5. Add .nojekyll to bypass Jekyll processing
touch .nojekyll

# 6. Commit and push
git add .
git commit -m "Deploy docs site"
git push origin gh-pages --force

# 7. Return to main
git checkout main
```

## Deployment Verification

### Check Site is Live
```bash
curl -s -o /dev/null -w "%{http_code}" https://11thandorange.github.io/OrderMate/
# Should return 200
```

### Check Specific Routes
```bash
# Home page
curl -I https://11thandorange.github.io/OrderMate/

# API docs (SPA route, should return index.html)
curl -I https://11thandorange.github.io/OrderMate/api/orders
```

### Monitor Workflow
```bash
# Watch latest run
gh run watch

# List recent runs
gh run list --workflow=deploy-docs.yml --limit=5
```

## Troubleshooting

### 404 on Routes
Add `404.html` that redirects to `index.html`:

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <script>
    // Redirect to index.html with path preserved
    sessionStorage.redirect = location.href;
    location.replace(location.origin + '/OrderMate/');
  </script>
</head>
</html>
```

### Assets Not Loading
Check browser console for 404s. Common fixes:
- Verify `base` in vite.config.ts
- Ensure paths use relative URLs or start with base path

### Stale Cache
GitHub Pages caches aggressively. Wait 5-10 minutes or:
```bash
# Hard refresh in browser: Ctrl+Shift+R
# Or purge via API (if available)
```

### Build Fails in CI
Check:
- Node version matches local
- Dependencies are committed (package-lock.json)
- No missing env vars

## Environment Requirements

- Node.js 18+
- npm 9+
- GitHub token with `pages:write` permission
