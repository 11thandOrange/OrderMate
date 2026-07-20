# OrderMate Documentation Site

Interactive API documentation for OrderMate, built with React + TypeScript and Python FastAPI.

## Architecture

```
docs-site/
├── frontend/           # React + TypeScript + Tailwind
│   ├── src/
│   │   ├── components/ # Reusable UI components
│   │   ├── pages/      # Page components
│   │   ├── data/       # API endpoint definitions
│   │   └── types/      # TypeScript types
│   └── ...
│
├── backend/            # Python FastAPI
│   └── app/
│       ├── routes/     # API routes (proxy, mock)
│       └── services/   # Business logic
│
└── README.md
```

## Features

- **Stripe-style 3-panel layout**: Navigation | Documentation | Interactive Sandbox
- **Interactive API Sandbox**: Test API endpoints directly in the browser
- **Code examples**: Auto-generated cURL, Python, and Kotlin snippets
- **Mock mode**: Test without a real Clover account
- **Dark theme**: Matches OrderMate app branding

## Getting Started

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:12001

### Backend

```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

API docs at http://localhost:8000/docs

## Pages

| Route | Description |
|-------|-------------|
| `/` | Home - Documentation landing page |
| `/getting-started` | Installation and setup guide |
| `/features/*` | Feature documentation |
| `/api` | API Reference overview |
| `/api/orders` | Orders API with sandbox |
| `/api/customers` | Customers API |
| `/api/payments` | Payments API |
| `/api/webhooks` | Webhooks API |

## API Sandbox

The "Try it" panel on each endpoint page (`RequestBuilder.tsx`) calls the backend at
`import.meta.env.VITE_DOCS_API_URL` (defaults to `http://localhost:8000`):

1. **Live Mode**: if an API key is entered, POSTs to `backend/app/routes/proxy.py`,
   which forwards the request to the real Clover API with that key
2. **Mock Mode**: with no API key, POSTs to `backend/app/routes/mock.py`, which
   returns realistic mock data for testing

If the backend isn't reachable at all (a network error - e.g. the deployed static
GitHub Pages build, which has no backend behind it), the panel falls back to a
canned response built from the endpoint's `exampleResponse` in `endpoints.ts`, so
"Try it" still shows something rather than failing.

## Development

### Adding a new API endpoint

1. Add endpoint definition to `frontend/src/data/endpoints.ts`
2. Create page component in `frontend/src/pages/Api/`
3. Add route to `frontend/src/App.tsx`
4. Add mock handler in `backend/app/routes/mock.py` (optional)

### Styling

- Uses Tailwind CSS with OrderMate brand colors
- Custom theme in `frontend/tailwind.config.js`
- Global styles in `frontend/src/index.css`

## Deployment

### GitHub Pages (Frontend only)

```bash
cd frontend
npm run build
# Deploy dist/ to GitHub Pages
```

### Full Stack

Use Docker Compose or deploy frontend and backend separately.

## License

Copyright © 2026 11th and Orange. All rights reserved.
