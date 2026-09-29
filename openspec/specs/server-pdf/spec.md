## Purpose

Provide a server-side PDF download channel for bookkeeping delivery so accountants can obtain portable statement PDFs without relying on the browser print dialog.

## Requirements

### Requirement: Shared server-side table PDF writer
The system SHALL provide a shared server-side PDF writer for tabular financial statements that supports a document title, optional subtitle, column headers, and data rows, and that renders Chinese characters correctly when a usable CJK font is available on the host.

#### Scenario: PDF contains title and table
- **WHEN** the writer is invoked with a title, headers, and rows
- **THEN** it SHALL produce a PDF byte stream whose content type is `application/pdf`
- **AND** the document SHALL include the title and a table matching the provided headers and row count

### Requirement: Balance sheet PDF export
The system SHALL allow an authorized user to download the current book's balance sheet for a selected period as a PDF file.

#### Scenario: Balance sheet PDF download
- **WHEN** an authorized user requests balance-sheet PDF export for a book and period
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL identify the statement as a balance sheet and include asset and liability/equity line amounts for that period

### Requirement: Income statement PDF export
The system SHALL allow an authorized user to download the current book's income statement for a selected period as a PDF file.

#### Scenario: Income statement PDF download
- **WHEN** an authorized user requests income-statement PDF export for a book and period
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include income-statement line items and period amounts

### Requirement: Cash flow statement PDF export
The system SHALL allow an authorized user to download the current book's cash flow statement for a selected period as a PDF file.

#### Scenario: Cash flow PDF download
- **WHEN** an authorized user requests cash-flow PDF export for a book and period
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include cash-flow line items and period amounts

### Requirement: Subject balance PDF export
The system SHALL allow an authorized user to download the current book's subject balance sheet for a selected period as a PDF file.

#### Scenario: Subject balance PDF download
- **WHEN** an authorized user requests subject-balance PDF export for a book and period
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include subject codes/names and balance columns for the period

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

### Requirement: Expense detail PDF export
The system SHALL allow an authorized user to download the current book's expense detail report for a selected period range as a PDF file.

#### Scenario: Expense detail PDF download
- **WHEN** an authorized user requests expense-detail PDF export for a book and period range
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include subject codes/names and period amount columns consistent with the on-screen report

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

### Requirement: Voucher summary PDF export
The system SHALL allow an authorized user to download the voucher summary report for a selected period as a PDF file.

#### Scenario: Voucher summary PDF download
- **WHEN** an authorized user requests voucher-summary PDF export
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include subject codes/names and period debit/credit amounts
