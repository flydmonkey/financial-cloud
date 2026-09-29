# 吞吐 V2 · W2–W4 Implementation Plan

> **For agentic workers:** execute task-by-task; checkbox tracking optional once commits land.

**Goal:** Finish throughput V2: journal reverse sync on 红冲, purchase voucher messaging, ban depreciated bump_qty, overdue hard-block config, program closeout.

**Decisions:**
- W2-1: create offsetting journal entries linked to reverse voucher (keep original links).
- W3-2: skip (ban) bump_qty when card has existing depreciation.
- W4-1: `settlement.verify.arap.overdue.hard` default `false`.
- W2-2: skip (可裁).

---

## Tasks

- [x] W2-1 `JournalEntryService.createReversalEntriesForVoucher` + `VoucherService.reverseById` hook + unit test
- [x] W3-1 `FixedAssetPurchaseRules.skipVoucherReason` + create message + unit test
- [x] W3-2 ban bump when depreciated (preview warning + bookSurplus skip) + unit test
- [x] W4-1 config key + SettlementService hardFail + VoucherSettlementParams API/UI + SQL seed/patch + tests
- [x] W4-2 docs: product 06/07/08, gap 5.2, backlog complete, design status, openspec month-end overdue clause

## Acceptance

| Slice | Evidence |
|-------|----------|
| W2-1 | `JournalEntryServiceTest#createReversalEntriesForVoucher_flipsIncomeToExpenditure` |
| W3-1 | `FixedAssetPurchaseRulesTest#skipVoucherReason_whenZeroCredit` |
| W3-2 | `FixedAssetCheckServiceTest#bookSurplus_skipsBumpWhenCardHasDepreciation` |
| W4-1 | `SettlementServiceTest#verify_arapOverdueHardFailsWhenEnabled`；UI `voucher-settlement.vue` |
| W4-2 | backlog「吞吐 V2 已完成」；分册同步 |
