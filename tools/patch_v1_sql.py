#!/usr/bin/env python3
"""Strip embedded standard_subject seed data from financial_cloud_v1.0.1.sql (use sql/seed/data/standard_subjects.sql instead)."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SQL_FILE = ROOT / "sql" / "financial_cloud_v1.0.1.sql"

REPLACEMENT_SUBJECT = """\
-- Dumping data for table `standard_subject`
--
-- Historical seed removed. Import from:
--   sql/seed/data/standard_subjects.sql
-- Regenerate with:
--   python tools/import_standard_subjects.py

LOCK TABLES `standard_subject` WRITE;
/*!40000 ALTER TABLE `standard_subject` DISABLE KEYS */;
/*!40000 ALTER TABLE `standard_subject` ENABLE KEYS */;
UNLOCK TABLES;
"""

REPLACEMENT_CASH_FLOW = """\
-- Dumping data for table `standard_subject_cash_flow`
--
-- Cleared with standard subject reseed. Reconfigure after import.

LOCK TABLES `standard_subject_cash_flow` WRITE;
/*!40000 ALTER TABLE `standard_subject_cash_flow` DISABLE KEYS */;
/*!40000 ALTER TABLE `standard_subject_cash_flow` ENABLE KEYS */;
UNLOCK TABLES;
"""


def replace_block(text: str, table: str, replacement: str) -> str:
    pattern = (
        rf"--\s*\n-- Dumping data for table `{table}`\s*\n--\s*\n"
        rf"LOCK TABLES `{table}` WRITE;\s*"
        rf"/\*!40000 ALTER TABLE `{table}` DISABLE KEYS \*/;\s*"
        rf"INSERT INTO `{table}` VALUES .*?"
        rf"/\*!40000 ALTER TABLE `{table}` ENABLE KEYS \*/;\s*"
        rf"UNLOCK TABLES;"
    )
    new_text, count = re.subn(pattern, replacement, text, count=1, flags=re.DOTALL)
    if count != 1:
        raise RuntimeError(f"Expected to replace one `{table}` block, replaced {count}")
    return new_text


def main() -> int:
    if not SQL_FILE.exists():
        print(f"Missing {SQL_FILE}", file=sys.stderr)
        return 1
    original = SQL_FILE.read_text(encoding="utf-8")
    updated = replace_block(original, "standard_subject", REPLACEMENT_SUBJECT)
    updated = replace_block(updated, "standard_subject_cash_flow", REPLACEMENT_CASH_FLOW)
    if updated == original:
        print("No changes made")
        return 0
    SQL_FILE.write_text(updated, encoding="utf-8")
    print(f"Patched {SQL_FILE} ({len(original)} -> {len(updated)} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
