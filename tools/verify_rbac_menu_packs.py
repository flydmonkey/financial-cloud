"""Assert menu packs for four product roles against the rbac matrix.

DB via env: JB_DB_HOST / JB_DB_PORT / JB_DB_USER / JB_DB_PASSWORD / JB_DB_NAME
"""
from __future__ import annotations

import os

import pymysql

# Top-level menu names after rename: former 账套管理 root → 系统设置
EXPECTED = {
    "ROLE_BOOKKEEPER": {
        "must": ["仪表盘", "凭证", "账簿", "报表", "出纳", "固定资产", "薪资", "往来管理", "系统设置", "基础设置"],
        "must_not": ["结账", "日志审计"],
    },
    "ROLE_REVIEWER": {
        "must": ["仪表盘", "凭证", "账簿", "报表", "结账", "出纳", "固定资产", "薪资", "往来管理", "系统设置", "基础设置"],
        "must_not": [],
        # 日志审计 may be hidden in resources.status; do not require it as must
    },
    "ROLE_VIEWER": {
        "must": ["仪表盘", "凭证", "账簿", "报表", "往来管理"],
        "must_not": ["结账", "出纳", "固定资产", "薪资", "系统设置", "基础设置", "日志审计"],
    },
}

TOP = {
    "981331493802475520": "仪表盘",
    "1869692874272862209": "凭证",
    "2026082817000000001": "账簿",
    "1886357455563137026": "报表",
    "1917420357065609218": "结账",
    "1881534934875557889": "出纳",
    "2026082818000000001": "固定资产",
    "981334321270882304": "薪资",
    "2026090315000000001": "往来管理",
    "981334814802051072": "系统设置",
    "1915219176348123138": "基础设置",
    "981334679749656576": "系统设置(已下线)",
    "981334866064834560": "日志审计",
}


def main() -> None:
    conn = pymysql.connect(
        host=os.environ.get("JB_DB_HOST", "127.0.0.1"),
        port=int(os.environ.get("JB_DB_PORT", "3307")),
        user=os.environ.get("JB_DB_USER", "financial_cloud"),
        password=os.environ.get("JB_DB_PASSWORD", ""),
        database=os.environ.get("JB_DB_NAME", "financial_cloud"),
        charset="utf8mb4",
    )
    cur = conn.cursor()
    errors = []
    for role_id, expect in EXPECTED.items():
        cur.execute(
            "select distinct resource_id from permission where role_id=%s and status=1",
            (role_id,),
        )
        resources = {r[0] for r in cur.fetchall()}
        tops = {name for rid, name in TOP.items() if rid in resources}
        for name in expect["must"]:
            if name not in tops:
                errors.append(f"{role_id} missing top menu {name}")
        for name in expect.get("must_not", []):
            if name in tops:
                errors.append(f"{role_id} should not have top menu {name}")
        if role_id in ("ROLE_BOOKKEEPER", "ROLE_REVIEWER") and "1899760631214723073" in resources:
            errors.append(f"{role_id} must not include 系统参数 resource")
        if role_id in ("ROLE_BOOKKEEPER", "ROLE_REVIEWER", "ROLE_VIEWER") and "981335810039087104" in resources:
            errors.append(f"{role_id} must not include 角色管理 resource")
    conn.close()
    if errors:
        raise SystemExit("FAIL:\n" + "\n".join(errors))
    print("OK: menu packs match matrix")


if __name__ == "__main__":
    main()
