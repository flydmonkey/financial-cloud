"""Offline source/file/mock tests; never import a driver or connect to MySQL."""
from dataclasses import replace
import io
import json
from pathlib import Path
import re
import tempfile
import unittest
from unittest.mock import Mock, patch

import init_payroll_it_schema as schema


ENVIRONMENT = {"FC_DB_HOST": "127.0.0.1", "FC_DB_PORT": "13317",
               "FC_DB_NAME": "financial_cloud_e2e_20261003_it4_unit_fixture",
               "FC_DB_USER": "fixture_user", "FC_DB_PASSWORD": "do-not-emit-fixture-secret"}


class SchemaPlanSafetyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = schema.INIT_SQL.read_text(encoding="utf-8")
        cls.patch_source = schema.INDEX_PATCH.read_text(encoding="utf-8")
        cls.plan = schema.build_schema_plan()

    def before_seed(self, statement):
        return self.source.replace("-- Seed data", statement + "\n-- Seed data", 1)

    def test_current_complete_production_plan_has_no_reset_or_seeds(self):
        self.assertEqual(78, len(self.plan.tables))
        self.assertEqual(87, len(self.plan.statements))
        self.assertEqual(78, sum(statement.startswith("CREATE TABLE ") for statement in self.plan.statements))
        self.assertEqual(9, sum(statement.startswith("ALTER TABLE ") for statement in self.plan.statements))
        self.assertTrue(all(statement.startswith(("CREATE TABLE `", "ALTER TABLE `")) for statement in self.plan.statements))
        self.assertEqual(3, len(self.plan.redundant_alters))
        self.assertEqual({"dispose_voucher_id", "purchase_voucher_id", "suspended_period"},
                         {item["column"] for item in self.plan.redundant_alters})
        self.assertTrue(all(len(item["sourceSqlSha256"]) == 64 and "sourceSql" in item for item in self.plan.redundant_alters))
        schema.validate_plan(self.plan)

    def test_real_post_create_columns_and_index_are_retained(self):
        self.assertIn("source_voucher_id", self.plan.tables["voucher"])
        self.assertIn("prev_opening_balance", self.plan.tables["journal_account"])
        self.assertIn("status", self.plan.tables["journal_account"])
        self.assertIn("reconciled", self.plan.tables["journal_entry"])
        self.assertIn("ip", self.plan.tables["history_system_logs"])
        self.assertEqual("ALTER TABLE `employee_salary` ADD INDEX `idx_salary_payroll_scope` "
                         "(`book_id`, `belong_date`, `employee_id`, `created_date`)", self.plan.statements[-1])

    def test_original_sql_and_section_indices_are_recorded(self):
        self.assertEqual(len(self.plan.statements), len(self.plan.selections))
        self.assertTrue(all(item["section"] == "schema" and isinstance(item["statementIndex"], int)
                            and item["sourceSql"] for item in self.plan.selections))
        self.assertEqual(4, self.plan.selections[-1]["sourceStatementCount"])
        self.assertEqual(schema.sha256(schema.INIT_SQL.read_bytes()), self.plan.sources["sql/financial_cloud_init.sql"])

    def test_seed_write_before_seed_marker_never_enters_plan(self):
        plan = schema.build_schema_plan(self.before_seed("INSERT INTO `employee_salary` (id) VALUES ('unwanted');"))
        self.assertEqual(self.plan.statements, plan.statements)
        self.assertNotEqual(self.plan.sources, plan.sources)
        self.assertEqual(self.plan.sha256, plan.sha256)

    def test_repository_can_add_a_supported_empty_table_without_fixed_count(self):
        extra = "CREATE TABLE `future_empty_table` (`id` varchar(45) NOT NULL, PRIMARY KEY (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
        plan = schema.build_schema_plan(self.before_seed(extra))
        self.assertEqual(79, len(plan.tables))
        self.assertEqual(("id",), plan.tables["future_empty_table"])

    def test_sql_split_preserves_quoted_semicolons_and_escapes(self):
        source = "-- comment ;\nSELECT 'a;it''s', 'escaped\\\';value'; /* ; */ SELECT `semi`; # ;\n SELECT 'done';"
        statements = schema.split_sql(source)
        self.assertEqual(3, len(statements))
        self.assertIn("'a;it''s'", statements[0])
        self.assertIn("'escaped\\\';value'", statements[0])
        self.assertEqual("SELECT 'done'", statements[-1])

    def test_unterminated_quotes_comments_and_escapes_fail(self):
        for source in ("SELECT 'unfinished", "SELECT 'escape\\", "/* incomplete", "/*!SET NAMES utf8mb4 */;"):
            with self.subTest(source=source), self.assertRaises(schema.SchemaError):
                schema.split_sql(source)

    def test_executable_version_comment_cannot_hide_dangerous_ddl(self):
        for statement in ("/*!99999 DROP DATABASE `financial_cloud` */;",
                          "/*!99999 ALTER TABLE `book` DROP COLUMN `id` */;",
                          "/*!99999 PREPARE hidden FROM 'DROP DATABASE financial_cloud' */;"):
            with self.subTest(statement=statement), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(self.before_seed(statement))

    def test_structural_change_after_seed_marker_is_not_silently_omitted(self):
        for statement in ("ALTER TABLE `book` ADD COLUMN `leaked` varchar(45);", "CREATE VIEW dangerous AS SELECT * FROM book;"):
            with self.subTest(statement=statement), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(self.source + "\n" + statement)

    def test_cross_database_create_or_alter_is_rejected(self):
        statements = ("CREATE TABLE `other`.`book` (`id` int) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
                      "ALTER TABLE `other`.`book` ADD COLUMN `extra` int;")
        for statement in statements:
            with self.subTest(statement=statement), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(self.before_seed(statement))

    def test_foreign_database_header_is_rejected(self):
        with self.assertRaises(schema.SchemaError):
            schema.build_schema_plan(self.source.replace("USE `financial_cloud`;", "USE `financial_cloud_live`;", 1))

    def test_create_as_select_is_rejected(self):
        with self.assertRaises(schema.SchemaError):
            schema.build_schema_plan(self.before_seed("CREATE TABLE `leaked` (`id` int) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AS SELECT 1;"))

    def test_unknown_structure_and_destructive_operations_fail(self):
        for statement in ("TRUNCATE TABLE `book`;", "DROP DATABASE `financial_cloud`;",
                          "DROP TABLE IF EXISTS `outside_plan`;", "RENAME TABLE `book` TO `book_old`;",
                          "ALTER TABLE `book` DROP COLUMN `name`;", "CREATE USER 'unsafe'@'%';",
                          "ALTER TABLE `book` ADD COLUMN `generated` int AS (1);",
                          "CREATE TABLE `unsafe` (`id` int, FOREIGN KEY (`id`) REFERENCES other(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"):
            with self.subTest(statement=statement), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(self.before_seed(statement))

    def test_column_functions_and_cross_schema_default_fail_offline(self):
        for expression in ("UUID()", "LOAD_FILE('/private')", "other_function()", "(SELECT 1)"):
            with self.subTest(expression=expression), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(self.before_seed(f"ALTER TABLE `book` ADD COLUMN `unsafe` varchar(45) DEFAULT {expression};"))

    def test_duplicate_index_or_conflicting_column_attributes_fail_before_creation(self):
        for statement in ("ALTER TABLE `book` ADD KEY `PRIMARY` (`id`);",
                          "ALTER TABLE `book` ADD COLUMN `invalid` int NOT NULL NULL;",
                          "ALTER TABLE `book` ADD COLUMN `invalid` int DEFAULT - 'text';",
                          "ALTER TABLE `employee_salary` ADD INDEX `idx_salary_payroll_scope` (`book_id`, `belong_date`, `employee_id`, `created_date`);"):
            with self.subTest(statement=statement), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(self.before_seed(statement))

    def test_dynamic_statements_are_not_generally_allowed(self):
        for statement in ("SET @unapproved = CONCAT('DROP ', 'DATABASE financial_cloud');",
                          "PREPARE injected FROM 'SELECT 1';", "EXECUTE injected;", "DEALLOCATE PREPARE injected;"):
            with self.subTest(statement=statement), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(self.before_seed(statement))

    def test_tampered_patch_or_init_patch_copy_fails(self):
        for changed in (self.patch_source.replace("'SELECT 1'", "'SELECT 2'"),
                        self.patch_source.replace("NON_UNIQUE = 1", "NON_UNIQUE = 0"),
                        self.patch_source + "\nDROP DATABASE `financial_cloud`;",
                        self.patch_source.replace("`created_date`", "`id`")):
            with self.subTest(changed=changed[-40:]), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(patch_source=changed)
        with self.assertRaises(schema.SchemaError):
            schema.build_schema_plan(self.source.replace("EXECUTE payroll_scope_index_stmt;", "EXECUTE changed;", 1))

    def test_patch_ordinary_comment_and_line_ending_changes_preserve_plan(self):
        changed = "-- additional review comment\n" + self.patch_source.replace("\n", "\r\n")
        plan = schema.build_schema_plan(patch_source=changed)
        self.assertEqual(self.plan.statements, plan.statements)
        self.assertNotEqual(self.plan.sources, plan.sources)

    def test_fixed_asset_duplicate_type_or_default_conflict_fails(self):
        for original, changed in (("ADD COLUMN `dispose_voucher_id` varchar(45)", "ADD COLUMN `dispose_voucher_id` varchar(64)"),
                                  ("ADD COLUMN `suspended_period` varchar(7) COLLATE utf8mb4_bin DEFAULT NULL",
                                   "ADD COLUMN `suspended_period` varchar(7) COLLATE utf8mb4_bin DEFAULT 'wrong'")):
            with self.subTest(changed=changed), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(self.source.replace(original, changed, 1))

    def test_markers_duplicate_or_missing_fail(self):
        for changed in (self.source.replace("-- Seed data", "-- renamed section", 1),
                        self.source + "\n-- Schema\n", self.source + "\n-- Seed data\n"):
            with self.subTest(changed=changed[-30:]), self.assertRaises(schema.SchemaError):
                schema.build_schema_plan(changed)

    def test_plan_metadata_hash_or_execution_statement_tampering_fail(self):
        for candidate in (replace(self.plan, sha256="0" * 64), replace(self.plan, tables={}),
                          replace(self.plan, statements=self.plan.statements + ("INSERT INTO book (id) VALUES ('x')",)),
                          replace(self.plan, statements=self.plan.statements + ("DROP DATABASE financial_cloud",)),
                          replace(self.plan, statements=self.plan.statements + ("ALTER TABLE other.book ADD COLUMN x int",))):
            with self.subTest(candidate=candidate.sha256), self.assertRaises(schema.SchemaError):
                schema.validate_plan(candidate)


class SettingsSafetyTests(unittest.TestCase):
    def test_explicit_settings_and_redacted_repr(self):
        settings = schema.connection_settings(ENVIRONMENT)
        self.assertEqual(13317, settings.port)
        self.assertNotIn(ENVIRONMENT["FC_DB_PASSWORD"], repr(settings))
        self.assertNotIn(ENVIRONMENT["FC_DB_USER"], repr(settings))

    def test_every_missing_or_blank_setting_fails_without_defaults(self):
        for name in ENVIRONMENT:
            for missing in (True, False):
                environment = dict(ENVIRONMENT)
                environment.pop(name) if missing else environment.update({name: ""})
                with self.subTest(name=name, missing=missing), self.assertRaises(schema.SchemaError):
                    schema.connection_settings(environment)

    def test_namespace_identifier_and_length_boundaries(self):
        valid = "financial_cloud_e2e_20261003_it4_" + "a" * (64 - len("financial_cloud_e2e_20261003_it4_"))
        self.assertEqual(64, len(schema.connection_settings({**ENVIRONMENT, "FC_DB_NAME": valid}).database))
        for database in (valid + "a", "financial_cloud", "financial_cloud_e2e_ci", "financial_cloud_e2e_20261003_it4_A",
                         "financial_cloud_e2e_20261003_it4_x`;DROP_DATABASE_x", "financial_cloud_e2e_20261003_it4_"):
            with self.subTest(database=database), self.assertRaises(schema.SchemaError):
                schema.connection_settings({**ENVIRONMENT, "FC_DB_NAME": database})

    def test_invalid_host_or_port_cannot_enter_connection(self):
        for port in ("0", "65536", " 3306", "3.3", "-1", "port", "３３０６"):
            with self.subTest(port=port), self.assertRaises(schema.SchemaError):
                schema.connection_settings({**ENVIRONMENT, "FC_DB_PORT": port})
        for host in ("localhost/database", "127.0.0.1?x=1", "localhost;drop", " "):
            with self.subTest(host=host), self.assertRaises(schema.SchemaError):
                schema.connection_settings({**ENVIRONMENT, "FC_DB_HOST": host})


class MysqlFixtureError(Exception):
    pass


class FakeConnection:
    """Record commands and supply only deterministic metadata, never a driver."""
    def __init__(self, plan, *, exists=False):
        self.plan = plan
        self.exists = exists
        self.database = None
        self.version = "9.4.0"
        self.isolation = "REPEATABLE-READ"
        self.global_isolation = "REPEATABLE-READ"
        self.performance_schema = 1
        self.readonly = 0
        self.commands = []
        self.closed = False
        self.rollbacks = 0
        self.begins = 0
        self.ddl_count = 0
        self.fail_create = False
        self.fail_ddl_at = None
        self.deny_lock_table = None
        self.extra_table = False
        self.column_drift = None
        self.nonempty_table = None
        self.trigger_count = 0
        self.index = [{"Key_name": schema.SCOPE_INDEX, "Seq_in_index": index + 1, "Column_name": name,
                       "Non_unique": 1, "Sub_part": None, "Visible": "YES", "Collation": "A", "Index_type": "BTREE"}
                      for index, name in enumerate(schema.SCOPE_COLUMNS)]
        self.response = []

    def cursor(self):
        return self

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def execute(self, command, parameters=None):
        self.commands.append((command, parameters))
        if command.startswith("SELECT VERSION()"):
            self.response = [{"version": self.version, "versionComment": "Offline metadata fixture",
                              "globalIsolation": self.global_isolation, "sessionIsolation": self.isolation,
                              "performanceSchema": self.performance_schema, "transactionReadOnly": self.readonly,
                              "database": self.database}]
        elif "FROM performance_schema." in command:
            table = command.split("performance_schema.")[1]
            if table == self.deny_lock_table:
                raise MysqlFixtureError(1142, "Permission denied containing " + ENVIRONMENT["FC_DB_PASSWORD"])
            self.response = [{"count": 0}]
        elif "FROM information_schema.SCHEMATA" in command:
            self.response = [{"name": parameters[0]}] if self.exists else []
        elif command.startswith("CREATE DATABASE"):
            if self.fail_create:
                raise MysqlFixtureError(1007, "Database exists, private " + ENVIRONMENT["FC_DB_PASSWORD"])
            self.exists = True
            self.response = []
        elif command == "SET SESSION TRANSACTION READ ONLY":
            self.readonly = 1
        elif command.startswith(("CREATE TABLE ", "ALTER TABLE ")):
            self.ddl_count += 1
            if self.ddl_count == self.fail_ddl_at:
                raise MysqlFixtureError(1060, "Actual DDL error, private " + ENVIRONMENT["FC_DB_PASSWORD"])
        elif "FROM information_schema.TABLES" in command:
            self.response = [{"name": name, "type": "BASE TABLE", "engine": "InnoDB"} for name in self.plan.tables]
            if self.extra_table:
                self.response.append({"name": "unowned_table", "type": "BASE TABLE", "engine": "InnoDB"})
        elif "FROM information_schema.TRIGGERS" in command:
            self.response = [{"count": self.trigger_count}]
        elif command.startswith("SHOW COLUMNS"):
            table = command.split("`")[1]
            columns = self.plan.tables[table]
            self.response = [{"Field": column} for column in columns if not (table == self.column_drift and column == "source_voucher_id")]
        elif command.startswith("SELECT COUNT(*) AS `count` FROM `"):
            table = command.split("`")[3]
            self.response = [{"count": 1 if table == self.nonempty_table else 0}]
        elif command.startswith("SHOW CREATE TABLE"):
            self.response = [{"Create Table": "CREATE TABLE " + command.split("`")[1] + " metadata fixture"}]
        elif command.startswith("SHOW INDEX"):
            self.response = self.index
        else:
            raise AssertionError("Unexpected command in offline fixture: " + command)

    def fetchone(self):
        return self.response[0] if self.response else None

    def fetchall(self):
        return self.response

    def select_db(self, database):
        self.database = database

    def begin(self):
        self.begins += 1

    def rollback(self):
        self.rollbacks += 1

    def close(self):
        self.closed = True


class InitializationAndObservationSafetyTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.plan = schema.build_schema_plan()
        cls.settings = schema.connection_settings(ENVIRONMENT)

    def perform(self, connection, *, observe=False, plan=None):
        evidence = {"success": False, "passed": False, "databaseCreated": False}
        schema.run(self.settings, plan or self.plan, evidence, observe=observe, connect=lambda _: connection)
        return evidence

    def mutations(self, connection):
        return [sql for sql, _ in connection.commands if sql.startswith(("CREATE ", "ALTER ", "INSERT ", "DELETE ", "DROP ", "TRUNCATE ", "UPDATE "))]

    def test_fresh_create_executes_only_the_prechecked_plan_and_observes_all_tables(self):
        connection = FakeConnection(self.plan)
        evidence = self.perform(connection)
        self.assertTrue(evidence["passed"] and evidence["success"] and evidence["databaseCreated"])
        self.assertEqual(87, evidence["ddlStatementsCompleted"])
        self.assertEqual(78, len(evidence["rowCounts"]))
        self.assertEqual({0}, set(evidence["rowCounts"].values()))
        self.assertEqual(78, len(evidence["showCreateSha256"]))
        mutations = self.mutations(connection)
        self.assertTrue(mutations[0].startswith("CREATE DATABASE `"))
        self.assertNotIn("IF NOT EXISTS", mutations[0])
        self.assertEqual(list(self.plan.statements), mutations[1:])
        self.assertTrue(connection.closed)

    def test_metadata_queries_quote_aliases_including_reserved_database_name(self):
        connection = FakeConnection(self.plan)
        self.perform(connection)
        queries = [sql for sql, _ in connection.commands if sql.startswith("SELECT ")]
        self.assertTrue(any("DATABASE() AS `database`" in sql for sql in queries))
        aliases = [alias for sql in queries for alias in re.findall(r"\bAS\s+([^\s,]+)", sql)]
        self.assertTrue(aliases)
        self.assertTrue(all(re.fullmatch(r"`[A-Za-z_][A-Za-z0-9_]*`", alias) for alias in aliases))

    def test_existing_target_is_rejected_before_any_mutation(self):
        connection = FakeConnection(self.plan, exists=True)
        with self.assertRaisesRegex(schema.SchemaError, "Target already exists"):
            self.perform(connection)
        self.assertEqual([], self.mutations(connection))
        self.assertTrue(connection.closed)

    def test_create_race_failure_is_not_ignored_or_followed_by_schema_writes(self):
        connection = FakeConnection(self.plan)
        connection.fail_create = True
        with self.assertRaises(MysqlFixtureError):
            self.perform(connection)
        self.assertEqual(1, len(self.mutations(connection)))
        self.assertEqual(0, connection.ddl_count)
        self.assertTrue(connection.closed)

    def test_partial_initialization_retains_failure_without_drop_or_retry(self):
        connection = FakeConnection(self.plan)
        connection.fail_ddl_at = 2
        evidence = {"success": False, "passed": False}
        with self.assertRaises(MysqlFixtureError):
            schema.run(self.settings, self.plan, evidence, connect=lambda _: connection)
        self.assertTrue(evidence["databaseCreated"])
        self.assertEqual(1, evidence["ddlStatementsCompleted"])
        self.assertFalse(evidence["success"])
        self.assertFalse(any(sql.startswith(("DROP ", "DELETE ", "TRUNCATE ", "INSERT ")) for sql, _ in connection.commands))

    def test_unsafe_plan_or_target_is_rejected_before_connect(self):
        connect = Mock()
        with self.assertRaises(schema.SchemaError):
            schema.run(self.settings, replace(self.plan, statements=("DROP DATABASE financial_cloud",)), {}, connect=connect)
        with self.assertRaises(schema.SchemaError):
            schema.run(replace(self.settings, database="financial_cloud"), self.plan, {}, connect=connect)
        connect.assert_not_called()

    def test_version_mismatch_keeps_observed_version_and_performs_no_mutation(self):
        connection = FakeConnection(self.plan)
        connection.version = "9.7.0"
        evidence = {}
        with self.assertRaisesRegex(schema.SchemaError, "version"):
            schema.run(self.settings, self.plan, evidence, connect=lambda _: connection)
        self.assertEqual("9.7.0", evidence["observed"]["version"])
        self.assertEqual("REPEATABLE-READ", evidence["observed"]["sessionIsolation"])
        self.assertEqual([], self.mutations(connection))

    def test_official_version_suffix_is_observed_without_relabelling(self):
        connection = FakeConnection(self.plan)
        connection.version = "9.4.0-commercial"
        evidence = self.perform(connection)
        self.assertEqual("9.4.0-commercial", evidence["observed"]["version"])

    def test_wrong_session_or_global_default_isolation_fails_before_creation(self):
        for field in ("isolation", "global_isolation"):
            connection = FakeConnection(self.plan)
            setattr(connection, field, "READ-COMMITTED")
            with self.subTest(field=field), self.assertRaisesRegex(schema.SchemaError, "isolation"):
                self.perform(connection)
            self.assertEqual([], self.mutations(connection))

    def test_disabled_performance_schema_or_permission_denial_fails_before_creation(self):
        connection = FakeConnection(self.plan)
        connection.performance_schema = 0
        with self.assertRaisesRegex(schema.SchemaError, "performance_schema"):
            self.perform(connection)
        self.assertEqual([], self.mutations(connection))
        for table in ("data_lock_waits", "data_locks", "threads"):
            connection = FakeConnection(self.plan)
            connection.deny_lock_table = table
            evidence = {}
            with self.subTest(table=table), self.assertRaises(MysqlFixtureError):
                schema.run(self.settings, self.plan, evidence, connect=lambda _: connection)
            self.assertFalse(evidence["lockObservation"][table]["readable"])
            self.assertEqual([], self.mutations(connection))

    def test_actual_nonempty_table_or_trigger_or_column_drift_fails(self):
        for field, value in (("nonempty_table", "config"), ("trigger_count", 1), ("column_drift", "voucher"), ("extra_table", True)):
            connection = FakeConnection(self.plan, exists=True)
            setattr(connection, field, value)
            with self.subTest(field=field), self.assertRaises(schema.SchemaError):
                self.perform(connection, observe=True)
            self.assertEqual([], self.mutations(connection))
            self.assertEqual(1, connection.rollbacks)

    def test_every_required_scope_index_dimension_is_checked(self):
        changes = ({"Non_unique": 0}, {"Sub_part": 10}, {"Visible": "NO"}, {"Collation": "D"},
                   {"Index_type": "HASH"}, {"Column_name": "id"}, {"Seq_in_index": 5}, {"Key_name": "other"})
        for changed in changes:
            connection = FakeConnection(self.plan, exists=True)
            connection.index[0].update(changed)
            with self.subTest(changed=changed), self.assertRaisesRegex(schema.SchemaError, "index"):
                self.perform(connection, observe=True)
        connection = FakeConnection(self.plan, exists=True)
        connection.index.pop()
        with self.assertRaisesRegex(schema.SchemaError, "index"):
            self.perform(connection, observe=True)

    def test_observe_only_uses_readonly_transaction_and_never_ddl_or_dml(self):
        connection = FakeConnection(self.plan, exists=True)
        evidence = self.perform(connection, observe=True)
        self.assertEqual([], self.mutations(connection))
        self.assertEqual(1, connection.begins)
        self.assertEqual(1, connection.rollbacks)
        self.assertEqual(1, evidence["observed"]["transactionReadOnly"])
        self.assertFalse(evidence["databaseCreated"])

    def test_observe_missing_target_fails_without_creating_it(self):
        connection = FakeConnection(self.plan)
        with self.assertRaisesRegex(schema.SchemaError, "existing dedicated target"):
            self.perform(connection, observe=True)
        self.assertEqual([], self.mutations(connection))

    def test_main_saves_secret_free_failure_and_actual_version_observation(self):
        for version, secret_error in (("9.7.0", False), ("9.4.0", True)):
            connection = FakeConnection(self.plan)
            connection.version = version
            connection.fail_create = secret_error
            with tempfile.TemporaryDirectory() as directory, patch.dict(schema.os.environ, ENVIRONMENT, clear=True), \
                    patch.object(schema, "mysql_connect", return_value=connection), patch("sys.stdout", new_callable=io.StringIO) as output:
                # run's default connector was bound on definition; explicitly
                # inject the file/mock connection without importing pymysql.
                original = schema.run
                def inject(settings, plan, evidence, **kwargs):
                    return original(settings, plan, evidence, connect=lambda _: connection, **kwargs)
                path = Path(directory) / "failure.json"
                with patch.object(schema, "run", side_effect=inject):
                    status = schema.main(["--output", str(path)])
                saved = path.read_text(encoding="utf-8")
                evidence = json.loads(saved)
                self.assertEqual(1, status)
                self.assertFalse(evidence["passed"])
                self.assertEqual(version, evidence["observed"]["version"])
                self.assertIn("sourceInputs", evidence)
                self.assertNotIn(ENVIRONMENT["FC_DB_PASSWORD"], saved + output.getvalue())
                self.assertNotIn(ENVIRONMENT["FC_DB_USER"], saved + output.getvalue())
                self.assertEqual(1007 if secret_error else None, evidence["failure"]["code"])

    def test_main_rejects_source_before_any_connection(self):
        with tempfile.TemporaryDirectory() as directory, patch.dict(schema.os.environ, ENVIRONMENT, clear=True), \
                patch.object(schema, "build_schema_plan", side_effect=schema.SchemaError("Unrecognized statement")), \
                patch.object(schema, "mysql_connect") as connect, patch("sys.stdout", new_callable=io.StringIO):
            path = Path(directory) / "failure.json"
            self.assertEqual(1, schema.main(["--output", str(path)]))
            connect.assert_not_called()
            self.assertFalse(json.loads(path.read_text(encoding="utf-8"))["databaseCreated"])

    def test_source_input_drift_during_plan_construction_fails_before_connection(self):
        changed = replace(self.plan, sources={**self.plan.sources, "sql/financial_cloud_init.sql": "0" * 64})
        with tempfile.TemporaryDirectory() as directory, patch.dict(schema.os.environ, ENVIRONMENT, clear=True), \
                patch.object(schema, "build_schema_plan", return_value=changed), \
                patch.object(schema, "run") as run, patch("sys.stdout", new_callable=io.StringIO):
            path = Path(directory) / "failure.json"
            self.assertEqual(1, schema.main(["--output", str(path)]))
            run.assert_not_called()
            evidence = json.loads(path.read_text(encoding="utf-8"))
            self.assertIn("inputs changed", evidence["failure"]["reason"])
            self.assertFalse(evidence["databaseCreated"])

    def test_main_successful_observe_file_has_phase_and_passed(self):
        connection = FakeConnection(self.plan, exists=True)
        with tempfile.TemporaryDirectory() as directory, patch.dict(schema.os.environ, ENVIRONMENT, clear=True), \
                patch("sys.stdout", new_callable=io.StringIO):
            original = schema.run
            def inject(settings, plan, evidence, **kwargs):
                return original(settings, plan, evidence, connect=lambda _: connection, **kwargs)
            path = Path(directory) / "observation.json"
            with patch.object(schema, "run", side_effect=inject):
                self.assertEqual(0, schema.main(["--observe", "--output", str(path)]))
            evidence = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual("observe", evidence["phase"])
            self.assertTrue(evidence["passed"])
            self.assertFalse(evidence["databaseCreated"])
            self.assertEqual([], self.mutations(connection))


if __name__ == "__main__":
    unittest.main()
