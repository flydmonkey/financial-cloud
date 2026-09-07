## Purpose

Enable accountants to download a shared Excel template, export all vouchers matching list filters, and re-import that file (or filled templates) as new draft vouchers with optional word-number reuse.

## ADDED Requirements

### Requirement: Unified Excel template for import and export
The system SHALL provide a single Excel (xlsx) column layout used for both the downloadable import template and voucher export files, such that an exported file can be submitted to import without reformatting columns. The system SHALL expose a download-template action that returns an empty (or header-only) workbook matching that layout.

#### Scenario: Download import template
- **WHEN** an authorized user requests the voucher import template
- **THEN** the system SHALL return an xlsx file whose columns match the export layout

#### Scenario: Exported file is import-compatible
- **WHEN** a user exports vouchers using the shared layout
- **AND** later uploads that file to import without changing column headers
- **THEN** the system SHALL accept the file for import parsing (subject to per-voucher validation)

### Requirement: Filter-aware full export
When the user exports from the voucher list, the system SHALL export all vouchers in the current book that match the list’s active filter criteria. The export MUST NOT be limited to the current page of pagination results. Exported content SHALL include voucher header fields and journal entry lines in the shared template layout.

#### Scenario: Export ignores page size
- **WHEN** the list filters match more vouchers than one page
- **AND** the user triggers export with those filters
- **THEN** the downloaded workbook SHALL include all matching vouchers, not only the current page

#### Scenario: Export respects period filter
- **WHEN** the user has set a voucher period (or other supported list filters)
- **AND** the user triggers export
- **THEN** the workbook SHALL include only vouchers matching those filters

### Requirement: Import creates draft vouchers only
Import SHALL create new vouchers only. Any identifier column in the file MUST be ignored for update purposes. Every successfully imported voucher MUST be stored with status draft (暂存). Import MUST NOT set review, audit, manager, or posting (sender) fields, and MUST NOT post to subject balances.

#### Scenario: Successful import is draft
- **WHEN** a valid voucher block in the Excel file passes validation
- **THEN** the system SHALL create a new voucher in draft status
- **AND** SHALL NOT mark it reviewed or posted

#### Scenario: No update by id
- **WHEN** the Excel file contains a voucher id that already exists in the book
- **AND** the row is otherwise valid
- **THEN** the system SHALL create a new voucher rather than updating the existing one

### Requirement: Import validation for balance, subject, and open period
For each voucher group in the import file, the system SHALL require: debit total equals credit total; every line’s subject code exists in the current book; and the voucher date falls in an open voucher period for that book. Failed voucher groups MUST NOT be persisted; other valid groups in the same file MAY still be imported.

#### Scenario: Unbalanced voucher rejected
- **WHEN** a voucher group’s debit total does not equal its credit total
- **THEN** the system SHALL reject that voucher group
- **AND** SHALL NOT create a voucher for it

#### Scenario: Unknown subject code rejected
- **WHEN** a journal line references a subject code not present in the current book
- **THEN** the system SHALL reject that voucher group

#### Scenario: Closed period rejected
- **WHEN** a voucher date is outside the book’s open voucher period rules
- **THEN** the system SHALL reject that voucher group

### Requirement: Word number reuse or auto-assign
When importing, if the Excel provides a word head and word number that are unused for the target book and period rules, the system SHALL assign that word number to the new draft. If the word fields are empty or conflict with an existing voucher, the system SHALL allocate the next available word number using the same rules as the normal draft/create path (`able-word-num` semantics).

#### Scenario: Non-conflicting word reused
- **WHEN** Excel specifies word head and word number with no conflict in the book
- **AND** the voucher group is otherwise valid
- **THEN** the created draft SHALL use that word head and word number

#### Scenario: Conflict auto-assigns
- **WHEN** Excel specifies a word head and word number that already exist for the book under numbering rules
- **AND** the voucher group is otherwise valid
- **THEN** the system SHALL create the draft with an auto-assigned available word number

#### Scenario: Empty word auto-assigns
- **WHEN** Excel omits word head or word number
- **AND** the voucher group is otherwise valid
- **THEN** the system SHALL auto-assign an available word number

### Requirement: Import result summary
After an import attempt, the system SHALL return a summary that includes at least the count of successfully created vouchers, the count of failed voucher groups, and a per-failure reason suitable for display to the user.

#### Scenario: Partial success reported
- **WHEN** a file contains both valid and invalid voucher groups
- **THEN** the response SHALL report success count greater than zero and failure count greater than zero
- **AND** each failure SHALL include a human-readable reason

### Requirement: List UI actions for template, import, and export
The voucher list page SHALL expose actions to download the import template, upload an Excel file for import, and export using current filters. These actions MUST be available to users who already may export vouchers (same authorization class as existing export), unless product roles further restrict write/import.

#### Scenario: More menu includes import and template
- **WHEN** an authorized user opens the voucher list “更多” menu
- **THEN** the menu SHALL include download-template and import actions in addition to export

#### Scenario: Import feedback shown
- **WHEN** import completes with a result summary
- **THEN** the UI SHALL present success and failure counts (and failure reasons when present)
- **AND** the list SHALL be refreshable to show newly created drafts
