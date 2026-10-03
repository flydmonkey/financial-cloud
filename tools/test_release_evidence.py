"""Independent offline protocol tests, using only temporary synthetic files.

Run with ``python -m unittest discover -s tools -p test_release_evidence.py``.
No acceptance command, service, database, network client, or Git command runs.
"""
from __future__ import annotations

import ast
from contextlib import redirect_stderr, redirect_stdout
from copy import deepcopy
import hashlib
import html
import io
import json
import os
from pathlib import Path
import re
import tempfile
import unittest
from unittest import mock

import release_evidence_fixtures as fixtures
import verify_release_evidence as verifier


def digest_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def all_specs(report: dict):
    def visit(suites):
        for suite in suites:
            yield from suite.get("specs", [])
            yield from visit(suite.get("suites", []))
    return list(visit(report["suites"]))


def first_spec_list(report: dict) -> list[dict]:
    def visit(suites):
        for suite in suites:
            if suite.get("specs"):
                return suite["specs"]
            found = visit(suite.get("suites", []))
            if found is not None:
                return found
        return None
    return visit(report["suites"])


class ReleaseEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="jinbooks-synthetic-evidence-")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.bundle_path = fixtures.create_fixture(self.root / "bundle")
        self.bundle = json.loads(self.bundle_path.read_text(encoding="utf-8"))

    def save_bundle(self):
        self.bundle_path.write_text(json.dumps(self.bundle, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")

    def edit_record(self, reference: dict, change):
        path = self.bundle_path.parent / reference["path"]
        record = json.loads(path.read_text(encoding="utf-8"))
        change(record)
        path.write_text(json.dumps(record, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        reference["sha256"] = digest_file(path)
        self.save_bundle()
        return record

    def edit_external(self, kind: str, change):
        return self.edit_record(self.bundle["evidence"][kind], change)

    def edit_summary(self, change, index: int = 0):
        return self.edit_record(self.bundle["evidence"]["acceptance"][index]["provenance"], change)

    def edit_report(self, change, index: int = 0):
        run = self.bundle["evidence"]["acceptance"][index]
        report = self.edit_record(run["report"], change)
        # Rebind the original summary hash too: semantic negative examples must
        # reach raw report validation, rather than pass only a tampering check.
        self.edit_summary(lambda summary: summary["groups"][0]["report"].update(sha256=run["report"]["sha256"]), index)
        return report

    def assess(self):
        self.save_bundle()
        return verifier.assess_bundle(self.bundle_path)

    def assert_blocked(self, prefix: str, statuses: set[str] | None = None):
        result = self.assess()
        self.assertFalse(result["releaseReady"])
        blockers = [condition for condition in result["conditions"]
                    if condition["id"].startswith(prefix) and condition["status"] != "satisfied"]
        self.assertTrue(blockers, f"Expected blockers for {prefix}; got {result}")
        if statuses:
            self.assertTrue(any(condition["status"] in statuses for condition in blockers), blockers)
        for condition in blockers:
            self.assertTrue(condition["reason"])
            self.assertTrue(condition["nextAction"])
        return result

    def cli(self, *extra):
        stdout, stderr = io.StringIO(), io.StringIO()
        with redirect_stdout(stdout), redirect_stderr(stderr):
            code = verifier.main(["--bundle", str(self.bundle_path), *map(str, extra)])
        return code, stdout.getvalue(), stderr.getvalue()

    def file_snapshot(self):
        return {path.relative_to(self.bundle_path.parent).as_posix(): digest_file(path)
                for path in self.bundle_path.parent.rglob("*") if path.is_file()}

    def test_complete_synthetic_bundle_covers_all_trusted_cases(self):
        profile = json.loads(fixtures.DEFAULT_PROFILE.read_text(encoding="utf-8"))
        actual_count = 0
        for run, group in zip(self.bundle["evidence"]["acceptance"], profile["groups"]):
            report = json.loads((self.bundle_path.parent / run["report"]["path"]).read_text(encoding="utf-8"))
            self.assertEqual(run["group"], group["name"])
            self.assertEqual(len(all_specs(report)), len(group["cases"]))
            actual_count += len(all_specs(report))
        self.assertEqual(actual_count, sum(len(group["cases"]) for group in profile["groups"]))
        result = self.assess()
        self.assertTrue(result["releaseReady"], [c for c in result["conditions"] if c["status"] != "satisfied"])
        self.assertTrue(result["synthetic"])
        self.assertIn("synthetic", result["trustBoundary"].lower())
        self.assertIn("cannot independently authenticate", result["trustBoundary"].lower())
        self.assertTrue(all(c["status"] == "satisfied" for c in result["conditions"]))

    def test_reviewed_profile_update_defines_case_count_without_fixed_totals(self):
        profile = json.loads(fixtures.DEFAULT_PROFILE.read_text(encoding="utf-8"))
        extra = deepcopy(profile["groups"][0]["cases"][0])
        extra["titlePath"][-1] = "Synthetic additional case in an explicitly reviewed profile"
        profile["groups"][0]["cases"].append(extra)
        updated_profile = self.root / "reviewed-profile.json"
        updated_profile.write_text(json.dumps(profile, ensure_ascii=False, indent=2), encoding="utf-8")
        bundle_path = fixtures.create_fixture(self.root / "updated-profile-fixture", profile_path=updated_profile)
        result = verifier.assess_bundle(bundle_path, profile_path=updated_profile)
        self.assertTrue(result["releaseReady"], [c for c in result["conditions"] if c["status"] != "satisfied"])

    def test_original_synthetic_marker_cannot_be_removed_by_root_relabeling(self):
        self.bundle.pop("synthetic")
        result = self.assess()
        self.assertTrue(result["synthetic"])
        self.assertIn("synthetic", result["trustBoundary"].lower())

    def test_condition_contract_and_deterministic_order(self):
        result = self.assess()
        identifiers = [item["id"] for item in result["conditions"]]
        self.assertEqual(identifiers, sorted(identifiers))
        self.assertEqual(len(identifiers), len(set(identifiers)))
        for item in result["conditions"]:
            self.assertEqual(set(item), {"id", "status", "evidenceRef", "reason", "nextAction"})
            self.assertIn(item["status"], {"satisfied", "missing", "failed", "identity_mismatch", "insufficient_scope", "pending_review"})
        self.assertEqual(result, self.assess())

    def test_missing_candidate_identity(self):
        del self.bundle["candidate"]["commitSha"]
        self.assert_blocked("candidate", {"missing"})

    def test_dirty_source(self):
        self.bundle["candidate"]["source"]["state"] = "dirty"
        self.assert_blocked("candidate.source", {"failed"})

    def test_unknown_source(self):
        self.bundle["candidate"]["source"]["state"] = "unknown"
        self.assert_blocked("candidate.source", {"failed"})

    def test_missing_production_frontend(self):
        del self.bundle["candidate"]["artifacts"]["frontend"]
        self.assert_blocked("candidate.artifacts.frontend", {"missing", "insufficient_scope"})

    def test_development_frontend(self):
        self.bundle["candidate"]["artifacts"]["frontend"]["kind"] = "vite-development"
        self.assert_blocked("candidate.artifacts.frontend", {"insufficient_scope"})

    def test_acceptance_missing_identity(self):
        del self.bundle["evidence"]["acceptance"][0]["identity"]["candidateId"]
        self.assert_blocked("acceptance", {"missing"})

    def test_historical_acceptance_sha(self):
        self.bundle["evidence"]["acceptance"][0]["identity"]["commitSha"] = "0" * 40
        self.assert_blocked("acceptance", {"identity_mismatch"})

    def test_acceptance_backend_artifact_mismatch(self):
        self.bundle["evidence"]["acceptance"][0]["identity"]["backendSha256"] = "0" * 64
        self.assert_blocked("acceptance", {"identity_mismatch"})

    def test_acceptance_frontend_artifact_mismatch(self):
        self.bundle["evidence"]["acceptance"][0]["identity"]["frontendSha256"] = "0" * 64
        self.assert_blocked("acceptance", {"identity_mismatch"})

    def test_missing_group(self):
        self.bundle["evidence"]["acceptance"].pop()
        self.assert_blocked("acceptance.scope.book-isolation", {"insufficient_scope"})

    def test_duplicate_group(self):
        self.bundle["evidence"]["acceptance"].append(deepcopy(self.bundle["evidence"]["acceptance"][0]))
        self.assert_blocked("acceptance", {"failed", "insufficient_scope"})

    def test_missing_case_with_consistent_green_count(self):
        def change(report):
            first_spec_list(report).pop(0)
            report["stats"]["expected"] -= 1
        self.edit_report(change)
        self.assert_blocked("acceptance.000", {"insufficient_scope"})

    def test_duplicate_case_cannot_replace_missing_case(self):
        def change(report):
            specs = first_spec_list(report)
            specs[1] = deepcopy(specs[0])
        self.edit_report(change)
        self.assert_blocked("acceptance.000", {"failed", "insufficient_scope"})

    def test_extra_duplicate_case_cannot_inflate_pass_count(self):
        def change(report):
            specs = first_spec_list(report)
            specs.append(deepcopy(specs[0]))
            report["stats"]["expected"] += 1
        self.edit_report(change)
        self.assert_blocked("acceptance.000", {"failed", "insufficient_scope"})

    def test_wrong_project_case(self):
        self.edit_report(lambda report: all_specs(report)[0]["tests"][0].update(projectName="firefox", projectId="firefox"))
        self.assert_blocked("acceptance.000", {"insufficient_scope"})

    def test_raw_stats_missing_field_is_not_zero(self):
        for field in ("expected", "unexpected", "skipped", "flaky", "startTime", "duration"):
            with self.subTest(field=field):
                original = json.loads((self.bundle_path.parent / self.bundle["evidence"]["acceptance"][0]["report"]["path"]).read_text(encoding="utf-8"))
                self.edit_report(lambda report: report["stats"].pop(field))
                self.assert_blocked("acceptance.000", {"failed", "missing"})
                self.edit_report(lambda report: (report.clear(), report.update(original)))

    def test_green_summary_without_raw_report_is_insufficient(self):
        self.edit_report(lambda report: (report.clear(), report.update({"summary": {"passed": True, "total": 75}})))
        self.assert_blocked("acceptance.000", {"failed", "insufficient_scope", "missing"})

    def test_raw_stats_must_agree_with_actual_results(self):
        self.edit_report(lambda report: report["stats"].update(expected=999))
        self.assert_blocked("acceptance.000", {"failed"})

    def test_report_tampering_is_not_accepted(self):
        reference = self.bundle["evidence"]["acceptance"][0]["report"]
        path = self.bundle_path.parent / reference["path"]
        path.write_bytes(path.read_bytes() + b" ")
        self.assert_blocked("acceptance.000", {"failed"})

    def test_skipped_case_is_blocked_even_if_expected_skip(self):
        def change(report):
            test = all_specs(report)[0]["tests"][0]
            test.update(expectedStatus="skipped", status="skipped")
            test["results"][0]["status"] = "skipped"
            report["stats"].update(expected=report["stats"]["expected"] - 1, skipped=1)
        self.edit_report(change)
        self.assert_blocked("acceptance.000", {"failed"})

    def test_failed_case(self):
        def change(report):
            test = all_specs(report)[0]["tests"][0]
            test["status"] = "unexpected"
            test["results"][0].update(status="failed", errors=[{"message": "synthetic failure"}])
            report["stats"].update(expected=report["stats"]["expected"] - 1, unexpected=1)
        self.edit_report(change)
        self.assert_blocked("acceptance.000", {"failed"})

    def test_flaky_failed_then_passed_case(self):
        def change(report):
            test = all_specs(report)[0]["tests"][0]
            passed = deepcopy(test["results"][0])
            test["results"][0].update(status="failed", errors=[{"message": "synthetic failure"}])
            passed["retry"] = 1
            test["results"].append(passed)
            test["status"] = "flaky"
            report["stats"].update(expected=report["stats"]["expected"] - 1, flaky=1)
        self.edit_report(change)
        self.assert_blocked("acceptance.000", {"failed"})

    def test_retry_only_green_case(self):
        self.edit_report(lambda report: all_specs(report)[0]["tests"][0]["results"][0].update(retry=1))
        self.assert_blocked("acceptance.000", {"failed"})

    def test_unfinished_case(self):
        self.edit_report(lambda report: all_specs(report)[0]["tests"][0].update(results=[]))
        self.assert_blocked("acceptance.000", {"failed"})

    def test_interrupted_case(self):
        self.edit_report(lambda report: all_specs(report)[0]["tests"][0]["results"][0].update(status="interrupted"))
        self.assert_blocked("acceptance.000", {"failed"})

    def test_report_level_error(self):
        self.edit_report(lambda report: report["errors"].append({"message": "synthetic global setup failed"}))
        self.assert_blocked("acceptance.000", {"failed"})

    def test_report_start_time_cannot_repackage_historical_run(self):
        self.edit_report(lambda report: report["stats"].update(startTime="2025-01-01T00:00:00Z"))
        self.assert_blocked("acceptance.000", {"failed", "identity_mismatch"})

    def test_missing_completion_timestamp(self):
        self.bundle["evidence"]["acceptance"][0].pop("finishedAt")
        self.assert_blocked("acceptance.000", {"missing", "failed"})

    def test_nonzero_acceptance_exit(self):
        self.bundle["evidence"]["acceptance"][0]["exitCode"] = 1
        self.assert_blocked("acceptance.000", {"failed"})

    def test_missing_original_run_provenance(self):
        self.bundle["evidence"]["acceptance"][0].pop("provenance")
        self.assert_blocked("acceptance.000", {"missing"})

    def test_original_run_keeps_old_sha(self):
        self.edit_summary(lambda summary: summary["sourceBefore"].update(commitSha="0" * 40))
        self.assert_blocked("acceptance.000", {"identity_mismatch", "failed"})

    def test_original_run_dirty_source(self):
        self.edit_summary(lambda summary: summary["sourceBefore"].update(state="dirty", gitStatus={"tracked": [{"path": "synthetic_source.py", "status": " M"}], "untracked": []}))
        self.assert_blocked("acceptance.000", {"failed", "identity_mismatch"})

    def test_original_source_changes_during_run(self):
        self.edit_summary(lambda summary: (summary.update(sourceChanged=True), summary["sourceAfter"].update(fingerprint="0" * 64)))
        self.assert_blocked("acceptance.000", {"failed", "identity_mismatch"})

    def test_original_source_manifest_cannot_fake_fingerprint(self):
        self.edit_summary(lambda summary: summary["sourceBefore"]["files"][0].update(size=999))
        self.assert_blocked("acceptance.000", {"failed", "identity_mismatch"})

    def test_original_artifact_changes_during_run(self):
        self.edit_summary(lambda summary: (summary.update(artifactsChanged=True), summary["artifactsAfter"]["backend"].update(sha256="0" * 64)))
        self.assert_blocked("acceptance.000", {"failed", "identity_mismatch"})

    def test_original_summary_report_hash_mismatch(self):
        self.edit_summary(lambda summary: summary["groups"][0]["report"].update(sha256="0" * 64))
        self.assert_blocked("acceptance.000", {"failed", "identity_mismatch"})

    def test_original_summary_unfinished(self):
        self.edit_summary(lambda summary: summary.update(status="running", finishedAt=None))
        self.assert_blocked("acceptance.000", {"failed", "missing"})

    def test_declared_environment_is_not_observed_binding(self):
        reference = self.bundle["evidence"]["environments"][0]
        self.edit_record(reference, lambda environment: environment["observation"].update(method="declared"))
        self.assert_blocked("environments", {"insufficient_scope"})

    def test_environment_binding_needs_approved_review(self):
        self.edit_record(self.bundle["evidence"]["environments"][0], lambda environment: environment["review"].update(conclusion="pending"))
        self.assert_blocked("environments", {"pending_review"})

    def test_environment_artifact_mismatch(self):
        self.edit_record(self.bundle["evidence"]["environments"][0], lambda environment: environment["identity"].update(backendSha256="0" * 64))
        self.assert_blocked("environments", {"identity_mismatch"})

    def test_environment_without_reviewed_run_bindings(self):
        self.edit_record(self.bundle["evidence"]["environments"][0], lambda environment: environment.pop("runs"))
        self.assert_blocked("environments", {"missing", "insufficient_scope", "failed"})

    def test_original_summary_change_requires_new_environment_review(self):
        self.edit_summary(lambda summary: summary.update(api="https://other.example.invalid/api"))
        self.assert_blocked("acceptance.000", {"identity_mismatch", "failed", "insufficient_scope"})

    def test_using_unobserved_environment_for_acceptance(self):
        self.bundle["evidence"]["acceptance"][0]["environmentId"] = "unobserved-other-environment"
        self.assert_blocked("acceptance.000", {"missing", "insufficient_scope", "failed"})

    def test_environment_run_binding_for_other_run(self):
        self.edit_record(self.bundle["evidence"]["environments"][0], lambda environment: environment["runs"][0].update(runId="other-run"))
        self.assert_blocked("acceptance.000", {"identity_mismatch", "failed", "insufficient_scope"})

    def test_rebound_summary_still_requires_matching_observed_endpoints_and_database(self):
        run = self.bundle["evidence"]["acceptance"][0]
        original_summary = json.loads((self.bundle_path.parent / run["provenance"]["path"]).read_text(encoding="utf-8"))
        for field in ("api", "frontend", "database"):
            with self.subTest(field=field):
                value = "other_database" if field == "database" else "https://other.example.invalid"
                self.edit_summary(lambda summary: (summary.update({field: value}), summary["declaredEnvironment"].update({field: value})))
                self.edit_record(self.bundle["evidence"]["environments"][0], lambda environment: environment["runs"][0]["provenance"].update(sha256=run["provenance"]["sha256"]))
                self.assert_blocked("acceptance.000", {"identity_mismatch", "failed", "insufficient_scope"})
                self.edit_summary(lambda summary: (summary.clear(), summary.update(deepcopy(original_summary))))

    def test_declared_environment_cannot_contradict_original_run(self):
        run = self.bundle["evidence"]["acceptance"][0]
        self.edit_summary(lambda summary: summary["declaredEnvironment"].update(database="different_from_original_run"))
        self.edit_record(self.bundle["evidence"]["environments"][0], lambda environment: environment["runs"][0]["provenance"].update(sha256=run["provenance"]["sha256"]))
        self.assert_blocked("acceptance.000", {"identity_mismatch", "failed", "insufficient_scope"})

    def test_declared_datasource_host_port_must_match_observation(self):
        run = self.bundle["evidence"]["acceptance"][0]
        self.edit_summary(lambda summary: summary["declaredEnvironment"].update(databaseHost="other-database.invalid", databasePort=3308))
        self.edit_record(self.bundle["evidence"]["environments"][0], lambda environment: environment["runs"][0]["provenance"].update(sha256=run["provenance"]["sha256"]))
        self.assert_blocked("acceptance.000", {"identity_mismatch", "failed", "insufficient_scope"})

    def test_ci_smoke_scope(self):
        self.edit_external("ci", lambda ci: ci.update(workflow="pr-smoke.yml", jobs=[ci["jobs"][0]]))
        self.assert_blocked("ci", {"insufficient_scope", "failed"})

    def test_ci_old_head_sha(self):
        self.edit_external("ci", lambda ci: ci.update(headSha="0" * 40))
        self.assert_blocked("ci", {"identity_mismatch"})

    def test_ci_failed_conclusion(self):
        self.edit_external("ci", lambda ci: ci.update(conclusion="failure"))
        self.assert_blocked("ci", {"failed"})

    def test_ci_skipped_job(self):
        self.edit_external("ci", lambda ci: ci["jobs"][-1].update(conclusion="skipped"))
        self.assert_blocked("ci", {"failed"})

    def test_ci_skipped_step(self):
        self.edit_external("ci", lambda ci: ci["jobs"][-1]["steps"][-1].update(conclusion="skipped"))
        self.assert_blocked("ci", {"failed"})

    def test_pending_month_review(self):
        self.edit_external("accountant", lambda accountant: accountant["review"].update(conclusion="pending"))
        self.assert_blocked("accountant", {"pending_review"})

    def test_incomplete_or_test_month(self):
        self.edit_external("accountant", lambda accountant: accountant.update(realMonth=False, completeMonth=False))
        self.assert_blocked("accountant", {"insufficient_scope"})

    def test_missing_human_role(self):
        self.edit_external("accountant", lambda accountant: accountant["signoffs"].pop())
        self.assert_blocked("accountant.signoffs.role.technical", {"pending_review"})

    def test_missing_actual_signature_material(self):
        self.edit_external("accountant", lambda accountant: accountant["signoffs"][0].pop("evidence"))
        self.assert_blocked("accountant.signoffs", {"missing"})

    def test_month_difference_requires_accountant_approval(self):
        self.edit_external("accountant", lambda accountant: accountant["comparisons"][0].update(outcome="explained", accountantApproved=False))
        self.assert_blocked("accountant.comparisons", {"pending_review"})

    def test_same_backend_instance_restore(self):
        self.edit_external("restore", lambda restore: restore["target"].update(instanceId=restore["source"]["instanceId"]))
        self.assert_blocked("restore.independent.instance", {"insufficient_scope"})

    def test_same_datasource_restore(self):
        self.edit_external("restore", lambda restore: restore["target"].update(datasourceId=restore["source"]["datasourceId"]))
        self.assert_blocked("restore.independent.datasource", {"insufficient_scope"})

    def test_restore_missing_cash_flow(self):
        self.edit_external("restore", lambda restore: restore.update(comparisons=[item for item in restore["comparisons"] if item["item"] != "cashFlow"]))
        self.assert_blocked("restore.comparisons.scope.cashFlow", {"insufficient_scope"})

    def test_open_release_blocker_overrides_other_green_evidence(self):
        self.edit_external("issueReview", lambda review: review["issues"].append({"id": "SYNTHETIC-LOST-DATA", "releaseBlocking": True, "status": "open", "reviewed": True, "owner": "Synthetic technical reviewer", "resolutionPlan": "Repair restored data loss"}))
        result = self.assert_blocked("issueReview.issues", {"failed"})
        self.assertTrue(all(c["status"] == "satisfied" for c in result["conditions"] if not c["id"].startswith("issueReview")))

    def test_closed_blocker_without_fix_review_is_blocked(self):
        self.edit_external("issueReview", lambda review: review["issues"].append({"id": "SYNTHETIC-AMOUNT-ERROR", "releaseBlocking": True, "status": "closed", "reviewed": False, "owner": "Synthetic reviewer", "resolutionPlan": "Review amount correction"}))
        self.assert_blocked("issueReview.issues", {"failed"})

    def test_missing_issue_ledger_review(self):
        self.bundle["evidence"].pop("issueReview")
        self.assert_blocked("issueReview", {"missing"})

    def test_unapproved_issue_amount_difference(self):
        self.edit_external("issueReview", lambda review: review["differences"].append({"id": "SYNTHETIC-DIFFERENCE", "explanation": "Synthetic rounding difference", "accountantApproved": False}))
        self.assert_blocked("issueReview.differences", {"pending_review"})

    def test_unexplained_issue_amount_difference(self):
        self.edit_external("issueReview", lambda review: review["differences"].append({"id": "SYNTHETIC-DIFFERENCE", "explanation": "", "accountantApproved": True}))
        self.assert_blocked("issueReview.differences", {"failed", "missing"})

    def test_nonblocking_open_issue_keeps_owner_and_plan(self):
        self.edit_external("issueReview", lambda review: review["issues"][0].pop("resolutionPlan"))
        self.assert_blocked("issueReview.issues", {"missing"})

    def test_missing_support_material_cannot_be_replaced_by_review_flag(self):
        self.edit_external("ci", lambda ci: ci.pop("supportingEvidence"))
        self.assert_blocked("ci", {"missing"})

    def test_bundle_supplied_profile_cannot_reduce_trusted_scope(self):
        reduced = json.loads(fixtures.DEFAULT_PROFILE.read_text(encoding="utf-8"))
        reduced["groups"] = reduced["groups"][:1]
        reduced_path = self.bundle_path.parent / "reduced-profile.json"
        reduced_path.write_text(json.dumps(reduced), encoding="utf-8")
        self.bundle["profile"].update(sha256=digest_file(reduced_path), path="reduced-profile.json", groups=reduced["groups"])
        self.bundle["evidence"]["acceptance"] = self.bundle["evidence"]["acceptance"][:1]
        result = self.assert_blocked("profile.binding.sha256", {"identity_mismatch"})
        self.assertTrue(any(c["id"] == "acceptance.scope.book-isolation" and c["status"] != "satisfied" for c in result["conditions"]))

    def test_frontend_manifest_identity_mismatch(self):
        self.edit_record(self.bundle["candidate"]["artifacts"]["frontend"]["manifest"], lambda manifest: manifest.update(sha256="0" * 64))
        self.assert_blocked("candidate.artifacts.frontend.manifest", {"identity_mismatch"})

    def test_frontend_manifest_duplicate_and_unsafe_paths(self):
        self.edit_record(self.bundle["candidate"]["artifacts"]["frontend"]["manifest"], lambda manifest: manifest["files"].append({"path": "../escape", "size": 1, "sha256": "0" * 64}))
        self.assert_blocked("candidate.artifacts.frontend.manifest", {"failed"})

    def test_unsafe_reference_forms(self):
        paths = [str(self.root / "outside.json"), "/absolute.json", "\\\\server\\share\\ci.json", "C:relative.json", "../outside.json", "external/../ci.json"]
        for path in paths:
            with self.subTest(path=path):
                self.bundle["evidence"]["ci"]["path"] = path
                self.assert_blocked("ci.integrity", {"failed"})

    def test_parent_traversal_rejected_before_external_file_read(self):
        outside = self.root / "outside.json"
        outside.write_text('{"sensitive": "must never be read"}', encoding="utf-8")
        self.bundle["evidence"]["ci"] = {"path": "../outside.json", "sha256": digest_file(outside)}
        self.save_bundle()
        original_read = Path.read_bytes
        def guarded_read(path):
            self.assertNotEqual(path.resolve(), outside.resolve(), "Verifier opened evidence outside the bundle")
            return original_read(path)
        with mock.patch.object(Path, "read_bytes", guarded_read):
            result = verifier.assess_bundle(self.bundle_path)
        self.assertFalse(result["releaseReady"])
        self.assertTrue(any(c["id"] == "ci.integrity" and c["status"] == "failed" for c in result["conditions"]))

    def test_symlink_or_windows_junction_escape_rejected_before_external_file_read(self):
        outside_directory = self.root / "outside-directory"
        outside_directory.mkdir()
        outside = outside_directory / "ci.json"
        outside.write_text('{"sensitive": "must never be read"}', encoding="utf-8")
        link = self.bundle_path.parent / "escaping-directory"
        try:
            os.symlink(outside_directory, link, target_is_directory=True)
        except OSError as error:
            if os.name != "nt":
                self.skipTest(f"Host cannot create symlinks: {error}")
            # A Windows directory junction is a native reparse link with the
            # same resolved-root escape. It does not require symlink privilege.
            import _winapi
            _winapi.CreateJunction(str(outside_directory), str(link))
            self.addCleanup(link.rmdir)
        self.bundle["evidence"]["ci"] = {"path": link.name + "/ci.json", "sha256": digest_file(outside)}
        self.save_bundle()
        original_read = Path.read_bytes
        def guarded_read(path):
            self.assertNotEqual(path.resolve(), outside.resolve(), "Verifier opened an escaping symlink")
            return original_read(path)
        with mock.patch.object(Path, "read_bytes", guarded_read):
            result = verifier.assess_bundle(self.bundle_path)
        self.assertFalse(result["releaseReady"])
        self.assertTrue(any(c["id"] == "ci.integrity" and c["status"] == "failed" for c in result["conditions"]))

    def test_missing_referenced_file(self):
        (self.bundle_path.parent / self.bundle["evidence"]["ci"]["path"]).unlink()
        self.assert_blocked("ci.integrity", {"missing"})

    def test_multiple_missing_external_items_are_all_reported(self):
        for name in ("ci", "accountant", "restore"):
            self.bundle["evidence"].pop(name)
        result = self.assess()
        for name in ("ci", "accountant", "restore"):
            self.assertTrue(any(c["id"].startswith(name) and c["status"] == "missing" for c in result["conditions"]), name)
        self.assertFalse(result["releaseReady"])

    def test_default_cli_is_read_only_and_exits_zero(self):
        before = self.file_snapshot()
        code, output, errors = self.cli()
        self.assertEqual(code, 0, errors)
        self.assertEqual(errors, "")
        self.assertTrue(json.loads(output)["releaseReady"])
        self.assertEqual(self.file_snapshot(), before)
        self.assertEqual({path.name for path in self.root.iterdir()}, {"bundle"})

    def test_blocked_assessment_preserves_all_original_file_hashes(self):
        self.bundle["evidence"].pop("ci")
        self.save_bundle()
        before = self.file_snapshot()
        first = verifier.assess_bundle(self.bundle_path)
        second = verifier.assess_bundle(self.bundle_path)
        self.assertFalse(first["releaseReady"])
        self.assertEqual(first, second)
        self.assertEqual(self.file_snapshot(), before)

    def test_blocked_cli_exits_one(self):
        self.bundle["evidence"].pop("ci")
        self.save_bundle()
        code, output, errors = self.cli()
        self.assertEqual(code, 1)
        self.assertEqual(errors, "")
        self.assertFalse(json.loads(output)["releaseReady"])

    def test_invalid_root_cli_exits_two(self):
        for raw in (b"{invalid", b"[]", b'{"schemaVersion":999}', b'{"schemaVersion":true}', b'{"schemaVersion":1,"schemaVersion":1}'):
            with self.subTest(raw=raw):
                self.bundle_path.write_bytes(raw)
                code, output, errors = self.cli()
                self.assertEqual(code, 2)
                self.assertEqual(output, "")
                self.assertTrue(errors)

    def test_missing_root_cli_exits_two(self):
        self.bundle_path.unlink()
        code, output, errors = self.cli()
        self.assertEqual(code, 2)
        self.assertEqual(output, "")
        self.assertTrue(errors)

    def test_explicit_json_markdown_outputs_share_assessment(self):
        before = self.file_snapshot()
        json_output, md_output = self.root / "assessment.json", self.root / "assessment.md"
        code, output, errors = self.cli("--json-output", json_output, "--markdown-output", md_output)
        self.assertEqual(code, 0, errors)
        assessment = json.loads(output)
        self.assertEqual(json.loads(json_output.read_text(encoding="utf-8")), assessment)
        self.assertEqual(md_output.read_text(encoding="utf-8"), verifier.render_markdown(assessment))
        self.assertIn("Synthetic fixture", md_output.read_text(encoding="utf-8"))
        for condition in assessment["conditions"]:
            self.assertIn(condition["id"], md_output.read_text(encoding="utf-8"))
        self.assertEqual(self.file_snapshot(), before)

    def test_blocked_markdown_table_has_same_status_reason_and_next_action(self):
        for kind in ("ci", "accountant", "restore"):
            self.bundle["evidence"].pop(kind)
        result = self.assess()
        markdown = verifier.render_markdown(result)
        rows = [line for line in markdown.splitlines() if line.startswith("| ")][2:]
        def semantic_cell(cell):
            return html.unescape(re.sub(r"\\([\\|])", r"\1", cell))
        actual = [[semantic_cell(cell) for cell in row[2:-2].split(" | ")] for row in rows]
        expected = [[str(condition[key]) if condition[key] is not None else ""
                     for key in ("id", "status", "evidenceRef", "reason", "nextAction")]
                    for condition in result["conditions"]]
        self.assertEqual(actual, expected)
        self.assertIn("Evidence ready: no", markdown)

    def test_output_cannot_overwrite_evidence_or_trusted_profile(self):
        for destination in (self.bundle_path, self.bundle_path.parent / "new-report.json", fixtures.DEFAULT_PROFILE):
            with self.subTest(destination=str(destination)):
                before = self.file_snapshot()
                profile_before = digest_file(fixtures.DEFAULT_PROFILE)
                code, output, errors = self.cli("--json-output", destination)
                self.assertEqual(code, 2)
                self.assertEqual(output, "")
                self.assertTrue(errors)
                self.assertEqual(self.file_snapshot(), before)
                self.assertEqual(digest_file(fixtures.DEFAULT_PROFILE), profile_before)

    def test_outputs_prevalidated_before_any_report_is_written(self):
        first_output = self.root / "report.json"
        code, _, _ = self.cli("--json-output", first_output, "--markdown-output", self.bundle_path)
        self.assertEqual(code, 2)
        self.assertFalse(first_output.exists())

    def test_output_hard_link_alias_cannot_overwrite_evidence(self):
        alias = self.root / "candidate-alias.json"
        try:
            os.link(self.bundle_path, alias)
        except OSError as error:
            self.skipTest(f"Host cannot create hard links: {error}")
        before = self.file_snapshot()
        code, _, errors = self.cli("--json-output", alias)
        self.assertEqual(code, 2, errors)
        self.assertEqual(self.file_snapshot(), before)

    def test_verifier_has_no_runner_process_network_or_database_import(self):
        tree = ast.parse(Path(verifier.__file__).read_text(encoding="utf-8"))
        imports = []
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                imports.extend(alias.name for alias in node.names)
            elif isinstance(node, ast.ImportFrom):
                imports.append(node.module or "")
        forbidden = {"subprocess", "socket", "requests", "urllib", "http", "sqlite3", "pymysql", "mysql", "run_accounting_acceptance", "release_evidence_provenance"}
        self.assertFalse([name for name in imports if name.split(".")[0] in forbidden])

    def test_fixture_generator_requires_explicit_output_and_protects_existing_files(self):
        before = self.file_snapshot()
        with self.assertRaises(ValueError):
            fixtures.create_fixture(self.bundle_path.parent)
        self.assertEqual(self.file_snapshot(), before)
        stdout, stderr = io.StringIO(), io.StringIO()
        with redirect_stdout(stdout), redirect_stderr(stderr), self.assertRaises(SystemExit) as error:
            fixtures.main([])
        self.assertEqual(error.exception.code, 2)
        self.assertEqual(self.file_snapshot(), before)

    def test_missing_fixture_cli_is_explicitly_synthetic_and_blocks(self):
        output = self.root / "missing-fixture"
        stdout = io.StringIO()
        with redirect_stdout(stdout):
            code = fixtures.main(["--output", str(output), "--missing"])
        self.assertEqual(code, 0)
        self.assertTrue(json.loads(stdout.getvalue())["synthetic"])
        result = verifier.assess_bundle(output / "candidate.json")
        self.assertTrue(result["synthetic"])
        self.assertFalse(result["releaseReady"])
        for prefix in ("ci", "accountant", "restore"):
            self.assertTrue(any(c["id"].startswith(prefix) and c["status"] == "missing" for c in result["conditions"]))


if __name__ == "__main__":
    unittest.main()
