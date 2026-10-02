## Purpose

Preserve actual balance sheet totals across queries and delivery outputs, and expose reconciliation differences so users can identify inconsistent accounting data without a silently adjusted grand total.

## ADDED Requirements

### Requirement: Correct R&D binding and completed carry validation
The small-enterprise balance-sheet R&D default rule SHALL use subject 4301 instead of non-operating income 5301. Month-end close SHALL reject nonzero remaining carry source balances even when an earlier carry record exists.

#### Scenario: Revenue posted after an earlier carry
- **WHEN** an income carry exists and later posted revenue leaves a nonzero income balance
- **THEN** verify and checkout fail until a supplemental carry is posted

#### Scenario: Non-operating income is not an asset
- **WHEN** a small-enterprise book posts credit income to 5301
- **THEN** its R&D asset line does not include that income balance

#### Scenario: Reversed expense balances
- **WHEN** a carry includes positive expense balances and negative balances from expense reversals
- **THEN** each source balance is closed on the opposite side and the profit counterpart uses their signed net amount

### Requirement: Preserve actual totals
The system SHALL preserve calculated asset and liability/equity totals in non-strict queries, print, Excel and PDF outputs.

#### Scenario: Out of balance query
- **WHEN** assets total 100000 and liabilities/equity total 90000
- **THEN** the response retains both totals and reports a signed difference of 10000 and balanced=false

#### Scenario: Negative difference
- **WHEN** assets total 90000 and liabilities/equity total 100000
- **THEN** the difference is -10000 and neither total changes

### Requirement: Visible reconciliation status
The system SHALL display an out-of-balance warning containing actual totals and signed difference for the loaded report. The warning SHALL appear in browser print content.

#### Scenario: User views inconsistent report
- **WHEN** the response reports balanced=false
- **THEN** the report page shows the totals and difference and recommends checking profit/loss carry-forward, opening balances and report rules

#### Scenario: Balanced or unavailable report
- **WHEN** the difference is within 0.01 inclusive or totals are unavailable
- **THEN** no out-of-balance warning is displayed and unavailable totals are not reported as balanced

### Requirement: Strict mode remains blocking
The system SHALL preserve error code 513013 when strict mode is enabled and the absolute difference exceeds 0.01.

#### Scenario: Strict query fails
- **WHEN** strict mode is enabled and difference is 0.02
- **THEN** the query fails with 513013 and no total is modified

### Requirement: Book-scoped report rules
The system SHALL query balance-sheet rules using the authenticated user's current book and the requested item code.

#### Scenario: Different standards coexist
- **WHEN** two accessible books use different accounting standards with the same report item code
- **THEN** the rule endpoint returns only rules belonging to the current book, including after switching books
