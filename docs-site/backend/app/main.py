from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routes import proxy, mock

app = FastAPI(
    title="OrderMate Docs API",
    description="Backend API for OrderMate documentation sandbox",
    version="1.0.0",
)

# CORS middleware for frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:12001",
        "http://localhost:3000",
        "https://*.all-hands.dev",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(proxy.router, prefix="/api/proxy", tags=["Proxy"])
app.include_router(mock.router, prefix="/api/mock", tags=["Mock"])


@app.get("/")
async def root():
    return {
        "name": "OrderMate Docs API",
        "version": "1.0.0",
        "docs": "/docs",
    }


@app.get("/health")
async def health():
    return {"status": "healthy"}
