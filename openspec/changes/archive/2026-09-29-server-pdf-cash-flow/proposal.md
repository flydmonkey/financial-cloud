## Why

首批服务端 PDF 已覆盖资产负债/利润/科目余额；代账交付常一并需要现金流量表 PDF，复用 `PdfTableExporter` 即可补齐三表闭环。

## What Changes

- 现金流量表增加 `GET .../cash-flow/export-pdf`
- 前端现金流量表页增加「导出 PDF」
- 更新 `server-pdf` 规格与产品文档表述

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `server-pdf`: 增加现金流量表 PDF 导出要求

## Impact

- `StatementReportService` / `StatementReportController` / `statement.ts` / `cash-flow-statement.vue` / docs
