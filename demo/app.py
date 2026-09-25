import gradio as gr
import spaces
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM

MODEL="Qwen/Qwen2.5-0.5B-Instruct"
tokenizer=AutoTokenizer.from_pretrained(MODEL)
model=AutoModelForCausalLM.from_pretrained(MODEL, torch_dtype=torch.float16)
model.to("cuda")

@spaces.GPU(duration=60)
def generate(message, history):
    messages=[]
    for item in history or []:
        if isinstance(item, dict) and item.get("role") in ("user","assistant"):
            messages.append({"role":item["role"],"content":item.get("content","")})
    messages.append({"role":"user","content":message})
    prompt=tokenizer.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs=tokenizer(prompt, return_tensors="pt").to("cuda")
    with torch.inference_mode():
        out=model.generate(**inputs,max_new_tokens=256,do_sample=False)
    new=out[0][inputs["input_ids"].shape[1]:]
    return tokenizer.decode(new,skip_special_tokens=True)

demo=gr.ChatInterface(
    fn=generate,
    title="NOTVISIBLEAI",
    description="NV-0.2 research inference demo",
)
demo.launch()
