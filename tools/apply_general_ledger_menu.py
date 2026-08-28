#!/usr/bin/env python3
"""Apply general ledger menu seed (idempotent)."""
from pathlib import Path
import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "sql" / "seed" / "general_ledger_menu.sql"


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
                cur.execute(
                    "SELECT id, res_name, request_url, parent_id, sort_index, is_visible "
                    "FROM resources WHERE id=%s OR request_url=%s",
                    ("2026082816300000001", "/statement/general-ledger"),
                )
                rows = cur.fetchall()
                cur.execute(
                    "SELECT id, role_id, resource_id FROM permission WHERE resource_id=%s",
                    ("2026082816300000001",),
                )
                perms = cur.fetchall()
            conn.close()
            print(f"OK applied {SQL.name} as {user}")
            print("resources:", rows)
            print("permission:", perms)
            return 0
        except Exception as e:
            print(f"fail {user}: {e}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
