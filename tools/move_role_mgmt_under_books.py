"""Move 角色管理 under 账套管理 and hide 系统设置 menu root.

DB connection via env (same defaults as other local tools):
  JB_DB_HOST / JB_DB_PORT / JB_DB_USER / JB_DB_PASSWORD / JB_DB_NAME
"""
from __future__ import annotations

import os

import pymysql

SYS_SETTINGS = "981334679749656576"
ROLE_MGMT = "981335810039087104"
BOOK_ROOT = "981334814802051072"


def main() -> None:
    conn = pymysql.connect(
        host=os.environ.get("JB_DB_HOST", "127.0.0.1"),
        port=int(os.environ.get("JB_DB_PORT", "3307")),
        user=os.environ.get("JB_DB_USER", "financial_cloud"),
        password=os.environ.get("JB_DB_PASSWORD", ""),
        database=os.environ.get("JB_DB_NAME", "financial_cloud"),
        charset="utf8mb4",
        autocommit=False,
    )
    cur = conn.cursor()

    cur.execute(
        """
        update resources
        set parent_id=%s,
            parent_name='账套管理',
            sort_index=8,
            status='1',
            deleted='n',
            is_visible='y',
            modified_date=now()
        where id=%s
        """,
        (BOOK_ROOT, ROLE_MGMT),
    )
    print("moved role mgmt rows:", cur.rowcount)

    cur.execute(
        """
        update resources
        set status='0', modified_date=now()
        where id=%s or parent_id=%s
        """,
        (SYS_SETTINGS, SYS_SETTINGS),
    )
    print("hidden system settings rows:", cur.rowcount)

    cur.execute("delete from permission where resource_id=%s", (SYS_SETTINGS,))
    print("removed sys settings permissions:", cur.rowcount)

    cur.execute(
        "select id from permission where role_id=%s and resource_id=%s limit 1",
        ("ROLE_ADMINISTRATORS", ROLE_MGMT),
    )
    if not cur.fetchone():
        cur.execute(
            """
            insert into permission (id, role_id, resource_id, created_by, status, book_id)
            values (%s, 'ROLE_ADMINISTRATORS', %s, '1', 1, '1')
            """,
            (f"move{ROLE_MGMT[-12:]}", ROLE_MGMT),
        )
        print("restored admin perm for role mgmt")

    cur.execute(
        "select id from permission where role_id=%s and resource_id=%s limit 1",
        ("ROLE_ADMINISTRATORS", BOOK_ROOT),
    )
    if not cur.fetchone():
        cur.execute(
            """
            insert into permission (id, role_id, resource_id, created_by, status, book_id)
            values (%s, 'ROLE_ADMINISTRATORS', %s, '1', 1, '1')
            """,
            (f"move{BOOK_ROOT[-12:]}", BOOK_ROOT),
        )
        print("restored admin perm for book root")

    conn.commit()

    cur.execute(
        """
        select id, res_name, parent_id, parent_name, status, sort_index
        from resources
        where id in (%s,%s,%s) or (parent_id=%s and deleted='n' and status='1')
        order by parent_id, sort_index, id
        """,
        (SYS_SETTINGS, ROLE_MGMT, BOOK_ROOT, BOOK_ROOT),
    )
    print("result:")
    for row in cur.fetchall():
        print(row)
    conn.close()


if __name__ == "__main__":
    main()
