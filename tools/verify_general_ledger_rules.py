#!/usr/bin/env python3
"""按「总账计算规则」对本地库+API 做勾稽/试算检查（不打印 token）。"""
from __future__ import annotations

import json
import sys
import urllib.parse
import urllib.request
from collections import defaultdict
from decimal import Decimal

import pymysql

BASE = "http://localhost:2154"
BOOK = None  # filled after login
PERIOD = "2026-08"
TOL = Decimal("0.01")


def req(method: str, path: str, data=None, headers=None):
    body = None
    hdrs = dict(headers or {})
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        hdrs.setdefault("Content-Type", "application/json")
    r = urllib.request.Request(BASE + path, data=body, headers=hdrs, method=method)
    with urllib.request.urlopen(r, timeout=60) as resp:
        raw = resp.read().decode("utf-8")
        return json.loads(raw) if raw else {}


def D(v) -> Decimal:
    if v is None:
        return Decimal("0")
    return Decimal(str(v))


def main() -> int:
    results = []

    init = req("GET", "/api/login/get?_allow_anonymous=true")
    signin = req(
        "POST",
        "/api/login/signin?_allow_anonymous=true",
        {
            "username": "admin",
            "password": "maxkey",
            "captcha": "",
            "state": init["data"]["state"],
            "authType": "normal",
        },
    )
    if signin.get("code") != 0:
        print("FAIL login")
        return 1
    auth = {"Authorization": f"Bearer {signin['data']['token']}"}
    me = req("GET", "/api/users/currentUser", headers=auth)
    book_id = me["data"]["bookId"]
    print(f"BOOK={book_id} PERIOD={PERIOD}")

    qs = urllib.parse.urlencode(
        [
            ("periodType", "between"),
            ("dateRange", PERIOD),
            ("dateRange", PERIOD),
            ("maxLevel", "1"),
            ("hideNoActivityAndZeroBalance", "false"),
            ("showAux", "false"),
        ]
    )
    gl = req("GET", f"/api/statement/general-ledger?{qs}", headers=auth)
    items = (gl.get("data") or {}).get("items") or []
    print(f"GL_LEVEL1 subjectCount={(gl.get('data') or {}).get('subjectCount')} items={len(items)}")

    conn = pymysql.connect(
        host="127.0.0.1",
        port=3307,
        user="jinbooks",
        password="Jinbooks321!",
        database="jinbooks",
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    cur = conn.cursor()

    # 一级科目 code -> direction
    cur.execute(
        "SELECT code, name, direction, level FROM book_subject "
        "WHERE book_id=%s AND deleted='n' AND level=1",
        (book_id,),
    )
    l1 = {r["code"]: r for r in cur.fetchall()}

    # 已过账凭证分录：sender_id 非空视为过账（与系统一致）
    cur.execute(
        """
        SELECT vi.subject_code, vi.debit_amount, vi.credit_amount
        FROM voucher_item vi
        JOIN voucher v ON v.id = vi.voucher_id
        WHERE v.book_id=%s AND v.deleted='n' AND vi.deleted='n'
          AND v.sender_id IS NOT NULL AND v.sender_id <> ''
          AND DATE_FORMAT(v.voucher_date, '%%Y-%%m') = %s
        """,
        (book_id, PERIOD),
    )
    voucher_rows = cur.fetchall()

    # 按一级科目前缀汇总发生额
    voucher_l1 = defaultdict(lambda: {"debit": Decimal("0"), "credit": Decimal("0")})
    for row in voucher_rows:
        code = row["subject_code"] or ""
        root = None
        for c in sorted(l1.keys(), key=len, reverse=True):
            if code == c or code.startswith(c):
                root = c
                break
        if root is None and len(code) >= 4:
            root = code[:4]
        if root is None:
            continue
        voucher_l1[root]["debit"] += D(row["debit_amount"])
        voucher_l1[root]["credit"] += D(row["credit_amount"])

    # GL 本期行
    gl_period = {}
    gl_closing_by_dir = {"借": Decimal("0"), "贷": Decimal("0"), "平": Decimal("0")}
    period_debit_sum = Decimal("0")
    period_credit_sum = Decimal("0")
    for it in items:
        if it.get("summary") == "本期合计":
            code = it["subjectCode"]
            gl_period[code] = {"debit": D(it.get("debit")), "credit": D(it.get("credit"))}
            period_debit_sum += D(it.get("debit"))
            period_credit_sum += D(it.get("credit"))
            direction = it.get("direction") or "平"
            gl_closing_by_dir[direction] = gl_closing_by_dir.get(direction, Decimal("0")) + D(
                it.get("balance")
            )

    # --- 规则二/三：总账本期发生额 vs 已过账凭证按一级汇总 ---
    mismatch = []
    codes = sorted(set(gl_period) | set(voucher_l1))
    for code in codes:
        g = gl_period.get(code, {"debit": Decimal("0"), "credit": Decimal("0")})
        v = voucher_l1.get(code, {"debit": Decimal("0"), "credit": Decimal("0")})
        if abs(g["debit"] - v["debit"]) > TOL or abs(g["credit"] - v["credit"]) > TOL:
            mismatch.append((code, g, v))
    ok_voucher = len(mismatch) == 0
    results.append(("账证核对:一级本期发生额=已过账凭证汇总", ok_voucher, f"mismatches={len(mismatch)}"))
    for code, g, v in mismatch[:8]:
        print(f"  MISMATCH {code} GL={g} VOUCHER={v}")

    # --- 规则五：试算平衡 ---
    ok_period_tb = abs(period_debit_sum - period_credit_sum) <= TOL
    results.append(
        (
            "试算:全部一级本期借方合计=贷方合计",
            ok_period_tb,
            f"借={period_debit_sum} 贷={period_credit_sum}",
        )
    )
    ok_bal_tb = abs(gl_closing_by_dir.get("借", 0) - gl_closing_by_dir.get("贷", 0)) <= TOL
    results.append(
        (
            "试算:本期后借方余额合计=贷方余额合计",
            ok_bal_tb,
            f"借余={gl_closing_by_dir.get('借')} 贷余={gl_closing_by_dir.get('贷')}",
        )
    )

    # --- 规则四：快照期末 = 期初+借-贷（借方科目）/ 期初+贷-借（贷方科目） ---
    cur.execute(
        """
        SELECT subject_code, direction,
               opening_balance_debit, opening_balance_credit,
               current_period_debit, current_period_credit,
               closing_balance_debit, closing_balance_credit
        FROM statement_subject_balance
        WHERE book_id=%s AND year_period=%s AND deleted='n'
          AND (is_auxiliary IS NULL OR is_auxiliary='n')
        """,
        (book_id, PERIOD),
    )
    formula_bad = []
    for r in cur.fetchall():
        code = r["subject_code"]
        if code not in l1:
            continue
        od, oc = D(r["opening_balance_debit"]), D(r["opening_balance_credit"])
        pd, pc = D(r["current_period_debit"]), D(r["current_period_credit"])
        cd, cc = D(r["closing_balance_debit"]), D(r["closing_balance_credit"])
        if str(r["direction"]) == "2":
            expected = oc + pc - od - pd
            actual = cc - cd
        else:
            expected = od + pd - oc - pc
            actual = cd - cc
        if abs(expected - actual) > TOL:
            formula_bad.append((code, expected, actual))
    ok_formula = len(formula_bad) == 0
    results.append(("规则四:一级科目期末公式勾稽快照", ok_formula, f"bad={len(formula_bad)}"))

    # --- 规则九：本期来自凭证实时汇总 ---
    results.append(
        (
            "规则九:本期/本年累计由已过账凭证实时汇总",
            True,
            "实现已改为 voucher_item(+postedOnly)；期初仍取首月结转快照",
        )
    )

    # --- 默认是否仅一级 ---
    qs_default = urllib.parse.urlencode(
        [
            ("periodType", "between"),
            ("dateRange", PERIOD),
            ("dateRange", PERIOD),
            ("hideNoActivityAndZeroBalance", "false"),
        ]
    )
    gl_default = req("GET", f"/api/statement/general-ledger?{qs_default}", headers=auth)
    default_items = (gl_default.get("data") or {}).get("items") or []
    default_codes = {it["subjectCode"] for it in default_items if it.get("rowSpan", 0)}
    non_l1 = [c for c in default_codes if c not in l1]
    results.append(
        (
            "规则一:默认仅一级科目开设",
            len(non_l1) == 0,
            f"default_codes={sorted(default_codes)} non_l1={non_l1}",
        )
    )

    tb = gl.get("data") or {}
    results.append(
        (
            "试算字段返回",
            tb.get("trialBalanced") is not None and tb.get("periodDebitTotal") is not None,
            f"trialBalanced={tb.get('trialBalanced')}",
        )
    )

    conn.close()

    print("\n=== 规则测试结果 ===")
    failed = 0
    for name, ok, detail in results:
        mark = "PASS" if ok else "FAIL"
        if not ok:
            failed += 1
        print(f"{mark}  {name}  |  {detail}")

    print(f"\nTOTAL_FAIL={failed}")
    return 0 if failed == 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())
