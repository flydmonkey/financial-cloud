#!/usr/bin/env python3
"""Smoke-check general-ledger menu + API without printing secrets."""
import json
import sys
import urllib.parse
import urllib.request

BASE = "http://localhost:2154"


def req(method: str, path: str, data=None, headers=None):
    body = None
    hdrs = dict(headers or {})
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        hdrs.setdefault("Content-Type", "application/json")
    r = urllib.request.Request(BASE + path, data=body, headers=hdrs, method=method)
    with urllib.request.urlopen(r, timeout=30) as resp:
        raw = resp.read().decode("utf-8")
        return resp.status, json.loads(raw) if raw else {}


def main() -> int:
    _, init = req("GET", "/api/login/get?_allow_anonymous=true")
    state = init["data"]["state"]
    _, signin = req(
        "POST",
        "/api/login/signin?_allow_anonymous=true",
        {
            "username": "admin",
            "password": "changeme",
            "captcha": "",
            "state": state,
            "authType": "normal",
        },
    )
    if signin.get("code") != 0:
        print("LOGIN_FAIL", signin.get("message"))
        return 1
    token = signin["data"]["token"]
    auth = {"Authorization": f"Bearer {token}"}

    _, funcs = req("GET", "/api/open/func/list?_allow_anonymous=true&appId=1", headers=auth)
    functions = funcs.get("data", {}).get("functions") or []
    matched = [
        f
        for f in functions
        if (f.get("requestUrl") or "")
        in (
            "/statement/general-ledger",
            "/statement/expense-detail",
            "/statement/subject-balance",
        )
    ]
    print("MENU_MATCHES", len(matched))
    for f in matched:
        print(
            "MENU",
            f.get("resName"),
            f.get("requestUrl"),
            "status=",
            f.get("status"),
            "visible=",
            f.get("isVisible"),
        )
    gl = [f for f in matched if f.get("requestUrl") == "/statement/general-ledger"]
    print("GL_IN_MENU", bool(gl))

    qs = urllib.parse.urlencode(
        [
            ("periodType", "between"),
            ("dateRange", "2026-08"),
            ("dateRange", "2026-08"),
            ("hideNoActivityAndZeroBalance", "false"),
        ]
    )
    _, gl_body = req("GET", f"/api/statement/general-ledger?{qs}", headers=auth)
    data = gl_body.get("data") or {}
    print(
        "API",
        "code=",
        gl_body.get("code"),
        "subjectCount=",
        data.get("subjectCount"),
        "items=",
        len(data.get("items") or []),
    )
    for row in (data.get("items") or [])[:3]:
        print(
            "ROW",
            row.get("subjectCode"),
            row.get("summary"),
            row.get("period"),
            "debit=",
            row.get("debit"),
            "balance=",
            row.get("balance"),
            "span=",
            row.get("rowSpan"),
        )
    return 0 if gl and gl_body.get("code") == 0 and (data.get("subjectCount") or 0) > 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())
