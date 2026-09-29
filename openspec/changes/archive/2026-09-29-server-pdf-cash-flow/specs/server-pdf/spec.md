## ADDED Requirements

### Requirement: Cash flow statement PDF export
The system SHALL allow an authorized user to download the current book's cash flow statement for a selected period as a PDF file.

#### Scenario: Cash flow PDF download
- **WHEN** an authorized user requests cash-flow PDF export for a book and period
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include cash-flow line items and period amounts
