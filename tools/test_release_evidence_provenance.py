"""Local provenance fixtures use mocked Git and temporary artifact trees only."""
import hashlib
import json
from pathlib import Path
from types import SimpleNamespace
import tempfile
import unittest
from unittest.mock import patch

import release_evidence_provenance as provenance


class SourceProvenanceTest(unittest.TestCase):
    def git_fixture(self, *, tracked=b"source.py\0.gitignore\0", untracked=b"", status=b"", commit="1" * 40):
        def launch(args, **_kwargs):
            command = args[3:]
            if command[:1] == ["rev-parse"]:
                output = (commit + "\n").encode("ascii")
            elif command[:1] == ["status"]:
                output = status
            elif "--cached" in command:
                output = tracked
            else:
                output = untracked
            return SimpleNamespace(stdout=output, stderr=b"", returncode=0)
        return launch

    def test_full_sha_state_sorted_files_and_canonical_source_fingerprint(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "source.py").write_text("fixture source", encoding="utf-8")
            (root / ".gitignore").write_text("/.e2e-run/\n", encoding="utf-8")
            (root / "untracked.txt").write_text("uncommitted fixture", encoding="utf-8")
            launch = self.git_fixture(untracked=b"untracked.txt\0", status=b" M source.py\0?? untracked.txt\0")
            with patch.object(provenance.subprocess, "run", side_effect=launch):
                snapshot = provenance.collect_source_snapshot(root)
            self.assertEqual(snapshot["commitSha"], "1" * 40)
            self.assertEqual(snapshot["state"], "dirty")
            self.assertEqual(snapshot["gitStatus"], {"tracked": [{"path": "source.py", "status": " M"}], "untracked": ["untracked.txt"]})
            self.assertEqual([item["path"] for item in snapshot["files"]], [".gitignore", "source.py", "untracked.txt"])
            serialized = json.dumps(snapshot["files"], sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
            self.assertEqual(snapshot["fingerprint"], hashlib.sha256(serialized).hexdigest())
            self.assertFalse(snapshot["files"][-1]["tracked"])

    def test_ignored_run_output_does_not_change_source_fingerprint(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "source.py").write_text("fixture", encoding="utf-8")
            (root / ".gitignore").write_text("/.e2e-run/\n", encoding="utf-8")
            with patch.object(provenance.subprocess, "run", side_effect=self.git_fixture()):
                before = provenance.collect_source_snapshot(root)
                output = root / ".e2e-run" / "fixture"
                output.mkdir(parents=True)
                (output / "report.json").write_text('{"fixture":true}', encoding="utf-8")
                after = provenance.collect_source_snapshot(root)
            self.assertEqual(before["fingerprint"], after["fingerprint"])
            self.assertFalse(provenance.source_changed(before, after))
            self.assertEqual(after["state"], "clean")

    def test_source_bytes_changes_are_detected_even_when_git_status_is_same(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "source.py").write_text("first dirty content", encoding="utf-8")
            (root / ".gitignore").write_text("/.e2e-run/\n", encoding="utf-8")
            with patch.object(provenance.subprocess, "run", side_effect=self.git_fixture(status=b" M source.py\0")):
                before = provenance.collect_source_snapshot(root)
                (root / "source.py").write_text("second dirty content", encoding="utf-8")
                after = provenance.collect_source_snapshot(root)
            self.assertEqual(before["gitStatus"], after["gitStatus"])
            self.assertTrue(provenance.source_changed(before, after))

    def test_missing_git_or_non_full_commit_is_explicit_unknown(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for launch in (self.git_fixture(commit="1234567"), OSError("Git unavailable")):
                with self.subTest(launch=launch), patch.object(provenance.subprocess, "run", side_effect=launch):
                    snapshot = provenance.collect_source_snapshot(root)
                    self.assertEqual(snapshot["state"], "unknown")
                    self.assertEqual(snapshot["commitSha"], "unknown")
                    self.assertEqual(snapshot["fingerprint"], "unknown")

    def test_git_status_changes_during_collection_are_unknown(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "source.py").write_text("fixture", encoding="utf-8")
            (root / ".gitignore").write_text("/.e2e-run/\n", encoding="utf-8")
            launch = self.git_fixture()
            status_calls = 0

            def unstable(args, **kwargs):
                nonlocal status_calls
                if args[3] == "status":
                    status_calls += 1
                    return SimpleNamespace(stdout=b"" if status_calls == 1 else b" M source.py\0", stderr=b"", returncode=0)
                return launch(args, **kwargs)

            with patch.object(provenance.subprocess, "run", side_effect=unstable):
                snapshot = provenance.collect_source_snapshot(root)
            self.assertEqual(snapshot["status"], "unknown")
            self.assertEqual(snapshot["state"], "unknown")
            self.assertFalse(snapshot["collectionStable"])
            self.assertEqual(provenance.source_changed(snapshot, snapshot), "unknown")

    def test_tracked_rename_and_deleted_file_remain_in_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "new.txt").write_text("renamed", encoding="utf-8")
            launch = self.git_fixture(tracked=b"deleted.txt\0new.txt\0", status=b"R  new.txt\0old.txt\0 D deleted.txt\0")
            with patch.object(provenance.subprocess, "run", side_effect=launch):
                snapshot = provenance.collect_source_snapshot(root)
            self.assertEqual(snapshot["state"], "dirty")
            self.assertEqual(snapshot["gitStatus"]["tracked"][1]["originalPath"], "old.txt")
            self.assertEqual(snapshot["files"][0]["type"], "missing")
            self.assertEqual(snapshot["files"][0]["sha256"], "unknown")


class ArtifactAndEnvironmentTest(unittest.TestCase):
    def artifact_fixture(self, root):
        target = root / "financial-cloud" / "target"
        target.mkdir(parents=True)
        (target / "fixture.jar").write_bytes(b"fixture JAR bytes")
        dist = root / "financial-cloud-ui" / "dist"
        (dist / "assets").mkdir(parents=True)
        (dist / "index.html").write_bytes(b"fixture frontend")
        (dist / "assets" / "app.js").write_bytes(b"fixture script")
        return target, dist

    def test_local_artifacts_are_declared_and_frontend_hashes_sorted_manifest(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target, _dist = self.artifact_fixture(root)
            result = provenance.collect_artifact_identity(root, {})
            self.assertEqual(result["backend"]["provenance"], "declared")
            self.assertEqual(result["backend"]["sha256"], provenance.sha256_file(target / "fixture.jar"))
            frontend = result["frontend"]
            self.assertEqual(frontend["kind"], "production")
            self.assertEqual(frontend["provenance"], "declared")
            self.assertEqual([item["path"] for item in frontend["files"]], ["assets/app.js", "index.html"])
            serialized = json.dumps(frontend["files"], sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
            self.assertEqual(frontend["sha256"], hashlib.sha256(serialized).hexdigest())

    def test_artifact_file_changes_are_detected_and_mtimes_are_irrelevant(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target, dist = self.artifact_fixture(root)
            before = provenance.collect_artifact_identity(root, {})
            (dist / "index.html").touch()
            same = provenance.collect_artifact_identity(root, {})
            self.assertFalse(provenance.artifacts_changed(before, same))
            (target / "fixture.jar").write_bytes(b"updated JAR")
            after = provenance.collect_artifact_identity(root, {})
            self.assertTrue(provenance.artifacts_changed(before, after))
            (dist / "assets" / "app.js").write_bytes(b"updated frontend")
            self.assertNotEqual(after["frontend"]["sha256"], provenance.collect_artifact_identity(root, {})["frontend"]["sha256"])

    def test_missing_or_ambiguous_artifacts_stay_unknown(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            missing = provenance.collect_artifact_identity(root, {})
            self.assertEqual(missing["backend"]["provenance"], "unknown")
            self.assertEqual(missing["frontend"]["sha256"], "unknown")
            self.assertEqual(provenance.artifacts_changed(missing, missing), "unknown")
            target, _dist = self.artifact_fixture(root)
            (target / "second.jar").write_bytes(b"another fixture")
            self.assertEqual(provenance.collect_artifact_identity(root, {})["backend"]["sha256"], "unknown")
            declared = provenance.collect_artifact_identity(root, {"RELEASE_BACKEND_JAR": "financial-cloud/target/fixture.jar"})
            self.assertEqual(declared["backend"]["provenance"], "declared")

    def test_environment_whitelist_sanitizes_all_urls_and_never_claims_binding(self):
        environment = {
            "FC_DB_NAME": "financial_cloud_e2e_fixture", "FC_DB_PASSWORD": "database-secret",
            "FC_DB_HOST": "127.0.0.1", "FC_DB_PORT": "3307",
            "E2E_API_URL": "https://user:password@example.test:2254/api?token=secret#private",
            "E2E_BASE_URL": "http://user:password@[::1]:3254/?key=secret",
            "E2E_COMPONENT_BASE_URL": "http://user:password@localhost:3154?token=secret",
            "PRIVATE_TOKEN": "token-secret",
        }
        declared = provenance.collect_declared_environment(environment)
        serialized = json.dumps(declared)
        for secret in ("user", "password", "secret", "private", "FC_DB_PASSWORD", "PRIVATE_TOKEN"):
            self.assertNotIn(secret, serialized)
        self.assertEqual(declared["api"], "https://example.test:2254/api")
        self.assertEqual(declared["frontend"], "http://[::1]:3254/")
        self.assertEqual(declared["componentFrontend"], "http://localhost:3154")
        self.assertEqual(declared["databaseHost"], "127.0.0.1")
        self.assertEqual(declared["databasePort"], 3307)
        self.assertEqual(declared["provenance"], "declared")
        self.assertEqual(declared["binding"]["status"], "unknown")
        self.assertEqual(declared["binding"]["review"], "pending")

    def test_invalid_urls_are_unknown_without_echoing_the_input(self):
        for url in ("password=secret", "http://host:bad?token=secret", "file:///private/secret"):
            self.assertEqual(provenance.sanitize_url(url), "unknown")

    def test_invalid_datasource_location_is_unknown_without_echoing_credentials(self):
        declared = provenance.collect_declared_environment({"FC_DB_HOST": "user:password@host", "FC_DB_PORT": "3307password"})
        self.assertEqual(declared["databaseHost"], "unknown")
        self.assertEqual(declared["databasePort"], "unknown")
        self.assertNotIn("password", json.dumps(declared))


if __name__ == "__main__":
    unittest.main()
