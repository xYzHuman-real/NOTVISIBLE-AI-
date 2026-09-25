import os, time, uuid
from typing import Any

import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel

BASE_MODEL = os.getenv("NV_BASE_MODEL", "Qwen/Qwen2.5-0.5B-Instruct")
ADAPTER_PATH = os.getenv("NV_ADAPTER_PATH", "./artifacts/nv-0.2-lora/final")
MODEL_ID = os.getenv("NV_MODEL_ID", "nv-0.2")
MAX_INPUT_TOKENS = int(os.getenv("NV_MAX_INPUT_TOKENS", "4096"))

app = FastAPI(title="NOTVISIBLEAI NV-0.2 Inference", version="0.1.0")
tokenizer = None
model = None

class Message(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    model: str = MODEL_ID
    messages: list[Message]
    temperature: float = Field(0.7, ge=0, le=2)
    max_tokens: int = Field(256, ge=1, le=2048)
    stream: bool = False

def load_model():
    global tokenizer, model
    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL)
    base = AutoModelForCausalLM.from_pretrained(BASE_MODEL)
    model = PeftModel.from_pretrained(base, ADAPTER_PATH)
    model.eval()

@app.on_event("startup")
def startup():
    load_model()

@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_ID, "adapter_attached": model is not None}

@app.get("/v1/models")
def models():
    return {"object": "list", "data": [{
        "id": MODEL_ID, "object": "model", "owned_by": "notvisibleai"
    }]}

@app.post("/v1/chat/completions")
def chat(req: ChatRequest):
    if req.model != MODEL_ID:
        raise HTTPException(404, f"Unknown model: {req.model}")
    if req.stream:
        raise HTTPException(501, "Streaming is not enabled in this runner")
    messages = [m.model_dump() for m in req.messages]
    prompt = tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(
        prompt, return_tensors="pt", truncation=True, max_length=MAX_INPUT_TOKENS
    )
    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=req.max_tokens,
            do_sample=req.temperature > 0,
            temperature=max(req.temperature, 1e-5),
            pad_token_id=tokenizer.eos_token_id,
        )
    text = tokenizer.decode(
        outputs[0][inputs["input_ids"].shape[1]:],
        skip_special_tokens=True,
    ).strip()
    now = int(time.time())
    return {
        "id": "chatcmpl-" + uuid.uuid4().hex[:16],
        "object": "chat.completion",
        "created": now,
        "model": MODEL_ID,
        "choices": [{
            "index": 0,
            "message": {"role": "assistant", "content": text},
            "finish_reason": "stop",
        }],
        "usage": {
            "prompt_tokens": int(inputs["input_ids"].shape[1]),
            "completion_tokens": int(outputs.shape[1] - inputs["input_ids"].shape[1]),
            "total_tokens": int(outputs.shape[1]),
        },
    }
