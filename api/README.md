# NOTVISIBLEAI API Gateway

A lightweight gateway for NOTVISIBLEAI model serving.

## Run

```bash
python -m pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

Set `NV_INFERENCE_URL` to an OpenAI-compatible inference server. The gateway then routes `/v1/chat/completions` requests to that runner.

The gateway itself does not claim a model is ready until a runner is attached. This keeps the API layer separate from model training and prevents a base model from being mislabeled as an NV release.
