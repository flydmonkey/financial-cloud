## ADDED Requirements

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
