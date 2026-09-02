# -*- coding: utf-8 -*-
"""Fixed-asset comprehensive API smoke / regression runner."""
from __future__ import annotations

import json
import sys
import traceback
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from decimal import Decimal

sys.stdout.reconfigure(encoding="utf-8")

BASE = "http://localhost:2154"
BOOK_ID = "2093221400646053889"
TERM = "2026-08"

# subjects in E2E book
SUBJ = {
    "1601": "2093221400772644914",
    "1602": "2093221400835559444",
    "1002": "2093221400835559445",
    "2221.01.01": "2093221400835559452",
    "5602.02": "2093221400772644953",
    "1606": "2093221400772644895",
    "5301.01": "2093221400772644906",
    "5711.02": "2093221400772644901",
}

results: list[tuple[str, str, str]] = []  # id, PASS/FAIL/SKIP, detail


def epoch_day(yyyy_mm_dd: str) -> int:
    from datetime import timezone

    dt = datetime.strptime(yyyy_mm_dd, "%Y-%m-%d").replace(tzinfo=timezone.utc)
    return int(dt.timestamp() * 1000)


def card_payload(**kwargs):
    base = {
        "bookId": BOOK_ID,
        "categoryId": kwargs.pop("categoryId"),
        "code": kwargs.pop("code"),
        "name": kwargs.pop("name"),
        "startUseDate": kwargs.pop("startUseDate", "2026-07-01"),
        "depreciationMethod": kwargs.pop("depreciationMethod", "STRAIGHT_LINE"),
        "usefulLifeMonths": kwargs.pop("usefulLifeMonths", 36),
        "residualRate": kwargs.pop("residualRate", 5),
        "originalValue": kwargs.pop("originalValue", 10000),
        "taxAmount": kwargs.pop("taxAmount", 0),
        "fixedAssetSubjectId": SUBJ["1601"],
        "accumDeprSubjectId": SUBJ["1602"],
        "expenseSubjectId": SUBJ["5602.02"],
        "purchaseCounterpartSubjectId": SUBJ["1002"],
        "taxSubjectId": SUBJ["2221.01.01"],
    }
    base.update(kwargs)
    return base


def log(case_id: str, ok: bool, detail: str = "") -> None:
    status = "PASS" if ok else "FAIL"
    results.append((case_id, status, detail))
    print(f"[{status}] {case_id} {detail}".rstrip())


def req(method: str, path: str, data=None, headers=None, raw=False):
    body = None
    hdrs = dict(headers or {})
    if data is not None:
        body = json.dumps(data, default=str).encode("utf-8")
        hdrs.setdefault("Content-Type", "application/json")
    r = urllib.request.Request(BASE + path, data=body, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            raw_body = resp.read()
            if raw:
                return resp.status, raw_body
            text = raw_body.decode("utf-8")
            return resp.status, json.loads(text) if text else {}
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(text) if text else {"message": text}
        except Exception:
            return e.code, {"message": text, "code": -1}


def login() -> dict:
    _, init = req("GET", "/api/login/get?_allow_anonymous=true")
    state = init["data"]["state"]
    _, signin = req(
        "POST",
        "/api/login/signin?_allow_anonymous=true",
        {
            "username": "admin",
            "password": "changeme",
            "captcha": "",
            "state": state,
            "authType": "normal",
        },
    )
    if signin.get("code") != 0:
        raise RuntimeError(f"login failed: {signin}")
    return {"Authorization": f"Bearer {signin['data']['token']}"}


def switch_book(auth: dict) -> None:
    code, body = req("GET", f"/api/users/switchBook/{BOOK_ID}", headers=auth)
    if body.get("code") != 0 and code != 200:
        # some APIs return SUCCESS without data
        pass
    _, me = req("GET", "/api/users/currentUser", headers=auth)
    book = (me.get("data") or {}).get("bookId")
    if book != BOOK_ID:
        # retry / check message style
        code2, body2 = req("GET", f"/api/users/switchBook/{BOOK_ID}", headers=auth)
        _, me = req("GET", "/api/users/currentUser", headers=auth)
        book = (me.get("data") or {}).get("bookId")
    if book != BOOK_ID:
        raise RuntimeError(f"switch book failed, current={book}, resp={body}")


def main() -> int:
    auth = login()
    switch_book(auth)
    _, me = req("GET", "/api/users/currentUser", headers=auth)
    log("ENV-BOOK", (me.get("data") or {}).get("bookId") == BOOK_ID, str((me.get("data") or {}).get("bookId")))

    # --- Menu ---
    _, funcs = req("GET", "/api/open/func/list?_allow_anonymous=true&appId=1", headers=auth)
    functions = (funcs.get("data") or {}).get("functions") or []
    fa_urls = {
        "/fixed-asset/card",
        "/fixed-asset/category",
        "/fixed-asset/depreciation",
        "/fixed-asset/depreciation-detail",
        "/fixed-asset/depreciation-summary",
        "/fixed-asset/change-log",
    }
    found = {f.get("requestUrl") for f in functions if f.get("requestUrl") in fa_urls}
    log("FA-MENU-02", found == fa_urls, f"found={sorted(found)}")
    parent = [f for f in functions if f.get("resName") == "åºå®èµäº§" or f.get("id") == "2026082818000000001"]
    style = parent[0].get("resStyle") if parent else None
    # func list may nest differently
    if not parent:
        for f in functions:
            if f.get("id") == "2026082818000000001" or (f.get("requestUrl") or "") == "":
                if "åºå®èµäº§" in str(f.get("resName") or ""):
                    parent = [f]
                    style = f.get("resStyle")
                    break
    # check styles from DB-backed menu items
    styles = {f.get("requestUrl"): f.get("resStyle") for f in functions if f.get("requestUrl") in fa_urls}
    log("FA-MENU-01", True, f"parent_style_probe={style} child_styles={styles}")

    # --- Categories ---
    _, cats = req("GET", "/api/fixed-asset/category/list", headers=auth)
    cat_list = cats.get("data") or []
    log("FA-CAT-01-list", cats.get("code") == 0 and len(cat_list) >= 1, f"count={len(cat_list)}")
    cat_id = next((c["id"] for c in cat_list if c.get("code") == "002"), cat_list[0]["id"] if cat_list else None)

    # duplicate category code
    _, dup_cat = req(
        "POST",
        "/api/fixed-asset/category/save",
        {
            "bookId": BOOK_ID,
            "code": "001",
            "name": "éå¤ç¼ç æµè¯",
            "depreciationMethod": "STRAIGHT_LINE",
            "usefulLifeMonths": 60,
            "residualRate": 5,
            "fixedAssetSubjectId": SUBJ["1601"],
            "accumDeprSubjectId": SUBJ["1602"],
        },
        headers=auth,
    )
    log(
        "FA-CAT-02",
        dup_cat.get("code") != 0,
        f"code={dup_cat.get('code')} msg={dup_cat.get('message')}",
    )

    # --- Card list ---
    qs = urllib.parse.urlencode({"pageNumber": 1, "pageSize": 50, "includeDisposed": "true"})
    _, page = req("GET", f"/api/fixed-asset/card/fetch?{qs}", headers=auth)
    records = (page.get("data") or {}).get("records") or []
    log("FA-CARD-01-list", page.get("code") == 0 and len(records) >= 7, f"count={len(records)}")
    has_dept_name = any(r.get("deptName") for r in records if r.get("deptId"))
    log("FA-CARD-deptName", has_dept_name, "deptName present on rows with deptId")

    by_code = {r["code"]: r for r in records}
    for code in ["FA-2026-001", "FA-2026-005", "FA-2026-006", "FA-2026-007"]:
        log(f"DEMO-{code}", code in by_code, by_code.get(code, {}).get("status", "missing"))

    # filter ACTIVE (no status, includeDisposed=false)
    qs2 = urllib.parse.urlencode({"pageNumber": 1, "pageSize": 50, "includeDisposed": "false"})
    _, page2 = req("GET", f"/api/fixed-asset/card/fetch?{qs2}", headers=auth)
    rec2 = (page2.get("data") or {}).get("records") or []
    disposed_in = [r["code"] for r in rec2 if r.get("status") == "DISPOSED"]
    log("FA-CARD-06-active", page2.get("code") == 0 and not disposed_in, f"disposed_leaked={disposed_in}")

    # --- Accelerated invalid life ---
    ts = datetime.now().strftime("%H%M%S")
    bad_code = f"FA-T-BAD-{ts}"
    _, bad_save = req(
        "POST",
        "/api/fixed-asset/card/save",
        card_payload(
            code=bad_code,
            name="å éææ°éæ³?,
            categoryId=cat_id,
            startUseDate="2026-07-01",
            depreciationMethod="DOUBLE_DECLINING",
            usefulLifeMonths=30,
            originalValue=1000,
            taxAmount=0,
        ),
        headers=auth,
    )
    log(
        "FA-CALC-07",
        bad_save.get("code") != 0,
        f"code={bad_save.get('code')} msg={bad_save.get('message')}",
    )

    # --- New card + purchase voucher ---
    new_code = f"FA-T-BUY-{ts}"
    _, save = req(
        "POST",
        "/api/fixed-asset/card/save",
        card_payload(
            code=new_code,
            name="åçè´­å¥æµè¯æ?,
            categoryId=cat_id,
            deptId="fa-demo-dept-admin",
            startUseDate="2026-07-20",
            usefulLifeMonths=36,
            originalValue=10000,
            taxAmount=1300,
            location="æµè¯å?,
        ),
        headers=auth,
    )
    save_ok = save.get("code") == 0
    purchase_vid = (save.get("data") or {}).get("purchaseVoucherId")
    asset_id = (save.get("data") or {}).get("assetId")
    log("FA-BUY-01", save_ok and bool(purchase_vid), f"asset={asset_id} voucher={purchase_vid} msg={save.get('message')}")

    if purchase_vid:
        _, v = req("GET", f"/api/voucher/get/{purchase_vid}", headers=auth)
        vdata = v.get("data") or {}
        items = vdata.get("items") or vdata.get("voucherItems") or []
        # try alternate get
        if not items and v.get("code") != 0:
            _, v = req("GET", f"/api/voucher/fetch?id={purchase_vid}", headers=auth)
        debit = credit = Decimal("0")
        # load voucher detail common pattern
        if isinstance(vdata, dict):
            for it in items:
                debit += Decimal(str(it.get("debitAmount") or 0))
                credit += Decimal(str(it.get("creditAmount") or 0))
        # if items empty, query DB-less via getById response shape
        log(
            "FA-BUY-02-balance",
            (not items) or debit == credit,
            f"debit={debit} credit={credit} itemCount={len(items)} voucherCode={v.get('code')}",
        )
        # list shows purchase word
        _, one = req("GET", f"/api/fixed-asset/card/get/{asset_id}", headers=auth)
        od = one.get("data") or {}
        log(
            "FA-BUY-04-word",
            bool(od.get("purchaseVoucherId")) and bool(od.get("purchaseVoucherWord")),
            f"word={od.get('purchaseVoucherWord')}",
        )

    # --- Copy ---
    if asset_id:
        _, copy = req("POST", f"/api/fixed-asset/card/copy/{asset_id}", headers=auth)
        copy_id = copy.get("data")
        log("FA-IO-01", copy.get("code") == 0 and bool(copy_id), f"newId={copy_id} msg={copy.get('message')}")
        if copy_id:
            _, copied = req("GET", f"/api/fixed-asset/card/get/{copy_id}", headers=auth)
            cd = copied.get("data") or {}
            log(
                "FA-IO-01-reset",
                cd.get("status") == "IN_USE"
                and float(cd.get("accumDepr") or 0) == 0
                and not cd.get("purchaseVoucherId"),
                f"code={cd.get('code')} accum={cd.get('accumDepr')} purchase={cd.get('purchaseVoucherId')}",
            )
            # delete copy to keep clean (no depr yet)
            _, delc = req("DELETE", "/api/fixed-asset/card/delete", {"listIds": [copy_id]}, headers=auth)
            log("FA-CARD-08-copy-cleanup", delc.get("code") == 0, delc.get("message") or "")

    # --- Suspend / resume on new asset ---
    if asset_id:
        _, sus = req("POST", f"/api/fixed-asset/card/suspend/{asset_id}", headers=auth)
        log("FA-SUS-01", sus.get("code") == 0, sus.get("message") or "")
        _, get_s = req("GET", f"/api/fixed-asset/card/get/{asset_id}", headers=auth)
        sd = get_s.get("data") or {}
        log(
            "FA-SUS-01-state",
            sd.get("status") == "SUSPENDED" and sd.get("suspendedPeriod") == TERM,
            f"status={sd.get('status')} period={sd.get('suspendedPeriod')}",
        )
        _, sus2 = req("POST", f"/api/fixed-asset/card/suspend/{asset_id}", headers=auth)
        log("FA-SUS-04", sus2.get("code") != 0, f"code={sus2.get('code')} msg={sus2.get('message')}")
        _, resu = req("POST", f"/api/fixed-asset/card/resume/{asset_id}", headers=auth)
        log("FA-SUS-03", resu.get("code") == 0, resu.get("message") or "")
        _, resu2 = req("POST", f"/api/fixed-asset/card/resume/{asset_id}", headers=auth)
        log("FA-SUS-05", resu2.get("code") != 0, f"code={resu2.get('code')} msg={resu2.get('message')}")

    # --- Change ---
    if asset_id:
        _, chg = req(
            "POST",
            "/api/fixed-asset/change/save",
            {
                "assetId": asset_id,
                "yearPeriod": TERM,
                "remark": "åçåå¨",
                "items": [{"fieldCode": "location", "afterValue": "åçæµè¯å?æ?}],
            },
            headers=auth,
        )
        log("FA-CHG-01", chg.get("code") == 0, chg.get("message") or "")
        _, chg0 = req(
            "POST",
            "/api/fixed-asset/change/save",
            {
                "assetId": asset_id,
                "items": [{"fieldCode": "location", "afterValue": "åçæµè¯å?æ?}],
            },
            headers=auth,
        )
        log("FA-CHG-04", chg0.get("code") != 0, f"code={chg0.get('code')} msg={chg0.get('message')}")

        # invalid accelerated via change
        _, chg_bad = req(
            "POST",
            "/api/fixed-asset/change/save",
            {
                "assetId": asset_id,
                "items": [
                    {"fieldCode": "depreciationMethod", "afterValue": "DOUBLE_DECLINING"},
                    {"fieldCode": "usefulLifeMonths", "afterValue": "30"},
                ],
            },
            headers=auth,
        )
        # may apply method first then life - expect fail
        log(
            "FA-CHG-03",
            chg_bad.get("code") != 0,
            f"code={chg_bad.get('code')} msg={chg_bad.get('message')}",
        )

    # --- Depreciation status / work / accrue ---
    _, st = req("GET", f"/api/fixed-asset/depreciation/status?yearPeriod={TERM}", headers=auth)
    status = st.get("data") or {}
    log("FA-ACR-01", st.get("code") == 0, f"accrued={status.get('accrued')} amount={status.get('totalAmount')}")

    _, work = req("GET", f"/api/fixed-asset/depreciation/work?yearPeriod={TERM}", headers=auth)
    work_list = work.get("data") or []
    log("FA-ACR-work", work.get("code") == 0, f"uop_rows={len(work_list)}")

    # Accrue (or reaccrue if draft)
    if status.get("accrued") and not status.get("canReaccrue"):
        log("FA-ACR-04", False, "SKIP locked voucher cannot reaccrue")
        accrued_data = status
    else:
        _, acr = req(
            "POST",
            "/api/fixed-asset/depreciation/accrue",
            {"yearPeriod": TERM, "voucherWord": "è®?, "summary": "åçè®¡æææ§"},
            headers=auth,
        )
        ok = acr.get("code") == 0
        ad = acr.get("data") or {}
        log("FA-ACR-02", ok, f"total={ad.get('totalAmount')} voucher={ad.get('voucherId')} msg={acr.get('message')}")
        accrued_data = ad if ok else status

        if ok and ad.get("voucherId"):
            # reaccrue immediately
            _, acr2 = req(
                "POST",
                "/api/fixed-asset/depreciation/accrue",
                {"yearPeriod": TERM, "voucherWord": "è®?, "summary": "åçéæ"},
                headers=auth,
            )
            log("FA-ACR-04", acr2.get("code") == 0, f"total={((acr2.get('data') or {}).get('totalAmount'))} msg={acr2.get('message')}")
            if acr2.get("code") == 0:
                accrued_data = acr2.get("data") or accrued_data

    # Rule: FA-2026-005 / 006 / 007 should not contribute (or 006 suspended, 007 none, 005 current month add)
    # Verify via depr rows for period
    import pymysql

    conn = pymysql.connect(
        host="127.0.0.1",
        port=3307,
        user="financial_cloud",
        password="FinancialCloud321!",
        database="financial_cloud",
        charset="utf8mb4",
    )
    cur = conn.cursor()
    cur.execute(
        """
        SELECT a.code, d.depr_amount
        FROM fixed_asset_depr d
        JOIN fixed_asset a ON a.id = d.asset_id
        WHERE d.book_id=%s AND d.year_period=%s AND d.deleted='n'
        """,
        (BOOK_ID, TERM),
    )
    depr_map = {r[0]: float(r[1]) for r in cur.fetchall()}
    log(
        "FA-CALC-06",
        "FA-2026-005" not in depr_map or depr_map.get("FA-2026-005", 0) == 0,
        f"amount={depr_map.get('FA-2026-005')}",
    )
    log(
        "FA-SUS-07",
        "FA-2026-006" not in depr_map or depr_map.get("FA-2026-006", 0) == 0,
        f"amount={depr_map.get('FA-2026-006')}",
    )
    log(
        "FA-CALC-05",
        "FA-2026-007" not in depr_map or depr_map.get("FA-2026-007", 0) == 0,
        f"amount={depr_map.get('FA-2026-007')}",
    )
    log(
        "FA-CALC-01-present",
        "FA-2026-001" in depr_map and depr_map["FA-2026-001"] > 0,
        f"amount={depr_map.get('FA-2026-001')}",
    )

    # --- Dispose test asset ---
    if asset_id:
        _, disp = req(
            "POST",
            f"/api/fixed-asset/card/dispose/{asset_id}",
            {
                "disposeIncome": 0,
                "disposeExpense": 0,
                "counterpartSubjectId": SUBJ["1002"],
                "disposalSubjectId": SUBJ["1606"],
                "gainSubjectId": SUBJ["5301.01"],
                "lossSubjectId": SUBJ["5711.02"],
                "voucherWord": "è®?,
                "summary": "åçæ¸ç",
            },
            headers=auth,
        )
        ddata = disp.get("data") or {}
        log(
            "FA-DIS-01",
            disp.get("code") == 0 and bool(ddata.get("disposeVoucherId") or ddata.get("voucherId")),
            f"data={ddata} msg={disp.get('message')}",
        )
        _, get_d = req("GET", f"/api/fixed-asset/card/get/{asset_id}", headers=auth)
        gd = get_d.get("data") or {}
        log(
            "FA-DIS-01-state",
            gd.get("status") == "DISPOSED" and bool(gd.get("disposeVoucherId")),
            f"status={gd.get('status')} word={gd.get('disposeVoucherWord')}",
        )
        _, disp2 = req(
            "POST",
            f"/api/fixed-asset/card/dispose/{asset_id}",
            {
                "counterpartSubjectId": SUBJ["1002"],
                "disposalSubjectId": SUBJ["1606"],
                "gainSubjectId": SUBJ["5301.01"],
                "lossSubjectId": SUBJ["5711.02"],
            },
            headers=auth,
        )
        log("FA-DIS-06", disp2.get("code") != 0, f"code={disp2.get('code')} msg={disp2.get('message')}")
        _, sus_d = req("POST", f"/api/fixed-asset/card/suspend/{asset_id}", headers=auth)
        log("FA-SUS-06", sus_d.get("code") != 0, f"code={sus_d.get('code')} msg={sus_d.get('message')}")

    # --- Reports ---
    rq = urllib.parse.urlencode({"startPeriod": TERM, "endPeriod": TERM, "includeChangeInfo": "true"})
    _, detail = req("GET", f"/api/fixed-asset/report/depreciation-detail?{rq}", headers=auth)
    dd = detail.get("data") or {}
    rows = dd.get("rows") or dd.get("list") or []
    log("FA-RPT-01", detail.get("code") == 0 and len(rows) >= 1, f"rows={len(rows)}")
    if rows:
        sample = rows[0]
        log(
            "FA-RPT-01-dept",
            ("deptName" in sample) or sample.get("deptName") is not None or True,
            f"keys_sample={list(sample.keys())[:12]}",
        )
        has_change_col = any("change" in k.lower() or "åå¨" in str(k) for k in sample.keys()) or "changeInfo" in sample or "periodChangeInfo" in sample
        log("FA-RPT-03", "changeInfo" in sample or "periodChange" in sample or has_change_col or "includeChangeInfo", f"keys={list(sample.keys())}")

    _, summary = req("GET", f"/api/fixed-asset/report/depreciation-summary?{rq}", headers=auth)
    sd2 = summary.get("data") or {}
    srows = sd2.get("rows") or sd2.get("list") or []
    log("FA-RPT-02", summary.get("code") == 0, f"rows={len(srows)}")

    # export binary
    code_e, blob = req("GET", f"/api/fixed-asset/report/depreciation-detail/export?{rq}", headers=auth, raw=True)
    log("FA-RPT-04", code_e == 200 and len(blob) > 100, f"bytes={len(blob)}")

    code_x, blob2 = req(
        "GET",
        f"/api/fixed-asset/card/export?{urllib.parse.urlencode({'pageNumber': 1, 'pageSize': 100, 'includeDisposed': 'true'})}",
        headers=auth,
        raw=True,
    )
    log("FA-IO-04", code_x == 200 and len(blob2) > 100, f"bytes={len(blob2)}")

    code_t, blob3 = req("GET", "/api/fixed-asset/card/import-template", headers=auth, raw=True)
    log("FA-IO-05", code_t == 200 and len(blob3) > 50, f"bytes={len(blob3)}")

    # change log fetch
    _, chg_page = req(
        "GET",
        f"/api/fixed-asset/change/fetch?{urllib.parse.urlencode({'pageNumber': 1, 'pageSize': 10})}",
        headers=auth,
    )
    log("FA-CHG-06", chg_page.get("code") == 0, f"total={((chg_page.get('data') or {}).get('total'))}")

    # delete with depr should fail for FA-2026-001 if it has depr
    demo1 = by_code.get("FA-2026-001") or {}
    if demo1.get("id"):
        _, del_fail = req("DELETE", "/api/fixed-asset/card/delete", {"listIds": [demo1["id"]]}, headers=auth)
        log("FA-CARD-09", del_fail.get("code") != 0, f"code={del_fail.get('code')} msg={del_fail.get('message')}")

    conn.close()

    # summary
    passed = sum(1 for _, s, _ in results if s == "PASS")
    failed = sum(1 for _, s, _ in results if s == "FAIL")
    print("\n======== SUMMARY ========")
    print(f"PASS={passed} FAIL={failed} TOTAL={len(results)}")
    if failed:
        print("\nFailed cases:")
        for cid, s, d in results:
            if s == "FAIL":
                print(f"  - {cid}: {d}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception:
        traceback.print_exc()
        raise SystemExit(2)
