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

from release_evidence_provenance import (
    UNKNOWN, artifacts_changed, collect_artifact_identity, collect_declared_environment,
    collect_source_snapshot, sha256_file, source_changed, unknown_source, utc_now,
)

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "financial-cloud-ui"
GROUPS = [
    ("independent-reference", ["independent-accounting-reference.spec.ts"]),
    ("balance-sheet-golden", ["04-balance-sheet-golden-dataset.spec.ts"]),
    ("income-golden", ["05-income-statement-golden-dataset.spec.ts"]),
    ("year-end", ["carry-forward-year-end.spec.ts"]),
    ("asset-reference", ["fixed-asset-accounting-reference.spec.ts"]),
    ("payroll-reference", ["payroll-smb-regression.spec.ts"]),
    ("portable-backup", ["portable-backup.spec.ts"]),
    ("business-regression", ["arap.spec.ts", "arap-writeoff.spec.ts", "report-export.spec.ts", "journal.spec.ts", "zz-accounting-flow.spec.ts"]),
    ("guided-delivery", ["guided-accounting-delivery.spec.ts", "balance-sheet-integrity-ui.spec.ts"]),
]


def _save_summary(output: Path, summary: dict) -> None:
    # Replace the summary atomically so an interrupted write does not leave
    # apparently completed, truncated evidence behind.
    temporary = output / "summary.json.tmp"
    temporary.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(output / "summary.json")


def _source_snapshot(root: Path) -> dict:
    try:
        return collect_source_snapshot(root)
    except Exception:
        # Missing release provenance must not prevent isolated R&D acceptance.
        return unknown_source("Source collection failed")


def _reference(output: Path, path: Path) -> dict:
    try:
        return {"path": path.relative_to(output).as_posix(), "sha256": sha256_file(path)}
    except OSError:
        return {"path": path.relative_to(output).as_posix(), "sha256": UNKNOWN}


def _artifacts_snapshot(root: Path, env: dict) -> dict:
    try:
        return collect_artifact_identity(root, env)
    except Exception:
        return {
            "collectedAt": utc_now(),
            "backend": {"sha256": UNKNOWN, "path": UNKNOWN, "provenance": UNKNOWN},
            "frontend": {"kind": "production", "sha256": UNKNOWN, "path": UNKNOWN,
                         "provenance": UNKNOWN, "files": []},
            "reason": "Local artifact collection failed",
        }


def _report_result(report: Path) -> tuple[dict, list, bool]:
    """Require every result statistic explicitly; omitted failures are unknown."""
    try:
        data = json.loads(report.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}, ["Original Playwright JSON report is missing or invalid"], False
    if not isinstance(data, dict):
        return {}, ["Original Playwright JSON report must be an object"], False
    stats, errors = data.get("stats"), data.get("errors")
    if not isinstance(stats, dict):
        return {}, ["Required Playwright stats are missing or invalid"], False
    required = ("expected", "unexpected", "skipped", "flaky")
    invalid = [key for key in required if type(stats.get(key)) is not int or stats[key] < 0]
    if invalid:
        return stats, [f"Required Playwright statistics are missing or invalid: {', '.join(invalid)}"], False
    if not isinstance(errors, list):
        return stats, ["Required Playwright errors field is missing or invalid"], False
    valid = stats["expected"] > 0 and all(stats[key] == 0 for key in required[1:]) and not errors
    return stats, errors, valid


def main() -> int:
    env = os.environ.copy()
    if not env.get("FC_DB_NAME", "").startswith("financial_cloud_e2e_"):
        print("FC_DB_NAME must name an isolated financial_cloud_e2e_ database", file=sys.stderr)
        return 2
    if not env.get("E2E_API_URL") or not env.get("E2E_BASE_URL"):
        print("Set E2E_API_URL and E2E_BASE_URL explicitly for the isolated service", file=sys.stderr)
        return 2
    env.update(E2E_RESET_BOOK="1", E2E_ENABLE_UI="1", CI="1")
    run_id = str(uuid4())
    started_at = utc_now()
    output = ROOT / ".e2e-run" / "accounting-acceptance" / f"{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}-{run_id}"
    output.mkdir(parents=True, exist_ok=False)
    declared = collect_declared_environment(env)
    summary = {
        "schemaVersion": 1, "runId": run_id, "startedAt": started_at,
        "database": declared["database"], "api": declared["api"], "frontend": declared["frontend"],
        "declaredEnvironment": declared, "reviewedBinding": UNKNOWN,
        "sourceBefore": _source_snapshot(ROOT), "artifacts": _artifacts_snapshot(ROOT, env),
        "plannedGroups": [{"name": name, "specs": specs} for name, specs in GROUPS],
        "groups": [], "accountantSignOff": "pending", "passed": False,
        "releaseReady": False, "status": "running", "exitCode": None,
    }
    exit_code = 1
    try:
        _save_summary(output, summary)
        npx = shutil.which("npx.cmd" if os.name == "nt" else "npx")
        if not npx:
            summary["status"] = "startup_failed"
            summary["failure"] = {"type": "npx_unavailable", "reason": "npx is unavailable"}
            print("npx is unavailable", file=sys.stderr)
            exit_code = 2
            return exit_code
        for name, specs in GROUPS:
            print(f"Accounting acceptance: {name}", flush=True)
            report, log = output / f"{name}.json", output / f"{name}.log"
            group = {
                "name": name, "specs": specs, "startedAt": utc_now(),
                "exitCode": None, "stats": {}, "passed": False, "errors": [],
                "status": "running", "report": _reference(output, report), "log": _reference(output, log),
            }
            summary["groups"].append(group)
            _save_summary(output, summary)
            try:
                # Each group is sequential, uses this run's report path, and has
                # no retries. No prior directory can become current evidence.
                report_env = dict(env, PLAYWRIGHT_JSON_OUTPUT_NAME=str(report))
                result = subprocess.run([npx, "playwright", "test", *[f"e2e/{spec}" for spec in specs], "--reporter=json", "--retries=0"], cwd=UI, env=report_env, capture_output=True, text=True, encoding="utf-8", errors="replace")
                log.write_text(result.stdout + result.stderr, encoding="utf-8")
                stats, errors, valid = _report_result(report)
                passed = result.returncode == 0 and valid
                group.update(exitCode=result.returncode, stats=stats, passed=passed, errors=errors,
                             status="completed" if passed else "failed")
            except (OSError, subprocess.SubprocessError) as error:
                # Preserve report bytes if a launch partially produced them, but
                # never infer a process exit or success from those bytes.
                group.update(status="startup_failed", errors=[f"Acceptance process could not complete ({type(error).__name__})"])
                summary["status"] = "startup_failed"
                summary["failure"] = {"type": type(error).__name__, "group": name,
                                      "reason": "Acceptance process could not complete"}
                log.write_text(group["errors"][0] + "\n", encoding="utf-8")
                return exit_code
            finally:
                group["finishedAt"] = utc_now()
                group["report"] = _reference(output, report)
                group["log"] = _reference(output, log)
            print(f"  {'PASS' if group['passed'] else 'FAIL'}: {group['stats']}", flush=True)
            if not group["passed"]:
                summary["status"] = "failed"
                print(f"Evidence: {log}", file=sys.stderr)
                return exit_code
            _save_summary(output, summary)
        summary["passed"] = True
        summary["status"] = "completed"
        exit_code = 0
        print(f"Acceptance passed; accountant sign-off remains pending. Evidence: {output / 'summary.json'}")
        return exit_code
    except KeyboardInterrupt:
        summary["status"] = "interrupted"
        summary["failure"] = {"type": "KeyboardInterrupt", "reason": "Acceptance was interrupted"}
        exit_code = 130
        return exit_code
    except Exception as error:
        summary["status"] = "failed"
        summary["passed"] = False
        summary["failure"] = {"type": type(error).__name__, "reason": "Acceptance runner could not complete"}
        exit_code = 1
        return exit_code
    finally:
        for group in summary["groups"]:
            if group["status"] == "running":
                group["status"] = "interrupted" if summary["status"] == "interrupted" else "failed"
            group["report"] = _reference(output, output / f"{group['name']}.json")
            group["log"] = _reference(output, output / f"{group['name']}.log")
        summary["sourceAfter"] = _source_snapshot(ROOT)
        summary["sourceChanged"] = source_changed(summary["sourceBefore"], summary["sourceAfter"])
        summary["artifactsAfter"] = _artifacts_snapshot(ROOT, env)
        summary["artifactsChanged"] = artifacts_changed(summary["artifacts"], summary["artifactsAfter"])
        summary["finishedAt"] = utc_now()
        summary["exitCode"] = exit_code
        _save_summary(output, summary)


if __name__ == "__main__":
    raise SystemExit(main())
