## 1. Domain discovery & AR/AP engine

- [x] 1.1 Confirm assist_type codes for 客户/供应商 and AR/AP subject roots via `SubjectCodeCompat` / seed data; record mapping constants and verify against a standard 小企业 book
- [x] 1.2 Implement Arap balance query (posted voucher + auxiliary aggregation) for receivable and payable; verify unit tests for opening/movement/ending on a fixture counterpart
- [x] 1.3 Implement Arap detail ledger with running balance; verify unit/API test filters by book, counterpart, and period
- [x] 1.4 Implement aging FIFO buckets (0–30 / 31–60 / 61–90 / 91–180 / 180+) as-of date; verify bucket sum equals ending balance in unit tests
- [x] 1.5 Implement statement Excel export for one counterpart + range; verify file downloads and contains opening/lines/ending

## 2. API & UI

- [x] 2.1 Expose REST endpoints under `/api/arap/*` (balance, detail, aging, statement export) with book-scoped auth; verify API smoke / permission rejection
- [x] 2.2 Add frontend 往来 pages (应收/应付余额、明细、账龄、对账单导出) and menu routes; verify navigation and list rendering with empty and non-empty data
- [x] 2.3 Wire balance → detail drill-down; verify selecting a row opens filtered detail for that counterpart

## 3. Month-end close integration

- [x] 3.1 Extend `SettlementService.verify` with AR/AP + aging summary item (system check, warning on overdue, not hard-fail); verify unit tests for zero activity pass and overdue sets warning without failing hard gates
- [x] 3.2 Update month-end wizard UI: remove 往来「本期不系统检」placeholder; show system verify row / summary; keep bank/存货/税控 manual; verify copy and checkout still allowed with overdue warning

## 4. Docs & regression

- [x] 4.1 Add or extend E2E/API coverage for balance, aging reconcile, statement export, and month-end verify AR/AP item; verify specs pass against running stack or documented skip if env missing
- [x] 4.2 Update `docs/product` gap/settlement/basic-settings notes for L1+L2 status; verify docs match shipped behavior and state L3 核销仍未做
