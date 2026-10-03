"""Offline CI contract checks using the actual workflow and synthetic evidence.

Requires PyYAML. No workflow, network client, database or Git command runs.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re
import tempfile
import unittest

import yaml

import release_evidence_fixtures as fixtures
import verify_release_evidence as verifier


ROOT = Path(__file__).resolve().parents[1]
NEW_JOBS = ("verification-tools", "payroll-mysql-it")
REQUIRED_NEW_STEPS = {
    "verification-tools": [
        "Run evidence tool tests",
        "Run payroll SQL offline boundary tests",
        "Run payroll initializer safety tests",
        "Save verification tools evidence",
    ],
    "payroll-mysql-it": [
        "Initialize payroll IT schema",
        "Verify payroll IT environment",
        "Prepare payroll transaction evidence",
        "Run payroll MySQL transaction integration",
        "Validate payroll IT reports",
        "Observe payroll IT cleanup",
        "Save payroll integration evidence",
    ],
}
LEGACY_REQUIRED_STEPS = {
    "backend-test": ["Run backend unit tests"],
    "frontend-check": [
        "ESLint", "Typecheck", "Unit tests (voucher workspace)",
        "AI UI regression scripts syntax check",
    ],
    "e2e": [
        "Run balance sheet integrity regression", "Run E2E accounting suite",
        "Run E2E year-end suite", "Run E2E balance sheet golden suite",
        "Run E2E income statement golden suite",
        "Run independent accounting acceptance (no skips or retries)",
    ],
}


def condition(expression: str) -> str:
    return re.sub(r"\s+", " ", expression.removeprefix("${{").removesuffix("}}").strip())


class VerificationWorkflowContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # BaseLoader preserves the literal `on` key; YAML 1.1 SafeLoader
        # otherwise treats it as a boolean and obscures event coverage.
        cls.workflow = yaml.load((ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8"), Loader=yaml.BaseLoader)
        cls.profile = json.loads((ROOT / "tools/release_evidence_profile.json").read_text(encoding="utf-8"))

    def named_steps(self, job_name):
        return {step["name"]: step for step in self.workflow["jobs"][job_name]["steps"] if "name" in step}

    def test_both_integration_events_reach_both_jobs_without_bypass(self):
        for event in ("push", "pull_request"):
            with self.subTest(event=event):
                trigger = self.workflow["on"][event]
                self.assertEqual(trigger["branches"], ["main"])
                for key in ("paths", "paths-ignore", "branches-ignore"):
                    self.assertNotIn(key, trigger)
        for name in NEW_JOBS:
            with self.subTest(job=name):
                job = self.workflow["jobs"][name]
                self.assertEqual(job.get("name", name), name)
                for key in ("if", "needs", "continue-on-error"):
                    self.assertNotIn(key, job)
                for step in job["steps"]:
                    self.assertNotIn("continue-on-error", step)
                for step in job["steps"]:
                    if "run" in step and not step.get("name", "").startswith(("Validate", "Observe")):
                        self.assertNotIn("if", step)

    def test_legacy_release_scope_and_database_free_backend_are_retained(self):
        for job_name, required in LEGACY_REQUIRED_STEPS.items():
            with self.subTest(job=job_name):
                self.assertIn(job_name, self.profile["ci"]["requiredJobs"])
                self.assertEqual(self.profile["ci"]["requiredSteps"][job_name], required)
        backend = self.workflow["jobs"]["backend-test"]
        self.assertNotIn("services", backend)
        command = self.named_steps("backend-test")["Run backend unit tests"]["run"]
        self.assertIn("-Dtest='*Test'", command)
        self.assertNotIn("PayrollTransactionMysqlIT", command)

    def test_trusted_profile_uses_exact_unique_new_job_and_step_names(self):
        self.assertGreaterEqual(self.profile["version"], 2)
        jobs = self.profile["ci"]["requiredJobs"]
        self.assertEqual(len(jobs), len(set(jobs)))
        for job_name, required in REQUIRED_NEW_STEPS.items():
            with self.subTest(job=job_name):
                self.assertIn(job_name, jobs)
                self.assertEqual(self.profile["ci"]["requiredSteps"][job_name], required)
                steps = self.workflow["jobs"][job_name]["steps"]
                actual_names = [step["name"] for step in steps if "name" in step]
                self.assertEqual(len(actual_names), len(set(actual_names)))
                self.assertEqual({name for name in actual_names if name in required}, set(required))

    def test_offline_scopes_are_explicit_and_do_not_enable_database_execution(self):
        steps = self.named_steps("verification-tools")
        for name, scope in zip(REQUIRED_NEW_STEPS["verification-tools"][:3], ("evidence", "sql", "safety")):
            with self.subTest(scope=scope):
                command = steps[name]["run"]
                self.assertIn("tools/run_verification_ci_tests.py", command)
                self.assertIn("--scope " + scope, command)
                self.assertIn("--output .e2e-run/verification-tools/" + scope, command)
                self.assertNotIn("--mysql", command)
                self.assertNotIn("discover", command)
        self.assertNotIn("services", self.workflow["jobs"]["verification-tools"])
        dependencies = steps["Install verification dependencies"]["run"]
        self.assertIn("PyYAML==6.0.3", dependencies)

    def test_mysql_service_and_connection_are_fixed_and_isolated(self):
        job = self.workflow["jobs"]["payroll-mysql-it"]
        service = job["services"]["mysql"]
        self.assertEqual(service["image"], "mysql:9.4.0-oraclelinux9")
        self.assertNotIn("volumes", service)
        self.assertIn("3307:3306", service["ports"])
        environment = job["env"]
        for key in ("FC_DB_HOST", "FC_DB_PORT", "FC_DB_NAME", "FC_DB_USER", "FC_DB_PASSWORD"):
            self.assertTrue(environment.get(key), key + " must be explicit")
        self.assertEqual(environment["FC_DB_HOST"], "127.0.0.1")
        self.assertEqual(environment["FC_DB_PORT"], "3307")
        self.assertEqual(environment["FC_DB_NAME"], "financial_cloud_e2e_20261003_it4_ci_${{ github.run_id }}_${{ github.run_attempt }}")
        self.assertEqual(environment["FC_DB_PASSWORD"], service["env"]["MYSQL_ROOT_PASSWORD"])
        for name in NEW_JOBS:
            self.assertIn("github.event.pull_request.head.sha", self.workflow["jobs"][name]["env"]["VERIFICATION_SOURCE_HEAD"])
            self.assertIn("github.sha", self.workflow["jobs"][name]["env"]["VERIFICATION_SOURCE_HEAD"])
        commands = "\n".join(step.get("run", "") for step in job["steps"])
        self.assertNotIn("docker compose", commands)
        self.assertNotIn("run_init_sql.py", commands)
        dependencies = self.named_steps("payroll-mysql-it")["Install payroll integration dependencies"]["run"]
        self.assertIn("PyMySQL[rsa]==1.1.2", dependencies)

    def test_integration_prepares_fresh_evidence_and_propagates_maven_exit(self):
        job = self.workflow["jobs"]["payroll-mysql-it"]
        steps = self.named_steps("payroll-mysql-it")
        order = [step.get("name") for step in job["steps"]]
        required_order = [
            "Initialize payroll IT schema", "Verify payroll IT environment",
            "Prepare payroll transaction evidence", "Run payroll MySQL transaction integration",
            "Observe payroll IT cleanup", "Validate payroll IT reports",
            "Save payroll integration evidence",
        ]
        self.assertEqual(sorted(required_order, key=order.index), required_order)
        prepare = steps["Prepare payroll transaction evidence"]
        self.assertEqual(prepare["id"], "payroll-evidence")
        self.assertIn("verify_payroll_ci_reports.py prepare", prepare["run"])
        self.assertIn(".e2e-run/payroll-mysql-it/run.json", prepare["run"])
        self.assertIn("GITHUB_OUTPUT", prepare["run"])
        for marker_field in ("runNonce", "commitSha", "fingerprint"):
            self.assertIn("['" + marker_field + "']", prepare["run"])
        self.assertIn("re.fullmatch(r'[0-9a-f]+'", prepare["run"])
        maven = steps["Run payroll MySQL transaction integration"]
        self.assertEqual(maven["id"], "payroll-tests")
        self.assertEqual(maven["working-directory"], "financial-cloud")
        command = maven["run"]
        self.assertIn("./mvnw -B test -Dtest=PayrollTransactionMysqlIT", command)
        self.assertNotIn("failIfNoTests=false", command)
        self.assertNotIn("failIfNoSpecifiedTests=false", command)
        self.assertNotIn("skipTests", command)
        self.assertNotIn("maven.test.skip", command)
        self.assertNotIn("skipITs", command)
        self.assertNotIn("|| true", command)
        self.assertIn("set -o pipefail", command)
        self.assertIn("set +e", command)
        self.assertRegex(command, r"status=\$\?")
        self.assertIn('echo "exit_code=$status" >> "$GITHUB_OUTPUT"', command)
        self.assertIn('exit "$status"', command)
        for property_name, output_name in (
            ("run.nonce", "run_nonce"),
            ("checkout.sha", "checkout_sha"),
            ("source.fingerprint", "source_fingerprint"),
        ):
            self.assertIn(
                "'-Dverification." + property_name
                + "=${{ steps.payroll-evidence.outputs." + output_name + " }}'",
                command,
            )
        verify = steps["Validate payroll IT reports"]["run"]
        self.assertIn("verify_payroll_ci_reports.py verify", verify)
        self.assertIn("--command-exit '${{ steps.payroll-tests.outputs.exit_code }}'", verify)

    def test_failed_runs_keep_conditional_validation_observation_and_upload(self):
        steps = self.named_steps("payroll-mysql-it")
        self.assertEqual(condition(steps["Validate payroll IT reports"]["if"]), "always() && steps.payroll-evidence.outcome == 'success'")
        self.assertEqual(condition(steps["Observe payroll IT cleanup"]["if"]), "always() && steps.payroll-schema.outcome == 'success'")
        self.assertEqual(steps["Initialize payroll IT schema"]["id"], "payroll-schema")
        outputs = [steps[name]["run"].split("--output ", 1)[1].split()[0]
                   for name in ("Initialize payroll IT schema", "Verify payroll IT environment", "Observe payroll IT cleanup")]
        self.assertEqual(len(set(outputs)), 3, "Observation must not overwrite original initialization evidence")
        for job_name, upload_name in (("verification-tools", "Save verification tools evidence"), ("payroll-mysql-it", "Save payroll integration evidence")):
            with self.subTest(job=job_name):
                upload = self.named_steps(job_name)[upload_name]
                self.assertEqual(condition(upload["if"]), "always()")
                self.assertEqual(upload["uses"], "actions/upload-artifact@v4")
                self.assertEqual(upload["with"]["if-no-files-found"], "error")
                self.assertIn(".e2e-run/" + job_name + "/", upload["with"]["path"])
        self.assertIn("financial-cloud/target/surefire-reports/", steps["Save payroll integration evidence"]["with"]["path"])


class ExpandedCiEvidenceContractTests(unittest.TestCase):
    def assess_mutated_ci(self, change):
        with tempfile.TemporaryDirectory(prefix="synthetic-expanded-ci-") as temporary:
            bundle_path = fixtures.create_fixture(Path(temporary) / "bundle")
            baseline = verifier.assess_bundle(bundle_path)
            self.assertTrue(baseline["releaseReady"], "Synthetic baseline must satisfy the trusted profile")
            self.assertTrue(baseline["synthetic"])
            bundle = json.loads(bundle_path.read_text(encoding="utf-8"))
            reference = bundle["evidence"]["ci"]
            ci_path = bundle_path.parent / reference["path"]
            ci = json.loads(ci_path.read_text(encoding="utf-8"))
            change(ci)
            ci_path.write_text(json.dumps(ci, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            # Rebind the synthetic external-file hash so failures prove the
            # semantic scope check, rather than just content tampering.
            reference["sha256"] = hashlib.sha256(ci_path.read_bytes()).hexdigest()
            bundle_path.write_text(json.dumps(bundle, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            result = verifier.assess_bundle(bundle_path)
            self.assertFalse(result["releaseReady"])
            self.assertTrue(result["synthetic"])
            return result

    def assert_ci_blocked(self, result, identifier):
        failed = [item for item in result["conditions"] if item["id"] == identifier and item["status"] != "satisfied"]
        self.assertTrue(failed, identifier + " must block the reduced CI evidence")

    def test_old_green_ci_missing_new_jobs_is_blocked(self):
        result = self.assess_mutated_ci(lambda ci: ci.update(jobs=[job for job in ci["jobs"] if job["name"] not in NEW_JOBS]))
        for name in NEW_JOBS:
            self.assert_ci_blocked(result, "ci.job." + name)

    def test_skipped_new_job_is_blocked_even_with_green_run(self):
        for name in NEW_JOBS:
            with self.subTest(job=name):
                def skip(ci):
                    next(job for job in ci["jobs"] if job["name"] == name)["conclusion"] = "skipped"
                result = self.assess_mutated_ci(skip)
                self.assert_ci_blocked(result, "ci.job." + name)

    def test_missing_prepare_or_verify_step_is_blocked(self):
        for name in ("Prepare payroll transaction evidence", "Validate payroll IT reports"):
            with self.subTest(step=name):
                def omit(ci):
                    job = next(job for job in ci["jobs"] if job["name"] == "payroll-mysql-it")
                    job["steps"] = [step for step in job["steps"] if step["name"] != name]
                result = self.assess_mutated_ci(omit)
                self.assert_ci_blocked(result, "ci.job.payroll-mysql-it.step." + name)


if __name__ == "__main__":
    unittest.main()
