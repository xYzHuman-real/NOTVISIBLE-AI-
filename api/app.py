import os
import time
import uuid
from collections import defaultdict, deque
from typing import Any

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from pydantic import BaseModel, Field

APP_NAME = "NOTVISIBLEAI API"
API_VERSION = "0.1.0"
MODEL_ID = os.getenv("NV_MODEL_ID", "nv-0.2")
MODEL_BACKEND = os.getenv("NV_MODEL_BACKEND", "transformers")
API_KEY = os.getenv("NV_API_KEY", "")

app = FastAPI(title=APP_NAME, version=API_VERSION)

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

class ChatResponse(BaseModel):
    id: str
    object: str = "chat.completion"
    created: int
    model: str
    choices: list[dict[str, Any]]
    usage: dict[str, int]

def authenticate(authorization: str | None = Header(default=None)) -> str:
    if not API_KEY:
        return "development"
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Missing API key")
    token = authorization.removeprefix("Bearer ").strip()
    if token != API_KEY:
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
    return {"status": "ok", "service": APP_NAME, "version": API_VERSION}

@app.get("/v1/models")
def models(_: str = Depends(authenticate)):
    return {"object": "list", "data": [{
        "id": MODEL_ID,
        "object": "model",
        "owned_by": "notvisibleai",
        "backend": MODEL_BACKEND,
        "ready": os.getenv("NV_MODEL_READY", "false").lower() == "true",
    }]}

@app.post("/v1/chat/completions", response_model=ChatResponse)
def chat(req: ChatRequest, request: Request, identity: str = Depends(authenticate)):
    rate_limit(identity)
    if req.model != MODEL_ID:
        raise HTTPException(404, f"Unknown model: {req.model}")
    if not os.getenv("NV_MODEL_READY", "false").lower() == "true":
        raise HTTPException(503, "NV model is not ready; API infrastructure is online")
    # The model runner is deliberately separated from the API gateway.
    # Set NV_INFERENCE_URL or replace this adapter with the deployed runner.
    raise HTTPException(501, "Inference runner not attached")

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
