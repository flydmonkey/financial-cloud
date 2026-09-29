## 1. Backend Excel IO

- [x] 1.1 Add `BookInitBalanceImportResultVo` and controller endpoints `GET /export`, `GET /import-template`, `POST /import`; verify they compile and bind `bookId` from current user
- [x] 1.2 Implement export workbook from `list()` flattening; verify headers and amount columns match design D1
- [x] 1.3 Implement template download with headers (+ optional sample); verify Content-Disposition filename
- [x] 1.4 Implement import: parse rows, skip blank, match code, enforce initialize-task + leaf + !hasVoucher, call `save` for successes; verify failure rows do not block successes and init-locked book rejects entirely

## 2. Frontend

- [x] 2.1 Add API helpers (blob export/template + FormData import) in `bookInitBalance.ts`; verify paths match controller
- [x] 2.2 Add export / template / import controls on `initBalance/index.vue` (import only when `ableEdit`); verify import dialog shows success/fail counts and list refreshes

## 3. Docs & acceptance

- [x] 3.1 Update `docs/product/02-basic-settings.md` opening-balance row for Excel import/export
- [x] 3.2 Smoke: export → edit amounts → import → trial balance; record in acceptance notes
- [x] 3.3 Document unknown-code / locked-init failure paths in acceptance notes (manual smoke; no dedicated unit harness for this service yet)
