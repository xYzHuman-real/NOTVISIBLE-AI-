# NOTVISIBLEAI

Research project for developing a small instruction-following language model.

## Current release

NV-0.2 is an SFT + LoRA experiment based on Qwen/Qwen2.5-0.5B-Instruct.

The repository contains the training and evaluation pipeline. The hosted demo in `demo/` currently uses the base model; it is not represented as NV-0.2 until a trained adapter is produced and evaluated.

## GPU demo

The Gradio demo is designed for Hugging Face ZeroGPU. ZeroGPU dynamically allocates GPU hardware to decorated functions. See the official documentation for current hardware, quotas, and compatibility.

## Reproducibility

Never claim NV-0.2 performance from the demo. Release adapter weights and evaluation reports only after a real training run.
