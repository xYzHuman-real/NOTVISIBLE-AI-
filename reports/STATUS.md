# NV-0.2 Status

## Completed
- Expanded synthetic dataset: 28 total examples (20 train / 3 validation / 5 test).
- Explicit train/validation/test separation.
- LoRA target-module configuration.
- Reproducible training script.
- Deterministic evaluation script.
- Run manifest and configuration.
- README with installation and execution instructions.
- Source structure prepared for a real ML runtime.

## Not claimed
The model adapter is **not** claimed to be trained in this environment. The current runtime has CPU-only PyTorch and does not have the Hugging Face training dependencies installed. A genuine NV-0.2 training result requires executing the training script in a suitable ML runtime.

## Current objective
Run `src/train_nv02.py`, then `src/evaluate_nv02.py`, and review the held-out generations before calling NV-0.2 a successful training release.
