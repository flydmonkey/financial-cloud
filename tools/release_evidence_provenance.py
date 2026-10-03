"""Collect local acceptance provenance; supplied values never prove runtime binding.

This module only reads Git and local files. It does not contact the declared API,
database, or frontend, and never starts a build or a service.
"""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import urlsplit, urlunsplit

UNKNOWN = "unknown"
DECLARED = "declared"


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def canonical_sha256(value: object) -> str:
    """Hash the UTF-8, sorted-key, compact JSON representation of a value."""
    data = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(data.encode("utf-8")).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def unknown_source(reason: str) -> dict:
    return {
        "collectedAt": utc_now(), "status": UNKNOWN, "commitSha": UNKNOWN,
        "state": UNKNOWN, "fingerprint": UNKNOWN, "gitStatus": UNKNOWN,
        "files": [], "reason": reason,
    }


def _git(root: Path, *args: str) -> bytes:
    result = subprocess.run(
        ["git", "-C", str(root), *args], capture_output=True, timeout=30,
    )
    if result.returncode != 0:
        # Git stderr can include local paths and configuration. Do not export it.
        raise RuntimeError("Git source collection failed")
    return result.stdout


def _paths(raw: bytes) -> list[str]:
    return [value.decode("utf-8", errors="surrogateescape") for value in raw.split(b"\0") if value]


def _tracked_status(raw: bytes) -> tuple[list[dict], list[str]]:
    records = _paths(raw)
    tracked, untracked = [], []
    index = 0
    while index < len(records):
        record = records[index]
        index += 1
        if len(record) < 4 or record[2] != " ":
            raise ValueError("Invalid Git status output")
        code, path = record[:2], record[3:]
        if code == "??":
            untracked.append(path)
            continue
        entry = {"path": path, "status": code}
        if "R" in code or "C" in code:
            if index == len(records):
                raise ValueError("Incomplete Git rename status")
            entry["originalPath"] = records[index]
            index += 1
        tracked.append(entry)
    return sorted(tracked, key=lambda entry: entry["path"]), sorted(untracked)


def _file_entry(root: Path, relative: str, tracked: bool) -> dict:
    """Fingerprint links as links without opening their possible external target."""
    entry = {"path": relative, "tracked": tracked}
    path = root / relative
    # Git cannot return absolute paths or parent traversal; verify that assumption.
    if Path(relative).is_absolute() or ".." in Path(relative).parts:
        raise ValueError("Invalid source path")
    if path.is_symlink():
        target = os.readlink(path).encode("utf-8", errors="surrogateescape")
        return dict(entry, type="symlink", size=len(target), sha256=hashlib.sha256(target).hexdigest())
    if not path.exists():
        return dict(entry, type="missing", size=None, sha256=UNKNOWN)
    if not path.is_file():
        # A submodule/directory cannot silently stand in for a fingerprinted file.
        return dict(entry, type="unsupported", size=None, sha256=UNKNOWN)
    if not path.resolve().is_relative_to(root.resolve()):
        raise ValueError("Source path escapes the repository")
    return dict(entry, type="file", size=path.stat().st_size, sha256=sha256_file(path))


def collect_source_snapshot(root: Path) -> dict:
    """Capture full HEAD, tracked/untracked state and a sorted source manifest."""
    try:
        commit = _git(root, "rev-parse", "--verify", "HEAD").decode("ascii").strip()
        if not re.fullmatch(r"[0-9a-fA-F]{40}", commit):
            raise ValueError("Git did not provide a full SHA-1 commit")
        status_before = _git(root, "status", "--porcelain=v1", "-z", "--untracked-files=all")
        tracked_status, untracked = _tracked_status(status_before)
        tracked = set(_paths(_git(root, "ls-files", "--cached", "-z")))
        other = set(_paths(_git(root, "ls-files", "--others", "--exclude-standard", "-z")))
        files = [_file_entry(root, path, path in tracked) for path in sorted(tracked | other)]
        # Detect obvious races while gathering the manifest, without claiming an
        # atomic snapshot of concurrently edited files.
        stable = status_before == _git(root, "status", "--porcelain=v1", "-z", "--untracked-files=all")
        stable = stable and commit.lower() == _git(root, "rev-parse", "--verify", "HEAD").decode("ascii").strip().lower()
        complete = all(entry["type"] in ("file", "symlink", "missing") for entry in files)
        return {
            "collectedAt": utc_now(), "status": "collected" if stable and complete else UNKNOWN,
            "commitSha": commit.lower(),
            "state": ("clean" if not tracked_status and not untracked else "dirty") if stable and complete else UNKNOWN,
            "fingerprint": canonical_sha256(files),
            "gitStatus": {"tracked": tracked_status, "untracked": untracked}, "files": files,
            "collectionStable": stable,
            "reason": "" if stable and complete else "Source changed during collection or contains unsupported files",
        }
    except (OSError, ValueError, RuntimeError, UnicodeError, subprocess.SubprocessError):
        return unknown_source("Git/source identity could not be collected")


def sanitize_url(value: str) -> str:
    """Retain the service location, removing credentials, queries and fragments."""
    try:
        parsed = urlsplit(value)
        if parsed.scheme not in ("http", "https") or not parsed.hostname:
            return UNKNOWN
        hostname = parsed.hostname
        if ":" in hostname:
            hostname = f"[{hostname}]"
        netloc = hostname if parsed.port is None else f"{hostname}:{parsed.port}"
        return urlunsplit((parsed.scheme, netloc, parsed.path, "", ""))
    except ValueError:
        return UNKNOWN


def collect_declared_environment(env: dict) -> dict:
    # Deliberately whitelist fields: passwords/tokens and the rest of the process
    # environment never become evidence.
    supplied_host = env.get("FC_DB_HOST", "")
    host = supplied_host if isinstance(supplied_host, str) and re.fullmatch(r"[A-Za-z0-9_.:\[\]-]+", supplied_host) else UNKNOWN
    supplied_port = str(env.get("FC_DB_PORT", ""))
    port = int(supplied_port) if supplied_port.isascii() and supplied_port.isdigit() and len(supplied_port) <= 5 and 1 <= int(supplied_port) <= 65535 else UNKNOWN
    return {
        "provenance": DECLARED, "collectedAt": utc_now(),
        "database": env.get("FC_DB_NAME", UNKNOWN),
        "databaseHost": host, "databasePort": port,
        "api": sanitize_url(env.get("E2E_API_URL", "")),
        "frontend": sanitize_url(env.get("E2E_BASE_URL", "")),
        "componentFrontend": sanitize_url(env.get("E2E_COMPONENT_BASE_URL", "")),
        "instanceId": UNKNOWN, "datasourceId": UNKNOWN,
        "binding": {"status": UNKNOWN, "review": "pending", "evidence": []},
        "method": "Supplied environment values; runtime/data-source binding was not observed. Component frontend may serve source fixtures and does not prove production dist execution",
    }


def _artifact_path(root: Path, value: str) -> Path:
    path = Path(value)
    return path if path.is_absolute() else root / path


def _location(root: Path, path: Path) -> str:
    # A supplied external path is useful locally, but do not export an unrelated
    # user's directory or secrets embedded in it.
    try:
        return path.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        return "external-local-artifact"


def collect_artifact_identity(root: Path, env: dict) -> dict:
    """Hash available local JAR/dist bytes; these remain DECLARED identities."""
    backend = {"provenance": UNKNOWN, "sha256": UNKNOWN, "path": UNKNOWN}
    frontend = {"kind": "production", "provenance": UNKNOWN, "sha256": UNKNOWN, "path": UNKNOWN, "files": []}
    try:
        supplied = env.get("RELEASE_BACKEND_JAR")
        jars = [_artifact_path(root, supplied)] if supplied else sorted((root / "financial-cloud" / "target").glob("*.jar"))
        if len(jars) == 1 and jars[0].is_file() and not jars[0].is_symlink():
            backend = {
                "provenance": DECLARED, "sha256": sha256_file(jars[0]),
                "path": _location(root, jars[0]), "size": jars[0].stat().st_size,
                "method": "Hash of available local JAR; running service was not observed",
            }
        else:
            backend["reason"] = "No unique readable local backend JAR"
    except OSError:
        backend["reason"] = "Local backend JAR could not be read"
    try:
        dist = _artifact_path(root, env["RELEASE_FRONTEND_DIST"]) if env.get("RELEASE_FRONTEND_DIST") else root / "financial-cloud-ui" / "dist"
        if not dist.is_dir() or dist.is_symlink():
            frontend["reason"] = "Production frontend dist is unavailable"
        else:
            manifest = []
            for path in sorted(dist.rglob("*"), key=lambda path: path.relative_to(dist).as_posix()):
                if path.is_symlink() or not path.resolve().is_relative_to(dist.resolve()):
                    raise ValueError("Frontend tree contains a symbolic link")
                if path.is_file():
                    manifest.append({"path": path.relative_to(dist).as_posix(), "size": path.stat().st_size, "sha256": sha256_file(path)})
            if manifest:
                frontend = {
                    "kind": "production", "provenance": DECLARED, "sha256": canonical_sha256(manifest),
                    "path": _location(root, dist), "files": manifest,
                    "method": "Sorted production dist file manifest; served frontend was not observed",
                }
            else:
                frontend["reason"] = "Production frontend dist contains no files"
    except (OSError, ValueError):
        frontend["reason"] = "Production frontend dist could not be safely fingerprinted"
    return {"collectedAt": utc_now(), "backend": backend, "frontend": frontend}


def source_changed(before: dict, after: dict) -> bool | str:
    if before.get("status") != "collected" or after.get("status") != "collected":
        return UNKNOWN
    fields = ("commitSha", "fingerprint", "state", "gitStatus")
    return any(before.get(field) != after.get(field) for field in fields)


def artifacts_changed(before: dict, after: dict) -> bool | str:
    unknown = False
    for kind in ("backend", "frontend"):
        first, last = before.get(kind, {}).get("sha256", UNKNOWN), after.get(kind, {}).get("sha256", UNKNOWN)
        if first == UNKNOWN or last == UNKNOWN:
            unknown = True
        elif first != last:
            return True
    return UNKNOWN if unknown else False
