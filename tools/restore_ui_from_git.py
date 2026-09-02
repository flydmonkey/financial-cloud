#!/usr/bin/env python3
"""Restore corrupted UI files from pre-rename git commit and apply import renames."""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
UI = REPO / "financial-cloud-ui" / "src"
BASE = "fda2539^"

# Files known to need restore (syntax errors or replacement chars)
TARGETS = [
    "views/voucher/voucher-edit.vue",
    "views/voucher/voucher-template.vue",
    "views/voucher/voucher-index.vue",
    "views/statement/expense-detail.vue",
    "views/hr/employee.vue",
    "views/config/initBalance/index.vue",
    "views/dashboard/accounting/receivable.vue",
    "views/config/standard-subject.vue",
    "views/idm/groups.vue",
    "views/settlement/settle-list.vue",
    "api/menu.ts",
    "plugins/download.ts",
]

IMPORT_REPLACEMENTS = [
    ("@/utils/Jinbooks", "@/utils/financialCloud"),
    ("from '@/utils/Jinbooks'", "from '@/utils/financialCloud'"),
    ('from "@/utils/Jinbooks"', 'from "@/utils/financialCloud"'),
]


def git_show(git_path: str) -> bytes:
    result = subprocess.run(
        ["git", "show", f"{BASE}:jinbooks-ui/src/{git_path}"],
        cwd=REPO,
        capture_output=True,
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace"))
    return result.stdout


def apply_renames(text: str) -> str:
    for old, new in IMPORT_REPLACEMENTS:
        text = text.replace(old, new)
    # normalize line endings
    return text.replace("\r\n", "\n")


def has_syntax_red_flags(text: str) -> list[str]:
    flags: list[str] = []
    if re.search(r'\? : "', text):
        flags.append("broken ternary")
    if "\ufffd" in text:
        flags.append("replacement char")
    if re.search(r"['\"][^'\"]*\?\)", text):
        flags.append("unclosed string")
    return flags


def main() -> None:
    restored = 0
    for rel in TARGETS:
        dest = UI / rel
        if not dest.parent.exists():
            print(f"SKIP missing dir: {rel}")
            continue
        try:
            raw = git_show(rel)
        except RuntimeError as e:
            print(f"SKIP git: {rel}: {e}")
            continue
        text = raw.decode("utf-8", errors="replace")
        text = apply_renames(text)
        before = dest.read_text(encoding="utf-8", errors="replace") if dest.exists() else ""
        dest.write_text(text, encoding="utf-8", newline="\n")
        flags = has_syntax_red_flags(text)
        status = "OK" if not flags else f"WARN: {', '.join(flags)}"
        changed = "changed" if text != before else "same"
        print(f"{rel}: restored ({changed}) [{status}]")
        restored += 1
    print(f"\nRestored {restored} files from {BASE}")


if __name__ == "__main__":
    main()
