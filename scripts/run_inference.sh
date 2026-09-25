#!/usr/bin/env bash
set -euo pipefail
export NV_ADAPTER_PATH="${NV_ADAPTER_PATH:-./artifacts/nv-0.2-lora/final}"
uvicorn inference.server:app --host "${NV_HOST:-0.0.0.0}" --port "${NV_PORT:-8100}"
