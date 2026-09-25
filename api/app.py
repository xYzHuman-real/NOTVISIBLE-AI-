import os
import time
from collections import defaultdict, deque
from typing import Any

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field\nfrom fastapi.middleware.cors import CORSMiddleware

APP_NAME = "NOTVISIBLEAI API"
API_VERSION = "0.1.0"
MODEL_ID = os.getenv("NV_MODEL_ID", "nv-0.2")
MODEL_BACKEND = os.getenv("NV_MODEL_BACKEND", "transformers")
API_KEY = os.getenv("NV_API_KEY", "")
INFERENCE_URL = os.getenv("NV_INFERENCE_URL", "").rstrip("/")

app = FastAPI(title=APP_NAME, version=API_VERSION)\nCORS_ORIGINS = [x.strip() for x in os.getenv("NV_CORS_ORIGINS", "*").split(",") if x.strip()]\napp.add_middleware(\n    CORSMiddleware,\n    allow_origins=CORS_ORIGINS,\n    allow_credentials=False,\n    allow_methods=["GET", "POST", "OPTIONS"],\n    allow_headers=["Authorization", "Content-Type"],\n)
_windows: dict[str, deque[float]] = defaultdict(deque)
RATE_LIMIT = int(os.getenv("NV_RATE_LIMIT", "60"))
RATE_WINDOW = 60

class Message(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    model: str = MODEL_ID
    messages: list[Message]
    temperature: float = Field(0.7, ge=0, le=2)
    max_tokens: int = Field(256, ge=1, le=4096)
    stream: bool = False

def authenticate(authorization: str | None = Header(default=None)) -> str:
    if not API_KEY:
        return "development"
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Missing API key")
    if authorization.removeprefix("Bearer ").strip() != API_KEY:
        raise HTTPException(401, "Invalid API key")
    return "authenticated"

def rate_limit(identity: str) -> None:
    now = time.time()
    q = _windows[identity]
    while q and q[0] <= now - RATE_WINDOW:
        q.popleft()
    if len(q) >= RATE_LIMIT:
        raise HTTPException(429, "Rate limit exceeded")
    q.append(now)

@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": APP_NAME,
        "version": API_VERSION,
        "inference_attached": bool(INFERENCE_URL),
    }

@app.get("/v1/models")
def models(_: str = Depends(authenticate)):
    return {"object": "list", "data": [{
        "id": MODEL_ID,
        "object": "model",
        "owned_by": "notvisibleai",
        "backend": MODEL_BACKEND,
        "ready": bool(INFERENCE_URL),
    }]}

@app.post("/v1/chat/completions")
async def chat(req: ChatRequest, identity: str = Depends(authenticate)):
    rate_limit(identity)
    if req.model != MODEL_ID:
        raise HTTPException(404, f"Unknown model: {req.model}")
    if not INFERENCE_URL:
        raise HTTPException(503, "No inference runner is attached")
    if req.stream:
        raise HTTPException(501, "Streaming gateway is not enabled yet")
    payload = req.model_dump()
    try:
        async with httpx.AsyncClient(timeout=120) as client:
            response = await client.post(f"{INFERENCE_URL}/v1/chat/completions", json=payload)
        response.raise_for_status()
        return response.json()
    except httpx.HTTPStatusError as exc:
        raise HTTPException(502, f"Inference runner returned HTTP {exc.response.status_code}")
    except httpx.HTTPError as exc:
        raise HTTPException(502, f"Inference runner unavailable: {exc}") from exc

@app.get("/")
def root():
    return {
        "name": APP_NAME,
        "version": API_VERSION,
        "docs": "/docs",
        "health": "/health",
        "models": "/v1/models",
        "chat": "/v1/chat/completions",
    }
