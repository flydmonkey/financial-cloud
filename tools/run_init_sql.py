#!/usr/bin/env python3
"""Drop and reinitialize financial_cloud database from sql/financial_cloud_init.sql."""
from __future__ import annotations

import os
import sys
from pathlib import Path

import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "sql" / "financial_cloud_init.sql"

HOST = os.environ.get("FC_DB_HOST", "127.0.0.1")
PORT = int(os.environ.get("FC_DB_PORT", "3307"))
DB = "financial_cloud"
DB_USER = "financial_cloud"
DB_PASSWORD = "FinancialCloud321!"

CREDENTIALS = [
    ("root", "root"),
    (DB_USER, DB_PASSWORD),
]

LEDGER_MENU_ID = "2026082817000000001"
FIXED_ASSET_MENU_ID = "2026082818000000001"


def connect(user: str, password: str, database: str | None = None):
    return pymysql.connect(
        host=HOST,
        port=PORT,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        client_flag=CLIENT.MULTI_STATEMENTS,
        autocommit=True,
    )


def pick_admin_connection():
    last_error = None
    for user, password in CREDENTIALS:
        try:
            return connect(user, password), user
        except pymysql.Error as exc:
            last_error = exc
    raise SystemExit(f"Cannot connect to MySQL on {HOST}:{PORT}: {last_error}")


def drain_results(cursor) -> None:
    while True:
        try:
            if cursor.nextset() is None:
                break
        except pymysql.Error:
            break


def strip_leading_sql_comments(chunk: str) -> str:
    lines = chunk.splitlines()
    while lines:
        stripped = lines[0].strip()
        if stripped and not stripped.startswith("--"):
            break
        lines.pop(0)
    return "\n".join(lines).strip()


def execute_sql_script(cursor, sql: str) -> None:
    """Execute a multi-statement SQL script.

    Splits on ';' outside of single-quoted strings. Line comments starting with
    '--' (MySQL) are ignored so a ';' inside a comment does not end a statement.
    """
    statement: list[str] = []
    in_string = False
    escape = False
    i = 0
    n = len(sql)
    while i < n:
        char = sql[i]
        # Start of line/end-of-whitespace line comment: -- ...\\n
        if (
            not in_string
            and char == "-"
            and i + 1 < n
            and sql[i + 1] == "-"
            and (i == 0 or sql[i - 1] in "\n\r\t ")
        ):
            # skip until newline (keep newline for readability of next chunk)
            while i < n and sql[i] not in "\n\r":
                i += 1
            continue
        if escape:
            statement.append(char)
            escape = False
            i += 1
            continue
        if char == "\\" and in_string:
            escape = True
            statement.append(char)
            i += 1
            continue
        if char == "'":
            in_string = not in_string
            statement.append(char)
            i += 1
            continue
        if char == ";" and not in_string:
            chunk = strip_leading_sql_comments("".join(statement).strip())
            statement = []
            i += 1
            if not chunk:
                continue
            try:
                cursor.execute(chunk)
                drain_results(cursor)
            except pymysql.Error as exc:
                code = exc.args[0] if getattr(exc, "args", None) else None
                if code in (1060, 1061, 1050, 1051, 1091):
                    continue
                raise RuntimeError(f"SQL failed ({code}): {exc}\n---\n{chunk[:500]}") from exc
            continue
        statement.append(char)
        i += 1
    # trailing statement without semicolon
    chunk = strip_leading_sql_comments("".join(statement).strip())
    if chunk:
        cursor.execute(chunk)
        drain_results(cursor)


def verify_init(cursor) -> None:
    cursor.execute("SHOW TABLES LIKE 'fixed_asset'")
    if cursor.fetchone() is None:
        raise RuntimeError("fixed_asset table missing after init")

    cursor.execute(
        "SELECT COUNT(*) FROM resources WHERE id IN (%s, %s) AND deleted = 'n'",
        (LEDGER_MENU_ID, FIXED_ASSET_MENU_ID),
    )
    menu_count = cursor.fetchone()[0]
    if menu_count < 2:
        raise RuntimeError(
            f"Expected ledger + fixed-asset menus, found {menu_count} "
            f"(ids {LEDGER_MENU_ID}, {FIXED_ASSET_MENU_ID})"
        )

    cursor.execute("SELECT COUNT(*) FROM standard_subject")
    subject_count = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM userinfo")
    user_count = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM book")
    book_count = cursor.fetchone()[0]
    cursor.execute(
        "SELECT COUNT(*) FROM config_cash_flow_balance WHERE book_id IS NULL"
    )
    cash_flow_templates = cursor.fetchone()[0]
    print(
        f"Done. standard_subject={subject_count}, userinfo={user_count}, "
        f"book={book_count}, cash_flow_templates={cash_flow_templates}, "
        f"menus={menu_count}"
    )
    if cash_flow_templates <= 0:
        raise RuntimeError("config_cash_flow_balance templates missing after init")
    if subject_count < 300:
        raise RuntimeError(f"standard_subject count suspiciously low: {subject_count}")


def main() -> int:
    if not INIT_SQL.exists():
        print(f"Missing {INIT_SQL}", file=sys.stderr)
        return 1

    sql = INIT_SQL.read_text(encoding="utf-8")
    conn, user = pick_admin_connection()
    print(f"Connected as {user}")

    try:
        with conn.cursor() as cursor:
            print(f"Dropping database {DB} ...")
            cursor.execute(f"DROP DATABASE IF EXISTS `{DB}`")
            drain_results(cursor)
            print(f"Creating database {DB} ...")
            cursor.execute(
                f"CREATE DATABASE `{DB}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            )
            drain_results(cursor)

            if user != DB_USER:
                cursor.execute(
                    f"CREATE USER IF NOT EXISTS '{DB_USER}'@'%' "
                    f"IDENTIFIED BY '{DB_PASSWORD}'"
                )
                drain_results(cursor)
                cursor.execute(f"GRANT ALL PRIVILEGES ON `{DB}`.* TO '{DB_USER}'@'%'")
                drain_results(cursor)
                cursor.execute("FLUSH PRIVILEGES")
                drain_results(cursor)

            conn.select_db(DB)
            print(f"Executing {INIT_SQL} ...")
            execute_sql_script(cursor, sql)
            verify_init(cursor)
    finally:
        conn.close()

    # 四角色 RBAC 种子（做账员/审核员/查看员 + 权限包），幂等；
    # 缺失时非管理员用户无法过产品权限校验（500014）。
    import apply_rbac_four_roles

    apply_rbac_four_roles.main()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
