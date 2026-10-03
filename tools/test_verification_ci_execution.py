"""Failure/stale-report fixtures; these tests do not run database or Maven."""
import io
from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

import run_verification_ci_tests as runner
import verify_payroll_ci_reports as payroll
from test_verification_ci_evidence import source


def identity():
    snapshot = source()
    snapshot["files"] = [{"path": name, "sha256": digest} for name, digest in payroll.build_schema_plan().sources.items()]
    return {"source": snapshot, "context": {"execution": "local", "remoteRunObserved": False}}


def database_record(operation="observe"):
    plan = payroll.build_schema_plan()
    database = "financial_cloud_e2e_20261003_it4_synthetic_reports"
    return {"synthetic": True, "passed": True, "success": True, "operation": operation,
            "targetDatabase": database, "plan": plan.evidence(), "sourceInputs": plan.sources,
            "observed": {"version": "9.4.0", "database": database, "performanceSchema": 1,
                         "sessionIsolation": "REPEATABLE-READ", "globalIsolation": "REPEATABLE-READ", "transactionReadOnly": 1},
            "lockObservation": {name: {"readable": True, "observedRows": 0} for name in ("data_lock_waits", "data_locks", "threads")},
            "rowCounts": dict.fromkeys(plan.tables, 0), "showCreateSha256": dict.fromkeys(plan.tables, "d" * 64),
            "schemaObjects": [{"name": name, "type": "BASE TABLE", "engine": "InnoDB"} for name in plan.tables],
            "triggerCount": 0, "payrollScopeIndex": [{"Column_name": name, "Seq_in_index": i, "Key_name": payroll.SCOPE_INDEX,
                "Non_unique": 1, "Sub_part": None, "Visible": "YES", "Collation": "A", "Index_type": "BTREE"}
                for i, name in enumerate(payroll.SCOPE_COLUMNS, 1)],
            "databaseCreated": operation == "fresh-create", "targetExisted": operation != "fresh-create",
            "ddlStatementsCompleted": len(plan.statements) if operation == "fresh-create" else 0,
            "startedAt": datetime.now(timezone.utc).isoformat(), "completedAt": datetime.now(timezone.utc).isoformat()}


class RunnerBoundaryTests(unittest.TestCase):
    def suite(self, outcome):
        class Case(unittest.TestCase):
            def runTest(self):
                if outcome == "fail":
                    self.fail("synthetic failure")
                if outcome == "error":
                    raise RuntimeError("synthetic error")
                if outcome == "skip":
                    self.skipTest("synthetic skip")
                if outcome == "subtest":
                    with self.subTest(value=1):
                        self.fail("synthetic subtest")
        return unittest.TestSuite([Case()])

    def test_success_has_positive_complete_case_outcomes(self):
        result = runner.execute(self.suite("pass"), io.StringIO())
        self.assertTrue(result["passed"])
        self.assertEqual((result["planned"], result["executed"]), (1, 1))

    def test_failure_error_skip_and_subtest_fail_the_required_run(self):
        for outcome in ("fail", "error", "skip", "subtest"):
            with self.subTest(outcome=outcome):
                self.assertFalse(runner.execute(self.suite(outcome), io.StringIO())["passed"])

    def test_empty_and_duplicate_case_suites_are_rejected(self):
        for suite in (unittest.TestSuite(), unittest.TestSuite([self.suite("pass"), self.suite("pass")])):
            with self.assertRaises(ValueError):
                runner.execute(suite, io.StringIO())

    def test_sql_selection_never_loads_real_database_tests(self):
        suite = runner.load_scope("sql")
        names = [case.id() for case in runner.cases(suite)]
        self.assertEqual(len(names), 13)
        self.assertTrue(all("OfflineSafetyTests" in name for name in names))

    def test_source_drift_returns_failure_and_preserves_actual_results(self):
        with tempfile.TemporaryDirectory() as directory:
            after = identity()
            after["source"]["fingerprint"] = "d" * 64
            with patch.object(runner, "identity", side_effect=[identity(), after]), patch.object(runner, "load_scope", return_value=self.suite("pass")):
                self.assertEqual(runner.main(["--scope", "evidence", "--output", directory]), 1)
            result = json.loads((Path(directory) / "result.json").read_text())
            self.assertTrue(result["tests"]["passed"])
            self.assertFalse(result["sourceUnchanged"])

    def test_launch_exception_or_interrupt_leaves_failed_evidence(self):
        for problem in (RuntimeError("private-field-value"), KeyboardInterrupt(), SystemExit(0)):
            with self.subTest(kind=type(problem).__name__), tempfile.TemporaryDirectory() as directory:
                with patch.object(runner, "identity", return_value=identity()), patch.object(runner, "load_scope", side_effect=problem):
                    self.assertEqual(runner.main(["--scope", "evidence", "--output", directory]), 1)
                result = (Path(directory) / "result.json").read_text()
                self.assertFalse(json.loads(result)["passed"])
                self.assertNotIn("private-field-value", result)

    def test_reusing_result_directory_fails_without_overwriting_history(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "result.json"
            path.write_text("historical record")
            self.assertEqual(runner.main(["--scope", "evidence", "--output", directory]), 1)
            self.assertEqual(path.read_text(), "historical record")


class PayrollReportBoundaryTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.report_dir, self.output = self.root / "reports", self.root / "evidence"
        self.report_dir.mkdir()
        self.output.mkdir()
        for name in ("schema-init.json", "database-before.json", "database-after.json"):
            (self.output / name).write_text(json.dumps(database_record("fresh-create" if name == "schema-init.json" else "observe")))
        self.path = self.report_dir / payroll.REPORT
        self.patch = patch.object(payroll, "identity", return_value=identity())
        self.patch.start()
        self.addCleanup(self.patch.stop)

    def xml(self):
        root = ET.Element("testsuite", name=payroll.CLASS, tests="21", failures="0", errors="0", skipped="0")
        marker_path = self.output / "run.json"
        if marker_path.exists():
            props = ET.SubElement(root, "properties")
            for key, value in json.loads(marker_path.read_text())["reportProperties"].items():
                ET.SubElement(props, "property", name=key, value=value)
        for name in sorted(payroll.EXPECTED_CASES):
            ET.SubElement(root, "testcase", name=name, classname=payroll.CLASS)
        return root

    def write(self, root=None):
        ET.ElementTree(root if root is not None else self.xml()).write(self.path, encoding="utf-8", xml_declaration=True)
        (self.output / "maven.log").write_text("synthetic current command log")
        (self.output / "database-after.json").write_text(json.dumps(database_record()))

    def prepare(self):
        return payroll.prepare(self.report_dir, self.output)

    def test_complete_fresh_report_and_successful_command_are_accepted(self):
        self.prepare()
        self.write()
        result = payroll.verify(self.report_dir, self.output, "0")
        self.assertTrue(result["passed"])
        self.assertEqual(result["tests"]["executed"], 21)

    def test_old_unchanged_report_is_rejected_even_with_new_mtime(self):
        self.write()
        self.prepare()
        self.path.write_bytes(self.path.read_bytes())
        with self.assertRaises(ValueError):
            payroll.verify(self.report_dir, self.output, "0")

    def test_marker_is_required_and_cannot_be_reused(self):
        self.write()
        with self.assertRaises(FileNotFoundError):
            payroll.verify(self.report_dir, self.output, "0")
        self.prepare()
        with self.assertRaises(ValueError):
            self.prepare()

    def test_missing_duplicate_or_replaced_case_is_rejected_despite_total_21(self):
        for operation in ("missing", "duplicate", "replaced"):
            root = self.xml()
            if operation == "missing":
                root.remove(root.find("testcase"))
            elif operation == "duplicate":
                nodes = root.findall("testcase")
                nodes[0].set("name", nodes[1].get("name"))
            else:
                root.find("testcase").set("name", "inventedCase")
            self.write(root)
            with self.subTest(operation=operation), self.assertRaises(ValueError):
                payroll.validate_xml(self.path)

    def test_missing_counters_wrong_class_skipped_failure_or_retry_are_rejected(self):
        for kind in ("counter", "class", "skipped", "failure", "error", "rerunFailure"):
            root = self.xml()
            if kind == "counter":
                del root.attrib["skipped"]
            elif kind == "class":
                root.find("testcase").set("classname", "wrong.Class")
            else:
                ET.SubElement(root.find("testcase"), kind)
            self.write(root)
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                payroll.validate_xml(self.path)

    def test_missing_or_failed_command_and_missing_log_are_not_success(self):
        self.prepare()
        self.write()
        for code in ("unknown", "", "1"):
            with self.subTest(code=code), self.assertRaises(ValueError):
                payroll.verify(self.report_dir, self.output, code)
        (self.output / "maven.log").unlink()
        with self.assertRaises(ValueError):
            payroll.verify(self.report_dir, self.output, "0")

    def test_source_drift_rejects_an_otherwise_passing_current_report(self):
        self.prepare()
        self.write()
        altered = identity()
        altered["source"]["fingerprint"] = "d" * 64
        with patch.object(payroll, "identity", return_value=altered), self.assertRaises(ValueError):
            payroll.verify(self.report_dir, self.output, "0")

    def test_malformed_xml_and_entities_are_rejected(self):
        for raw in (b"<testsuite", b'<!DOCTYPE x [<!ENTITY y "payload">]><testsuite/>'):
            self.path.write_bytes(raw)
            with self.subTest(raw=raw), self.assertRaises((ET.ParseError, ValueError)):
                payroll.validate_xml(self.path)

    def test_copied_or_whitespace_rewritten_historical_xml_cannot_bind_to_new_marker(self):
        historical = ET.tostring(self.xml())
        self.prepare()
        for content in (historical, historical + b"\n\n"):
            self.path.write_bytes(content)
            (self.output / "maven.log").write_text("new log despite old XML")
            with self.subTest(content_size=len(content)), self.assertRaises(ValueError):
                payroll.verify(self.report_dir, self.output, "0")

    def test_nonce_and_source_properties_must_match_exactly_once(self):
        self.prepare()
        for operation in ("old-nonce", "wrong-sha", "duplicate", "missing"):
            root = self.xml()
            props = root.find("properties")
            if operation == "old-nonce":
                props[0].set("value", "stale-run-nonce")
            elif operation == "wrong-sha":
                props[1].set("value", "d" * 40)
            elif operation == "duplicate":
                ET.SubElement(props, "property", **props[0].attrib)
            else:
                root.remove(props)
            self.write(root)
            with self.subTest(operation=operation), self.assertRaises(ValueError):
                payroll.verify(self.report_dir, self.output, "0")

    def test_database_evidence_missing_failed_or_changed_is_not_accepted(self):
        self.prepare()
        self.write()
        for name in ("schema-init.json", "database-before.json", "database-after.json"):
            path = self.output / name
            original = path.read_bytes()
            path.write_text(json.dumps({"passed": False}))
            with self.subTest(name=name), self.assertRaises(ValueError):
                payroll.verify(self.report_dir, self.output, "0")
            path.write_bytes(original)

    def test_incomplete_environment_foreign_database_and_stale_sources_are_rejected(self):
        for operation in ("incomplete", "database", "source", "version", "rows", "index", "permission"):
            record = database_record("fresh-create")
            if operation == "incomplete":
                record = {"passed": True}
            elif operation == "database":
                record["observed"]["database"] = "foreign"
            elif operation == "source":
                record["sourceInputs"] = {"sql/financial_cloud_init.sql": "a" * 64}
            elif operation == "version":
                record["observed"]["version"] = "9.7.0"
            elif operation == "rows":
                record["rowCounts"]["employee_salary"] = 1
            elif operation == "index":
                record["payrollScopeIndex"][0]["Visible"] = "NO"
            else:
                record["lockObservation"]["threads"]["readable"] = False
            (self.output / "schema-init.json").write_text(json.dumps(record))
            with self.subTest(operation=operation), self.assertRaises(ValueError):
                self.prepare()

    def test_missing_naive_reversed_future_and_out_of_order_observation_times_are_rejected(self):
        for kind in ("missing", "naive", "reversed", "future"):
            record = database_record()
            if kind == "missing":
                del record["completedAt"]
            elif kind == "naive":
                record["startedAt"] = datetime.now().isoformat()
            elif kind == "reversed":
                record["completedAt"] = (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()
            else:
                record["completedAt"] = (datetime.now(timezone.utc) + timedelta(days=1)).isoformat()
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                payroll.observation_times(record)
        prior = database_record()
        prior["startedAt"] = (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()
        prior["completedAt"] = prior["startedAt"]
        (self.output / "database-before.json").write_text(json.dumps(prior))
        with self.assertRaises(ValueError):
            self.prepare()

    def test_post_check_before_maven_completion_is_rejected(self):
        self.prepare()
        premature = database_record()
        self.write()
        (self.output / "database-after.json").write_text(json.dumps(premature))
        with self.assertRaises(ValueError):
            payroll.verify(self.report_dir, self.output, "0")


if __name__ == "__main__":
    unittest.main()
