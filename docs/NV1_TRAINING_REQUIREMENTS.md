# NV-1 Training Requirements

A from-scratch model with ChatGPT-level intelligence cannot be completed by adding a few files or running the existing CPU workflow. The critical resources are training compute, data scale and quality, tokenizer/model experimentation, post-training, evaluation, and inference infrastructure.

## Required workstreams
- Licensed, high-quality multilingual pretraining corpus.
- Deduplication, quality filtering and contamination detection.
- Tokenizer training and versioning.
- Distributed data-parallel/FSDP/ZeRO training.
- Checkpointing and recovery.
- Experiment tracking.
- Instruction/SFT datasets.
- Preference/reward optimization with independently defined evaluators.
- Tool-use and agent trajectories.
- Long-context curriculum.
- Multimodal training/adapters.
- Safety evaluation and red-team testing.
- Large benchmark suite with held-out test sets.

## Engineering gate
Do not call NV-1 “frontier” or “ChatGPT-equivalent” until:
1. The target training run has completed.
2. Independent evaluations are available.
3. Results are reproduced on held-out tasks.
4. Inference is served entirely by NOTVISIBLEAI-owned model weights.
