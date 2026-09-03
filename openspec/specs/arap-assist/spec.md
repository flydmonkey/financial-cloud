# arap-assist Specification

## Purpose

Deliver a minimum accounts-receivable/payable loop on existing customer/supplier assist dimensions: balances, details, statement export, and aging buckets—without write-off matching.

## Requirements

### Requirement: AR/AP balance by counterparty assist
The system SHALL provide accounts-receivable and accounts-payable balance lists for the current book, aggregated by customer assist (应收) or supplier assist (应付), including opening, period debit/credit, and ending balance for a selected period. Only posted vouchers SHALL contribute to balances.

#### Scenario: List receivable balances by customer
- **WHEN** a user with permission opens the receivable balance view for a period
- **THEN** the system SHALL list customers (assist type 客户) with non-zero or all balances per product filter defaults
- **AND** each row SHALL show opening, period movements, and ending balance

#### Scenario: List payable balances by supplier
- **WHEN** a user opens the payable balance view for a period
- **THEN** the system SHALL list suppliers with balances derived from posted payable-subject lines tagged with supplier assist

#### Scenario: Empty period returns empty or zero rows without error
- **WHEN** no posted AR/AP auxiliary activity exists in the period
- **THEN** the system SHALL return a successful empty or all-zero result set
- **AND** MUST NOT fail solely because no counterparts have activity

### Requirement: AR/AP detail ledger by counterparty
The system SHALL provide a detail ledger for a selected counterparty assist and period, showing chronological posted voucher lines that affect AR or AP for that counterpart, with running balance.

#### Scenario: Drill from balance to detail
- **WHEN** the user selects a counterparty row from the balance list
- **THEN** the system SHALL show that counterpart’s AR or AP detail lines for the chosen period
- **AND** each line SHALL identify voucher word/number and date

#### Scenario: Detail respects book and period filters
- **WHEN** detail is requested for book B, counterpart C, and period P
- **THEN** lines from other books or outside P MUST NOT appear

### Requirement: Counterparty statement Excel export
The system SHALL allow exporting a statement of account (对账单) as Excel for one counterparty, one AR or AP side, and a date or period range, containing opening balance, period lines, and ending balance suitable for customer/supplier confirmation.

#### Scenario: Export statement for one customer
- **WHEN** the user requests statement export for a customer and range
- **THEN** the system SHALL download an Excel file with opening, movements, and ending balance for that customer’s receivables

#### Scenario: Reject export without counterpart
- **WHEN** statement export is requested without a valid counterpart id
- **THEN** the system SHALL reject the request with a clear validation error

### Requirement: Aging analysis buckets
The system SHALL provide aging analysis for AR and AP as of a selected as-of date, bucketing each counterpart’s ending balance into at least: within 30 days, 31–60, 61–90, 91–180, and over 180 days. Aging SHALL be based on posted voucher line dates (or document dates used by the product) for open balances under a FIFO or remaining-balance allocation rule documented in design.

#### Scenario: Aging returns standard buckets
- **WHEN** the user runs AR aging as of date D
- **THEN** each counterpart row SHALL include amounts in the standard buckets and a total equaling the ending balance at D

#### Scenario: Bucket totals reconcile to ending balance
- **WHEN** aging is computed for a counterpart
- **THEN** the sum of bucket amounts for that counterpart SHALL equal that counterpart’s AR or AP ending balance as of D

#### Scenario: Zero balance counterparts may be omitted
- **WHEN** a counterpart’s ending balance as of D is zero
- **THEN** the system MAY omit the row from the default aging list
