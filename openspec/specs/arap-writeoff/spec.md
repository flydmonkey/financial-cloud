# arap-writeoff Specification

## Purpose

Provide open-item write-off (核销) for AR/AP counterparts so aging and month-end overdue use matched remaining balances instead of FIFO estimates alone.

## Requirements

### Requirement: Open-item write-off matching
The system SHALL allow authorized users to match posted AR/AP voucher lines (tagged with customer or supplier assist) for a single counterpart into write-off pairs or groups. Partial write-off of a line SHALL be supported. A write-off MUST NOT create or require a separate invoice/receipt document entity in this capability.

#### Scenario: Manual match two lines for one customer
- **WHEN** a user selects an open receivable line and an offsetting receipt/credit line for the same customer
- **AND** submits a write-off for amount A not exceeding either line’s remaining open amount
- **THEN** the system SHALL persist the match
- **AND** each line’s remaining open amount SHALL decrease by A

#### Scenario: Reject cross-counterpart match
- **WHEN** the user attempts to match lines belonging to different counterparts
- **THEN** the system SHALL reject the request with a clear validation error

#### Scenario: Partial write-off leaves remainder open
- **WHEN** a line of 100 is matched for 40
- **THEN** the remaining open amount for that line SHALL be 60
- **AND** the line SHALL still appear in open-item lists until fully written off

### Requirement: List open items by counterpart
The system SHALL list open (not fully written-off) AR or AP lines for a selected counterpart and as-of date, showing original amount, written-off amount, and remaining open amount.

#### Scenario: Open list after partial write-off
- **WHEN** open items are queried for a counterpart after a partial write-off
- **THEN** lines with remaining open amount greater than zero SHALL appear
- **AND** fully written-off lines MUST NOT appear in the default open list

### Requirement: Reverse write-off under guards
The system SHALL allow reversing a write-off when business guards pass (e.g. period still open or product-defined undo window). Reversal MUST restore open amounts atomically for all lines in that write-off.

#### Scenario: Reverse restores open amounts
- **WHEN** the user reverses an eligible write-off
- **THEN** the matched amounts SHALL return to open
- **AND** subsequent aging/open lists SHALL reflect the restored remainders

#### Scenario: Reverse blocked when not allowed
- **WHEN** guards disallow reverse (e.g. closed period policy)
- **THEN** the system SHALL reject the reverse with a clear error

### Requirement: Suggested match does not auto-post
The system MAY suggest FIFO or amount-based match candidates for a counterpart. Suggested matches MUST NOT be persisted until the user explicitly confirms.

#### Scenario: Suggestion preview only
- **WHEN** the user requests match suggestions
- **THEN** the system SHALL return candidate pairs/amounts
- **AND** MUST NOT create write-off records until confirmation
