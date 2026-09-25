#!/usr/bin/env bash
set -euo pipefail

python scripts/validate_project.py
python src/train_nv02.py
python src/evaluate_nv02.py

echo "NV-0.2 training and evaluation pipeline finished."
