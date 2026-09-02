#!/usr/bin/env python3
"""Fully restore remaining UI regressions from a2685e7 and fix import paths."""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
BASE = "a2685e7"

TARGETS = [
    "financial-cloud-ui/src/layout/components/Navbar.vue",
    "financial-cloud-ui/src/layout/components/TagsView/index.vue",
    "financial-cloud-ui/src/api/menu.ts",
    "financial-cloud-ui/src/utils/Request.ts",
    "financial-cloud-ui/src/views/login.vue",
    "financial-cloud-ui/src/views/dashboard/accounting/added_tax.vue",
    "financial-cloud-ui/src/views/dashboard/accounting/receivable.vue",
    "financial-cloud-ui/src/views/dashboard/accounting/net_profit.vue",
]

IMPORT_REPS = [
    ("@/utils/Jinbooks", "@/utils/financialCloud"),
    ("@/api/system/standard/standard-subject", "@/api/standard/standard-subject"),
    ("@/api/system/standard/standard-statement-income", "@/api/standard/standard-statement-income"),
    ("@/api/system/standard/standard", "@/api/standard/standard"),
    ("@/api/system/voucher/voucher-template", "@/api/voucher/voucher-template"),
    ("@/api/system/voucher/voucher", "@/api/voucher/voucher"),
    ("@/api/system/statement/statement-income-config", "@/api/statement/statement-income-config"),
    ("@/api/system/statement/statement-income", "@/api/statement/statement-income"),
    ("@/api/system/statement/statement-cash-flow", "@/api/statement/statement-cash-flow"),
    ("@/api/system/statement/statement-config", "@/api/statement/statement-config"),
    ("@/api/system/statement/statement", "@/api/statement/statement"),
    ("@/api/system/book/book-subject", "@/api/book/book-subject"),
    ("@/api/system/book/settlement", "@/api/book/settlement"),
    ("@/api/system/hr/employee", "@/api/hr/employee"),
    ("@/api/system/dept", "@/api/idm/dept"),
    ("@/api/system/group.js", "@/api/idm/group"),
    ("@/api/system/user", "@/api/idm/user"),
]


def restore(rel: str) -> str:
    r = subprocess.run(["git", "show", f"{BASE}:{rel}"], cwd=REPO, capture_output=True, check=True)
    text = r.stdout.decode("utf-8", errors="replace").replace("\r\n", "\n")
    for old, new in IMPORT_REPS:
        text = text.replace(old, new)
    return text


def fix_common_corruption(text: str) -> str:
    """Fix known truncated UTF-8 sequences that break Vue attrs/strings."""
    reps = [
        # Navbar / common
        ("账套�?/span>", "账套：</span>"),
        ("退出登�?/span>", "退出登录</span>"),
        ("确定注销并退出系统吗�?", "确定注销并退出系统吗？"),
        ("{y}年{m}�?", "{y}年{m}月"),
        ("yyyyMM[0]+'�?+yyyyMM[1]+'�?", "yyyyMM[0]+'年'+yyyyMM[1]+'月'"),
        ("yyyyMM[0]+'\ufffd?'+yyyyMM[1]+'\ufffd?'", "yyyyMM[0]+'年'+yyyyMM[1]+'月'"),
        # generic truncated closers often seen after restore
        ("开始期�?", "开始期间"),
        ("YYYY年MM�?", "YYYY年MM期"),
        ("反审�?", "反审核"),
        ("反过�?", "反过账"),
        ("净利润�?/span>", "净利润率</span>"),
        ("增值税及附?/span>", "增值税及附加</span>"),
    ]
    for a, b in reps:
        text = text.replace(a, b)

    # Fix common pattern: broken year/month join with replacement chars
    text = re.sub(
        r"yyyyMM\[0\]\s*\+\s*['\"][^'\"]*['\"]\s*\+\s*yyyyMM\[1\]\s*\+\s*['\"][^'\"]*['\"]",
        "yyyyMM[0] + '年' + yyyyMM[1] + '月'",
        text,
    )
    return text


def fix_login_css(text: str) -> str:
    """Ensure login form border wasn't swallowed by broken comments."""
    bad = "background-color: rgba(255, 255, 255, 0.7); //"
    if bad in text and "border: 1px solid #eaeaea" not in text.split(".login-form", 1)[-1][:400]:
        text = text.replace(
            "background-color: rgba(255, 255, 255, 0.7); //",
            "background-color: rgba(255, 255, 255, 0.7);\n  border: 1px solid #eaeaea; //",
            1,
        )
    # if comment-smashed border exists, split it
    text = re.sub(
        r"background-color: rgba\(255, 255, 255, 0\.7\); //[^\n]*border: 1px solid #eaeaea;[^\n]*",
        "background-color: rgba(255, 255, 255, 0.7);\n  border: 1px solid #eaeaea;",
        text,
    )
    return text


def main() -> None:
    for rel in TARGETS:
        text = restore(rel)
        text = fix_common_corruption(text)
        if rel.endswith("login.vue"):
            text = fix_login_css(text)
        path = REPO / rel
        path.write_text(text, encoding="utf-8", newline="\n")
        print(f"restored {rel} ({len(text.splitlines())} lines)")


if __name__ == "__main__":
    main()
