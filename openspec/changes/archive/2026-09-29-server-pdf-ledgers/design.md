## Context

`PdfTableExporter` already powers statement PDFs. General ledger has Excel export from `query()` items. Sub-ledger has print via frontend full fetch; no Excel yet.

## Goals / Non-Goals

**Goals:** General ledger + sub-ledger PDF download; docs.

**Non-Goals:** Multi-column / quantity ledgers; expense-detail / voucher-summary in this knife; row-cap UI warnings beyond a hard server pageSize ceiling.

## Decisions

1. General ledger PDF columns match Excel: 科目编码/名称/期间/摘要/借方/贷方/方向/余额；landscape.
2. Sub-ledger: set pageSize high (e.g. 10000) in export path; columns match print: 日期/凭证字号/摘要/借方/贷方/余额.
3. Cap: if records exceed pageSize, export first pageSize rows (document in acceptance); avoid OOM.

## Risks / Trade-offs

- **[Risk] Huge sub-ledger** → Mitigation: pageSize ceiling 10000; acceptance notes.
