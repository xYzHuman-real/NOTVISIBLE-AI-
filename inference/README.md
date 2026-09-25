# NV-0.2 Inference Runner

This service loads the verified NV-0.2 LoRA adapter on top of Qwen2.5-0.5B-Instruct and exposes an OpenAI-compatible chat endpoint.

## Start

Place the downloaded training artifact so that the final adapter is available at:

`./artifacts/nv-0.2-lora/final`

Then:

```bash
pip install -r inference/requirements.txt
uvicorn inference.server:app --host 0.0.0.0 --port 8100
```

Or set `NV_ADAPTER_PATH` to another adapter location.

## Endpoints

- `GET /health`
- `GET /v1/models`
- `POST /v1/chat/completions`

The API returns the standard chat-completion response shape used by the NOTVISIBLEAI gateway.

## Architecture

Client → NOTVISIBLEAI API gateway → NV-0.2 inference runner → Qwen2.5 base + NV-0.2 LoRA adapter.

For larger production workloads, use a dedicated inference engine such as vLLM or SGLang after validating adapter compatibility and deployment requirements.
