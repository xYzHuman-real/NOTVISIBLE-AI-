import argparse, json, math, os
from pathlib import Path
import torch
from torch.utils.data import Dataset, DataLoader
from torch.optim import AdamW
from nv1_model import NV1Config, NV1Model

class TokenDataset(Dataset):
    def __init__(self,path,seq_len):
        self.rows=[json.loads(x) for x in Path(path).read_text().splitlines() if x.strip()]
        self.seq_len=seq_len
    def __len__(self): return len(self.rows)
    def __getitem__(self,i):
        ids=self.rows[i]["input_ids"][:self.seq_len]
        ids=ids+[0]*max(0,self.seq_len-len(ids))
        return torch.tensor(ids,dtype=torch.long)

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--data",required=True); p.add_argument("--out",default="artifacts/nv1")
    p.add_argument("--steps",type=int,default=1000); p.add_argument("--batch-size",type=int,default=1)
    p.add_argument("--grad-accum",type=int,default=8); p.add_argument("--lr",type=float,default=3e-4)
    p.add_argument("--seq-len",type=int,default=4096); p.add_argument("--save-every",type=int,default=250)
    a=p.parse_args(); device="cuda" if torch.cuda.is_available() else "cpu"
    cfg=NV1Config(max_seq_len=a.seq_len); model=NV1Model(cfg).to(device)
    if device=="cuda": model=torch.compile(model)
    opt=AdamW(model.parameters(),lr=a.lr,weight_decay=.1)
    loader=DataLoader(TokenDataset(a.data,a.seq_len),batch_size=a.batch_size,shuffle=True)
    it=iter(loader); model.train(); step=0; opt.zero_grad(set_to_none=True)
    Path(a.out).mkdir(parents=True,exist_ok=True)
    while step<a.steps:
        try: x=next(it)
        except StopIteration: it=iter(loader); x=next(it)
        x=x.to(device)
        out=model(x,labels=x); (out["loss"]/a.grad_accum).backward()
        if (step+1)%a.grad_accum==0:
            torch.nn.utils.clip_grad_norm_(model.parameters(),1.0); opt.step(); opt.zero_grad(set_to_none=True)
        step+=1
        if step%10==0: print(f"step={step} loss={out['loss'].item():.4f} ppl={math.exp(min(out['loss'].item(),20)):.2f}")
        if step%a.save_every==0:
            torch.save({"model":model.state_dict(),"config":cfg.__dict__,"step":step},Path(a.out)/f"checkpoint-{step}.pt")
    torch.save({"model":model.state_dict(),"config":cfg.__dict__,"step":step},Path(a.out)/"final.pt")
if __name__=="__main__": main()
