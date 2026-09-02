#!/usr/bin/env python3
"""Comprehensive encoding / syntax corruption scan for financial-cloud-ui."""
from __future__ import annotations

import json
import re
import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
UI = REPO / "financial-cloud-ui"
SRC = UI / "src"

PATTERNS = {
    "ascii_qmark_run": re.compile(r"\?{3,}"),  # ??? or more
    "replacement_char": re.compile("\ufffd"),
    "broken_html_close": re.compile(r"\?/(?:div|span|template|el-[\w-]+)>"),
    "broken_ternary": re.compile(r'\? : "'),
    "unclosed_str_qmark": re.compile(r"""['"][^'"\n]*\?\)"""),
    "attr_unclosed_quote": re.compile(
        r"""(?:format|label|content|range-separator|placeholder|title)=\"[^\"]*$""",
        re.M,
    ),
    "mojibake_latin": re.compile(r"(?:Ã.|Â.|å.|æ.|ç.|è.|é.){4,}"),
}


def vue_parse_failures() -> list[str]:
    script = r"""
const fs=require('fs'); const path=require('path');
const { parse } = require('@vue/compiler-sfc');
function walk(d,out=[]){for(const e of fs.readdirSync(d,{withFileTypes:true})){const p=path.join(d,e.name);if(e.isDirectory()&&e.name!=='node_modules')walk(p,out);else if(e.name.endsWith('.vue'))out.push(p);}return out;}
const fails=[];
for(const f of walk('src')){
  try{ parse(fs.readFileSync(f,'utf8')); }
  catch(e){ fails.push({file:f.replace(/\\/g,'/'), msg: String(e.message||e).split('\n')[0]}); }
}
console.log(JSON.stringify(fails));
"""
    r = subprocess.run(["node", "-e", script], cwd=UI, capture_output=True, text=True)
    if r.returncode != 0:
        return [{"file": "_scanner_", "msg": r.stderr[:500]}]
    raw = (r.stdout or "").strip()
    return json.loads(raw) if raw else []


def classify_qmark_hits(path: Path, text: str) -> list[dict]:
    hits = []
    for i, line in enumerate(text.splitlines(), 1):
        if "???" not in line and "\ufffd" not in line:
            continue
        stripped = line.strip()
        kind = "code"
        if stripped.startswith("//") or stripped.startswith("*") or "<!--" in stripped:
            kind = "comment"
        elif re.search(r"\?\?\?", stripped):
            # template text / string literal
            if re.search(r">\s*\?{2,}|'\?{2,}|\"\?{2,}|`\?{2,}", stripped) or re.search(
                r">\s*\?{2,}|\{\{.*\?{2,}", stripped
            ):
                kind = "ui_text"
            elif "?" * 3 in stripped and ("span" in stripped or "div" in stripped or "label" in stripped or "content" in stripped or "name:" in stripped or "text:" in stripped):
                kind = "ui_text"
            else:
                kind = "maybe_ui"
        hits.append({"line": i, "kind": kind, "preview": stripped[:120]})
    return hits


def main() -> None:
    report: dict = {
        "files_scanned": 0,
        "by_pattern": {},
        "ui_text_suspects": [],
        "comment_only": [],
        "vue_parse_failures": [],
        "api_system_imports": [],
    }

    for key in PATTERNS:
        report["by_pattern"][key] = []

    for path in sorted(list(SRC.rglob("*.vue")) + list(SRC.rglob("*.ts")) + list(SRC.rglob("*.js"))):
        if "node_modules" in path.parts:
            continue
        report["files_scanned"] += 1
        text = path.read_text(encoding="utf-8", errors="replace")
        rel = str(path.relative_to(SRC)).replace("\\", "/")

        for name, pat in PATTERNS.items():
            if pat.search(text):
                report["by_pattern"][name].append(rel)

        if "@/api/system/" in text:
            report["api_system_imports"].append(rel)

        if "???" in text or "\ufffd" in text:
            hits = classify_qmark_hits(path, text)
            ui = [h for h in hits if h["kind"] in {"ui_text", "maybe_ui", "code"}]
            comments = [h for h in hits if h["kind"] == "comment"]
            if ui:
                report["ui_text_suspects"].append({"file": rel, "hits": ui[:15], "hit_count": len(ui)})
            elif comments:
                report["comment_only"].append(rel)

    report["vue_parse_failures"] = vue_parse_failures()

    out = REPO / "tools" / "encoding-audit-report.json"
    out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Scanned {report['files_scanned']} files")
    print("\nPattern hits:")
    for name, files in report["by_pattern"].items():
        print(f"  {name}: {len(files)}")
        for f in files[:20]:
            print(f"    - {f}")
    print(f"\nUI/text suspects: {len(report['ui_text_suspects'])}")
    for item in report["ui_text_suspects"]:
        print(f"  {item['file']} ({item['hit_count']} hits)")
        for h in item["hits"][:5]:
            print(f"    L{h['line']} [{h['kind']}] {h['preview']}")
    print(f"\nComment-only qmarks: {len(report['comment_only'])}")
    for f in report["comment_only"]:
        print(f"  - {f}")
    print(f"\nLegacy @/api/system imports: {len(report['api_system_imports'])}")
    for f in report["api_system_imports"]:
        print(f"  - {f}")
    print(f"\nVue parse failures: {len(report['vue_parse_failures'])}")
    for f in report["vue_parse_failures"]:
        print(f"  - {f}")
    print(f"\nReport written: {out}")


if __name__ == "__main__":
    main()
