# Opening Balance Excel Specification

## Purpose

Speed up bookkeeping-firm opening-balance capture: accountants can export the current book's subject opening balances to Excel, fill amounts offline, and import them back by subject code before initialization is locked.

## Requirements

### Requirement: Export opening balances to Excel
The system SHALL allow an authorized user to download an Excel workbook of the current book's opening-balance subjects. Each data row MUST include subject code, subject name, balance direction, year-opening debit, year-opening credit, year-to-date debit, year-to-date credit, and balance.

#### Scenario: Successful export
- **WHEN** an authorized user requests opening-balance export for the current book
- **THEN** the system SHALL return an Excel file whose filename indicates opening balances
- **AND** the sheet SHALL contain a header row and one data row per listed subject

### Requirement: Download import template
The system SHALL provide an Excel import template with the same column headers as the export, suitable for offline filling.

#### Scenario: Template download
- **WHEN** a user requests the opening-balance import template
- **THEN** the system SHALL return an Excel workbook with the required headers
- **AND** it MAY include a sample row that importers can replace

### Requirement: Import opening balances by subject code
The system SHALL accept an uploaded Excel workbook and update opening-balance amounts for matching subject codes in the current book. Import MUST refuse writes when book initialization is already completed. Rows for unknown codes, non-editable parent aggregates, or subjects already used by vouchers MUST be reported as failures without applying those rows. Successfully matched editable leaf subjects MUST persist through the same save path as interactive edits.

#### Scenario: Import updates matched leaf subjects
- **WHEN** an authorized user uploads a valid workbook while initialization is open
- **AND** a row's subject code matches an editable leaf subject without voucher usage
- **THEN** the system SHALL update that subject's opening and cumulative amount fields from the row
- **AND** the response SHALL report success and failure counts with per-row error messages for failures

#### Scenario: Import blocked after initialization
- **WHEN** book initialization is marked completed
- **AND** a user uploads an opening-balance workbook
- **THEN** the system SHALL reject the import with a clear error and apply no amount changes
