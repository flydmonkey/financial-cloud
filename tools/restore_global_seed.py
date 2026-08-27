#!/usr/bin/env python3
"""Restore global seed rows accidentally removed by clear_books.py."""
from __future__ import annotations

import re
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "sql" / "jinbooks_init.sql"

TABLES = (
    "userinfo",
    "role_member",
    "permission",
    "config_login_policy",
    "config",
)

HOST = "127.0.0.1"
PORT = 3307
DB = "jinbooks"
USER = "jinbooks"
PASSWORD = "Jinbooks321!"


def extract_insert(sql_text: str, table: str) -> str | None:
    pattern = re.compile(
        rf"INSERT INTO `{re.escape(table)}` VALUES .*?;",
        re.DOTALL,
    )
    match = pattern.search(sql_text)
    return match.group(0) if match else None


def main() -> int:
    init_sql = INIT_SQL.read_text(encoding="utf-8")
    conn = pymysql.connect(
        host=HOST,
        port=PORT,
        user=USER,
        password=PASSWORD,
        database=DB,
        autocommit=True,
    )
    cur = conn.cursor()
    for table in TABLES:
        insert_sql = extract_insert(init_sql, table)
        if not insert_sql:
            print(f"skip {table}: no INSERT found")
            continue
        cur.execute(f"DELETE FROM `{table}`")
        cur.execute(insert_sql)
        cur.execute(f"SELECT COUNT(*) FROM `{table}`")
        print(f"restored {table}: {cur.fetchone()[0]} rows")
    conn.close()
    print("done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
