FROM python:3.11-slim

WORKDIR /app
COPY inference/requirements.txt /app/inference/requirements.txt
RUN pip install --no-cache-dir -r /app/inference/requirements.txt
COPY inference /app/inference

ENV NV_ADAPTER_PATH=/app/artifacts/nv-0.2-lora/final
EXPOSE 8100
CMD ["uvicorn", "inference.server:app", "--host", "0.0.0.0", "--port", "8100"]
