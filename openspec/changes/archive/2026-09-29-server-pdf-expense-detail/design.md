## Context

Reuse `PdfTableExporter`; flatten expense tree like Excel export.

## Goals / Non-Goals

**Goals:** Expense detail PDF + UI + docs.
**Non-Goals:** Multi-column / quantity ledger PDF.

## Decisions

1. Dynamic headers: 编码/名称 + each period label + year total.
2. Flatten depth-first with indent spaces on name by level.
3. Landscape when period columns > 3.

## Risks

- Wide tables when many months → landscape + smaller font already in exporter.
