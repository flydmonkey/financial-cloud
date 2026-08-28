#!/usr/bin/env python3
"""Apply expense detail menu seed (idempotent)."""
from pathlib import Path
import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "sql" / "seed" / "expense_detail_menu.sql"

def main() -> int:
    sql = SQL.read_text(encoding="utf-8")
    for user, password in (("jinbooks", "Jinbooks321!"), ("root", "root")):
        try:
            conn = pymysql.connect(
                host="127.0.0.1",
                port=3307,
                user=user,
                password=password,
                database="jinbooks",
                charset="utf8mb4",
                client_flag=CLIENT.MULTI_STATEMENTS,
                autocommit=True,
            )
            with conn.cursor() as cur:
                cur.execute(sql)
            conn.close()
            print(f"OK applied {SQL.name} as {user}")
            return 0
        except Exception as e:
            print(f"fail {user}: {e}")
    return 1

if __name__ == "__main__":
    raise SystemExit(main())
