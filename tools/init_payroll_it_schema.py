#!/usr/bin/env python3
"""Create a fresh, empty payroll IT schema from fixed repository DDL.

Unlike run_init_sql.py, this command never resets, seeds, copies or drops a
database. The complete source and selected DDL are checked before connecting.
``--observe`` only reads the same dedicated namespace in a read-only transaction.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import sys
from typing import Callable


ROOT = Path(__file__).resolve().parents[1]
INIT_SQL = ROOT / "sql/financial_cloud_init.sql"
INDEX_PATCH = ROOT / "sql/patches/2026-10-03-payroll-write-scope-index.sql"
EXPECTED_MYSQL_VERSION = "9.4.0"
NAMESPACE = r"financial_cloud_e2e_20261003_it4_[a-z0-9_]+"
SCOPE_COLUMNS = ["book_id", "belong_date", "employee_id", "created_date"]
SCOPE_INDEX = "idx_salary_payroll_scope"
# Whitespace/comment-insensitive approval of the whole production patch, not
# permission to run arbitrary PREPARE. Its single ALTER literal is extracted.
APPROVED_PATCH_SHA256 = "d9fa39bd5ab0c060c295b1565c54de5dd7f0691d8f0f14c965f97238f89dbc9f"
IDENTIFIER = r"[A-Za-z_][A-Za-z0-9_]*"
QUOTED_IDENTIFIER = rf"`({IDENTIFIER})`"
REQUIRED_TABLES = {
    "book", "employee", "employee_salary", "employee_salary_temp", "voucher",
    "voucher_item", "voucher_word", "voucher_auxiliary", "voucher_item_cash_flow",
    "voucher_template", "voucher_template_item", "settlement_carryforward",
}
TYPES = {"bigint", "bit", "char", "date", "datetime", "decimal", "enum", "int",
         "longblob", "smallint", "text", "timestamp", "tinyint", "varchar"}


class SchemaError(ValueError):
    """A fixed, credential-free validation failure."""


@dataclass(frozen=True)
class Settings:
    host: str
    port: int
    database: str
    user: str = field(repr=False)
    password: str = field(repr=False)


@dataclass(frozen=True)
class SchemaPlan:
    statements: tuple[str, ...]
    tables: dict[str, tuple[str, ...]]
    selections: tuple[dict, ...]
    sources: dict[str, str]
    sha256: str
    redundant_alters: tuple[dict, ...] = ()

    def evidence(self) -> dict:
        return {"sha256": self.sha256, "sources": self.sources,
                "tableCount": len(self.tables), "statementCount": len(self.statements),
                "tables": sorted(self.tables), "selections": list(self.selections),
                "statements": list(self.statements), "redundantAlters": list(self.redundant_alters)}


def sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def connection_settings(environment: dict[str, str]) -> Settings:
    required = ("FC_DB_HOST", "FC_DB_PORT", "FC_DB_NAME", "FC_DB_USER", "FC_DB_PASSWORD")
    if any(not isinstance(environment.get(name), str) or not environment[name] for name in required):
        raise SchemaError("All five FC_DB settings must be explicitly nonempty")
    database = environment["FC_DB_NAME"]
    if len(database) > 64 or not re.fullmatch(NAMESPACE, database):
        raise SchemaError("Database must be in the dedicated payroll IT namespace and at most 64 characters")
    host = environment["FC_DB_HOST"]
    if not re.fullmatch(r"[A-Za-z0-9_.:-]+", host):
        raise SchemaError("Database host must be a plain hostname or address")
    port = environment["FC_DB_PORT"]
    if not re.fullmatch(r"[0-9]+", port) or not 1 <= int(port) <= 65535:
        raise SchemaError("Database port must be explicitly between 1 and 65535")
    return Settings(host, int(port), database, environment["FC_DB_USER"], environment["FC_DB_PASSWORD"])


def split_sql(source: str) -> list[str]:
    """Split real statements, exposing executable version comments for checking."""
    result: list[str] = []
    chunk: list[str] = []
    quote = None
    position = 0
    while position < len(source):
        char = source[position]
        if quote:
            chunk.append(char)
            if char == "\\" and quote != "`":
                position += 1
                if position >= len(source):
                    raise SchemaError("Unterminated SQL escape")
                chunk.append(source[position])
            elif char == quote:
                if position + 1 < len(source) and source[position + 1] == quote:
                    position += 1
                    chunk.append(source[position])
                else:
                    quote = None
            position += 1
            continue
        if char in "'\"`":
            quote = char
            chunk.append(char)
            position += 1
            continue
        if char == "#" or (source.startswith("--", position)
                           and (position + 2 == len(source) or source[position + 2].isspace())):
            newline = source.find("\n", position)
            position = len(source) if newline < 0 else newline + 1
            chunk.append(" ")
            continue
        if source.startswith("/*", position):
            end = source.find("*/", position + 2)
            if end < 0:
                raise SchemaError("Unterminated SQL comment")
            payload = source[position + 2:end]
            if payload.startswith("!"):
                match = re.fullmatch(r"!\d{5,6}\s*(.*)", payload, re.DOTALL)
                if not match:
                    raise SchemaError("Unsupported executable SQL comment")
                # Reinsert the payload, so an embedded semicolon or DDL is
                # classified rather than silently discarded as a comment.
                source = source[:position] + " " + match[1] + " " + source[end + 2:]
                continue
            if payload.startswith("+"):
                raise SchemaError("Unsupported SQL hint comment")
            chunk.append(" ")
            position = end + 2
            continue
        if char == ";":
            statement = "".join(chunk).strip()
            if statement:
                result.append(statement)
            chunk = []
        else:
            chunk.append(char)
        position += 1
    if quote:
        raise SchemaError("Unterminated SQL quote")
    if "".join(chunk).strip():
        result.append("".join(chunk).strip())
    return result


def tokens(statement: str) -> list[str]:
    pattern = re.compile(r"`[A-Za-z_][A-Za-z0-9_]*`|'(?:\\.|''|[^'\\])*'|[A-Za-z_][A-Za-z0-9_]*|\d+(?:\.\d+)?|[(),=+.-]")
    values = []
    end = 0
    for match in pattern.finditer(statement):
        if statement[end:match.start()].strip():
            raise SchemaError("Unsupported DDL token")
        values.append(match[0])
        end = match.end()
    if statement[end:].strip():
        raise SchemaError("Unsupported DDL token")
    return values


def comma_parts(value: str) -> list[str]:
    """Split DDL clauses without splitting decimals, enums or quoted comments."""
    values = tokens(value)
    depth = 0
    start = 0
    parts = []
    for index, token in enumerate(values):
        if token == "(":
            depth += 1
        elif token == ")":
            depth -= 1
        elif token == "," and depth == 0:
            parts.append(" ".join(values[start:index]))
            start = index + 1
        if depth < 0:
            raise SchemaError("Unbalanced DDL parentheses")
    if depth:
        raise SchemaError("Unbalanced DDL parentheses")
    parts.append(" ".join(values[start:]))
    if any(not part for part in parts):
        raise SchemaError("Empty DDL clause")
    return parts


def timestamp_end(values: list[str], index: int) -> int:
    if index >= len(values) or values[index].upper() != "CURRENT_TIMESTAMP":
        raise SchemaError("Unsupported timestamp expression")
    index += 1
    if values[index:index + 2] == ["(", ")"]:
        return index + 2
    if index < len(values) and values[index] == "(":
        if index + 2 >= len(values) or not re.fullmatch(r"[0-6]", values[index + 1]) or values[index + 2] != ")":
            raise SchemaError("Unsupported timestamp precision")
        return index + 3
    return index


def column_name(definition: str) -> str:
    values = tokens(definition)
    if len(values) < 2 or not re.fullmatch(QUOTED_IDENTIFIER, values[0]) or values[1].lower() not in TYPES:
        raise SchemaError("Unsupported column definition")
    name = values[0][1:-1]
    index = 2
    if index < len(values) and values[index] == "(":
        index += 1
        dimensions = []
        while index < len(values) and values[index] != ")":
            dimensions.append(values[index])
            index += 1
        if index == len(values) or not dimensions:
            raise SchemaError("Unsupported column type parameters")
        atom = r"'(?:\\.|''|[^'\\])*'" if values[1].lower() == "enum" else r"\d+"
        if not re.fullmatch(rf"{atom}(?: , {atom})*", " ".join(dimensions)):
            raise SchemaError("Unsupported column type parameters")
        index += 1
    attributes = set()
    while index < len(values):
        word = values[index].upper()
        if word in attributes:
            raise SchemaError("Duplicate column attribute")
        attributes.add(word)
        index += 1
        if word == "UNSIGNED" or word == "AUTO_INCREMENT":
            continue
        if word == "NOT":
            if "NULL" in attributes or index >= len(values) or values[index].upper() != "NULL":
                raise SchemaError("Unsupported column nullability")
            index += 1
        elif word == "NULL":
            if "NOT" in attributes:
                raise SchemaError("Conflicting column nullability")
            continue
        elif word == "ON":
            if index >= len(values) or values[index].upper() != "UPDATE":
                raise SchemaError("Unsupported timestamp update")
            index = timestamp_end(values, index + 1)
        elif word == "CHARACTER":
            if index + 1 >= len(values) or values[index].upper() != "SET" or values[index + 1] not in {"ascii", "utf8mb3", "utf8mb4"}:
                raise SchemaError("Unsupported column character set")
            index += 2
        elif word in {"COLLATE", "COMMENT"}:
            if index >= len(values) or not (values[index].startswith("'") if word == "COMMENT"
                                           else re.fullmatch(r"(?:utf8mb[34]|ascii)_[a-z0-9_]+", values[index])):
                raise SchemaError("Unsupported column attribute value")
            index += 1
        elif word == "DEFAULT":
            if index >= len(values):
                raise SchemaError("Missing column default")
            value = values[index]
            if value in {"+", "-"}:
                index += 1
                if index >= len(values):
                    raise SchemaError("Missing signed column default")
                value = values[index]
                if not re.fullmatch(r"\d+(?:\.\d+)?", value):
                    raise SchemaError("Signed column default must be numeric")
            if value.upper() == "B" and index + 1 < len(values) and re.fullmatch(r"'[01]+'", values[index + 1]):
                index += 2
            elif value.startswith("'") or re.fullmatch(r"\d+(?:\.\d+)?", value) or value.upper() == "NULL":
                index += 1
            elif value.upper() == "CURRENT_TIMESTAMP":
                index = timestamp_end(values, index)
            else:
                raise SchemaError("Unsupported column default expression")
        else:
            raise SchemaError("Unsupported column attribute")
    return name


def index_columns(definition: str, columns: list[str]) -> str:
    match = re.fullmatch(rf"(?:PRIMARY KEY|(?:UNIQUE )?(?:KEY|INDEX) `{IDENTIFIER}`)\s*\((.+)\)", definition, re.IGNORECASE)
    if not match:
        raise SchemaError("Unsupported index definition")
    index_columns = []
    for part in comma_parts(match[1]):
        if not re.fullmatch(QUOTED_IDENTIFIER, part):
            raise SchemaError("Unsupported index column")
        index_columns.append(part[1:-1])
    if len(set(index_columns)) != len(index_columns) or not set(index_columns) <= set(columns):
        raise SchemaError("Index refers to unknown or repeated columns")
    name = re.search(rf"(?:KEY|INDEX) `{IDENTIFIER}`", definition, re.IGNORECASE)
    return name[0].split("`")[1].lower() if name else "primary"


def validate_ddl(statements: tuple[str, ...]) -> dict[str, tuple[str, ...]]:
    tables: dict[str, list[str]] = {}
    table_indices: dict[str, set[str]] = {}
    for statement in statements:
        create = re.fullmatch(rf"CREATE TABLE {QUOTED_IDENTIFIER}\s*\((.*)\)\s*(ENGINE\s*=.*)", statement, re.DOTALL | re.IGNORECASE)
        if create:
            table, body, suffix = create.groups()
            if table in tables or not re.fullmatch(r"[a-z][a-z0-9_]*", table):
                raise SchemaError("Duplicate or unsupported table name")
            attributes = tokens(suffix)
            if attributes[:6] != ["ENGINE", "=", "InnoDB", "DEFAULT", "CHARSET", "="] or len(attributes) < 7 or attributes[6] not in {"utf8mb3", "utf8mb4"}:
                raise SchemaError("Only repository InnoDB table options are supported")
            position = 7
            seen = set()
            while position < len(attributes):
                key = attributes[position].upper()
                if key in seen or attributes[position + 1:position + 2] != ["="] or position + 2 >= len(attributes):
                    raise SchemaError("Unsupported table option")
                seen.add(key)
                value = attributes[position + 2]
                if not ((key == "COLLATE" and re.fullmatch(r"utf8mb[34]_[a-z0-9_]+", value))
                        or (key == "ROW_FORMAT" and value == "DYNAMIC") or (key == "COMMENT" and value.startswith("'"))):
                    raise SchemaError("Unsupported table option")
                position += 3
            columns = []
            indices = []
            for clause in comma_parts(body):
                if clause.startswith("`"):
                    name = column_name(clause)
                    if name.lower() in {existing.lower() for existing in columns}:
                        raise SchemaError("Duplicate column")
                    columns.append(name)
                else:
                    indices.append(clause)
            if not columns:
                raise SchemaError("Repository table must define columns")
            index_names = set()
            for key in indices:
                index_name = index_columns(key, columns)
                if index_name in index_names:
                    raise SchemaError("Duplicate index name in repository CREATE")
                index_names.add(index_name)
            tables[table] = columns
            table_indices[table] = index_names
            continue
        alter = re.fullmatch(rf"ALTER TABLE {QUOTED_IDENTIFIER}\s+(.+)", statement, re.DOTALL | re.IGNORECASE)
        if not alter or alter[1] not in tables:
            raise SchemaError("Execution plan contains unsupported or cross-database DDL")
        columns = tables[alter[1]]
        for action in comma_parts(alter[2]):
            column = re.fullmatch(r"(ADD|MODIFY) COLUMN (.+)", action, re.IGNORECASE | re.DOTALL)
            if column:
                definition = column[2]
                after = re.search(rf"\s+AFTER {QUOTED_IDENTIFIER}$", definition, re.IGNORECASE)
                if after:
                    if after[1] not in columns:
                        raise SchemaError("ALTER AFTER refers to an unknown column")
                    definition = definition[:after.start()]
                name = column_name(definition)
                if (column[1].upper() == "ADD") == (name in columns):
                    raise SchemaError("ALTER column does not match current table structure")
                if column[1].upper() == "ADD":
                    columns.append(name)
            elif action.upper().startswith("ADD "):
                index_name = index_columns(action[4:], columns)
                if index_name in table_indices[alter[1]]:
                    raise SchemaError("ALTER attempts to add an existing index")
                table_indices[alter[1]].add(index_name)
            else:
                raise SchemaError("Unsupported ALTER operation")
    if not REQUIRED_TABLES <= set(tables):
        raise SchemaError("Execution plan is missing required payroll tables")
    return {name: tuple(columns) for name, columns in sorted(tables.items())}


def approved_index_patch(source: str) -> tuple[list[str], str]:
    statements = split_sql(source)
    canonical = " ".join(("; ".join(statements) + ";").split())
    if sha256(canonical.encode()) != APPROVED_PATCH_SHA256:
        raise SchemaError("Production payroll index patch is outside the approved boundary")
    match = re.search(r"'(ALTER TABLE `employee_salary` ADD INDEX `idx_salary_payroll_scope` \(`book_id`, `belong_date`, `employee_id`, `created_date`\))'", statements[0])
    if len(statements) != 4 or not match:
        raise SchemaError("Approved payroll index payload is missing")
    return statements, match[1]


def discarded_statement(statement: str, section: str) -> bool:
    normalized = " ".join(statement.split())
    if re.match(r"^(INSERT|UPDATE|DELETE)\b", statement, re.IGNORECASE):
        return True
    if re.fullmatch(rf"DROP TABLE IF EXISTS {QUOTED_IDENTIFIER}", normalized, re.IGNORECASE):
        return section == "schema"
    if normalized in {"SET NAMES utf8mb4", "SET FOREIGN_KEY_CHECKS = 0", "SET FOREIGN_KEY_CHECKS = 1",
                      "SET @saved_cs_client = @@character_set_client", "SET character_set_client = utf8mb4",
                      "SET character_set_client = @saved_cs_client", "UNLOCK TABLES"}:
        return True
    if re.fullmatch(r"SET @[a-z][a-z0-9_]* = '(?:\\.|''|[^'\\])*'", normalized, re.IGNORECASE):
        return True
    if section == "seed" and (re.fullmatch(rf"LOCK TABLES {QUOTED_IDENTIFIER} WRITE", normalized, re.IGNORECASE)
                              or re.fullmatch(rf"ALTER TABLE {QUOTED_IDENTIFIER} (?:DISABLE|ENABLE) KEYS", normalized, re.IGNORECASE)):
        return True
    return False


def exclude_known_redundant_alters(statements: list[str], selections: list[dict]) -> tuple[list[str], list[dict], list[dict]]:
    """The production init already puts three historical ADDs in its CREATE.

    Do not swallow duplicate-column errors. Prove these exact known ADDs are
    redundant offline, before the database exists, and retain the source proof.
    Comments differ for suspended_period; comments and unexecuted AFTER order
    have no effect on the existing CREATE's structure or column defaults.
    """
    expected = {"dispose_voucher_id": "remark", "purchase_voucher_id": "dispose_voucher_id",
                "suspended_period": "disposed_period"}
    definitions = {}
    for statement in statements:
        match = re.fullmatch(r"CREATE TABLE `fixed_asset`\s*\((.*)\)\s*ENGINE.*", statement, re.DOTALL)
        if match:
            for clause in comma_parts(match[1]):
                if clause.startswith("`"):
                    definitions[column_name(clause)] = clause

    def structural_tokens(definition):
        values = tokens(definition)
        if "COMMENT" in values:
            index = values.index("COMMENT")
            values = values[:index] + values[index + 2:]
        return values

    kept, kept_selections, excluded = [], [], []
    for statement, selection in zip(statements, selections):
        match = re.fullmatch(r"ALTER TABLE `fixed_asset`\s+ADD COLUMN (.+)", statement, re.DOTALL)
        if match:
            after = re.search(rf"\s+AFTER {QUOTED_IDENTIFIER}$", match[1])
            definition = match[1][:after.start()] if after else match[1]
            name = column_name(definition)
            if name in expected and name in definitions:
                if not after or after[1] != expected[name] or structural_tokens(definition) != structural_tokens(definitions[name]):
                    raise SchemaError("Historical fixed-asset ADD conflicts with repository CREATE")
                excluded.append({**selection, "reason": "Identical existing column structure in fixed_asset CREATE",
                                 "column": name, "sourceSqlSha256": sha256(statement.encode("utf-8"))})
                continue
        kept.append(statement)
        kept_selections.append(selection)
    return kept, kept_selections, excluded


def build_schema_plan(init_source: str | None = None, patch_source: str | None = None) -> SchemaPlan:
    """Fixed files in production; text arguments exist for offline safety tests."""
    init_bytes = INIT_SQL.read_bytes() if init_source is None else init_source.encode("utf-8")
    patch_bytes = INDEX_PATCH.read_bytes() if patch_source is None else patch_source.encode("utf-8")
    source = init_bytes.decode("utf-8")
    patch = patch_bytes.decode("utf-8")
    starts = list(re.finditer(r"^-- Schema\s*$", source, re.MULTILINE))
    ends = list(re.finditer(r"^-- Seed data\s*$", source, re.MULTILINE))
    if len(starts) != 1 or len(ends) != 1 or starts[0].end() >= ends[0].start():
        raise SchemaError("Repository schema section markers are missing or ambiguous")
    patch_statements, index_ddl = approved_index_patch(patch)
    canonical_patch = [" ".join(item.split()) for item in patch_statements]
    sections = {"header": source[:starts[0].start()], "schema": source[starts[0].end():ends[0].start()],
                "seed": source[ends[0].end():]}
    selected = []
    selections = []
    patch_count = 0
    discarded_drops = set()
    for section, text in sections.items():
        statements = split_sql(text)
        position = 0
        while position < len(statements):
            statement = statements[position]
            normalized = " ".join(statement.split())
            dropped = re.fullmatch(rf"DROP TABLE IF EXISTS {QUOTED_IDENTIFIER}", normalized)
            if section == "schema" and dropped:
                discarded_drops.add(dropped[1])
            if section == "header" and normalized in {
                "CREATE DATABASE IF NOT EXISTS `financial_cloud` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                "USE `financial_cloud`"}:
                position += 1
                continue
            if section == "schema" and normalized == canonical_patch[0]:
                if [" ".join(item.split()) for item in statements[position:position + 4]] != canonical_patch:
                    raise SchemaError("Initialization index block differs from the production patch")
                patch_count += 1
                selected.append(index_ddl)
                selections.append({"source": "payroll-index-patch", "section": section,
                                   "statementIndex": position, "sourceStatementCount": 4,
                                   "sourceSql": patch_statements})
                position += 4
                continue
            if section == "schema" and re.match(r"^(CREATE TABLE|ALTER TABLE)\b", statement, re.IGNORECASE):
                selected.append(re.sub(r"^CREATE TABLE IF NOT EXISTS\b", "CREATE TABLE", statement, flags=re.IGNORECASE))
                selections.append({"source": "financial-cloud-init", "section": section,
                                   "statementIndex": position, "sourceStatementCount": 1,
                                   "sourceSql": statement})
            elif not discarded_statement(statement, section):
                raise SchemaError("Unrecognized statement or structural change outside the schema boundary")
            position += 1
    if patch_count != 1:
        raise SchemaError("Repository schema must contain exactly one approved payroll index block")
    selected, selections, redundant = exclude_known_redundant_alters(selected, selections)
    tables = validate_ddl(tuple(selected))
    if not discarded_drops <= set(tables):
        raise SchemaError("Source DROP refers to a table outside the repository creation plan")
    digest = sha256(json.dumps(selected, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))
    return SchemaPlan(tuple(selected), tables, tuple(selections),
                      {"sql/financial_cloud_init.sql": sha256(init_bytes),
                       "sql/patches/2026-10-03-payroll-write-scope-index.sql": sha256(patch_bytes)}, digest,
                      tuple(redundant))


def validate_plan(plan: SchemaPlan) -> None:
    if validate_ddl(plan.statements) != plan.tables or len(plan.selections) != len(plan.statements):
        raise SchemaError("Execution plan metadata does not match its DDL")
    digest = sha256(json.dumps(list(plan.statements), ensure_ascii=False, separators=(",", ":")).encode("utf-8"))
    if digest != plan.sha256 or not re.fullmatch(r"[0-9a-f]{64}", plan.sha256):
        raise SchemaError("Execution plan hash does not match its DDL")


def mysql_connect(settings: Settings):
    import pymysql
    # No MULTI_STATEMENTS and no automatic target selection or fallback login.
    return pymysql.connect(host=settings.host, port=settings.port, user=settings.user,
                           password=settings.password, charset="utf8mb4", autocommit=True,
                           connect_timeout=10, read_timeout=20, write_timeout=20,
                           cursorclass=pymysql.cursors.DictCursor)


def observe_server(cursor, evidence: dict) -> None:
    cursor.execute("SELECT VERSION() AS `version`, @@version_comment AS `versionComment`, "
                   "@@global.transaction_isolation AS `globalIsolation`, "
                   "@@session.transaction_isolation AS `sessionIsolation`, "
                   "@@performance_schema AS `performanceSchema`, "
                   "@@session.transaction_read_only AS `transactionReadOnly`, DATABASE() AS `database`")
    server = cursor.fetchone()
    evidence["observed"] = server
    if not isinstance(server.get("version"), str) or server["version"].split("-", 1)[0] != EXPECTED_MYSQL_VERSION:
        raise SchemaError("Observed MySQL version does not equal required 9.4.0")
    if server.get("sessionIsolation") != "REPEATABLE-READ" or server.get("globalIsolation") != "REPEATABLE-READ":
        raise SchemaError("Observed default transaction isolation must be REPEATABLE-READ")
    if server.get("performanceSchema") != 1:
        raise SchemaError("Actual performance_schema must be enabled")
    permissions = {}
    evidence["lockObservation"] = permissions
    for table in ("data_lock_waits", "data_locks", "threads"):
        permissions[table] = {"readable": False}
        cursor.execute(f"SELECT COUNT(*) AS `count` FROM performance_schema.{table}")
        permissions[table] = {"readable": True, "observedRows": cursor.fetchone()["count"]}


def observe_schema(cursor, settings: Settings, plan: SchemaPlan, evidence: dict, *, readonly: bool) -> None:
    observe_server(cursor, evidence)
    if evidence["observed"].get("database") != settings.database:
        raise SchemaError("Observed selected database differs from the dedicated target")
    if readonly and evidence["observed"].get("transactionReadOnly") != 1:
        raise SchemaError("Observation must use an actual read-only transaction")
    cursor.execute("SELECT TABLE_NAME AS `name`, TABLE_TYPE AS `type`, ENGINE AS `engine` "
                   "FROM information_schema.TABLES WHERE TABLE_SCHEMA=%s ORDER BY TABLE_NAME", (settings.database,))
    objects = cursor.fetchall()
    evidence["schemaObjects"] = objects
    if {row["name"] for row in objects} != set(plan.tables) or any(row["type"] != "BASE TABLE" or row["engine"] != "InnoDB" for row in objects):
        raise SchemaError("Actual table set or engines differ from the complete repository DDL plan")
    cursor.execute("SELECT COUNT(*) AS `count` FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=%s", (settings.database,))
    evidence["triggerCount"] = cursor.fetchone()["count"]
    if evidence["triggerCount"] != 0:
        raise SchemaError("Payroll IT schema must not contain existing triggers")
    row_counts = {}
    structures = {}
    evidence["rowCounts"] = row_counts
    evidence["showCreateSha256"] = structures
    for table, columns in plan.tables.items():
        cursor.execute(f"SHOW COLUMNS FROM `{table}`")
        if {row["Field"] for row in cursor.fetchall()} != set(columns):
            raise SchemaError("Actual table columns differ from repository CREATE and ALTER statements")
        cursor.execute(f"SELECT COUNT(*) AS `count` FROM `{table}`")
        row_counts[table] = cursor.fetchone()["count"]
        if row_counts[table] != 0:
            raise SchemaError("Every payroll IT table must be empty")
        cursor.execute(f"SHOW CREATE TABLE `{table}`")
        structures[table] = sha256(cursor.fetchone()["Create Table"].encode("utf-8"))
    cursor.execute("SHOW INDEX FROM employee_salary WHERE Key_name=%s", (SCOPE_INDEX,))
    rows = sorted(cursor.fetchall(), key=lambda row: row["Seq_in_index"])
    evidence["payrollScopeIndex"] = [{key: row.get(key) for key in
        ("Key_name", "Seq_in_index", "Column_name", "Non_unique", "Sub_part", "Visible", "Collation", "Index_type")} for row in rows]
    if (len(rows) != 4 or [row.get("Column_name") for row in rows] != SCOPE_COLUMNS
            or [row.get("Seq_in_index") for row in rows] != [1, 2, 3, 4]
            or any(row.get("Key_name") != SCOPE_INDEX or row.get("Non_unique") != 1 or row.get("Sub_part") is not None
                   or row.get("Visible") != "YES" or row.get("Collation") != "A" or row.get("Index_type") != "BTREE" for row in rows)):
        raise SchemaError("Observed production payroll scope index is incompatible")
    evidence["checks"] = {"completeTableSet": True, "repositoryColumns": True, "allTablesEmpty": True,
                          "noExistingTriggers": True, "productionScopeIndex": True, "requiredVersion": True,
                          "defaultRepeatableRead": True, "lockObservationReadable": True}


def run(settings: Settings, plan: SchemaPlan, evidence: dict, *, observe: bool = False,
        connect: Callable = mysql_connect) -> None:
    # Defend this public execution boundary too: callers cannot substitute DML
    # or an incomplete/modified plan after the offline source parser ran.
    connection_settings({"FC_DB_HOST": settings.host, "FC_DB_PORT": str(settings.port),
                         "FC_DB_NAME": settings.database, "FC_DB_USER": settings.user,
                         "FC_DB_PASSWORD": settings.password})
    validate_plan(plan)
    evidence["plan"] = plan.evidence()
    connection = connect(settings)
    try:
        with connection.cursor() as cursor:
            observe_server(cursor, evidence)
            cursor.execute("SELECT SCHEMA_NAME AS `name` FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=%s", (settings.database,))
            exists = cursor.fetchone() is not None
            evidence["targetExisted"] = exists
            if observe:
                if not exists:
                    raise SchemaError("Observation requires an existing dedicated target")
                connection.select_db(settings.database)
                cursor.execute("SET SESSION TRANSACTION READ ONLY")
                connection.begin()
                try:
                    observe_schema(cursor, settings, plan, evidence, readonly=True)
                finally:
                    connection.rollback()
            else:
                if exists:
                    raise SchemaError("Target already exists; initialization refuses to reuse or reset it")
                cursor.execute(f"CREATE DATABASE `{settings.database}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
                evidence["databaseCreated"] = True
                connection.select_db(settings.database)
                evidence["ddlStatementsCompleted"] = 0
                for statement in plan.statements:
                    cursor.execute(statement)
                    evidence["ddlStatementsCompleted"] += 1
                observe_schema(cursor, settings, plan, evidence, readonly=False)
        evidence["success"] = True
        evidence["passed"] = True
    finally:
        connection.close()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True, help="Credential-free JSON observation file")
    parser.add_argument("--observe", action="store_true", help="Only inspect an existing dedicated schema in a read-only transaction")
    arguments = parser.parse_args(argv)
    evidence = {"formatVersion": 1, "operation": "observe" if arguments.observe else "fresh-create",
                "phase": "observe" if arguments.observe else "fresh",
                "requiredMysqlVersion": EXPECTED_MYSQL_VERSION, "startedAt": datetime.now(timezone.utc).isoformat(),
                "success": False, "passed": False, "databaseCreated": False}
    try:
        settings = connection_settings(dict(os.environ))
        evidence["targetDatabase"] = settings.database
        evidence["sourceInputs"] = {"sql/financial_cloud_init.sql": sha256(INIT_SQL.read_bytes()),
                                    "sql/patches/2026-10-03-payroll-write-scope-index.sql": sha256(INDEX_PATCH.read_bytes())}
        plan = build_schema_plan()
        if plan.sources != evidence["sourceInputs"]:
            raise SchemaError("Repository inputs changed while the initialization plan was constructed")
        run(settings, plan, evidence, observe=arguments.observe)
    except Exception as exception:
        # MySQL errors can contain connection strings or credentials. Never
        # emit their message, repr, traceback or SQL text.
        evidence["failure"] = {"reason": str(exception) if isinstance(exception, SchemaError)
                                else "Database operation or repository input failed",
                                "code": exception.args[0] if exception.args and isinstance(exception.args[0], int) else None}
    evidence["completedAt"] = datetime.now(timezone.utc).isoformat()
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Payroll schema {'observation' if arguments.observe else 'initialization'}: "
          f"{'passed' if evidence['success'] else 'failed'}; evidence saved")
    return 0 if evidence["success"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
