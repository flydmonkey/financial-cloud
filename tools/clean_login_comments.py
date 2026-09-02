#!/usr/bin/env python3
"""Clean corrupted comments in login.vue (non-user-facing)."""
from pathlib import Path

p = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src" / "views" / "login.vue"
t = p.read_text(encoding="utf-8")

replacements = [
    ("<!--  ??  -->", "<!-- tip -->"),
    ("// ?????", "// captcha"),
    ("// ????", "// state"),
    ("// ??????", "// social login"),
    ("// ?? action ?????", "// oauth action params"),
    ("// ???????", "// open other login"),
]

# style comments: strip broken trailing comments on those lines
lines = []
for line in t.splitlines():
    if "// ???" in line or "// ??" in line:
        # keep code before comment if present
        if "background-color" in line or "transition" in line or "box-shadow" in line:
            code = line.split("//", 1)[0].rstrip()
            # fix accidental merge of next property into comment line
            if "border:" in line and "background-color" in line:
                # original had two props smashed; restore border on same area from known style
                lines.append("  background-color: rgba(255, 255, 255, 0.7);")
                lines.append("  border: 1px solid #eaeaea;")
                continue
            lines.append(code if code else line)
            continue
        for old, new in replacements:
            if old in line:
                line = line.replace(old, new)
                break
    else:
        for old, new in replacements:
            if old in line:
                line = line.replace(old, new)
    lines.append(line)

text = "\n".join(lines) + "\n"
# apply remaining simple replacements
for old, new in replacements:
    text = text.replace(old, new)

p.write_text(text, encoding="utf-8", newline="\n")
verify = p.read_text(encoding="utf-8")
assert "????" not in verify
print("login.vue comments cleaned")
