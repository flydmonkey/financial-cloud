#!/usr/bin/env python3
"""jinbooks 年度验收：连续月结 9→12 月 → 12 月年结硬门(qm_jz_bnlr 本年利润转利润分配) → 跨年新账期 → 红字冲销。

用法:
    FC_API=http://localhost:2154 python tools/acceptance_year_close.py

前置: 先执行 tools/clear_books.py 清空账套。复用 acceptance_month_flow 的助手函数与分录台账。
"""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import acceptance_month_flow as m


def sale_voucher(book_id: str, term: str, subjects: list[dict], amount: float, label: str) -> str:
    bank = m.subject_by_code(subjects, "1002")
    revenue = m.subject_by_code(subjects, "5001")
    return m.voucher_full_flow(
        m.make_voucher(book_id, term, [(bank, amount, 0), (revenue, 0, amount)],
                       f"{term} 销售收款", subjects), label)


def expense_voucher(book_id: str, term: str, subjects: list[dict], amount: float, label: str) -> str:
    bank = m.subject_by_code(subjects, "1002")
    admin_exp = m.subject_by_code(subjects, "5602")
    return m.voucher_full_flow(
        m.make_voucher(book_id, term, [(admin_exp, amount, 0), (bank, 0, amount)],
                       f"{term} 费用支出", subjects), label)


def carry_month(book_id: str, term: str, codes=("qm_jz_sr", "qm_jz_cbfy")) -> None:
    """生成并过账指定结转模板凭证；无余额时后端拒生成，记为跳过。"""
    templates = (m.req("GET", "/api/settlementcarry/fetchcarry?pageNumber=1&pageSize=50")
                 ["data"].get("records") or [])
    for code in codes:
        tpl = next((t for t in templates if t.get("code") == code), None)
        if not tpl:
            m.fail(f"缺少结转模板 {code}")
        body = m.req("POST", "/api/settlementcarry/generate-voucher", expect_ok=False,
                     json={"id": tpl["id"], "templateId": tpl["id"], "voucherType": 1})
        if body.get("code") != 0:
            if "无需结转" in str(body.get("message") or ""):
                m.ok(f"结转 {code} 本期无余额，跳过({body.get('message')})")
                continue
            m.fail(f"结转 {code} 生成失败: code={body.get('code')} message={body.get('message')!r}")
        m.ensure_voucher_posted(body["data"], f"结转 {code}")
        m.ok(f"结转 {code}({tpl.get('name')}) 已过账")


def checkout(book_id: str, term: str) -> str:
    gaps = m.req("GET", "/api/voucher/successive").get("data") or []
    if gaps:
        m.req("PUT", "/api/voucher/successive", json=gaps)
    m.req("GET", f"/api/settlement/checkout?year={term[:4]}")
    new_term = m.current_term()
    if new_term == term:
        m.fail(f"结账后账期未推进: 仍为 {term}")
    m.ok(f"已结账 {term} → {new_term}")
    return new_term


def year_profit_balance() -> float:
    """3103 本年利润当前余额(贷方正数)。"""
    return m.occ_credit("3103") - m.occ_debit("3103")


def multi_month_close(book_id: str, term: str, subjects: list[dict]) -> str:
    """9→10→11 月连续月结：每月一笔小额销售，验证跨期推进。"""
    m.step("连续月结: 9/10/11 月各一笔销售后结账")
    plan = [(term, 10_000), ]
    y, mo = int(term[:4]), int(term[5:7])
    for _ in range(2):
        mo += 1
        plan.append((f"{y}-{mo:02d}", 1_000))
    for t, amount in plan:
        sale_voucher(book_id, t, subjects, amount, f"{t} 销售{amount:,.0f}")
        carry_month(book_id, t)
        checkout(book_id, t)
    new_term = m.current_term()
    if new_term != f"{y}-12":
        m.fail(f"连续月结后账期 {new_term} ≠ {y}-12")
    return new_term


def december_year_close(book_id: str, term: str, subjects: list[dict]) -> str:
    """12 月: 业务→月度结转→年结硬门拦截→qm_jz_bnlr→结账跨年。"""
    m.step("12月: 销售2,000+费用500→月度结转→年结硬门→qm_jz_bnlr→跨年结账")
    sale_vid = sale_voucher(book_id, term, subjects, 2_000, "12月销售2,000")
    expense_voucher(book_id, term, subjects, 500, "12月费用500")
    carry_month(book_id, term)

    profit = year_profit_balance()
    expected_profit = 10_000 + 1_000 + 1_000 + 2_000 - 500
    if abs(profit - expected_profit) > 0.005:
        m.fail(f"年结前 3103 本年利润余额 {profit} ≠ {expected_profit:,.2f}")
    m.ok(f"年结前 3103 本年利润余额 {profit:,.2f}")

    # 硬门: 未做 qm_jz_bnlr 时 12 月结账必须被拒
    body = m.req("GET", f"/api/settlement/checkout?year={term[:4]}", expect_ok=False)
    if body.get("code") == 0:
        m.fail("12月未做本年利润结转，结账却未被拦截")
    if "结账条件" not in str(body.get("message") or ""):
        m.fail(f"12月结账拦截消息异常: {body.get('message')!r}")
    m.ok(f"硬门拦截生效: {body.get('message')}")

    carry_month(book_id, term, codes=("qm_jz_bnlr",))
    bnlr_credit = m.occ_credit("3104.02")
    if abs(bnlr_credit - expected_profit) > 0.005:
        m.fail(f"qm_jz_bnlr 贷记 3104.02 未分配利润 {bnlr_credit} ≠ {expected_profit:,.2f}")
    if abs(year_profit_balance()) > 0.005:
        m.fail(f"年结后 3103 余额 {year_profit_balance()} 未清零")
    m.ok(f"qm_jz_bnlr: 借3103/贷3104.02 {expected_profit:,.2f}, 本年利润清零")

    new_term = checkout(book_id, term)
    y = int(term[:4])
    if new_term != f"{y + 1}-01":
        m.fail(f"跨年结账后账期 {new_term} ≠ {y + 1}-01")
    m.ok(f"跨年结账成功 → {new_term}")

    # 12月科目余额快照: 3103=0, 3104.02=13,500
    sb = m.req("GET", f"/api/statement/subject-balance?periodType=month&reportDate={term}&showAll=true")["data"]
    def closing(code: str) -> float:
        for r in sb:
            if str(r.get("subjectCode") or r.get("code") or "") == code:
                v = r.get("closingBalance")
                if v is None:
                    v = (float(r.get("closingBalanceCredit") or 0) - float(r.get("closingBalanceDebit") or 0))
                return float(v or 0)
        return None
    c3103 = closing("3103")
    c3104 = closing("3104.02")
    if c3103 is not None and abs(c3103) > 0.005:
        m.fail(f"12月 3103 期末余额 {c3103} ≠ 0")
    if c3104 is not None and abs(c3104 - expected_profit) > 0.005:
        m.fail(f"12月 3104.02 期末余额 {c3104} ≠ {expected_profit:,.2f}")
    m.ok(f"12月科目余额快照: 3103={c3103} 3104.02={c3104}")
    return sale_vid


def new_year_opening_check(term: str) -> None:
    """新年度期初滚存: 资产/权益科目上年期末应滚入新年期初, 损益类归零。"""
    m.step("新年度期初滚存检查 (2027-01 期初)")
    sb = m.req("GET", f"/api/statement/subject-balance?periodType=month&reportDate={term}&showAll=true")["data"]

    def row_of(code: str) -> dict | None:
        return next((r for r in sb if str(r.get("subjectCode") or r.get("code") or "") == code), None)

    # 银行存款: 90,000期初 +10,000+1,000+1,000+2,000 销售 -500 费用 = 103,500
    bank = row_of("1002")
    if bank is None:
        m.fail("新账期科目余额表缺少 1002")
    bank_open = float(bank.get("openingYearBalanceDebit") or 0) - float(bank.get("openingYearBalanceCredit") or 0)
    if abs(bank_open - 103_500) > 0.005:
        m.fail(f"1002 新年初余额 {bank_open} ≠ 103,500(上年期末滚存)")
    # 实收资本滚存 100,000
    capital = row_of("3001")
    cap_open = float(capital.get("openingYearBalanceCredit") or 0) - float(capital.get("openingYearBalanceDebit") or 0)
    if abs(cap_open - 100_000) > 0.005:
        m.fail(f"3001 新年初余额 {cap_open} ≠ 100,000")
    # 未分配利润滚存 13,500(年结成果)
    profit = row_of("3104.02")
    if profit is None:
        m.fail("新账期科目余额表缺少 3104.02(年结滚存缺失)")
    p_open = float(profit.get("openingYearBalanceCredit") or 0) - float(profit.get("openingYearBalanceDebit") or 0)
    if abs(p_open - 13_500) > 0.005:
        m.fail(f"3104.02 新年初余额 {p_open} ≠ 13,500(本年利润年结滚存)")
    # 损益类新年初始应为 0
    rev = row_of("5001")
    if rev is not None:
        r_open = float(rev.get("openingYearBalanceCredit") or 0) - float(rev.get("openingYearBalanceDebit") or 0)
        if abs(r_open) > 0.005:
            m.fail(f"5001 新年初余额 {r_open} ≠ 0(损益类不应滚存)")
    m.ok(f"期初滚存正确: 1002=103,500 3001=100,000 3104.02=13,500 5001=0")


def reversal_flow(book_id: str, term: str, subjects: list[dict], target_vid: str) -> None:
    """红字冲销: 跨期冲销12月销售凭证(落新账期)→金额全负→过账→重复冲销/未过账冲销拦截。"""
    m.step("红字冲销: 冲销12月销售凭证→过账→重复/未过账拦截")
    src = m.req("GET", f"/api/voucher/get/{target_vid}")["data"]
    body = m.req("POST", f"/api/voucher/reverse/{target_vid}")
    rev_id = body["data"]
    if not rev_id:
        m.fail("红字冲销未返回冲销凭证 ID")
    rev = m.req("GET", f"/api/voucher/get/{rev_id}")["data"]
    if str(rev.get("sourceVoucherId") or "") != str(target_vid):
        m.fail(f"冲销凭证 sourceVoucherId={rev.get('sourceVoucherId')} 未指向原凭证 {target_vid}")
    items = rev.get("items") or []
    if len(items) != len(src.get("items") or []):
        m.fail(f"冲销凭证明细行数 {len(items)} ≠ 原凭证 {len(src.get('items') or [])}")
    for i in items:
        d = float(i.get("debitAmount") or 0)
        c = float(i.get("creditAmount") or 0)
        if d > 0 or c > 0:
            m.fail(f"冲销凭证存在正数金额: 借{d} 贷{c}")
    total = sum(float(i.get("creditAmount") or 0) for i in items)
    if abs(total - (-2_000)) > 0.005:
        m.fail(f"冲销凭证贷方合计 {total} ≠ -2,000")
    # 冲销凭证应落在当前开放账期(2027-01)而非原凭证账期(2026-12)
    rev_term = f"{rev.get('voucherYear')}-{int(rev.get('voucherMonth')):02d}"
    if rev_term != term:
        m.fail(f"冲销凭证账期 {rev_term} 未落在开放账期 {term}")
    m.ok(f"冲销凭证 记-{rev.get('wordNum')} 金额全负(-2,000) 账期 {rev_term} sourceVoucherId 正确")

    rev_detail = m.ensure_voucher_posted(rev_id, "冲销凭证")

    # 重复冲销拦截
    body = m.req("POST", f"/api/voucher/reverse/{target_vid}", expect_ok=False)
    if body.get("code") == 0 or "重复" not in str(body.get("message") or ""):
        m.fail(f"重复冲销未被拦截: code={body.get('code')} message={body.get('message')!r}")
    m.ok("重复冲销拦截生效")

    # 未过账凭证冲销拦截
    bank = m.subject_by_code(subjects, "1002")
    cash = m.subject_by_code(subjects, "1001")
    draft = m.req("POST", "/api/voucher/draft",
                  json=m.make_voucher(book_id, term, [(bank, 100, 0), (cash, 0, 100)],
                                      "未过账凭证", subjects))["data"]
    body = m.req("POST", f"/api/voucher/reverse/{draft}", expect_ok=False)
    if body.get("code") == 0 or "已过账" not in str(body.get("message") or ""):
        m.fail(f"未过账凭证冲销未被拦截: code={body.get('code')} message={body.get('message')!r}")
    m.ok("未过账凭证冲销拦截生效")

    # 新账期科目余额: 5001 贷方净额 -2,000(仅冲销发生)
    sb = m.req("GET", f"/api/statement/subject-balance?periodType=month&reportDate={term}&showAll=true")["data"]
    row = next((r for r in sb if str(r.get("subjectCode") or r.get("code") or "") == "5001"), None)
    if row is None:
        m.fail("新账期科目余额表缺少 5001(冲销后应有发生额)")
    m.ok(f"新账期 5001 行: { {k: v for k, v in row.items() if 'mount' in str(k) or 'alance' in str(k)} }")


def main() -> None:
    print(f"=== jinbooks 年度验收(连续月结+年结+红字冲销) @ {m.BASE} ===")
    m.login()
    book_id = m.setup_book()
    m.create_reviewer(book_id)
    term = m.current_term()
    print(f"    当前账期: {term}")
    subjects = m.fetch_subjects(book_id)
    m.save_opening_balances(book_id)
    dec_term = multi_month_close(book_id, term, subjects)
    sale_vid = december_year_close(book_id, dec_term, subjects)
    new_term = m.current_term()
    new_year_opening_check(new_term)
    reversal_flow(book_id, new_term, subjects, sale_vid)
    print(f"\n=== 年度验收完成: {m.STEP_NO} 步全部通过 ===")


if __name__ == "__main__":
    main()
