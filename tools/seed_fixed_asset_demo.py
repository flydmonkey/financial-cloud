# -*- coding: utf-8 -*-
"""Seed demo fixed-asset categories + cards for the active book (idempotent by code)."""
from __future__ import annotations

from datetime import datetime
from decimal import Decimal

import pymysql

BOOK_ID = "2093221400646053889"
TERM = "2026-08"
NOW = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

# known subjects from E2E book
SUBJ = {
    "1601": "2093221400772644914",
    "1602": "2093221400835559444",
    "1002": "2093221400835559445",
    "2221.01.01": "2093221400835559452",
    "5602.02": "2093221400772644953",
    "1606": "2093221400772644895",
}

EXISTING_CAT = "2093290768666992642"  # 001 机器机械生产设备


def connect():
    return pymysql.connect(
        host="127.0.0.1",
        port=3307,
        user="jinbooks",
        password="Jinbooks321!",
        database="jinbooks",
        charset="utf8mb4",
        autocommit=True,
    )


def upsert_org(cur, org_id: str, code: str, name: str) -> None:
    cur.execute("SELECT id FROM organizations WHERE id=%s", (org_id,))
    if cur.fetchone():
        return
    cur.execute(
        """
        INSERT INTO organizations (
            id, org_code, org_name, full_name, type, level,
            parent_id, parent_code, parent_name, code_path, name_path,
            description, status, book_id,
            created_by, created_date, modified_by, modified_date, deleted
        ) VALUES (
            %s, %s, %s, %s, 'department', 1,
            NULL, NULL, NULL, %s, %s,
            '固定资产演示部门', 1, %s,
            '1', %s, '1', %s, 'n'
        )
        """,
        (org_id, code, name, name, f"/{code}", f"/{name}", BOOK_ID, NOW, NOW),
    )


def upsert_category(cur, cat_id: str, code: str, name: str, method: str, months: int, rate: str) -> None:
    cur.execute(
        "SELECT id FROM asset_category WHERE book_id=%s AND code=%s AND deleted='n'",
        (BOOK_ID, code),
    )
    row = cur.fetchone()
    if row:
        return
    years = months // 12 if months else None
    cur.execute(
        """
        INSERT INTO asset_category (
            id, book_id, code, name, depreciation_method, useful_life_years, useful_life_months,
            residual_rate, fixed_asset_subject_id, accum_depr_subject_id, remark,
            created_by, created_date, modified_by, modified_date, deleted
        ) VALUES (
            %s, %s, %s, %s, %s, %s, %s,
            %s, %s, %s, '演示类别',
            '1', %s, '1', %s, 'n'
        )
        """,
        (
            cat_id,
            BOOK_ID,
            code,
            name,
            method,
            years,
            months,
            Decimal(rate),
            SUBJ["1601"],
            SUBJ["1602"],
            NOW,
            NOW,
        ),
    )


def upsert_asset(cur, asset: dict) -> None:
    cur.execute(
        "SELECT id FROM fixed_asset WHERE book_id=%s AND code=%s AND deleted='n'",
        (BOOK_ID, asset["code"]),
    )
    if cur.fetchone():
        print(f"skip asset {asset['code']}")
        return
    cur.execute(
        """
        INSERT INTO fixed_asset (
            id, book_id, code, name, category_id, dept_id, start_use_date, entry_period,
            quantity, spec, location, status, suspended_period,
            depreciation_method, useful_life_months, expected_total_work, residual_rate,
            original_value, tax_amount, impairment, depreciated_periods,
            opening_accum_depr, accum_depr, year_depr,
            fixed_asset_subject_id, purchase_counterpart_subject_id, tax_subject_id,
            accum_depr_subject_id, expense_subject_id, disposal_subject_id, remark,
            created_by, created_date, modified_by, modified_date, deleted
        ) VALUES (
            %(id)s, %(book_id)s, %(code)s, %(name)s, %(category_id)s, %(dept_id)s,
            %(start_use_date)s, %(entry_period)s,
            %(quantity)s, %(spec)s, %(location)s, %(status)s, %(suspended_period)s,
            %(depreciation_method)s, %(useful_life_months)s, %(expected_total_work)s, %(residual_rate)s,
            %(original_value)s, %(tax_amount)s, 0, %(depreciated_periods)s,
            %(opening_accum_depr)s, %(accum_depr)s, %(year_depr)s,
            %(fa_subj)s, %(cp_subj)s, %(tax_subj)s,
            %(accum_subj)s, %(exp_subj)s, %(disp_subj)s, %(remark)s,
            '1', %(now)s, '1', %(now)s, 'n'
        )
        """,
        asset,
    )
    print(f"inserted asset {asset['code']} {asset['name']}")


def main() -> int:
    conn = connect()
    cur = conn.cursor()

    # departments
    upsert_org(cur, "fa-demo-dept-admin", "FA-ADMIN", "行政部")
    upsert_org(cur, "fa-demo-dept-prod", "FA-PROD", "生产部")
    upsert_org(cur, "fa-demo-dept-sales", "FA-SALES", "销售部")

    # categories
    upsert_category(cur, "fa-demo-cat-002", "002", "电子设备", "STRAIGHT_LINE", 36, "5.0000")
    upsert_category(cur, "fa-demo-cat-003", "003", "运输工具", "DOUBLE_DECLINING", 48, "5.0000")
    upsert_category(cur, "fa-demo-cat-004", "004", "办公家具", "STRAIGHT_LINE", 60, "5.0000")
    upsert_category(cur, "fa-demo-cat-005", "005", "生产设备(加速)", "SUM_OF_YEARS", 60, "5.0000")
    upsert_category(cur, "fa-demo-cat-006", "006", "计量设备(工作量)", "UNITS_OF_PRODUCTION", 0, "5.0000")

    common = dict(
        book_id=BOOK_ID,
        fa_subj=SUBJ["1601"],
        cp_subj=SUBJ["1002"],
        tax_subj=SUBJ["2221.01.01"],
        accum_subj=SUBJ["1602"],
        exp_subj=SUBJ["5602.02"],
        disp_subj=SUBJ["1606"],
        now=NOW,
        suspended_period=None,
        expected_total_work=None,
        year_depr=Decimal("0.00"),
    )

    assets = [
        {
            **common,
            "id": "fa-demo-asset-001",
            "code": "FA-2026-001",
            "name": "联想ThinkPad笔记本",
            "category_id": "fa-demo-cat-002",
            "dept_id": "fa-demo-dept-admin",
            "start_use_date": "2026-07-15",
            "entry_period": "2026-07",
            "quantity": 1,
            "spec": "X1 Carbon",
            "location": "行政办公室",
            "status": "IN_USE",
            "depreciation_method": "STRAIGHT_LINE",
            "useful_life_months": 36,
            "residual_rate": Decimal("5.0000"),
            "original_value": Decimal("12000.00"),
            "tax_amount": Decimal("1560.00"),
            "depreciated_periods": 0,
            "opening_accum_depr": Decimal("0.00"),
            "accum_depr": Decimal("0.00"),
            "remark": "演示-平均年限法",
        },
        {
            **common,
            "id": "fa-demo-asset-002",
            "code": "FA-2026-002",
            "name": "五菱荣光厢式货车",
            "category_id": "fa-demo-cat-003",
            "dept_id": "fa-demo-dept-sales",
            "start_use_date": "2026-06-01",
            "entry_period": "2026-06",
            "quantity": 1,
            "spec": "1.5L",
            "location": "车库",
            "status": "IN_USE",
            "depreciation_method": "DOUBLE_DECLINING",
            "useful_life_months": 48,
            "residual_rate": Decimal("5.0000"),
            "original_value": Decimal("68000.00"),
            "tax_amount": Decimal("8840.00"),
            "depreciated_periods": 1,
            "opening_accum_depr": Decimal("2833.33"),
            "accum_depr": Decimal("2833.33"),
            "year_depr": Decimal("2833.33"),
            "remark": "演示-双倍余额递减法（已提1期）",
        },
        {
            **common,
            "id": "fa-demo-asset-003",
            "code": "FA-2026-003",
            "name": "数控铣床",
            "category_id": EXISTING_CAT,
            "dept_id": "fa-demo-dept-prod",
            "start_use_date": "2026-05-10",
            "entry_period": "2026-05",
            "quantity": 1,
            "spec": "XK7132",
            "location": "一车间",
            "status": "IN_USE",
            "depreciation_method": "SUM_OF_YEARS",
            "useful_life_months": 60,
            "residual_rate": Decimal("5.0000"),
            "original_value": Decimal("250000.00"),
            "tax_amount": Decimal("32500.00"),
            "depreciated_periods": 2,
            "opening_accum_depr": Decimal("15833.33"),
            "accum_depr": Decimal("15833.33"),
            "year_depr": Decimal("15833.33"),
            "remark": "演示-年数总和法",
        },
        {
            **common,
            "id": "fa-demo-asset-004",
            "code": "FA-2026-004",
            "name": "冲压机(按产量)",
            "category_id": "fa-demo-cat-006",
            "dept_id": "fa-demo-dept-prod",
            "start_use_date": "2026-07-01",
            "entry_period": "2026-07",
            "quantity": 1,
            "spec": "JH21-80",
            "location": "二车间",
            "status": "IN_USE",
            "depreciation_method": "UNITS_OF_PRODUCTION",
            "useful_life_months": None,
            "expected_total_work": Decimal("100000.0000"),
            "residual_rate": Decimal("5.0000"),
            "original_value": Decimal("180000.00"),
            "tax_amount": Decimal("23400.00"),
            "depreciated_periods": 0,
            "opening_accum_depr": Decimal("0.00"),
            "accum_depr": Decimal("0.00"),
            "remark": "演示-工作量法（计提前需录本期工作量）",
        },
        {
            **common,
            "id": "fa-demo-asset-005",
            "code": "FA-2026-005",
            "name": "办公桌椅套装",
            "category_id": "fa-demo-cat-004",
            "dept_id": "fa-demo-dept-admin",
            "start_use_date": "2026-08-01",
            "entry_period": "2026-08",
            "quantity": 10,
            "spec": "板式",
            "location": "行政楼3F",
            "status": "IN_USE",
            "depreciation_method": "STRAIGHT_LINE",
            "useful_life_months": 60,
            "residual_rate": Decimal("5.0000"),
            "original_value": Decimal("15000.00"),
            "tax_amount": Decimal("1950.00"),
            "depreciated_periods": 0,
            "opening_accum_depr": Decimal("0.00"),
            "accum_depr": Decimal("0.00"),
            "remark": "演示-本月新增（本月不提、次月起提）",
        },
        {
            **common,
            "id": "fa-demo-asset-006",
            "code": "FA-2026-006",
            "name": "备用发电机(暂停)",
            "category_id": EXISTING_CAT,
            "dept_id": "fa-demo-dept-prod",
            "start_use_date": "2026-04-01",
            "entry_period": "2026-04",
            "quantity": 1,
            "spec": "50kW",
            "location": "动力房",
            "status": "SUSPENDED",
            "suspended_period": "2026-08",
            "depreciation_method": "STRAIGHT_LINE",
            "useful_life_months": 120,
            "residual_rate": Decimal("5.0000"),
            "original_value": Decimal("42000.00"),
            "tax_amount": Decimal("5460.00"),
            "depreciated_periods": 3,
            "opening_accum_depr": Decimal("997.50"),
            "accum_depr": Decimal("997.50"),
            "year_depr": Decimal("997.50"),
            "remark": "演示-暂停计提（自2026-08起停提）",
        },
        {
            **common,
            "id": "fa-demo-asset-007",
            "code": "FA-2026-007",
            "name": "土地使用权(不提折旧)",
            "category_id": "fa-demo-cat-004",
            "dept_id": "fa-demo-dept-admin",
            "start_use_date": "2026-01-01",
            "entry_period": "2026-01",
            "quantity": 1,
            "spec": None,
            "location": "厂区",
            "status": "IN_USE",
            "depreciation_method": "NONE",
            "useful_life_months": 0,
            "residual_rate": Decimal("0.0000"),
            "original_value": Decimal("500000.00"),
            "tax_amount": Decimal("0.00"),
            "depreciated_periods": 0,
            "opening_accum_depr": Decimal("0.00"),
            "accum_depr": Decimal("0.00"),
            "exp_subj": None,
            "remark": "演示-不计提折旧",
        },
    ]

    for a in assets:
        upsert_asset(cur, a)

    # work amount for UOP asset current term
    cur.execute(
        "SELECT id FROM fixed_asset_work WHERE book_id=%s AND asset_id=%s AND year_period=%s AND deleted='n'",
        (BOOK_ID, "fa-demo-asset-004", TERM),
    )
    if not cur.fetchone():
        cur.execute(
            """
            INSERT INTO fixed_asset_work (
                id, book_id, asset_id, year_period, period_work,
                created_by, created_date, modified_by, modified_date, deleted
            ) VALUES (
                'fa-demo-work-004', %s, 'fa-demo-asset-004', %s, 2500.0000,
                '1', %s, '1', %s, 'n'
            )
            """,
            (BOOK_ID, TERM, NOW, NOW),
        )
        print(f"inserted work for FA-2026-004 period {TERM}=2500")

    cur.execute(
        "SELECT code, name, status, depreciation_method, original_value FROM fixed_asset "
        "WHERE book_id=%s AND deleted='n' ORDER BY code",
        (BOOK_ID,),
    )
    print("\n=== assets now ===")
    for r in cur.fetchall():
        print(r)
    cur.execute(
        "SELECT code, name FROM asset_category WHERE book_id=%s AND deleted='n' ORDER BY code",
        (BOOK_ID,),
    )
    print("\n=== categories ===")
    for r in cur.fetchall():
        print(r)

    conn.close()
    print("\nOK demo data ready. Current term:", TERM)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
