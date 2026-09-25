# NOTVISIBLEAI API

NOTVISIBLEAI exposes an OpenAI-compatible shape for model access.

## Endpoints

- GET /health
- GET /v1/models
- POST /v1/chat/completions

The API gateway handles authentication, rate limiting, model routing, and request validation. Model inference remains a separate service so it can later run behind Transformers, vLLM, SGLang, or another supported inference backend.

## Local development

```bash
cd api
python -m pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

Then open /docs.

## Production boundary

Do not expose the development configuration publicly. Production deployment must use a secret API key store, TLS, persistent usage accounting, stronger rate limiting, request logging with privacy controls, and an actual model runner.

The current API intentionally returns 501 for inference until a verified NV model artifact is attached. This prevents accidentally presenting the base model or an unavailable adapter as NV-0.2.

Hugging Face documents OpenAI-compatible serving endpoints such as /v1/chat/completions and /v1/models, which is the compatibility target for this interface.
