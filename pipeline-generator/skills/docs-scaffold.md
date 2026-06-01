# Docs Scaffold Skill

Creates the `docs/` site skeleton (React + Vite + TypeScript + Tailwind) that
`docs-agent`, `api-spec-generator`, `changelog-agent`, `site-deployer`, and the
`deploy-docs.yml` workflow all depend on. Output goes to `/tmp/generated-docs/`.
Appends one entry to the manifest.

## Step 1 — Load intake

```bash
INTAKE="/tmp/intake-${REPO_SLUG}.json"
MANIFEST="/tmp/pipeline-manifest-${REPO_SLUG}.json"
OUTPUT_DIR="/tmp/generated-docs"
FRONTEND="${OUTPUT_DIR}/docs/frontend"
BACKEND="${OUTPUT_DIR}/docs/backend"
mkdir -p "${FRONTEND}/src/data" \
         "${FRONTEND}/src/pages/Api" \
         "${FRONTEND}/src/components" \
         "${BACKEND}/app/routes"

REPO_NAME=$(jq -r '.REPO_NAME' "$INTAKE")
REPO_URL=$(jq -r '.REPO_URL'   "$INTAKE")
```

## Step 2 — Create docs/frontend files

```bash
# package.json
cat > "${FRONTEND}/package.json" << PKG
{
  "name": "$(echo "$REPO_NAME" | tr '[:upper:]' '[:lower:]')-docs",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev":     "vite",
    "build":   "tsc && vite build",
    "preview": "vite preview",
    "deploy":  "gh-pages -d dist"
  },
  "dependencies": {
    "react":            "^18.2.0",
    "react-dom":        "^18.2.0",
    "react-router-dom": "^6.20.0"
  },
  "devDependencies": {
    "@types/react":        "^18.2.0",
    "@types/react-dom":    "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0",
    "autoprefixer":        "^10.4.16",
    "gh-pages":            "^6.1.0",
    "postcss":             "^8.4.32",
    "tailwindcss":         "^3.4.0",
    "typescript":          "^5.2.0",
    "vite":                "^5.0.0"
  }
}
PKG

# index.html
cat > "${FRONTEND}/index.html" << HTML
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${REPO_NAME} Docs</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
HTML

# src/main.tsx
cat > "${FRONTEND}/src/main.tsx" << TSX
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
TSX

# src/App.tsx
cat > "${FRONTEND}/src/App.tsx" << TSX
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ApiOverview from './pages/Api/ApiOverview';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ApiOverview />} />
      </Routes>
    </BrowserRouter>
  );
}
TSX

# src/index.css
cat > "${FRONTEND}/src/index.css" << CSS
@tailwind base;
@tailwind components;
@tailwind utilities;
CSS

# src/data/endpoints.ts  — filled by api-spec-generator going forward
cat > "${FRONTEND}/src/data/endpoints.ts" << TS
export interface Endpoint {
  method:      'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  path:        string;
  description: string;
  params?:     Record<string, string>;
  body?:       Record<string, string>;
  response?:   string;
}

export const endpoints: Endpoint[] = [];
TS

# src/data/navigation.ts
cat > "${FRONTEND}/src/data/navigation.ts" << TS
export interface NavItem {
  label: string;
  path:  string;
}

export const navigation: NavItem[] = [
  { label: 'API Overview', path: '/' },
];
TS

# src/pages/Api/ApiOverview.tsx
cat > "${FRONTEND}/src/pages/Api/ApiOverview.tsx" << TSX
export default function ApiOverview() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold mb-4">${REPO_NAME} API Reference</h1>
      <p className="text-gray-600">
        Documentation is generated automatically. Run the <code>api-spec-generator</code> agent to populate endpoints.
      </p>
    </div>
  );
}
TSX
```

## Step 3 — Create docs/backend mock server

```bash
# requirements.txt
cat > "${BACKEND}/requirements.txt" << REQ
fastapi>=0.104
uvicorn>=0.24
httpx>=0.25
REQ

# app/__init__.py
touch "${BACKEND}/app/__init__.py"
touch "${BACKEND}/app/routes/__init__.py"

# app/main.py
cat > "${BACKEND}/app/main.py" << PY
from fastapi import FastAPI
from app.routes import mock

app = FastAPI(title="${REPO_NAME} Docs Backend")
app.include_router(mock.router)

@app.get("/health")
def health():
    return {"status": "ok"}
PY

# app/routes/mock.py
cat > "${BACKEND}/app/routes/mock.py" << PY
from fastapi import APIRouter

router = APIRouter(prefix="/api/mock")

@router.get("/{path:path}")
def mock_get():
    return {}

@router.post("/{path:path}")
def mock_post():
    return {}
PY
```

## Step 4 — Collect created files and append to manifest

```bash
CREATED_FILES=()
while IFS= read -r -d '' f; do
  CREATED_FILES+=("${f#${OUTPUT_DIR}/}")
done < <(find "$OUTPUT_DIR" -type f -print0)

echo ""
echo "docs/ scaffold files created: ${#CREATED_FILES[@]}"
printf '  %s\n' "${CREATED_FILES[@]}"

FILES_JSON=$(printf '%s\n' "${CREATED_FILES[@]}" | jq -R . | jq -s .)

jq --argjson files "$FILES_JSON" \
   --arg ts "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '. + [{"step":"docs-scaffold","status":"done","timestamp":$ts,
          "output_dir":"/tmp/generated-docs","files":$files}]' \
   "$MANIFEST" > "${MANIFEST}.tmp" && mv "${MANIFEST}.tmp" "$MANIFEST"

echo "✅ docs-scaffold complete. Manifest updated."
echo "Manifest entries so far: $(jq 'length' "$MANIFEST")"
```
