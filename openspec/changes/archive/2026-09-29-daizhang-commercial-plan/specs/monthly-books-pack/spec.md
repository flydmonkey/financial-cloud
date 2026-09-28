## Purpose

Provide bookkeeping firms a one-click monthly delivery package: a ZIP of the period's core ledgers and statements so accountants can hand off books without exporting each report separately.

## ADDED Requirements

### Requirement: Export monthly books pack as ZIP
The system SHALL allow an authorized user to export a monthly books pack for the current book and a specified `YYYY-MM` period as a single ZIP download. The pack MUST include, as separate files for that period: subject balance sheet, subject detail ledger (明细账), balance sheet, income statement, and cash flow statement. Each entry's name MUST accurately describe its accounting content.

#### Scenario: Successful pack download for a period with data
- **WHEN** an authorized user requests a monthly books pack for book B and period P
- **AND** the book has statement or ledger data for P (or empty templates that still represent the period)
- **THEN** the system SHALL return a ZIP whose filename identifies book and period
- **AND** the ZIP SHALL contain files covering subject balance, detail ledger, balance sheet, income statement, and cash flow statement for P

#### Scenario: Detail ledger preserves transaction-level traceability
- **WHEN** the pack contains a file named as a subject detail ledger for period P
- **THEN** that file SHALL expose transaction-level rows traceable to vouchers, including voucher identity, debit or credit amounts, and running balance
- **AND** a summarized general-ledger workbook MUST NOT be substituted under the detail-ledger name

#### Scenario: Pack contents match single-report exports
- **WHEN** the user exports the monthly books pack for period P
- **AND** separately exports the same period via existing single-report export endpoints for the same reports
- **THEN** corresponding monetary totals in the pack files SHALL match the single-report exports for P

### Requirement: Optional voucher list in the pack
The system SHALL allow the user to include or exclude a voucher list (凭证清单) for the period when requesting the pack. When included, the ZIP MUST contain a voucher-list file identifying individual vouchers dated in that period for the book. An aggregate voucher summary MUST NOT be substituted under the voucher-list name. Default MUST be include.

#### Scenario: Include voucher list by default
- **WHEN** the user requests a pack without explicitly disabling the voucher list
- **THEN** the ZIP SHALL contain a voucher-list file for the period

#### Scenario: Exclude voucher list when opted out
- **WHEN** the user requests a pack with voucher list disabled
- **THEN** the ZIP MUST NOT contain a voucher-list file
- **AND** the required ledger and statement files SHALL still be present

### Requirement: Period and book scoping
The system MUST generate pack contents only for the authenticated user's current book and the requested period. The system MUST reject requests for another book or a malformed period.

#### Scenario: Reject malformed period
- **WHEN** the user requests a pack with a period that is not a valid `YYYY-MM`
- **THEN** the system SHALL reject the request with a clear error
- **AND** no ZIP SHALL be returned

#### Scenario: Pack uses current book only
- **WHEN** the user requests a pack
- **THEN** all files in the ZIP SHALL reflect only the current book context
- **AND** data from other books MUST NOT appear

### Requirement: Authorization and failure semantics
Only authenticated users who can access the current book's settlement or statement delivery area SHALL be allowed to export the pack. The system MUST apply the same effective access rule as that area and MUST NOT accept a client-selected book outside the authenticated current-book context. On generation failure after the request is accepted, the system MUST fail the download with a clear error and MUST NOT return a partial ZIP presented as success.

#### Scenario: Unauthenticated or inaccessible-book request rejected
- **WHEN** an unauthenticated user or a user without access to the current book requests a pack
- **THEN** the system SHALL reject the request
- **AND** no ZIP SHALL be returned

#### Scenario: Generation failure does not yield partial success
- **WHEN** pack assembly fails while producing one of the required files
- **THEN** the system SHALL fail the overall download
- **AND** the client MUST be able to show an error rather than treating a truncated archive as success

### Requirement: Entry point for delivery workflow
The product UI MUST expose an explicit「导出本月账本包」action in the settlement or statement delivery area, with period selection defaulting to the book's current term and showing the selected period clearly.

#### Scenario: User can trigger pack from delivery UI
- **WHEN** an authorized user opens the settlement or statement delivery screen
- **THEN** they SHALL see an action to export the monthly books pack
- **AND** confirming the action SHALL start the pack download for the selected period
