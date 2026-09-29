## Context

See proposal.md. Fixed-asset card already has export / import-template / multipart import via Apache POI + `ExcelImport`. Opening balance already has `list` + `save` with initialize-task guard and voucher lock flags (`hasVoucher`).

## Goals / Non-Goals

**Goals:**
- Mirror fixed-asset Excel UX for opening balances.
- Reuse `BookInitBalanceService.list` for export source and `save` for persistence after building change DTOs.
- Only mutate editable leaf rows (no children in tree OR subject is leaf; skip `hasVoucher`).

**Non-Goals:**
- Cash-flow opening Excel.
- Creating new subjects from Excel.
- Changing trial-balance algorithm.

## Decisions

### D1: Columns
`科目编码 | 科目名称 | 方向(1借/2贷) | 年初余额借方 | 年初余额贷方 | 本年累计借方 | 本年累计贷方 | 余额`

### D2: Match key
Subject `code` within current `bookId`. Name in Excel is informational; server trusts code.

### D3: Leaf detection
A row is editable iff `hasVoucher == false` and it has no children in the listed tree (same rule as UI: parents with children are display-only). Implement by building parent→children map from list() result.

### D4: Import applies via existing `save`
Build `List<BookInitBalanceChangeDto>` for successful rows (merge with current list state for unchanged fields) and call `save`.

## Risks / Trade-offs

- **[Risk] Parent totals drift if only leaves imported** → Mitigation: existing save/recalc paths used by UI; after import refresh list + prompt trial balance.
- **[Risk] Large sheets** → Mitigation: same 10k subject page bound as list.

## Migration Plan

Ship API then UI buttons. Rollback: hide UI; endpoints unused.

## Open Questions

None.
