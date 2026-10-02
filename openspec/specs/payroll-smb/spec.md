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
