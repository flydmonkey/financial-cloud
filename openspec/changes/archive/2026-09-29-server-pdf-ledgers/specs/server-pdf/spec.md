## ADDED Requirements

### Requirement: General ledger PDF export
The system SHALL allow an authorized user to download the current book's general ledger for a selected period as a PDF file.

#### Scenario: General ledger PDF download
- **WHEN** an authorized user requests general-ledger PDF export for a book and period
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include subject codes/names, period summaries, and debit/credit/balance columns

### Requirement: Sub-ledger PDF export
The system SHALL allow an authorized user to download the current book's subject sub-ledger (明细账) for the selected query filters as a PDF file.

#### Scenario: Sub-ledger PDF download
- **WHEN** an authorized user requests sub-ledger PDF export with book and filter parameters
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include date, voucher word/number, summary, debit, credit, and running balance columns for matching rows
