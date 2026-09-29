## Context

See proposal.md for motivation. Classic voucher print (`public/voucher-print-classic.html` + localStorage payload) and `tablePrint.ts` already cover most delivery surfaces. Remaining work is UI wiring, dead-code removal, and doc sync—no new backend services.

## Goals / Non-Goals

**Goals:**
- Wire cash-flow print to the existing `openTablePrintWindow` pattern used by balance sheet / income statement.
- Remove or neutralize unused iframe / over-page print code in `voucher-edit.vue` without changing classic print behavior.
- Resolve `voucherPrintHtml.ts` orphan status (delete with tests, or mark deprecated if still useful as reference—prefer delete if only tests import it).
- Update product docs so S1 print status matches code.

**Non-Goals:**
- Server-side PDF generation or new print engines.
- Redesigning classic voucher layout or changing page size rules.
- Adding print to every remaining obscure page beyond cash flow (unless trivially already patterned and listed in tasks as optional).
- Changing books-pack ZIP contents.

## Decisions

### D1: Reuse `tablePrint` for cash flow (not a new template)
- **Choice**: Uncomment/add Print button; build a simple HTML table from `cashFlowStatementList` (项目 / 行次 / 本期金额列 / 本年累计), call `openTablePrintWindow`.
- **Why**: Matches balance sheet / income statement; zero new deps; browser "Save as PDF" already accepted product-wide.
- **Alternative considered**: Server PDF → out of scope (user chose polish, not server PDF).

### D2: Cash flow column labels
- **Choice**: Mirror on-screen labels (`monthlyAmountLabel` for the period column + 「本年累计金额」); skip amount cells for section-header itemCodes the UI already blanks.
- **Why**: Print should look like what the accountant sees, not invent a different report.

### D3: Dead code removal strategy for voucher-edit
- **Choice**: Delete `printContentInIframe` and any helpers only used by it (`buildPrintSheets` / related CSS hooks) once confirmed unused by `onPrint` and `mode=print` (which already redirects to classic HTML). Keep `mode=print` redirect behavior intact.
- **Why**: post-restore audit flagged misleading volume; safer to delete than leave dual paths.
- **Alternative**: Comment-only — rejected; comments drift.

### D4: `voucherPrintHtml.ts`
- **Choice**: Prefer delete file + its unit test if grep shows no production imports (current state: tests only). If kept, add a file-level `@deprecated` note pointing to classic HTML—but deletion is preferred for this change.
- **Why**: Specs require no silent dual generators.

### D5: Docs in the same change
- **Choice**: Patch product docs in the same PR/tasks as code so archive leaves no "print not wired" lies.
- **Files**: at least `docs/product/03-voucher.md`, `05-statement.md`, `04-ledger.md` print/PDF rows; light touch on `20-gap-analysis.md` / `21-roadmap.md` S1 wording and `00-overview` if still claiming print/pack unfinished.

## Risks / Trade-offs

- **[Risk] Deleting `buildPrintSheets` breaks a hidden caller** → Mitigation: ripgrep for symbols before delete; run existing voucher print unit tests / smoke print from editor + list.
- **[Risk] Cash flow print blanks wrong rows** → Mitigation: reuse the same itemCode exclusion lists the table template uses for empty amount cells.
- **[Trade-off] Browser PDF quality varies by client** → Accepted; same as other statements; server PDF remains a later enhancement.

## Migration Plan

1. Ship cash-flow Print button + handler.
2. Remove dead print code and orphan generator in a follow-up commit within the same change.
3. Update docs.
4. Rollback: re-comment cash-flow Print only; dead-code deletion is low risk if classic path untouched.

## Open Questions

None deferrable—scope locked to option 1 (polish, no server PDF).
