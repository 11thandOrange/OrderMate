from fastapi import APIRouter, HTTPException, Header
from pydantic import BaseModel
from typing import Optional
import httpx

router = APIRouter()

CLOVER_API_BASE = "https://api.clover.com"
CLOVER_SANDBOX_BASE = "https://sandbox.dev.clover.com"


class ProxyRequest(BaseModel):
    method: str
    path: str
    headers: Optional[dict] = None
    body: Optional[dict] = None
    use_sandbox: bool = False


class ProxyResponse(BaseModel):
    status: int
    status_text: str
    headers: dict
    data: dict
    time_ms: int


@router.post("/request", response_model=ProxyResponse)
async def proxy_request(
    request: ProxyRequest,
    authorization: Optional[str] = Header(None),
):
    """
    Proxy a request to the Clover API.
    The authorization header should be passed from the frontend.
    """
    base_url = CLOVER_SANDBOX_BASE if request.use_sandbox else CLOVER_API_BASE
    url = f"{base_url}{request.path}"

    # Build headers
    headers = request.headers or {}
    if authorization:
        headers["Authorization"] = authorization
    headers["Content-Type"] = "application/json"

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            import time
            start = time.time()

            response = await client.request(
                method=request.method,
                url=url,
                headers=headers,
                json=request.body if request.method in ["POST", "PUT", "PATCH"] else None,
            )

            elapsed = int((time.time() - start) * 1000)

            # Parse response
            try:
                data = response.json()
            except Exception:
                data = {"raw": response.text}

            return ProxyResponse(
                status=response.status_code,
                status_text=response.reason_phrase or "OK",
                headers=dict(response.headers),
                data=data,
                time_ms=elapsed,
            )

    except httpx.TimeoutException:
        raise HTTPException(status_code=504, detail="Request to Clover API timed out")
    except httpx.RequestError as e:
        raise HTTPException(status_code=502, detail=f"Error connecting to Clover API: {str(e)}")
