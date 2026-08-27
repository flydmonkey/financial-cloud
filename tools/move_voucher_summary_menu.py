#!/usr/bin/env python3
"""Move voucher summary menu from report to voucher menu."""
from __future__ import annotations

import pymysql

VOUCHER_SUMMARY_ID = "1891486309700673537"
VOUCHER_MENU_ID = "1869692874272862209"
REPORT_MENU_ID = "1886357455563137026"

HOST = "127.0.0.1"
PORT = 3307
DB = "jinbooks"
USER = "jinbooks"
PASSWORD = "Jinbooks321!"


def main() -> int:
    conn = pymysql.connect(
        host=HOST, port=PORT, user=USER, password=PASSWORD, database=DB, autocommit=True
    )
    cur = conn.cursor()
    cur.execute(
        """
        UPDATE resources
        SET parent_id = %s, parent_name = '凭证', sort_index = 3
        WHERE id = %s
        """,
        (VOUCHER_MENU_ID, VOUCHER_SUMMARY_ID),
    )
    print(f"updated rows: {cur.rowcount}")

    cur.execute(
        """
        SELECT id, res_name, parent_id, parent_name, sort_index, request_url
        FROM resources
        WHERE id = %s OR parent_id = %s
        ORDER BY parent_id, sort_index
        """,
        (VOUCHER_SUMMARY_ID, VOUCHER_MENU_ID),
    )
    for row in cur.fetchall():
        print(row)

    cur.execute(
        """
        SELECT id, res_name, sort_index, request_url
        FROM resources
        WHERE parent_id = %s
        ORDER BY sort_index
        """,
        (REPORT_MENU_ID,),
    )
    print("\nreport children:")
    for row in cur.fetchall():
        print(row)

    conn.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
