#!/usr/bin/env python3
"""Apply fixed asset tables + menu seed (idempotent)."""
from pathlib import Path
import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
TABLES_SQL = ROOT / "sql" / "seed" / "fixed_asset_tables.sql"
CHANGE_SQL = ROOT / "sql" / "seed" / "fixed_asset_change_tables.sql"
MENU_SQL = ROOT / "sql" / "seed" / "fixed_asset_menu.sql"
DISPOSE_ALTER_SQL = ROOT / "sql" / "seed" / "fixed_asset_dispose_alter.sql"
PURCHASE_ALTER_SQL = ROOT / "sql" / "seed" / "fixed_asset_purchase_alter.sql"
SUSPEND_ALTER_SQL = ROOT / "sql" / "seed" / "fixed_asset_suspend_alter.sql"


def apply(conn, path: Path) -> None:
    sql = path.read_text(encoding="utf-8")
    with conn.cursor() as cur:
        cur.execute(sql)


def ensure_column(conn, column: str, alter_sql: Path) -> None:
    with conn.cursor() as cur:
        cur.execute(f"SHOW COLUMNS FROM fixed_asset LIKE '{column}'")
        if cur.fetchone():
            print(f"{column} already exists")
            return
    apply(conn, alter_sql)
    print(f"added {column}")


def main() -> int:
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
            apply(conn, TABLES_SQL)
            apply(conn, CHANGE_SQL)
            apply(conn, MENU_SQL)
            ensure_column(conn, "dispose_voucher_id", DISPOSE_ALTER_SQL)
            ensure_column(conn, "purchase_voucher_id", PURCHASE_ALTER_SQL)
            ensure_column(conn, "suspended_period", SUSPEND_ALTER_SQL)
            with conn.cursor() as cur:
                cur.execute(
                    "SELECT id, res_name, request_url, parent_id, sort_index "
                    "FROM resources WHERE id IN (%s,%s,%s,%s,%s,%s,%s) ORDER BY sort_index",
                    (
                        "2026082818000000001",
                        "2026082818000000011",
                        "2026082818000000021",
                        "2026082818000000031",
                        "2026082818000000041",
                        "2026082818000000051",
                        "2026082818000000061",
                    ),
                )
                rows = cur.fetchall()
                cur.execute("SHOW TABLES LIKE 'fixed_asset%%'")
                tables = cur.fetchall()
                cur.execute("SHOW COLUMNS FROM fixed_asset LIKE 'purchase_voucher_id'")
                col = cur.fetchone()
            conn.close()
            print(f"OK applied tables+change+menu as {user}")
            print("tables:", tables)
            print("purchase_voucher_id:", col)
            print("resources:", rows)
            return 0
        except Exception as e:
            print(f"fail {user}: {e}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
