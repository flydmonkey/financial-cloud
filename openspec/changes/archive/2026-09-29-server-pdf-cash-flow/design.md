## Context

Reuses `PdfTableExporter` and existing `cashFlowStatement` query. No new dependencies.

## Goals / Non-Goals

**Goals:** Cash-flow PDF endpoint + UI button + docs.

**Non-Goals:** Indirect-method appendix as separate PDF; expense-detail / ledger PDFs.

## Decisions

1. Map cash-flow rows to columns: 项目 / 行次 / 本月金额 / 本年累计（对齐 Excel/页面常用列）.
2. Landscape optional false (few columns).

## Risks / Trade-offs

- **[Risk] Font missing** → Same FINANCIAL_CLOUD_PDF_FONT requirement as first batch.
