#!/usr/bin/env python3
"""Restore all Vue/TS files that fail Vue SFC parse or have encoding corruption."""
from __future__ import annotations

import json
import re
import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
UI = REPO / "financial-cloud-ui"
SRC = UI / "src"

CORRUPT = [
    re.compile(r'\? : "'),
    re.compile(r"['\"][^'\"]*\?\)"),
    re.compile(r"\?\s*/(?:div|span|el-)"),
    re.compile(r"format=\"[^\"]*$", re.M),
    re.compile(r"range-separator=\"[^\"]*$", re.M),
    re.compile(r"label=\"[^\"]*$", re.M),
]


def git_show(spec: str) -> str | None:
    r = subprocess.run(["git", "show", spec], cwd=REPO, capture_output=True)
    if r.returncode != 0:
        return None
    text = r.stdout.decode("utf-8", errors="replace")
    return text.replace("@/utils/Jinbooks", "@/utils/financialCloud").replace("\r\n", "\n")


def find_git_source(rel: str) -> str | None:
    candidates = [
        f"fda2539^:jinbooks-ui/src/{rel}",
        f"1c22560:financial-cloud-ui/src/{rel}",
        f"fda2539:financial-cloud-ui/src/{rel}",
        f"HEAD~1:financial-cloud-ui/src/{rel}",
    ]
    for spec in candidates:
        text = git_show(spec)
        if text and not any(p.search(text) for p in CORRUPT) and "\ufffd" not in text:
            return spec
    return None


def vue_parse_failures() -> list[str]:
    script = r"""
const fs=require('fs'); const path=require('path');
const { parse } = require('@vue/compiler-sfc');
function walk(d,out=[]){for(const e of fs.readdirSync(d,{withFileTypes:true})){const p=path.join(d,e.name);if(e.isDirectory()&&e.name!=='node_modules')walk(p,out);else if(e.name.endsWith('.vue'))out.push(p);}return out;}
const fails=[];
for(const f of walk('src')){try{parse(fs.readFileSync(f,'utf8'));}catch(e){fails.push(f.replace(/\\\\/g,'/'));}}
console.log(JSON.stringify(fails));
"""
    r = subprocess.run(["node", "-e", script], cwd=UI, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stderr)
        return []
    raw = r.stdout.strip()
    return json.loads(raw) if raw else []


def main() -> None:
    fails = vue_parse_failures()
    print(f"Vue parse failures: {len(fails)}")
    restored = 0
    for f in fails:
        rel = f.replace("src/", "").replace("src\\", "")
        spec = find_git_source(rel)
        if not spec:
            print(f"  NO SOURCE: {rel}")
            continue
        text = git_show(spec)
        assert text
        (SRC / rel).write_text(text, encoding="utf-8", newline="\n")
        print(f"  restored {rel} from {spec}")
        restored += 1

    # also scan corrupt text without parse fail
    for path in sorted(SRC.rglob("*")):
        if path.suffix not in {".vue", ".ts"}:
            continue
        rel = str(path.relative_to(SRC)).replace("\\", "/")
        text = path.read_text(encoding="utf-8", errors="replace")
        if not (any(p.search(text) for p in CORRUPT) or "\ufffd" in text):
            continue
        spec = find_git_source(rel)
        if not spec:
            print(f"  STILL CORRUPT (no source): {rel}")
            continue
        clean = git_show(spec)
        if clean and clean != text:
            path.write_text(clean, encoding="utf-8", newline="\n")
            print(f"  cleaned {rel} from {spec}")
            restored += 1

    print(f"Done. Restored/ cleaned {restored} files.")


if __name__ == "__main__":
    main()
