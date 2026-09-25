# NV-0.2 Training — Google Colab / GPU Runtime

This notebook is an execution entry point for the NV-0.2 training package.
It downloads the repository, installs the pinned dependencies, runs SFT+LoRA, and then runs held-out evaluation.

> Training must be executed on an actual compatible ML runtime. This repository does not claim training occurred merely because this notebook exists.

## 1. Clone
```bash
git clone https://github.com/xYzHuman-real/NOTVISIBLE-AI-.git
cd NOTVISIBLE-AI-
```

## 2. Install
```bash
pip install -r requirements.txt
```

## 3. Train
```bash
python src/train_nv02.py
```

The script writes adapter artifacts and a run manifest to the configured output directory.

## 4. Evaluate
```bash
python src/evaluate_nv02.py
```

## 5. Record results
Copy the generated evaluation report into `reports/` and record the runtime, dependency versions, adapter path, and evaluation results before calling NV-0.2 trained/reproducible.

## Reproducibility
- Keep `data/test.jsonl` out of training.
- Do not change the test set after seeing results.
- Record the exact base-model revision when publishing a trained adapter.
