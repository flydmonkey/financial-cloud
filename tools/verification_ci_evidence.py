"""Local/Actions verification identity; never claims production or remote success."""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re

from release_evidence_provenance import collect_source_snapshot, source_changed, utc_now

ROOT = Path(__file__).resolve().parents[1]


def identity(root=ROOT, environment=None):
    env = os.environ if environment is None else environment
    source = collect_source_snapshot(root)
    if source.get("status") != "collected":
        raise ValueError("Complete source identity could not be collected")
    context = {"execution": "local", "remoteRunObserved": False}
    if env.get("GITHUB_ACTIONS") == "true":
        sha, head = env.get("GITHUB_SHA", ""), env.get("VERIFICATION_SOURCE_HEAD", "")
        if any(re.fullmatch(r"[0-9a-f]{40}", v) is None for v in (sha, head)):
            raise ValueError("CI requires full checkout and source-head identities")
        if sha != source["commitSha"] or source["state"] != "clean":
            raise ValueError("CI checkout identity or clean source state does not match")
        run_id, attempt = env.get("GITHUB_RUN_ID", ""), env.get("GITHUB_RUN_ATTEMPT", "")
        event = env.get("GITHUB_EVENT_NAME", "")
        if not run_id.isascii() or not run_id.isdecimal() or int(run_id) < 1:
            raise ValueError("CI run identity is missing")
        if not attempt.isascii() or not attempt.isdecimal() or int(attempt) < 1:
            raise ValueError("CI attempt identity is missing")
        if event not in {"push", "pull_request"}:
            raise ValueError("Unsupported verification event")
        if event == "push" and head != sha:
            raise ValueError("Push source-head identity differs from the checkout")
        context = {"execution": "github-actions", "remoteRunObserved": True,
                   "checkoutSha": sha, "sourceHeadSha": head, "event": event,
                   "runId": run_id, "runAttempt": attempt}
    return {"source": source, "context": context}


def same_identity(before, after):
    return before.get("context") == after.get("context") and source_changed(before.get("source", {}), after.get("source", {})) is False


def artifact(path):
    path = Path(path)
    raw = path.read_bytes()
    return {"path": str(path), "sha256": hashlib.sha256(raw).hexdigest(), "size": len(raw)}


def write_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
