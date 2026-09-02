#!/usr/bin/env python3
"""Inspect seed-worthy rows in financial_cloud_v1.0.1.sql."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
text = (ROOT / "sql/financial_cloud_v1.0.1.sql").read_text(encoding="utf-8", errors="replace")


def extract_insert_block(table: str) -> str | None:
    pattern = (
        rf"-- Dumping data for table `{re.escape(table)}`\s*\n--\s*\n"
        rf"LOCK TABLES `{re.escape(table)}` WRITE;\s*"
        rf"/\*!40000 ALTER TABLE `{re.escape(table)}` DISABLE KEYS \*/;\s*"
        rf"(INSERT INTO `{re.escape(table)}` VALUES .*?);"
        rf"\n/\*!40000 ALTER TABLE `{re.escape(table)}` ENABLE KEYS \*/;"
    )
    match = re.search(pattern, text, re.DOTALL)
    return match.group(1) if match else None


for table in [
    "userinfo",
    "institutions",
    "organizations",
    "standard",
    "config",
    "roles",
    "voucher_template",
    "voucher_word",
    "permission",
]:
    block = extract_insert_block(table)
    print(f"=== {table} ===")
    if not block:
        print("NO DATA")
    else:
        print(block[:400])
        print(f"... ({len(block)} chars)")
    print()
