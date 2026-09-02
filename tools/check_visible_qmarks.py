#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path

SRC = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src"

PAT_TMPL = re.compile(r">(\s*\?{2,}[^<]{0,40})<")
PAT_ATTR = re.compile(r'(?:content|label|placeholder|title)="(\?{2,}[^"]*)"')
PAT_JS = re.compile(r"(?:text|name|confirmButtonText|cancelButtonText):\s*'(\?{2,}[^']*)'")


def main() -> None:
    hits = []
    for path in SRC.rglob("*.vue"):
        text = path.read_text(encoding="utf-8", errors="replace")
        rel = str(path.relative_to(SRC)).replace("\\", "/")
        for m in PAT_TMPL.finditer(text):
            hits.append((rel, "tmpl", m.group(1).strip()))
        for m in PAT_ATTR.finditer(text):
            hits.append((rel, "attr", m.group(1)))
        for m in PAT_JS.finditer(text):
            hits.append((rel, "js", m.group(1)))

    print(f"visible qmark UI hits: {len(hits)}")
    for h in hits:
        print(f"  {h[0]} [{h[1]}] {h[2]}")

    nav = (SRC / "layout/components/Navbar.vue").read_text(encoding="utf-8")
    checks = ["\u5f53\u524d\u8d26\u671f", "\u8d26\u5957\uff1a", "\u4e2a\u4eba\u4e2d\u5fc3", "\u9000\u51fa\u767b\u5f55", "\u5e74", "\u6708"]
    print("navbar:", {c: (c in nav) for c in checks})


if __name__ == "__main__":
    main()
