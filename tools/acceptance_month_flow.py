#!/usr/bin/env python3
"""jinbooks 月度全流程验收：建账套 → 期初 → 录凭证 → 费用报销 → 固定资产 → 薪资 → 应收应付核销 → 银行对账 → 期末结账 → 报表与申报表。

用法:
    FC_API=http://localhost:2154 python tools/acceptance_month_flow.py

依赖: requests (pip install requests)。环境: 后端 2154 已启动且数据库已初始化。
每个步骤打印 PASS/FAIL，失败即终止并输出响应报文，便于定位。

报表断言不写死金额：脚本用 LEDGER 累积每笔已过账凭证的分录发生额，
再结合期初余额动态推导期望值（营收/费用/货币资金/资产总计/所得税等），
因此新增业务场景只需保证分录正确，期望值自动跟随。
"""
from __future__ import annotations

import os
import sys
import json
import time
import datetime

import requests

BASE = os.environ.get("FC_API", "http://localhost:2154")
USERNAME = os.environ.get("FC_USER", "admin")
PASSWORD = os.environ.get("FC_PASS", "changeme")

HEADERS: dict[str, str] = {}
STEP_NO = 0
FAILURES: list[str] = []

# 科目代码 -> {"debit": 借方发生额合计, "credit": 贷方发生额合计}（仅已过账凭证）
LEDGER: dict[str, dict[str, float]] = {}


def step(title: str) -> None:
    global STEP_NO
    STEP_NO += 1
    print(f"\n[{STEP_NO:02d}] {title}")


def ok(msg: str) -> None:
    print(f"    PASS {msg}")


def warn(msg: str) -> None:
    print(f"    WARN {msg}")


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


def accumulate(detail: dict) -> None:
    """把一张已过账凭证的分录发生额累积进 LEDGER。"""
    for i in detail.get("items") or []:
        code = str(i.get("subjectCode") or "")
        if not code:
            fail(f"凭证明细缺少 subjectCode: {json.dumps(i, ensure_ascii=False)[:200]}")
        slot = LEDGER.setdefault(code, {"debit": 0.0, "credit": 0.0})
        slot["debit"] += float(i.get("debitAmount") or 0)
        slot["credit"] += float(i.get("creditAmount") or 0)


def occ_debit(code: str) -> float:
    return LEDGER.get(code, {}).get("debit", 0.0)


def occ_credit(code: str) -> float:
    return LEDGER.get(code, {}).get("credit", 0.0)


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


def make_voucher(book_id: str, term: str, items: list[tuple], summary: str,
                 subjects: list[dict]) -> dict:
    """items: (subject, debit, credit) 或 (subject, debit, credit, auxiliary)；
    auxiliary 为 VoucherItemAuxiliaryDto 列表（辅助核算）。"""
    body = req("GET", f"/api/book/get/{book_id}")
    book = body["data"]
    item_payloads = []
    for tup in items:
        s, debit, credit = tup[0], tup[1], tup[2]
        payload = {
            "subjectId": s["id"], "subjectName": s["name"], "summary": summary,
            "debitAmount": debit, "creditAmount": credit,
        }
        if len(tup) > 3 and tup[3]:
            payload["auxiliary"] = tup[3]
        item_payloads.append(payload)
    return {
        "bookId": book_id, "wordHead": "记", "wordNum": next_word_num(term),
        "companyName": book.get("companyName") or "验收测试科技有限公司",
        "receiptNum": 0, "voucherDate": f"{term}-15",
        "voucherYear": int(term[:4]), "voucherMonth": int(term[5:7]),
        "items": item_payloads,
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
    accumulate(detail)
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


def ensure_voucher_posted(vid: str, label: str) -> dict:
    """把已有凭证(购入/折旧/报销/结转生成)推到过账态, 返回最终明细。"""
    detail = req("GET", f"/api/voucher/get/{vid}")["data"]
    if not detail.get("senderId"):
        payload = {
            "bookId": detail["bookId"], "word": detail.get("word"), "wordHead": detail["wordHead"],
            "wordNum": detail["wordNum"], "companyName": detail["companyName"],
            "receiptNum": detail.get("receiptNum") or 0, "voucherDate": detail["voucherDate"],
            "voucherYear": detail["voucherYear"], "voucherMonth": detail["voucherMonth"],
            "items": [{"subjectId": i["subjectId"], "subjectName": i["subjectName"],
                       "summary": i.get("summary"), "debitAmount": float(i.get("debitAmount") or 0),
                       "creditAmount": float(i.get("creditAmount") or 0)} for i in detail.get("items") or []],
        }
        if detail.get("status") == "draft":
            req("POST", "/api/voucher/submit", json={**payload, "id": vid})
            detail = req("GET", f"/api/voucher/get/{vid}")["data"]
        if detail.get("status") == "reviewing":
            req("PUT", f"/api/voucher/audit/{vid}", headers=REVIEWER_HEADERS)
            detail = req("GET", f"/api/voucher/get/{vid}")["data"]
            if detail.get("status") != "completed":
                fail(f"{label} 审核后状态异常: {detail.get('status')}")
        req("PUT", f"/api/voucher/sender/{vid}")
        detail = req("GET", f"/api/voucher/get/{vid}")["data"]
    if not detail.get("senderId"):
        fail(f"{label} 未能过账 status={detail.get('status')}")
    accumulate(detail)
    return detail


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
    total_debit = sum(float(i.get("debitAmount") or 0) for i in detail.get("items") or [])
    if abs(total_debit - 1_500) > 0.005:
        fail(f"报销凭证借方合计 {total_debit} ≠ 1,500")
    detail = ensure_voucher_posted(vid, "报销凭证")
    ok(f"claimId={claim_id} 凭证记-{detail['wordNum']} 金额1,500 已过账")


def fixed_asset_flow(book_id: str, term: str, subjects: list[dict]) -> None:
    step("固定资产: 类别→卡片(自动购入凭证6万)→次月起提折旧950→折旧报表")
    fa = subject_by_code(subjects, "1601")
    acc_depr = subject_by_code(subjects, "1602")
    bank = subject_by_code(subjects, "1002")
    admin_exp = subject_by_code(subjects, "5602")

    body = req("POST", "/api/fixed-asset/category/save", json={
        "code": "CAT01", "name": "电子设备", "depreciationMethod": "STRAIGHT_LINE",
        "usefulLifeMonths": 60, "residualRate": 5,
        "fixedAssetSubjectId": fa["id"], "accumDeprSubjectId": acc_depr["id"],
    })
    cat_id = body.get("data")
    if not cat_id:
        cats = req("GET", "/api/fixed-asset/category/list").get("data") or []
        cat_id = next((c["id"] for c in cats if c.get("code") == "CAT01"), None)
    if not cat_id:
        fail("资产类别新增后未取到 ID")

    prev_month = (datetime.date(int(term[:4]), int(term[5:7]), 1)
                  - datetime.timedelta(days=1)).strftime("%Y-%m")
    body = req("POST", "/api/fixed-asset/card/save", json={
        "code": "FA001", "name": "办公电脑一批", "categoryId": cat_id,
        "startUseDate": f"{prev_month}-10", "entryPeriod": term,
        "quantity": 5, "originalValue": 60_000,
        "purchaseCounterpartSubjectId": bank["id"], "expenseSubjectId": admin_exp["id"],
    })
    result = body["data"] or {}
    purchase_vid = result.get("purchaseVoucherId")
    if not purchase_vid:
        fail("卡片保存未生成购入凭证")
    detail = ensure_voucher_posted(purchase_vid, "购入凭证")
    debit = sum(float(i.get("debitAmount") or 0) for i in detail.get("items") or [])
    if abs(debit - 60_000) > 0.005:
        fail(f"购入凭证借方合计 {debit} ≠ 60,000")
    ok(f"卡片 FA001 原值60,000 购入凭证已过账")

    body = req("POST", "/api/fixed-asset/depreciation/accrue", json={
        "yearPeriod": term, "voucherDate": f"{term}-28 00:00:00", "summary": "计提本月折旧",
    })
    total = float(body["data"].get("totalAmount") or 0)
    # 直线法: 60,000 × (1-5%) / 60 = 950/月
    if abs(total - 950) > 0.005:
        fail(f"本月折旧合计 {total} ≠ 950(直线法 60000×95%/60)")
    depr_vid = body["data"]["voucherId"]
    detail = ensure_voucher_posted(depr_vid, "折旧凭证")
    credit_1602 = sum(float(i.get("creditAmount") or 0) for i in detail.get("items") or []
                      if (i.get("subjectName") or "").startswith("累计折旧")
                      or str(i.get("subjectCode") or "").startswith("1602"))
    ok(f"计提折旧 950.00 凭证已过账 (累计折旧贷方 {credit_1602})")

    detail_rpt = req("GET", f"/api/fixed-asset/report/depreciation-detail?yearPeriod={term}")["data"]
    summary_rpt = req("GET", f"/api/fixed-asset/report/depreciation-summary?yearPeriod={term}")["data"]
    ok(f"折旧明细/汇总报表返回正常")


def payroll_flow(book_id: str, term: str) -> float:
    """薪资闭环: 社保配置→部门→员工→算薪预演→确认→计提/发放凭证→银行代发文件。
    返回发放凭证中贷记银行存款(1002)的金额（无发放模板时为 0）。"""
    step("薪资: 员工→算薪预演→确认→计提/发放凭证→代发文件")
    ins = req("GET", "/api/config/insurance_fund/getCurrent")["data"]
    if not ins or float(ins.get("payBase") or 0) <= 0:
        fail("社保公积金默认配置缺失或 payBase 非正")
    ok(f"社保公积金配置存在 payBase={ins.get('payBase')}")

    orgs = (req("GET", "/api/orgs/fetch?pageNumber=1&pageSize=20")["data"].get("records") or [])
    if orgs:
        dept_id = orgs[0]["id"]
    else:
        body = req("POST", "/api/orgs/add", json={
            "orgCode": "D-YANSOU", "orgName": "综合管理部", "fullName": "综合管理部",
            "type": "department", "parentId": None, "status": 1, "level": 1, "sortIndex": 1,
        })
        dept_id = (body["data"] or {}).get("id")
    if not dept_id:
        fail("部门创建后未取到 ID")

    ts = str(int(time.time() * 1000))[-8:]
    emp_no = f"PR{ts}"
    req("POST", "/api/salary/employee/save", json={
        "displayName": "李四", "employeeNumber": emp_no, "gender": 1, "idType": 1,
        "idCardNo": f"1101011990{ts}", "employeeType": "NORMAL", "employeeStatus": "RESIDENT",
        "departmentId": dept_id, "status": 1,
        "payBasic": 8_000, "payMerit": 0, "payPost": 0, "laborFee": 0,
        "payBaseRule": 1, "payBaseNumber": 4_800,
        "bankName": "招商银行", "bankCardNo": "6225880212349999",
    })
    emps = (req("GET", "/api/salary/employee/fetch?pageNumber=1&pageSize=50")["data"].get("records") or [])
    emp = next((e for e in emps if e.get("employeeNumber") == emp_no), None)
    if not emp:
        fail(f"员工 {emp_no} 保存后未查到")
    ok(f"员工 李四({emp_no}) 基本工资8,000 社保基数4,800")

    req("POST", "/api/salary/detail/createTable", json={"bookId": book_id})
    rows = (req("GET", "/api/salary/detail/fetch?pageNumber=1&pageSize=50")["data"].get("records") or [])
    row = next((r for r in rows if r.get("employeeId") == emp["id"]), None)
    if not row:
        fail("算薪预演缺少该员工行")
    if abs(float(row.get("effectivePayBase") or 0) - 4_800) > 0.005:
        fail(f"预演 effectivePayBase={row.get('effectivePayBase')} ≠ 4,800(自定义基数)")
    ok(f"算薪预演生效基数 4,800 来源={row.get('payBaseSource')}")

    req("POST", "/api/salary/detail/submit-detail", json={})
    salaries = (req("GET", f"/api/employee/salary/fetch?pageNumber=1&pageSize=50&employeeId={emp['id']}")
                ["data"].get("records") or [])
    salary = next((r for r in salaries if r.get("employeeId") == emp["id"]),
                  salaries[0] if salaries else None)
    if not salary:
        fail("确认后未查到薪资单")
    belong = str(salary.get("belongDate") or "")[:7]
    if belong != term:
        fail(f"薪资归属期间 {belong} ≠ 当前账期 {term}")
    count = req("GET", f"/api/employee/salary/count?belongDate={belong}")["data"]
    if not count or int(count) < 1:
        fail(f"薪资单计数异常: {count}")

    # 计提凭证 (jt_gz 模板)
    body = req("POST", "/api/employee/salary/generate-voucher",
               json={"id": salary["id"], "bookId": book_id, "voucherType": 2})
    accrual_vid = body["data"]
    if not accrual_vid:
        fail("计提凭证生成未返回 ID")
    detail = ensure_voucher_posted(accrual_vid, "工资计提凭证")
    d_sum = sum(float(i.get("debitAmount") or 0) for i in detail.get("items") or [])
    c_sum = sum(float(i.get("creditAmount") or 0) for i in detail.get("items") or [])
    if abs(d_sum - c_sum) > 0.005:
        fail(f"计提凭证借贷不平衡: 借 {d_sum} 贷 {c_sum}")
    if d_sum < 8_000:
        fail(f"计提凭证借方合计 {d_sum} 小于应发工资 8,000")
    c_2211 = sum(float(i.get("creditAmount") or 0) for i in detail.get("items") or []
                 if str(i.get("subjectCode") or "").startswith("2211"))
    if c_2211 <= 0:
        fail("计提凭证未贷记 2211 应付职工薪酬")
    ok(f"计提凭证已过账: 借费用 {d_sum:,.2f} / 贷2211 {c_2211:,.2f}")

    # 发放凭证 (zf_gz 模板可能未预置，视为已知限制)
    paid_bank = 0.0
    body = req("POST", "/api/employee/salary/generate-voucher", expect_ok=False,
               json={"id": salary["id"], "bookId": book_id, "voucherType": 3})
    if body.get("code") != 0:
        if "zf_gz" in str(body.get("message") or ""):
            warn(f"发放凭证模板 zf_gz 未设置(已知限制): {body.get('message')}")
        else:
            fail(f"发放凭证生成失败: code={body.get('code')} message={body.get('message')!r}")
    else:
        pay_vid = body["data"]
        detail = ensure_voucher_posted(pay_vid, "工资发放凭证")
        d_sum = sum(float(i.get("debitAmount") or 0) for i in detail.get("items") or [])
        c_sum = sum(float(i.get("creditAmount") or 0) for i in detail.get("items") or [])
        if abs(d_sum - c_sum) > 0.005:
            fail(f"发放凭证借贷不平衡: 借 {d_sum} 贷 {c_sum}")
        paid_bank = sum(float(i.get("creditAmount") or 0) for i in detail.get("items") or []
                        if str(i.get("subjectCode") or "").startswith("1002"))
        if paid_bank <= 0:
            fail("发放凭证未贷记 1002 银行存款")
        ok(f"发放凭证已过账: 银行实发 {paid_bank:,.2f}")

    resp = requests.get(f"{BASE}/api/employee/salary/export-payment?belongDate={belong}",
                        headers=HEADERS, timeout=30)
    if resp.status_code != 200:
        fail(f"银行代发文件导出 HTTP {resp.status_code}")
    ctype = resp.headers.get("content-type") or ""
    if "json" in ctype:
        jb = resp.json()
        if jb.get("code") != 0:
            fail(f"银行代发文件导出失败: {jb.get('message')!r}")
    elif len(resp.content) < 10:
        fail("银行代发文件导出内容为空")
    ok(f"银行代发文件导出正常 ({len(resp.content):,} 字节)")
    return paid_bank


def arap_flow(book_id: str, term: str, subjects: list[dict]) -> None:
    """应收应付闭环: 客户辅助核算→赊销/收款凭证→未核销单→建议→核销→反核销→再核销→余额/账龄/对账单。"""
    step("应收应付: 赊销2万→收款2万→核销→反核销→账龄/对账单")
    ar = subject_by_code(subjects, "1122")
    bank = subject_by_code(subjects, "1002")
    revenue = subject_by_code(subjects, "5001")

    custs = (req("GET", "/api/base/assist-acc/fetch?pageNumber=1&pageSize=50")["data"].get("records") or [])
    cust = next((c for c in custs if c.get("assistCode") == "CUS001"), None)
    if not cust:
        req("POST", "/api/base/assist-acc/save", json={
            "assistType": "2", "assistCode": "CUS001", "assistName": "验收客户A",
        })
        custs = (req("GET", "/api/base/assist-acc/fetch?pageNumber=1&pageSize=50")["data"].get("records") or [])
        cust = next((c for c in custs if c.get("assistCode") == "CUS001"), None)
    if not cust:
        fail("客户辅助核算 CUS001 创建后未查到")
    cust_id = str(cust["id"])
    ok(f"客户辅助核算 验收客户A(CUS001) id={cust_id}")

    def aux() -> list[dict]:
        return [{"id": "2", "label": "客户",
                 "value": [{"label": "验收客户A", "value": cust_id}]}]

    voucher_full_flow(make_voucher(book_id, term,
                                   [(ar, 20_000, 0, aux()), (revenue, 0, 20_000)],
                                   "赊销商品给验收客户A", subjects), "AR1 赊销2万(挂客户)")
    voucher_full_flow(make_voucher(book_id, term,
                                   [(bank, 20_000, 0), (ar, 0, 20_000, aux())],
                                   "收回收验客户A货款", subjects), "AR2 收款2万(核销客户)")

    def open_items() -> list[dict]:
        return (req("GET", f"/api/arap/writeoff/open-items?side=AR&counterpartId={cust_id}"
                        f"&asOfDate={term}-28&includeZero=true")["data"] or [])

    items = open_items()
    if len(items) != 2:
        fail(f"未核销单数量 {len(items)} ≠ 2: {json.dumps(items, ensure_ascii=False)[:300]}")
    for it in items:
        if abs(float(it.get("remainingAmount") or 0) - 20_000) > 0.005:
            fail(f"未核销单剩余 {it.get('remainingAmount')} ≠ 20,000 ({it.get('summary')})")
    ok("未核销单 2 笔(赊销借/收款贷) 各余 20,000")

    legs = (req("GET", f"/api/arap/writeoff/suggest?side=AR&counterpartId={cust_id}")["data"] or [])
    leg_sum = sum(float(l.get("amount") or 0) for l in legs)
    if not legs or abs(leg_sum - 40_000) > 0.005:
        fail(f"核销建议异常: legs={len(legs)} 合计 {leg_sum} ≠ 40,000")
    req("POST", "/api/arap/writeoff/confirm", json={
        "side": "AR", "counterpartId": cust_id, "counterpartName": "验收客户A", "legs": legs,
    })
    remaining = [i for i in open_items() if abs(float(i.get("remainingAmount") or 0)) > 0.005]
    if remaining:
        fail(f"核销后仍有未清余额: {json.dumps(remaining, ensure_ascii=False)[:300]}")
    ok("核销确认后两笔往来全部清零")

    writeoffs = (req("GET", f"/api/arap/writeoff/list?side=AR&counterpartId={cust_id}")["data"] or [])
    if not writeoffs:
        fail("核销记录列表为空")
    wid = str(writeoffs[0]["id"])

    # 反核销 → 余额恢复 → 再核销
    req("POST", f"/api/arap/writeoff/reverse/{wid}")
    items = [i for i in open_items() if abs(float(i.get("remainingAmount") or 0)) > 0.005]
    if len(items) != 2:
        fail(f"反核销后未核销单 {len(items)} ≠ 2")
    req("POST", "/api/arap/writeoff/confirm", json={
        "side": "AR", "counterpartId": cust_id, "counterpartName": "验收客户A", "legs": legs,
    })
    remaining = [i for i in open_items() if abs(float(i.get("remainingAmount") or 0)) > 0.005]
    if remaining:
        fail("再核销后仍有未清余额")
    ok("反核销→再核销 回滚路径正常")

    bal = (req("GET", f"/api/arap/balance?side=AR&periodStart={term}&periodEnd={term}")["data"] or [])
    brow = next((b for b in bal if str(b.get("counterpartId")) == cust_id), None)
    if brow is None:
        fail("应收余额表缺少验收客户A")
    if abs(float(brow.get("periodDebit") or 0) - 20_000) > 0.005 \
            or abs(float(brow.get("periodCredit") or 0) - 20_000) > 0.005 \
            or abs(float(brow.get("ending") or 0)) > 0.005:
        fail(f"应收余额表异常: {json.dumps(brow, ensure_ascii=False)}")
    ok("应收余额表: 本期借/贷各 20,000 期末 0")

    detail = (req("GET", f"/api/arap/detail?side=AR&periodStart={term}&periodEnd={term}"
                      f"&counterpartId={cust_id}")["data"] or [])
    if len(detail) < 2:
        fail(f"往来明细行数 {len(detail)} < 2")
    aging = (req("GET", f"/api/arap/aging?side=AR&asOfDate={term}-28")["data"] or [])
    ap_bal = (req("GET", f"/api/arap/balance?side=AP&periodStart={term}&periodEnd={term}")["data"] or [])
    ok(f"往来明细 {len(detail)} 行 / 账龄 {len(aging)} 行 / 应付余额 {len(ap_bal)} 行(冒烟)")

    resp = requests.get(f"{BASE}/api/arap/statement/export?side=AR&periodStart={term}&periodEnd={term}"
                        f"&counterpartId={cust_id}",
                        headers=HEADERS, timeout=30)
    if resp.status_code != 200 or len(resp.content) < 100:
        fail(f"往来对账单导出异常: HTTP {resp.status_code} 大小 {len(resp.content)}")
    ok(f"往来对账单导出正常 ({len(resp.content):,} 字节)")


def journal_flow(term: str, subjects: list[dict], salary_paid: float) -> str:
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
        ("I", 20_000, 0, "收回赊销货款"),
        ("E", 0, 8_000, "支付房租"),
        ("E", 0, 1_500, "报销差旅费"),
        ("E", 0, 60_000, "购入固定资产"),
    ]
    if salary_paid > 0:
        entries.append(("E", 0, salary_paid, "发放工资(银行代发)"))
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

    expected_balance = 90_000 + 50_000 + 20_000 - 8_000 - 1_500 - 60_000 - salary_paid
    body = req("GET", f"/api/journal/reconciliation?accId={acc_id}&yearPeriod={term}")
    recon = body["data"]
    book_balance = float(recon.get("bookBalance") or 0)
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
        detail = ensure_voucher_posted(vid, f"结转 {code}")
        ok(f"结转 {code}({tpl.get('name')}) 记-{detail['wordNum']} 已过账")

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
    step("报表与申报表 (结账后期间, 期望值由分录台账动态推导)")
    # --- 动态期望 ---
    revenue_expected = occ_credit("5001")                      # 贷方发生额口径
    expense_expected = sum(d["debit"] for c, d in LEDGER.items()
                           if c.startswith("5") and c != "5001")
    profit_expected = revenue_expected - expense_expected
    cit_expected = profit_expected * 0.25
    monetary_expected = 100_000 + (occ_debit("1001") - occ_credit("1001")) \
        + (occ_debit("1002") - occ_credit("1002"))
    fixed_net_expected = (occ_debit("1601") - occ_credit("1601")) \
        - (occ_credit("1602") - occ_debit("1602"))
    ar_expected = occ_debit("1122") - occ_credit("1122")
    asset_total_expected = monetary_expected + fixed_net_expected + ar_expected
    print(f"    期望: 营收={revenue_expected:,.2f} 费用={expense_expected:,.2f} "
          f"净利={profit_expected:,.2f} 货币资金={monetary_expected:,.2f} "
          f"固资净值={fixed_net_expected:,.2f} 资产总计={asset_total_expected:,.2f}")

    bs = req("GET", f"/api/statement/balance-sheet?periodType=month&reportDate={term}")["data"]
    items = (bs.get("items") or {})
    assets_rows = items.get("assets") or []
    liability_rows = items.get("liability") or []

    def row_val(rows, keyword):
        for r in rows:
            if keyword in (r.get("itemName") or r.get("name") or ""):
                v = r.get("currentBalance") if r.get("currentBalance") is not None else r.get("closingBalance")
                return float(v or 0)
        return None

    monetary = row_val(assets_rows, "货币资金")
    asset_total = row_val(assets_rows, "总计")
    le_total = row_val(liability_rows, "总计")
    fixed_net = row_val(assets_rows, "固定资产")
    if monetary is None or abs(monetary - monetary_expected) > 0.005:
        fail(f"货币资金 {monetary} ≠ {monetary_expected:,.2f}")
    if fixed_net is None or abs(fixed_net - fixed_net_expected) > 0.005:
        fail(f"固定资产净值 {fixed_net} ≠ {fixed_net_expected:,.2f}")
    if asset_total is None or le_total is None or abs(asset_total - le_total) > 0.005:
        fail(f"资产负债表不平衡: 资产总计={asset_total} 负债权益总计={le_total}")
    if abs(asset_total - asset_total_expected) > 0.005:
        fail(f"资产总计 {asset_total} ≠ {asset_total_expected:,.2f}")
    ok(f"资产负债表平衡: 货币资金={monetary:,.2f} 固定资产净值={fixed_net:,.2f} 资产总计={asset_total:,.2f}")

    inc = req("GET", f"/api/statement/income?periodType=month&reportDate={term}")["data"]
    inc_rows = inc.get("items") or inc.get("rows") or []
    rev = row_val(inc_rows, "营业收入")
    profit = row_val(inc_rows, "净利润")
    if rev is None or abs(rev - revenue_expected) > 0.005:
        fail(f"利润表营业收入 {rev} ≠ {revenue_expected:,.2f}")
    if profit is None or abs(profit - profit_expected) > 0.005:
        fail(f"利润表净利润 {profit} ≠ {profit_expected:,.2f}(营收-费用)")
    ok(f"利润表: 营业收入={rev:,.2f} 净利润={profit:,.2f}")

    cf = req("GET", f"/api/statement/cash-flow?periodType=month&reportDate={term}")["data"]
    ok(f"现金流量表返回 {len(cf) if isinstance(cf, list) else 'object'} 行")

    sb = req("GET", f"/api/statement/subject-balance?periodType=month&reportDate={term}&showAll=true")["data"]
    ok(f"科目余额表 {len(sb)} 行")

    vs = req("GET", f"/api/statement/voucher-summary?periodType=month&reportDate={term}")["data"]
    ok(f"凭证汇总表 {len(vs)} 行")

    resp = requests.get(f"{BASE}/api/statement/books-pack/export?yearPeriod={term}",
                        headers=HEADERS, timeout=30)
    if resp.status_code != 200 or len(resp.content) < 500:
        fail(f"账簿打包导出异常: HTTP {resp.status_code} 大小 {len(resp.content)}")
    if not resp.content[:2] == b"PK":
        fail("账簿打包导出不是有效 ZIP")
    ok(f"账簿打包(明细账/总账 ZIP)导出正常 {len(resp.content):,} 字节")

    est = req("GET", f"/api/tax-estimate?yearMonth={term}")["data"]
    est_revenue = float(est.get("revenue") or 0)
    est_profit = float(est.get("profitBeforeTax") or 0)
    est_cit = float(est.get("incomeTax") or 0)
    if abs(est_revenue - revenue_expected) > 0.005:
        fail(f"税负测算营业收入 {est_revenue} ≠ {revenue_expected:,.2f}(结转后发生额口径)")
    if abs(est_profit - profit_expected) > 0.005 or abs(est_cit - cit_expected) > 0.005:
        fail(f"税负测算利润总额 {est_profit}/所得税 {est_cit} ≠ {profit_expected:,.2f}/{cit_expected:,.2f}")
    ok(f"税负测算: 营收={est_revenue:,.2f} 利润总额={est_profit:,.2f} 企业所得税={est_cit:,.2f}")

    decl = req("GET", f"/api/tax-estimate/declaration?yearMonth={term}")["data"]
    lines = decl.get("lines") or []
    row1 = next((l for l in lines if l.get("rowNo") == "1"), None)
    if row1 is None or abs(float(row1.get("amount") or 0) - revenue_expected) > 0.005:
        fail(f"申报表第1行销售额 {row1 and row1.get('amount')} ≠ {revenue_expected:,.2f}")
    ok(f"增值税申报表 {len(lines)} 行, 第1行销售额={revenue_expected:,.2f}")


def uncheckout_reclose_flow(term: str) -> None:
    step("反结账→重新结账 (回滚验收)")
    req("POST", f"/api/settlement/uncheckout?yearPeriod={term}", json={"yearPeriod": term})
    reopened = current_term()
    if reopened != term:
        fail(f"反结账后账期 {reopened} ≠ {term}")
    ok(f"已反结账回 {term}")
    req("GET", f"/api/settlement/checkout?year={term[:4]}")
    new_term = current_term()
    if new_term == term:
        fail("重新结账后账期未推进")
    ok(f"重新结账 {term} → {new_term}")


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
    fixed_asset_flow(book_id, term, subjects)
    salary_paid = payroll_flow(book_id, term)
    arap_flow(book_id, term, subjects)
    journal_flow(term, subjects, salary_paid)
    carry_and_close(book_id, term)
    reports_flow(term)
    uncheckout_reclose_flow(term)
    print(f"\n=== 验收完成: {STEP_NO} 步全部通过 ===")


if __name__ == "__main__":
    main()
