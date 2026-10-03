"""Check the actual Java mapper SQL against session-local MySQL temporary tables.

Default execution runs only offline extraction and configuration tests. To run
the SQL scenarios explicitly use ``python tools/test_payroll_live_voucher_sql.py
--mysql`` with FC_DB_HOST, FC_DB_PORT, FC_DB_NAME, FC_DB_USER and FC_DB_PASSWORD
set. Only financial_cloud_e2e* databases are accepted. Both table names are
shadowed by TEMPORARY TABLEs before fixture INSERT/DELETE or mapper SELECT runs.
The connection is closed at completion, discarding its temporary tables.

--legacy-query and --mapper-source can evaluate a saved original mapper for a
regression counterexample; the executed SELECT is always read from its annotation.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
import os
from pathlib import Path
import re
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MAPPER = ROOT / "financial-cloud/src/main/java/com/financial/cloud/repository/hr/EmployeeSalaryMapper.java"
METHODS = {
    "accrual": "findAnyLiveAccrualVoucherId",
    "payment": "findAnyLiveSalaryVoucherId",
}
LEGACY_METHODS = {"accrual": "findAnyAccrualVoucherId", "payment": "findAnySalaryVoucherId"}
RUN_MYSQL = False
MAPPER_SOURCE = DEFAULT_MAPPER
METHOD_SELECTION = METHODS
BOOK = "sql-test-book"
EMPLOYEE = "sql-test-employee"
MONTH = "2026-09"


@dataclass(frozen=True)
class MapperQuery:
    method: str
    sql: str

    def bind(self, values: dict[str, str]) -> tuple[str, tuple[str, ...]]:
        parameters: list[str] = []
        def replace(match):
            name = match.group(1)
            if name not in values:
                raise ValueError("Mapper SQL contains an unsupported parameter")
            parameters.append(values[name])
            return "%s"
        statement = re.sub(r"#\{([A-Za-z][A-Za-z0-9]*)\}", replace, self.sql)
        if "#{" in statement:
            raise ValueError("Mapper parameter syntax is unsupported")
        return statement, tuple(parameters)


def extract_mapper_queries(source: Path = DEFAULT_MAPPER,
                           methods: dict[str, str] | None = None) -> dict[str, MapperQuery]:
    """Extract the Java text blocks; never maintain a separate SELECT copy."""
    methods = METHODS if methods is None else methods
    raw = Path(source).read_text(encoding="utf-8")
    annotations = re.findall(r'@Select\s*\(\s*"""\s*((?:(?!""").)*?)\s*"""\s*\)\s*(?:@Options\s*\([^)]*\)\s*)?String\s+(\w+)\s*\(', raw, re.DOTALL)
    queries = {}
    for kind, method in methods.items():
        matches = [sql.strip() for sql, name in annotations if name == method]
        if len(matches) != 1:
            raise ValueError("Expected exactly one @Select text block for " + method)
        sql = matches[0]
        # A single, strictly final FOR UPDATE is the only permitted locking
        # extension. Validate the remaining SELECT with the original boundary;
        # keep the tail in MapperQuery so actual MySQL executes the mapper SQL.
        select_body = re.sub(r"\s+FOR\s+UPDATE\s*$", "", sql, flags=re.IGNORECASE)
        # Only the actual peer-query salary alias and production scope index
        # are permitted. Remove that exact hint for validation, not execution;
        # unknown hints and all other function-like expressions remain rejected.
        select_body = re.sub(
            r"(\bFROM\s+employee_salary\s+es)\s+FORCE\s+INDEX\s*\(\s*idx_salary_payroll_scope\s*\)",
            r"\1", select_body, count=1, flags=re.IGNORECASE)
        # Only the two shadowed fixture tables may be read. Reject changes that
        # would access another database/table or perform a write through SELECT.
        tables = re.findall(r"\b(?:FROM|JOIN|STRAIGHT_JOIN)\s+([`\w.]+)", select_body, re.IGNORECASE)
        if (not re.match(r"^SELECT\s+(?:DISTINCT\s+)?(?:[A-Za-z_]\w*\.)?(?:accrual_voucher_id|salary_voucher_id|id)(?:\s+AS\s+[A-Za-z_]\w*)?\s+FROM\b", select_body, re.IGNORECASE)
                or ";" in select_body or "," in select_body or "/*" in select_body or "--" in select_body
                or "#" in re.sub(r"#\{[A-Za-z][A-Za-z0-9]*\}", "", select_body)
                or len(re.findall(r"\bSELECT\b", select_body, re.IGNORECASE)) != 1
                or re.search(r"\b[A-Za-z_]\w*\s*\(", select_body)
                or not tables or any(table.strip("`").lower() not in {"employee_salary", "voucher"} for table in tables)
                or re.search(r"\b(?:INTO|OUTFILE|DUMPFILE|FOR|LOCK|SKIP|NOWAIT|FORCE|USE|IGNORE|INDEX)\b", select_body, re.IGNORECASE)):
            raise ValueError("Mapper query is outside the temporary-table SELECT boundary")
        query = MapperQuery(method, sql)
        query.bind({"bookId": BOOK, "employeeId": EMPLOYEE, "belongDate": MONTH})
        queries[kind] = query
    return queries


def connection_settings(environment: dict[str, str]) -> dict:
    required = ("FC_DB_HOST", "FC_DB_PORT", "FC_DB_NAME", "FC_DB_USER", "FC_DB_PASSWORD")
    missing = [name for name in required if name not in environment]
    if missing:
        raise ValueError("Explicit database configuration is required: " + ", ".join(missing))
    database = environment["FC_DB_NAME"]
    if len(database) > 64 or re.fullmatch(r"financial_cloud_e2e[a-z0-9_]*", database) is None:
        raise ValueError("FC_DB_NAME must identify a financial_cloud_e2e* database")
    host, user = environment["FC_DB_HOST"], environment["FC_DB_USER"]
    if not host.strip() or not user.strip():
        raise ValueError("FC_DB_HOST and FC_DB_USER must be explicitly nonempty")
    port = environment["FC_DB_PORT"]
    if not port.isascii() or not port.isdecimal() or not 1 <= int(port) <= 65535:
        raise ValueError("FC_DB_PORT must be an explicit valid port")
    return {"host": host, "port": int(port), "database": database,
            "user": user, "password": environment["FC_DB_PASSWORD"], "charset": "utf8mb4",
            "connect_timeout": 10, "read_timeout": 10, "write_timeout": 10, "autocommit": True}


class OfflineSafetyTests(unittest.TestCase):
    def valid_environment(self):
        return {"FC_DB_HOST": "explicit-e2e.invalid", "FC_DB_PORT": "3307",
                "FC_DB_NAME": "financial_cloud_e2e_payroll_sql", "FC_DB_USER": "test-user",
                "FC_DB_PASSWORD": "synthetic-password"}

    def test_required_connection_fields_have_no_implicit_business_defaults(self):
        for field in self.valid_environment():
            with self.subTest(field=field):
                environment = self.valid_environment()
                del environment[field]
                with self.assertRaises(ValueError):
                    connection_settings(environment)

    def test_business_or_unsafe_database_names_are_rejected(self):
        for database in ("financial_cloud", "mysql", "FINANCIAL_cloud_e2e", "financial_cloud_e2e;DROP", "financial_cloud_e2e/other", "financial_cloud_e2e" + "x" * 64):
            with self.subTest(database=database):
                with self.assertRaises(ValueError):
                    connection_settings(dict(self.valid_environment(), FC_DB_NAME=database))

    def test_explicit_e2e_settings_are_preserved(self):
        settings = connection_settings(self.valid_environment())
        self.assertEqual(settings["database"], "financial_cloud_e2e_payroll_sql")
        self.assertEqual(settings["port"], 3307)

    def test_invalid_or_implicit_ports_are_rejected(self):
        for port in ("", "0", "65536", "3307oops", "３３０７"):
            with self.subTest(port=port):
                with self.assertRaises(ValueError):
                    connection_settings(dict(self.valid_environment(), FC_DB_PORT=port))

    def test_parameter_binding_preserves_named_order_and_duplicates(self):
        query = MapperQuery("synthetic", "SELECT #{employeeId}, #{bookId}, #{employeeId}, #{belongDate}")
        sql, values = query.bind({"bookId": BOOK, "employeeId": EMPLOYEE, "belongDate": MONTH})
        self.assertEqual(sql, "SELECT %s, %s, %s, %s")
        self.assertEqual(values, (EMPLOYEE, BOOK, EMPLOYEE, MONTH))

    def test_extracted_queries_are_the_actual_selected_java_annotations(self):
        queries = extract_mapper_queries(MAPPER_SOURCE, METHOD_SELECTION)
        source = MAPPER_SOURCE.read_text(encoding="utf-8")
        for kind, query in queries.items():
            self.assertEqual(query.method, METHOD_SELECTION[kind])
            self.assertIn(query.sql, source)

    def test_select_boundary_rejects_hidden_business_tables_and_expressions(self):
        rejected = (
            "SELECT e.accrual_voucher_id FROM employee_salary e, financial_cloud.employee_salary b",
            "SELECT e.accrual_voucher_id FROM employee_salary e JOIN/**/financial_cloud.employee_salary b ON 1=1",
            "SELECT accrual_voucher_id FROM financial_cloud.employee_salary",
            "SELECT accrual_voucher_id FROM unrelated_table",
            "SELECT accrual_voucher_id INTO OUTFILE '/tmp/exposure' FROM employee_salary",
            "SELECT accrual_voucher_id FROM employee_salary WHERE EXISTS (SELECT 1 FROM financial_cloud.employee_salary)",
            "SELECT dangerous_function() FROM employee_salary",
            "SELECT accrual_voucher_id FROM employee_salary WHERE dangerous_function() = 1",
            "SELECT accrual_voucher_id FROM employee_salary; DELETE FROM employee_salary",
            "SELECT accrual_voucher_id FROM financial_cloud.employee_salary FOR UPDATE",
            "SELECT accrual_voucher_id FROM employee_salary; DELETE FROM employee_salary FOR UPDATE",
        )
        with tempfile.TemporaryDirectory(prefix="payroll-mapper-guard-") as directory:
            source = Path(directory) / "SyntheticMapper.java"
            for sql in rejected:
                with self.subTest(sql=sql):
                    source.write_text('@Select("""\n' + sql + '\n""")\nString findAnyLiveAccrualVoucherId();', encoding="utf-8")
                    with self.assertRaises(ValueError):
                        extract_mapper_queries(source, {"accrual": METHODS["accrual"]})

    def test_select_boundary_accepts_only_a_single_final_for_update(self):
        base = "SELECT accrual_voucher_id FROM employee_salary WHERE book_id = #{bookId} LIMIT 1"
        with tempfile.TemporaryDirectory(prefix="payroll-lock-select-") as directory:
            source = Path(directory) / "SyntheticMapper.java"
            for tail in (" FOR UPDATE", "\nFOR UPDATE", " for update"):
                with self.subTest(tail=tail):
                    sql = base + tail
                    source.write_text('@Select("""\n' + sql + '\n""")\n'
                                      '@Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)\n'
                                      'String findAnyLiveAccrualVoucherId();', encoding="utf-8")
                    query = extract_mapper_queries(source, {"accrual": METHODS["accrual"]})["accrual"]
                    statement, parameters = query.bind({"bookId": BOOK})
                    self.assertTrue(re.search(r"\bFOR\s+UPDATE$", statement, re.IGNORECASE))
                    self.assertEqual(parameters, (BOOK,))
                    self.assertEqual(query.sql, sql)

    def test_select_boundary_rejects_other_or_misplaced_lock_clauses(self):
        rejected = (
            "SELECT accrual_voucher_id FROM employee_salary FOR UPDATE NOWAIT",
            "SELECT accrual_voucher_id FROM employee_salary FOR UPDATE SKIP LOCKED",
            "SELECT accrual_voucher_id FROM employee_salary FOR SHARE",
            "SELECT accrual_voucher_id FROM employee_salary LOCK IN SHARE MODE",
            "SELECT accrual_voucher_id FROM employee_salary FOR UPDATE LIMIT 1",
            "SELECT accrual_voucher_id FROM employee_salary FOR UPDATE FOR UPDATE",
            "SELECT accrual_voucher_id FROM employee_salary FOR UPDATE;",
            "SELECT accrual_voucher_id FROM employee_salary FOR/**/UPDATE",
        )
        with tempfile.TemporaryDirectory(prefix="payroll-lock-guard-") as directory:
            source = Path(directory) / "SyntheticMapper.java"
            for sql in rejected:
                with self.subTest(sql=sql):
                    source.write_text('@Select("""\n' + sql + '\n""")\nString findAnyLiveAccrualVoucherId();', encoding="utf-8")
                    with self.assertRaises(ValueError):
                        extract_mapper_queries(source, {"accrual": METHODS["accrual"]})

    def test_select_boundary_preserves_the_single_allowed_production_index_hint(self):
        sql = ("SELECT es.accrual_voucher_id FROM employee_salary es "
               "FORCE INDEX (idx_salary_payroll_scope) "
               "WHERE es.book_id = #{bookId} LIMIT 1 FOR UPDATE")
        with tempfile.TemporaryDirectory(prefix="payroll-index-select-") as directory:
            source = Path(directory) / "SyntheticMapper.java"
            source.write_text('@Select("""\n' + sql + '\n""")\nString findAnyLiveAccrualVoucherId();', encoding="utf-8")
            query = extract_mapper_queries(source, {"accrual": METHODS["accrual"]})["accrual"]
            self.assertEqual(query.sql, sql)
            statement, parameters = query.bind({"bookId": BOOK})
            self.assertIn("FORCE INDEX (idx_salary_payroll_scope)", statement)
            self.assertEqual(parameters, (BOOK,))

    def test_select_boundary_preserves_straight_join_with_the_production_scope_index(self):
        sql = ("SELECT es.accrual_voucher_id FROM employee_salary es "
               "FORCE INDEX (idx_salary_payroll_scope) STRAIGHT_JOIN voucher v "
               "ON es.accrual_voucher_id = v.id WHERE es.book_id = #{bookId} "
               "AND v.deleted = 'n' LIMIT 1 FOR UPDATE")
        with tempfile.TemporaryDirectory(prefix="payroll-straight-join-") as directory:
            source = Path(directory) / "SyntheticMapper.java"
            source.write_text('@Select("""\n' + sql + '\n""")\nString findAnyLiveAccrualVoucherId();', encoding="utf-8")
            query = extract_mapper_queries(source, {"accrual": METHODS["accrual"]})["accrual"]
            self.assertEqual(query.sql, sql)
            statement, parameters = query.bind({"bookId": BOOK})
            self.assertIn("STRAIGHT_JOIN voucher v", statement)
            self.assertIn("FORCE INDEX (idx_salary_payroll_scope)", statement)
            self.assertEqual(parameters, (BOOK,))

    def test_select_boundary_rejects_foreign_tables_for_both_join_kinds(self):
        with tempfile.TemporaryDirectory(prefix="payroll-join-scope-") as directory:
            source = Path(directory) / "SyntheticMapper.java"
            for join in ("JOIN", "STRAIGHT_JOIN", "LEFT JOIN"):
                for table in ("unrelated_table", "financial_cloud.voucher", "`financial_cloud`.`voucher`"):
                    with self.subTest(join=join, table=table):
                        sql = ("SELECT es.accrual_voucher_id FROM employee_salary es "
                               "FORCE INDEX (idx_salary_payroll_scope) " + join + " " + table + " v "
                               "ON es.accrual_voucher_id = v.id LIMIT 1 FOR UPDATE")
                        source.write_text('@Select("""\n' + sql + '\n""")\nString findAnyLiveAccrualVoucherId();', encoding="utf-8")
                        with self.assertRaises(ValueError):
                            extract_mapper_queries(source, {"accrual": METHODS["accrual"]})

    def test_select_boundary_rejects_unknown_multiple_or_wrong_table_index_hints(self):
        rejected = (
            "SELECT es.accrual_voucher_id FROM employee_salary es FORCE INDEX (unknown_index) FOR UPDATE",
            "SELECT es.accrual_voucher_id FROM employee_salary es FORCE INDEX (idx_salary_payroll_scope, unknown_index) FOR UPDATE",
            "SELECT es.accrual_voucher_id FROM employee_salary es FORCE INDEX (idx_salary_payroll_scope) FORCE INDEX (idx_salary_payroll_scope) FOR UPDATE",
            "SELECT es.accrual_voucher_id FROM employee_salary es USE INDEX (idx_salary_payroll_scope) FOR UPDATE",
            "SELECT es.accrual_voucher_id FROM employee_salary es IGNORE INDEX (idx_salary_payroll_scope) FOR UPDATE",
            "SELECT es.accrual_voucher_id FROM employee_salary es FORCE INDEX FOR JOIN (idx_salary_payroll_scope) FOR UPDATE",
            "SELECT e.accrual_voucher_id FROM employee_salary e FORCE INDEX (idx_salary_payroll_scope) FOR UPDATE",
            "SELECT accrual_voucher_id FROM voucher FORCE INDEX (idx_salary_payroll_scope) FOR UPDATE",
            "SELECT es.accrual_voucher_id FROM financial_cloud.employee_salary es FORCE INDEX (idx_salary_payroll_scope) FOR UPDATE",
            "SELECT es.accrual_voucher_id FROM employee_salary es FORCE INDEX (idx_salary_payroll_scope) WHERE dangerous_function() = 1 FOR UPDATE",
        )
        with tempfile.TemporaryDirectory(prefix="payroll-index-guard-") as directory:
            source = Path(directory) / "SyntheticMapper.java"
            for sql in rejected:
                with self.subTest(sql=sql):
                    source.write_text('@Select("""\n' + sql + '\n""")\nString findAnyLiveAccrualVoucherId();', encoding="utf-8")
                    with self.assertRaises(ValueError):
                        extract_mapper_queries(source, {"accrual": METHODS["accrual"]})


class LiveVoucherSqlTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not RUN_MYSQL:
            raise unittest.SkipTest("Actual MySQL checks require explicit --mysql")
        settings = connection_settings(dict(os.environ))
        cls.queries = extract_mapper_queries(MAPPER_SOURCE, METHOD_SELECTION)
        import pymysql
        cls.connection = pymysql.connect(**settings)
        cls.addClassCleanup(cls.connection.close)
        with cls.connection.cursor() as cursor:
            cursor.execute("SELECT DATABASE()")
            if cursor.fetchone()[0] != settings["database"]:
                raise RuntimeError("Connection did not select the explicitly allowed E2E database")
            cursor.execute("""CREATE TEMPORARY TABLE employee_salary (
                id VARCHAR(64) PRIMARY KEY, book_id VARCHAR(64), employee_id VARCHAR(64),
                belong_date VARCHAR(7), accrual_voucher_id VARCHAR(64), salary_voucher_id VARCHAR(64),
                deleted CHAR(1), created_date DATETIME(6),
                INDEX idx_salary_payroll_scope (book_id, belong_date, employee_id, created_date))""")
            cursor.execute("""CREATE TEMPORARY TABLE voucher (
                id VARCHAR(64) PRIMARY KEY, book_id VARCHAR(64), deleted CHAR(1),
                status VARCHAR(32), voucher_date DATE, voucher_year INT, voucher_month INT)""")
        # No fixture mutation runs unless both temporary shadow tables exist.

    def clear(self):
        with self.connection.cursor() as cursor:
            cursor.execute("DELETE FROM employee_salary")
            cursor.execute("DELETE FROM voucher")

    def voucher(self, identifier, *, deleted="n", book=BOOK, status="draft", date="2026-09-30"):
        with self.connection.cursor() as cursor:
            cursor.execute("INSERT INTO voucher (id,book_id,deleted,status,voucher_date,voucher_year,voucher_month) VALUES (%s,%s,%s,%s,%s,%s,%s)",
                           (identifier, book, deleted, status, date, int(date[:4]), int(date[5:7])))

    def salary(self, identifier, kind, link, *, deleted="n", book=BOOK,
               employee=EMPLOYEE, month=MONTH, created="2026-09-01 00:00:00"):
        accrual, payment = (link, None) if kind == "accrual" else (None, link)
        with self.connection.cursor() as cursor:
            cursor.execute("INSERT INTO employee_salary (id,book_id,employee_id,belong_date,accrual_voucher_id,salary_voucher_id,deleted,created_date) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)",
                           (identifier, book, employee, month, accrual, payment, deleted, created))

    def lookup(self, kind):
        sql, parameters = self.queries[kind].bind({"bookId": BOOK, "employeeId": EMPLOYEE, "belongDate": MONTH})
        with self.connection.cursor() as cursor:
            cursor.execute(sql, parameters)
            row = cursor.fetchone()
        return row[0] if row else None

    def test_newest_stale_peer_cannot_hide_older_live_peer(self):
        for kind in METHODS:
            with self.subTest(kind=kind):
                self.clear()
                self.voucher("old-live")
                self.voucher("new-deleted", deleted="y")
                self.salary("old-row", kind, "old-live", created="2026-09-01 00:00:00")
                self.salary("new-missing-row", kind, "missing-voucher", created="2026-09-02 00:00:00")
                self.salary("new-deleted-row", kind, "new-deleted", created="2026-09-03 00:00:00")
                self.assertEqual(self.lookup(kind), "old-live")

    def test_historical_soft_deleted_salary_still_blocks_when_voucher_is_live(self):
        for kind in METHODS:
            with self.subTest(kind=kind):
                self.clear()
                self.voucher("live-history")
                self.salary("deleted-history", kind, "live-history", deleted="y")
                self.assertEqual(self.lookup(kind), "live-history")

    def test_deleted_voucher_is_not_live(self):
        for kind in METHODS:
            with self.subTest(kind=kind):
                self.clear()
                self.voucher("deleted-voucher", deleted="y")
                self.salary("row", kind, "deleted-voucher")
                self.assertIsNone(self.lookup(kind))

    def test_missing_null_and_empty_references_are_not_live(self):
        for kind in METHODS:
            with self.subTest(kind=kind):
                self.clear()
                for index, link in enumerate(("missing-voucher", None, "")):
                    self.salary("row-" + str(index), kind, link)
                self.assertIsNone(self.lookup(kind))

    def test_book_employee_and_salary_month_scope(self):
        for kind in METHODS:
            with self.subTest(kind=kind):
                self.clear()
                self.voucher("other-book", book="other-book")
                self.voucher("other-employee")
                self.voucher("other-month")
                self.salary("book-row", kind, "other-book", book="other-book")
                self.salary("employee-row", kind, "other-employee", employee="other-employee")
                self.salary("month-row", kind, "other-month", month="2026-08")
                self.assertIsNone(self.lookup(kind))
                self.voucher("matching-live")
                self.salary("matching-row", kind, "matching-live", created="2026-08-01 00:00:00")
                self.assertEqual(self.lookup(kind), "matching-live")

    def test_existing_foreign_book_voucher_id_remains_live(self):
        for kind in METHODS:
            with self.subTest(kind=kind):
                self.clear()
                self.voucher("foreign-book-voucher", book="other-book")
                self.salary("current-row", kind, "foreign-book-voucher")
                # Preserve historical FK liveness by ID. This change filters
                # the salary scope and voucher deletion, not voucher.book_id.
                self.assertEqual(self.lookup(kind), "foreign-book-voucher")

    def test_other_voucher_type_does_not_block_selected_type(self):
        for kind in METHODS:
            with self.subTest(kind=kind):
                self.clear()
                other_kind = "payment" if kind == "accrual" else "accrual"
                self.voucher("other-type-live")
                self.salary("row", other_kind, "other-type-live")
                self.assertIsNone(self.lookup(kind))
                self.assertEqual(self.lookup(other_kind), "other-type-live")

    def test_voucher_status_and_date_do_not_narrow_live_dedupe(self):
        for kind in METHODS:
            for status in ("draft", "reviewing", "completed", "rejected", "cancelled", None):
                with self.subTest(kind=kind, status=status):
                    self.clear()
                    self.voucher("live-old-date", status=status, date="2025-01-31")
                    self.salary("historical-row", kind, "live-old-date", deleted="y")
                    self.assertEqual(self.lookup(kind), "live-old-date")


def main(argv=None) -> int:
    global RUN_MYSQL, MAPPER_SOURCE, METHOD_SELECTION
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql", action="store_true", help="Explicitly execute temporary-table MySQL scenarios")
    parser.add_argument("--legacy-query", action="store_true", help="Extract original pre-fix method names for counterexamples")
    parser.add_argument("--mapper-source", type=Path, default=DEFAULT_MAPPER, help="Java mapper source or saved original source")
    args = parser.parse_args(argv)
    RUN_MYSQL = args.mysql
    MAPPER_SOURCE = args.mapper_source
    METHOD_SELECTION = LEGACY_METHODS if args.legacy_query else METHODS
    if RUN_MYSQL:
        try:
            connection_settings(dict(os.environ))
            extract_mapper_queries(MAPPER_SOURCE, METHOD_SELECTION)
        except (ValueError, OSError) as error:
            print("Refusing MySQL checks: " + str(error), file=sys.stderr)
            return 2
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(OfflineSafetyTests)
    if RUN_MYSQL:
        suite.addTests(unittest.defaultTestLoader.loadTestsFromTestCase(LiveVoucherSqlTests))
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(main())
