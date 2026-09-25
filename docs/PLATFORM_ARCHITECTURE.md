# NOTVISIBLEAI Platform Architecture

## Layers

1. Model research: NV model family, datasets, training and evaluation.
2. Model artifacts: immutable model versions and adapter/weight manifests.
3. Inference: dedicated model runner.
4. API gateway: authentication, validation, routing and rate limits.
5. Developer SDKs: Python and JavaScript clients.
6. Products: chat, agents, tools and multimodal interfaces.
7. Observability: latency, errors, token accounting and evaluation telemetry.
8. Safety: input/output policy enforcement, abuse controls and privacy.
9. Deployment: reproducible containers and GPU-capable serving infrastructure.

## Model/API boundary

The API must never silently substitute a base model for an NV release. A model is considered ready only after the artifact is present, evaluation succeeds, and the release manifest identifies the exact weights and tokenizer.

## Serving direction

For local development and moderate loads, Transformers provides an OpenAI-compatible server. For large-scale production, the serving backend can be replaced by a dedicated inference engine without changing the public API contract.
