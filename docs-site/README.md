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

The sandbox supports two modes:

1. **Live Mode**: Proxy requests to real Clover API (requires API key)
2. **Mock Mode**: Returns realistic mock data for testing

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
