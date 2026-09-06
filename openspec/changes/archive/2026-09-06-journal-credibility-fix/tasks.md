## 1. List filter and sort

- [x] 1.1 Fix `journalentry.vue` query binding from `providerName` to `remark` (and keep/use `accName` if exposed); verify the list request payload includes `remark` when searching by 摘要
- [x] 1.2 Update `JournalEntryMapper.xml` `pageList` to filter by `remark` LIKE when provided and order by `trade_date DESC, id DESC`; verify mapper SQL / integration fetch returns only matching rows in trade-date order

## 2. Balance lifecycle (B2)

- [x] 2.1 Implement journal entry `update`: dual period check, reject money-field changes when `voucherId` set, reverse old effect + apply new effect with insufficient-balance guard; verify unit tests for amount change, direction change, and voucher-linked reject
- [x] 2.2 Implement per-account running-balance recalculation from the earliest affected trade date forward (`trade_date ASC, id ASC`); verify a middle-entry amount edit updates later entries' `balance` and the account balance
- [x] 2.3 Harden `delete` messaging for partial closed-period skips if needed; verify mixed open/closed delete behavior is clear and balances only reverse for deleted rows

## 3. Generate voucher subjects and trade date

- [x] 3.1 Rewrite `generateVoucher` to use entry `tradeDate` for settlement check and voucher date/year/month; verify closed trade-date period is rejected and open period uses trade date on the draft
- [x] 3.2 Map income/expenditure debit-credit sides from account fund subject + entry counterpart subject via BookSubject (id/code/name); refuse opening-init, missing subject, and duplicate `voucherId`; verify unit tests cover income, expenditure, and each refuse path

## 4. Verification

- [x] 4.1 Extend `JournalEntryServiceTest` (and/or focused integration tests) for B2 recalc, generate-voucher subjects, and filter/order; verify tests pass
- [x] 4.2 Smoke `financial-cloud-ui/e2e/journal.spec.ts` or manual path: create income/expense, edit middle entry, generate voucher, remark search; verify balances and voucher lines look correct
