"""Read-only, offline assessment of version 1 release candidate evidence.

This module deliberately has no access to the acceptance runner, process launchers,
network clients, or databases. A successful assessment validates supplied evidence;
reviewers remain responsible for the authenticity of external records.
"""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime
import hashlib
import json
from pathlib import Path, PureWindowsPath
import re
import sys
from typing import Any


DEFAULT_PROFILE = Path(__file__).resolve().with_name("release_evidence_profile.json")
STATUSES = {"satisfied", "missing", "failed", "identity_mismatch", "insufficient_scope", "pending_review"}
SHA256 = re.compile(r"^[0-9a-f]{64}$")
COMMIT = re.compile(r"^[0-9a-f]{40}$")
TRUST_BOUNDARY = (
    "Offline assessment checks supplied file integrity, identity, coverage and reviewed records. "
    "It cannot independently authenticate an entirely fabricated set of CI exports, runtime "
    "observations, signatures or restore records. Named reviewers must verify those sources. "
    "Evidence readiness is not authorization to deploy."
)


class BundleFormatError(ValueError):
    """The input root cannot be interpreted as a version 1 evidence bundle."""


def _text(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _integer(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and value >= 0 and value < float("inf")


def _digest(value: Any) -> bool:
    return isinstance(value, str) and SHA256.fullmatch(value) is not None


def _url(value: Any) -> bool:
    """Require the credential/query-free form exported by the collector."""
    return isinstance(value, str) and re.fullmatch(r"https?://[^@\s/?#]+(?:/[^\s?#]*)?", value) is not None


def _time(value: Any) -> datetime | None:
    if not _text(value):
        return None
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None


def _ordered_times(start: Any, finish: Any) -> bool:
    beginning, end = _time(start), _time(finish)
    if beginning is None or end is None:
        return False
    try:
        return beginning <= end
    except TypeError:
        return False


def _basename(value: str) -> str:
    return value.replace("\\", "/").rsplit("/", 1)[-1]


def _relative_parts(value: Any) -> tuple[str, ...] | None:
    """Reject Windows absolute/drive/UNC/ADS forms even on a POSIX host."""
    if not _text(value) or any(ord(character) < 32 or character in '<>:"|?*' for character in value):
        return None
    windows = PureWindowsPath(value)
    normalized = value.replace("\\", "/")
    if windows.drive or windows.root or normalized.startswith("/"):
        return None
    parts = normalized.split("/")
    if any(part in ("", ".", "..") or part.endswith((" ", ".")) or re.fullmatch(r"(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\..*)?", part, re.IGNORECASE) for part in parts):
        return None
    return tuple(parts)


def _json_bytes(raw: bytes) -> Any:
    # Duplicate keys must not let a second "passed" or identity conceal the first.
    def unique(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("duplicate JSON object key")
            result[key] = value
        return result

    def invalid_constant(_value: str) -> Any:
        raise ValueError("non-finite JSON number")

    def finite_float(value: str) -> float:
        number = float(value)
        if not -float("inf") < number < float("inf"):
            raise ValueError("non-finite JSON number")
        return number

    return json.loads(raw.decode("utf-8-sig"), object_pairs_hook=unique, parse_constant=invalid_constant, parse_float=finite_float)


class Assessment:
    def __init__(self, root: Path):
        self.root = root
        self.conditions: list[dict[str, Any]] = []
        self.expected_identity: dict[str, Any] = {}
        self.actual_cases: set[tuple[str, str, tuple[str, ...]]] = set()
        self.synthetic = False

    def add(self, identifier: str, status: str, ref: Any, reason: str, action: str) -> None:
        if status not in STATUSES:
            raise ValueError("invalid internal condition status")
        self.conditions.append({"id": identifier, "status": status,
                                "evidenceRef": ref if isinstance(ref, str) else None, "reason": reason, "nextAction": action})

    def check(self, identifier: str, valid: bool, ref: Any, reason: str, action: str,
              status: str = "failed", success: str = "Required evidence is valid.") -> bool:
        self.add(identifier, "satisfied" if valid else status, ref,
                 success if valid else reason, "None." if valid else action)
        return valid

    def fields(self, record: dict[str, Any], names: list[str], identifier: str, ref: Any) -> bool:
        valid = True
        for name in names:
            value = record.get(name)
            ok = _text(value)
            self.check(f"{identifier}.{name}", ok, ref, f"Required {name} is missing or invalid.",
                       f"Provide the original record's {name}.", "missing" if value is None else "failed")
            valid = valid and ok
        return valid

    def reference(self, ref: Any, identifier: str) -> bytes | None:
        label = ref.get("path") if isinstance(ref, dict) and isinstance(ref.get("path"), str) else None
        if not isinstance(ref, dict):
            self.add(identifier, "missing" if ref is None else "failed", None,
                     "A file reference with path and SHA-256 is required.", "Provide a bundle-relative file reference and its SHA-256.")
            return None
        parts = _relative_parts(ref.get("path"))
        if parts is None:
            self.add(identifier, "failed", label, "Evidence path is not a safe relative path.",
                     "Use a relative path inside the evidence bundle; remove absolute, drive, traversal or stream syntax.")
            return None
        if not _digest(ref.get("sha256")):
            self.add(identifier, "missing" if ref.get("sha256") is None else "failed", label,
                     "A valid lowercase SHA-256 is required.", "Record the original file's content SHA-256.")
            return None
        try:
            target = self.root.joinpath(*parts).resolve(strict=True)
            if not target.is_relative_to(self.root):
                self.add(identifier, "failed", label, "Resolved evidence path escapes the bundle root.",
                         "Place evidence inside the bundle and remove escaping links.")
                return None
            if not target.is_file():
                self.add(identifier, "failed", label, "Evidence reference is not a regular file.", "Reference a regular evidence file.")
                return None
            raw = target.read_bytes()
        except (OSError, ValueError, RuntimeError):
            self.add(identifier, "missing", label, "Evidence file is unavailable or its path cannot be resolved.",
                     "Supply the referenced original file inside the bundle.")
            return None
        if hashlib.sha256(raw).hexdigest() != ref["sha256"]:
            self.add(identifier, "failed", label, "Evidence content does not match its recorded SHA-256.",
                     "Recover the original file or collect and review a new evidence record.")
            return None
        if not raw:
            self.add(identifier, "failed", label, "Evidence file is empty.", "Supply the complete original evidence.")
            return None
        self.add(identifier, "satisfied", label, "Evidence file is contained, nonempty and matches its content SHA-256.", "None.")
        return raw

    def record(self, ref: Any, identifier: str) -> dict[str, Any] | None:
        raw = self.reference(ref, f"{identifier}.integrity")
        if raw is None:
            return None
        label = ref["path"]
        try:
            data = _json_bytes(raw)
        except (UnicodeError, ValueError, RecursionError):
            self.add(f"{identifier}.format", "failed", label, "Original evidence is not valid UTF-8 JSON.",
                     "Supply the complete original JSON record with unique keys and finite numbers.")
            return None
        if not isinstance(data, dict):
            self.add(f"{identifier}.format", "failed", label, "Original evidence must be a JSON object.", "Supply the original object record.")
            return None
        if data.get("synthetic") is True or isinstance(data.get("metadata"), dict) and data["metadata"].get("synthetic") is True:
            self.synthetic = True
        self.add(f"{identifier}.format", "satisfied", label, "Original evidence is a JSON object.", "None.")
        return data

    def identity(self, data: Any, identifier: str, ref: Any) -> None:
        record = data if isinstance(data, dict) else {}
        for field in ("candidateId", "commitSha", "sourceFingerprint", "backendSha256", "frontendSha256"):
            actual, expected = record.get(field), self.expected_identity.get(field)
            valid_value = _text(actual) if field == "candidateId" else (
                isinstance(actual, str) and COMMIT.fullmatch(actual) is not None if field == "commitSha" else _digest(actual))
            ok = valid_value and expected is not None and actual == expected
            status = "missing" if actual is None else "identity_mismatch" if valid_value else "failed"
            self.check(f"{identifier}.{field}", ok, ref, f"{field} is missing, invalid or does not match the candidate.",
                       "Collect evidence for the exact clean candidate and its production artifacts; preserve historical identities.", status)

    def review(self, data: dict[str, Any], identifier: str, ref: Any) -> None:
        review = data.get("review")
        record = review if isinstance(review, dict) else {}
        self.fields(record, ["reviewer"], f"{identifier}.review", ref)
        value = record.get("reviewedAt")
        self.check(f"{identifier}.review.reviewedAt", _time(value) is not None, ref,
                   "Review timestamp is missing or invalid.", "Record when the named reviewer verified the original source.",
                   "missing" if value is None else "failed")
        self.check(f"{identifier}.review.conclusion", record.get("conclusion") == "approved", ref,
                   "Original source review is not approved.", "Have the designated reviewer verify the source and record an approved conclusion.", "pending_review")
        supports = data.get("supportingEvidence")
        if not isinstance(supports, list) or not supports:
            self.add(f"{identifier}.supportingEvidence", "missing" if supports is None else "insufficient_scope", ref,
                     "At least one supporting evidence reference is required.", "Include the original supporting records and their content hashes.")
        else:
            for index, support in enumerate(supports):
                self.reference(support, f"{identifier}.supportingEvidence.{index:03d}")


def _candidate(assessment: Assessment, data: Any) -> dict[str, Any]:
    candidate = data if isinstance(data, dict) else {}
    assessment.fields(candidate, ["candidateId"], "candidate", "candidate")
    sha = candidate.get("commitSha")
    assessment.check("candidate.commitSha", isinstance(sha, str) and COMMIT.fullmatch(sha) is not None,
                     "candidate", "Candidate requires a full lowercase 40-character commit SHA.", "Identify the exact committed candidate.",
                     "missing" if sha is None else "failed")
    source = candidate.get("source") if isinstance(candidate.get("source"), dict) else {}
    assessment.check("candidate.source.state", source.get("state") == "clean", "candidate",
                     "Candidate source is dirty, unknown or lacks a clean source record.", "Collect candidate evidence from a clean committed source tree.",
                     "missing" if source.get("state") is None else "failed")
    assessment.check("candidate.source.fingerprint", _digest(source.get("fingerprint")), "candidate",
                     "Candidate source fingerprint is missing or invalid.", "Supply the complete collected source fingerprint.",
                     "missing" if source.get("fingerprint") is None else "failed")
    artifacts = candidate.get("artifacts") if isinstance(candidate.get("artifacts"), dict) else {}
    backend = artifacts.get("backend") if isinstance(artifacts.get("backend"), dict) else {}
    frontend = artifacts.get("frontend") if isinstance(artifacts.get("frontend"), dict) else {}
    for name, artifact in (("backend", backend), ("frontend", frontend)):
        assessment.check(f"candidate.artifacts.{name}.sha256", _digest(artifact.get("sha256")), "candidate",
                         f"Candidate {name} artifact SHA-256 is missing or invalid.", "Collect the production artifact identity.",
                         "missing" if artifact.get("sha256") is None else "failed")
    assessment.check("candidate.artifacts.frontend.kind", frontend.get("kind") == "production", "candidate",
                     "A production frontend artifact is required.", "Build and identify the production frontend artifact.", "insufficient_scope")
    assessment.expected_identity = {"candidateId": candidate.get("candidateId"), "commitSha": sha,
                                    "sourceFingerprint": source.get("fingerprint"), "backendSha256": backend.get("sha256"),
                                    "frontendSha256": frontend.get("sha256")}
    manifest = assessment.record(frontend.get("manifest"), "candidate.artifacts.frontend.manifest")
    if manifest is not None:
        files = manifest.get("files")
        valid = manifest.get("kind") == "production" and isinstance(files, list) and bool(files)
        seen: set[str] = set()
        if isinstance(files, list):
            for entry in files:
                if not isinstance(entry, dict):
                    valid = False
                    continue
                parts = _relative_parts(entry.get("path"))
                normalized = "/".join(parts) if parts else ""
                if not parts or normalized.casefold() in seen or not _integer(entry.get("size")) or entry["size"] < 0 or not _digest(entry.get("sha256")):
                    valid = False
                seen.add(normalized.casefold())
        assessment.check("candidate.artifacts.frontend.manifest.files", valid, frontend["manifest"]["path"],
                         "Production frontend manifest has missing, unsafe, duplicate or invalid file entries.",
                         "Supply a nonempty production file manifest with unique relative paths, sizes and SHA-256 values.")
        if valid:
            canonical = json.dumps(sorted(files, key=lambda item: item["path"]), sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
            digest = hashlib.sha256(canonical).hexdigest()
            assessment.check("candidate.artifacts.frontend.manifest.identity", _digest(manifest.get("sha256")) and digest == manifest.get("sha256") == frontend.get("sha256"),
                             frontend["manifest"]["path"], "Frontend file manifest identity does not match the candidate artifact.",
                             "Collect the production manifest and use its canonical content digest as the candidate frontend identity.", "identity_mismatch")
    return candidate


def _profile(assessment: Assessment, declared: Any, path: Path) -> dict[str, Any]:
    try:
        raw = path.read_bytes()
        profile = _json_bytes(raw)
    except (OSError, ValueError, UnicodeError, RecursionError):
        assessment.add("profile.trusted", "failed", str(path), "The trusted local profile cannot be read as valid JSON.",
                       "Restore and review the version-controlled acceptance profile.")
        return {}
    valid = isinstance(profile, dict) and type(profile.get("schemaVersion")) is int and profile["schemaVersion"] == 1 and _text(profile.get("id")) and _integer(profile.get("version")) and profile["version"] > 0
    if not valid:
        assessment.add("profile.trusted", "failed", str(path), "Trusted profile root format is invalid.", "Restore the reviewed profile.")
        return {}
    assessment.add("profile.trusted", "satisfied", str(path), "Trusted profile is read from the explicitly selected local profile file.", "None.")
    record = declared if isinstance(declared, dict) else {}
    for key, expected in (("id", profile["id"]), ("version", profile["version"]), ("sha256", hashlib.sha256(raw).hexdigest())):
        value = record.get(key)
        assessment.check(f"profile.binding.{key}", type(value) is type(expected) and value == expected, "profile",
                         "Bundle profile identity does not match the trusted local profile.",
                         "Use the reviewed profile id, version and exact file content SHA-256; do not reduce required scope.",
                         "missing" if value is None else "identity_mismatch")
    groups = profile.get("groups")
    seen_groups: set[str] = set()
    seen_cases: set[tuple[str, str, tuple[str, ...]]] = set()
    valid_groups = isinstance(groups, list) and bool(groups)
    if isinstance(groups, list):
        for group in groups:
            if not isinstance(group, dict) or not _text(group.get("name")) or group["name"] in seen_groups:
                valid_groups = False
                continue
            seen_groups.add(group["name"])
            specs, cases = group.get("specs"), group.get("cases")
            if not isinstance(specs, list) or not specs or any(not _text(s) or _basename(s) != s for s in specs) or len(set(s for s in specs if isinstance(s, str))) != len(specs):
                valid_groups = False
            if not isinstance(cases, list) or not cases:
                valid_groups = False
                continue
            for case in cases:
                key = _case_key(case)
                if key is None or key in seen_cases or not isinstance(specs, list) or key[0] not in specs:
                    valid_groups = False
                if key is not None:
                    seen_cases.add(key)
    assessment.check("profile.groups", valid_groups, str(path), "Trusted profile groups/specs/cases are empty, malformed or duplicated.",
                     "Review and restore the complete stable group, project, spec and case identity list.")
    if not valid_groups:
        profile = dict(profile, groups=[])
    ci = profile.get("ci")
    valid_ci = isinstance(ci, dict) and _text(ci.get("workflow")) and isinstance(ci.get("requiredJobs"), list) and bool(ci["requiredJobs"]) and isinstance(ci.get("requiredSteps"), dict)
    if valid_ci:
        jobs = ci["requiredJobs"]
        valid_ci = all(_text(job) for job in jobs) and len(set(job for job in jobs if isinstance(job, str))) == len(jobs)
        for job in jobs:
            steps = ci["requiredSteps"].get(job) if isinstance(job, str) else None
            valid_ci = valid_ci and isinstance(steps, list) and bool(steps) and all(_text(step) for step in steps) and len(set(step for step in steps if isinstance(step, str))) == len(steps)
    assessment.check("profile.ci", valid_ci, str(path), "Trusted CI workflow/jobs/steps are missing, malformed or duplicated.", "Review the complete required CI scope.")
    if not valid_ci:
        profile = dict(profile, ci={})
    for name in ("requiredAccountingComparisons", "requiredRestoreComparisons"):
        values = profile.get(name)
        valid_values = isinstance(values, list) and bool(values) and all(_text(v) for v in values) and len(set(v for v in values if isinstance(v, str))) == len(values)
        assessment.check(f"profile.{name}", valid_values, str(path), "Trusted comparison scope is missing, malformed or duplicated.", "Review and restore the required comparison list.")
        if not valid_values:
            profile = dict(profile, **{name: []})
    return profile


def _case_key(case: Any) -> tuple[str, str, tuple[str, ...]] | None:
    if not isinstance(case, dict) or not _text(case.get("file")) or _basename(case["file"]) != case["file"] or not _text(case.get("project")):
        return None
    titles = case.get("titlePath")
    if not isinstance(titles, list) or not titles or not all(_text(title) for title in titles):
        return None
    return case["file"], case["project"], tuple(titles)


def _playwright(assessment: Assessment, data: dict[str, Any], group: dict[str, Any], identifier: str, ref: Any) -> None:
    stats = data.get("stats")
    stats_valid = isinstance(stats, dict) and all(_integer(stats.get(name)) and stats[name] >= 0 for name in ("expected", "unexpected", "skipped", "flaky")) and _time(stats.get("startTime")) is not None and _number(stats.get("duration"))
    assessment.check(f"{identifier}.stats", stats_valid, ref, "Original report is missing valid expected/unexpected/skipped/flaky, startTime or duration statistics.",
                     "Retain the complete original execution report; never infer omitted failures as zero.")
    errors = data.get("errors")
    assessment.check(f"{identifier}.errors", isinstance(errors, list) and not errors, ref, "Original report errors are missing, malformed or nonempty.", "Resolve report errors and run the complete suite again.")
    cases: list[tuple[str, str, tuple[str, ...]]] = []
    case_results_valid = True
    structure_valid = True

    def walk(suites: Any, parents: tuple[str, ...] = (), depth: int = 0) -> None:
        nonlocal structure_valid, case_results_valid
        if not isinstance(suites, list) or depth > 128:
            structure_valid = False
            return
        for suite in suites:
            if not isinstance(suite, dict) or not _text(suite.get("title")):
                structure_valid = False
                continue
            suite_title = suite["title"]
            file_suite = depth == 0 and _text(suite.get("file")) and _basename(suite["file"]) == _basename(suite_title)
            titles = parents if file_suite else parents + (suite_title,)
            specs = suite.get("specs")
            if not isinstance(specs, list):
                structure_valid = False
            else:
                for spec in specs:
                    if not isinstance(spec, dict) or not _text(spec.get("title")) or not _text(spec.get("file")) or not isinstance(spec.get("tests"), list) or not spec["tests"]:
                        structure_valid = False
                        continue
                    for test in spec["tests"]:
                        if not isinstance(test, dict) or not _text(test.get("projectName")):
                            structure_valid = False
                            continue
                        cases.append((_basename(spec["file"]), test["projectName"], titles + (spec["title"],)))
                        results = test.get("results")
                        good = spec.get("ok") is True and test.get("expectedStatus") == "passed" and test.get("status") == "expected" and isinstance(results, list) and len(results) == 1
                        if good:
                            attempt = results[0]
                            good = isinstance(attempt, dict) and attempt.get("status") == "passed" and _integer(attempt.get("retry")) and attempt["retry"] == 0 and isinstance(attempt.get("errors"), list) and not attempt["errors"] and not attempt.get("error")
                        case_results_valid = case_results_valid and good
            if "suites" in suite:
                walk(suite["suites"], titles, depth + 1)

    walk(data.get("suites"))
    assessment.check(f"{identifier}.structure", structure_valid and bool(cases), ref, "Original execution suites/specs/tests are missing, empty or malformed.", "Supply the complete original executed Playwright report.")
    assessment.check(f"{identifier}.results", case_results_valid and bool(cases), ref, "At least one case is unfinished, failed, skipped, flaky, expected-failed or retried, or required result fields are absent.",
                     "Run every case once with retries disabled and retain successful original results.")
    duplicates = len(cases) != len(set(cases)) or bool(assessment.actual_cases.intersection(cases))
    assessment.actual_cases.update(cases)
    assessment.check(f"{identifier}.duplicates", not duplicates, ref, "Case identities are duplicated within or across supplied groups.", "Supply each stable project/spec/title case exactly once.")
    expected = {_case_key(case) for case in group.get("cases", [])}
    missing = expected.difference(cases)
    assessment.check(f"{identifier}.coverage", bool(expected) and None not in expected and not missing, ref,
                     f"Required group cases are absent ({len(missing)} missing); summary totals cannot prove coverage.",
                     "Execute and retain every required profile case in this group.", "insufficient_scope")
    if stats_valid:
        complete = stats["expected"] == len(cases) and len(cases) > 0 and all(stats[name] == 0 for name in ("unexpected", "skipped", "flaky"))
        assessment.check(f"{identifier}.counts", complete, ref, "Original statistics do not match complete successful case execution or include failures/skips/flaky cases.",
                         "Resolve all non-passing cases and retain consistent complete original statistics.")


def _environments(assessment: Assessment, refs: Any) -> dict[str, list[dict[str, Any]]]:
    environments: dict[str, list[dict[str, Any]]] = {}
    if not isinstance(refs, list) or not refs:
        assessment.add("environments", "missing" if refs is None else "insufficient_scope", "evidence.environments",
                       "Reviewed execution environments are absent or malformed.", "Provide actual observed runtime and datasource binding records.")
        return environments
    for index, reference in enumerate(refs):
        identifier = f"environments.{index:03d}"
        record = assessment.record(reference, identifier)
        if record is None:
            continue
        label = reference["path"]
        assessment.fields(record, ["environmentId", "instanceId", "apiUrl", "frontendUrl"], identifier, label)
        for name in ("apiUrl", "frontendUrl"):
            value = record.get(name)
            assessment.check(f"{identifier}.{name}.format", _url(value), label,
                             "Runtime service URL is missing or invalid.", "Record the actual observed HTTP or HTTPS service URL.")
        environment_id = record.get("environmentId")
        if _text(environment_id):
            environments.setdefault(environment_id, []).append(record)
        assessment.identity(record.get("identity"), f"{identifier}.identity", label)
        datasource = record.get("datasource") if isinstance(record.get("datasource"), dict) else {}
        assessment.fields(datasource, ["host", "database", "instanceId"], f"{identifier}.datasource", label)
        assessment.check(f"{identifier}.datasource.port", _integer(datasource.get("port")) and 1 <= datasource["port"] <= 65535, label,
                         "Observed datasource port is missing or invalid.", "Record the observed datasource port.")
        assessment.check(f"{identifier}.frontendKind", record.get("frontendKind") == "production", label,
                         "Runtime evidence does not prove the production frontend was exercised.", "Observe and review execution of the identified production frontend.", "insufficient_scope")
        observation = record.get("observation") if isinstance(record.get("observation"), dict) else {}
        method = observation.get("method")
        assessment.check(f"{identifier}.observation.method", _text(method) and re.match(r"^(declared|unknown|pending)(?:$|[\s_-])", method.strip().casefold()) is None and method.strip().casefold() not in ("configuration", "config", "environment variables"), label,
                         "Runtime binding is declared, unknown or lacks an actual observation method.", "Record actual deployment/startup and datasource observation, with supporting records.", "insufficient_scope")
        assessment.check(f"{identifier}.observation.observedAt", _time(observation.get("observedAt")) is not None, label,
                         "Actual runtime observation timestamp is missing or invalid.", "Record when the binding was actually observed.")
        assessment.review(record, identifier, label)
        runs = record.get("runs")
        valid_runs = isinstance(runs, list) and bool(runs)
        assessment.check(f"{identifier}.runs", valid_runs, label,
                         "Reviewed environment does not cover actual run IDs and original provenance hashes.", "Review this exact run and include its original provenance reference.", "insufficient_scope")
        run_ids: list[str] = []
        if isinstance(runs, list):
            for run_index, run in enumerate(runs):
                run_identifier = f"{identifier}.runs.{run_index:03d}"
                if not isinstance(run, dict):
                    assessment.add(run_identifier, "failed", label, "Observed run binding must be an object.", "Record runId and the actual original provenance reference.")
                    continue
                assessment.fields(run, ["runId"], run_identifier, label)
                if _text(run.get("runId")):
                    run_ids.append(run["runId"])
                raw_run = assessment.record(run.get("provenance"), f"{run_identifier}.provenance")
                if raw_run is not None:
                    assessment.check(f"{run_identifier}.identity", _text(run.get("runId")) and run["runId"] == raw_run.get("runId"), label,
                                     "Observed run ID differs from its original provenance record.", "Bind the actual observed run to its exact original provenance file.", "identity_mismatch")
        assessment.check(f"{identifier}.runs.unique", len(run_ids) == len(set(run_ids)), label,
                         "Observed run identities are duplicated.", "Provide one unambiguous provenance binding per observed run.")
    assessment.check("environments.unique", all(len(paths) == 1 for paths in environments.values()), "evidence.environments",
                     "Environment identities are duplicated.", "Supply one unambiguous reviewed record per environment identity.")
    return environments


def _provenance_source(assessment: Assessment, record: Any, identifier: str, ref: Any) -> None:
    source = record if isinstance(record, dict) else {}
    assessment.check(f"{identifier}.state", source.get("state") == "clean", ref,
                     "Original run source is dirty, unknown or missing.", "Collect execution provenance from a clean committed candidate.")
    for name, expected in (("commitSha", assessment.expected_identity.get("commitSha")),
                           ("fingerprint", assessment.expected_identity.get("sourceFingerprint"))):
        valid = isinstance(source.get(name), str) and (COMMIT.fullmatch(source[name]) is not None if name == "commitSha" else _digest(source[name]))
        assessment.check(f"{identifier}.{name}", valid and source[name] == expected, ref,
                         "Original run source identity does not match the candidate.", "Collect a new run for the exact candidate; do not relabel historical evidence.", "identity_mismatch")
    git = source.get("gitStatus")
    valid_git = isinstance(git, dict) and isinstance(git.get("tracked"), list) and not git["tracked"] and isinstance(git.get("untracked"), list) and not git["untracked"]
    assessment.check(f"{identifier}.collection", source.get("status") == "collected" and source.get("collectionStable") is True and valid_git, ref,
                     "Original source collection is missing, unstable or records tracked/untracked changes.", "Retain the stable original clean source collection with full Git status.")
    files = source.get("files")
    valid_files = isinstance(files, list) and bool(files)
    paths: set[str] = set()
    if isinstance(files, list):
        for entry in files:
            if not isinstance(entry, dict):
                valid_files = False
                continue
            parts = _relative_parts(entry.get("path"))
            normalized = "/".join(parts) if parts else ""
            if not parts or normalized.casefold() in paths or not _integer(entry.get("size")) or entry["size"] < 0 or not _digest(entry.get("sha256")):
                valid_files = False
            paths.add(normalized.casefold())
    assessment.check(f"{identifier}.files", valid_files, ref,
                     "Original full source file manifest is missing, malformed or duplicated.", "Retain the complete canonical source file manifest from the original collection.")
    if valid_files:
        canonical = json.dumps(sorted(files, key=lambda entry: entry["path"]), sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
        assessment.check(f"{identifier}.files.fingerprint", hashlib.sha256(canonical).hexdigest() == source.get("fingerprint"), ref,
                         "Original source file manifest does not match its recorded fingerprint.", "Recover the original complete source manifest or collect new provenance.", "identity_mismatch")


def _provenance(assessment: Assessment, entry: dict[str, Any], identifier: str) -> dict[str, Any] | None:
    provenance = assessment.record(entry.get("provenance"), f"{identifier}.provenance")
    if provenance is None:
        return None
    ref = entry["provenance"]["path"]
    prefix = f"{identifier}.provenance"
    assessment.check(f"{prefix}.schemaVersion", type(provenance.get("schemaVersion")) is int and provenance["schemaVersion"] == 1, ref,
                     "Original run provenance does not use schemaVersion 1.", "Retain the original version 1 source/run collection record.")
    assessment.check(f"{prefix}.completion", provenance.get("status") == "completed" and _ordered_times(provenance.get("startedAt"), provenance.get("finishedAt")), ref,
                     "Original acceptance run did not complete successfully or lacks valid completion fields.", "Run the complete scope and retain its actual successful completion record.")
    assessment.check(f"{prefix}.runId", _text(provenance.get("runId")) and provenance["runId"] == entry.get("runId"), ref,
                     "Outer acceptance run ID does not match the original run provenance.", "Use the original execution's exact run identity.", "identity_mismatch")
    assessment.check(f"{prefix}.times", provenance.get("startedAt") == entry.get("startedAt") and provenance.get("finishedAt") == entry.get("finishedAt") and _ordered_times(entry.get("startedAt"), entry.get("finishedAt")), ref,
                     "Outer acceptance start/completion times do not match the original run boundaries.", "Use the original execution's exact run start and completion times.", "identity_mismatch")
    for name in ("sourceBefore", "sourceAfter"):
        _provenance_source(assessment, provenance.get(name), f"{prefix}.{name}", ref)
    assessment.check(f"{prefix}.sourceChanged", provenance.get("sourceChanged") is False, ref,
                     "Source changed across execution or original source change tracking is absent.", "Repeat acceptance without source drift and retain both original source snapshots.", "identity_mismatch")
    for name in ("artifacts", "artifactsAfter"):
        artifacts = provenance.get(name) if isinstance(provenance.get(name), dict) else {}
        for kind, identity in (("backend", "backendSha256"), ("frontend", "frontendSha256")):
            artifact = artifacts.get(kind) if isinstance(artifacts.get(kind), dict) else {}
            assessment.check(f"{prefix}.{name}.{kind}", _digest(artifact.get("sha256")) and artifact["sha256"] == assessment.expected_identity.get(identity) and (kind != "frontend" or artifact.get("kind") == "production"), ref,
                             "Original run artifact identity is absent or differs from the candidate production artifact.", "Collect acceptance with the exact candidate backend and production frontend artifacts.", "identity_mismatch")
    assessment.check(f"{prefix}.artifactsChanged", provenance.get("artifactsChanged") is False, ref,
                     "Artifacts changed across execution or original artifact change tracking is absent.", "Repeat acceptance with stable candidate production artifacts.", "identity_mismatch")
    groups = provenance.get("groups")
    matches = [group for group in groups if isinstance(group, dict) and group.get("name") == entry.get("group")] if isinstance(groups, list) else []
    assessment.check(f"{prefix}.group", len(matches) == 1, ref,
                     "Original run provenance has no unique record for this acceptance group.", "Supply the original complete group execution record.", "insufficient_scope")
    if len(matches) == 1:
        group = matches[0]
        outer_report = entry.get("report") if isinstance(entry.get("report"), dict) else {}
        original_report = group.get("report") if isinstance(group.get("report"), dict) else {}
        assessment.check(f"{prefix}.group.report", _relative_parts(original_report.get("path")) is not None and _digest(original_report.get("sha256")) and original_report["sha256"] == outer_report.get("sha256"), ref,
                         "Original run group report content hash differs from the supplied report or lacks a safe original path.", "Supply the exact report produced by this original group execution.", "identity_mismatch")
        assessment.check(f"{prefix}.group.exitCode", _integer(group.get("exitCode")) and group["exitCode"] == 0 and group["exitCode"] == entry.get("exitCode"), ref,
                         "Original group exit code is missing, failed or differs from the outer record.", "Resolve the group failure and preserve its actual process exit result.")
        if "status" in group:
            assessment.check(f"{prefix}.group.status", group["status"] == "completed", ref,
                             "Original group execution did not complete successfully.", "Collect the actual completed group execution.")
        if "startedAt" in group or "finishedAt" in group:
            valid_times = _ordered_times(provenance.get("startedAt"), group.get("startedAt")) and _ordered_times(group.get("startedAt"), group.get("finishedAt")) and _ordered_times(group.get("finishedAt"), provenance.get("finishedAt"))
            assessment.check(f"{prefix}.group.times", valid_times, ref,
                             "Original group timestamps are invalid or fall outside the original run boundaries.", "Preserve actual ordered group times within the original run.")
    return provenance


def _runtime_binding(assessment: Assessment, entry: dict[str, Any], provenance: dict[str, Any] | None,
                     environment: dict[str, Any], identifier: str, ref: Any) -> None:
    prefix = f"{identifier}.binding"
    runs = environment.get("runs")
    matches = [run for run in runs if isinstance(run, dict) and run.get("runId") == entry.get("runId")] if isinstance(runs, list) else []
    outer_ref = entry.get("provenance") if isinstance(entry.get("provenance"), dict) else {}
    bound_ref = matches[0].get("provenance") if len(matches) == 1 and isinstance(matches[0].get("provenance"), dict) else {}
    assessment.check(f"{prefix}.observedRun", len(matches) == 1 and _digest(outer_ref.get("sha256")) and outer_ref["sha256"] == bound_ref.get("sha256"), ref,
                     "Reviewed environment does not cover this exact run and original provenance content hash.", "Have the technical reviewer observe and bind this exact run's original provenance to its actual service and datasource.", "identity_mismatch")
    raw = provenance if isinstance(provenance, dict) else {}
    declarations = raw.get("declaredEnvironment") if isinstance(raw.get("declaredEnvironment"), dict) else {}
    datasource = environment.get("datasource") if isinstance(environment.get("datasource"), dict) else {}
    expected = {"api": environment.get("apiUrl"), "frontend": environment.get("frontendUrl"), "database": datasource.get("database")}
    for source_name, source in (("run", raw), ("declaredEnvironment", declarations)):
        for name in ("api", "frontend", "database"):
            value = source.get(name)
            valid_value = _url(value) if name in ("api", "frontend") else _text(value) and value.strip().casefold() != "unknown"
            assessment.check(f"{prefix}.{source_name}.{name}", valid_value and value == expected[name], ref,
                             "Original run service/datasource declaration is missing or differs from the actually observed environment.", "Observe and review the actual service and datasource used by this run; preserve contradictory original declarations.", "missing" if value is None else "identity_mismatch")
        for name, observed in (("databaseHost", datasource.get("host")), ("databasePort", datasource.get("port"))):
            value = source.get(name)
            if value is None or isinstance(value, str) and value.strip().casefold() == "unknown":
                continue
            valid = _text(value) and value == observed if name == "databaseHost" else (_integer(value) and value == observed or isinstance(value, str) and value.isdecimal() and int(value) == observed)
            assessment.check(f"{prefix}.{source_name}.{name}", valid, ref,
                             "Known original datasource host/port contradicts the observed datasource.", "Resolve the actual datasource mismatch and collect a new reviewed run.", "identity_mismatch")
        component = source.get("componentFrontend")
        if component is not None and component != "" and not (isinstance(component, str) and component.strip().casefold() == "unknown"):
            assessment.check(f"{prefix}.{source_name}.componentFrontend", _url(component) and component == environment.get("componentFrontendUrl"), ref,
                             "Original component frontend URL lacks the same actual reviewed runtime binding.", "Observe and record this run's actual component frontend URL.", "identity_mismatch")
            source_before = raw.get("sourceBefore") if isinstance(raw.get("sourceBefore"), dict) else {}
            identity = environment.get("identity") if isinstance(environment.get("identity"), dict) else {}
            assessment.check(f"{prefix}.{source_name}.componentSource", _digest(source_before.get("fingerprint")) and source_before["fingerprint"] == identity.get("sourceFingerprint"), ref,
                             "Observed component frontend source does not match the original run source fingerprint.", "Review the exact component frontend source fingerprint used by this run.", "identity_mismatch")


def _acceptance(assessment: Assessment, entries: Any, profile: dict[str, Any], environments: dict[str, list[dict[str, Any]]]) -> None:
    groups = {group["name"]: group for group in profile.get("groups", [])}
    records = entries if isinstance(entries, list) else []
    assessment.check("acceptance.records", isinstance(entries, list) and bool(entries), "evidence.acceptance",
                     "Acceptance records are absent, empty or malformed.", "Collect the complete accounting and book-isolation execution records.",
                     "missing" if entries is None else "failed")
    counts = Counter(entry.get("group") for entry in records if isinstance(entry, dict) and isinstance(entry.get("group"), str))
    for name in sorted(groups):
        assessment.check(f"acceptance.scope.{name}", counts[name] == 1, "evidence.acceptance",
                         "Required acceptance group is missing or duplicated.", "Supply exactly one complete original record for this trusted profile group.", "insufficient_scope")
    for index, entry in enumerate(records):
        identifier = f"acceptance.{index:03d}"
        if not isinstance(entry, dict):
            assessment.add(identifier, "failed", "evidence.acceptance", "Acceptance entry must be an object.", "Supply a complete group execution record.")
            continue
        label = entry.get("report", {}).get("path") if isinstance(entry.get("report"), dict) else "evidence.acceptance"
        assessment.fields(entry, ["group", "runId", "environmentId"], identifier, label)
        group_name = entry.get("group")
        assessment.check(f"{identifier}.scope", isinstance(group_name, str) and group_name in groups, label,
                         "Acceptance group does not belong to the trusted profile.", "Associate reports with the reviewed required group identities.", "insufficient_scope")
        assessment.identity(entry.get("identity"), f"{identifier}.identity", label)
        provenance = _provenance(assessment, entry, identifier)
        assessment.check(f"{identifier}.completion", _ordered_times(entry.get("startedAt"), entry.get("finishedAt")), label,
                         "Acceptance lacks valid ordered start and completion timestamps.", "Retain the actual completed execution record.")
        assessment.check(f"{identifier}.exitCode", _integer(entry.get("exitCode")) and entry["exitCode"] == 0, label,
                         "Group process exit code is missing, invalid or nonzero.", "Resolve the execution failure and collect a successful complete run.")
        environment_id = entry.get("environmentId")
        assessment.check(f"{identifier}.environment", isinstance(environment_id, str) and len(environments.get(environment_id, [])) == 1, label,
                         "Acceptance is not bound to one reviewed execution environment.", "Provide the matching observed environment record for this execution.", "insufficient_scope")
        if isinstance(environment_id, str) and len(environments.get(environment_id, [])) == 1:
            _runtime_binding(assessment, entry, provenance, environments[environment_id][0], identifier, label)
        report = assessment.record(entry.get("report"), f"{identifier}.report")
        if report is not None:
            group = groups.get(group_name, {"cases": []}) if isinstance(group_name, str) else {"cases": []}
            _playwright(assessment, report, group, identifier, label)
            stats = report.get("stats") if isinstance(report.get("stats"), dict) else {}
            assessment.check(f"{identifier}.report.runTime", _ordered_times(entry.get("startedAt"), stats.get("startTime")) and _ordered_times(stats.get("startTime"), entry.get("finishedAt")), label,
                             "Original report start time does not fall within the recorded original run.", "Supply the report produced inside this actual run's time boundaries.", "identity_mismatch")


def _external(assessment: Assessment, evidence: dict[str, Any], name: str) -> tuple[dict[str, Any], Any] | None:
    ref = evidence.get(name)
    record = assessment.record(ref, name)
    if record is None:
        return None
    label = ref["path"]
    assessment.identity(record.get("identity"), f"{name}.identity", label)
    assessment.review(record, name, label)
    return record, label


def _ci(assessment: Assessment, evidence: dict[str, Any], profile: dict[str, Any]) -> None:
    external = _external(assessment, evidence, "ci")
    if external is None:
        return
    record, ref = external
    assessment.fields(record, ["provider", "runId", "url", "workflow"], "ci", ref)
    assessment.check("ci.url.format", isinstance(record.get("url"), str) and re.match(r"^https?://[^\s/]+(?:/|$)", record["url"]) is not None, ref,
                     "Actual CI run URL is missing or invalid.", "Supply the URL of the actual candidate CI run.")
    assessment.check("ci.headSha", record.get("headSha") == assessment.expected_identity.get("commitSha") and isinstance(record.get("headSha"), str) and COMMIT.fullmatch(record["headSha"]) is not None,
                     ref, "CI head SHA does not identify the full candidate commit.", "Collect the actual CI run for the exact candidate commit.", "identity_mismatch")
    ci_profile = profile.get("ci", {})
    assessment.check("ci.workflow.scope", bool(ci_profile) and record.get("workflow") == ci_profile.get("workflow"), ref,
                     "Actual CI workflow does not match the required release workflow.", "Provide the actual complete required release workflow execution.", "insufficient_scope")
    assessment.check("ci.conclusion", record.get("conclusion") == "success", ref, "Actual CI run did not conclude successfully.", "Resolve failed, pending, cancelled or skipped CI and collect a successful run.")
    jobs = record.get("jobs")
    assessment.check("ci.jobs.structure", isinstance(jobs, list) and bool(jobs) and all(isinstance(job, dict) and _text(job.get("name")) and _text(job.get("conclusion")) for job in jobs), ref,
                     "Actual CI jobs are missing, empty or malformed.", "Export complete job and step conclusions from the actual CI run.")
    job_map: dict[str, list[dict[str, Any]]] = {}
    if isinstance(jobs, list):
        for job in jobs:
            if isinstance(job, dict) and _text(job.get("name")):
                job_map.setdefault(job["name"], []).append(job)
    assessment.check("ci.jobs.unique", all(len(items) == 1 for items in job_map.values()), ref,
                     "CI job identities are duplicated.", "Provide an unambiguous original job export.")
    for job_name in ci_profile.get("requiredJobs", []):
        actual = job_map.get(job_name, [])
        assessment.check(f"ci.job.{job_name}", len(actual) == 1 and actual[0].get("conclusion") == "success", ref,
                         "Required release CI job is missing or not successful.", "Run and retain this required release check; smoke or skipped checks do not substitute.", "insufficient_scope" if not actual else "failed")
        steps = actual[0].get("steps") if len(actual) == 1 else None
        step_map: dict[str, list[dict[str, Any]]] = {}
        valid_steps = isinstance(steps, list) and bool(steps)
        if isinstance(steps, list):
            for step in steps:
                if not isinstance(step, dict) or not _text(step.get("name")) or not _text(step.get("conclusion")):
                    valid_steps = False
                    continue
                step_map.setdefault(step["name"], []).append(step)
        assessment.check(f"ci.job.{job_name}.steps.structure", valid_steps and all(len(items) == 1 for items in step_map.values()), ref,
                         "Required job steps are missing, malformed or duplicated.", "Export complete unique step conclusions for the required job.")
        for step_name in ci_profile.get("requiredSteps", {}).get(job_name, []):
            actual_steps = step_map.get(step_name, [])
            assessment.check(f"ci.job.{job_name}.step.{step_name}", len(actual_steps) == 1 and actual_steps[0].get("conclusion") == "success", ref,
                             "Required release CI step is missing or not successful.", "Execute and retain the required step's actual successful conclusion.", "insufficient_scope" if not actual_steps else "failed")


def _comparisons(assessment: Assessment, data: Any, required: list[str], identifier: str, ref: Any, accountant: bool) -> None:
    records = data if isinstance(data, list) else []
    valid = isinstance(data, list) and bool(data) and all(isinstance(entry, dict) and _text(entry.get("item")) for entry in records)
    assessment.check(f"{identifier}.structure", valid, ref, "Required comparison records are missing, empty or malformed.", "Provide the complete original item-by-item comparison records.")
    items: dict[str, list[dict[str, Any]]] = {}
    for entry in records:
        if not isinstance(entry, dict) or not _text(entry.get("item")):
            continue
        items.setdefault(entry["item"], []).append(entry)
        allowed = ("matched", "explained") if accountant else ("matched",)
        assessment.check(f"{identifier}.record.{len(items):03d}.{len(items[entry['item']]):03d}", entry.get("outcome") in allowed and (not accountant or entry.get("accountantApproved") is True), ref,
                         "Comparison is unresolved, failed or lacks accountant approval.", "Resolve the item and retain the reviewed comparison conclusion.", "pending_review" if accountant and entry.get("accountantApproved") is not True else "failed")
    assessment.check(f"{identifier}.unique", all(len(entries) == 1 for entries in items.values()), ref, "Comparison items are duplicated.", "Supply one unambiguous conclusion per required item.")
    for item in required:
        assessment.check(f"{identifier}.scope.{item}", len(items.get(item, [])) == 1, ref,
                         "Required comparison item is missing or duplicated.", "Complete and retain this required comparison.", "insufficient_scope")


def _accountant(assessment: Assessment, evidence: dict[str, Any], profile: dict[str, Any]) -> None:
    external = _external(assessment, evidence, "accountant")
    if external is None:
        return
    record, ref = external
    month = record.get("month")
    valid_month = isinstance(month, str) and re.fullmatch(r"\d{4}-(?:0[1-9]|1[0-2])", month) is not None and not month.startswith("0000")
    assessment.check("accountant.month", valid_month and record.get("realMonth") is True and record.get("completeMonth") is True, ref,
                     "Evidence does not establish a real complete accounting month.", "Perform and review one real complete month against the original accounting system.", "insufficient_scope")
    _comparisons(assessment, record.get("comparisons"), profile.get("requiredAccountingComparisons", []), "accountant.comparisons", ref, True)
    signoffs = record.get("signoffs")
    assessment.check("accountant.signoffs.structure", isinstance(signoffs, list) and bool(signoffs) and all(isinstance(item, dict) and _text(item.get("role")) for item in signoffs), ref,
                     "Human sign-off records are missing, empty or malformed.", "Obtain actual accountant, operator and technical sign-offs.", "pending_review")
    roles: dict[str, list[dict[str, Any]]] = {}
    if isinstance(signoffs, list):
        for index, signoff in enumerate(signoffs):
            identifier = f"accountant.signoffs.{index:03d}"
            if not isinstance(signoff, dict):
                continue
            if _text(signoff.get("role")):
                roles.setdefault(signoff["role"], []).append(signoff)
            assessment.fields(signoff, ["role", "name"], identifier, ref)
            assessment.check(f"{identifier}.signedAt", _time(signoff.get("signedAt")) is not None, ref,
                             "Actual sign-off date is missing or invalid.", "Record the actual signer's signing date.", "pending_review")
            assessment.reference(signoff.get("evidence"), f"{identifier}.evidence")
    assessment.check("accountant.signoffs.unique", all(len(entries) == 1 for entries in roles.values()), ref,
                     "Human sign-off roles are duplicated.", "Provide one actual sign-off for each required role.")
    for role in ("accountant", "operator", "technical"):
        assessment.check(f"accountant.signoffs.role.{role}", len(roles.get(role, [])) == 1, ref,
                         "Required human role has no unique actual sign-off.", "Obtain and retain this role's actual reviewed sign-off.", "pending_review")


def _restore(assessment: Assessment, evidence: dict[str, Any], profile: dict[str, Any]) -> None:
    external = _external(assessment, evidence, "restore")
    if external is None:
        return
    record, ref = external
    endpoints = {}
    for endpoint in ("source", "target"):
        value = record.get(endpoint)
        endpoints[endpoint] = value if isinstance(value, dict) else {}
        assessment.fields(endpoints[endpoint], ["instanceId", "datasourceId"], f"restore.{endpoint}", ref)
    source, target = endpoints["source"], endpoints["target"]
    assessment.check("restore.independent.instance", _text(source.get("instanceId")) and _text(target.get("instanceId")) and source["instanceId"] != target["instanceId"], ref,
                     "Restore does not establish an independent target backend instance.", "Restore into a second backend instance and retain its reviewed identity.", "insufficient_scope")
    assessment.check("restore.independent.datasource", _text(source.get("datasourceId")) and _text(target.get("datasourceId")) and source["datasourceId"] != target["datasourceId"], ref,
                     "Restore does not establish an independent target datasource.", "Restore into an independent target datasource and retain its reviewed identity.", "insufficient_scope")
    assessment.reference(record.get("backup"), "restore.backup")
    _comparisons(assessment, record.get("comparisons"), profile.get("requiredRestoreComparisons", []), "restore.comparisons", ref, False)


def _issues(assessment: Assessment, evidence: dict[str, Any]) -> None:
    external = _external(assessment, evidence, "issueReview")
    if external is None:
        return
    record, ref = external
    issues = record.get("issues")
    assessment.check("issueReview.issues.structure", isinstance(issues, list), ref,
                     "Candidate issue ledger dispositions are missing or malformed.", "Review the existing candidate issue ledger and explicitly record all issue dispositions.")
    identifiers: list[str] = []
    if isinstance(issues, list):
        for index, issue in enumerate(issues):
            identifier = f"issueReview.issues.{index:03d}"
            if not isinstance(issue, dict):
                assessment.add(identifier, "failed", ref, "Issue disposition must be an object.", "Record the complete reviewed issue disposition.")
                continue
            assessment.fields(issue, ["id"], identifier, ref)
            if _text(issue.get("id")):
                identifiers.append(issue["id"])
            valid = isinstance(issue.get("releaseBlocking"), bool) and issue.get("status") in ("open", "closed") and isinstance(issue.get("reviewed"), bool)
            assessment.check(f"{identifier}.structure", valid, ref, "Issue severity/status/reviewed fields are missing or invalid.", "Retain an explicit boolean release classification, valid status and review result.")
            if issue.get("releaseBlocking") is True:
                assessment.check(f"{identifier}.closure", issue.get("status") == "closed" and issue.get("reviewed") is True, ref,
                                 "A release-blocking issue is unresolved or its fix is unreviewed.", "Fix and review the blocking issue before collecting release-ready evidence.")
            elif issue.get("releaseBlocking") is False:
                assessment.fields(issue, ["owner", "resolutionPlan"], identifier, ref)
                if issue.get("status") == "closed":
                    assessment.check(f"{identifier}.closure", issue.get("reviewed") is True, ref,
                                     "A closed nonblocking issue lacks review.", "Review the resolution or retain it as open with its owner and plan.", "pending_review")
    assessment.check("issueReview.issues.unique", len(identifiers) == len(set(identifiers)), ref,
                     "Issue ledger identities are duplicated.", "Provide one reviewed disposition per ledger issue.")
    differences = record.get("differences")
    assessment.check("issueReview.differences.structure", isinstance(differences, list), ref,
                     "Amount difference review is missing or malformed.", "Explicitly review and record every amount difference, or an empty reviewed list.")
    difference_ids: list[str] = []
    if isinstance(differences, list):
        for index, difference in enumerate(differences):
            identifier = f"issueReview.differences.{index:03d}"
            if not isinstance(difference, dict):
                assessment.add(identifier, "failed", ref, "Amount difference must be a reviewed object.", "Provide the complete reviewed amount difference.")
                continue
            assessment.fields(difference, ["id", "explanation"], identifier, ref)
            if _text(difference.get("id")):
                difference_ids.append(difference["id"])
            assessment.check(f"{identifier}.accountantApproved", difference.get("accountantApproved") is True, ref,
                             "An amount difference lacks accountant approval.", "Explain the difference and obtain actual accountant approval.", "pending_review")
    assessment.check("issueReview.differences.unique", len(difference_ids) == len(set(difference_ids)), ref,
                     "Amount difference identities are duplicated.", "Provide one reviewed conclusion per amount difference.")


def assess_bundle(bundle_path: Path, profile_path: Path = DEFAULT_PROFILE) -> dict[str, Any]:
    """Assess parseable local evidence, collecting every applicable blocker.

    Only an unreadable/non-object/unsupported root raises BundleFormatError.
    Missing or malformed local sections are assessment blockers, not short circuits.
    """
    bundle_path, profile_path = Path(bundle_path), Path(profile_path)
    try:
        resolved = bundle_path.resolve(strict=True)
        bundle = _json_bytes(resolved.read_bytes())
    except (OSError, ValueError, UnicodeError, RuntimeError, RecursionError) as error:
        raise BundleFormatError("Bundle is unavailable or is not valid UTF-8 JSON with unique keys and finite numbers.") from error
    if not isinstance(bundle, dict) or type(bundle.get("schemaVersion")) is not int or bundle["schemaVersion"] != 1:
        raise BundleFormatError("Bundle root must be a JSON object with schemaVersion 1.")
    assessment = Assessment(resolved.parent)
    assessment.synthetic = bundle.get("synthetic") is True
    if "synthetic" in bundle:
        assessment.check("bundle.synthetic", isinstance(bundle["synthetic"], bool), "synthetic",
                         "Synthetic marker must be an explicit boolean.", "Use synthetic:true for fixtures and synthetic:false for actual supplied evidence.")
    candidate = _candidate(assessment, bundle.get("candidate"))
    profile = _profile(assessment, bundle.get("profile"), profile_path)
    evidence_value = bundle.get("evidence")
    evidence = evidence_value if isinstance(evidence_value, dict) else {}
    assessment.check("bundle.evidence", isinstance(evidence_value, dict), "evidence",
                     "Evidence object is absent or malformed.", "Provide the candidate-specific original evidence references.", "missing" if evidence_value is None else "failed")
    environments = _environments(assessment, evidence.get("environments"))
    _acceptance(assessment, evidence.get("acceptance"), profile, environments)
    _ci(assessment, evidence, profile)
    _accountant(assessment, evidence, profile)
    _restore(assessment, evidence, profile)
    _issues(assessment, evidence)
    conditions = sorted(assessment.conditions, key=lambda item: item["id"])
    synthetic = assessment.synthetic
    return {"schemaVersion": 1, "candidateId": candidate.get("candidateId") if isinstance(candidate.get("candidateId"), str) else None,
            "commitSha": candidate.get("commitSha") if isinstance(candidate.get("commitSha"), str) else None,
            "releaseReady": bool(conditions) and all(condition["status"] == "satisfied" for condition in conditions),
            "synthetic": synthetic, "conditions": conditions,
            "trustBoundary": TRUST_BOUNDARY + (" This is synthetic fixture evidence: success proves verifier behavior only and does not establish a real candidate's release readiness." if synthetic else "")}


def render_markdown(result: dict[str, Any]) -> str:
    """Render the same stable assessment without exposing original business data."""
    def escape(value: Any) -> str:
        return str(value if value is not None else "").replace("\\", "\\\\").replace("|", "\\|").replace("\r", " ").replace("\n", " ").replace("<", "&lt;").replace(">", "&gt;")

    lines = ["# Release candidate evidence assessment", "", f"Candidate: {escape(result.get('candidateId'))}",
             f"Commit: {escape(result.get('commitSha'))}",
             f"Evidence ready: {'yes' if result.get('releaseReady') is True else 'no'}", ""]
    if result.get("synthetic") is True:
        lines.extend(["**Synthetic fixture: this result verifies tool behavior only; it is not real release evidence.**", ""])
    lines.extend(["| Condition | Status | Evidence | Reason | Next action |", "| --- | --- | --- | --- | --- |"])
    for condition in sorted(result.get("conditions", []), key=lambda item: item["id"]):
        lines.append("| " + " | ".join(escape(condition.get(key)) for key in ("id", "status", "evidenceRef", "reason", "nextAction")) + " |")
    lines.extend(["", "Trust boundary: " + escape(result.get("trustBoundary")), ""])
    return "\n".join(lines)


def _output_paths(paths: list[str | None], bundle_path: Path, profile_path: Path) -> list[Path | None]:
    """Validate every output before any write; the entire bundle is protected."""
    bundle_root = bundle_path.resolve(strict=True).parent
    profile = profile_path.resolve()
    outputs: list[Path | None] = []
    destinations: set[Path] = set()
    for value in paths:
        if value is None:
            outputs.append(None)
            continue
        output = Path(value).resolve()
        if output.is_relative_to(bundle_root) or output == profile:
            raise BundleFormatError("Report output must be outside the evidence bundle and must not overwrite the trusted profile.")
        if output in destinations:
            raise BundleFormatError("JSON and Markdown outputs must use distinct paths.")
        if output.exists():
            # Hard links can alias evidence without having a contained path.
            if not output.is_file():
                raise BundleFormatError("Report output must name a regular file.")
            for evidence_file in bundle_root.rglob("*"):
                if evidence_file.is_file() and output.samefile(evidence_file):
                    raise BundleFormatError("Report output aliases an evidence file.")
            if profile.exists() and output.samefile(profile):
                raise BundleFormatError("Report output aliases the trusted profile.")
        if not output.parent.exists() or not output.parent.is_dir():
            raise BundleFormatError("Explicit report output parent directory must already exist.")
        destinations.add(output)
        outputs.append(output)
    return outputs


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bundle", required=True, type=Path, help="Local version 1 candidate JSON; its parent is the evidence root.")
    parser.add_argument("--profile", type=Path, default=DEFAULT_PROFILE, help="Explicit trusted local profile (default: version-controlled profile).")
    parser.add_argument("--json-output", help="Explicit JSON report destination outside the bundle (existing parent required).")
    parser.add_argument("--markdown-output", help="Explicit Markdown report destination outside the bundle (existing parent required).")
    args = parser.parse_args(argv)
    try:
        result = assess_bundle(args.bundle, args.profile)
        outputs = _output_paths([args.json_output, args.markdown_output], args.bundle, args.profile) if args.json_output or args.markdown_output else [None, None]
        encoded = json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
        markdown = render_markdown(result)
        for output, content in zip(outputs, [encoded, markdown]):
            if output is not None:
                output.write_text(content, encoding="utf-8")
    except (BundleFormatError, OSError, ValueError, RuntimeError) as error:
        print(f"Invalid input/output: {error}", file=sys.stderr)
        return 2
    print(encoded, end="")
    return 0 if result["releaseReady"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
