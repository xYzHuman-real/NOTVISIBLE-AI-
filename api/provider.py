import os
from typing import Any

import httpx

PROVIDER_URL = os.getenv("NV_PROVIDER_URL", "").rstrip("/")
PROVIDER_KEY = os.getenv("NV_PROVIDER_KEY", "")
PROVIDER_MODEL = os.getenv("NV_PROVIDER_MODEL", "")

async def provider_chat(payload: dict[str, Any]) -> dict[str, Any]:
    if not PROVIDER_URL:
        raise RuntimeError("No remote provider configured")
    headers = {"Content-Type": "application/json"}
    if PROVIDER_KEY:
        headers["Authorization"] = "Bearer " + PROVIDER_KEY
    body = dict(payload)
    if PROVIDER_MODEL:
        body["model"] = PROVIDER_MODEL
    async with httpx.AsyncClient(timeout=120) as client:
        response = await client.post(PROVIDER_URL + "/v1/chat/completions", json=body, headers=headers)
        response.raise_for_status()
        return response.json()
