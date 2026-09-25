# NOTVISIBLEAI — Local Product Stack

## 1. Obtain the verified NV-0.2 artifact

Download the GitHub Actions artifact named `nv-0.2-training-output` from the verified training run and extract it so this path exists:

`artifacts/nv-0.2-lora/final/`

GitHub documents workflow-artifact downloads and their retention rules. See the official GitHub Actions documentation.

## 2. Start the stack

Create an environment file or export the required key:

```bash
export NV_API_KEY="$(python -c 'import secrets; print("nv-"+secrets.token_urlsafe(32))')"
docker compose up --build
```

Services:
- Web: http://localhost:8080
- API: http://localhost:8000
- Inference: http://localhost:8100

## 3. Verify

```bash
curl http://localhost:8000/health
curl -H "Authorization: Bearer $NV_API_KEY" http://localhost:8000/v1/models
```

For chat:

```bash
curl -X POST http://localhost:8000/v1/chat/completions \
  -H "Authorization: Bearer $NV_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"nv-0.2","messages":[{"role":"user","content":"Explain what an API is in one paragraph."}]}'
```

The compose stack is intended for development/self-hosting. Production deployment still requires TLS, persistent observability, secret management, backups and an operational hosting environment.
