## MODIFIED Requirements

### Requirement: Month-end verify includes AR/AP and aging summary
Month-end verification SHALL include a system-computed summary for the current open term that reports accounts-receivable and accounts-payable totals (and overdue aging totals when aging data exists) using the same data sources as `arap-assist` / `arap-writeoff` open-item aging when write-offs exist. The summary item MUST be marked as a system check (not a placeholder). Overdue aging alone MUST NOT cause a hard-gate checkout failure; it MAY set warning on the verify item. Absence of any AR/AP activity SHALL pass as applicable with zero totals (or N/A only when the book has no receivable/payable subjects configured—prefer zero totals).

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

#### Scenario: Overdue uses open-item aging when write-offs exist
- **WHEN** write-off data exists for the book
- **AND** month-end verify computes overdue totals
- **THEN** overdue amounts SHALL be consistent with open-item aging for the term end
- **AND** overdue alone still MUST NOT hard-fail checkout
