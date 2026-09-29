## Purpose

Provide a server-side PDF download channel for bookkeeping delivery so accountants can obtain portable statement PDFs without relying on the browser print dialog.

## ADDED Requirements

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

### Requirement: Subject balance PDF export
The system SHALL allow an authorized user to download the current book's subject balance sheet for a selected period as a PDF file.

#### Scenario: Subject balance PDF download
- **WHEN** an authorized user requests subject-balance PDF export for a book and period
- **THEN** the system SHALL return an `application/pdf` attachment
- **AND** the PDF SHALL include subject codes/names and balance columns for the period
