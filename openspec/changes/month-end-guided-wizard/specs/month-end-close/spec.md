## MODIFIED Requirements

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

## ADDED Requirements

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
