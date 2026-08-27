#!/usr/bin/env python3
"""Generate config_cash_flow_balance template seed SQL from CashFlowItemEnum.java."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENUM_FILE = ROOT / "financial-cloud/src/main/java/com/financial/cloud/enums/statement/CashFlowItemEnum.java"
OUT_FILE = ROOT / "sql/seed/config_cash_flow_balance.sql"

ENTRY_RE = re.compile(r'\s+[A-Z_]+\("([^"]+)",\s*"([^"]+)"\),')


def sort_index(db_code: str) -> int:
    return int(db_code.split("-", 1)[0])


def flags(db_code: str, desc: str) -> dict:
    idx = sort_index(db_code)
    is_result = 1 if any(k in desc for k in ("小计", "净额", "净增加", "五、", "六、")) else 0
    is_title = 1 if desc.startswith(("一、", "二、", "三、", "四、")) else 0
    is_main = 1 if 2 <= idx <= 38 else 0
    is_additional = 1 if idx >= 41 else 0
    is_edit = 0 if is_result or is_title else 1
    direction = 0
    if is_edit and is_main:
        direction = 2 if any(k in desc for k in ("收到", "收回", "取得", "吸收")) else 1
    return {
        "sort_index": idx,
        "is_result": is_result,
        "is_edit": is_edit,
        "is_title": is_title,
        "is_main": is_main,
        "is_additional": is_additional,
        "direction": direction,
    }


def main() -> None:
    text = ENUM_FILE.read_text(encoding="utf-8")
    entries = ENTRY_RE.findall(text)
    if not entries:
        raise SystemExit("No CashFlowItemEnum entries found")

    lines = [
        "-- Template rows for config_cash_flow_balance (book_id IS NULL)",
        "DELETE FROM config_cash_flow_balance WHERE book_id IS NULL;",
        "",
    ]
    for i, (db_code, desc) in enumerate(entries, start=1):
        f = flags(db_code, desc)
        row_id = f"cf-template-{i:03d}"
        lines.append(
            "INSERT INTO config_cash_flow_balance "
            "(id, item_name, item_code, sort_index, is_result, is_edit, is_title, balance, direction, "
            "book_id, is_main, is_additional) VALUES "
            f"('{row_id}', '{desc.replace(chr(39), chr(39)+chr(39))}', '{db_code}', {f['sort_index']}, "
            f"{f['is_result']}, {f['is_edit']}, {f['is_title']}, 0.00, {f['direction']}, "
            f"NULL, {f['is_main']}, {f['is_additional']});"
        )

    OUT_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {len(entries)} rows to {OUT_FILE}")


if __name__ == "__main__":
    main()
