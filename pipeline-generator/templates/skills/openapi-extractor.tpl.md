# OpenAPI Extractor Skill — {{REPO_NAME}}

Extracts HTTP route definitions from {{REPO_NAME}} source files and formats
them as TypeScript `Endpoint` objects for the docs site.

## Extract Routes

```bash
# Find route definition files
find {{DOMAIN_A}} -name "*.kt" -o -name "*.js" -o -name "routes*" 2>/dev/null | head -20

# Extract HTTP method + path patterns
grep -rn "@GET\|@POST\|@PUT\|@DELETE\|router\.\(get\|post\|put\|delete\)" \
  {{DOMAIN_A}} 2>/dev/null | head -40
```

## Format as Endpoints

For each route found, produce a TypeScript `Endpoint` entry:

```typescript
{
  method:      'GET',
  path:        '/api/resource/{id}',
  description: '<description from code comment or route name>',
  params:      { id: 'Resource identifier' },
}
```

## Write to endpoints.ts

Update `docs/frontend/src/data/endpoints.ts` — append new entries,
update changed entries, do not remove entries without verification.
