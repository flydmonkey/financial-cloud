#!/usr/bin/env python3
"""jinbooks 企业会计制度(standard=2)账套端到端验收：
建账 → 期初 → 录凭证(5101收入/5402税金/5502费用/5201投资收益/1122应收利息)
→ 结转损益(3131本年利润) → 结账 → 资产负债表/利润表勾稽。

用法:
    FC_API=http://localhost:2154 python tools/acceptance_enterprise_standard.py

关键守卫:
- 应收账款(1131)与应收利息(1122)同时存在，资产负债表规则 1131 不得经别名误并 1122 的余额；
- 利润表锚点 1/2/3/4 + 1xx 减项/加项符号约定在 企业会计制度 模板下数值正确；
- 结转模板已按准则分发（qm_jz_sr/qm_jz_cbfy 用 3131）。
"""
from __future__ import annotations

import datetime
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import acceptance_month_flow as mf


def setup_enterprise_book() -> str:
    mf.step("创建企业会计制度账套并切换")
    enable_date = datetime.date.today().strftime("%Y-%m")
    body = mf.req("POST", "/api/book/setup", json={
        "name": "企业制度验收账套", "companyName": "企业制度验收有限公司",
        "standardId": "2", "enableDate": enable_date,
        "vatType": 1, "voucherReviewed": 1, "status": 1,
    })
    book_id = body["data"]["bookId"]
    mf.req("GET", f"/api/users/switchBook/{book_id}")
    mf.ok(f"bookId={book_id} 准则=企业会计制度 启用期间={enable_date}")
    return book_id


def save_opening_balances(book_id: str) -> None:
    mf.step("录入期初余额 (现金1万/银行9万/实收资本10万)")
    rows = mf.req("GET", "/api/base/init-balance/list").get("data") or []

    def row_of(code: str):
        for r in rows:
            if r.get("code") == code:
                return r
        mf.fail(f"期初列表缺少 {code}")

    payload = []
    for row, debit, credit in [
        (row_of("1001"), 10_000, 0),
        (row_of("1002"), 90_000, 0),
        (row_of("3101"), 0, 100_000),
    ]:
        if row.get("hasVoucher"):
            mf.fail(f"{row['code']} 已有凭证，无法录入期初")
        payload.append({
            **row, "bookId": book_id,
            "openingYearBalanceDebit": debit, "openingYearBalanceCredit": credit,
            "debitAmount": row.get("debitAmount") or 0, "creditAmount": row.get("creditAmount") or 0,
            "balance": debit - credit,
        })
    mf.req("POST", "/api/base/init-balance/save", json=payload)
    mf.ok("期初借贷平衡 100,000 = 100,000")


def record_vouchers(book_id: str, term: str, subjects: list[dict]) -> None:
    mf.step("录入业务凭证 (5笔: 销售收款/赊销/管理费用/税金计提/计提利息收入)")
    bank = mf.subject_by_code(subjects, "1002")
    ar = mf.subject_by_code(subjects, "1131")
    interest = mf.subject_by_code(subjects, "1122")
    revenue = mf.subject_by_code(subjects, "5101")
    invest = mf.subject_by_code(subjects, "5201")
    tax_sur = mf.subject_by_code(subjects, "5402")
    tax_pay = mf.subject_by_code(subjects, "2171")
    admin_exp = mf.subject_by_code(subjects, "5502")

    mf.voucher_full_flow(mf.make_voucher(book_id, term, [(bank, 50_000, 0), (revenue, 0, 50_000)],
                                         "销售商品收到银行转账", subjects), "V1 销售收款5万")
    mf.voucher_full_flow(mf.make_voucher(book_id, term, [(ar, 20_000, 0), (revenue, 0, 20_000)],
                                         "赊销商品", subjects), "V2 赊销2万")
    mf.voucher_full_flow(mf.make_voucher(book_id, term, [(admin_exp, 8_000, 0), (bank, 0, 8_000)],
                                         "支付本月办公房租", subjects), "V3 管理费用8千")
    mf.voucher_full_flow(mf.make_voucher(book_id, term, [(tax_sur, 3_500, 0), (tax_pay, 0, 3_500)],
                                         "计提主营业务税金及附加", subjects), "V4 税金计提3.5千")
    # 计提应收利息：验证 1122 与 1131 别名隔离 + 利润表加项符号
    mf.voucher_full_flow(mf.make_voucher(book_id, term, [(interest, 500, 0), (invest, 0, 500)],
                                         "计提存款利息收入", subjects), "V5 计提利息500")


def carry_and_close(book_id: str, term: str) -> None:
    mf.step("期末: 结转损益(3131)→结账检查→结账")
    templates = (mf.req("GET", "/api/settlementcarry/fetchcarry?pageNumber=1&pageSize=50")
                 ["data"].get("records") or [])
    for code in ("qm_jz_sr", "qm_jz_cbfy"):
        tpl = next((t for t in templates if t.get("code") == code), None)
        if not tpl:
            mf.fail(f"缺少结转模板 {code}")
        body = mf.req("POST", "/api/settlementcarry/generate-voucher",
                      json={"id": tpl["id"], "templateId": tpl["id"], "voucherType": 1})
        vid = body["data"]
        detail = mf.ensure_voucher_posted(vid, f"结转 {code}")
        codes = [str(i.get("subjectCode") or "") for i in detail.get("items") or []]
        if "3131" not in codes:
            mf.fail(f"结转 {code} 未使用企业会计制度科目 3131 本年利润: {codes}")
        mf.ok(f"结转 {code}({tpl.get('name')}) 记-{detail['wordNum']} 已过账, 本年利润3131 在分录中")

    gaps = mf.req("GET", "/api/voucher/successive").get("data") or []
    if gaps:
        mf.req("PUT", "/api/voucher/successive", json=gaps)
        mf.ok(f"整理凭证号断号 {len(gaps)} 处")

    checks = mf.req("GET", "/api/settlement/verify").get("data") or []
    hard_failed = [c for c in checks
                   if c.get("hard") is not False and c.get("applicable") is not False
                   and not c.get("result")]
    if hard_failed:
        mf.fail("结账硬检未通过: " + "; ".join(f"{c.get('item')}:{c.get('reason')}" for c in hard_failed))
    mf.ok(f"结账检查 {len(checks)} 项全部通过")

    mf.req("GET", f"/api/settlement/checkout?year={term[:4]}")
    new_term = mf.current_term()
    if new_term == term:
        mf.fail(f"结账后账期未推进: 仍为 {term}")
    mf.ok(f"已结账 {term} → 当前账期 {new_term}")


def reports_flow(term: str) -> None:
    mf.step("报表勾稽 (期望值由分录台账动态推导)")
    revenue = mf.occ_credit("5101")
    tax_sur = mf.occ_debit("5402")
    admin = mf.occ_debit("5502")
    invest = mf.occ_credit("5201")
    # 企业会计制度口径：投资收益在营业利润之后计入利润总额（与 CAS2006 不同）
    operating_expected = revenue - tax_sur - admin
    total_profit_expected = operating_expected + invest
    net_expected = total_profit_expected  # 无营业外收支与所得税
    monetary_expected = 100_000 + (mf.occ_debit("1001") - mf.occ_credit("1001")) \
        + (mf.occ_debit("1002") - mf.occ_credit("1002"))
    ar_expected = mf.occ_debit("1131") - mf.occ_credit("1131")
    interest_expected = mf.occ_debit("1122") - mf.occ_credit("1122")
    tax_pay_expected = mf.occ_credit("2171") - mf.occ_debit("2171")
    capital_expected = 100_000.0
    asset_total_expected = monetary_expected + ar_expected + interest_expected
    print(f"    期望: 主营收入={revenue:,.2f} 营业利润={operating_expected:,.2f} "
          f"净利={net_expected:,.2f} 货币资金={monetary_expected:,.2f} "
          f"应收={ar_expected:,.2f} 应收利息={interest_expected:,.2f} 资产总计={asset_total_expected:,.2f}")

    bs = mf.req("GET", f"/api/statement/balance-sheet?periodType=month&reportDate={term}")["data"]
    items = (bs.get("items") or {})
    assets_rows = items.get("assets") or []
    liability_rows = items.get("liability") or []
    if not assets_rows or not liability_rows:
        mf.fail("企业会计制度资产负债表为空（模板未初始化）")

    def row_val(rows, keyword):
        for r in rows:
            if keyword in (r.get("itemName") or r.get("name") or ""):
                v = r.get("currentBalance") if r.get("currentBalance") is not None else r.get("closingBalance")
                return float(v or 0)
        return None

    for label, rows, expected in [
        ("货币资金", assets_rows, monetary_expected),
        ("应收账款", assets_rows, ar_expected),
        ("应收利息", assets_rows, interest_expected),
        ("应交税金", liability_rows, tax_pay_expected),
        ("实收资本", liability_rows, capital_expected),
        ("未分配利润", liability_rows, net_expected),
    ]:
        actual = row_val(rows, label)
        if actual is None or abs(actual - expected) > 0.005:
            mf.fail(f"资产负债表 {label} {actual} ≠ {expected:,.2f}")

    asset_total = row_val(assets_rows, "总计")
    le_total = row_val(liability_rows, "总计")
    if asset_total is None or le_total is None or abs(asset_total - le_total) > 0.005:
        mf.fail(f"资产负债表不平衡: 资产总计={asset_total} 负债权益总计={le_total}")
    if abs(asset_total - asset_total_expected) > 0.005:
        mf.fail(f"资产总计 {asset_total} ≠ {asset_total_expected:,.2f}")
    mf.ok(f"资产负债表平衡: 货币资金={monetary_expected:,.2f} 应收账款={ar_expected:,.2f} "
          f"应收利息={interest_expected:,.2f} 资产总计={asset_total:,.2f}")

    inc = mf.req("GET", f"/api/statement/income?periodType=month&reportDate={term}")["data"]
    inc_rows = inc.get("items") or inc.get("rows") or []
    if not inc_rows:
        mf.fail("企业会计制度利润表为空（模板未初始化）")
    for label, expected in [
        ("主营业务收入", revenue),
        ("营业利润", operating_expected),
        ("利润总额", total_profit_expected),
        ("净利润", net_expected),
    ]:
        actual = row_val(inc_rows, label)
        if actual is None or abs(actual - expected) > 0.005:
            mf.fail(f"利润表 {label} {actual} ≠ {expected:,.2f}")
    mf.ok(f"利润表: 主营业务收入={revenue:,.2f} 营业利润={operating_expected:,.2f} 净利润={net_expected:,.2f}")

    sb = mf.req("GET", f"/api/statement/subject-balance?periodType=month&reportDate={term}&showAll=true")["data"]
    mf.ok(f"科目余额表 {len(sb)} 行")


def main() -> None:
    print(f"=== jinbooks 企业会计制度账套验收 @ {mf.BASE} ===")
    mf.login()
    book_id = setup_enterprise_book()
    mf.create_reviewer(book_id)
    term = mf.current_term()
    print(f"    当前账期: {term}")
    subjects = mf.fetch_subjects(book_id)
    if len(subjects) < 100:
        mf.fail(f"科目数量异常: {len(subjects)}（企业会计制度应为 161）")
    for code in ("5101", "5402", "5502", "5201", "1122", "1131", "2171", "3101", "3131"):
        mf.subject_by_code(subjects, code)
    mf.ok(f"科目 {len(subjects)} 个, 关键科目齐全")
    save_opening_balances(book_id)
    record_vouchers(book_id, term, subjects)
    carry_and_close(book_id, term)
    reports_flow(term)
    print(f"\n=== 企业会计制度验收完成: {mf.STEP_NO} 步全部通过 ===")


if __name__ == "__main__":
    main()
