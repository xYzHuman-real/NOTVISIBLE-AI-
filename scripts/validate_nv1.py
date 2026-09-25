import torch
from src.nv1_model import NV1Config,NV1Model
cfg=NV1Config(vocab_size=256,dim=128,layers=2,heads=4,mlp_hidden=256,max_seq_len=64)
m=NV1Model(cfg)
x=torch.randint(0,256,(2,32))
y=m(x,labels=x)
assert y["logits"].shape==(2,32,256)
assert torch.isfinite(y["loss"])
print("NV-1 model smoke test: PASS")
