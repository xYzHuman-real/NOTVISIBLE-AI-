# NOTVISIBLEAI Roadmap

## NV-0.2 — current
Purpose: establish a reproducible fine-tuning and evaluation pipeline.

- Base: Qwen/Qwen2.5-0.5B-Instruct
- SFT + LoRA
- Explicit train/validation/test splits
- Deterministic held-out evaluation

## NV-0.3
Purpose: improve behavior from measured failures.

- Expand dataset substantially.
- Add difficult instruction-following examples.
- Add reasoning and uncertainty cases.
- Add regression tests from every observed failure.
- Compare NV-0.2 against NV-0.3 on the same held-out suite.

## NV-0.4
Purpose: improve reliability and product usefulness.

- Larger curated dataset.
- Safety and refusal regression suite.
- More systematic evaluation.
- Inference optimization.

## NV-1.0
A release should only be considered after reproducible evaluation demonstrates that the model meets predefined quality criteria. No performance claims should be made without recorded measurements.


## Assistant platform expansion
- Persistent conversations and chat history
- Streaming responses
- File ingestion for common documents
- Pluggable web search and citations
- Multimodal provider adapters
- Tool/function calling
- Account and usage controls
- Production observability and deployment automation
