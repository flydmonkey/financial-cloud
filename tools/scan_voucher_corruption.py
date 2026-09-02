#!/usr/bin/env python3
from pathlib import Path
import re

root = Path("financial-cloud-ui/src/views/voucher")
for p in sorted(root.glob("*.vue")):
    t = p.read_text(encoding="utf-8", errors="replace")
    issues = []
    if "\ufffd" in t:
        issues.append("replacement")
    if re.search(r'\? : "', t):
        issues.append("broken_ternary")
    if re.search(r'format="[^"]*$', t, re.M):
        issues.append("unclosed_format")
    if re.search(r'content="[^"]*$', t, re.M):
        issues.append("unclosed_content")
    q = len(re.findall(r"\?\?\?", t))
    # show lines with likely truncated chinese ending in ?
    bad = []
    for i, line in enumerate(t.splitlines(), 1):
        if re.search(r"[\u4e00-\u9fff]\?", line) or re.search(r"\?[/\w]", line):
            if "??" in line or line.rstrip().endswith("?") or "/span>" in line or "format=" in line:
                bad.append((i, line.strip()[:100]))
    print(f"{p.name}: issues={issues or ['none']} ???={q} suspicious={len(bad)}")
    for i, preview in bad[:8]:
        print(f"  L{i}: {preview}")
