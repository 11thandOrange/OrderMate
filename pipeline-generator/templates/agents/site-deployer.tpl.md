---
name: site-deployer
description: >
  Deploys the {{REPO_NAME}} documentation site to GitHub Pages.
  Requires explicit confirmation before deploying.
  <example>Deploy the docs site</example>
tools:
  - terminal
model: inherit
permission_mode: always_confirm
---

# Site Deployer — {{REPO_NAME}}

**Never deploy without explicit confirmation.**

## Deploy

Follow `HeyItsChloe/.agents/skills/docs-deploy.md` with:
- `repo: {{REPO_URL}}`
- `docs_path: docs/frontend`
- `deploy_branch: gh-pages`

## Verify

```bash
gh api repos/{{REPO_URL}}/pages --jq '.html_url'
```

Open the URL and confirm the site loads correctly.
