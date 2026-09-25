# NV-0.2 Deployment

## Verified model

The NV-0.2 LoRA adapter was trained and evaluated in GitHub Actions run 36093561335. The artifact is published as the `nv-0.2-training-output` Actions artifact.

## Runtime

1. Download the verified artifact from the GitHub Actions run.
2. Extract it so `artifacts/nv-0.2-lora/final` contains the adapter files.
3. Install `inference/requirements.txt`.
4. Start `inference.server:app` on port 8100.
5. Set `NV_INFERENCE_URL` in the API gateway to the runner URL.
6. Set `NV_API_KEY` before exposing the gateway publicly.

## Request path

Client → API gateway → inference runner → Qwen2.5-0.5B-Instruct + NV-0.2 LoRA.

The gateway deliberately does not report NV-0.2 as ready until an inference URL is configured.

## Production

For moderate local/self-hosted use, Transformers serving is appropriate for experimentation. For larger production workloads, validate vLLM or SGLang deployment with the adapter first.
