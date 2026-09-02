#!/usr/bin/env python3
"""Fix legacy @/api/system/* import paths after package restructure."""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src"

REPLACEMENTS = [
    ("@/api/system/standard/standard-subject", "@/api/standard/standard-subject"),
    ("@/api/system/standard/standard-statement-income", "@/api/standard/standard-statement-income"),
    ("@/api/system/standard/standard", "@/api/standard/standard"),
    ("@/api/system/voucher/voucher-template", "@/api/voucher/voucher-template"),
    ("@/api/system/voucher/voucher", "@/api/voucher/voucher"),
    ("@/api/system/statement/statement-income-config", "@/api/statement/statement-income-config"),
    ("@/api/system/statement/statement-income", "@/api/statement/statement-income"),
    ("@/api/system/statement/statement-cash-flow", "@/api/statement/statement-cash-flow"),
    ("@/api/system/statement/statement-config", "@/api/statement/statement-config"),
    ("@/api/system/statement/statement", "@/api/statement/statement"),
    ("@/api/system/book/book-subject", "@/api/book/book-subject"),
    ("@/api/system/book/settlement", "@/api/book/settlement"),
    ("@/api/system/hr/employee", "@/api/hr/employee"),
    ("@/api/system/dept", "@/api/idm/dept"),
    ("@/api/system/group.js", "@/api/idm/group"),
]


def main() -> None:
    changed = 0
    for path in sorted(ROOT.rglob("*")):
        if path.suffix not in {".vue", ".ts", ".js"}:
            continue
        text = path.read_text(encoding="utf-8")
        new = text
        for old, repl in REPLACEMENTS:
            new = new.replace(old, repl)
        if new != text:
            path.write_text(new, encoding="utf-8", newline="\n")
            print(path.relative_to(ROOT.parent))
            changed += 1
    print(f"Updated {changed} files")


if __name__ == "__main__":
    main()
