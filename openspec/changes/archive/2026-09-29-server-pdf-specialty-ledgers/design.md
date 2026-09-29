## Context

Reuse `PdfTableExporter` and existing `query()` on both services.

## Goals / Non-Goals

**Goals:** Multi-column + quantity PDF endpoints and UI.
**Non-Goals:** Excel export for these pages; overwrite restore.

## Decisions

1. Multi-column: dynamic headers from `columns`; landscape always.
2. Quantity: flat 12 data columns (no HTML colspan); landscape.
3. Require subjectCode; empty subject → 400 from existing query.

## Risks

- Many child columns → wide PDF; landscape mitigates.
