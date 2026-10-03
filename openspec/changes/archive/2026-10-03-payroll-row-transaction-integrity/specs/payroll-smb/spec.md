## ADDED Requirements

### Requirement: Serialized confirmed payroll writes
Confirmed salary creation, direct modification, batch deletion, payroll voucher generation and removal, and preview-to-confirmed salary replacement SHALL coordinate writes within the same book for the duration of their transactions. Competing operations MUST evaluate the current committed salary and linked-voucher state after any wait, including when a caller already has an older transaction snapshot. The same employee and belonging month on different salary rows MUST remain protected by the existing live-voucher duplicate rule. The operated salary records MUST belong to the coordinated book. Batch deletion MUST validate the complete batch before mutations. Independent books SHALL remain able to progress independently when these entry points start their own transactions. Such new transactions MUST use READ COMMITTED; joining an existing transaction MUST preserve its isolation level and may retain its range or gap contention. This coordination SHALL preserve existing authorization, template type grouping and linked-detail correction rules; it does not establish concurrency guarantees for general voucher, month-end, backup/restore or preview-editing operations outside these participating payroll entry points.

#### Scenario: Concurrent generation on one detail or historical peers
- **WHEN** two requests generate the same link type for one detail or different details of the same book, employee and belonging month
- **THEN** at most one creates a linked voucher and the waiting request rejects generation after observing the committed live reference

#### Scenario: Generation precedes direct correction
- **WHEN** voucher generation commits while a direct salary modification or deletion is waiting
- **THEN** the waiting operation rejects the linked detail and preserves its values, links and voucher

#### Scenario: Direct correction precedes generation
- **WHEN** an unlinked detail is modified or deleted before a waiting generation request resumes
- **THEN** generation uses the committed corrected salary values or rejects a missing detail without creating a voucher

#### Scenario: Generation competes with replacement or voucher removal
- **WHEN** salary replacement or payroll voucher removal competes with generation in the same book
- **THEN** replacement evaluates current links before replacing any detail and voucher removal evaluates the current requested link under the existing removal rules

#### Scenario: Older caller snapshot
- **WHEN** a caller has read an earlier salary state before waiting for another participating payroll transaction to commit
- **THEN** the resumed operation evaluates the newly committed salary and effective duplicate references instead of the older snapshot

#### Scenario: Batch and book boundaries
- **WHEN** a batch contains a linked, missing or differently owned detail, or these entry points start transactions for writes in another book
- **THEN** the invalid batch performs no salary deletion and unrelated books can progress independently, including creating a voucher or replacing confirmed salary rows

### Requirement: Atomic payroll voucher generation
Payroll voucher generation MUST commit its voucher, associated items and word records, and the selected salary link together. An unsuccessful save, a zero-row link update or an exception MUST roll back all changes made by that generation attempt. A request that is rejected before save MUST preserve salary values and existing links, including stale links. Successful generation SHALL update only the requested current detail's selected link and preserve the other link. Invalid types, missing book scope and missing or differently owned salary details MUST fail before a voucher is created.

#### Scenario: Save reports failure after partial writes
- **WHEN** voucher save returns a failure after creating some dependent records
- **THEN** generation returns the failure and rolls back all records and salary changes from that attempt

#### Scenario: Link write returns zero or throws
- **WHEN** the voucher was saved but its salary link update affects no row or raises an error
- **THEN** generation fails and the voucher, items, word records and salary changes are all rolled back

#### Scenario: Invalid request or missing template
- **WHEN** generation has an invalid type, book scope or detail, or a required template is absent
- **THEN** it creates no voucher and does not clear the existing salary links

#### Scenario: Successful selected link write
- **WHEN** generation succeeds under the existing voucher rules
- **THEN** the saved voucher and current detail's selected link are committed together while the other link remains unchanged
