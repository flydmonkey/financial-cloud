#!/usr/bin/env python3
"""Move insurance/tax menus under salary; rename 配置管理 → 系统设置."""
from pathlib import Path
import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "sql" / "seed" / "menu_salary_tax_and_rename_config.sql"

SALARY_ID = "981334321270882304"
INSURANCE_ID = "1889594633392771074"
TAX_RATE_ID = "1887317090379808769"
CONFIG_ID = "981334679749656576"


def verify(cur) -> None:
    cur.execute(
        "SELECT parent_id, parent_name, sort_index, request_url FROM resources WHERE id=%s",
        (INSURANCE_ID,),
    )
    row = cur.fetchone()
    assert row and row[0] == SALARY_ID and row[1] == "薪资" and row[2] == 8, row
    assert row[3] == "/config/insurance-fund", row

    cur.execute(
        "SELECT parent_id, parent_name, sort_index, request_url FROM resources WHERE id=%s",
        (TAX_RATE_ID,),
    )
    row = cur.fetchone()
    assert row and row[0] == SALARY_ID and row[1] == "薪资" and row[2] == 9, row
    assert row[3] == "/config/tax", row

    cur.execute(
        "SELECT res_name, parent_id FROM resources WHERE id=%s",
        (CONFIG_ID,),
    )
    row = cur.fetchone()
    assert row and row[0] == "系统设置" and row[1] == "1", row

    cur.execute(
        "SELECT COUNT(*) FROM resources WHERE parent_id=%s AND parent_name<>'系统设置' AND deleted='n'",
        (CONFIG_ID,),
    )
    assert cur.fetchone()[0] == 0, "系统设置子菜单 parent_name 未全部同步"

    cur.execute(
        "SELECT COUNT(*) FROM resources WHERE parent_id=%s AND id IN (%s,%s) AND deleted='n'",
        ("981334814802051072", INSURANCE_ID, TAX_RATE_ID),
    )
    assert cur.fetchone()[0] == 0, "账套管理下不应再挂社保/税率"


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
