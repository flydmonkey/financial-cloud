"""Run accountant reference and supplementary regression in an isolated database."""
from __future__ import annotations

import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
from uuid import uuid4
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "financial-cloud-ui"
GROUPS = [
    ("independent-reference", ["independent-accounting-reference.spec.ts"]),
    ("balance-sheet-golden", ["04-balance-sheet-golden-dataset.spec.ts"]),
    ("income-golden", ["05-income-statement-golden-dataset.spec.ts"]),
    ("year-end", ["carry-forward-year-end.spec.ts"]),
    ("asset-reference", ["fixed-asset-accounting-reference.spec.ts"]),
    ("business-regression", ["arap.spec.ts", "arap-writeoff.spec.ts", "payroll-smb-regression.spec.ts", "report-export.spec.ts", "journal.spec.ts", "zz-accounting-flow.spec.ts"]),
    ("guided-delivery", ["guided-accounting-delivery.spec.ts", "balance-sheet-integrity-ui.spec.ts"]),
]


def main() -> int:
    env = os.environ.copy()
    if not env.get("FC_DB_NAME", "").startswith("financial_cloud_e2e_"):
        print("FC_DB_NAME must name an isolated financial_cloud_e2e_ database", file=sys.stderr)
        return 2
    if not env.get("E2E_API_URL") or not env.get("E2E_BASE_URL"):
        print("Set E2E_API_URL and E2E_BASE_URL explicitly for the isolated service", file=sys.stderr)
        return 2
    npx = shutil.which("npx.cmd" if os.name == "nt" else "npx")
    if not npx:
        print("npx is unavailable", file=sys.stderr)
        return 2
    env.update(E2E_RESET_BOOK="1", E2E_ENABLE_UI="1", CI="1")
    output = ROOT / ".e2e-run" / "accounting-acceptance" / f"{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}-{uuid4().hex[:8]}"
    output.mkdir(parents=True, exist_ok=True)
    summary = {"startedAt": datetime.now(timezone.utc).isoformat(), "database": env["FC_DB_NAME"], "api": env["E2E_API_URL"], "frontend": env["E2E_BASE_URL"], "groups": [], "accountantSignOff": "pending", "passed": False}
    for name, specs in GROUPS:
        print(f"Accounting acceptance: {name}", flush=True)
        report = output / f"{name}.json"
        # Give each invocation a unique report destination; never read prior run evidence.
        report_env = dict(env, PLAYWRIGHT_JSON_OUTPUT_NAME=str(report))
        result = subprocess.run([npx, "playwright", "test", *[f"e2e/{spec}" for spec in specs], "--reporter=json", "--retries=0"], cwd=UI, env=report_env, capture_output=True, text=True, encoding="utf-8", errors="replace")
        (output / f"{name}.log").write_text(result.stdout + result.stderr, encoding="utf-8")
        stats = {}
        errors = []
        try:
            # Report must have been produced by this process; fail startup errors even if old file exists.
            if report.exists():
                data = json.loads(report.read_text(encoding="utf-8"))
                stats = data.get("stats", {})
                errors = data.get("errors", [])
        except (OSError, ValueError) as error:
            errors = [str(error)]
        passed = result.returncode == 0 and stats.get("expected", 0) > 0 and stats.get("unexpected", 0) == 0 and stats.get("skipped", 0) == 0 and stats.get("flaky", 0) == 0 and not errors
        summary["groups"].append({"name": name, "specs": specs, "exitCode": result.returncode, "stats": stats, "passed": passed, "errors": errors})
        (output / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"  {'PASS' if passed else 'FAIL'}: {stats}", flush=True)
        if not passed:
            print(f"Evidence: {output / (name + '.log')}", file=sys.stderr)
            return 1
    summary["passed"] = True
    summary["finishedAt"] = datetime.now(timezone.utc).isoformat()
    (output / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Acceptance passed; accountant sign-off remains pending. Evidence: {output / 'summary.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
