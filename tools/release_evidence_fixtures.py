"""Generate explicitly synthetic, offline release-evidence examples.

The files exercise the documented v1 protocol. They are not CI exports, real
accountant approvals, deployed artifacts, or restore drill evidence. No service,
database, Git command, or acceptance runner is invoked.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


DEFAULT_PROFILE = Path(__file__).with_name("release_evidence_profile.json")
STARTED_AT = "2026-09-01T09:00:00Z"
FINISHED_AT = "2026-09-01T09:01:00Z"


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical_digest(value: object) -> str:
    data = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return sha256_bytes(data.encode("utf-8"))


def write_json(path: Path, value: object) -> dict:
    """Write a new fixture file and return its reference; never overwrite it."""
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
    with path.open("xb") as handle:
        handle.write(raw)
    return {"path": path.name, "sha256": sha256_bytes(raw)}


def playwright_report(cases: list[dict]) -> dict:
    """Represent every trusted case as one completed first-attempt result."""
    suites: list[dict] = []
    files: dict[str, dict] = {}
    for case in cases:
        filename = case["file"]
        if filename not in files:
            outer = {"title": filename, "file": filename, "line": 0, "column": 0,
                     "suites": [], "specs": []}
            files[filename] = outer
            suites.append(outer)
        suite = files[filename]
        for title in case["titlePath"][:-1]:
            nested = next((item for item in suite["suites"] if item["title"] == title), None)
            if nested is None:
                nested = {"title": title, "file": filename, "line": 1, "column": 1,
                          "suites": [], "specs": []}
                suite["suites"].append(nested)
            suite = nested
        suite["specs"].append({
            "title": case["titlePath"][-1], "file": filename, "line": 1, "column": 1,
            "ok": True, "tags": [],
            "tests": [{"projectName": case["project"], "projectId": case["project"],
                       "expectedStatus": "passed", "status": "expected",
                       "results": [{"status": "passed", "retry": 0,
                                    "workerIndex": 0, "parallelIndex": 0,
                                    "startTime": STARTED_AT, "duration": 100,
                                    "errors": [], "attachments": [], "stdout": [], "stderr": []}]}],
        })
    return {
        "config": {"retries": 0, "projects": [{"name": "chromium"}]},
        "metadata": {"synthetic": True, "warning": "Synthetic fixture; no tests were executed"},
        "suites": suites, "errors": [],
        "stats": {"startTime": STARTED_AT, "duration": 60000,
                  "expected": len(cases), "unexpected": 0, "flaky": 0, "skipped": 0},
    }


def create_fixture(output: Path, *, missing: bool = False,
                   profile_path: Path = DEFAULT_PROFILE) -> Path:
    """Create a complete or missing-items fixture in a new/empty directory."""
    output = Path(output)
    if output.exists() and (not output.is_dir() or any(output.iterdir())):
        raise ValueError("Fixture output must be a new or empty directory; existing files are protected")
    profile_raw = Path(profile_path).read_bytes()
    profile = json.loads(profile_raw.decode("utf-8"))
    output.mkdir(parents=True, exist_ok=True)

    def record(relative: str, value: object) -> dict:
        ref = write_json(output / relative, value)
        ref["path"] = relative
        return ref

    def support(relative: str, description: str) -> dict:
        path = output / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        raw = ("SYNTHETIC FIXTURE ONLY\n" + description + "\nNo actual observation or signature occurred.\n").encode("utf-8")
        with path.open("xb") as handle:
            handle.write(raw)
        return {"path": relative, "sha256": sha256_bytes(raw)}

    frontend_files = [
        {"path": "assets/app.js", "size": len(b"synthetic production javascript"),
         "sha256": sha256_bytes(b"synthetic production javascript")},
        {"path": "index.html", "size": len(b"synthetic production html"),
         "sha256": sha256_bytes(b"synthetic production html")},
    ]
    frontend_sha = canonical_digest(frontend_files)
    source_files = [{"path": "synthetic_source.py", "tracked": True, "type": "file",
                     "size": len(b"synthetic source"), "sha256": sha256_bytes(b"synthetic source")}]
    source_fingerprint = canonical_digest(source_files)
    candidate = {
        "candidateId": "SYNTHETIC-jinbooks-release-example",
        "commitSha": "1234567890abcdef1234567890abcdef12345678",
        "source": {"state": "clean", "fingerprint": source_fingerprint},
        "artifacts": {
            "backend": {"sha256": sha256_bytes(b"synthetic backend jar")},
            "frontend": {"kind": "production", "sha256": frontend_sha,
                         "manifest": record("artifacts/frontend.json", {
                             "kind": "production", "files": frontend_files, "sha256": frontend_sha})},
        },
    }
    identity = {
        "candidateId": candidate["candidateId"], "commitSha": candidate["commitSha"],
        "sourceFingerprint": candidate["source"]["fingerprint"],
        "backendSha256": candidate["artifacts"]["backend"]["sha256"],
        "frontendSha256": frontend_sha,
    }
    review = {"reviewer": "Synthetic technical reviewer", "reviewedAt": FINISHED_AT,
              "conclusion": "approved"}
    environment = {
        "synthetic": True, "environmentId": "synthetic-production-acceptance",
        "identity": identity, "instanceId": "synthetic-backend-source",
        "apiUrl": "https://synthetic.example.invalid/api",
        "frontendUrl": "https://synthetic.example.invalid", "frontendKind": "production",
        "datasource": {"host": "synthetic-source.invalid", "port": 3307,
                       "database": "financial_cloud_e2e_synthetic", "instanceId": "synthetic-db-source"},
        "observation": {"method": "deployment_and_datasource_inspection", "observedAt": FINISHED_AT},
        "review": review,
        "supportingEvidence": [support("support/runtime.txt", "Synthetic deployment and datasource correspondence")],
    }
    acceptance = []
    for group in profile["groups"]:
        report = record("runs/" + group["name"] + "/report.json", playwright_report(group["cases"]))
        source = {"commitSha": candidate["commitSha"], "state": "clean", "fingerprint": source_fingerprint,
                  "collectedAt": STARTED_AT, "status": "collected", "collectionStable": True,
                  "gitStatus": {"tracked": [], "untracked": []}, "files": source_files}
        artifacts = {"backend": {"sha256": identity["backendSha256"]},
                     "frontend": {"kind": "production", "sha256": frontend_sha, "files": frontend_files}}
        summary = {
            "schemaVersion": 1, "synthetic": True, "runId": "synthetic-" + group["name"],
            "startedAt": STARTED_AT, "finishedAt": FINISHED_AT, "status": "completed", "exitCode": 0,
            "sourceBefore": source, "sourceAfter": dict(source, collectedAt=FINISHED_AT),
            "sourceChanged": False,
            "api": environment["apiUrl"], "frontend": environment["frontendUrl"],
            "database": environment["datasource"]["database"],
            "declaredEnvironment": {
                "provenance": "declared", "api": environment["apiUrl"],
                "frontend": environment["frontendUrl"], "database": environment["datasource"]["database"],
                "databaseHost": environment["datasource"]["host"],
                "databasePort": environment["datasource"]["port"], "componentFrontend": "unknown"},
            "artifacts": artifacts, "artifactsAfter": artifacts, "artifactsChanged": False,
            "groups": [{"name": group["name"], "exitCode": 0, "status": "completed",
                        "startedAt": STARTED_AT, "finishedAt": FINISHED_AT,
                        "report": {"path": "report.json", "sha256": report["sha256"]}}],
            "releaseReady": False,
        }
        acceptance.append({
            "group": group["name"], "identity": identity,
            "runId": "synthetic-" + group["name"], "startedAt": STARTED_AT,
            "finishedAt": FINISHED_AT, "environmentId": environment["environmentId"],
            "exitCode": 0, "report": report,
            "provenance": record("runs/" + group["name"] + "/summary.json", summary),
        })
    environment["runs"] = [{"runId": run["runId"], "provenance": run["provenance"]} for run in acceptance]
    ci = {
        "synthetic": True, "identity": identity, "provider": "Synthetic CI",
        "runId": "synthetic-complete-ci", "url": "https://synthetic.example.invalid/ci/1",
        "headSha": candidate["commitSha"], "workflow": profile["ci"]["workflow"],
        "conclusion": "success", "review": review,
        "jobs": [{"name": job, "conclusion": "success", "steps": [
            {"name": step, "conclusion": "success"}
            for step in profile["ci"]["requiredSteps"][job]]}
            for job in profile["ci"]["requiredJobs"]],
        "supportingEvidence": [support("support/ci-export.txt", "Synthetic full CI export")],
    }
    accountant = {
        "synthetic": True, "identity": identity, "month": "2026-08",
        "realMonth": True, "completeMonth": True, "review": review,
        "comparisons": [{"item": item, "outcome": "matched", "accountantApproved": True}
                        for item in profile["requiredAccountingComparisons"]],
        "signoffs": [{"role": role, "name": "Synthetic " + role,
                     "signedAt": FINISHED_AT,
                     "evidence": support("support/signoff-" + role + ".txt", "Synthetic " + role + " signature")}
                    for role in ("accountant", "operator", "technical")],
        "supportingEvidence": [support("support/month-comparison.txt", "Synthetic complete month comparison")],
    }
    restore = {
        "synthetic": True, "identity": identity, "review": review,
        "source": {"instanceId": "synthetic-backend-source", "datasourceId": "synthetic-db-source"},
        "target": {"instanceId": "synthetic-backend-target", "datasourceId": "synthetic-db-target"},
        "backup": support("support/portable-backup.fixture", "Synthetic backup bytes; never restore this file"),
        "comparisons": [{"item": item, "outcome": "matched"}
                        for item in profile["requiredRestoreComparisons"]],
        "supportingEvidence": [support("support/restore-comparison.txt", "Synthetic independent-instance restore comparison")],
    }
    issues = {
        "synthetic": True, "identity": identity, "review": review,
        "issues": [{"id": "SYNTHETIC-NONBLOCKING", "releaseBlocking": False,
                    "status": "open", "reviewed": True, "owner": "Synthetic operator",
                    "resolutionPlan": "Synthetic wording refinement after release"}],
        "differences": [],
        "supportingEvidence": [support("support/issue-ledger.txt", "Synthetic reviewed issue ledger")],
    }
    evidence = {"acceptance": acceptance,
                "environments": [record("environments/production.json", environment)],
                "ci": record("external/ci.json", ci),
                "accountant": record("external/accountant.json", accountant),
                "restore": record("external/restore.json", restore),
                "issueReview": record("external/issues.json", issues)}
    if missing:
        for key in ("ci", "accountant", "restore"):
            del evidence[key]
    bundle = {
        "schemaVersion": 1, "synthetic": True, "candidate": candidate,
        "profile": {"id": profile["id"], "version": profile["version"],
                    "sha256": sha256_bytes(profile_raw)},
        "evidence": evidence,
    }
    record("candidate.json", bundle)
    support("SYNTHETIC-README.txt", "This bundle tests the verifier only and cannot establish real release readiness")
    return output / "candidate.json"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path, help="New or empty output directory")
    parser.add_argument("--missing", action="store_true", help="Omit CI, accountant and restore references")
    args = parser.parse_args(argv)
    try:
        bundle = create_fixture(args.output, missing=args.missing)
    except (OSError, ValueError, KeyError) as exc:
        parser.error(str(exc))
    print(json.dumps({"synthetic": True, "bundle": str(bundle),
                      "warning": "Fixture only; no real release evidence was collected"}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
