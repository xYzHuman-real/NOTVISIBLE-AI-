import os, sys, json, platform
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
MODEL = os.environ.get("NV_MODEL", "Qwen/Qwen2.5-0.5B-Instruct")
OUT = ROOT / "artifacts" / "nv-0.2-lora"

try:
    import torch
    from datasets import load_dataset
    from peft import LoraConfig
    from trl import SFTConfig, SFTTrainer
except Exception as e:
    print("NV-0.2 training dependencies are not available.")
    print("Install: pip install -U transformers datasets 'trl[peft]' accelerate torch")
    print(f"Import error: {e}")
    raise SystemExit(2)

print(f"Python: {platform.python_version()}")
print(f"PyTorch: {torch.__version__}")
print(f"CUDA available: {torch.cuda.is_available()}")
if not torch.cuda.is_available() and os.environ.get("NV_ALLOW_CPU") != "1":
    print("No CUDA device detected. Set NV_ALLOW_CPU=1 only for a small CPU experiment.")
    raise SystemExit(3)

data = load_dataset("json", data_files={
    "train": str(ROOT / "data/train.jsonl"),
    "validation": str(ROOT / "data/validation.jsonl"),
})

peft_config = LoraConfig(
    r=16,
    lora_alpha=32,
    lora_dropout=0.05,
    bias="none",
    task_type="CAUSAL_LM",
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],
)

args = SFTConfig(
    output_dir=str(OUT),
    num_train_epochs=1,
    per_device_train_batch_size=1,
    gradient_accumulation_steps=2,
    learning_rate=1e-4,
    logging_steps=1,\n    max_steps=int(os.environ.get("NV_MAX_STEPS", "10")),
    eval_strategy="steps",
    eval_steps=10,
    save_steps=10,
    save_total_limit=2,
    report_to="none",
    use_cpu=not torch.cuda.is_available(),
    max_length=512,
    packing=True,
)

trainer = SFTTrainer(
    model=MODEL,
    args=args,
    train_dataset=data["train"],
    eval_dataset=data["validation"],
    peft_config=peft_config,
)

trainer.train()
trainer.save_model(str(OUT / "final"))
trainer.save_state()

manifest = {
    "version": "NV-0.2",
    "base_model": MODEL,
    "method": "SFT+LoRA",
    "train_examples": len(data["train"]),
    "validation_examples": len(data["validation"]),
    "output": str(OUT / "final"),
}
(OUT / "run_manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
print(json.dumps(manifest, indent=2))
