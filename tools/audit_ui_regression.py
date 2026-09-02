#!/usr/bin/env python3
"""Audit UI regressions vs pre-encoding-fix commit a2685e7."""
from __future__ import annotations

import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
BASE = "a2685e7"
ROOT = "financial-cloud-ui"


def run(args: list[str]) -> str:
    r = subprocess.run(args, cwd=REPO, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout


def line_count_at(commit: str, rel: str) -> int | None:
    r = subprocess.run(
        ["git", "show", f"{commit}:{rel}"],
        cwd=REPO,
        capture_output=True,
    )
    if r.returncode != 0:
        return None
    return len(r.stdout.decode("utf-8", errors="replace").splitlines())


def main() -> None:
    # files changed between base and bad encoding commit
    changed = [
        line.strip().replace("\\", "/")
        for line in run(["git", "diff", "--name-only", BASE, "8baa601", "--", f"{ROOT}/src"]).splitlines()
        if line.strip()
    ]

    print(f"Files touched by encoding commit vs {BASE}: {len(changed)}\n")
    print(f"{'status':10} {'delta':>7} {'work':>6} {'base':>6} {'HEAD':>6}  path")
    print("-" * 100)

    regressions = []
    matches = []
    kept_intentional = []
    missing_in_base = []

    for rel in changed:
        work_path = REPO / rel
        base_n = line_count_at(BASE, rel)
        head_n = line_count_at("HEAD", rel)
        if not work_path.exists():
            print(f"{'MISSING':10} {'':>7} {'-':>6} {base_n or '-':>6} {head_n or '-':>6}  {rel}")
            continue
        work_n = len(work_path.read_text(encoding="utf-8", errors="replace").splitlines())
        if base_n is None:
            missing_in_base.append(rel)
            print(f"{'NEW':10} {'':>7} {work_n:>6} {'-':>6} {head_n or '-':>6}  {rel}")
            continue

        # content equal to base?
        base_blob = subprocess.run(
            ["git", "show", f"{BASE}:{rel}"], cwd=REPO, capture_output=True
        ).stdout.decode("utf-8", errors="replace").replace("\r\n", "\n")
        # normalize imports for comparison
        base_norm = base_blob.replace("@/utils/Jinbooks", "@/utils/financialCloud")
        work_norm = work_path.read_text(encoding="utf-8", errors="replace").replace("\r\n", "\n")
        # also normalize api system paths in base for fair compare
        for a, b in [
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
        ]:
            base_norm = base_norm.replace(a, b)

        equal = base_norm == work_norm
        delta = work_n - base_n
        if equal:
            status = "OK"
            matches.append(rel)
        elif abs(delta) >= 50 or (delta < -20):
            status = "REGRESS" if delta < 0 else "CHANGED"
            regressions.append((rel, work_n, base_n, delta))
        else:
            status = "DIFF"
            regressions.append((rel, work_n, base_n, delta))

        print(f"{status:10} {delta:>+7} {work_n:>6} {base_n:>6} {head_n or '-':>6}  {rel}")

    print("\n=== Summary ===")
    print(f"Exact match (after import normalize): {len(matches)}")
    print(f"Still differ from {BASE}: {len(regressions)}")
    for rel, w, b, d in sorted(regressions, key=lambda x: x[3]):
        print(f"  {d:+5}  {rel}  (work={w}, base={b})")

    # Also: any vue under src/views smaller than base by >30 lines?
    print("\n=== Broader views scan (work vs base line delta < -30) ===")
    r = subprocess.run(
        ["git", "ls-tree", "-r", "--name-only", BASE, f"{ROOT}/src/views"],
        cwd=REPO,
        capture_output=True,
        text=True,
    )
    broad = []
    for rel in r.stdout.splitlines():
        rel = rel.strip().replace("\\", "/")
        if not rel.endswith(".vue"):
            continue
        wp = REPO / rel
        if not wp.exists():
            continue
        bn = line_count_at(BASE, rel) or 0
        wn = len(wp.read_text(encoding="utf-8", errors="replace").splitlines())
        if wn - bn <= -30:
            broad.append((rel, wn, bn, wn - bn))
    for rel, w, b, d in sorted(broad, key=lambda x: x[3]):
        print(f"  {d:+5}  {rel}  (work={w}, base={b})")
    if not broad:
        print("  none")


if __name__ == "__main__":
    main()
