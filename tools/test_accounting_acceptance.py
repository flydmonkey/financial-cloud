"""Acceptance safety: invalid database never launches reset; skipped evidence fails."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace
from uuid import UUID

import run_accounting_acceptance as runner

ENVIRONMENT = {"FC_DB_NAME": "financial_cloud_e2e_test", "E2E_API_URL": "http://localhost:2254", "E2E_BASE_URL": "http://localhost:3254"}
GREEN_STATS = {"expected": 2, "skipped": 0, "unexpected": 0, "flaky": 0}


def source_fixture(fingerprint="a" * 64, state="clean"):
    return {"status": "collected", "commitSha": "b" * 40, "state": state,
            "fingerprint": fingerprint, "files": [],
            "gitStatus": {"tracked": [], "untracked": []}, "collectedAt": "2026-10-03T00:00:00+00:00"}


class AccountingAcceptanceSafetyTest(unittest.TestCase):
    def test_business_database_is_rejected_before_any_process_or_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with patch.dict(runner.os.environ, {"FC_DB_NAME": "financial_cloud"}, clear=True), patch.object(runner, "ROOT", root), patch.object(runner.subprocess, "run") as launch:
                self.assertEqual(runner.main(), 2)
                launch.assert_not_called()
                self.assertFalse((root / ".e2e-run").exists())

    def test_missing_explicit_service_addresses_are_rejected(self):
        with patch.dict(runner.os.environ, {"FC_DB_NAME": "financial_cloud_e2e_test"}, clear=True), patch.object(runner.subprocess, "run") as launch:
            self.assertEqual(runner.main(), 2)
            launch.assert_not_called()

    def execute_report(self, stats, exit_code=0):
        with tempfile.TemporaryDirectory() as directory:
            def launch(*_args, **kwargs):
                Path(kwargs["env"]["PLAYWRIGHT_JSON_OUTPUT_NAME"]).write_text(json.dumps({"stats": stats, "errors": []}), encoding="utf-8")
                return SimpleNamespace(stdout="fixture output", stderr="", returncode=exit_code)

            with patch.dict(runner.os.environ, ENVIRONMENT, clear=True), patch.object(runner, "ROOT", Path(directory)), patch.object(runner, "GROUPS", [("fixture", ["fixture.spec.ts"])]), patch.object(runner.shutil, "which", return_value="npx"), patch.object(runner, "collect_source_snapshot", return_value=source_fixture()), patch.object(runner.subprocess, "run", side_effect=launch):
                result = runner.main()
                summary_path = next((Path(directory) / ".e2e-run" / "accounting-acceptance").glob("*/summary.json"))
                summary = json.loads(summary_path.read_text(encoding="utf-8"))
                return result, summary

    def test_a_green_process_with_skipped_required_tests_fails(self):
        result, summary = self.execute_report({"expected": 1, "skipped": 1, "unexpected": 0, "flaky": 0})
        self.assertEqual(result, 1)
        self.assertFalse(summary["passed"])

    def test_no_executed_tests_cannot_pass(self):
        result, summary = self.execute_report({"expected": 0, "skipped": 0, "unexpected": 0, "flaky": 0})
        self.assertEqual(result, 1)
        self.assertFalse(summary["passed"])

    def test_passing_reports_still_require_accountant_signoff(self):
        result, summary = self.execute_report({"expected": 2, "skipped": 0, "unexpected": 0, "flaky": 0})
        self.assertEqual(result, 0)
        self.assertTrue(summary["passed"])
        self.assertEqual(summary["accountantSignOff"], "pending")

    def test_missing_result_fields_are_not_zero_failures(self):
        for key in GREEN_STATS:
            with self.subTest(key=key):
                stats = dict(GREEN_STATS)
                del stats[key]
                result, summary = self.execute_report(stats)
                self.assertEqual(result, 1)
                self.assertFalse(summary["groups"][0]["passed"])
                self.assertIn(key, summary["groups"][0]["errors"][0])

    def test_invalid_result_fields_cannot_pass(self):
        for value in (None, True, "0", -1, 0.0):
            with self.subTest(value=value):
                result, _summary = self.execute_report(dict(GREEN_STATS, unexpected=value))
                self.assertEqual(result, 1)


class AccountingAcceptanceProvenanceTest(unittest.TestCase):
    def run_fixture(self, reports, *, launch_error=None, sources=None, npx="npx", environment=None):
        """Only mocked launches run; no Git, Playwright or service is invoked."""
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            calls = []

            def launch(args, **kwargs):
                calls.append((args, kwargs["env"]["PLAYWRIGHT_JSON_OUTPUT_NAME"]))
                payload, exit_code = reports[len(calls) - 1]
                report = Path(kwargs["env"]["PLAYWRIGHT_JSON_OUTPUT_NAME"])
                if payload is not None:
                    report.write_text(json.dumps(payload), encoding="utf-8")
                if launch_error:
                    raise launch_error
                return SimpleNamespace(stdout="fixture output", stderr="", returncode=exit_code)

            groups = [(f"fixture-{index}", [f"fixture-{index}.spec.ts"]) for index in range(len(reports))]
            snapshots = sources or [source_fixture(), source_fixture()]
            with patch.dict(runner.os.environ, environment or ENVIRONMENT, clear=True), patch.object(runner, "ROOT", root), patch.object(runner, "GROUPS", groups), patch.object(runner.shutil, "which", return_value=npx), patch.object(runner, "collect_source_snapshot", side_effect=snapshots) as collect, patch.object(runner.subprocess, "run", side_effect=launch):
                result = runner.main()
            summary_path = next((root / ".e2e-run" / "accounting-acceptance").glob("*/summary.json"))
            summary = json.loads(summary_path.read_text(encoding="utf-8"))
            for group in summary["groups"]:
                for kind in ("report", "log"):
                    ref = group[kind]
                    path = summary_path.parent / ref["path"]
                    self.assertEqual(ref["sha256"], runner.sha256_file(path) if path.exists() else runner.UNKNOWN)
            self.assertEqual(collect.call_count, 2)
            return result, summary, calls

    def test_completed_run_has_run_identity_final_state_and_raw_hashes(self):
        result, summary, calls = self.run_fixture([({"stats": GREEN_STATS, "errors": []}, 0)] * 2)
        self.assertEqual(result, 0)
        self.assertEqual(summary["status"], "completed")
        self.assertEqual(summary["exitCode"], 0)
        self.assertEqual(str(UUID(summary["runId"])), summary["runId"])
        self.assertLessEqual(summary["startedAt"], summary["finishedAt"])
        self.assertFalse(summary["sourceChanged"])
        self.assertFalse(summary["releaseReady"])
        self.assertEqual(summary["reviewedBinding"], "unknown")
        self.assertEqual(summary["accountantSignOff"], "pending")
        self.assertEqual(summary["declaredEnvironment"]["provenance"], "declared")
        self.assertEqual(summary["artifacts"]["backend"]["sha256"], "unknown")
        self.assertEqual(len(calls), 2)
        self.assertEqual(len({path for _args, path in calls}), 2)
        for args, _path in calls:
            self.assertIn("--retries=0", args)
        self.assertTrue(all(group["status"] == "completed" and group["finishedAt"] for group in summary["groups"]))

    def test_partial_run_stops_at_failed_group_and_preserves_finished_evidence(self):
        reports = [({"stats": GREEN_STATS, "errors": []}, 0),
                   ({"stats": dict(GREEN_STATS, unexpected=1), "errors": []}, 1),
                   ({"stats": GREEN_STATS, "errors": []}, 0)]
        result, summary, calls = self.run_fixture(reports)
        self.assertEqual(result, 1)
        self.assertEqual(summary["status"], "failed")
        self.assertEqual(len(calls), 2)
        self.assertEqual(len(summary["plannedGroups"]), 3)
        self.assertEqual([group["passed"] for group in summary["groups"]], [True, False])
        self.assertIn("finishedAt", summary)
        self.assertIn("sourceAfter", summary)

    def test_launch_exception_keeps_partial_original_report_and_final_source(self):
        result, summary, calls = self.run_fixture(
            [({"stats": GREEN_STATS, "errors": []}, 0)], launch_error=OSError("fixture launch denied"))
        self.assertEqual(result, 1)
        self.assertEqual(summary["status"], "startup_failed")
        self.assertFalse(summary["passed"])
        group = summary["groups"][0]
        self.assertEqual(group["status"], "startup_failed")
        self.assertIsNone(group["exitCode"])
        self.assertNotEqual(group["report"]["sha256"], "unknown")
        self.assertIn("finishedAt", group)
        self.assertIn("sourceAfter", summary)
        self.assertEqual(len(calls), 1)

    def test_no_report_startup_failure_records_unknown_report_hash(self):
        result, summary, _calls = self.run_fixture([(None, 0)], launch_error=OSError("fixture"))
        self.assertEqual(result, 1)
        self.assertEqual(summary["groups"][0]["report"]["sha256"], "unknown")
        self.assertIn("finishedAt", summary)

    def test_missing_launcher_also_saves_finished_startup_failure(self):
        result, summary, calls = self.run_fixture([({"stats": GREEN_STATS, "errors": []}, 0)], npx=None)
        self.assertEqual(result, 2)
        self.assertEqual(summary["status"], "startup_failed")
        self.assertEqual(summary["exitCode"], 2)
        self.assertEqual(calls, [])
        self.assertIn("finishedAt", summary)

    def test_source_change_is_reported_without_preventing_rd_acceptance(self):
        result, summary, _calls = self.run_fixture(
            [({"stats": GREEN_STATS, "errors": []}, 0)],
            sources=[source_fixture(), source_fixture("c" * 64, "dirty")])
        self.assertEqual(result, 0)
        self.assertTrue(summary["passed"])
        self.assertTrue(summary["sourceChanged"])
        self.assertEqual(summary["sourceAfter"]["state"], "dirty")
        self.assertFalse(summary["releaseReady"])

    def test_unknown_source_still_allows_isolated_rd_acceptance(self):
        result, summary, _calls = self.run_fixture(
            [({"stats": GREEN_STATS, "errors": []}, 0)],
            sources=[runner.unknown_source("fixture"), runner.unknown_source("fixture")])
        self.assertEqual(result, 0)
        self.assertEqual(summary["sourceChanged"], "unknown")
        self.assertFalse(summary["releaseReady"])

    def test_separate_runs_have_separate_ids_and_paths(self):
        first = self.run_fixture([({"stats": GREEN_STATS, "errors": []}, 0)])
        second = self.run_fixture([({"stats": GREEN_STATS, "errors": []}, 0)])
        self.assertNotEqual(first[1]["runId"], second[1]["runId"])
        self.assertNotEqual(first[2][0][1], second[2][0][1])

    def test_exported_declared_urls_remove_credentials_and_parameters(self):
        environment = dict(ENVIRONMENT, E2E_API_URL="http://user:secret@localhost:2254/api?token=hidden#secret", FC_DB_PASSWORD="password-secret")
        result, summary, _calls = self.run_fixture([({"stats": GREEN_STATS, "errors": []}, 0)], environment=environment)
        self.assertEqual(result, 0)
        exported = json.dumps(summary)
        self.assertNotIn("secret", exported)
        self.assertNotIn("hidden", exported)
        self.assertNotIn("FC_DB_PASSWORD", exported)
        self.assertEqual(summary["api"], "http://localhost:2254/api")

    def test_artifact_change_during_run_is_saved_without_claiming_release_ready(self):
        before = {"backend": {"sha256": "a" * 64, "provenance": "declared"},
                  "frontend": {"kind": "production", "sha256": "b" * 64, "provenance": "declared", "files": []}}
        after = {"backend": {"sha256": "c" * 64, "provenance": "declared"},
                 "frontend": dict(before["frontend"])}
        with patch.object(runner, "collect_artifact_identity", side_effect=[before, after]) as collect:
            result, summary, _calls = self.run_fixture([({"stats": GREEN_STATS, "errors": []}, 0)])
        self.assertEqual(result, 0)
        self.assertEqual(collect.call_count, 2)
        self.assertEqual(summary["artifacts"], before)
        self.assertEqual(summary["artifactsAfter"], after)
        self.assertTrue(summary["artifactsChanged"])
        self.assertFalse(summary["sourceChanged"])
        self.assertFalse(summary["releaseReady"])

    def test_collection_exceptions_are_unknown_and_do_not_hide_end_state(self):
        with patch.object(runner, "collect_artifact_identity", side_effect=OSError("fixture")):
            result, summary, _calls = self.run_fixture(
                [({"stats": GREEN_STATS, "errors": []}, 0)],
                sources=[OSError("fixture"), OSError("fixture")])
        self.assertEqual(result, 0)
        self.assertEqual(summary["sourceBefore"]["state"], "unknown")
        self.assertEqual(summary["sourceAfter"]["state"], "unknown")
        self.assertEqual(summary["artifactsAfter"]["backend"]["sha256"], "unknown")
        self.assertEqual(summary["artifactsChanged"], "unknown")
        self.assertIn("finishedAt", summary)

    def test_interruption_still_saves_final_source_and_report_hash(self):
        result, summary, _calls = self.run_fixture(
            [({"stats": GREEN_STATS, "errors": []}, 0)], launch_error=KeyboardInterrupt())
        self.assertEqual(result, 130)
        self.assertEqual(summary["status"], "interrupted")
        self.assertEqual(summary["groups"][0]["status"], "interrupted")
        self.assertIn("finishedAt", summary["groups"][0])
        self.assertIn("sourceAfter", summary)
        self.assertFalse(summary["releaseReady"])

    def test_unexpected_launch_exception_still_saves_final_failure(self):
        result, summary, _calls = self.run_fixture([(None, 0)], launch_error=RuntimeError("secret fixture message"))
        self.assertEqual(result, 1)
        self.assertEqual(summary["status"], "failed")
        self.assertEqual(summary["failure"]["type"], "RuntimeError")
        self.assertIn("finishedAt", summary)
        self.assertIn("sourceAfter", summary)
        self.assertNotIn("secret", json.dumps(summary))

    def test_missing_error_field_or_invalid_report_object_cannot_pass(self):
        for report in ({"stats": GREEN_STATS}, {"stats": GREEN_STATS, "errors": None}, []):
            with self.subTest(report=report):
                result, summary, _calls = self.run_fixture([(report, 0)])
                self.assertEqual(result, 1)
                self.assertFalse(summary["passed"])
                self.assertTrue(summary["groups"][0]["errors"])

    def test_failure_after_success_does_not_return_a_success_exit_code(self):
        def fail_success_output(*args, **_kwargs):
            if args and str(args[0]).startswith("Acceptance passed;"):
                raise OSError("fixture output failure")

        with patch("builtins.print", side_effect=fail_success_output):
            result, summary, _calls = self.run_fixture([({"stats": GREEN_STATS, "errors": []}, 0)])
        self.assertEqual(result, 1)
        self.assertEqual(summary["exitCode"], 1)
        self.assertEqual(summary["status"], "failed")
        self.assertFalse(summary["passed"])


if __name__ == "__main__":
    unittest.main()
