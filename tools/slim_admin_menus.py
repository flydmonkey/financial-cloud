"""Hide leftover system-settings / audit menus after 角色管理 moved under 账套管理.

DB via env: JB_DB_HOST / JB_DB_PORT / JB_DB_USER / JB_DB_PASSWORD / JB_DB_NAME
"""
from __future__ import annotations

import os

import pymysql

SYS_SETTINGS = "981334679749656576"
ROLE_MGMT = "981335810039087104"
BOOK_ROOT = "981334814802051072"  # side menu 「系统设置」(formerly 账套管理)
AUDIT_ROOT = "981334866064834560"


def collect_descendants(cur, root_id: str) -> list[str]:
    cur.execute(
        """
        with recursive t as (
          select id from resources where id=%s
          union all
          select r.id from resources r join t on r.parent_id=t.id
        )
        select id from t
        """,
        (root_id,),
    )
    return [r[0] for r in cur.fetchall()]


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

    # Soft-hide entire 系统设置 + 日志审计 trees (角色管理 already under 账套管理)
    hide_ids = collect_descendants(cur, SYS_SETTINGS) + collect_descendants(cur, AUDIT_ROOT)
    # Never hide 角色管理 if somehow still linked
    hide_ids = sorted({i for i in hide_ids if i != ROLE_MGMT})

    if hide_ids:
        fmt = ",".join(["%s"] * len(hide_ids))
        cur.execute(
            f"update resources set status='0', modified_date=now() where id in ({fmt})",
            hide_ids,
        )
        print("hidden resources:", cur.rowcount)
        cur.execute(f"delete from permission where resource_id in ({fmt})", hide_ids)
        print("removed permission rows:", cur.rowcount)

    # Ensure 角色管理 stays under 账套管理 and visible
    cur.execute(
        """
        update resources
        set parent_id=%s, parent_name='账套管理', sort_index=8,
            status='1', deleted='n', is_visible='y', modified_date=now()
        where id=%s
        """,
        (BOOK_ROOT, ROLE_MGMT),
    )

    for rid in (BOOK_ROOT, ROLE_MGMT):
        cur.execute(
            "select id from permission where role_id=%s and resource_id=%s limit 1",
            ("ROLE_ADMINISTRATORS", rid),
        )
        if not cur.fetchone():
            cur.execute(
                """
                insert into permission (id, role_id, resource_id, created_by, status, book_id)
                values (%s, 'ROLE_ADMINISTRATORS', %s, '1', 1, '1')
                """,
                (f"slim{rid[-12:]}", rid),
            )
            print("restored admin perm for", rid)

    conn.commit()
    cur.execute(
        """
        select id, res_name, parent_id, status
        from resources
        where id in (%s,%s,%s,%s) or parent_id=%s
        order by id
        """,
        (SYS_SETTINGS, ROLE_MGMT, BOOK_ROOT, AUDIT_ROOT, BOOK_ROOT),
    )
    for row in cur.fetchall():
        print(row)
    conn.close()


if __name__ == "__main__":
    main()
