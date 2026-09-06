## Context

See proposal.md for motivation. Today `JournalEntryService` applies balance only on save/delete; `update` copies fields only. `generateVoucher` writes both voucher lines with `bookId` as subject placeholders and uses "today" for period checks and voucher date. List UI binds `providerName` while `JournalEntryPageDto` expects `remark`/`accName`, and `JournalEntryMapper.xml` `pageList` ignores `remark` and sorts by `created_date`.

Constraints: keep existing REST shapes (`/api/journal/entry/*`); stay book-scoped; reuse `settlementService.check` and `BookSubject` lookup patterns already used by voucher save; no new tables.

## Goals / Non-Goals

**Goals:**

- Make update a first-class balance operation (B2: reverse + apply + recalculate later running balances).
- Make generate-voucher produce draft lines that voucher save can resolve to real subjects.
- Align period guards and voucher dating with `tradeDate`.
- Make list remark/account filters and sort match professional diary expectations.

**Non-Goals:**

- Bank reconciliation, transfers, ARAP linkage, reverse sync from voucher edits, batch generate, import/export, inventory count.
- Changing settlement checkout / `prev_opening_balance` behavior.
- Dropping stored running `balance` in favor of fully computed-on-read (possible later; out of this change).

## Decisions

### 1. B2 update algorithm (chosen over B1 lock)

**Choice:** On amount/direction/account change: load old entry → settlement-check old and new trade-date periods → reject if `voucherId` set for money fields → reverse old effect on old account → apply new effect on new account (insufficient-balance guard) → persist entry → recompute running balances for all entries of each touched account from the earlier of (old trade date, new trade date) forward, ordered by `trade_date ASC, id ASC`, starting from the prior entry's balance (or account opening semantics consistent with current create path).

**Alternatives:** B1 (forbid edit when later entries exist) — simpler but poor UX for SMB typos; full ledger rewrite without stored balance — larger schema/UI change.

**Detail:** Remark/description-only edits skip reverse/apply/recalc. Changing only `tradeDate` still triggers recalc for that account and dual period checks.

### 2. Subject mapping for generate-voucher

**Choice:**

| Direction | Debit | Credit |
|-----------|-------|--------|
| income `i` | `journal_account.subjectId` | `journal_entry.subjectId` |
| expenditure `e` | `journal_entry.subjectId` | `journal_account.subjectId` |
| opening `o` | refuse | refuse |

Resolve each id via book subject service to set `subjectId` / `subjectCode` / `subjectName` on `VoucherItemChangeDto`. Missing/blank/unknown → business error, no voucher, no link.

**Alternatives:** Keep placeholders for accountant to edit — rejected (credibility goal); single-subject templates — rejected (UI already collects counterpart subject).

### 3. Voucher date = trade date

**Choice:** `voucherDate` / year / month and `settlementService.check` all use entry `tradeDate`.

**Alternative:** Generate-today for voucher date while checking tradeDate period — confusing for month-end catch-up; rejected.

### 4. List filter and sort

**Choice:** Fix UI to bind `remark` (and keep/use `accName` if exposed); extend mapper `WHERE` for `remark LIKE`; order by `trade_date DESC, id DESC`. Optional: add `accId` filter later if UI adds account dropdown — not required for MVP if remark + accName cover the bug.

**Alternative:** Only fix frontend field name without mapper remark — insufficient (`remark` currently unused in SQL).

### 5. Delete / partial failure messaging

**Choice:** Keep reverse-on-delete; if some ids blocked by closed period, only delete eligible ones and return a message that distinguishes partial success when applicable (minimal clarity improvement within same endpoint).

## Risks / Trade-offs

- **[Risk] Concurrent updates on same account** → Mitigation: rely on existing DB transaction on update/recalc; accept last-write-wins for SMB concurrency (same as today).
- **[Risk] Historical bad data already drifted** → Mitigation: recalc on next update of that account repairs forward from edit point; optional one-off repair tool is out of scope (document for ops if needed).
- **[Risk] Opening-balance `o` interact with recalc** → Mitigation: treat `o` like income for balance math as create does today; still forbid voucher generation.
- **[Risk] Voucher save validation stricter than draft placeholders** → Mitigation: fill code/name from BookSubject; keep status DRAFT so accountant can adjust auxiliaries before submit.

## Migration Plan

- Deploy backend + frontend together; no schema migration.
- No forced data backfill; balances heal when users edit or when a future optional repair is run.
- Rollback: revert services/mapper/UI; drafts already generated with correct subjects remain valid vouchers.

## Open Questions

- None blocking; optional `accId` date-range filters can wait until after apply if product asks.
