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
The system SHALL restore a validated backup as a NEW book (new `book_id`) for the default clone restore path. All primary keys SHALL be reassigned and every foreign-key edge enumerated in the design's table specification SHALL be remapped to the new IDs within a single transaction. Personnel and department references (auditor, poster, manager, department) SHALL be nulled on restore.

#### Scenario: Round-trip fidelity
- **WHEN** a book is exported and then restored
- **THEN** the restored book SHALL contain the same voucher count and the same subject-balance monetary totals as the source book
- **AND** all voucher items, auxiliaries, journal entries, assets and salary rows SHALL reference the restored book's new IDs

#### Scenario: Failure rolls back completely
- **WHEN** any insert fails during restore
- **THEN** the whole transaction SHALL roll back including the new book shell

#### Scenario: Existing books untouched
- **WHEN** a clone restore completes
- **THEN** no pre-existing book's rows SHALL have been modified or deleted

### Requirement: Overwrite restore with confirmation and pre-backup
The system SHALL allow an authorized book administrator to restore a validated backup package into an existing target book, replacing that book's business data, only when an explicit confirmation phrase is provided. Before any destructive write, the system SHALL persist a pre-overwrite backup ZIP of the target book to the configured backup directory. The overwrite SHALL keep the target `book_id` and existing membership grants, remap all other primary keys like clone restore, run in a single transaction, and reject sealed books.

#### Scenario: Confirmation phrase required
- **WHEN** an overwrite restore is requested without the exact confirmation phrase
- **THEN** the system SHALL reject the request with zero writes

#### Scenario: Pre-backup then overwrite
- **WHEN** an authorized administrator requests overwrite restore with a valid package and confirmation phrase
- **THEN** the system SHALL first write a pre-overwrite backup ZIP of the target book to disk
- **AND** SHALL wipe and replace book-scoped business rows while keeping the target book id and memberships

#### Scenario: Sealed book rejected
- **WHEN** overwrite restore targets a sealed book
- **THEN** the overwrite restore SHALL be rejected

#### Scenario: Failure after wipe rolls back DB
- **WHEN** any insert fails during overwrite restore after wipe has begun
- **THEN** the database transaction SHALL roll back
- **AND** the pre-overwrite ZIP file on disk MAY remain for manual recovery

### Requirement: Audit and authorization
Backup export and restore SHALL require authorization on the book and SHALL write audit events recording operator, book, table count, row count, and outcome.

#### Scenario: Unauthorized export rejected
- **WHEN** a user without permission on the book requests export or restore
- **THEN** the system SHALL reject the request

#### Scenario: Operations audited
- **WHEN** an export or restore completes
- **THEN** an audit event SHALL exist recording operator, book, and outcome

### Requirement: System-initiated export for scheduled backups
The system SHALL allow a trusted server-side scheduler to export a book backup package using the same ZIP format and table scope as manual export, without requiring an interactive book-administrator session. Each such export SHALL still be audited as a system-initiated backup.

#### Scenario: Scheduler export produces restore-compatible ZIP
- **WHEN** the scheduler requests export for an enabled book
- **THEN** the system SHALL produce a ZIP accepted by the existing restore validation
- **AND** an audit event SHALL record the system-initiated export outcome
