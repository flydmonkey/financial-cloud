"""Run explicitly selected offline suites, retaining results even on failure."""
from __future__ import annotations

import argparse
import importlib
from pathlib import Path
import sys
import unittest

from verification_ci_evidence import ROOT, artifact, identity, same_identity, utc_now, write_json

SCOPES = {
    "evidence": ("test_release_evidence", "test_accounting_acceptance", "test_release_evidence_provenance"),
    "sql": ("test_payroll_live_voucher_sql",),
    "safety": ("test_init_payroll_it_schema", "test_verification_ci_evidence", "test_verification_ci_execution", "test_verification_ci_workflow"),
}


def cases(suite):
    for item in suite:
        if isinstance(item, unittest.TestSuite):
            yield from cases(item)
        else:
            yield item


def load_scope(scope):
    suite = unittest.TestSuite()
    for name in SCOPES[scope]:
        module = importlib.import_module(name)
        selected = (unittest.defaultTestLoader.loadTestsFromTestCase(module.OfflineSafetyTests)
                    if name == "test_payroll_live_voucher_sql" else
                    unittest.defaultTestLoader.loadTestsFromModule(module))
        if selected.countTestCases() == 0:
            raise ValueError("Required offline module contains no cases: " + name)
        suite.addTests(selected)
    return suite


class RecordedResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.outcomes = {}

    def startTest(self, test):
        self.outcomes[test.id()] = {"id": test.id(), "outcome": "unfinished"}
        super().startTest(test)

    def addSuccess(self, test):
        self.outcomes[test.id()]["outcome"] = "passed"
        super().addSuccess(test)

    def addFailure(self, test, err):
        self.outcomes[test.id()]["outcome"] = "failed"
        super().addFailure(test, err)

    def addError(self, test, err):
        self.outcomes[test.id()]["outcome"] = "error"
        super().addError(test, err)

    def addSkip(self, test, reason):
        self.outcomes[test.id()]["outcome"] = "skipped"
        super().addSkip(test, reason)

    def addExpectedFailure(self, test, err):
        self.outcomes[test.id()]["outcome"] = "expected-failure"
        super().addExpectedFailure(test, err)

    def addUnexpectedSuccess(self, test):
        self.outcomes[test.id()]["outcome"] = "unexpected-success"
        super().addUnexpectedSuccess(test)

    def addSubTest(self, test, subtest, err):
        if err is not None:
            self.outcomes[test.id()]["outcome"] = "failed-subtest"
        super().addSubTest(test, subtest, err)


def execute(suite, stream):
    planned = [case.id() for case in cases(suite)]
    if not planned or len(set(planned)) != len(planned):
        raise ValueError("Required suite is empty or contains duplicate cases")
    result = unittest.TextTestRunner(stream=stream, verbosity=2, resultclass=RecordedResult).run(suite)
    complete = (result.testsRun == len(planned) and set(result.outcomes) == set(planned)
                and all(case["outcome"] == "passed" for case in result.outcomes.values()))
    return {"planned": len(planned), "executed": result.testsRun,
            "failures": len(result.failures), "errors": len(result.errors), "skipped": len(result.skipped),
            "cases": list(result.outcomes.values()), "passed": result.wasSuccessful() and complete}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scope", choices=SCOPES, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(argv)
    args.output.mkdir(parents=True, exist_ok=True)
    if (args.output / "result.json").exists():
        print("Output already contains a result; choose a fresh evidence directory", file=sys.stderr)
        return 1
    report = {"schemaVersion": 1, "kind": "offline-verification", "scope": args.scope,
              "startedAt": utc_now(), "passed": False,
              "command": ["python", "tools/run_verification_ci_tests.py", "--scope", args.scope, "--output", str(args.output)]}
    log = args.output / "tests.log"
    try:
        report["before"] = identity(ROOT)
        suite = load_scope(args.scope)
        with log.open("w", encoding="utf-8") as stream:
            report["tests"] = execute(suite, stream)
        report["after"] = identity(ROOT)
        report["sourceUnchanged"] = same_identity(report["before"], report["after"])
        report["passed"] = report["tests"]["passed"] and report["sourceUnchanged"]
    except BaseException as error:
        # Unexpected exception content can contain environment values. Preserve
        # the failure class; expected test failures are retained in tests.log.
        report["failure"] = type(error).__name__
    finally:
        report["finishedAt"] = utc_now()
        if log.exists():
            report["log"] = artifact(log)
        write_json(args.output / "result.json", report)
    print(f"{args.scope}: {'passed' if report['passed'] else 'failed'}; evidence: {args.output}")
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
