"""Small, dependency-light decoder-only Transformer used as the NV-1 foundation-model reference.

This is a research implementation, not a claim of frontier capability.
"""
import math
import torch
from torch import nn

class RMSNorm(nn.Module):
    def __init__(self, dim, eps=1e-6):
        super().__init__()
        self.weight = nn.Parameter(torch.ones(dim))
        self.eps = eps
    def forward(self, x):
        return x * torch.rsqrt(x.pow(2).mean(-1, keepdim=True) + self.eps) * self.weight

class SwiGLU(nn.Module):
    def __init__(self, dim, hidden):
        super().__init__()
        self.gate = nn.Linear(dim, hidden, bias=False)
        self.up = nn.Linear(dim, hidden, bias=False)
        self.down = nn.Linear(hidden, dim, bias=False)
    def forward(self, x):
        return self.down(torch.nn.functional.silu(self.gate(x)) * self.up(x))

class CausalSelfAttention(nn.Module):
    def __init__(self, dim, heads):
        super().__init__()
        if dim % heads:
            raise ValueError("dim must be divisible by heads")
        self.heads = heads
        self.head_dim = dim // heads
        self.qkv = nn.Linear(dim, 3 * dim, bias=False)
        self.out = nn.Linear(dim, dim, bias=False)
    def forward(self, x):
        b, t, d = x.shape
        q, k, v = self.qkv(x).chunk(3, dim=-1)
        q = q.view(b,t,self.heads,self.head_dim).transpose(1,2)
        k = k.view(b,t,self.heads,self.head_dim).transpose(1,2)
        v = v.view(b,t,self.heads,self.head_dim).transpose(1,2)
        y = torch.nn.functional.scaled_dot_product_attention(q,k,v,is_causal=True)
        return self.out(y.transpose(1,2).contiguous().view(b,t,d))

class Block(nn.Module):
    def __init__(self, dim, heads, mlp_hidden):
        super().__init__()
        self.norm1=RMSNorm(dim); self.attn=CausalSelfAttention(dim,heads)
        self.norm2=RMSNorm(dim); self.mlp=SwiGLU(dim,mlp_hidden)
    def forward(self,x):
        x=x+self.attn(self.norm1(x))
        return x+self.mlp(self.norm2(x))

class NV1Config:
    def __init__(self,vocab_size=32000,dim=1024,layers=24,heads=16,mlp_hidden=2816,max_seq_len=4096):
        self.vocab_size=vocab_size; self.dim=dim; self.layers=layers
        self.heads=heads; self.mlp_hidden=mlp_hidden; self.max_seq_len=max_seq_len

class NV1Model(nn.Module):
    def __init__(self,cfg):
        super().__init__()
        self.cfg=cfg
        self.embed=nn.Embedding(cfg.vocab_size,cfg.dim)
        self.blocks=nn.ModuleList([Block(cfg.dim,cfg.heads,cfg.mlp_hidden) for _ in range(cfg.layers)])
        self.norm=RMSNorm(cfg.dim)
        self.lm_head=nn.Linear(cfg.dim,cfg.vocab_size,bias=False)
        self.lm_head.weight=self.embed.weight
        self.register_buffer("pos",torch.arange(cfg.max_seq_len),persistent=False)
    def forward(self,input_ids,labels=None):
        x=self.embed(input_ids)
        for block in self.blocks: x=block(x)
        logits=self.lm_head(self.norm(x))
        loss=None
        if labels is not None:
            loss=torch.nn.functional.cross_entropy(logits[:,:-1].reshape(-1,logits.size(-1)),labels[:,1:].reshape(-1),ignore_index=-100)
        return {"logits":logits,"loss":loss}
