# voucher-import-export Specification

## Purpose

Enable accountants to download a shared Excel template, export all vouchers matching list filters, and re-import that file (or filled templates) as draft vouchers, with word-number conflict handling and list actions for template/import/export and batch submit of drafts.

## Requirements

### Requirement: Unified Excel template for import and export
The system SHALL provide a single Excel (xlsx) column layout used for both the downloadable import template and voucher export files, such that an exported file can be submitted to import without reformatting columns. The system SHALL expose a download-template action that returns an empty (or header-only) workbook matching that layout. The import template SHALL include a filling-instruction sheet and a styled data sheet; export SHALL use the same data-sheet column contract and comparable header styling.

#### Scenario: Download import template
- **WHEN** an authorized user requests the voucher import template
- **THEN** the system SHALL return an xlsx file whose data-sheet columns match the export layout
- **AND** the workbook SHALL include an instruction sheet and a styled voucher data sheet

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

### Requirement: Import creates or overwrites as draft
By default, import SHALL create new vouchers. Identifier columns in the file MUST NOT be used to update by id. Successfully imported or overwrite-updated vouchers MUST be stored with status draft (暂存). Import MUST NOT set posting (sender) fields and MUST NOT post to subject balances, unless the operation is an overwrite of an existing unposted voucher that clears workflow fields and remains draft.

#### Scenario: Successful import is draft
- **WHEN** a valid voucher block in the Excel file passes validation and is created as new
- **THEN** the system SHALL create a new voucher in draft status
- **AND** SHALL NOT mark it reviewed or posted

#### Scenario: No update by id
- **WHEN** the Excel file contains a voucher id that already exists in the book
- **AND** the row is otherwise valid
- **AND** the user is not performing a word-number overwrite of that voucher
- **THEN** the system SHALL create a new voucher rather than updating the existing one by id

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

### Requirement: Word number reuse, conflict decision, or auto-assign
When importing, if the Excel provides a word head and word number that are unused for the target book and period rules, the system SHALL assign that word number to the new draft. If the word fields are empty, the system SHALL allocate the next available word number using `able-word-num` semantics. If the word conflicts with an existing **unposted** voucher and the client has not supplied a conflict mode, the system SHALL return a conflict decision payload without writing. If the client supplies overwrite, the system SHALL update that unposted voucher’s entries and leave it as draft. If the client supplies skip, the system SHALL skip that group. If the conflicting voucher is **posted**, the system SHALL fail that group and MUST NOT overwrite it.

#### Scenario: Non-conflicting word reused
- **WHEN** Excel specifies word head and word number with no conflict in the book
- **AND** the voucher group is otherwise valid
- **THEN** the created draft SHALL use that word head and word number

#### Scenario: Unposted conflict requires decision
- **WHEN** Excel specifies a word head and word number that match an unposted voucher
- **AND** no conflict mode is provided
- **THEN** the system SHALL return a needs-conflict-decision result with conflict details
- **AND** SHALL NOT persist import changes for that request

#### Scenario: Overwrite unposted conflict
- **WHEN** the client imports with conflict mode overwrite
- **AND** a group’s word matches an unposted voucher
- **AND** the group is otherwise valid
- **THEN** the system SHALL update that voucher’s journal lines
- **AND** the voucher SHALL remain or become draft

#### Scenario: Skip unposted conflict
- **WHEN** the client imports with conflict mode skip
- **AND** a group’s word matches an unposted voucher
- **THEN** the system SHALL skip that group without changing the existing voucher

#### Scenario: Posted conflict not overwritten
- **WHEN** Excel specifies a word that matches a posted voucher
- **THEN** the system SHALL fail that group
- **AND** MUST NOT overwrite the posted voucher

#### Scenario: Empty word auto-assigns
- **WHEN** Excel omits word head or word number
- **AND** the voucher group is otherwise valid
- **THEN** the system SHALL auto-assign an available word number

### Requirement: Import result summary
After an import attempt, the system SHALL return a summary that includes at least the count of successfully created or overwritten vouchers, the count of failed voucher groups, skipped counts when applicable, and a per-failure reason suitable for display to the user. When a conflict decision is required, the summary SHALL include conflict details for the UI.

#### Scenario: Partial success reported
- **WHEN** a file contains both valid and invalid voucher groups
- **THEN** the response SHALL report success count and failure count appropriately
- **AND** each failure SHALL include a human-readable reason

### Requirement: List UI actions for template, import, export, and submit drafts
The voucher list page SHALL expose actions to download the import template, upload an Excel file for import, and export using current filters. The list「更多」menu SHALL also expose「提交审核」to batch-submit selected draft vouchers via the existing submit-batch path. Import and write actions MUST be available under the same authorization class as existing voucher write/export unless product roles further restrict them.

#### Scenario: More menu includes import and template
- **WHEN** an authorized user opens the voucher list “更多” menu
- **THEN** the menu SHALL include download-template and import actions in addition to export

#### Scenario: More menu includes submit drafts
- **WHEN** an authorized user selects one or more draft vouchers
- **AND** activates「提交审核」
- **THEN** the system SHALL submit those drafts using the existing batch submit behavior

#### Scenario: Import feedback shown
- **WHEN** import completes with a result summary
- **THEN** the UI SHALL present success and failure counts (and failure reasons when present)
- **AND** the list SHALL be refreshable to show newly created or updated drafts

#### Scenario: Conflict decision UI
- **WHEN** import returns a needs-conflict-decision result
- **THEN** the UI SHALL offer overwrite, skip conflicts, and cancel
- **AND** cancel SHALL NOT send a write request
