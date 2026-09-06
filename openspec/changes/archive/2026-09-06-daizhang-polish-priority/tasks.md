## 1. Schema and journal checkout snapshot

- [x] 1.1 Add nullable `prev_opening_balance` to `journal_account` (init/patch SQL) and verify column exists on a fresh or migrated DB
- [x] 1.2 Change journal checkout to set `prev_opening_balance = opening_balance` then `opening_balance = balance`; verify with a unit/integration assertion on one account before/after checkout
- [x] 1.3 Document that periods closed before this migration cannot uncheckout; verify API returns the dedicated error when `prev_opening_balance` is null

## 2. Uncheckout service and API

- [x] 2.1 Implement guard validation (adjacent month, settlement exists, no vouchers in C, no journal entries in C, journal snapshot present) and verify each rejection path returns a clear message with zero DB changes
- [x] 2.2 Implement transactional reverse of subject-balance rows for C, journal opening restore, IS/BS snapshot cleanup for T (+ conditional Q/Y), logic-delete settlement T, `currentTerm := T`; verify with service-level tests
- [x] 2.3 Expose `POST /api/settlement/uncheckout` (bookId from current user; yearPeriod optional but must match T) and verify happy path + rejection via API/smoke
- [x] 2.4 Add structured success/failure logging (bookId, userId, periods, reason) and verify log lines appear on both outcomes

## 3. Frontend

- [x] 3.1 Add settlement API client for uncheckout and verify request shape against backend
- [x] 3.2 Show uncheckout only on the eligible closed month in settlement list; verify other rows have no action
  - **Note (2026-09-02):** `a78dbf6` UI restore overwrote `settle-list.vue` and dropped the button; re-added on restored list (eligible row only + typed `YYYY-MM` confirm).
- [x] 3.3 Require typing `YYYY-MM` in confirm dialog before submit; on success refresh book current term and show toast; verify manually or with UI/e2e

## 4. E2E / acceptance (P0 gate)

- [x] 4.1 E2E: close period → uncheckout → current term back; C subject balances gone; T vouchers unchanged
- [x] 4.2 E2E: after uncheckout, unpost/adjust → carry-forward → checkout again → three statements reconcile (BS/IS/CF checks as in existing report specs)
- [x] 4.3 E2E: reject when C has a voucher; reject when requesting non-adjacent period; data unchanged
- [x] 4.4 Manual or automated check: small-enterprise sample book 3103/3104 (and alias 4103/4104) equity reconciliation after the re-close path

## 5. Follow-ups tracked (not blocking P0 ship)

- [x] 5.1 P0.6 spike: reduce carry-forward dependence on enterprise template codes for 小企业 books; verify with a 小企业-only fixture
- [x] 5.2 P1 backlog note: voucher/report print and 账本包 export (no implementation required in this change)
- [x] 5.3 P2 backlog note: bank reconciliation, ARAP write-off/aging, attachments, tax accrue wizard
- [x] 5.4 P1.5: align settlement self-check copy with Non-goals (external checklist items labeled as external)
