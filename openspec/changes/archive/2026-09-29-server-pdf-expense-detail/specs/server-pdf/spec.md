## ADDED Requirements

### Requirement: Expense detail PDF export
The system SHALL allow an authorized user to download the current book's expense detail report for a selected period range as a PDF file.

#### Scenario: Expense detail PDF download
- **WHEN** an authorized user requests expense-detail PDF export for a book and period range
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include subject codes/names and period amount columns consistent with the on-screen report
