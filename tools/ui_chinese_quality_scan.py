#!/usr/bin/env python3
"""Deeper UI Chinese quality scan: dashboard + layout + restored views."""
from __future__ import annotations

import re
from pathlib import Path

SRC = Path(__file__).resolve().parents[1] / "financial-cloud-ui" / "src"

# Files users see most / previously corrupted
FOCUS = [
    "layout/components/Navbar.vue",
    "views/login.vue",
    "views/index.vue",
    "views/dashboard/accounting/added_tax.vue",
    "views/dashboard/accounting/receivable.vue",
    "views/dashboard/accounting/net_profit.vue",
    "views/dashboard/accounting/fund_balance.vue",
    "views/dashboard/accounting/cost.vue",
    "views/dashboard/accounting/revenue_cost.vue",
    "views/dashboard/accounting/expected_available_funds.vue",
    "views/dashboard/accounting/other_subjects.vue",
    "views/voucher/voucher-edit.vue",
    "views/voucher/voucher-index.vue",
    "views/voucher/voucher-template.vue",
    "views/voucher/sub-ledger.vue",
    "views/statement/balance-sheet.vue",
    "views/statement/income-statement.vue",
    "views/statement/cash-flow-statement.vue",
    "views/statement/expense-detail.vue",
    "views/statement/general-ledger.vue",
    "views/statement/subject-balance.vue",
    "views/statement/voucher-summary.vue",
    "views/books/subject.vue",
    "views/config/initBalance/index.vue",
    "views/hr/employee.vue",
    "views/settlement/settle-list.vue",
]

CJK = re.compile(r"[\u4e00-\u9fff]")
QMARK = re.compile(r"\?{2,}")
# common double-encoded leftovers
WEIRD = re.compile(r"[\u0080-\u009f]|Ã.|Â.|ä¸.|å.|æ.|ç.|è.|é.|ï¼.|â.|ďź.")


def analyze(rel: str) -> dict:
    path = SRC / rel
    if not path.exists():
        return {"file": rel, "missing": True}
    text = path.read_text(encoding="utf-8", errors="replace")
    template = ""
    m = re.search(r"<template>(.*)</template>", text, re.S)
    if m:
        template = m.group(1)
    cjk_count = len(CJK.findall(text))
    tmpl_cjk = len(CJK.findall(template))
    qmarks = [(i, l.strip()[:100]) for i, l in enumerate(text.splitlines(), 1) if QMARK.search(l)]
    weird = [(i, l.strip()[:100]) for i, l in enumerate(text.splitlines(), 1) if WEIRD.search(l)]
    # suspicious: vue template with almost no CJK (except pure chart wrappers)
    low_cjk = path.suffix == ".vue" and tmpl_cjk < 5 and "dashboard" in rel.replace("\\", "/")
    return {
        "file": rel,
        "missing": False,
        "cjk_total": cjk_count,
        "cjk_template": tmpl_cjk,
        "qmark_lines": len(qmarks),
        "weird_lines": len(weird),
        "low_template_cjk": low_cjk,
        "qmark_samples": qmarks[:5],
        "weird_samples": weird[:8],
    }


def scan_all_vue_for_weird() -> list[dict]:
    hits = []
    for path in sorted(SRC.rglob("*.vue")):
        text = path.read_text(encoding="utf-8", errors="replace")
        weird_lines = []
        for i, line in enumerate(text.splitlines(), 1):
            if WEIRD.search(line) or QMARK.search(line):
                # skip pure comments? still report
                weird_lines.append((i, line.strip()[:100]))
        if weird_lines:
            hits.append({
                "file": str(path.relative_to(SRC)).replace("\\", "/"),
                "count": len(weird_lines),
                "samples": weird_lines[:6],
            })
    return hits


def main() -> None:
    print("=== Focus file quality ===")
    problems = []
    for rel in FOCUS:
        info = analyze(rel)
        if info.get("missing"):
            print(f"MISSING {rel}")
            problems.append(rel)
            continue
        flags = []
        if info["qmark_lines"]:
            flags.append(f"qmarks={info['qmark_lines']}")
        if info["weird_lines"]:
            flags.append(f"weird={info['weird_lines']}")
        if info["low_template_cjk"]:
            flags.append("low_cjk")
        status = "OK" if not flags else "WARN " + ",".join(flags)
        print(f"{status:30} {rel}  (tmpl_cjk={info['cjk_template']}, total_cjk={info['cjk_total']})")
        if flags:
            problems.append(rel)
            for s in info["qmark_samples"] + info["weird_samples"]:
                print(f"    L{s[0]}: {s[1]}")

    print("\n=== All Vue files with qmarks/weird ===")
    all_hits = scan_all_vue_for_weird()
    print(f"count={len(all_hits)}")
    for h in all_hits:
        print(f"  {h['file']} ({h['count']})")
        for ln, prev in h["samples"]:
            print(f"    L{ln}: {prev}")

    print(f"\nFocus problems: {len(problems)}")


if __name__ == "__main__":
    main()
