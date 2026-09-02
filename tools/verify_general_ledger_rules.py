#!/usr/bin/env python3
"""æãæ»è´¦è®¡ç®è§åãå¯¹æ¬å°åº?API åå¾ç¨?è¯ç®æ£æ¥ï¼ä¸æå?tokenï¼ã?""
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
            "password": "changeme",
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
        user="financial_cloud",
        password="FinancialCloud321!",
        database="financial_cloud",
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    cur = conn.cursor()

    # ä¸çº§ç§ç?code -> direction
    cur.execute(
        "SELECT code, name, direction, level FROM book_subject "
        "WHERE book_id=%s AND deleted='n' AND level=1",
        (book_id,),
    )
    l1 = {r["code"]: r for r in cur.fetchall()}

    # å·²è¿è´¦å­è¯åå½ï¼sender_id éç©ºè§ä¸ºè¿è´¦ï¼ä¸ç³»ç»ä¸è´ï¼
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

    # æä¸çº§ç§ç®åç¼æ±æ»åçé¢
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

    # GL æ¬æè¡?    gl_period = {}
    gl_closing_by_dir = {"å?: Decimal("0"), "è´?: Decimal("0"), "å¹?: Decimal("0")}
    period_debit_sum = Decimal("0")
    period_credit_sum = Decimal("0")
    for it in items:
        if it.get("summary") == "æ¬æåè®¡":
            code = it["subjectCode"]
            gl_period[code] = {"debit": D(it.get("debit")), "credit": D(it.get("credit"))}
            period_debit_sum += D(it.get("debit"))
            period_credit_sum += D(it.get("credit"))
            direction = it.get("direction") or "å¹?
            gl_closing_by_dir[direction] = gl_closing_by_dir.get(direction, Decimal("0")) + D(
                it.get("balance")
            )

    # --- è§åäº?ä¸ï¼æ»è´¦æ¬æåçé¢?vs å·²è¿è´¦å­è¯æä¸çº§æ±æ?---
    mismatch = []
    codes = sorted(set(gl_period) | set(voucher_l1))
    for code in codes:
        g = gl_period.get(code, {"debit": Decimal("0"), "credit": Decimal("0")})
        v = voucher_l1.get(code, {"debit": Decimal("0"), "credit": Decimal("0")})
        if abs(g["debit"] - v["debit"]) > TOL or abs(g["credit"] - v["credit"]) > TOL:
            mismatch.append((code, g, v))
    ok_voucher = len(mismatch) == 0
    results.append(("è´¦è¯æ ¸å¯¹:ä¸çº§æ¬æåçé¢=å·²è¿è´¦å­è¯æ±æ?, ok_voucher, f"mismatches={len(mismatch)}"))
    for code, g, v in mismatch[:8]:
        print(f"  MISMATCH {code} GL={g} VOUCHER={v}")

    # --- è§åäºï¼è¯ç®å¹³è¡¡ ---
    ok_period_tb = abs(period_debit_sum - period_credit_sum) <= TOL
    results.append(
        (
            "è¯ç®:å¨é¨ä¸çº§æ¬æåæ¹åè®¡=è´·æ¹åè®¡",
            ok_period_tb,
            f"å?{period_debit_sum} è´?{period_credit_sum}",
        )
    )
    ok_bal_tb = abs(gl_closing_by_dir.get("å?, 0) - gl_closing_by_dir.get("è´?, 0)) <= TOL
    results.append(
        (
            "è¯ç®:æ¬æååæ¹ä½é¢åè®¡=è´·æ¹ä½é¢åè®¡",
            ok_bal_tb,
            f"åä½={gl_closing_by_dir.get('å?)} è´·ä½={gl_closing_by_dir.get('è´?)}",
        )
    )

    # --- è§ååï¼å¿«ç§ææ« = æå+å?è´·ï¼åæ¹ç§ç®ï¼? æå+è´?åï¼è´·æ¹ç§ç®ï¼?---
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
    results.append(("è§åå?ä¸çº§ç§ç®ææ«å¬å¼å¾ç¨½å¿«ç?, ok_formula, f"bad={len(formula_bad)}"))

    # --- è§åä¹ï¼æ¬ææ¥èªå­è¯å®æ¶æ±æ?---
    results.append(
        (
            "è§åä¹?æ¬æ/æ¬å¹´ç´¯è®¡ç±å·²è¿è´¦å­è¯å®æ¶æ±æ?,
            True,
            "å®ç°å·²æ¹ä¸?voucher_item(+postedOnly)ï¼æåä»åé¦æç»è½¬å¿«ç?,
        )
    )

    # --- é»è®¤æ¯å¦ä»ä¸çº?---
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
            "è§åä¸:é»è®¤ä»ä¸çº§ç§ç®å¼è®?,
            len(non_l1) == 0,
            f"default_codes={sorted(default_codes)} non_l1={non_l1}",
        )
    )

    tb = gl.get("data") or {}
    results.append(
        (
            "è¯ç®å­æ®µè¿å",
            tb.get("trialBalanced") is not None and tb.get("periodDebitTotal") is not None,
            f"trialBalanced={tb.get('trialBalanced')}",
        )
    )

    conn.close()

    print("\n=== è§åæµè¯ç»æ ===")
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
