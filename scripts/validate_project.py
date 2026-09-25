import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

for path in ROOT.glob("configs/*.json"):
    json.loads(path.read_text())
for path in ROOT.glob("data/*.jsonl"):
    for line_no,line in enumerate(path.read_text().splitlines(),1):
        if line.strip():
            row=json.loads(line)
            assert isinstance(row.get("messages"), list), f"{path}:{line_no}"
            assert row["messages"], f"{path}:{line_no}: empty messages"

print("Project validation passed.")
