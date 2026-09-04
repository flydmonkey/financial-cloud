## 1. Persistence & domain

- [x] 1.1 Add write-off header/line schema (book, counterpart, side AR/AP, amounts, status) referencing voucher item ids; verify migration/seed applies cleanly
- [x] 1.2 Implement match service: same-counterpart validation, partial amounts, remaining open calc; verify unit tests for full/partial/cross-counterpart reject
- [x] 1.3 Implement open-item query as-of date; verify remainders after matches
- [x] 1.4 Implement reverse with open-period guards; verify restore and blocked cases

## 2. Aging & month-end

- [x] 2.1 Switch aging to open-item layers when write-offs exist, else FIFO + method label; verify bucket sum = open ending
- [x] 2.2 Wire month-end AR/AP overdue summary to open-item aging when applicable; verify warning-only still holds

## 3. API & UI

- [x] 3.1 Expose REST under `/api/arap/writeoff/*` (open items, suggest, confirm, reverse, list); verify auth + validation errors
- [x] 3.2 Add 核销工作台 page + menu seed; verify manual match and suggestion confirm flows
- [x] 3.3 Surface aging method label on aging UI; verify copy for OPEN_ITEM vs FIFO_ESTIMATE

## 4. Docs & regression

- [x] 4.1 E2E/API: partial write-off, reverse, aging reconcile, month-end verify overdue warning
- [x] 4.2 Update `docs/product/11-arap.md`, gap, settlement for L3 shipped vs remaining non-goals
