"""Generate and optionally apply rbac-four-roles SQL seed."""
from __future__ import annotations

import pymysql

# Module resource IDs (root + descendants) from DB exploration
DASHBOARD = ["981331493802475520"]
VOUCHER = [
    "1869692874272862209",
    "1879553833064067074",
    "1879028005231357953",
    "1891486309700673537",
]
LEDGER = [
    "2026082817000000001",
    "1903024792422047745",
    "2026082816300000001",
    "1886384516205912065",
]
REPORT = [
    "1886357455563137026",
    "1886366126259052545",
    "1886384073945915394",
    "1886384309938429954",
    "2026082814300000001",
]
SETTLE = [
    "1917420357065609218",
    "1917421261886033922",
    "1917421313257869313",
    "1917421497123573762",
]
JOURNAL = [
    "1881534934875557889",
    "1881535430596153345",
    "1881535629171281921",
    "1888073658178420737",
]
FIXED_ASSET = [
    "2026082818000000001",
    "2026082818000000011",
    "2026082818000000021",
    "2026082818000000031",
    "2026082818000000041",
    "2026082818000000051",
    "2026082818000000061",
]
# salary filled from DB at runtime
ARAP = [
    "2026090315000000001",
    "2026090315000000002",
    "2026090315000000003",
    "2026090315000000004",
    "2026090315000000005",
]
BOOK_MGMT_P = [
    "981334814802051072",  # 系统设置 root
    "1874027145762447361",  # books list
    "1915219176348123138",  # 基础设置 root (init balance / assist / cashflow)
    "1899369820127911938",  # init balance
    "981623658751459329",  # assist
    "1902625741973843969",  # cash flow balance
    "1913072049310191618",  # subject cash flow
]
AUDIT_P = [
    "981334866064834560",
    "981337003041751040",
    "981337181773627392",
]

LEGACY = {
    "1880191154616516610": "ROLE_BOOKKEEPER",
    "1880191529151086594": "ROLE_BOOKKEEPER",
    "1880191070453612545": "ROLE_BOOKKEEPER",
    "1880191264779911169": "ROLE_REVIEWER",
    "1880190696367833089": "ROLE_REVIEWER",
    "1880191529151086593": "ROLE_VIEWER",
}


def fetch_salary_ids(cur) -> list[str]:
    root = "981334321270882304"
    cur.execute(
        """
        with recursive tree as (
          select id from resources where id=%s
          union all
          select r.id from resources r join tree t on r.parent_id=t.id
        )
        select id from tree
        """,
        (root,),
    )
    return [r[0] for r in cur.fetchall()]


def fetch_all_under(cur, root: str) -> list[str]:
    cur.execute(
        """
        with recursive tree as (
          select id from resources where id=%s
          union all
          select r.id from resources r join tree t on r.parent_id=t.id
        )
        select id from tree
        """,
        (root,),
    )
    return [r[0] for r in cur.fetchall()]


def main() -> None:
    conn = pymysql.connect(
        host="127.0.0.1",
        port=3307,
        user="financial_cloud",
        password="FinancialCloud321!",
        database="financial_cloud",
        charset="utf8mb4",
        autocommit=False,
    )
    cur = conn.cursor()

    salary = fetch_salary_ids(cur)
    # Also expand modules that might have more children than hardcoded
    voucher = fetch_all_under(cur, "1869692874272862209")
    ledger = fetch_all_under(cur, "2026082817000000001")
    report = fetch_all_under(cur, "1886357455563137026")
    settle = fetch_all_under(cur, "1917420357065609218")
    journal = fetch_all_under(cur, "1881534934875557889")
    fixed = fetch_all_under(cur, "2026082818000000001")
    arap = fetch_all_under(cur, "2026090315000000001")

    bookkeeper = sorted(
        set(
            DASHBOARD
            + voucher
            + ledger
            + report
            + journal
            + fixed
            + salary
            + arap
            + BOOK_MGMT_P
        )
    )
    reviewer = sorted(
        set(
            DASHBOARD
            + voucher
            + ledger
            + report
            + settle
            + journal
            + fixed
            + salary
            + arap
            + BOOK_MGMT_P
            + AUDIT_P
        )
    )
    viewer = sorted(set(DASHBOARD + voucher + ledger + report + arap))

    # 1) Ensure product roles
    cur.execute(
        "update roles set role_name=%s, modified_date=now() where id=%s",
        ("管理员", "ROLE_ADMINISTRATORS"),
    )
    for rid, code, name in [
        ("ROLE_BOOKKEEPER", "BOOKKEEPER", "做账员"),
        ("ROLE_REVIEWER", "REVIEWER", "审核员"),
        ("ROLE_VIEWER", "VIEWER", "查看员"),
    ]:
        cur.execute("select id from roles where id=%s", (rid,))
        if cur.fetchone():
            cur.execute(
                "update roles set role_code=%s, role_name=%s, category='general', "
                "status=1, deleted='n', modified_date=now() where id=%s",
                (code, name, rid),
            )
        else:
            cur.execute(
                """
                insert into roles (
                  id, role_code, role_name, category, pattern, filters, org_ids_list,
                  status, created_by, isdefault, created_date, modified_by, modified_date,
                  description, deleted
                ) values (
                  %s, %s, %s, 'general', 'static', null, '',
                  1, '1', 0, now(), '1', now(),
                  %s, 'n'
                )
                """,
                (rid, code, name, f"product role {name}"),
            )

    # 2) Remap legacy members
    for legacy_id, target in LEGACY.items():
        cur.execute(
            "select ID, member_id, type, book_id, created_by from role_member where role_id=%s",
            (legacy_id,),
        )
        for mid, member_id, typ, book_id, created_by in cur.fetchall():
            cur.execute(
                "select ID from role_member where role_id=%s and member_id=%s",
                (target, member_id),
            )
            if not cur.fetchone():
                new_id = f"rbac{mid[-12:]}" if len(mid) >= 12 else f"rbac{mid}"
                cur.execute(
                    """
                    insert into role_member (ID, role_id, member_id, type, created_by, created_date, book_id)
                    values (%s, %s, %s, %s, %s, now(), %s)
                    """,
                    (new_id, target, member_id, typ or "USER", created_by or "1", book_id or "1"),
                )
            cur.execute("delete from role_member where ID=%s", (mid,))

    # Soft-delete legacy roles
    for legacy_id in LEGACY:
        cur.execute(
            "update roles set deleted='y', status=0, modified_date=now() where id=%s",
            (legacy_id,),
        )

    # 3) Replace permission packs for the three roles (template book_id=1)
    def seed_pack(role_id: str, resource_ids: list[str]) -> None:
        cur.execute("delete from permission where role_id=%s", (role_id,))
        for i, rid in enumerate(resource_ids):
            tag = role_id.replace("ROLE_", "")[:8]
            pid = f"p{tag}{i:04d}{rid[-10:]}"[:100]
            cur.execute(
                """
                insert into permission (id, role_id, resource_id, created_by, created_date, status, book_id)
                values (%s, %s, %s, '1', now(), 1, '1')
                """,
                (pid, role_id, rid),
            )

    seed_pack("ROLE_BOOKKEEPER", bookkeeper)
    seed_pack("ROLE_REVIEWER", reviewer)
    seed_pack("ROLE_VIEWER", viewer)

    # 4) Backfill: each permission_book must have a book-scoped role_member
    cur.execute(
        """
        select distinct pb.user_id, pb.book_id
        from permission_book pb
        where pb.deleted='n'
          and not exists (
            select 1 from role_member rm
            where rm.member_id=pb.user_id and rm.book_id=pb.book_id
          )
        """
    )
    for user_id, book_id in cur.fetchall():
        cur.execute(
            """
            select role_id from role_member
            where member_id=%s
            order by case when role_id='ROLE_ADMINISTRATORS' then 0 else 1 end
            limit 1
            """,
            (user_id,),
        )
        row = cur.fetchone()
        role_id = row[0] if row else "ROLE_VIEWER"
        nid = f"bf{user_id[-10:]}{str(book_id)[-6:]}"[:100]
        cur.execute(
            """
            insert into role_member (ID, role_id, member_id, type, created_by, created_date, book_id)
            values (%s, %s, %s, 'USER', '1', now(), %s)
            """,
            (nid, role_id, user_id, book_id or "1"),
        )

    conn.commit()

    # Verify
    cur.execute(
        "select id, role_code, role_name, deleted from roles "
        "where id in ('ROLE_ADMINISTRATORS','ROLE_BOOKKEEPER','ROLE_REVIEWER','ROLE_VIEWER') "
        "or deleted='n' order by role_code"
    )
    print("ROLES:")
    for r in cur.fetchall():
        print(r)
    for role in ("ROLE_BOOKKEEPER", "ROLE_REVIEWER", "ROLE_VIEWER"):
        cur.execute("select count(*) from permission where role_id=%s", (role,))
        print(role, "perms", cur.fetchone()[0])
    cur.execute(
        """
        select count(*) from permission_book pb
        where pb.deleted='n' and not exists (
          select 1 from role_member rm
          where rm.member_id=pb.user_id and rm.book_id=pb.book_id
        )
        """
    )
    print("books without book-scoped role:", cur.fetchone()[0])
    conn.close()


if __name__ == "__main__":
    main()
