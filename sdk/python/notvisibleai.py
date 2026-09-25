from __future__ import annotations

import requests

class NotVisibleAI:
    def __init__(self, api_key: str, base_url: str = "http://localhost:8000"):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.headers.update({"Authorization": f"Bearer {api_key}"})

    def models(self):
        r = self.session.get(f"{self.base_url}/v1/models", timeout=30)
        r.raise_for_status()
        return r.json()

    def chat(self, messages, model="nv-0.2", temperature=0.7, max_tokens=256):
        r = self.session.post(
            f"{self.base_url}/v1/chat/completions",
            json={
                "model": model,
                "messages": messages,
                "temperature": temperature,
                "max_tokens": max_tokens,
                "stream": False,
            },
            timeout=120,
        )
        r.raise_for_status()
        return r.json()
