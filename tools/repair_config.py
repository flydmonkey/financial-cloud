"""Repair config rows corrupted by updateByKey without book_id filter."""
from __future__ import annotations

import pymysql

HOST = "127.0.0.1"
PORT = 3307
DB = "jinbooks"
USER = "jinbooks"
PASSWORD = "Jinbooks321!"


def main() -> int:
    conn = pymysql.connect(
        host=HOST, port=PORT, user=USER, password=PASSWORD, database=DB, autocommit=False
    )
    cur = conn.cursor()

    cur.execute("SELECT id FROM book")
    live_books = {row[0] for row in cur.fetchall()}
    print("live books:", len(live_books))

    cur.execute("SELECT DISTINCT book_id FROM config WHERE book_id NOT IN ('template', '')")
    all_book_ids = [row[0] for row in cur.fetchall() if row[0]]
    orphans = [bid for bid in all_book_ids if bid not in live_books]
    print("orphan book config groups:", len(orphans))

    if orphans:
        placeholders = ",".join(["%s"] * len(orphans))
        cur.execute(f"DELETE FROM config WHERE book_id IN ({placeholders})", orphans)
        print("deleted orphan config rows:", cur.rowcount)

    cur.execute(
        "SELECT COUNT(*) FROM config WHERE config_key=%s AND book_id=%s",
        ("sys.payment.term.current", "template"),
    )
    if cur.fetchone()[0] == 0:
        cur.execute(
            """
            INSERT INTO config (
                config_id, book_id, config_name, config_key, config_value, config_type,
                remark, created_by, created_date, modified_by, modified_date
            )
            SELECT
                '1923586195398561795', 'template', '当前账期', 'sys.payment.term.current',
                COALESCE(
                    (SELECT config_value FROM config
                     WHERE config_key='sys.payment.term.start' AND book_id='template' LIMIT 1),
                    '2025-03'
                ),
                'y', '系统内置，不可删除', '1', NOW(), '1', NOW()
            FROM DUAL
            """
        )
        print("recreated template sys.payment.term.current")

    for book_id in live_books:
        cur.execute(
            """
            SELECT config_id FROM config
            WHERE book_id=%s AND config_key='sys.payment.term.current'
            ORDER BY config_id
            """,
            (book_id,),
        )
        ids = [row[0] for row in cur.fetchall()]
        if len(ids) > 1:
            drop = ids[1:]
            placeholders = ",".join(["%s"] * len(drop))
            cur.execute(f"DELETE FROM config WHERE config_id IN ({placeholders})", drop)
            print(f"deduped current term for {book_id}: dropped {len(drop)}")

    cur.execute(
        "SELECT COUNT(*) FROM config WHERE config_key='sys.payment.term.current'"
    )
    print("current key count after:", cur.fetchone()[0])
    cur.execute(
        "SELECT book_id, config_value FROM config WHERE config_key='sys.payment.term.current'"
    )
    print("current rows:", cur.fetchall())

    conn.commit()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
