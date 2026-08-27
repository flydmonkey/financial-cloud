#!/usr/bin/env python3
from pathlib import Path
import re

text = Path("sql/jinbooks_v1.0.1.sql").read_text(encoding="utf-8", errors="replace")
tables = re.findall(r"CREATE TABLE `([^`]+)`", text)
print("tables", len(tables))
for t in tables:
    m = re.search(
        rf"-- Dumping data for table `{re.escape(t)}`\s*\n--\s*\nLOCK TABLES `{re.escape(t)}` WRITE;.*?INSERT INTO `{re.escape(t)}`",
        text,
        re.DOTALL,
    )
    has_insert = bool(m)
    print(f"{t}\t{'DATA' if has_insert else 'STRUCT'}")
