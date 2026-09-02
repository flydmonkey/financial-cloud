#!/usr/bin/env python3
"""Apply ledger books menu seed (idempotent) and verify structure."""
from pathlib import Path
import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "sql" / "seed" / "ledger_books_menu.sql"

LEDGER_ID = "2026082817000000001"
SUB_LEDGER_ID = "1903024792422047745"
GENERAL_LEDGER_ID = "2026082816300000001"
SUBJECT_BALANCE_ID = "1886384516205912065"
REPORT_ID = "1886357455563137026"
EXPENSE_DETAIL_ID = "2026082814300000001"
VOUCHER_ID = "1869692874272862209"
SETTLE_ID = "1917420357065609218"


def verify(cur) -> None:
    cur.execute(
        "SELECT id, res_name, sort_index FROM resources "
        "WHERE parent_id='1' AND deleted='n' AND classify='MENU' "
        "ORDER BY sort_index, id"
    )
    tops = cur.fetchall()
    names = [r[1] for r in tops]
    for name in ("凭证", "账簿", "报表", "结账"):
        assert name in names, names
    vi = names.index("凭证")
    li = names.index("账簿")
    ri = names.index("报表")
    si = names.index("结账")
    assert li == vi + 1 and ri == li + 1 and si == ri + 1, (
        f"期望 凭证→账簿→报表→结账 连续: {names}"
    )
    by_id = {r[0]: r for r in tops}
    assert by_id[VOUCHER_ID][2] == 2, by_id[VOUCHER_ID]
    assert by_id[LEDGER_ID][2] == 3, by_id[LEDGER_ID]
    assert by_id[REPORT_ID][2] == 4, by_id[REPORT_ID]
    assert by_id[SETTLE_ID][2] == 5, by_id[SETTLE_ID]

    cur.execute(
        "SELECT sort_index, icon, res_style FROM resources WHERE id=%s AND deleted='n'",
        (LEDGER_ID,),
    )
    row = cur.fetchone()
    assert row and row[0] == 3, f"账簿 sort_index 应为 3: {row}"
    assert row[1] is None, f"账簿 icon 应为 NULL: {row}"
    assert row[2] == "account-book", f"账簿 res_style 应为 account-book: {row}"

    cur.execute(
        "SELECT id, res_name, sort_index, request_url FROM resources "
        "WHERE parent_id=%s AND deleted='n' ORDER BY sort_index, id",
        (LEDGER_ID,),
    )
    children = cur.fetchall()
    assert [c[0] for c in children] == [
        SUB_LEDGER_ID,
        GENERAL_LEDGER_ID,
        SUBJECT_BALANCE_ID,
    ], children
    assert [c[1] for c in children] == ["明细账", "总账", "科目余额表"], children
    assert [c[2] for c in children] == [1, 2, 3], children
    assert [c[3] for c in children] == [
        "/voucher/sub-ledger",
        "/statement/general-ledger",
        "/statement/subject-balance",
    ], children

    cur.execute(
        "SELECT parent_id FROM resources WHERE id=%s",
        (EXPENSE_DETAIL_ID,),
    )
    row = cur.fetchone()
    assert row and row[0] == REPORT_ID, f"费用明细表应仍在报表下: {row}"

    cur.execute(
        "SELECT COUNT(*) FROM resources WHERE parent_id=%s AND id IN (%s,%s,%s)",
        (REPORT_ID, SUB_LEDGER_ID, GENERAL_LEDGER_ID, SUBJECT_BALANCE_ID),
    )
    assert cur.fetchone()[0] == 0, "报表下不应再挂明细账/总账/科目余额表"

    cur.execute(
        "SELECT id FROM permission WHERE resource_id=%s AND role_id='ROLE_ADMINISTRATORS'",
        (LEDGER_ID,),
    )
    assert cur.fetchone(), "缺少账簿父菜单管理员权限"


def main() -> int:
    sql = SQL.read_text(encoding="utf-8")
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
            with conn.cursor() as cur:
                cur.execute(sql)
                verify(cur)
                # 幂等：再执行一次仍通过
                cur.execute(sql)
                verify(cur)
            conn.close()
            print(f"OK applied+verified {SQL.name} as {user} (twice)")
            return 0
        except Exception as e:
            print(f"fail {user}: {e}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
