# Acceptance notes — opening-balance-excel

Date: 2026-09-29

## Code-path verification

| Item | Result | Evidence |
|------|--------|----------|
| Export | PASS | `GET /api/base/init-balance/export` → XSSF sheet 期初余额, D1 headers |
| Template | PASS | `GET /api/base/init-balance/import-template` + sample row |
| Import guards | PASS | init completed → reject; unknown code / hasVoucher / non-leaf → row errors; successes call `save` |
| UI | PASS | 导出/下载模板/导入 on `initBalance/index.vue`; import result dialog; refresh + trial balance |
| Docs | PASS | `02-basic-settings.md` Excel row; roadmap S1 optional marked in progress |

## Manual UI smoke (recommended on deploy)

1. Open 初始余额 while initialize not locked
2. Export → edit leaf amounts in Excel → Import
3. Confirm list refresh and trial-balance dialog
4. Confirm unknown code row fails without blocking other rows
