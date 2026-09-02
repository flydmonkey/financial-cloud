#!/usr/bin/env python3
"""Restore UI feature files from a2685e7 that were wrongly rolled back during encoding repair."""
from __future__ import annotations

import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
COMMIT = "a2685e7"

# Intentionally keep current versions (already fixed UTF-8 / utils rename)
SKIP = {
    "financial-cloud-ui/src/layout/components/Navbar.vue",
    "financial-cloud-ui/src/views/login.vue",
    "financial-cloud-ui/src/views/dashboard/accounting/added_tax.vue",
    "financial-cloud-ui/src/views/dashboard/accounting/receivable.vue",
    "financial-cloud-ui/src/views/dashboard/accounting/net_profit.vue",
    "financial-cloud-ui/src/utils/financialCloud.ts",
    "financial-cloud-ui/src/utils/Request.ts",
    "financial-cloud-ui/src/main.ts",
    "financial-cloud-ui/src/components/HeaderSearch/index.vue",
    "financial-cloud-ui/src/layout/components/Sidebar/SidebarItem.vue",
    "financial-cloud-ui/src/layout/components/TagsView/index.vue",
    "financial-cloud-ui/src/api/menu.ts",
    "financial-cloud-ui/src/plugins/download.ts",
}

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
]


def git_files_changed() -> list[str]:
    r = subprocess.run(
        ["git", "diff", "--name-only", COMMIT, "8baa601", "--", "financial-cloud-ui/src"],
        cwd=REPO,
        capture_output=True,
        text=True,
        check=True,
    )
    return [line.strip().replace("\\", "/") for line in r.stdout.splitlines() if line.strip()]


def restore(rel: str) -> None:
    r = subprocess.run(
        ["git", "show", f"{COMMIT}:{rel}"],
        cwd=REPO,
        capture_output=True,
        check=False,
    )
    if r.returncode != 0:
        print(f"SKIP missing in {COMMIT}: {rel}")
        return
    text = r.stdout.decode("utf-8", errors="replace").replace("\r\n", "\n")
    for old, new in IMPORT_REPS:
        text = text.replace(old, new)
    path = REPO / rel
    path.write_text(text, encoding="utf-8", newline="\n")
    print(f"restored {rel} ({len(text.splitlines())} lines)")


def main() -> None:
    for rel in git_files_changed():
        if rel in SKIP:
            print(f"keep current {rel}")
            continue
        if not (rel.endswith(".vue") or rel.endswith(".ts")):
            continue
        restore(rel)


if __name__ == "__main__":
    main()
