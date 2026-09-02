## Purpose

Enable bookkeeping firms to reopen the most recently closed accounting period safely, restoring journal opening balances and clearing checkout side effects so corrections can be posted and the period closed again.

## ADDED Requirements

### Requirement: Uncheckout only the previous closed period
The system SHALL allow uncheckout only for the settlement period that is exactly one month before the book's current term, and only when that period has an existing closed settlement record.

#### Scenario: Successful uncheckout of previous month
- **WHEN** the book's current term is T+1 and period T is closed
- **AND** the user requests uncheckout for period T
- **AND** all other uncheckout guards pass
- **THEN** the system SHALL set the current term back to T
- **AND** the settlement record for T SHALL be logically deleted (not visible as closed)

#### Scenario: Reject uncheckout of non-adjacent period
- **WHEN** the user requests uncheckout for a closed period that is not the month immediately before the current term
- **THEN** the system SHALL reject the request
- **AND** settlement data, subject balances, journal openings, and current term SHALL remain unchanged

### Requirement: Guard against activity in the newer period
The system MUST refuse uncheckout when the current term (the month after the period being reopened) contains any voucher, or any journal entry whose trade date falls in that month.

#### Scenario: Reject when newer period has vouchers
- **WHEN** uncheckout is requested for T
- **AND** at least one voucher exists for term T+1
- **THEN** the system SHALL reject the request with a clear error
- **AND** no uncheckout side effects SHALL be applied

#### Scenario: Reject when newer period has journal entries
- **WHEN** uncheckout is requested for T
- **AND** at least one journal entry has a trade date in term T+1
- **THEN** the system SHALL reject the request with a clear error
- **AND** no uncheckout side effects SHALL be applied

### Requirement: Reverse checkout side effects transactionally
On successful uncheckout of period T (current term C = T+1), the system SHALL in one transaction: delete monthly subject-balance rows for C created by checkout; restore journal account opening balances from checkout snapshots; remove income-statement and balance-sheet checkout snapshots for T (and quarter/year snapshots only if that checkout wrote them); logically delete settlement T; set current term to T. The system MUST NOT automatically unpost, delete, or alter vouchers (including carry-forward vouchers).

#### Scenario: Subject balances for newer period removed
- **WHEN** uncheckout of T succeeds
- **THEN** monthly statement subject-balance rows for period C SHALL be absent
- **AND** subject-balance rows for period T SHALL remain unchanged

#### Scenario: Vouchers untouched
- **WHEN** uncheckout of T succeeds
- **AND** posted carry-forward or business vouchers exist in period T
- **THEN** those vouchers SHALL remain in their prior posted/status state

#### Scenario: Re-checkout after uncheckout is possible
- **WHEN** uncheckout of T has succeeded
- **AND** income-statement data for T was cleared as part of uncheckout
- **THEN** a subsequent checkout of T SHALL be allowed to generate period statements without failing solely because prior checkout snapshots still exist

### Requirement: Journal opening balance snapshot on checkout
On period checkout, before setting each journal account's opening balance equal to its balance, the system SHALL store the previous opening balance into `prev_opening_balance` on that account. On uncheckout, the system SHALL restore `opening_balance` from `prev_opening_balance`.

#### Scenario: Checkout preserves prior opening
- **WHEN** a period is checked out
- **THEN** each journal account's `prev_opening_balance` SHALL equal the opening balance that existed immediately before checkout
- **AND** `opening_balance` SHALL equal the account balance at checkout

#### Scenario: Uncheckout restores opening
- **WHEN** uncheckout succeeds for accounts that have non-null `prev_opening_balance`
- **THEN** each such account's `opening_balance` SHALL equal its `prev_opening_balance`

#### Scenario: Reject uncheckout without journal snapshot
- **WHEN** uncheckout is requested
- **AND** any journal account for the book has a null `prev_opening_balance` while journal accounts exist (legacy checkout without snapshot)
- **THEN** the system SHALL reject uncheckout with an error stating the period cannot be safely reopened

### Requirement: Same permission and destructive confirmation UX
Any user who can perform checkout for the current book SHALL be allowed to call uncheckout. The UI MUST present uncheckout on the settlement period list for the eligible closed month only, and MUST require the user to type the target year-period before confirming.

#### Scenario: Eligible row shows uncheckout
- **WHEN** the user opens the settlement list
- **AND** the previous month relative to current term is closed
- **THEN** that row SHALL offer an uncheckout action
- **AND** other months SHALL NOT offer uncheckout

#### Scenario: Confirmation requires typing period
- **WHEN** the user initiates uncheckout
- **AND** they have not entered the exact target year-period
- **THEN** the system SHALL NOT submit the uncheckout request

### Requirement: Auditable uncheckout outcome
The system SHALL record a structured log entry for each uncheckout attempt including book id, user id, target period, current term before change, and success or failure reason.

#### Scenario: Successful attempt logged
- **WHEN** uncheckout succeeds
- **THEN** a structured log entry SHALL include book id, user id, reopened period, and success

#### Scenario: Rejected attempt logged
- **WHEN** uncheckout is rejected by a guard
- **THEN** a structured log entry SHALL include the failure reason
- **AND** persisted accounting data SHALL remain unchanged
