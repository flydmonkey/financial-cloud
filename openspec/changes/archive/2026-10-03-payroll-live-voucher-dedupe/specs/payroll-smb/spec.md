## ADDED Requirements

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
