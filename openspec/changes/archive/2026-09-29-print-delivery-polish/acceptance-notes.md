# Acceptance notes — print-delivery-polish

Date: 2026-09-29

## Smoke checklist (code-path verification)

| Item | Result | Evidence |
|------|--------|----------|
| Cash-flow Print | PASS | `cash-flow-statement.vue`: Print button + `handlePrint` → `openTablePrintWindow`; subtitle = company + `reportDate`; section header itemCodes blank |
| Export independent | PASS | `handleExport` unchanged beside Print |
| Voucher editor Print | PASS | `onPrint` → `voucher-print-classic.html`; no `printContentInIframe` |
| Voucher list batch Print | PASS | `voucher-index.vue` still opens classic HTML with batch storage key |
| No iframe fallback | PASS | `printContentInIframe` / `buildPrintFrameHtml` / `printSpecificDiv` removed; `voucherPrintHtml.ts` deleted |

## Unit tests

```
node --experimental-strip-types --test financial-cloud-ui/src/utils/voucherPrint.test.ts
```

Result: **8/8 pass** (2026-09-29). `voucherPrintHtml` tests removed with the orphan module.