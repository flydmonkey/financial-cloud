"""Acceptance safety: invalid database never launches reset; skipped evidence fails."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace

import run_accounting_acceptance as runner


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

            environment = {"FC_DB_NAME": "financial_cloud_e2e_test", "E2E_API_URL": "http://localhost:2254", "E2E_BASE_URL": "http://localhost:3254"}
            with patch.dict(runner.os.environ, environment, clear=True), patch.object(runner, "ROOT", Path(directory)), patch.object(runner, "GROUPS", [("fixture", ["fixture.spec.ts"])]), patch.object(runner.shutil, "which", return_value="npx"), patch.object(runner.subprocess, "run", side_effect=launch):
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


if __name__ == "__main__":
    unittest.main()
