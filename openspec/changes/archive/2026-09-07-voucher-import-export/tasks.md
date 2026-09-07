## 1. Shared template & full export

- [x] 1.1 Align `template-voucher.xlsx` (or shared sibling) column headers for round-trip: date, word head/num, receipt/remark as applicable, line summary, subject code, debit, credit — verify header row matches the import parser contract documented in design
- [x] 1.2 Change `VoucherService.export` to load all filter-matched vouchers (not current page only) and write via the shared template — verify exporting with pageSize=1 still returns all filtered vouchers
- [x] 1.3 Add `GET /api/voucher/import-template` that downloads the empty shared workbook — verify response is xlsx and columns match export headers

## 2. Import API & persistence

- [x] 2.1 Add import result VO (successCount, failureCount, failure reasons) modeled after fixed-asset import — verify DTO compiles and serializes in a controller smoke call
- [x] 2.2 Implement Excel parse + group-by-voucher in `VoucherService`; validate balance, subject code in book, open period — verify unit/service tests reject unbalanced, unknown subject, and closed-period groups
- [x] 2.3 Persist valid groups via draft-create path only (status draft; ignore id; clear audit/sender/manager) — verify created rows are draft and balances unchanged
- [x] 2.4 Apply word rule: reuse non-conflicting wordHead+wordNum, else `able-word-num` — verify conflict and empty cases auto-assign; free number is kept
- [x] 2.5 Add `POST /api/voucher/import` with `ExcelImport` / `excelFile`; partial success isolation per group — verify mixed file returns both success and failure counts without rolling back successes
- [x] 2.6 Enforce write-business / same auth class as export+write — verify unauthorized caller is rejected

## 3. Frontend list actions

- [x] 3.1 Add `downloadVoucherImportTemplate` and `importVouchers` in `api/voucher/voucher.ts` — verify requests hit `/voucher/import-template` and `/voucher/import`
- [x] 3.2 On `voucher-index.vue`「更多」add 下载模板 and 导入 (ImportUpload); keep 导出 with `queryParams` — verify menu actions visible and template downloads
- [x] 3.3 Show import result (success/fail counts and reasons) and refresh list — verify UI after a mixed import file

## 4. Round-trip & regression checks

- [x] 4.1 Round-trip check: export filtered vouchers → re-import without column edits → new drafts created (word conflict auto-assigns) — verify success count equals exported voucher count when subjects/period still valid
- [x] 4.2 Smoke: existing list filters, audit/post, draft/save on entry workspace still work — verify no regression on sample book
