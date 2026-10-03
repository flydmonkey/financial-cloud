# payroll-smb Specification

## Purpose

Enable small-business payroll operators to finish a monthly cycle: apply per-employee social insurance bases, complete guided salary calculation, post accrual/payment vouchers, and export a bank payment file—without employee payslips or tax-bureau filing.

## Requirements

### Requirement: Per-employee social insurance contribution base
The system SHALL calculate employee and employer social insurance and housing-fund amounts using either the book-level default contribution base or a per-employee custom base when the employee is configured to use a custom base. When a custom base is selected, a positive contribution base amount MUST be present before salary calculation includes that employee. This capability SHALL treat a single unified custom base per employee as the supported product behavior for the period; per-insurance custom bases that are not applied MUST NOT silently diverge from the unified-base result without documentation in product docs.

#### Scenario: Custom base used in calculation
- **WHEN** a normal employee has custom contribution base enabled and a positive `payBaseNumber`
- **AND** monthly salary calculation runs for that employee
- **THEN** social insurance and housing-fund amounts SHALL be computed from that custom base and the book’s configured rates

#### Scenario: Book default base used
- **WHEN** a normal employee uses the system/default contribution base rule
- **AND** monthly salary calculation runs
- **THEN** social insurance and housing-fund amounts SHALL be computed from the book-level default base and rates

#### Scenario: Reject incomplete custom base
- **WHEN** a normal employee has custom contribution base enabled but no positive custom base amount
- **AND** the user attempts monthly salary calculation including that employee
- **THEN** the system SHALL reject calculation for that employee with a clear validation error naming the missing base

### Requirement: Contribution base visibility on salary preview
The system SHALL expose enough information on the monthly salary preview or employee salary result for an operator to verify which contribution base applied (book default vs employee custom) and the resulting personal social insurance and housing-fund withholdings.

#### Scenario: Operator can verify base on preview
- **WHEN** salary preview rows are shown after calculation
- **THEN** each applicable row SHALL indicate the effective contribution base (or an equivalent clear indicator of default vs custom)
- **AND** SHALL show personal social insurance and housing-fund amounts used in the net pay calculation

### Requirement: Guided monthly payroll path
The system SHALL provide a guided path for one belonging month that covers: select period → generate salary preview → adjust allowable earnings/deductions → push to salary details → generate accrual and/or payment vouchers. The path MUST surface the current step and block progression when a prior required step is incomplete for the selected book and period.

#### Scenario: Complete happy path
- **WHEN** an operator follows the guided path for an open period with eligible employees configured
- **THEN** the system SHALL allow completing preview, push-to-detail, and voucher generation without requiring undocumented side menus for those steps

#### Scenario: Block voucher before details exist
- **WHEN** no salary details exist for the selected book and belonging month
- **AND** the operator attempts to generate accrual or payment vouchers from the guided path
- **THEN** the system SHALL block the action and indicate that salary details must be pushed first

### Requirement: Bank payment file export
The system SHALL allow authorized users to export a bank payment file for a selected book and belonging month from confirmed salary details (or equivalent confirmed monthly payroll rows). The file MUST include at least: employee display name, bank account number, bank name (when available), and net pay amount. Rows missing a bank account number MUST be excluded from the payment file or listed in a blocking validation summary before download—product MUST choose one behavior and apply it consistently. Payslip generation and employee self-service delivery are out of scope.

#### Scenario: Export payment file for confirmed month
- **WHEN** confirmed salary details exist for the book and belonging month with bank account numbers present
- **AND** the user exports the bank payment file
- **THEN** the system SHALL download a CSV or Excel file containing one payment row per eligible employee with name, account number, bank name when available, and net pay

#### Scenario: Handle missing bank accounts
- **WHEN** one or more employees in the selected month lack a bank account number
- **AND** the user requests bank payment file export
- **THEN** the system SHALL either omit those rows and report the omitted employees, or block export with a list of employees missing accounts
- **AND** MUST NOT silently invent account numbers

#### Scenario: Empty month rejects export
- **WHEN** no confirmed salary details exist for the book and belonging month
- **AND** the user requests bank payment file export
- **THEN** the system SHALL reject the export with a clear error

### Requirement: Confirmed payroll voucher integrity
The system SHALL reject direct changes or deletion of a confirmed salary detail while either an accrual or payment voucher is linked. Batch deletion MUST validate the complete batch before any deletion, so that one linked row rejects the entire batch without changing any salary detail or voucher. Unlinked salary details SHALL retain the existing editing and deletion workflow.

#### Scenario: Editing a linked detail
- **WHEN** an operator edits a salary detail linked to an accrual or payment voucher
- **THEN** the system rejects the change with guidance to resolve the linked voucher first and preserves both links and salary values

#### Scenario: Mixed deletion batch
- **WHEN** a deletion request contains unlinked details and a detail linked to a voucher
- **THEN** the entire request fails before any salary detail or voucher changes

#### Scenario: Unlinked detail correction
- **WHEN** an authorized operator edits or deletes a salary detail without voucher links
- **THEN** the existing correction workflow remains available under the existing book permissions

### Requirement: Atomic payroll voucher removal
The system SHALL clear only the requested salary voucher link after successful deletion of that voucher. If voucher deletion is rejected because of its status, period or any existing rule, the system MUST return the actual rejection and preserve all salary links and accounting data. Voucher deletion and link clearing MUST be atomic: failure to clear the link MUST roll back deletion. Invalid voucher types, missing salary records and empty links MUST fail before any mutation.

#### Scenario: Posted voucher deletion is rejected
- **WHEN** an operator attempts to remove a posted salary voucher through payroll
- **THEN** the system returns the voucher deletion rejection, preserves the salary link, voucher and balances, and still rejects duplicate generation

#### Scenario: Draft voucher removal succeeds
- **WHEN** an operator removes a linked draft salary voucher which can be deleted under existing voucher rules
- **THEN** that voucher and its requested link are removed together while the other voucher link remains unchanged

#### Scenario: Link update fails
- **WHEN** voucher deletion succeeds but its salary link cannot be cleared
- **THEN** the operation fails and rolls back the voucher deletion

#### Scenario: Invalid payroll voucher request
- **WHEN** a voucher removal request has an unsupported type, a missing salary detail or no link of that type
- **THEN** the system rejects it without deleting any voucher or changing any salary detail

### Requirement: Live payroll voucher deduplication
Payroll voucher generation MUST reject a new accrual or payment voucher when any salary detail for the same book, employee and belonging month links to a non-deleted voucher of the requested link type. Missing or deleted voucher references MUST NOT hide another valid reference. Historical deleted salary details SHALL continue to participate in this duplicate check. Existing link-type grouping, voucher status handling and generation rules SHALL remain unchanged. A duplicate rejection MUST preserve salary values and all existing links and MUST NOT create a voucher. When no valid reference exists, the existing generation workflow SHALL remain available and only the current salary detail's own stale link of the requested type may be cleared.

#### Scenario: Current stale link with live peer
- **WHEN** the current salary detail references a missing or deleted voucher and another detail in the same book, employee and month links to a non-deleted voucher of that type
- **THEN** generation is rejected without clearing either link or creating a voucher

#### Scenario: Newer stale peer with older live peer
- **WHEN** the current detail has no requested link and the newest linked historical detail references a missing or deleted voucher while an older matching detail links to a non-deleted voucher
- **THEN** generation is rejected regardless of the stale detail's creation order

#### Scenario: Live voucher from deleted salary history
- **WHEN** a deleted historical salary detail for the same book, employee and month links to a non-deleted voucher of the requested type
- **THEN** generation is rejected even though that salary detail is deleted

#### Scenario: Only stale references remain
- **WHEN** all matching references of the requested type point to missing or deleted vouchers
- **THEN** the existing generation workflow remains available and stale references on other salary details are preserved

#### Scenario: Duplicate scope remains isolated
- **WHEN** a live reference exists only for another book, employee, belonging month or link type
- **THEN** that reference does not block the requested type's existing generation workflow

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
