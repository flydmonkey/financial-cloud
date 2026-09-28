## Context

See proposal.md for product identity (代账 × 小企业 × 商业化总规划 v1) and why the first slice is「本月账本包」. This design covers **how** to implement `monthly-books-pack` only; S1–S4 roadmap items remain non-implementing notes.

Today the product already exposes per-report Excel exports under statement controllers (subject-balance, balance-sheet, income, cash-flow, voucher-summary, general-ledger) and voucher export. Frontend `plugins/download` can save ZIP blobs. There is **no** single API that assembles a delivery pack. Detail ledgers (明细账) are primarily interactive (`sub-ledger`), so a bulk workbook must be built from that same query/accounting path. A general-ledger export is a different accounting deliverable and cannot be relabeled as a detail ledger.

## Goals / Non-Goals

**Goals:**

- One authenticated API that streams a ZIP for book + `YYYY-MM`
- Assemble pack by reusing existing export generators or their shared data/workbook builders (same templates/numbers as single exports)
- Minimal UI: period + optional voucher-list toggle + download
- Record masterplan phase ordering for later changes without implementing them here

**Non-Goals:**

- Async job queue / pack history store (v1 is request-scoped stream)
- PDF inside the pack (Excel/xlsx sufficient for v1; PDF is S1)
- Per-subject one-file-per-account ZIP explosion unless existing export already works that way
- Custom file naming rules UI or client-uploaded pack templates
- Changing settlement/uncheckout semantics

## Decisions

### D1: Server-side pack assembly (not client fan-out)

- Add `GET` (or `POST` if body needed) `/api/statement/books-pack/export` (name may align with existing `/api/statement/...` style)
- Server builds ZIP in one response; client uses existing blob download helper
- **Alternatives rejected:** browser calling 5–6 export APIs and zipping client-side — fragile CORS/auth, harder consistency, partial failure UX worse

### D2: Reuse existing export writers into temp bytes, then zip

- For template-based reports, extract a shared render-to-file/render-to-stream target from the existing `ExcelExporter` flow. It already supports file output when no `HttpServletResponse` is supplied, so pack generation should reuse that capability rather than introduce duplicate workbook logic.
- For programmatic POI reports such as general ledger, separate workbook construction from HTTP response writing (`buildWorkbook` or `write(OutputStream)`) so both single export and pack assembly use the same builder.
- Build each required member into a request-scoped temporary directory, then add it to a temporary ZIP. Delete all temporary artifacts in `finally`/resource cleanup paths.
- File names inside ZIP: stable Chinese or bilingual labels + period, e.g. `科目余额表_2026-08.xlsx`, `资产负债表_2026-08.xlsx`, …
- **明细账:** generate a dedicated period workbook from the same query/accounting path as `sub-ledger`, covering all applicable subjects and retaining at least date, voucher number, summary, debit, credit, direction, and running balance. A multi-sheet-by-subject workbook is acceptable. `general-ledger/export` MUST NOT be named or supplied as「科目明细账」.
- **凭证清单:** use the voucher-list export/query path and represent vouchers individually for the period. `voucher-summary/export` is an aggregate report and MUST NOT be named or supplied as「凭证清单」.

**Alternatives rejected:** re-query and rebuild Excel with new templates — duplicates formulas and drifts from single export.

### D3: Period default and eligibility

- Request param `yearPeriod=YYYY-MM` required (or default server-side to book `currentTerm`)
- UI default: book's **current term** (working month). User may select another valid period within the book's lifetime.
- Closed vs open: allow export for both open and closed months (delivery often happens after close); do not require settlement row
- Reject invalid `YYYY-MM` and periods outside book life if such bounds already exist elsewhere

### D4: Optional voucher list

- Query/body flag `includeVoucherList` default `true`
- When false, omit that entry only

### D5: UI placement

- Primary entry: settlement period/list delivery area（结账相关页）button「导出本月账本包」
- Secondary (optional if cheap): statement index / subject-balance toolbar — same API
- Confirm dialog: show period + checkbox「含凭证清单」

### D6: Auth and book scope

- The endpoint requires an authenticated user and always derives `bookId` from the authenticated current-book context; a client-supplied book id is ignored or rejected.
- Reuse the effective access rule that gates the settlement/statement delivery area. Before implementation, inventory whether current exports have a distinct permission check or only authenticated current-book access.
- If no distinct export permission exists, v1 SHALL explicitly use authenticated current-book access and tests SHALL describe it that way; do not claim or test a role distinction the platform does not enforce.
- Introducing a new export-specific role matrix is out of scope for v1.

### D7: Failure model

- If any required member fails to generate, abort before committing response body as success; prefer buffering ZIP until complete **or** fail fast before writing Content-Disposition success headers
- Practical approach: build full ZIP in memory/temp file, then stream; for large books, temp file on disk with size guard
- Log bookId, period, userId, failure stage

### D8: Masterplan layering (not implemented here)

| Phase | Focus | Tracking |
|-------|--------|----------|
| **This change** | 本月账本包 ZIP | `monthly-books-pack` |
| S1 | 凭证批量专业打印、报表 PDF/打印 | next change |
| S2 | 建账/期初接手与录入吞吐 | later |
| S3 | 银行调节、核销账龄、辅助穿透、影像、税费向导 | later |
| S4 | 周边防炸与审计卫生 | later |

## Risks / Trade-offs

- **[Risk] Pack generation timeout / OOM on large books** → Mitigation: temp-file ZIP; reuse streaming writers where possible; document practical period size; optional future async job (out of v1).
- **[Risk] Bulk 明细账 may be large** → Mitigation: use a multi-sheet-by-subject workbook from the existing sub-ledger query path, stream/write it to a temporary file, and enforce practical period/size limits if measurements require them.
- **[Risk] Dual code paths drift from single export** → Mitigation: call shared export methods; E2E spot-check totals vs single endpoints.
- **[Risk] Partial ZIP downloaded on error** → Mitigation: fully build then attach; frontend treat non-2xx as error (no saveAs).
- **[Trade-off] Sync request vs job** → Sync wins for v1 simplicity; revisit if timeouts appear in real packs.

## Migration Plan

1. Ship API + service with feature usable via curl/API client
2. Ship UI button on settlement delivery screen
3. No DB migration required for v1
4. Rollback: hide UI + disable route; no data migration to reverse

## Open Questions

- Whether cash-flow export for months without CF setup should include an empty template or omit with explanation file — prefer include the same empty/standard template produced by the single cash-flow export, so the required entry remains present.
