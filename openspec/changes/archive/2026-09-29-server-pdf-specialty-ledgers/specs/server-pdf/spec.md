## ADDED Requirements

### Requirement: Multi-column ledger PDF export
The system SHALL allow an authorized user to download a multi-column ledger for a selected parent subject and date range as a PDF file.

#### Scenario: Multi-column PDF download
- **WHEN** an authorized user requests multi-column-ledger PDF export with subject and date range
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include date, voucher word/number, summary, dynamic child-subject columns, row total, and running balance

### Requirement: Quantity ledger PDF export
The system SHALL allow an authorized user to download a quantity-amount ledger for a selected subject and date range as a PDF file.

#### Scenario: Quantity ledger PDF download
- **WHEN** an authorized user requests quantity-ledger PDF export with subject and date range
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include in/out/balance quantity, unit price, and amount columns for matching entries
