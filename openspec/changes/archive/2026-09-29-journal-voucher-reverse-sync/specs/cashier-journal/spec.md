## ADDED Requirements

### Requirement: Voucher changes sync back to linked journal entries
When a voucher linked from one or more journal entries is deleted or voided, the system SHALL clear those entries' `voucherId` so the cash-book rows remain and can be edited or re-generated. When such a linked voucher is successfully updated while still unposted, the system SHALL write the voucher date, remark, fund-side amount, and counterpart subject back onto each linked journal entry and recalculate running balances for affected accounts. If the voucher items no longer contain a line for the journal account's fund subject, the update SHALL be rejected with no partial journal writes.

#### Scenario: Delete voucher unlinks journal
- **WHEN** an unposted voucher that is referenced by a journal entry is deleted
- **THEN** that journal entry's `voucherId` SHALL be cleared
- **AND** the journal entry itself SHALL remain

#### Scenario: Void voucher unlinks journal
- **WHEN** a voucher linked to a journal entry is voided
- **THEN** that journal entry's `voucherId` SHALL be cleared

#### Scenario: Update voucher writes amount back
- **WHEN** an unposted linked voucher's fund-side amount or date is changed and saved
- **THEN** the linked journal entry SHALL receive the new amount, trade date, and remark
- **AND** account running balances SHALL be recalculated

#### Scenario: Incompatible voucher structure rejected
- **WHEN** a linked voucher is saved without a journal-line matching the account fund subject
- **THEN** the update SHALL fail
- **AND** journal entries SHALL be unchanged
