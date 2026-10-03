"""Identity boundaries using synthetic source metadata, without Git or network."""
from copy import deepcopy
import unittest
from unittest.mock import patch

import verification_ci_evidence as evidence


def source(state="clean"):
    return {"status": "collected", "commitSha": "a" * 40, "state": state,
            "fingerprint": "b" * 64, "gitStatus": {"tracked": [], "untracked": []}, "files": []}


def ci(event="pull_request"):
    return {"GITHUB_ACTIONS": "true", "GITHUB_SHA": "a" * 40,
            "VERIFICATION_SOURCE_HEAD": "c" * 40 if event == "pull_request" else "a" * 40,
            "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2", "GITHUB_EVENT_NAME": event}


class VerificationIdentityTests(unittest.TestCase):
    def observe(self, env, snapshot=None):
        with patch.object(evidence, "collect_source_snapshot", return_value=snapshot or source()):
            return evidence.identity(environment=env)

    def test_local_dirty_source_does_not_invent_remote_identity(self):
        result = self.observe({"FC_DB_PASSWORD": "do-not-export"}, source("dirty"))
        self.assertEqual(result["context"], {"execution": "local", "remoteRunObserved": False})
        self.assertNotIn("do-not-export", str(result))

    def test_pull_request_source_head_and_tested_merge_sha_are_separate(self):
        result = self.observe(ci())
        self.assertEqual(result["source"]["commitSha"], "a" * 40)
        self.assertEqual(result["context"]["checkoutSha"], "a" * 40)
        self.assertEqual(result["context"]["sourceHeadSha"], "c" * 40)

    def test_checkout_mismatch_or_dirty_ci_source_is_rejected(self):
        env = ci()
        env["GITHUB_SHA"] = "d" * 40
        for settings, snapshot in ((env, source()), (ci(), source("dirty"))):
            with self.subTest(settings=settings), self.assertRaises(ValueError):
                self.observe(settings, snapshot)

    def test_incomplete_ci_fields_are_rejected_without_echoing_input(self):
        for key in ci():
            env = ci()
            if key == "GITHUB_ACTIONS":
                continue
            env[key] = "private-field-value"
            with self.subTest(key=key), self.assertRaises(ValueError) as caught:
                self.observe(env)
            self.assertNotIn("private-field-value", str(caught.exception))

    def test_push_requires_same_source_head_as_checkout(self):
        self.assertEqual(self.observe(ci("push"))["context"]["event"], "push")
        with self.assertRaises(ValueError):
            self.observe({**ci("push"), "VERIFICATION_SOURCE_HEAD": "c" * 40})

    def test_unknown_source_is_rejected(self):
        with self.assertRaises(ValueError):
            self.observe({}, {**source(), "status": "unknown"})

    def test_any_source_or_execution_drift_blocks_unchanged_conclusion(self):
        before = self.observe(ci())
        self.assertTrue(evidence.same_identity(before, deepcopy(before)))
        for field, changed in (("fingerprint", "d" * 64), ("commitSha", "d" * 40),
                               ("state", "dirty"), ("gitStatus", {"tracked": ["changed"]}), ("status", "unknown")):
            after = deepcopy(before)
            after["source"][field] = changed
            with self.subTest(field=field):
                self.assertFalse(evidence.same_identity(before, after))
        after = deepcopy(before)
        after["context"]["runAttempt"] = "3"
        self.assertFalse(evidence.same_identity(before, after))
