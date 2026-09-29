# month-end-close Specification

## Purpose

Provide a professional monthly period-close (月结) flow: ordered checklist, hard system gates before checkout, period lock after close, and no separate year-end closing entry—aligned with Kingdee-style month-end only.

## Requirements

### Requirement: Unified month-end close wizard
The system SHALL present month-end close as a single guided wizard for the book's current open term, with ordered steps: **manual acknowledgment → voucher preparation (posting + successive) → accrue/carry-forward → system verification → checkout**. The system MUST NOT expose a separate “年终结账 / year-end close” entry point; December year-end carry items SHALL appear only as that month's carry-forward templates or checklist items.

#### Scenario: Open wizard on current term
- **WHEN** a user with settlement permission opens month-end close
- **THEN** the wizard SHALL show the book's current open year-period
- **AND** SHALL offer the five ordered steps for that period only

#### Scenario: No separate year-end entry
- **WHEN** the user browses settlement / period-close navigation
- **THEN** the system MUST NOT offer a distinct year-end closing workflow or menu item independent of monthly close

#### Scenario: Cannot skip ahead past failed hard gates
- **WHEN** any hard gate for an earlier step has failed
- **THEN** the system SHALL prevent advancing to checkout
- **AND** SHALL keep the checkout action disabled or rejected until those gates pass

#### Scenario: Step navigation gated by readiness
- **WHEN** the user attempts to advance from a wizard step
- **THEN** the system SHALL enable “下一步” only when that step's readiness rules pass
- **AND** SHALL NOT allow skipping voucher preparation or accrue/carry completion by jumping directly to checkout

### Requirement: Accrue and carry actions reachable from the wizard
The month-end wizard SHALL let the user complete fixed-asset depreciation accrual and required carry-forward voucher **generation and posting** for the current term without leaving the overall month-end close context. Required carry-forward completion in the wizard SHALL mean the carry voucher exists **and is posted** (consistent with the unposted-voucher hard gate).

#### Scenario: Carry-forward status visible in wizard
- **WHEN** the user is on the accrue/carry step
- **THEN** the system SHALL show which required carry-forward items are missing, generated-but-unposted, or posted for the current term

#### Scenario: After generating and posting carry-forward, status refreshes
- **WHEN** the user generates and posts a required carry-forward voucher from the wizard flow
- **THEN** the corresponding checklist/verify status SHALL update to reflect posted completion without requiring a full app reload to see the change

### Requirement: Hard gates before checkout
Before allowing checkout of the current open term, the system SHALL evaluate hard verification items and MUST refuse checkout when any hard item fails. Hard items MUST include at least:

1. No unposted vouchers remain in the current open term (including generated-but-unposted required carry-forward vouchers).
2. Voucher word-number continuity for the current term passes.
3. Period debit totals equal credit totals for voucher amounts in scope of the check.
4. Required period carry-forward vouchers for the current term have been **generated** per required templates (`qm_jz_sr`, `qm_jz_cbfy`, and December `qm_jz_bnlr` when applicable).
5. Fixed-asset depreciation for the current term has been accrued when the book has assets that require depreciation in that term; if no such assets exist, this item SHALL pass as not applicable.

The **wizard accrue/carry step** completion rule is stricter than verify item 4 alone: each required carry SHALL count as done in the wizard only when its carry voucher is **generated and posted**. Posting of required carries MAY be enforced jointly by verify item 1 (unposted vouchers) and the wizard step gate; verify item 4 SHALL NOT treat “generated but unposted” as sufficient for wizard step completion.

#### Scenario: Generated-but-unposted carry blocks wizard advance
- **WHEN** a required carry-forward voucher exists but is not posted
- **AND** the user is on the accrue/carry wizard step
- **THEN** the wizard SHALL treat that carry as incomplete
- **AND** SHALL disable advancing until the carry voucher is posted

#### Scenario: Unposted carry blocks checkout via unposted gate
- **WHEN** a required carry-forward voucher exists but is not posted
- **AND** the user attempts checkout
- **THEN** checkout SHALL be rejected because the unposted-voucher hard item fails
- **AND** verify MAY still report the carry-forward item as generated

#### Scenario: Missing required carry-forward blocks checkout
- **WHEN** a required carry-forward template for the current term has no generated carry-forward voucher
- **AND** the user attempts checkout
- **THEN** the system SHALL reject checkout
- **AND** verification results SHALL identify the missing carry-forward item

#### Scenario: Unposted voucher blocks checkout
- **WHEN** at least one voucher in the current open term is not posted
- **AND** the user attempts checkout
- **THEN** the system SHALL reject checkout
- **AND** verification results SHALL mark the unposted-voucher item as failed

#### Scenario: Depreciation accrued or not applicable
- **WHEN** the book has no assets requiring depreciation in the current term
- **THEN** the depreciation hard item SHALL pass as not applicable
- **WHEN** such assets exist and depreciation for the term has not been accrued
- **THEN** checkout SHALL be rejected until accrual completes

#### Scenario: All hard gates pass
- **WHEN** every hard verification item passes for the current open term
- **THEN** the system SHALL allow checkout to proceed (subject to existing checkout business rules such as not already settled)

### Requirement: Voucher preparation step before accrue/carry
The month-end wizard SHALL include a dedicated **voucher preparation** step before accrue/carry-forward. That step SHALL list current-term vouchers that block close because they are not posted, SHALL allow submit/audit/post actions in-flow (batch where APIs allow), and SHALL run successive (断号) check with one-click fix. The user MUST NOT advance past this step until no blocking unposted vouchers remain and successive check passes.

#### Scenario: Unposted list blocks advance
- **WHEN** at least one voucher in the current open term is not posted
- **AND** the user is on the voucher preparation step
- **THEN** the wizard SHALL list those vouchers
- **AND** SHALL disable advancing to accrue/carry until they are posted or otherwise resolved per product rules

#### Scenario: Successive gaps block advance
- **WHEN** successive check reports gaps for the current term
- **AND** the user is on the voucher preparation step
- **THEN** the wizard SHALL show the gaps
- **AND** SHALL disable advancing until successive check passes or the user applies the one-click fix successfully

#### Scenario: Voucher prep passes
- **WHEN** no blocking unposted vouchers remain
- **AND** successive check passes
- **THEN** the wizard SHALL allow advancing to accrue/carry

### Requirement: Cost carry includes main business cost subjects
The required carry-forward template `qm_jz_cbfy` SHALL be described and implemented to include **主营业务成本 (main business cost)** under both accounting standards: subject `5401` (小企业会计准则) and alias `6401` (企业会计准则). Only subjects with non-zero balances SHALL contribute carry lines. The wizard UI SHALL label this template as cost/expense carry **including main business cost**.

#### Scenario: 6401 alias resolves to 5401 for carry
- **WHEN** the book uses enterprise subject code `6401` for main business cost
- **AND** `qm_jz_cbfy` carry is generated
- **THEN** carry line selection SHALL include the `5401` alias mapping per `SubjectCodeCompat`
- **AND** non-zero balances on the resolved subject SHALL appear in the carry voucher

#### Scenario: Wizard labels cbfy clearly
- **WHEN** the user views required carry-forward rows in the accrue/carry step
- **THEN** the `qm_jz_cbfy` row SHALL display copy indicating it includes main business cost (5401/6401)

### Requirement: Verify failures surface in-wizard with navigation
When month-end verify returns business failures (including `code≠0` with verify row payload), the wizard SHALL render results inline in the verification step table or alerts. Failed hard items SHALL expose an action to navigate to the owning wizard step (e.g. unposted → voucher preparation; missing/unposted carry or depreciation → accrue/carry; successive → voucher preparation). Global error toast MUST NOT be the sole feedback for expected verify failures on the month-end page.

#### Scenario: Verify table is primary feedback
- **WHEN** verify returns one or more failed hard items for the current term
- **THEN** the wizard SHALL show the full verify result table inline
- **AND** SHALL NOT rely solely on a global interceptor toast to communicate those failures

#### Scenario: Jump back to owning step
- **WHEN** a verify row fails for unposted vouchers, successive gaps, missing/unposted carry-forward, or missing depreciation
- **THEN** the verification step SHALL offer navigation to the wizard step that owns remediation
- **AND** after the user fixes the issue and re-runs verify, hard gates SHALL reflect the updated state

#### Scenario: Silent verify on month-end page
- **WHEN** the month-end wizard calls the verify API
- **THEN** the client MAY request silent error handling so expected business failures return payload without a blocking global toast
- **AND** the wizard SHALL still present the failure details inline

### Requirement: Manual confirmation items are non-blocking for system verify
Items that the product still cannot system-check—including bank reconciliation, inventory stocktake, and tax-filing cross-checks—SHALL be presented as manual confirmation (人工确认) or explicitly labeled “本期不系统检”. AR/AP balances and aging MUST NOT use “系统暂无 / 本期不系统检” placeholder wording; they are covered by the system AR/AP month-end check. Completing checkout MUST NOT require a write-off (核销) module to exist.

#### Scenario: 往来 placeholder does not fail verify
- **WHEN** hard gates otherwise pass
- **AND** AR/AP assist data may be empty or non-empty
- **THEN** system verification SHALL still be allowed to succeed with respect to 往来
- **AND** the UI MUST NOT show 往来 as “系统暂无核销/账龄” or “本期不系统检” placeholder
- **AND** 往来 SHALL appear as a system-computed verify summary (see requirement Month-end verify includes AR/AP and aging summary)

#### Scenario: User acknowledges manual checklist
- **WHEN** the wizard shows remaining manual confirmation items (bank, inventory, tax cross-checks, etc.)
- **THEN** the user MUST be able to acknowledge them in the UI before checkout
- **AND** acknowledgment MUST NOT be confused with a passed hard system gate in the verify API results

#### Scenario: Bank and inventory remain manual
- **WHEN** hard gates otherwise pass
- **AND** bank reconciliation / inventory modules are not implemented
- **THEN** system verification SHALL still be allowed to succeed
- **AND** the UI SHALL show those rows as manual / not system-checked

### Requirement: Month-end verify includes AR/AP and aging summary
Month-end verification SHALL include a system-computed summary for the current open term that reports accounts-receivable and accounts-payable totals (and overdue aging totals when aging data exists) using the same data sources as `arap-assist` / `arap-writeoff` open-item aging when write-offs exist. The summary item MUST be marked as a system check (not a placeholder). By default (`settlement.verify.arap.overdue.hard` unset or `false`), overdue aging alone MUST NOT cause a hard-gate checkout failure; it MAY set warning on the verify item. When the book config `settlement.verify.arap.overdue.hard` is `true` and overdue amounts exist, the AR/AP verify item SHALL hard-fail. Absence of any AR/AP activity SHALL pass as applicable with zero totals (or N/A only when the book has no receivable/payable subjects configured—prefer zero totals).

#### Scenario: Verify surfaces AR/AP totals from real queries
- **WHEN** the user runs month-end verify for the current open term
- **THEN** verification results SHALL include an AR/AP (往来) system item
- **AND** that item’s reason or payload SHALL reflect queried balance totals (including zero)
- **AND** the UI MUST NOT label that item as “系统暂无核销/账龄” or “本期不系统检”

#### Scenario: Overdue aging warns without hard fail (default)
- **WHEN** aging shows overdue AR or AP amounts as of the term end
- **AND** overdue hard-block config is off (default)
- **AND** all hard gates otherwise pass
- **THEN** the AR/AP verify item MAY be marked warning
- **AND** checkout MUST still be allowed with respect to this item alone

#### Scenario: Overdue hard-fails when configured
- **WHEN** aging shows overdue AR or AP amounts as of the term end
- **AND** book config `settlement.verify.arap.overdue.hard` is `true`
- **THEN** the AR/AP verify item SHALL fail as a hard gate
- **AND** checkout MUST be blocked until overdue is cleared or the switch is turned off

#### Scenario: No counterparts still allows verify success
- **WHEN** the book has no customer/supplier AR/AP auxiliary balances
- **THEN** the AR/AP verify item SHALL pass
- **AND** MUST NOT fail solely due to empty counterpart lists

#### Scenario: Overdue uses open-item aging when write-offs exist
- **WHEN** write-off data exists for the book
- **AND** month-end verify computes overdue totals
- **THEN** overdue amounts SHALL be consistent with open-item aging for the term end
- **AND** with default config, overdue alone still MUST NOT hard-fail checkout

### Requirement: Checkout still snapshots and advances the term
On successful checkout, the system SHALL retain existing close side effects: persist settlement for the closed term, write period statement/balance snapshots as today, run journal-account checkout balance rollover, and advance the book's current open term to the next month. Uncheckout behavior and guards from the existing settlement-uncheckout capability SHALL remain in force.

#### Scenario: Successful month-end checkout
- **WHEN** all hard gates pass
- **AND** the user confirms checkout for the current open term T
- **THEN** the system SHALL record T as settled
- **AND** SHALL advance the current open term to the month after T
- **AND** SHALL produce the same class of checkout snapshots and journal opening updates as the current checkout path

### Requirement: Closed periods reject new and mutating voucher writes
After a term is settled (or for any voucher period strictly before the book's current open term), the system MUST reject creating a new voucher dated in a closed/non-open period, and MUST reject mutating operations that would alter vouchers in non-open periods, consistent with period-lock intent. The create/save path for new vouchers MUST enforce this (closing the known gap where only some mutation paths were guarded).

#### Scenario: New voucher in closed period rejected
- **WHEN** the book's current open term is T
- **AND** the user attempts to save a new voucher whose period is before T
- **THEN** the system SHALL reject the save with a clear period-lock error
- **AND** no new voucher row SHALL be persisted

#### Scenario: Open period voucher still editable under existing rules
- **WHEN** the user saves or mutates a voucher in the current open term
- **AND** other existing voucher rules (status, permissions) allow the operation
- **THEN** the period-lock rule SHALL NOT block solely because of period lock
