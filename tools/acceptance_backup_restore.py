#!/usr/bin/env python3
"""jinbooks 账套备份/恢复验收：先跑月度全流程造数 → 导出备份 ZIP → 克隆恢复为新账套 → 关键指纹逐项比对。

用法:
    FC_API=http://localhost:2154 python tools/acceptance_backup_restore.py

前置: 后端已启动。脚本会先清账套(clear_books)再跑 acceptance_month_flow 造数。
"""
from __future__ import annotations

import io
import os
import subprocess
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import requests

import acceptance_month_flow as m

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


def build_source_book() -> tuple[str, str]:
    """清账套并跑月度全流程，返回 (bookId, 已结账期间)。"""
    m.step("造数: 清账套 + 跑月度全流程验收(13步)")
    env = {**os.environ, "FC_API": m.BASE}
    env.setdefault("FC_DB_HOST", "192.168.137.121")
    env.setdefault("FC_DB_PORT", "3307")
    r = subprocess.run([sys.executable, os.path.join(SCRIPT_DIR, "clear_books.py")],
                       env=env, capture_output=True)
    if r.returncode != 0:
        m.fail(f"clear_books 失败: {r.stdout.decode('utf-8', 'replace')[-300:]}")
    r = subprocess.run([sys.executable, os.path.join(SCRIPT_DIR, "acceptance_month_flow.py")],
                       env=env, capture_output=True)
    out = r.stdout.decode("utf-8", "replace")
    if r.returncode != 0 or "全部通过" not in out:
        m.fail(f"月度全流程造数失败:\n{out[-1500:]}")
    m.ok("月度全流程 13 步通过，数据就绪")
    m.login()
    books = m.req("GET", "/api/book/fetch?pageNumber=1&pageSize=10")["data"]
    records = books.get("records") or []
    if len(records) != 1:
        m.fail(f"账套数量 {len(records)} ≠ 1")
    book_id = str(records[0]["id"])
    m.req("GET", f"/api/users/switchBook/{book_id}")
    return book_id, "2026-09"


def fingerprint(label: str) -> dict:
    """采集当前账套关键指标指纹。"""
    fp: dict = {}
    term = m.current_term()
    fp["current_term"] = term

    vouchers = m.req("GET", "/api/voucher/fetch?pageNumber=1&pageSize=200")["data"]
    rows = vouchers.get("records") or []
    fp["voucher_count"] = vouchers.get("total") or len(rows)
    fp["voucher_debit_sum"] = round(sum(float(v.get("debitAmount") or 0) for v in rows), 2)

    sb = m.req("GET", f"/api/statement/subject-balance?periodType=month&reportDate=2026-09&showAll=true")["data"]
    fp["subject_balance_rows"] = len(sb)

    inc = m.req("GET", "/api/statement/income?periodType=month&reportDate=2026-09")["data"]
    inc_rows = inc.get("items") or inc.get("rows") or []
    for r in inc_rows:
        name = r.get("itemName") or r.get("name") or ""
        if "净利润" in name:
            fp["net_profit"] = float(r.get("currentBalance") if r.get("currentBalance") is not None
                                     else r.get("closingBalance") or 0)
        if "营业收入" in name:
            fp["revenue"] = float(r.get("currentBalance") if r.get("currentBalance") is not None
                                  else r.get("closingBalance") or 0)

    bs = m.req("GET", "/api/statement/balance-sheet?periodType=month&reportDate=2026-09")["data"]
    for r in (bs.get("items") or {}).get("assets") or []:
        if "总计" in (r.get("itemName") or r.get("name") or ""):
            fp["asset_total"] = float(r.get("currentBalance") if r.get("currentBalance") is not None
                                      else r.get("closingBalance") or 0)

    wo = m.req("GET", "/api/arap/writeoff/list?side=AR")["data"] or []
    fp["arap_writeoffs"] = len(wo)

    depr = m.req("GET", "/api/fixed-asset/report/depreciation-detail?yearPeriod=2026-09")["data"]
    fp["fa_depr_rows"] = len(depr) if isinstance(depr, list) else -1

    salary = m.req("GET", "/api/employee/salary/fetch?pageNumber=1&pageSize=50")["data"]
    fp["salary_rows"] = salary.get("total") or len(salary.get("records") or [])

    emps = m.req("GET", "/api/salary/employee/fetch?pageNumber=1&pageSize=50")["data"]
    fp["employee_rows"] = emps.get("total") or len(emps.get("records") or [])

    accs = m.req("GET", "/api/journal/account/fetch?pageNumber=1&pageSize=50")["data"]
    fp["journal_accounts"] = accs.get("total") or len(accs.get("records") or [])

    assists = m.req("GET", "/api/base/assist-acc/fetch?pageNumber=1&pageSize=50")["data"]
    fp["assist_accs"] = assists.get("total") or len(assists.get("records") or [])

    print(f"    指纹[{label}]: {fp}")
    return fp


def main() -> None:
    print(f"=== jinbooks 账套备份/恢复验收 @ {m.BASE} ===")
    book_id, closed_term = build_source_book()

    m.step("采集源账套指纹")
    src_fp = fingerprint("源账套")

    m.step("导出备份 ZIP")
    resp = requests.post(f"{m.BASE}/api/book/backup/export", params={"bookId": book_id},
                         headers=m.HEADERS, timeout=60)
    if resp.status_code != 200 or len(resp.content) < 1_000:
        m.fail(f"备份导出异常: HTTP {resp.status_code} 大小 {len(resp.content)} body={resp.text[:200]}")
    if not resp.content[:2] == b"PK":
        m.fail("备份导出不是有效 ZIP")
    zf = zipfile.ZipFile(io.BytesIO(resp.content))
    names = zf.namelist()
    m.ok(f"备份 ZIP {len(resp.content):,} 字节, {len(names)} 个条目")

    m.step("恢复为新账套")
    body = m.req("POST", "/api/book/backup/restore",
                 files={"file": ("backup.zip", resp.content, "application/zip")})
    result = body["data"] or {}
    new_book_id = result.get("bookId")
    if not new_book_id or str(new_book_id) == str(book_id):
        m.fail(f"恢复结果异常: {result}")
    m.ok(f"恢复成功: 新账套 {new_book_id} 名称={result.get('name')} tables={result.get('tables')} rows={result.get('rows')}")

    m.step("采集恢复账套指纹并比对")
    m.req("GET", f"/api/users/switchBook/{new_book_id}")
    new_fp = fingerprint("恢复账套")
    mismatches = [f"{k}: 源={src_fp.get(k)} 恢复={new_fp.get(k)}"
                  for k in src_fp if src_fp.get(k) != new_fp.get(k)]
    if mismatches:
        m.fail("指纹不一致: " + "; ".join(mismatches))
    m.ok(f"指纹 {len(src_fp)} 项全部一致")

    m.step("恢复账套可用性: 新账期录凭证并过账")
    subjects = m.fetch_subjects(new_book_id)
    m.create_reviewer(new_book_id)
    new_term = m.current_term()
    bank = m.subject_by_code(subjects, "1002")
    revenue = m.subject_by_code(subjects, "5001")
    m.voucher_full_flow(
        m.make_voucher(new_book_id, new_term, [(bank, 100, 0), (revenue, 0, 100)],
                       "恢复账套测试凭证", subjects), "恢复账套新凭证")
    m.ok("恢复账套可正常录凭证/审核/过账")

    print(f"\n=== 备份/恢复验收完成: {m.STEP_NO} 步全部通过 ===")


if __name__ == "__main__":
    main()
