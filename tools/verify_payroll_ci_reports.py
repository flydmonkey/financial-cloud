"""Prepare/verify the current payroll IT report without running SQL or Maven."""
from __future__ import annotations

import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import re
import secrets
import sys
import xml.etree.ElementTree as ET

from verification_ci_evidence import ROOT, artifact, identity, same_identity, utc_now, write_json
from init_payroll_it_schema import build_schema_plan, NAMESPACE, SCOPE_COLUMNS, SCOPE_INDEX

CLASS = "com.financial.cloud.service.hr.PayrollTransactionMysqlIT"
REPORT = "TEST-" + CLASS + ".xml"
COMMAND = ["./mvnw", "-B", "test", "-Dtest=PayrollTransactionMysqlIT"]
EXPECTED_CASES = {
    "actualSqlLinkExceptionRollsBackHeaderItemsWordAndSalaryState",
    "generateWithOlderOuterRepeatableReadSnapshotSeesCommittedLivePeerAfterBookWait",
    "anotherBookCanCommitWhileFirstBookGenerationIsPaused",
    "oppositeBatchInputOrderUsesSameBookLockAndHasNoPartialMutation",
    "generateWaitsForUpdateAndUsesCommittedAmounts",
    "anotherBookPreviewCanReplaceSalaryWhileFirstBookGenerationIsPaused",
    "actualZeroRowLinkUpdateRollsBackHeaderItemsWordAndSalaryMutation",
    "unlinkWithOlderOuterRepeatableReadSnapshotDeletesNewVoucherAndItemsAfterBookWait",
    "mixedBookDeleteIsRejectedWithoutDeletingEitherFixture",
    "separateSalaryRowsForSameEmployeeCannotGenerateTwoLiveAccruals",
    "mismatchedRecordBookIsRejectedWithoutMutation",
    "missingOwnVoucherBeyondExistingMaximumDoesNotBlockAnotherBookGeneration",
    "generateWaitsForDeleteAndDoesNotCreateOrphan",
    "unsuccessfulSaveResponseAfterActualVoucherWritesRollsBackEntireGeneration",
    "failedTemplateLookupPreservesOwnStaleLinkAndCreatesNoAccountingRows",
} | {f"everySameBookWriteWaitsForActualGenerateTransaction(String)[{i}]" for i in range(1, 7)}


def observation_times(record):
    try:
        started, completed = (datetime.fromisoformat(record[key]) for key in ("startedAt", "completedAt"))
        if any(value.utcoffset() != timedelta(0) for value in (started, completed)):
            raise ValueError()
        if not started <= completed <= datetime.now(timezone.utc):
            raise ValueError()
    except (KeyError, TypeError, ValueError):
        raise ValueError("Required database observation times are missing or invalid") from None
    return started.timestamp(), completed.timestamp()


def database_evidence(path, source, operation, target=None):
    """Validate recorded facts against this checkout; no database access."""
    record = json.loads(Path(path).read_text(encoding="utf-8"))
    observation_times(record)
    plan = build_schema_plan()
    selected = record.get("targetDatabase", "")
    if (record.get("passed") is not True or record.get("success") is not True
            or record.get("operation") != operation or not isinstance(selected, str)
            or len(selected) > 64 or re.fullmatch(NAMESPACE, selected) is None
            or (target is not None and selected != target)
            or (os.environ.get("FC_DB_NAME") and selected != os.environ["FC_DB_NAME"])):
        raise ValueError("Required database execution identity is incomplete or differs")
    source_hashes = {f.get("path"): f.get("sha256") for f in source.get("files", [])}
    if (record.get("plan") != plan.evidence() or record.get("sourceInputs") != plan.sources
            or any(source_hashes.get(name) != digest for name, digest in plan.sources.items())):
        raise ValueError("Database plan or DDL source does not match the prepared checkout")
    observed = record.get("observed", {})
    if (not isinstance(observed.get("version"), str) or observed["version"].split("-", 1)[0] != "9.4.0"
            or observed.get("database") != selected or observed.get("performanceSchema") != 1
            or observed.get("sessionIsolation") != "REPEATABLE-READ"
            or observed.get("globalIsolation") != "REPEATABLE-READ"):
        raise ValueError("Required actual version, database or isolation facts are missing")
    lock = record.get("lockObservation", {})
    if any(lock.get(name, {}).get("readable") is not True or type(lock.get(name, {}).get("observedRows")) is not int
           or lock[name]["observedRows"] < 0 for name in ("data_lock_waits", "data_locks", "threads")):
        raise ValueError("Required lock observation facts are missing")
    tables = set(plan.tables)
    rows, structures = record.get("rowCounts", {}), record.get("showCreateSha256", {})
    objects = record.get("schemaObjects", [])
    if (set(rows) != tables or any(type(v) is not int or v != 0 for v in rows.values())
            or record.get("triggerCount") != 0 or set(structures) != tables
            or any(not isinstance(v, str) or re.fullmatch(r"[0-9a-f]{64}", v) is None for v in structures.values())
            or len(objects) != len(tables) or {o.get("name") for o in objects} != tables
            or any(o.get("type") != "BASE TABLE" or o.get("engine") != "InnoDB" for o in objects)):
        raise ValueError("Complete empty repository tables and structure evidence are required")
    indexes = record.get("payrollScopeIndex", [])
    if (len(indexes) != 4 or [r.get("Column_name") for r in indexes] != SCOPE_COLUMNS
            or [r.get("Seq_in_index") for r in indexes] != [1, 2, 3, 4]
            or any(r.get("Key_name") != SCOPE_INDEX or r.get("Non_unique") != 1 or r.get("Sub_part") is not None
                   or r.get("Visible") != "YES" or r.get("Collation") != "A" or r.get("Index_type") != "BTREE" for r in indexes)):
        raise ValueError("Complete production payroll index observation is required")
    if operation == "fresh-create":
        if record.get("targetExisted") is not False or record.get("databaseCreated") is not True or record.get("ddlStatementsCompleted") != len(plan.statements):
            raise ValueError("Fresh database creation evidence is incomplete")
    elif record.get("targetExisted") is not True or record.get("databaseCreated") is not False or observed.get("transactionReadOnly") != 1:
        raise ValueError("Read-only database observation evidence is incomplete")
    return selected


def validate_xml(path, expected_properties=None):
    raw = Path(path).read_bytes()
    if b"<!DOCTYPE" in raw.upper() or b"<!ENTITY" in raw.upper():
        raise ValueError("External or declared XML entities are outside the report boundary")
    root = ET.fromstring(raw)
    if root.tag != "testsuite" or root.get("name") != CLASS:
        raise ValueError("Required payroll suite identity is missing")
    if expected_properties is not None:
        properties = root.findall("properties/property")
        for key, value in expected_properties.items():
            matches = [p.get("value") for p in properties if p.get("name") == key]
            if matches != [value]:
                raise ValueError("Surefire report does not contain this prepared run identity")
    nodes = root.findall("testcase")
    names = [n.get("name") for n in nodes]
    if len(names) != len(EXPECTED_CASES) or set(names) != EXPECTED_CASES:
        raise ValueError("Required payroll case coverage is incomplete or duplicated")
    if any(n.get("classname") != CLASS or n.find("failure") is not None
           or n.find("error") is not None or n.find("skipped") is not None for n in nodes):
        raise ValueError("Payroll cases did not all pass")
    required = {"tests": str(len(EXPECTED_CASES)), "failures": "0", "errors": "0", "skipped": "0"}
    if any(root.get(k) != v for k, v in required.items()) or root.get("flakes", "0") != "0":
        raise ValueError("Payroll report counts are missing or do not match complete execution")
    for tag in ("failure", "error", "skipped", "flakyFailure", "flakyError", "rerunFailure", "rerunError"):
        if next(root.iter(tag), None) is not None:
            raise ValueError("Failure, retry or skipped content cannot qualify")
    return {"executed": len(nodes), "failures": 0, "errors": 0, "skipped": 0,
            "cases": [{"id": CLASS + "." + name, "outcome": "passed"} for name in sorted(names)]}


def prepare(report_dir, output, launcher="./mvnw", offline=False):
    marker = output / "run.json"
    if marker.exists() or (output / "result.json").exists():
        raise ValueError("Prepare requires a fresh evidence directory")
    path = report_dir / REPORT
    before = identity(ROOT)
    nonce = secrets.token_hex(16)
    properties = {"verification.run.nonce": nonce, "verification.checkout.sha": before["source"]["commitSha"],
                  "verification.source.fingerprint": before["source"]["fingerprint"]}
    environment_records = {}
    target = None
    structures = None
    previous_completion = None
    for filename in ("schema-init.json", "database-before.json"):
        metadata_path = output / filename
        target = database_evidence(metadata_path, before["source"], "fresh-create" if filename == "schema-init.json" else "observe", target)
        observed_structures = json.loads(metadata_path.read_text(encoding="utf-8"))["showCreateSha256"]
        started, completed = observation_times(json.loads(metadata_path.read_text(encoding="utf-8")))
        if previous_completion is not None and started < previous_completion:
            raise ValueError("Pre-check predates database initialization completion")
        previous_completion = completed
        if structures is not None and structures != observed_structures:
            raise ValueError("Database structure differs between initialization and pre-check")
        structures = observed_structures
        environment_records[filename] = artifact(metadata_path)
    value = {"schemaVersion": 1, "kind": "payroll-it", "phase": "prepared", "runNonce": nonce,
             "startedAt": utc_now(), "before": before, "targetDatabase": target, "environmentRecords": environment_records,
             "schemaStructureSha256": structures,
             "reportProperties": properties, "command": [launcher] + COMMAND[1:] + (["-o"] if offline else []) + [f"-D{k}={v}" for k, v in properties.items()],
             "reportPath": str(path.resolve()), "previousReport": artifact(path) if path.exists() else None}
    write_json(marker, value)
    return value


def verify(report_dir, output, command_exit):
    marker = json.loads((output / "run.json").read_text(encoding="utf-8"))
    path = report_dir / REPORT
    if marker.get("phase") != "prepared" or marker.get("reportPath") != str(path.resolve()):
        raise ValueError("Report does not match this prepared run")
    current = artifact(path)
    previous = marker.get("previousReport")
    if previous is not None and current["sha256"] == previous.get("sha256"):
        raise ValueError("An earlier unchanged report cannot prove this execution")
    started = datetime.fromisoformat(marker["startedAt"]).timestamp()
    if path.stat().st_mtime < started:
        raise ValueError("Report predates this prepared execution")
    after = identity(ROOT)
    if not same_identity(marker["before"], after):
        raise ValueError("Source or execution identity changed during verification")
    for filename, recorded in marker["environmentRecords"].items():
        if artifact(output / filename)["sha256"] != recorded["sha256"]:
            raise ValueError("Prepared database evidence changed during execution")
    after_database = output / "database-after.json"
    database_evidence(after_database, after["source"], "observe", marker["targetDatabase"])
    if json.loads(after_database.read_text(encoding="utf-8"))["showCreateSha256"] != marker["schemaStructureSha256"]:
        raise ValueError("Database structure changed during the integration suite")
    tests = validate_xml(path, marker["reportProperties"])
    if str(command_exit) != "0":
        raise ValueError("Maven did not complete successfully")
    log = output / "maven.log"
    if not log.is_file() or log.stat().st_size == 0 or log.stat().st_mtime < started:
        raise ValueError("Current Maven log is missing")
    post_started, _ = observation_times(json.loads(after_database.read_text(encoding="utf-8")))
    if post_started < max(started, path.stat().st_mtime, log.stat().st_mtime):
        raise ValueError("Database post-check predates report or command completion")
    return {**marker, "phase": "completed", "after": after, "sourceUnchanged": True,
            "tests": tests, "commandExit": 0, "report": current, "log": artifact(log),
            "databaseAfter": artifact(after_database), "passed": True}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("prepare", "verify"))
    parser.add_argument("--report-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--command-exit", default="unknown")
    parser.add_argument("--launcher", choices=("./mvnw", "mvn"), default="./mvnw")
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args(argv)
    args.output.mkdir(parents=True, exist_ok=True)
    result = {"schemaVersion": 1, "kind": "payroll-it", "phase": args.phase,
              "passed": False, "commandExit": args.command_exit, "startedAt": utc_now()}
    try:
        result = (prepare(args.report_dir, args.output, args.launcher, args.offline) if args.phase == "prepare" else
                  verify(args.report_dir, args.output, args.command_exit))
        code = 0
    except BaseException as error:
        result["failure"] = type(error).__name__
        result["reason"] = str(error) if isinstance(error, ValueError) else "Required execution evidence is missing or unreadable"
        code = 1
    result["finishedAt"] = utc_now()
    write_json(args.output / ("prepare-status.json" if args.phase == "prepare" else "result.json"), result)
    print(f"payroll {args.phase}: {'passed' if code == 0 else 'failed'}; evidence: {args.output}")
    return code


if __name__ == "__main__":
    sys.exit(main())
