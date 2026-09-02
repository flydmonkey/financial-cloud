#!/usr/bin/env python3
from pathlib import Path

p = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src" / "views" / "login.vue"
t = p.read_text(encoding="utf-8")
t = t.replace("\n          ????\n", "\n          \u5176\u4ed6\u767b\u5f55\n", 1)
t = t.replace('content="????"', 'content="\u9009\u62e9\u8bed\u8a00"', 1)
p.write_text(t, encoding="utf-8", newline="\n")
verify = p.read_text(encoding="utf-8")
assert "\u5176\u4ed6\u767b\u5f55" in verify
print("login.vue patched")
