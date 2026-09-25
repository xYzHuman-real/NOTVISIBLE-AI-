# NOTVISIBLEAI Model Status

## NV-0.2

**Engineering status:** trained and evaluated.

**Base model:** Qwen/Qwen2.5-0.5B-Instruct  
**Training method:** supervised fine-tuning with LoRA  
**Dataset:** 20 train / 3 validation / 5 held-out test examples  
**Training runtime:** GitHub Actions CPU experiment  
**Training run:** 36093561335  
**Verified artifact:** `nv-0.2-training-output`  
**Artifact SHA-256:** `7f6ffe214a01542d85aaeb65fdc94d194a657213d65a4f616d18b771f1865e66`  
**Evaluation:** 5 deterministic held-out generations completed successfully.

### Scope

NV-0.2 is a fine-tuned adapter built on Qwen2.5-0.5B-Instruct. It is not a newly pre-trained foundation model. The current experiment is small (20 training examples), so the evaluation is a functional smoke test rather than a broad capability benchmark.

### Artifact contents

The verified training artifact contains the final LoRA adapter, tokenizer/configuration files, training state, checkpoint, and evaluation report.

### Important

A successful training run verifies that the adapter can be produced and evaluated. It does not by itself establish broad model quality, factuality, safety, or production readiness. Human review and larger held-out benchmarks are still required before making stronger quality claims.
