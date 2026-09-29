#!/usr/bin/env python3
"""jinbooks 月度全流程验收：建账套 → 期初 → 录凭证 → 费用报销 → 银行对账 → 期末结账 → 报表与申报表。

用法:
    FC_API=http://localhost:2154 python tools/acceptance_month_flow.py

依赖: requests (pip install requests)。环境: 后端 2154 已启动且数据库已初始化。
每个步骤打印 PASS/FAIL，失败即终止并输出响应报文，便于定位。
"""
from __future__ import annotations

import os
import sys
import json
import datetime

import requests

BASE = os.environ.get("FC_API", "http://localhost:2154")
USERNAME = os.environ.get("FC_USER", "admin")
PASSWORD = os.environ.get("FC_PASS", "changeme")

HEADERS: dict[str, str] = {}
STEP_NO = 0
FAILURES: list[str] = []


def step(title: str) -> None:
    global STEP_NO
    STEP_NO += 1
    print(f"\n[{STEP_NO:02d}] {title}")


def ok(msg: str) -> None:
    print(f"    PASS {msg}")


def fail(msg: str) -> None:
    FAILURES.append(msg)
    print(f"    FAIL {msg}")
    raise SystemExit(1)


def req(method: str, path: str, expect_ok: bool = True, headers: dict | None = None, **kwargs):
    url = f"{BASE}{path}"
    resp = requests.request(method, url, headers=headers or HEADERS, timeout=30, **kwargs)
    try:
        body = resp.json()
    except Exception:
        fail(f"{method} {path} HTTP {resp.status_code} 非JSON: {resp.text[:300]}")
    if expect_ok and body.get("code") != 0:
        fail(f"{method} {path} code={body.get('code')} message={body.get('message')!r} body={json.dumps(body, ensure_ascii=False)[:400]}")
    return body


def login() -> None:
    step("登录 admin")
    HEADERS.update(login_as(USERNAME, PASSWORD))
    ok(f"token={HEADERS['Authorization']}")


def login_as(username: str, password: str) -> dict[str, str]:
    init = req("GET", "/api/login/get?_allow_anonymous=true")
    state = init["data"]["state"]
    body = req("POST", "/api/login/signin?_allow_anonymous=true", json={
        "username": username, "password": password, "captcha": "",
        "state": state, "authType": "normal",
    })
    return {"Authorization": f"Bearer {body['data']['token']}"}


REVIEWER_HEADERS: dict[str, str] = {}


def create_reviewer(book_id: str) -> None:
    """制单人与审核人不得同一人：建审核员(ROLE_REVIEWER)负责审核。"""
    step("创建审核员用户 ROLE_REVIEWER (职责分离)")
    global REVIEWER_HEADERS
    existing = req("GET", "/api/users/getByUsername/reviewer01", expect_ok=False)
    if not (existing.get("code") == 0 and existing.get("data")):
        req("POST", "/api/users/add", json={
            "username": "reviewer01", "password": "Review@2026",
            "displayName": "审核员小王", "userType": "EMPLOYEE", "userState": "RESIDENT",
            "status": 1, "sortIndex": 1,
        })
    user = req("GET", "/api/users/getByUsername/reviewer01")["data"]
    req("POST", "/api/idm/groupmembers/add", json={
        "roleId": "ROLE_REVIEWER", "memberIds": [user["id"]], "type": "USER",
    })
    REVIEWER_HEADERS = login_as("reviewer01", "Review@2026")
    req("GET", f"/api/users/switchBook/{book_id}", headers=REVIEWER_HEADERS)
    ok(f"reviewer01 已加入 ROLE_REVIEWER 并切入当前账套 userId={user['id']}")


def current_term() -> str:
    body = req("GET", "/api/config/sys/books")
    for item in body.get("data") or []:
        if item.get("configKey") == "sys.payment.term.current":
            return item["configValue"]
    fail("未找到 sys.payment.term.current")


def setup_book() -> str:
    step("创建测试账套并切换")
    standards = req("GET", "/api/standard/fetchAll?status=1").get("data") or []
    standard = next((s for s in standards if str(s.get("id")) == "1" or "小企业" in (s.get("name") or "")),
                    standards[0] if standards else None)
    if not standard:
        fail("缺少可用会计准则")
    enable_date = datetime.date.today().strftime("%Y-%m")
    body = req("POST", "/api/book/setup", json={
        "name": "月结验收账套", "companyName": "验收测试科技有限公司",
        "standardId": str(standard["id"]), "enableDate": enable_date,
        "vatType": 1, "voucherReviewed": 1, "status": 1,
    })
    book_id = body["data"]["bookId"]
    req("GET", f"/api/users/switchBook/{book_id}")
    ok(f"bookId={book_id} 准则={standard.get('name')} 启用期间={enable_date}")
    return book_id


def fetch_subjects(book_id: str) -> list[dict]:
    body = req("GET", f"/api/booksubject/fetch?bookId={book_id}&pageNum=1&pageSize=500&status=1")
    records = [r for r in (body["data"].get("records") or []) if r.get("status") == 1]
    return [{"id": str(r["id"]), "code": r.get("code"), "name": r.get("displayName") or r.get("name")} for r in records]


def subject_by_code(subjects: list[dict], code: str) -> dict:
    for s in subjects:
        if s["code"] == code:
            return s
    fail(f"科目 {code} 不存在")


def save_opening_balances(book_id: str) -> None:
    step("录入期初余额 (现金1万/银行9万/实收资本10万)")
    rows = req("GET", "/api/base/init-balance/list").get("data") or []
    def row_of(code: str, name_like: str | None = None):
        for r in rows:
            if r.get("code") == code and (name_like is None or name_like in (r.get("name") or "")):
                return r
        fail(f"期初列表缺少 {code} {name_like or ''}")
    payload = []
    for row, debit, credit in [
        (row_of("1001"), 10_000, 0),
        (row_of("1002"), 90_000, 0),
        (row_of("3001", "实收资本"), 0, 100_000),
    ]:
        if row.get("hasVoucher"):
            fail(f"{row['code']} 已有凭证，无法录入期初")
        payload.append({
            **row, "bookId": book_id,
            "openingYearBalanceDebit": debit, "openingYearBalanceCredit": credit,
            "debitAmount": row.get("debitAmount") or 0, "creditAmount": row.get("creditAmount") or 0,
            "balance": debit - credit,
        })
    req("POST", "/api/base/init-balance/save", json=payload)
    ok("期初借贷平衡 100,000 = 100,000")


def next_word_num(term: str, head: str = "记") -> int:
    body = req("GET", f"/api/voucher/able-word-num?head={head}&year={term[:4]}&month={int(term[5:7])}")
    return int(body.get("data") or 1)


def make_voucher(book_id: str, term: str, items: list[tuple[dict, float, float]], summary: str,
                 subjects: list[dict]) -> dict:
    """items: (subject, debit, credit)"""
    body = req("GET", f"/api/book/get/{book_id}")
    book = body["data"]
    return {
        "bookId": book_id, "wordHead": "记", "wordNum": next_word_num(term),
        "companyName": book.get("companyName") or "验收测试科技有限公司",
        "receiptNum": 0, "voucherDate": f"{term}-15",
        "voucherYear": int(term[:4]), "voucherMonth": int(term[5:7]),
        "items": [{
            "subjectId": s["id"], "subjectName": s["name"], "summary": summary,
            "debitAmount": debit, "creditAmount": credit,
        } for s, debit, credit in items],
    }


def voucher_full_flow(payload: dict, label: str) -> str:
    body = req("POST", "/api/voucher/draft", json=payload)
    vid = body["data"]
    req("POST", "/api/voucher/submit", json={**payload, "id": vid})
    detail = req("GET", f"/api/voucher/get/{vid}")["data"]
    if detail.get("status") == "reviewing":
        req("PUT", f"/api/voucher/audit/{vid}", headers=REVIEWER_HEADERS)
        detail = req("GET", f"/api/voucher/get/{vid}")["data"]
        if detail.get("status") != "completed" or not detail.get("auditMemberId"):
            fail(f"{label} 复核岗审核后状态异常: status={detail.get('status')} auditMemberId={detail.get('auditMemberId')}")
    req("PUT", f"/api/voucher/sender/{vid}")
    detail = req("GET", f"/api/voucher/get/{vid}")["data"]
    if not detail.get("senderId") and not detail.get("senderName"):
        fail(f"{label} 过账后无过账人标记 status={detail.get('status')}")
    ok(f"{label} 记-{payload['wordNum']} 已 暂存→提交→复核岗审核→过账")
    return vid


def record_vouchers(book_id: str, term: str, subjects: list[dict]) -> None:
    step("录入业务凭证 (3笔: 销售收款/支付房租/现金购办公用品)")
    bank = subject_by_code(subjects, "1002")
    cash = subject_by_code(subjects, "1001")
    revenue = subject_by_code(subjects, "5001")
    admin_exp = subject_by_code(subjects, "5602")
    sales_exp = subject_by_code(subjects, "5601")
    voucher_full_flow(make_voucher(book_id, term, [(bank, 50_000, 0), (revenue, 0, 50_000)],
                                   "销售商品收到银行转账", subjects), "V1 销售收款5万")
    voucher_full_flow(make_voucher(book_id, term, [(admin_exp, 8_000, 0), (bank, 0, 8_000)],
                                   "支付本月办公房租", subjects), "V2 支付房租8千")
    voucher_full_flow(make_voucher(book_id, term, [(sales_exp, 3_000, 0), (cash, 0, 3_000)],
                                   "现金购买销售用物料", subjects), "V3 现金购料3千")


def expense_claim_flow(book_id: str, term: str, subjects: list[dict]) -> None:
    step("费用报销: 填单→提交→审批→生成凭证→过账")
    body = req("POST", "/api/expense/claim", json={
        "claimant": "张三", "claimDate": f"{term}-16",
        "fundSubjectCode": "1002", "summary": "出差上海差旅费报销",
        "items": [{"expenseSubjectCode": "5602", "amount": 1_500, "summary": "高铁+住宿"}],
    })
    claim_id = body["data"]
    req("PUT", f"/api/expense/claim/submit/{claim_id}")
    req("PUT", f"/api/expense/claim/audit/{claim_id}?approve=true")
    body = req("POST", f"/api/expense/claim/voucher/{claim_id}")
    vid = body["data"]
    detail = req("GET", f"/api/voucher/get/{vid}")["data"]
    payload = {
        "bookId": detail["bookId"], "word": detail.get("word"), "wordHead": detail["wordHead"],
        "wordNum": detail["wordNum"], "companyName": detail["companyName"],
        "receiptNum": detail.get("receiptNum") or 0, "voucherDate": detail["voucherDate"],
        "voucherYear": detail["voucherYear"], "voucherMonth": detail["voucherMonth"],
        "items": [{"subjectId": i["subjectId"], "subjectName": i["subjectName"],
                   "summary": i.get("summary"), "debitAmount": float(i.get("debitAmount") or 0),
                   "creditAmount": float(i.get("creditAmount") or 0)} for i in detail.get("items") or []],
    }
    total_debit = sum(i["debitAmount"] for i in payload["items"])
    if abs(total_debit - 1_500) > 0.005:
        fail(f"报销凭证借方合计 {total_debit} ≠ 1,500")
    req("POST", "/api/voucher/submit", json={**payload, "id": vid})
    detail = req("GET", f"/api/voucher/get/{vid}")["data"]
    if detail.get("status") == "reviewing":
        req("PUT", f"/api/voucher/audit/{vid}", headers=REVIEWER_HEADERS)
    req("PUT", f"/api/voucher/sender/{vid}")
    ok(f"claimId={claim_id} 凭证记-{payload['wordNum']} 金额1,500 已过账")


def journal_flow(term: str, subjects: list[dict]) -> str:
    step("出纳: 建银行账户→录流水→银行对账")
    bank = subject_by_code(subjects, "1002")
    body = req("POST", "/api/journal/account/add", json={
        "category": "deposit", "accCode": "BANK01", "accName": "基本户-招商银行",
        "subjectId": bank["id"], "currency": "CNY", "bankNo": "6225880212345678",
        "bank": "招商银行", "sortIndex": 1, "status": 1,
        "description": "验收测试账户",
    })
    acc_id = body["data"]
    if not acc_id:
        fail("账户新增未返回 ID")
    ok(f"银行账户 accId={acc_id}")

    entries = [
        ("o", 90_000, 0, "期初余额"),
        ("I", 50_000, 0, "销售回款"),
        ("E", 0, 8_000, "支付房租"),
        ("E", 0, 1_500, "报销差旅费"),
    ]
    entry_ids = []
    for direction, income, expenditure, remark in entries:
        body = req("POST", "/api/journal/entry/add", json={
            "category": "deposit", "accId": acc_id, "accCode": "BANK01",
            "accName": "基本户-招商银行", "subjectId": bank["id"],
            "direction": direction, "income": income, "expenditure": expenditure,
            "remark": remark, "tradeDate": f"{term}-01 09:00:00" if direction == "o" else f"{term}-15 10:00:00",
        })
        entry_ids.append(body["data"])
    biz_entry_ids = entry_ids[1:]
    ok(f"录入 {len(entry_ids)} 笔流水(含期初)")

    body = req("GET", f"/api/journal/reconciliation?accId={acc_id}&yearPeriod={term}")
    recon = body["data"]
    book_balance = float(recon.get("bookBalance") or 0)
    expected_balance = 90_000 + 50_000 - 8_000 - 1_500
    if abs(book_balance - expected_balance) > 0.005:
        fail(f"对账账面余额 {book_balance} ≠ 预期 {expected_balance}")
    req("PUT", "/api/journal/reconciliation/statement", json={
        "accId": acc_id, "yearPeriod": term,
        "statementBalance": expected_balance, "remark": "银行对账单(验收)",
    })
    body = req("PUT", "/api/journal/reconciliation/mark", json={
        "entryIds": biz_entry_ids, "reconciled": True,
    })
    body = req("GET", f"/api/journal/reconciliation?accId={acc_id}&yearPeriod={term}")
    recon = body["data"]
    diff = float(recon.get("difference") or 0)
    if abs(diff) > 0.005:
        fail(f"对账后差异 {diff} ≠ 0")
    ok(f"对账完成: 账面=对账单={expected_balance:,.2f} 差异=0")
    return acc_id


def carry_and_close(book_id: str, term: str) -> None:
    step("期末: 结转损益→结账检查→结账")
    templates = (req("GET", "/api/settlementcarry/fetchcarry?pageNumber=1&pageSize=50")
                 ["data"].get("records") or [])
    for code in ("qm_jz_sr", "qm_jz_cbfy"):
        tpl = next((t for t in templates if t.get("code") == code), None)
        if not tpl:
            fail(f"缺少结转模板 {code}")
        body = req("POST", "/api/settlementcarry/generate-voucher",
                   json={"id": tpl["id"], "templateId": tpl["id"], "voucherType": 1})
        vid = body["data"]
        detail = req("GET", f"/api/voucher/get/{vid}")["data"]
        payload = {
            "bookId": detail["bookId"], "word": detail.get("word"), "wordHead": detail["wordHead"],
            "wordNum": detail["wordNum"], "companyName": detail["companyName"],
            "receiptNum": detail.get("receiptNum") or 0, "voucherDate": detail["voucherDate"],
            "voucherYear": detail["voucherYear"], "voucherMonth": detail["voucherMonth"],
            "items": [{"subjectId": i["subjectId"], "subjectName": i["subjectName"],
                       "summary": i.get("summary"), "debitAmount": float(i.get("debitAmount") or 0),
                       "creditAmount": float(i.get("creditAmount") or 0)} for i in detail.get("items") or []],
        }
        req("POST", "/api/voucher/submit", json={**payload, "id": vid})
        detail = req("GET", f"/api/voucher/get/{vid}")["data"]
        if detail.get("status") == "reviewing":
            req("PUT", f"/api/voucher/audit/{vid}", headers=REVIEWER_HEADERS)
        req("PUT", f"/api/voucher/sender/{vid}")
        ok(f"结转 {code}({tpl.get('name')}) 记-{payload['wordNum']} 已过账")

    gaps = req("GET", "/api/voucher/successive").get("data") or []
    if gaps:
        req("PUT", "/api/voucher/successive", json=gaps)
        ok(f"整理凭证号断号 {len(gaps)} 处")

    checks = req("GET", "/api/settlement/verify").get("data") or []
    hard_failed = [c for c in checks
                   if c.get("hard") is not False and c.get("applicable") is not False
                   and not c.get("result")]
    if hard_failed:
        fail("结账硬检未通过: " + "; ".join(f"{c.get('item')}:{c.get('reason')}" for c in hard_failed))
    ok(f"结账检查 {len(checks)} 项全部通过")

    req("GET", f"/api/settlement/checkout?year={term[:4]}")
    new_term = current_term()
    if new_term == term:
        fail(f"结账后账期未推进: 仍为 {term}")
    ok(f"已结账 {term} → 当前账期 {new_term}")


def reports_flow(term: str) -> None:
    step("报表与申报表 (结账后期间)")
    bs = req("GET", f"/api/statement/balance-sheet?periodType=month&reportDate={term}")["data"]
    items = (bs.get("items") or {})
    assets_rows = items.get("assets") or []
    liability_rows = items.get("liability") or []
    all_rows = assets_rows + liability_rows

    def row_val(rows, keyword):
        for r in rows:
            if keyword in (r.get("itemName") or r.get("name") or ""):
                v = r.get("currentBalance") if r.get("currentBalance") is not None else r.get("closingBalance")
                return float(v or 0)
        return None

    monetary = row_val(assets_rows, "货币资金")
    asset_total = row_val(assets_rows, "总计")
    le_total = row_val(liability_rows, "总计")
    if monetary is None or abs(monetary - 137_500) > 0.005:
        fail(f"货币资金 {monetary} ≠ 137,500")
    if asset_total is None or le_total is None or abs(asset_total - le_total) > 0.005:
        fail(f"资产负债表不平衡: 资产总计={asset_total} 负债权益总计={le_total}")
    ok(f"资产负债表平衡: 货币资金={monetary:,.2f} 资产总计={asset_total:,.2f}")

    inc = req("GET", f"/api/statement/income?periodType=month&reportDate={term}")["data"]
    inc_rows = inc.get("items") or inc.get("rows") or []
    rev = row_val(inc_rows, "营业收入")
    profit = row_val(inc_rows, "净利润")
    if rev is None or abs(rev - 50_000) > 0.005:
        fail(f"利润表营业收入 {rev} ≠ 50,000")
    if profit is None or abs(profit - 37_500) > 0.005:
        fail(f"利润表净利润 {profit} ≠ 37,500")
    ok(f"利润表: 营业收入={rev:,.2f} 净利润={profit:,.2f}")

    cf = req("GET", f"/api/statement/cash-flow?periodType=month&reportDate={term}")["data"]
    ok(f"现金流量表返回 {len(cf) if isinstance(cf, list) else 'object'} 行")

    sb = req("GET", f"/api/statement/subject-balance?periodType=month&reportDate={term}&showAll=true")["data"]
    ok(f"科目余额表 {len(sb)} 行")

    est = req("GET", f"/api/tax-estimate?yearMonth={term}")["data"]
    est_revenue = float(est.get("revenue") or 0)
    est_profit = float(est.get("profitBeforeTax") or 0)
    est_cit = float(est.get("incomeTax") or 0)
    if abs(est_revenue - 50_000) > 0.005:
        fail(f"税负测算营业收入 {est_revenue} ≠ 50,000(结转后发生额口径)")
    if abs(est_profit - 37_500) > 0.005 or abs(est_cit - 9_375) > 0.005:
        fail(f"税负测算利润总额 {est_profit}/所得税 {est_cit} ≠ 37,500/9,375")
    ok(f"税负测算: 营收={est_revenue:,.2f} 利润总额={est_profit:,.2f} 企业所得税={est_cit:,.2f}")

    decl = req("GET", f"/api/tax-estimate/declaration?yearMonth={term}")["data"]
    lines = decl.get("lines") or []
    row1 = next((l for l in lines if l.get("rowNo") == "1"), None)
    if row1 is None or abs(float(row1.get("amount") or 0) - 50_000) > 0.005:
        fail(f"申报表第1行销售额 {row1 and row1.get('amount')} ≠ 50,000")
    ok(f"增值税申报表 {len(lines)} 行, 第1行销售额=50,000.00")


def main() -> None:
    print(f"=== jinbooks 月度全流程验收 @ {BASE} ===")
    login()
    book_id = setup_book()
    create_reviewer(book_id)
    term = current_term()
    print(f"    当前账期: {term}")
    subjects = fetch_subjects(book_id)
    if len(subjects) < 50:
        fail(f"科目数量异常: {len(subjects)}")
    ok(f"科目 {len(subjects)} 个")
    save_opening_balances(book_id)
    record_vouchers(book_id, term, subjects)
    expense_claim_flow(book_id, term, subjects)
    journal_flow(term, subjects)
    carry_and_close(book_id, term)
    reports_flow(term)
    print(f"\n=== 验收完成: {STEP_NO} 步全部通过 ===")


if __name__ == "__main__":
    main()
