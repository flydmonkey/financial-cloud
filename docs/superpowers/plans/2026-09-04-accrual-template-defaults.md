# Accrual Template Defaults by Accounting Standard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When creating a book, empty accrual voucher templates (`jt_zj` / `jt_gz` / `jt_sds` / `jt_fjs`) get default debit/credit subject lines matching the chosen `standardId`.

**Architecture:** Extend `MonthEndCloseRules` with accrual item specs (same `CarryTemplateItemSpec` record). `VoucherTemplateService.insertBookTemplate` already seeds empty required P&L carries; generalize seeding so empty accrual codes also get defaults, with an idempotent `ensure*` pass.

**Tech Stack:** Java 17, Spring, MyBatis-Plus, JUnit 5

**Spec:** `docs/superpowers/specs/2026-09-04-accrual-template-defaults-design.md`

## Global Constraints

- Only four codes: `jt_zj`, `jt_gz`, `jt_sds`, `jt_fjs`
- Never overwrite non-empty template items
- Subject pairs per design table (小企业 `1` vs 企业会计制度 `2`)
- Do not change `SettlementCarryService` amount logic in this plan

---

### Task 1: Rules + unit tests for accrual defaults

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/book/MonthEndCloseRules.java`
- Modify: `financial-cloud/src/test/java/com/financial/cloud/service/book/MonthEndCloseRulesTest.java`

- [x] **Step 1:** Add failing tests asserting both standards for all four codes (2 lines each, correct subjects/directions).
- [x] **Step 2:** Implement `CODE_ACCRUE_*` constants, `isAccrualTemplateCode`, `defaultAccrualTemplateItems`, and fold into `defaultCarryTemplateItems` (or rename helper used by service to `defaultTemplateItems`).
- [x] **Step 3:** Run `mvn -Dtest=MonthEndCloseRulesTest test` — pass.

Default pairs (direction 1=借 2=贷):

| Code | 小企业 | 企业制度 |
|------|--------|----------|
| jt_zj | 5602/1602 | 5502/1502 |
| jt_gz | 5602/2211 | 5502/2151 |
| jt_sds | 5801/2221.05 | 5701/2171.06 |
| jt_fjs | 5403/2221 | 5402/2171 |

---

### Task 2: Seed empties on book template insert

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/service/voucher/VoucherTemplateService.java`

- [x] **Step 1:** When copied items empty and code is accrual (or any code with `defaultCarryTemplateItems` non-empty), call `buildDefaultPnlCarryItems` (rename to `buildDefaultTemplateItems` if cheap).
- [x] **Step 2:** Extend ensure pass to include the four accrual codes (same idempotent count check).
- [x] **Step 3:** Compile module; keep existing P&L behavior.

---

### Task 3: Optional live repair + verify

- [x] Soft-fill empty accrual items for existing books via one-off script or `ensure*` call (optional; only if easy).
- [x] Update design status to implemented.
- [x] Do **not** commit unless user asks.
