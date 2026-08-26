#!/usr/bin/env python3
"""Remove jbx_/JBX_ table prefix and lowercase names in source + SQL files."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCAN_DIRS = [ROOT / "financial-cloud" / "src", ROOT / "sql"]
SKIP_DIR_NAMES = {"target", "node_modules", ".git"}
EXTENSIONS = {".java", ".xml", ".sql"}
TABLE_PATTERN = re.compile(r"(?i)jbx_([a-z0-9_]+)")


def replace_tables(text: str) -> tuple[str, set[str]]:
    tables: set[str] = set()

    def repl(match: re.Match[str]) -> str:
        suffix = match.group(1).lower()
        tables.add(suffix)
        return suffix

    return TABLE_PATTERN.sub(repl, text), tables


def main() -> None:
    all_tables: set[str] = set()
    changed_files = 0

    for base in SCAN_DIRS:
        if not base.exists():
            continue
        for path in base.rglob("*"):
            if any(part in SKIP_DIR_NAMES for part in path.parts):
                continue
            if path.suffix.lower() not in EXTENSIONS:
                continue
            original = path.read_text(encoding="utf-8")
            updated, tables = replace_tables(original)
            all_tables.update(tables)
            if updated != original:
                path.write_text(updated, encoding="utf-8", newline="\n")
                changed_files += 1
                print(f"updated: {path.relative_to(ROOT)}")

    migration = ROOT / "sql" / "jinbooks_v1.1.0-rename-tables.sql"
    lines = [
        "-- Rename jbx_* / JBX_* tables to lowercase names without prefix.",
        "-- Run once against the jinbooks database before deploying this release.",
        "",
    ]
    for table in sorted(all_tables):
        old_lower = f"jbx_{table}"
        lines.append(
            f"RENAME TABLE `jinbooks`.`{old_lower}` TO `jinbooks`.`{table}`;"
        )
    lines.append("")
    migration.write_text("\n".join(lines), encoding="utf-8", newline="\n")

    print(f"\nchanged {changed_files} files")
    print(f"tables: {len(all_tables)}")
    print(f"migration: {migration.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
