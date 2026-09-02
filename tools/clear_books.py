#!/usr/bin/env python3
"""Remove all books and related data so onboarding can be tested from scratch."""
from __future__ import annotations

import pymysql

HOST = "127.0.0.1"
PORT = 3307
DB = "financial_cloud"
USER = "financial_cloud"
PASSWORD = "FinancialCloud321!"

# Tables with book_id that hold global/template rows, not per-book business data.
SKIP_TABLES = {
    "userinfo",
    "permission",
    "role_member",
    "config_login_policy",
    "institutions",
    "socials_provider",
}


def main() -> int:
    conn = pymysql.connect(
        host=HOST,
        port=PORT,
        user=USER,
        password=PASSWORD,
        database=DB,
        autocommit=False,
    )
    cur = conn.cursor()

    cur.execute("SELECT id, name FROM book")
    books = cur.fetchall()
    print("books before:", books)

    if not books:
        print("no books to clear")
    else:
        book_ids = [row[0] for row in books]
        placeholders = ",".join(["%s"] * len(book_ids))

        cur.execute(
            """
            SELECT TABLE_NAME FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = %s AND COLUMN_NAME = 'book_id'
            ORDER BY TABLE_NAME
            """,
            (DB,),
        )
        tables = [row[0] for row in cur.fetchall() if row[0] not in SKIP_TABLES]

        cur.execute("SET FOREIGN_KEY_CHECKS=0")
        for table in tables:
            if table == "config":
                # 保留 template 全局模板配置，仅清理账套配置
                sql = (
                    f"DELETE FROM `{table}` WHERE book_id IN ({placeholders}) "
                    "AND book_id <> 'template'"
                )
            else:
                sql = f"DELETE FROM `{table}` WHERE book_id IN ({placeholders})"
            cur.execute(sql, book_ids)
            print(f"  cleared {table}: {cur.rowcount} rows")

        cur.execute("DELETE FROM permission_book")
        print(f"  cleared permission_book: {cur.rowcount} rows")

        cur.execute("DELETE FROM book")
        print(f"  cleared book: {cur.rowcount} rows")
        cur.execute("SET FOREIGN_KEY_CHECKS=1")

    cur.execute("UPDATE userinfo SET book_id = ''")
    print(f"  reset userinfo book_id: {cur.rowcount} rows")

    conn.commit()

    cur.execute("SELECT COUNT(*) FROM book")
    print("books after:", cur.fetchone()[0])
    cur.execute("SELECT COUNT(*) FROM permission_book")
    print("permission_book after:", cur.fetchone()[0])
    cur.execute("SELECT username, book_id FROM userinfo WHERE username = 'admin'")
    print("admin:", cur.fetchone())
    cur.execute("SELECT COUNT(*) FROM config_cash_flow_balance WHERE book_id IS NULL")
    print("cash flow templates:", cur.fetchone()[0])

    conn.close()
    print("done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
