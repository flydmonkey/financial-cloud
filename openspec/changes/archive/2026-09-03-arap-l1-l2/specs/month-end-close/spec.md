## MODIFIED Requirements

### Requirement: Manual confirmation items are non-blocking for system verify
Items that the product still cannot system-check—including bank reconciliation, inventory stocktake, and tax-filing cross-checks—SHALL be presented as manual confirmation (人工确认) or explicitly labeled “本期不系统检”. AR/AP balances and aging MUST NOT use “系统暂无 / 本期不系统检” placeholder wording; they are covered by the system AR/AP month-end check. Completing checkout MUST NOT require a write-off (核销) module to exist.

#### Scenario: 往来 placeholder does not fail verify
- **WHEN** hard gates otherwise pass
- **AND** AR/AP assist data may be empty or non-empty
- **THEN** system verification SHALL still be allowed to succeed with respect to 往来
- **AND** the UI MUST NOT show 往来 as “系统暂无核销/账龄” or “本期不系统检” placeholder
- **AND** 往来 SHALL appear as a system-computed verify summary (see ADDED AR/AP month-end requirement)

#### Scenario: User acknowledges manual checklist
- **WHEN** the wizard shows remaining manual confirmation items (bank, inventory, tax cross-checks, etc.)
- **THEN** the user MUST be able to acknowledge them in the UI before checkout
- **AND** acknowledgment MUST NOT be confused with a passed hard system gate in the verify API results

#### Scenario: Bank and inventory remain manual
- **WHEN** hard gates otherwise pass
- **AND** bank reconciliation / inventory modules are not implemented
- **THEN** system verification SHALL still be allowed to succeed
- **AND** the UI SHALL show those rows as manual / not system-checked

## ADDED Requirements

### Requirement: Month-end verify includes AR/AP and aging summary
Month-end verification SHALL include a system-computed summary for the current open term that reports accounts-receivable and accounts-payable totals (and overdue aging totals when aging data exists) using the same data sources as `arap-assist`. The summary item MUST be marked as a system check (not a placeholder). Overdue aging alone MUST NOT cause a hard-gate checkout failure; it MAY set warning on the verify item. Absence of any AR/AP activity SHALL pass as applicable with zero totals (or N/A only when the book has no receivable/payable subjects configured—prefer zero totals).

#### Scenario: Verify surfaces AR/AP totals from real queries
- **WHEN** the user runs month-end verify for the current open term
- **THEN** verification results SHALL include an AR/AP (往来) system item
- **AND** that item’s reason or payload SHALL reflect queried balance totals (including zero)
- **AND** the UI MUST NOT label that item as “系统暂无核销/账龄” or “本期不系统检”

#### Scenario: Overdue aging warns without hard fail
- **WHEN** aging shows overdue AR or AP amounts as of the term end
- **AND** all hard gates otherwise pass
- **THEN** the AR/AP verify item MAY be marked warning
- **AND** checkout MUST still be allowed with respect to this item alone

#### Scenario: No counterparts still allows verify success
- **WHEN** the book has no customer/supplier AR/AP auxiliary balances
- **THEN** the AR/AP verify item SHALL pass
- **AND** MUST NOT fail solely due to empty counterpart lists
