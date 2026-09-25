import json, os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
BASE = os.environ.get("NV_MODEL", "Qwen/Qwen2.5-0.5B-Instruct")
ADAPTER = ROOT / "artifacts" / "nv-0.2-lora" / "final"
TEST = ROOT / "data" / "test.jsonl"
REPORT = ROOT / "reports" / "nv-0.2-evaluation.md"

try:
    import torch
    from transformers import AutoTokenizer, AutoModelForCausalLM
    from peft import PeftModel
except Exception as e:
    print("Evaluation dependencies are not available.")
    print("Install: pip install -U transformers peft torch")
    print(f"Import error: {e}")
    raise SystemExit(2)

if not ADAPTER.exists():
    print(f"Adapter not found: {ADAPTER}")
    print("Run src/train_nv02.py first.")
    raise SystemExit(4)

tokenizer = AutoTokenizer.from_pretrained(BASE)
base = AutoModelForCausalLM.from_pretrained(BASE)
model = PeftModel.from_pretrained(base, str(ADAPTER))
model.eval()

items = [json.loads(x) for x in TEST.read_text(encoding="utf-8").splitlines() if x.strip()]
rows = []
for i, item in enumerate(items, 1):
    messages = item["messages"]
    prompt = tokenizer.apply_chat_template(messages[:-1], tokenize=False, add_generation_prompt=True)
    inputs = tokenizer(prompt, return_tensors="pt")
    with torch.no_grad():
        outputs = model.generate(**inputs, max_new_tokens=160, do_sample=False)
    generated = tokenizer.decode(outputs[0][inputs["input_ids"].shape[1]:], skip_special_tokens=True).strip()
    rows.append((i, messages[-1]["content"], generated))

lines = ["# NV-0.2 Evaluation", "", "Deterministic held-out generations. Human review is required for factuality and safety.", ""]
for i, expected, generated in rows:
    lines += [f"## Test {i}", f"**Expected:** {expected}", "", f"**NV-0.2:** {generated}", ""]
REPORT.write_text("\n".join(lines), encoding="utf-8")
print(f"Wrote {REPORT}")
