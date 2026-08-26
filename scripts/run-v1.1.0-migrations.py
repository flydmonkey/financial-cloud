#!/usr/bin/env python3
"""Apply v1.1.0 SQL migrations to the local jinbooks database."""

from __future__ import annotations

import os
import re
from pathlib import Path

import pymysql

ROOT = Path(__file__).resolve().parents[1]
SQL_DIR = ROOT / "sql"
APP_DIR = ROOT / "financial-cloud"
SCRIPTS = [
    "jinbooks_v1.1.0-rename-tables.sql",
    "jinbooks_v1.1.0-migrate-plain-passwords.sql",
    "jinbooks_v1.1.0-cleanup-dead-menus.sql",
]


def load_defaults_from_application_yml() -> dict[str, str | int]:
    yml = ROOT / "financial-cloud" / "src" / "main" / "resources" / "application.yml"
    text = yml.read_text(encoding="utf-8")
    url = re.search(r"url:\s*jdbc:mysql://([^:/]+):(\d+)/([^?]+)", text)
    user = re.search(r"username:\s*(\S+)", text)
    password = re.search(r"password:\s*(\S+)", text)
    if not (url and user and password):
        raise RuntimeError("Could not parse DB settings from application.yml")
    return {
        "host": url.group(1),
        "port": int(url.group(2)),
        "database": url.group(3),
        "user": user.group(1),
        "password": password.group(1),
    }


def split_sql(content: str) -> list[str]:
    statements: list[str] = []
    buffer: list[str] = []
    for line in content.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("--"):
            continue
        buffer.append(line)
        if stripped.endswith(";"):
            statements.append("\n".join(buffer))
            buffer = []
    if buffer:
        statements.append("\n".join(buffer))
    return statements


def main() -> None:
    defaults = load_defaults_from_application_yml()
    conn = pymysql.connect(
        host=os.environ.get("JINBOOKS_DB_HOST", defaults["host"]),
        port=int(os.environ.get("JINBOOKS_DB_PORT", defaults["port"])),
        user=os.environ.get("JINBOOKS_DB_USER", defaults["user"]),
        password=os.environ.get("JINBOOKS_DB_PASSWORD", defaults["password"]),
        database=os.environ.get("JINBOOKS_DB_NAME", defaults["database"]),
        autocommit=False,
        charset="utf8mb4",
    )
    try:
        with conn.cursor() as cur:
            cur.execute("SHOW TABLES LIKE 'jbx\\_%'")
            jbx_count = len(cur.fetchall())
            print(f"jbx_* tables before migrations: {jbx_count}")

            for script in SCRIPTS:
                path = SQL_DIR / script
                print(f"\n==> {script}")
                sql = path.read_text(encoding="utf-8")
                for statement in split_sql(sql):
                    try:
                        cur.execute(statement)
                    except pymysql.MySQLError as exc:
                        if script.endswith("rename-tables.sql") and exc.args[0] == 1146:
                            print(f"skip missing table: {exc}")
                            continue
                        raise
                conn.commit()
                print("ok")

            cur.execute("SHOW TABLES LIKE 'jbx\\_%'")
            print(f"\njbx_* tables after migrations: {len(cur.fetchall())}")
            cur.execute("SELECT COUNT(*) FROM resources")
            print(f"resources rows: {cur.fetchone()[0]}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
