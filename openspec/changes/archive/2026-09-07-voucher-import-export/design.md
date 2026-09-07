## Context

See proposal.md for motivation. Today `GET /api/voucher/export` uses `VoucherService.export` with `template-voucher.xlsx` and `pageList(dto).getRecords()`, so export may only cover the current page. There is no import or template-download endpoint. Fixed-asset cards already implement `GET /import-template` + `POST /import` with `ExcelImport` and a success/fail result VO — that pattern is the implementation reference. Import MUST reuse draft-create validation semantics (`POST /draft`), not submit/audit/sender paths.

## Goals / Non-Goals

**Goals:**
- One shared xlsx layout for template download, export, and import parse
- Export all filter-matched vouchers (no pagination cap)
- Import as insert-only drafts with word reuse-or-auto-assign and partial-success reporting
- List UI: 下载模板 / 导入 / 导出

**Non-Goals:**
- Auxiliary accounting or cash-flow column round-trip
- Attachment files, PDF export, upsert-by-id, import into reviewing/completed/posted
- Changing the voucher status machine or settlement rules

## Decisions

### 1. API surface on existing voucher controller
- **Choice**: Add `GET /api/voucher/import-template` and `POST /api/voucher/import` (`@ModelAttribute ExcelImport`, field `excelFile`); enhance existing `GET /api/voucher/export` to load all matching rows for the filter DTO.
- **Rationale**: Keeps voucher I/O under one resource; mirrors FixedAssetController.
- **Alternatives considered**: Separate import controller — rejected as unnecessary indirection.

### 2. Full export query
- **Choice**: Export path queries matching vouchers without relying on UI page size (dedicated list-all-by-filter or `pageList` with page size unbounded / high enough to cover matches, preferring a non-paginated fetch if already available on mapper).
- **Rationale**: Spec requires filter-aware full export; current `getRecords()` alone is insufficient.
- **Alternatives considered**: Client-side multi-page merge — rejected (incomplete, slow, auth duplication).

### 3. Shared template resource
- **Choice**: Use one workbook under `static/export-template/` (evolve `template-voucher.xlsx` or replace with a round-trip-capable sibling referenced by both export and import-template). Import parser and export writer MUST share the same column contract (headers).
- **Rationale**: Spec requires mutual recognition for re-import.
- **Alternatives considered**: Separate import vs export templates with a mapping layer — higher drift risk.

### 4. Import persistence path
- **Choice**: Build `VoucherChangeDto` (+ items) per Excel voucher group; persist via the same service path used for draft create (balance, subject, open-period checks). Force `status = draft`; clear audit/sender/manager fields. Ignore any id column.
- **Rationale**: Guarantees draft-only and consistent validation with manual entry.
- **Alternatives considered**: Direct mapper inserts — rejected (skips business rules).

### 5. Word number rule (product 2B)
- **Choice**: If `wordHead` + `wordNum` present and unused under book numbering rules → use them; else call existing able-word-num allocation.
- **Rationale**: Locked product decision; supports migration while avoiding duplicates.
- **Alternatives considered**: Always auto-assign — rejected by product; always fail on conflict — worse UX for round-trip re-import of same book exports.

### 6. Partial success and error model
- **Choice**: Result VO with `successCount`, `failureCount`, and list of `{ rowOrGroupKey, reason }` (align naming with FixedAsset import result). Valid groups commit; invalid groups skip (per-group transaction or equivalent isolation so one failure does not roll back successes).
- **Rationale**: Matches existing import UX elsewhere; large files should not be all-or-nothing.
- **Alternatives considered**: All-or-nothing file transaction — rejected for usability.

### 7. Frontend wiring
- **Choice**: Extend `api/voucher/voucher.ts`; on `voucher-index.vue`「更多」add template download + ImportUpload dialog; keep `exportVouchers(queryParams)` but backend now returns full filtered set.
- **Rationale**: Minimal UI change on the existing list entry point.

### 8. Out-of-scope columns on round-trip
- **Choice**: Export may still include display-only columns (status, auditor names). Import MUST ignore status/workflow columns and auxiliary/cash-flow if absent from the shared contract; v1 shared contract covers header (date, word head/num, receipt count, remark as applicable) + lines (summary, subject code, debit, credit).
- **Rationale**: Keeps v1 shippable without auxiliary complexity while still allowing re-import of core amounts.

## Risks / Trade-offs

- **[Risk] Large filtered export memory/time** → Mitigation: document practical limits; reuse existing export streaming/template approach; avoid N+1 if item load is separate.
- **[Risk] Re-importing an export into the same book creates duplicate drafts** → Mitigation: by design (insert-only); word conflicts auto-assign new numbers; communicate in UI copy.
- **[Risk] Template column drift between export annotations and import parser** → Mitigation: single header contract tested by a round-trip unit/integration check in tasks.
- **[Risk] Subject matched by name only is ambiguous** → Mitigation: require subject **code** in the shared layout.

## Migration Plan

- Deploy backend endpoints + template first; frontend actions can ship in the same release.
- Existing “导出” keeps the same URL; behavior change is fuller row set (additive, not breaking clients).
- Rollback: remove/hide import UI and new endpoints; export can revert to previous query if needed.

## Open Questions

None for this change — product choices 1C/2B and design decisions above are locked.
