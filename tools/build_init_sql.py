#!/usr/bin/env python3
"""Build a full database init SQL: schema + seed data only (no business/test data)."""
from __future__ import annotations

import re
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "sql"
SOURCE_SQL = SQL / "financial_cloud_v1.0.1.sql"
PATCHES = SQL / "patches"
SEED = SQL / "seed"
MENU_CLEANUP_SQL = PATCHES / "cleanup-dead-menus.sql"
MENU_RESTRUCTURE_SQL = PATCHES / "menu-restructure.sql"
ASSIST_ACC_SQL = PATCHES / "assist-acc-config.sql"
SUBJECT_SEED_SQL = SEED / "data" / "standard_subjects.sql"
CASH_FLOW_SEED_SQL = SEED / "data" / "config_cash_flow_balance.sql"
OUT_SQL = SQL / "financial_cloud_init.sql"
DB_NAME = "financial_cloud"
DEFAULT_ADMIN_PASSWORD = "changeme"

SCHEMA_EXTENSION_SQL = [
    SEED / "schema" / "fixed_asset_tables.sql",
    SEED / "schema" / "fixed_asset_change_tables.sql",
    SEED / "schema" / "fixed_asset_dispose_alter.sql",
    SEED / "schema" / "fixed_asset_purchase_alter.sql",
    SEED / "schema" / "fixed_asset_suspend_alter.sql",
    SEED / "schema" / "journal_account_prev_opening.sql",
]

MENU_SEED_SQL = [
    SEED / "menus" / "ledger_books_menu.sql",
    SEED / "menus" / "general_ledger_menu.sql",
    SEED / "menus" / "expense_detail_menu.sql",
    SEED / "menus" / "fixed_asset_menu.sql",
    SEED / "menus" / "menu_salary_tax_and_rename_config.sql",
    SEED / "menus" / "menu_icons_align.sql",
]

BALANCE_SHEET_RULES_SQL = [
    SEED / "rules" / "balance_sheet_reclassification_rules.sql",
    SEED / "rules" / "balance_sheet_inventory_fixed_asset_rules.sql",
    SEED / "rules" / "balance_sheet_bad_debt_rules.sql",
]

VOUCHER_SUMMARY_OLD = (
    "('1891486309700673537','凭证汇总表','凭证汇总表','MENU',"
    "'1891486309700673537','/statement/voucher-summary','GET',NULL,'r',NULL,NULL,"
    "'menus-caiwu-pingzhenghuizongbiao','n','n','n','y','1886357455563137026','财务报表',5"
)
VOUCHER_SUMMARY_NEW = (
    "('1891486309700673537','凭证汇总表','凭证汇总表','MENU',"
    "'1891486309700673537','/statement/voucher-summary','GET',NULL,'r',NULL,NULL,"
    "'menus-caiwu-pingzhenghuizongbiao','n','n','n','y','1869692874272862209','凭证',3"
)

DROP_TABLES = {
    "employee_salary_voucher_rule",
    "employee_salary_voucher_rule_template",
}

SEED_TABLES: dict[str, dict] = {
    "institutions": {},
    "userinfo": {"row_filter": lambda row: ",'admin'," in row},
    "standard": {},
    "roles": {},
    "role_member": {
        "row_filter": lambda row: "ROLE_ADMINISTRATORS" in row and ",'1','USER'" in row
    },
    "resources": {},
    "permission": {},
    "config": {"row_filter": lambda row: ",'template'," in row},
    "config_login_policy": {},
    "config_password_policy": {},
    "config_personal_tax": {"row_filter": lambda row: ",'n')" in row or row.rstrip().endswith(",'n')")},
    "standard_statement_balance_sheet": {},
    "standard_statement_income": {},
    "standard_statement_rules": {},
    "voucher_template": {"row_filter": lambda row: bool(re.match(r"\('[^']*','[12]',", row))},
    "voucher_template_item": {"row_filter": lambda row: bool(re.match(r"\('[^']*','[12]',", row))},
}

STANDARD_NAME_UPDATES = {
    "1": "小企业会计准则",
    "2": "企业会计制度",
}


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def strip_seed_header(sql: str) -> list[str]:
    return [
        line
        for line in sql.splitlines()
        if line.strip()
        and not line.startswith("-- Generated")
        and not line.startswith("SET NAMES")
        and not line.startswith("-- Template rows")
    ]


def append_sql_chunks(chunks: list[str], paths: list[Path]) -> None:
    for path in paths:
        if not path.exists():
            print(f"Warning: missing {path}", file=sys.stderr)
            continue
        chunks.append(read_text(path).strip())


def extract_create_blocks(text: str) -> list[tuple[str, str]]:
    pattern = re.compile(
        r"(--\s*\n-- Table structure for table `([^`]+)`\s*\n--\s*\n"
        r"DROP TABLE IF EXISTS `[^`]+`;\s*"
        r"/\*!40101 SET @saved_cs_client.*?"
        r"CREATE TABLE `[^`]+` \(.*?\) ENGINE=.*?;"
        r"\s*/\*!40101 SET character_set_client = @saved_cs_client \*/;)",
        re.DOTALL,
    )
    blocks: list[tuple[str, str]] = []
    for match in pattern.finditer(text):
        table = match.group(2)
        if table in DROP_TABLES:
            continue
        blocks.append((table, match.group(1)))
    return blocks


def split_rows(values_sql: str) -> list[str]:
    rows: list[str] = []
    depth = 0
    start = 0
    in_quote = False
    i = values_sql.find("VALUES")
    if i < 0:
        return rows
    i += 6
    while i < len(values_sql):
        ch = values_sql[i]
        if ch == "'" and values_sql[i - 1] != "\\":
            in_quote = not in_quote
        elif not in_quote:
            if ch == "(":
                if depth == 0:
                    start = i
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    rows.append(values_sql[start : i + 1])
        i += 1
    return rows


def extract_insert_block(text: str, table: str) -> str | None:
    pattern = (
        rf"-- Dumping data for table `{re.escape(table)}`\s*\n--\s*\n"
        rf"LOCK TABLES `{re.escape(table)}` WRITE;\s*"
        rf"/\*!40000 ALTER TABLE `{re.escape(table)}` DISABLE KEYS \*/;\s*"
        rf"(INSERT INTO `{re.escape(table)}` VALUES .*?);"
        rf"\n/\*!40000 ALTER TABLE `{re.escape(table)}` ENABLE KEYS \*/;"
    )
    match = re.search(pattern, text, re.DOTALL)
    return match.group(1) if match else None


def ensure_semicolon(sql: str) -> str:
    sql = sql.strip()
    return sql if sql.endswith(";") else f"{sql};"


def build_seed_insert(text: str, table: str, options: dict) -> str | None:
    block = extract_insert_block(text, table)
    if not block:
        return None
    row_filter = options.get("row_filter")
    if not row_filter:
        return ensure_semicolon(block)
    rows = split_rows(block)
    kept = [row for row in rows if row_filter(row)]
    if not kept:
        return None
    return ensure_semicolon(f"INSERT INTO `{table}` VALUES {','.join(kept)}")


def patch_standard_rows(insert_sql: str) -> str:
    for standard_id, name in STANDARD_NAME_UPDATES.items():
        insert_sql = re.sub(
            rf"\('{standard_id}','[^']*'",
            f"('{standard_id}','{name}'",
            insert_sql,
            count=1,
        )
    return insert_sql


def patch_institutions(insert_sql: str) -> str:
    insert_sql = (
        insert_sql.replace("'jinbooks','jinbooks'", "'financial-cloud','financial-cloud'")
        .replace(
            "'sso.maxkey.top','financial-cloud','financial-cloud'",
            "'localhost','financial-cloud','financial-cloud'",
        )
        .replace("sso.maxkey.top", "localhost")
        .replace("mgt.maxkey.top", "localhost")
    )
    return insert_sql


def patch_admin_user(insert_sql: str) -> str:
    insert_sql = re.sub(
        r",NULL,NULL,'\d{10,}',1,'n'\);$",
        ",NULL,NULL,'',0,'n');",
        insert_sql,
    )
    insert_sql = insert_sql.replace("shimingxy@qq.com", "admin@localhost")
    insert_sql = insert_sql.replace("15618726256", "")
    insert_sql = insert_sql.replace("{plain}maxkey", f"{{plain}}{DEFAULT_ADMIN_PASSWORD}")
    insert_sql = insert_sql.replace("http://login.maxkey.org/", "")
    return insert_sql


def patch_resources(insert_sql: str) -> str:
    insert_sql = patch_voucher_summary_menu(insert_sql)
    insert_sql = insert_sql.replace("'JinBooks'", "'Financial Cloud'")
    insert_sql = insert_sql.replace("MaxKey管理系统", "财务云")
    insert_sql = insert_sql.replace("MaxKey", "Financial Cloud")
    return insert_sql.replace("mxk.menu.", "fc.menu.")


def patch_voucher_summary_menu(insert_sql: str) -> str:
    if VOUCHER_SUMMARY_OLD in insert_sql:
        return insert_sql.replace(VOUCHER_SUMMARY_OLD, VOUCHER_SUMMARY_NEW, 1)
    return insert_sql


def patch_schema_block(table: str, block: str) -> str:
    if table == "voucher_template" and "`voucher_date`" not in block:
        block = block.replace(
            "  `voucher_type` tinyint NOT NULL DEFAULT '0' COMMENT '凭证类型:0-计提,1-发放',\n",
            "  `voucher_type` tinyint NOT NULL DEFAULT '0' COMMENT '凭证类型:0-计提,1-发放',\n"
            "  `voucher_date` smallint DEFAULT 0 COMMENT '默认凭证日期，为月份的第几天，0为月末',\n",
        )
    if table == "employee_salary" and "`pay_amount`" not in block:
        block = block.replace(
            "  `personal_tax` decimal(10,2) DEFAULT '0.00' COMMENT '个税',\n",
            "  `personal_tax` decimal(10,2) DEFAULT '0.00' COMMENT '个税',\n"
            "  `pay_amount` decimal(10,5) DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣',\n",
            1,
        )
        if "`accrual_voucher_id`" not in block:
            block = block.replace(
                "  `deleted` varchar(1)",
                "  `accrual_voucher_id` varchar(45) DEFAULT NULL COMMENT '收票凭证编码',\n"
                "  `salary_voucher_id` varchar(45) DEFAULT NULL COMMENT '发放凭证编码',\n"
                "  `deleted` varchar(1)",
                1,
            )
    if table in {"employee_salary_temp", "employee_salary_summary"} and "`pay_amount`" not in block:
        block = block.replace(
            "  `personal_tax` decimal(10,2) DEFAULT '0.00' COMMENT '个税',\n",
            "  `personal_tax` decimal(10,2) DEFAULT '0.00' COMMENT '个税',\n"
            "  `pay_amount` decimal(10,5) DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣',\n",
            1,
        )
    if table == "employee_salary_summary" and "`accrual_voucher_id`" in block:
        block = re.sub(
            r"\n  `accrual_voucher_id` varchar\(45\) DEFAULT NULL COMMENT '计提凭证编码',"
            r"\n  `salary_voucher_id` varchar\(45\) DEFAULT NULL COMMENT '发放凭证编码',",
            "",
            block,
        )
    if table == "statement_subject_balance" and "`prev_balance`" not in block:
        block = block.replace(
            "  `closing_balance_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '期末余额（贷方）',\n",
            "  `closing_balance_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '期末余额（贷方）',\n"
            "  `prev_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月末余额',\n"
            "  `prev_closing_balance_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月期末余额（借方）',\n"
            "  `prev_closing_balance_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月期末余额（贷方）',\n"
            "  `prev_year_to_date_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月本年累计发生额（借方）',\n"
            "  `prev_year_to_date_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月本年累计发生额（贷方）',\n",
        )
    if table == "voucher_item" and "`subject_balance` decimal(10,2)" not in block:
        block = block.replace(
            "`subject_balance` decimal(18,2)",
            "`subject_balance` decimal(10,2)",
        )
    return block


def patch_voucher_template_insert(insert_sql: str) -> str:
    rows = split_rows(insert_sql)
    patched = [
        re.sub(
            r",(-?\d+),'([^']*)',(\d+),(\d+),'1','(\d{4}-)",
            r",\1,0,'\2',\3,\4,'1','\5",
            row,
            count=1,
        )
        for row in rows
    ]
    return ensure_semicolon(f"INSERT INTO `voucher_template` VALUES {','.join(patched)}")


def load_post_schema_sql() -> list[str]:
    chunks: list[str] = []
    append_sql_chunks(chunks, [MENU_CLEANUP_SQL, MENU_RESTRUCTURE_SQL, ASSIST_ACC_SQL])
    return chunks


def load_schema_extension_sql() -> list[str]:
    chunks: list[str] = []
    append_sql_chunks(chunks, SCHEMA_EXTENSION_SQL)
    return chunks


def load_menu_seed_sql() -> list[str]:
    chunks: list[str] = []
    append_sql_chunks(chunks, MENU_SEED_SQL)
    return chunks


def load_balance_sheet_rules_sql() -> list[str]:
    chunks: list[str] = []
    append_sql_chunks(chunks, BALANCE_SHEET_RULES_SQL)
    return chunks


def main() -> int:
    if not SOURCE_SQL.exists():
        print(f"Missing {SOURCE_SQL}", file=sys.stderr)
        return 1

    source = read_text(SOURCE_SQL)
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    lines: list[str] = [
        "-- Financial Cloud full init SQL (schema + seed data, no business/test data)",
        f"-- Generated at: {now}",
        "-- Generator: python tools/build_init_sql.py",
        "",
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "",
        f"CREATE DATABASE IF NOT EXISTS `{DB_NAME}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
        f"USE `{DB_NAME}`;",
        "",
        "-- ------------------------------------------------------------------",
        "-- Schema",
        "-- ------------------------------------------------------------------",
        "",
    ]

    create_blocks = extract_create_blocks(source)
    for table, block in create_blocks:
        lines.append(patch_schema_block(table, block))
        lines.append("")

    lines.extend(
        [
            "-- ------------------------------------------------------------------",
            "-- Post-schema patches",
            "-- ------------------------------------------------------------------",
            "",
        ]
    )
    for chunk in load_post_schema_sql():
        lines.append(chunk)
        lines.append("")

    lines.extend(
        [
            "-- ------------------------------------------------------------------",
            "-- Schema extensions (fixed asset, etc.)",
            "-- ------------------------------------------------------------------",
            "",
        ]
    )
    for chunk in load_schema_extension_sql():
        lines.append(chunk)
        lines.append("")

    lines.extend(
        [
            "-- ------------------------------------------------------------------",
            "-- Seed data",
            "-- ------------------------------------------------------------------",
            "",
        ]
    )

    for table, options in SEED_TABLES.items():
        insert_sql = build_seed_insert(source, table, options)
        if not insert_sql:
            continue
        if table == "standard":
            insert_sql = patch_standard_rows(insert_sql)
        if table == "institutions":
            insert_sql = patch_institutions(insert_sql)
        if table == "userinfo":
            insert_sql = patch_admin_user(insert_sql)
        if table == "resources":
            insert_sql = patch_resources(insert_sql)
        if table == "voucher_template":
            insert_sql = patch_voucher_template_insert(insert_sql)
        lines.append(f"-- {table}")
        lines.append(f"LOCK TABLES `{table}` WRITE;")
        lines.append(f"/*!40000 ALTER TABLE `{table}` DISABLE KEYS */;")
        lines.append(insert_sql)
        lines.append(f"/*!40000 ALTER TABLE `{table}` ENABLE KEYS */;")
        lines.append("UNLOCK TABLES;")
        lines.append("")

    if SUBJECT_SEED_SQL.exists():
        lines.append("-- standard_subject (from docs/*.xlsx)")
        lines.extend(strip_seed_header(read_text(SUBJECT_SEED_SQL)))
        lines.append("")

    if CASH_FLOW_SEED_SQL.exists():
        lines.append("-- config_cash_flow_balance templates (book_id IS NULL)")
        lines.extend(strip_seed_header(read_text(CASH_FLOW_SEED_SQL)))
        lines.append("")

    lines.extend(
        [
            "-- ------------------------------------------------------------------",
            "-- Menu seeds",
            "-- ------------------------------------------------------------------",
            "",
        ]
    )
    for chunk in load_menu_seed_sql():
        lines.append(chunk)
        lines.append("")

    lines.extend(
        [
            "-- ------------------------------------------------------------------",
            "-- Balance sheet rules",
            "-- ------------------------------------------------------------------",
            "",
        ]
    )
    for chunk in load_balance_sheet_rules_sql():
        lines.append(chunk)
        lines.append("")

    lines.extend(
        [
            "SET FOREIGN_KEY_CHECKS = 1;",
            "",
            f"-- Default admin: username=admin password={DEFAULT_ADMIN_PASSWORD} (change after first login)",
            "",
        ]
    )

    OUT_SQL.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {OUT_SQL} ({OUT_SQL.stat().st_size} bytes)")
    print(f"Tables in schema: {len(create_blocks)}")
    print(
        "Seed: core tables + standard_subject + cash_flow + "
        f"{len(MENU_SEED_SQL)} menu scripts + {len(BALANCE_SHEET_RULES_SQL)} rule scripts"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
