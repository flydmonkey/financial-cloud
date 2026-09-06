## Purpose

Defines credible cashier journal behavior: correct cash/bank running balances across create/update/delete, business-date settlement guards, usable list filters, and journal-to-voucher postings that use real book subjects so drafts can be completed and posted.

## Requirements

### Requirement: Journal entry balance lifecycle
The system SHALL keep each journal account's available balance and each entry's running `balance` consistent with the chronological effect of income, expenditure, and opening-init directions for that account. On create, the system SHALL apply the entry's amount to the account and store the resulting running balance on the entry. On delete of an unlocked entry, the system SHALL reverse that entry's effect on the account. On update of amount, direction, or account, the system SHALL reverse the prior effect, apply the new effect (rejecting expenditure that would make the account balance negative), and recalculate running `balance` for the affected entry and all later entries of the same account ordered by trade date then identity. Entries that already reference a voucher MUST NOT allow changes to amount, direction, account, or counterpart subject.

#### Scenario: Create income updates account and entry balance
- **WHEN** a user creates an income entry for an account with sufficient book period open
- **THEN** the account balance increases by the income amount and the entry's stored balance equals the new account balance

#### Scenario: Update middle entry recalculates later running balances
- **WHEN** a user changes the amount of an unlocked entry that is not the latest for its account
- **THEN** the account balance reflects the net correction and every subsequent entry for that account has its running balance recomputed in trade-date order

#### Scenario: Linked voucher blocks money fields
- **WHEN** a user attempts to change amount, direction, account, or counterpart subject on an entry that already has a voucher id
- **THEN** the system rejects the update and leaves account and entry balances unchanged

#### Scenario: Insufficient funds on update
- **WHEN** an update would leave the account balance negative after applying the new expenditure effect
- **THEN** the system rejects the update and leaves prior balances unchanged

### Requirement: Generate voucher with real subjects
The system SHALL create a draft voucher from a journal entry using the journal account's fund subject as the cash/bank side and the entry's counterpart subject as the other side. For income, the fund subject SHALL be debited and the counterpart credited; for expenditure, the counterpart SHALL be debited and the fund subject credited. Voucher line subject id, code, and name MUST come from the book's subject master for those ids. The voucher date and settlement period check MUST use the entry's trade date. The system MUST refuse generation when the entry direction is opening-init, when a voucher is already linked, or when either required subject id is missing or not found in the book.

#### Scenario: Income generates balanced draft with mapped subjects
- **WHEN** a user generates a voucher for an income entry with both fund and counterpart subjects configured
- **THEN** the system saves a draft voucher dated on the trade date with debit fund subject and credit counterpart subject for the income amount, and links the voucher id on the entry

#### Scenario: Expenditure reverses debit and credit sides
- **WHEN** a user generates a voucher for an expenditure entry with both subjects configured
- **THEN** the draft voucher debits the counterpart subject and credits the fund subject for the expenditure amount

#### Scenario: Reject opening-init generation
- **WHEN** a user requests voucher generation for an opening-init entry
- **THEN** the system refuses and does not create a voucher

#### Scenario: Reject missing subject
- **WHEN** either the account fund subject or the entry counterpart subject is blank or unknown
- **THEN** the system refuses generation with a clear error and does not link a voucher id

#### Scenario: Reject duplicate generation
- **WHEN** the entry already has a voucher id
- **THEN** the system refuses generation without creating another voucher

#### Scenario: Closed period on trade date blocks generation
- **WHEN** the accounting period of the entry's trade date is closed
- **THEN** the system refuses generation

### Requirement: Journal entry list filtering
The system SHALL filter journal entry list results by remark substring when provided, and SHALL support filtering by account name when provided. The list MUST be ordered by trade date descending then by identity descending so business chronology is visible. Filter parameters used by the UI MUST match the API query fields (remark and account name), not unrelated field names.

#### Scenario: Filter by remark
- **WHEN** a client requests the entry list with a non-empty remark filter
- **THEN** only entries whose remark contains that substring (within the book) are returned

#### Scenario: Default order by trade date
- **WHEN** a client requests the entry list without a custom sort
- **THEN** results are ordered by trade date descending, then identity descending
