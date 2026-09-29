## 1. Cash flow print

- [x] 1.1 Enable the Print control on `cash-flow-statement.vue` and implement `handlePrint` via `openTablePrintWindow` with 项目/行次/本期/本年累计 columns; verify the button is visible and opens a print window whose rows match the loaded list (section-header itemCodes stay blank like on-screen)
- [x] 1.2 Manually or via UI smoke: print after loading a known period and confirm subtitle shows company + period; verify Export still works independently

## 2. Classic path hygiene

- [x] 2.1 Ripgrep `printContentInIframe` / `buildPrintSheets` and related unused print-only helpers in `voucher-edit.vue`; remove only callers that are not on the classic `onPrint` / `mode=print` path; verify editor Print still opens `voucher-print-classic.html` and list batch print still works
- [x] 2.2 Confirm `voucherPrintHtml.ts` has no production imports; delete the module and its unit test (or document deprecated if retention is required) and verify no compile/import errors reference the removed symbols
- [x] 2.3 Grep production entry points (`onPrint`, batch print, `mode=print`) and verify only the classic static page path remains reachable

## 3. Docs sync

- [x] 3.1 Update `docs/product/05-statement.md` print/PDF rows to reflect cash-flow print + browser-save PDF via `tablePrint`; verify the status table no longer says cash-flow print is commented-only
- [x] 3.2 Update `docs/product/03-voucher.md` (and `04-ledger.md` if still claiming PDF/print missing) so classic print + batch print are described as production; verify wording no longer says classic HTML is unused
- [x] 3.3 Light-touch `docs/product/20-gap-analysis.md` / `21-roadmap.md` / `00-overview.md` S1/print paragraphs so they match landed print delivery and this polish; verify no remaining claim that S1 print is wholly unimplemented

## 4. Acceptance

- [x] 4.1 Smoke checklist: cash-flow Print; voucher editor Print; voucher list batch Print — all open expected print UIs without iframe fallback; record pass/fail in the change notes or PR description
- [x] 4.2 Run existing voucher print unit tests (and any remaining after 2.2) and verify they pass
