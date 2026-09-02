#!/usr/bin/env python3
"""Find and restore all corrupted UI source files from pre-rename git commit."""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
UI_SRC = REPO / "financial-cloud-ui" / "src"
BASE = "fda2539^"

IMPORT_REPLACEMENTS = [
    ("@/utils/Jinbooks", "@/utils/financialCloud"),
]

CORRUPT_PATTERNS = [
    re.compile(r'\? : "'),
    re.compile(r"['\"][^'\"]*\?\)"),
    re.compile(r"\?\s*/(?:div|span|el-)"),
    re.compile(r"[\u0080-\u009f]"),  # stray control chars from bad decode
]


def git_show(rel: str) -> str | None:
    result = subprocess.run(
        ["git", "show", f"{BASE}:jinbooks-ui/src/{rel}"],
        cwd=REPO,
        capture_output=True,
        check=False,
    )
    if result.returncode != 0:
        return None
    text = result.stdout.decode("utf-8", errors="replace")
    for old, new in IMPORT_REPLACEMENTS:
        text = text.replace(old, new)
    return text.replace("\r\n", "\n")


def is_corrupt(text: str) -> bool:
    if "\ufffd" in text:
        return True
    return any(p.search(text) for p in CORRUPT_PATTERNS)


def main() -> None:
    to_restore: list[str] = []
    no_git: list[str] = []
    for path in sorted(UI_SRC.rglob("*")):
        if path.suffix not in {".vue", ".ts", ".js"}:
            continue
        rel = str(path.relative_to(UI_SRC)).replace("\\", "/")
        current = path.read_text(encoding="utf-8", errors="replace")
        if not is_corrupt(current):
            continue
        git_text = git_show(rel)
        if git_text is None:
            no_git.append(rel)
            continue
        if is_corrupt(git_text):
            no_git.append(f"{rel} (git also corrupt)")
            continue
        to_restore.append(rel)

    print(f"Will restore {len(to_restore)} files:")
    for rel in to_restore:
        dest = UI_SRC / rel
        dest.write_text(git_show(rel) or "", encoding="utf-8", newline="\n")
        print(f"  restored {rel}")

    print(f"\nManual fix needed ({len(no_git)}):")
    for rel in no_git:
        print(f"  {rel}")


if __name__ == "__main__":
    main()
