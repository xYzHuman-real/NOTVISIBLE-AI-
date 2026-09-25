# NOTVISIBLEAI Foundation Model — NV-1

## Objective
Build an independently trained language model rather than using a hosted ChatGPT/OpenAI model at inference time.

## Important distinction
NV-0.2 is a Qwen2.5-0.5B-Instruct LoRA adapter. It is useful for experimentation but is not an independently trained foundation model.

NV-1 therefore uses a from-scratch causal-transformer training path. The repository contains the reproducible engineering pipeline; actual frontier-scale training requires external GPU infrastructure, large licensed datasets, evaluation infrastructure, and sustained compute.

## Capability targets
NV-1 should be developed in stages:
1. Base language modeling: tokenizer, data pipeline, decoder-only transformer, distributed training.
2. Instruction tuning: curated instruction/response data.
3. Reasoning training: verifiable math, code, science and multi-step tasks.
4. Tool use: structured tool-call format and execution feedback.
5. Long-context training.
6. Multimodal adapters.
7. Safety and refusal training.
8. Agent evaluation and regression testing.

## Independence requirement
Production inference must not call ChatGPT/OpenAI models. External models may only be used as explicitly documented research baselines or data-generation sources where licensing and provenance permit.

## Definition of “same intelligence”
This is an empirical target, not a percentage. NV-1 must be evaluated against fixed, contamination-resistant benchmarks across reasoning, coding, knowledge, instruction following, long-context tasks, tool use and multimodal tasks. A claim of parity requires comparable evaluation conditions and statistically meaningful results.

## Current blocker
The codebase can define and validate the training system, but it cannot honestly claim frontier-model parity until the model has actually been trained at the required scale and evaluated.
