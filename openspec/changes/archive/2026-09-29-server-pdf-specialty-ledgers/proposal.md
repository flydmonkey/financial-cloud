## Why

主账簿与报表 PDF 已齐；多栏账与数量金额账仍只有浏览器打印另存。代账对费用类多栏、存货数量金额交付时仍需服务端 PDF。

## What Changes

- `GET /api/voucher/multi-column-ledger/export-pdf`
- `GET /api/voucher/quantity-ledger/export-pdf`
- 两页增加「导出 PDF」
- 更新 `server-pdf` 与产品文档

## Capabilities

### Modified Capabilities

- `server-pdf`: 增加多栏账、数量金额账 PDF 导出

## Impact

- `MultiColumnLedgerService` / `QuantityLedgerService` / `VoucherController` / 前端两页 / docs
