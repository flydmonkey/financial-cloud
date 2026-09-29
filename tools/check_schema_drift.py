#!/usr/bin/env python3
"""Schema drift check: compare entity fields (@TableName) against init SQL table columns.

用法:
    python tools/check_schema_drift.py

输出每张表实体有而 DDL 缺失的列（camelCase→snake_case 简单映射，
跳过 @TableField(exist = false) 与 serialVersionUID）。用于初始化 SQL 巡检。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "sql" / "financial_cloud_init.sql"
DOMAIN = ROOT / "financial-cloud" / "src" / "main" / "java" / "com" / "financial" / "cloud" / "domain"

# 已知有意差异（实体字段非数据库列）白名单: (table, column)
ALLOWLIST: set[tuple[str, str]] = {
    # MaxKey 遗留字段，运行库从未建过这两列，业务代码也未写它们之外的场景
    ("history_synchronizer", "inst_name"),
    ("permission", "inst_name"),
}

# MaxKey 遗留实体，初始化库从不建表
ALLOWLIST_TABLES: set[str] = {"history_connector"}


def camel_to_snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def parse_init_tables(sql: str) -> dict[str, set[str]]:
    tables: dict[str, set[str]] = {}
    for m in re.finditer(r"CREATE TABLE(?: IF NOT EXISTS)? `([^`]+)` \((.*?)\) ENGINE=", sql, re.DOTALL):
        table, body = m.group(1), m.group(2)
        cols = set(re.findall(r"^\s*`([^`]+)`\s", body, re.MULTILINE))
        tables.setdefault(table, set()).update(cols)
    # ALTER TABLE 后置补列（如 voucher.source_voucher_id、history_system_logs.ip）
    for m in re.finditer(
        r"ALTER TABLE `([^`]+)`\s+(.*?);", sql, re.DOTALL
    ):
        table, body = m.group(1), m.group(2)
        if table not in tables:
            continue
        for cm in re.finditer(r"(?:ADD|MODIFY|CHANGE)\s+COLUMN\s+`([^`]+)`", body):
            tables[table].add(cm.group(1))
    return tables


def parse_entity(path: Path) -> tuple[str | None, set[str]]:
    text = path.read_text(encoding="utf-8", errors="replace")
    tm = re.search(r'@TableName\("([^"]+)"\)', text)
    if not tm:
        return None, set()
    table = tm.group(1)
    # 去掉 @TableField(exist = false) 标注的字段（含全限定名写法）
    text = re.sub(r"@(?:[\w.]+\.)?TableField\(exist\s*=\s*false\)[^;]*;", "", text, flags=re.DOTALL)
    fields = set()
    for fm in re.finditer(r"private\s+(?:final\s+)?[\w<>\[\],.? ]+\s+(\w+)\s*;", text):
        name = fm.group(1)
        if name in ("serialVersionUID",):
            continue
        fields.add(camel_to_snake(name))
    return table, fields


def main() -> int:
    tables = parse_init_tables(INIT_SQL.read_text(encoding="utf-8"))
    drift: list[str] = []
    checked = 0
    for path in sorted(DOMAIN.rglob("*.java")):
        table, fields = parse_entity(path)
        if not table:
            continue
        if table in ALLOWLIST_TABLES:
            continue
        if table not in tables:
            drift.append(f"{table}: 表不存在 (实体 {path.name})")
            continue
        checked += 1
        missing = sorted(f for f in fields if f not in tables[table] and (table, f) not in ALLOWLIST)
        if missing:
            drift.append(f"{table}: 缺列 {', '.join(missing)} (实体 {path.name})")
    print(f"checked {checked} entities against {len(tables)} init tables")
    if drift:
        print("\n".join(drift))
        return 1
    print("no drift")
    return 0


if __name__ == "__main__":
    sys.exit(main())
