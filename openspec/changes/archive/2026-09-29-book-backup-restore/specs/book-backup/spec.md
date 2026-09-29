# Book Backup & Restore Specification

## Purpose

Guarantee bookkeeping-firm-grade data safety: any book's business data can be exported as a self-contained backup package and restored as a new book, so accidental deletion, instance failure, or client handoff never means data loss.

## Requirements

### Requirement: Export book backup package
The system SHALL allow an authorized user to export the current book's business data as a single ZIP containing a `manifest.json` and one JSONL data file per in-scope table. The backup MUST cover all book-scoped business tables enumerated in the design's table specification (voucher chain, subjects and opening balances, auxiliary records, cashier journal, fixed assets, payroll, settlement, statements, book config). Instance-level tables (users, permissions, logs, sessions, global templates) MUST NOT be included.

#### Scenario: Successful export
- **WHEN** an authorized user requests a backup export for the current book
- **THEN** the system SHALL return a ZIP whose filename identifies book and export time
- **AND** the ZIP SHALL contain `manifest.json` plus `data/<table>.jsonl` for every in-scope table
- **AND** the manifest SHALL record format version, book metadata, per-table row counts and SHA-256 checksums

#### Scenario: Export scope excludes instance data
- **WHEN** a backup package is generated
- **THEN** it MUST NOT contain user credentials, permission grants, login/session history, or instance-level configuration

#### Scenario: Schema guard for future tables
- **WHEN** the schema gains a new table containing a `book_id` column
- **THEN** the table specification test SHALL fail until the table is registered as included or explicitly excluded

### Requirement: Manifest validation on restore
The system SHALL validate an uploaded backup before writing anything: format identifier, `formatVersion` compatibility, presence of all declared tables, per-table row counts, and SHA-256 checksums. Any mismatch MUST reject the restore with a clear error and produce zero writes.

#### Scenario: Tampered manifest rejected
- **WHEN** a data file's content does not match its declared checksum or row count
- **THEN** the restore SHALL be rejected
- **AND** no new book or rows SHALL remain after the attempt

#### Scenario: Unsupported format version rejected
- **WHEN** the manifest declares an unsupported `formatVersion`
- **THEN** the restore SHALL be rejected with a version error

### Requirement: Clone-style restore with ID remapping
The system SHALL restore a validated backup as a NEW book (new `book_id`), never overwriting an existing book. All primary keys SHALL be reassigned and every foreign-key edge enumerated in the design's table specification SHALL be remapped to the new IDs within a single transaction. Personnel and department references (auditor, poster, manager, department) SHALL be nulled on restore.

#### Scenario: Round-trip fidelity
- **WHEN** a book is exported and then restored
- **THEN** the restored book SHALL contain the same voucher count and the same subject-balance monetary totals as the source book
- **AND** all voucher items, auxiliaries, journal entries, assets and salary rows SHALL reference the restored book's new IDs

#### Scenario: Failure rolls back completely
- **WHEN** any insert fails during restore
- **THEN** the whole transaction SHALL roll back including the new book shell

#### Scenario: Existing books untouched
- **WHEN** a restore completes
- **THEN** no pre-existing book's rows SHALL have been modified or deleted

### Requirement: Audit and authorization
Backup export and restore SHALL require authorization on the book and SHALL write audit events recording operator, book, table count, row count, and outcome.

#### Scenario: Unauthorized export rejected
- **WHEN** a user without permission on the book requests export or restore
- **THEN** the system SHALL reject the request

#### Scenario: Operations audited
- **WHEN** an export or restore completes
- **THEN** an audit event SHALL exist recording operator, book, and outcome
