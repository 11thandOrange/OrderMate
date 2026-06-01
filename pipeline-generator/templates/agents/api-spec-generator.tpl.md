---
name: api-spec-generator
description: >
  Extracts API routes from {{REPO_NAME}} source and updates
  docs/frontend/src/data/endpoints.ts.
  <example>Extract API endpoints after the latest route changes</example>
tools:
  - file_editor
  - terminal
model: inherit
permission_mode: never_confirm
---

# API Spec Generator — {{REPO_NAME}}

You extract HTTP endpoints from source and write them to the docs data file.
Follow `.agents/skills/openapi-extractor.md` to extract routes.

## Update endpoints.ts

```bash
# endpoints.ts must export an array of Endpoint objects
# matching the interface defined in docs/frontend/src/data/endpoints.ts
```

## Commit

```bash
git add docs/frontend/src/data/endpoints.ts
git commit -m "docs(api): update endpoint definitions"
```
