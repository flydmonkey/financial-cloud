## Context

See proposal. Excel export already exists for BS/IS/subject-balance. Browser `tablePrint` remains for interactive print. No PDF library in pom yet.

## Goals / Non-Goals

**Goals:** Shared PDF table exporter + three statement endpoints + UI buttons.

**Non-Goals:** Classic voucher PDF layout; books-pack PDF members; every ledger page in v1.

## Decisions

### D1: Library = OpenPDF
`com.github.librepdf:openpdf` — maintained iText fork, Apache-2.0 friendly.

### D2: CJK font resolution
Resolve in order: env `FINANCIAL_CLOUD_PDF_FONT`, then common Windows paths (`msyh.ttc`, `simhei.ttf`, `simsun.ttc`), then Linux `/usr/share/fonts/**/NotoSansCJK*.ttc`. Fail with clear error if none found (do not silently emit mojibake).

### D3: Landscape A4 for wide BS table; portrait for IS / subject balance
Balance sheet has 8 columns → landscape. Others portrait.

### D4: Reuse query/export data assembly
`exportPdf` calls existing query methods used by Excel export / page display; map to string cells only.

## Risks / Trade-offs

- **[Risk] Font missing in container** → Mitigation: document env var; error message names expected paths.
- **[Trade-off] Simple tables vs pixel-perfect Excel templates** → Accepted for v1 delivery speed.

## Migration Plan

Add dependency → ship API → UI buttons → docs. Rollback: hide buttons.

## Open Questions

None.
