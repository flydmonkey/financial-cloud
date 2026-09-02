#!/usr/bin/env python3
"""Scan financial-cloud-ui for encoding corruption."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src"


def try_fix_mojibake(text: str) -> str | None:
    try:
        return text.encode("latin-1").decode("utf-8")
    except (UnicodeDecodeError, UnicodeEncodeError):
        return None


def main() -> None:
    broken_ternary: list[str] = []
    unclosed: list[str] = []
    replacement: list[str] = []
    mojibake_candidates: list[str] = []

    for path in sorted(ROOT.rglob("*")):
        if path.suffix not in {".vue", ".ts", ".js"}:
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        rel = str(path.relative_to(ROOT.parent))
        if re.search(r'\? : "', text):
            broken_ternary.append(rel)
        if "\ufffd" in text:
            replacement.append(rel)
        if re.search(r'["\'][^"\']*\?\)', text):
            unclosed.append(rel)
        sample = re.search(r"[\u00c0-\u00ff]{4,}", text)
        if sample and try_fix_mojibake(sample.group(0)):
            mojibake_candidates.append(rel)

    print("broken ternary:", len(broken_ternary))
    for x in broken_ternary:
        print(" ", x)
    print("replacement char:", len(replacement))
    for x in replacement:
        print(" ", x)
    print("unclosed ?):", len(unclosed))
    for x in unclosed[:20]:
        print(" ", x)
    print("mojibake candidates:", len(mojibake_candidates))
    for x in mojibake_candidates[:30]:
        print(" ", x)


if __name__ == "__main__":
    main()
