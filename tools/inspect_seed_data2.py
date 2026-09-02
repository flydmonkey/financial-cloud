#!/usr/bin/env python3
from __future__ import annotations

import re
from collections import Counter
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


def split_rows(values_sql: str) -> list[str]:
    rows: list[str] = []
    depth = 0
    start = 0
    in_quote = False
    i = values_sql.find("VALUES") + 6
    while i < len(values_sql):
        ch = values_sql[i]
        if ch == "'" and values_sql[i - 1] != "\\":
            in_quote = not in_quote
        elif not in_quote:
            if ch == "(":
                if depth == 0:
                    start = i
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    rows.append(values_sql[start : i + 1])
        i += 1
    return rows


for table in ["config", "voucher_template", "book", "organizations"]:
    block = extract_insert_block(table)
    if not block:
        print(table, "no data")
        continue
    rows = split_rows(block)
    if table == "config":
        ids = Counter()
        for row in rows:
            # ('id','book_id',...)
            m = re.match(r"\('([^']*)','([^']*)'", row)
            if m:
                ids[m.group(2)] += 1
        print("config book_id counts:", dict(ids))
    elif table == "voucher_template":
        ids = Counter()
        for row in rows:
            m = re.match(r"\('([^']*)','([^']*)'", row)
            if m:
                ids[m.group(2)] += 1
        print("voucher_template book_id counts:", dict(ids))
    else:
        print(table, "rows", len(rows))
