## Why

报表三表+科目余额已有服务端 PDF；代账交付还常需要总账、明细账 PDF。复用 `PdfTableExporter` 可快速补齐账簿交付通道。

## What Changes

- 总账 `GET /api/statement/general-ledger/export-pdf`
- 明细账 `GET /api/voucher/items/export-pdf`（按当前查询条件，拉全量行）
- 对应页增加「导出 PDF」
- 更新 `server-pdf` 规格与产品文档

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `server-pdf`: 增加总账与明细账 PDF 导出要求

## Impact

- `StatementGeneralLedgerService` / Controller、`VoucherService` / `VoucherController`、前端 general-ledger / sub-ledger、docs
