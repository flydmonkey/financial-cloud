#!/usr/bin/env python3
"""Drop and reinitialize jinbooks database from sql/jinbooks_init.sql."""
from __future__ import annotations

import sys
from pathlib import Path

import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "sql" / "jinbooks_init.sql"

HOST = "127.0.0.1"
PORT = 3307
DB = "jinbooks"

# Prefer root for DROP DATABASE; fall back to app user.
CREDENTIALS = [
    ("root", "root"),
    ("jinbooks", "Jinbooks321!"),
]


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


def execute_sql_script(cursor, sql: str) -> None:
    """Execute SQL script statement-by-statement for clearer errors."""
    statement: list[str] = []
    in_string = False
    escape = False
    for char in sql:
        if escape:
            statement.append(char)
            escape = False
            continue
        if char == "\\" and in_string:
            escape = True
            statement.append(char)
            continue
        if char == "'":
            in_string = not in_string
            statement.append(char)
            continue
        if char == ";" and not in_string:
            chunk = "".join(statement).strip()
            statement = []
            if not chunk or chunk.startswith("--"):
                continue
            try:
                cursor.execute(chunk)
                drain_results(cursor)
            except pymysql.Error as exc:
                code = exc.args[0] if exc.args else None
                # Ignore duplicate column/table when replaying patch section.
                if code in (1060, 1061, 1050, 1051, 1091):
                    continue
                raise RuntimeError(f"SQL failed ({code}): {exc}\n---\n{chunk[:500]}") from exc
            continue
        statement.append(char)


def main() -> int:
    if not INIT_SQL.exists():
        print(f"Missing {INIT_SQL}", file=sys.stderr)
        return 1

    sql = INIT_SQL.read_text(encoding="utf-8")
    conn, user = pick_admin_connection()
    print(f"Connected as {user}")

    try:
        with conn.cursor() as cursor:
            print("Dropping database jinbooks ...")
            cursor.execute(f"DROP DATABASE IF EXISTS `{DB}`")
            drain_results(cursor)
            print("Creating database jinbooks ...")
            cursor.execute(
                f"CREATE DATABASE `{DB}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            )
            drain_results(cursor)

            if user != "jinbooks":
                cursor.execute(
                    f"GRANT ALL PRIVILEGES ON `{DB}`.* TO 'jinbooks'@'%'"
                )
                drain_results(cursor)
                cursor.execute("FLUSH PRIVILEGES")
                drain_results(cursor)

            conn.select_db(DB)
            print(f"Executing {INIT_SQL} ...")
            execute_sql_script(cursor, sql)

            cursor.execute("SELECT COUNT(*) FROM standard_subject")
            subject_count = cursor.fetchone()[0]
            cursor.execute("SELECT COUNT(*) FROM userinfo")
            user_count = cursor.fetchone()[0]
            cursor.execute("SELECT COUNT(*) FROM book")
            book_count = cursor.fetchone()[0]
            print(f"Done. standard_subject={subject_count}, userinfo={user_count}, book={book_count}")
    finally:
        conn.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
