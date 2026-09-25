# NOTVISIBLEAI NV-0.2

Research training package for a small conversational assistant.

## Objective
Improve instruction following, explanation quality, uncertainty handling, and consistent assistant behavior using supervised fine-tuning (SFT) with LoRA on an open-weight Qwen model.

**Important:** NV-0.2 is an adapter experiment, not a new foundation model trained from scratch.

## Model
- Base: `Qwen/Qwen2.5-0.5B-Instruct`
- Method: SFT + LoRA
- Adapter rank: 16
- LoRA alpha: 32
- LoRA dropout: 0.05
- Learning rate: 1e-4
- Epochs: 2
- Effective batch size: 8

TRL documents PEFT/LoRA integration and SFTTrainer support for this workflow. Verify the current base-model license and terms before redistribution.

## Dataset
The included examples are synthetic research examples created for this project. They are not intended to establish benchmark performance. Train/validation/test sets are kept separate.

## Run
```bash
pip install -U "transformers" "datasets" "trl[peft]" "accelerate" "torch"
python src/train_nv02.py
python src/evaluate_nv02.py
```

For a real training run, use a machine with suitable ML compute. The scripts fail clearly if dependencies or compute are unavailable.

## Evaluation
The evaluator records deterministic generations for the held-out test set and computes simple structural checks. Human review remains necessary for factuality and safety.

## Versioning
- NV-0.1: initial SFT/LoRA scaffold
- NV-0.2: expanded dataset schema, explicit splits, robust trainer, deterministic evaluator, run manifest


## Product stack

NOTVISIBLEAI now includes a static product site in `web/`, a Docker Compose local stack, a FastAPI gateway, a self-hosted inference runner, Python and JavaScript SDKs, reproducible model training/evaluation, and product/security documentation.

### Full-stack quickstart

See [docs/QUICKSTART.md](docs/QUICKSTART.md). The verified NV-0.2 Actions artifact must be downloaded separately because model artifacts are not committed to source control.

### Web product

Serve `web/` directly for the public product interface. Configure its API endpoint at runtime; never place a production API key in source-controlled JavaScript.
