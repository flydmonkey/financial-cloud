## Purpose

Provide a professional monthly period-close (月结) flow: ordered checklist, hard system gates before checkout, period lock after close, and no separate year-end closing entry—aligned with Kingdee-style month-end only.

## ADDED Requirements

### Requirement: Unified month-end close wizard
The system SHALL present month-end close as a single guided wizard for the book's current open term, with ordered steps that cover readiness checks, accrue/carry-forward actions, system verification, and checkout. The system MUST NOT expose a separate “年终结账 / year-end close” entry point; December year-end carry items SHALL appear only as that month's carry-forward templates or checklist items.

#### Scenario: Open wizard on current term
- **WHEN** a user with settlement permission opens month-end close
- **THEN** the wizard SHALL show the book's current open year-period
- **AND** SHALL offer the ordered steps for that period only

#### Scenario: No separate year-end entry
- **WHEN** the user browses settlement / period-close navigation
- **THEN** the system MUST NOT offer a distinct year-end closing workflow or menu item independent of monthly close

#### Scenario: Cannot skip ahead past failed hard gates
- **WHEN** any hard gate for an earlier step has failed
- **THEN** the system SHALL prevent advancing to checkout
- **AND** SHALL keep the checkout action disabled or rejected until those gates pass

### Requirement: Hard gates before checkout
Before allowing checkout of the current open term, the system SHALL evaluate hard verification items and MUST refuse checkout when any hard item fails. Hard items MUST include at least:

1. No unposted vouchers remain in the current open term (and no vouchers left in a non-postable incomplete state that the product treats as blocking close—e.g. draft/unaudited when audit is required before posting).
2. Voucher word-number continuity for the current term passes (same rule family as existing successive check).
3. Period debit totals equal credit totals for voucher amounts in scope of the check.
4. Required period carry-forward vouchers for the current term have been generated per the book's carry-forward templates that are marked required for close.
5. Fixed-asset depreciation for the current term has been accrued when the book has assets that require depreciation in that term; if no such assets exist, this item SHALL pass as not applicable.

#### Scenario: Unposted voucher blocks checkout
- **WHEN** at least one voucher in the current open term is not posted
- **AND** the user attempts checkout
- **THEN** the system SHALL reject checkout
- **AND** verification results SHALL mark the unposted-voucher item as failed

#### Scenario: Missing required carry-forward blocks checkout
- **WHEN** a required carry-forward template for the current term has no generated carry-forward voucher
- **AND** the user attempts checkout
- **THEN** the system SHALL reject checkout
- **AND** verification results SHALL identify the missing carry-forward item

#### Scenario: Depreciation accrued or not applicable
- **WHEN** the book has no assets requiring depreciation in the current term
- **THEN** the depreciation hard item SHALL pass as not applicable
- **WHEN** such assets exist and depreciation for the term has not been accrued
- **THEN** checkout SHALL be rejected until accrual completes

#### Scenario: All hard gates pass
- **WHEN** every hard verification item passes for the current open term
- **THEN** the system SHALL allow checkout to proceed (subject to existing checkout business rules such as not already settled)

### Requirement: Manual confirmation items are non-blocking for system verify
Items that the product cannot system-check in this change—including AR/AP write-off (核销), aging, bank reconciliation, inventory stocktake, and tax-filing cross-checks—SHALL be presented as manual confirmation (人工确认) or explicitly labeled “本期不系统检”. The system MUST NOT treat absence of 往来核销/账龄 modules as a hard verify failure. Completing checkout MUST NOT require those modules to exist.

#### Scenario: 往来 placeholder does not fail verify
- **WHEN** hard gates otherwise pass
- **AND** 往来核销 / 账龄 are not implemented
- **THEN** system verification SHALL still be allowed to succeed
- **AND** the UI SHALL show 往来-related rows as manual / not system-checked for this release

#### Scenario: User acknowledges manual checklist
- **WHEN** the wizard shows manual confirmation items
- **THEN** the user MUST be able to acknowledge them in the UI before checkout
- **AND** acknowledgment MUST NOT be confused with a passed hard system gate in the verify API results

### Requirement: Checkout still snapshots and advances the term
On successful checkout, the system SHALL retain existing close side effects: persist settlement for the closed term, write period statement/balance snapshots as today, run journal-account checkout balance rollover, and advance the book's current open term to the next month. Uncheckout behavior and guards from the existing settlement-uncheckout capability SHALL remain in force.

#### Scenario: Successful month-end checkout
- **WHEN** all hard gates pass
- **AND** the user confirms checkout for the current open term T
- **THEN** the system SHALL record T as settled
- **AND** SHALL advance the current open term to the month after T
- **AND** SHALL produce the same class of checkout snapshots and journal opening updates as the current checkout path

### Requirement: Closed periods reject new and mutating voucher writes
After a term is settled (or for any voucher period strictly before the book's current open term), the system MUST reject creating a new voucher dated in a closed/non-open period, and MUST reject mutating operations that would alter vouchers in non-open periods, consistent with period-lock intent. The create/save path for new vouchers MUST enforce this (closing the known gap where only some mutation paths were guarded).

#### Scenario: New voucher in closed period rejected
- **WHEN** the book's current open term is T
- **AND** the user attempts to save a new voucher whose period is before T
- **THEN** the system SHALL reject the save with a clear period-lock error
- **AND** no new voucher row SHALL be persisted

#### Scenario: Open period voucher still editable under existing rules
- **WHEN** the user saves or mutates a voucher in the current open term
- **AND** other existing voucher rules (status, permissions) allow the operation
- **THEN** the period-lock rule SHALL NOT block solely because of period lock

### Requirement: Accrue and carry actions reachable from the wizard
The month-end wizard SHALL let the user open or complete fixed-asset depreciation accrual and required carry-forward voucher generation for the current term without leaving the overall month-end close context (embedded UI or in-flow navigation that returns to the wizard with refreshed statuses).

#### Scenario: Carry-forward status visible in wizard
- **WHEN** the user is on the accrue/carry step
- **THEN** the system SHALL show which required carry-forward items are done or pending for the current term

#### Scenario: After generating carry-forward, status refreshes
- **WHEN** the user generates a required carry-forward voucher from the wizard flow
- **THEN** the corresponding checklist/verify status SHALL update to reflect completion without requiring a full app reload to see the change
