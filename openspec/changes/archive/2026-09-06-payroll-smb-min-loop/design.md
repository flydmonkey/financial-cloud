## Context

See `proposal.md` for motivation. Existing stack already has:

- `Employee.payBaseRule` / `payBaseNumber`, bank fields, and richer per-insurance rule fields that calculation largely ignores in favor of a unified base.
- `EmployeeSalaryTempService.calculateSalary` applying book default vs employee custom unified base for `NORMAL` employees.
- UI flow split across `calc-salary` (preview/temp), push to `salary-detail`, then voucher actions on detail/summary; no bank payment file export.
- Product docs: `docs/product/09-payroll.md`.

Constraints: stay within HR/salary modules; reuse existing voucher generation; no new external payment gateway; Chinese SMB operators (行政兼财务 / 代账).

## Goals / Non-Goals

**Goals:**

- Make per-employee unified contribution base reliable (validate + visible on preview).
- Collapse the monthly operator path into a guided, step-aware UX on existing pages (minimal new routes).
- Ship a pragmatic bank payment export (CSV/Excel) from confirmed salary rows.

**Non-Goals:**

- Cumulative personal income tax algorithm rewrite.
- Payslips / employee portal.
- Per-bank proprietary file formats (ICBC/CCB-specific layouts) beyond a generic column set.
- Activating unused per-insurance custom bases as first-class product in this change (document as deferred).

## Decisions

### 1. Unified custom base only this period

- **Choice**: Product-support `payBaseRule` + `payBaseNumber` (and book `ConfigInsuranceFund.payBase`). Leave per-险种 `*Rule` fields unused in calculation; document in `09-payroll.md`.
- **Why**: Calculation already implements unified base; enabling per-险种 without UX and tests expands scope beyond the SMB min loop.
- **Alternatives**: Wire every per-insurance rule now — rejected for scope; hide unused fields in UI later if confusing.

### 2. Guided path = step strip on calc-salary (+ deep links), not a separate wizard app

- **Choice**: Add an in-page step indicator and primary CTAs on `calc-salary` / related salary pages: (1) 选月并生成预览 (2) 调整并推送明细 (3) 生成凭证 (4) 导出代发盘. Reuse existing APIs.
- **Why**: Lowest change cost; matches current data model (temp → detail → voucher).
- **Alternatives**: Full five-step wizard like month-end — deferred; overkill for payroll volume.

### 3. Bank payment file = generic CSV/Excel from salary details

- **Choice**: New export API (or extend existing salary export) filtered to payment columns: 姓名、开户行、账号、实发金额、所属月、工号(optional). Default format Excel or CSV with UTF-8 BOM for Excel compatibility.
- **Missing accounts**: **Block export** with employee list (safer for SMB paying wrong people) — operator must fill `bankCardNo` first. Soft-omit is rejected as silent risk.
- **Source of truth**: Confirmed `employee_salary` (post-push) for the belonging month, not temp preview.
- **Alternatives**: Bank-specific templates — later; payslip PDF — explicitly out of scope (option B chose 代发盘).

### 4. Validation of incomplete custom base at calculate time

- **Choice**: Fail the calculate request (or fail specific employees with a batch error summary) when `payBaseRule == 1` and base ≤ 0 / null for `NORMAL` employees.
- **Why**: Prevents zero SI silently underpaying withholdings.
- **Alternatives**: Auto-fallback to book default — rejected; masks misconfiguration.

## Risks / Trade-offs

- **[Risk] Operators confuse preview vs confirmed detail for 代发盘** → Mitigation: export only from confirmed details; CTA appears at step 4 after push.
- **[Risk] Generic file rejected by some online banking UIs** → Mitigation: document column mapping; allow Excel open/re-save; proprietary formats later.
- **[Risk] Unused per-险种 fields confuse power users** → Mitigation: docs + optional UI note “本期按统一基数计算”.
- **[Risk] Personal tax still not cumulative withholding** → Mitigation: called out as known non-goal; separate change if needed.
- **[Trade-off] Guided path without hard backend state machine** → Steps are UX + soft guards; concurrent edits still possible — acceptable for SMB concurrency.

## Migration Plan

- No schema migration required if existing columns suffice.
- Deploy backend validation + export first; then frontend step strip and export button.
- Rollback: feature is additive (export + stricter validation); revert validation only if it blocks existing tenants who relied on empty custom bases (communicate in release notes).

## Open Questions

- Exact column header wording for the payment file (中文表头定稿可在实现时与代账习惯对齐，不改变“必含姓名/账号/实发”的规格).
- Whether export lives under `/api/employee/salary/export-payment` vs query flag on existing export — implementation detail, same behavior.
