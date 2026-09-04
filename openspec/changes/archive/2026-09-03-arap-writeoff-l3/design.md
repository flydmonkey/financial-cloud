## Context

L1+L2 (`arap-assist`) reconstructs AR/AP from posted `voucher_auxiliary` on receivable/payable subjects. Aging uses FIFO on gross movements. Month-end shows overdue as warning only. Users need true open-item write-off (核销) without introducing invoice/receipt business documents.

## Goals / Non-Goals

**Goals:**

- Persist matches between open debit/credit AR (or AP) lines for one counterpart; support partial amounts.
- Open-item list + reverse under period guards.
- Optional match suggestions (preview only).
- Prefer open-item layers for aging and month-end overdue when write-offs exist; FIFO fallback otherwise.

**Non-Goals:**

- Bank reconciliation, scheduled auto-clearing jobs, multi-currency, bad-debt workflows, separate AR/AP subledger replacing GL.

## Decisions

### D1: Match store, not new business docs

**Choice:** New write-off header + line tables referencing `voucher_item` / auxiliary counterpart ids and amounts. Source remains posted voucher lines.

**Alternatives:** Invoice/receipt documents — rejected for L3 scope.

### D2: Signed open layers per side

**Choice:** For AR, treat customer-tagged receivable-subject lines as layers (debit increases open, credit decreases via match or as offsetting open credits). Matching consumes remaining amounts on both sides. Same pattern mirrored for AP/supplier.

### D3: Aging integration

**Choice:** If any write-off exists for counterpart (or book flag), age remaining open layers by layer date; else keep FIFO estimate and surface `agingMethod` in API (`OPEN_ITEM` | `FIFO_ESTIMATE`).

### D4: Reverse policy

**Choice:** Allow reverse only when both matched voucher periods are still open relative to the book’s current open term (or all lines’ periods ≥ current open term). Closed-period matches are reverse-blocked.

### D5: UI

**Choice:** New menu under 往来：核销工作台；reuse balance drill counterpart context. Suggestions button optional in same page.

## Risks / Trade-offs

- **[Risk] Historical books with no write-offs** → FIFO fallback keeps L2 behavior.
- **[Risk] Mis-match** → reverse + audit fields (`created_by`, timestamps).
- **[Trade-off] Warning-only overdue at month-end unchanged** → avoids blocking 代账 until process maturity.

## Migration Plan

1. Add tables + APIs; ship UI behind menu seed.
2. Cut aging/month-end to open-item when data present.
3. No forced backfill of historical matches.
4. Rollback: feature flag or stop writing matches; aging falls back to FIFO.

## Open Questions

- Exact table naming / whether soft-delete vs reverse-only history — decide in impl to match project persistence conventions.
- Whether statement Excel must show核销状态 in v1 — prefer open-item API first; Excel annotation optional if cheap.
