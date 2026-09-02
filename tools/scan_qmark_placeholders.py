#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src"
hits = []
for path in sorted(list(ROOT.rglob("*.vue")) + list(ROOT.rglob("*.ts"))):
    text = path.read_text(encoding="utf-8", errors="replace")
    if "????" in text:
        hits.append(str(path.relative_to(ROOT)))
print(f"files with ???? placeholders: {len(hits)}")
for h in hits:
    print(f"  {h}")
