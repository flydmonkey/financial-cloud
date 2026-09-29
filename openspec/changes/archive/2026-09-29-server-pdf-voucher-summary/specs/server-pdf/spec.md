## ADDED Requirements

### Requirement: Voucher summary PDF export
The system SHALL allow an authorized user to download the voucher summary report for a selected period as a PDF file.

#### Scenario: Voucher summary PDF download
- **WHEN** an authorized user requests voucher-summary PDF export
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include subject codes/names and period debit/credit amounts
