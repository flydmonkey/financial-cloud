## Why

凭证汇总表是账簿交付常项，此前仅 Excel/浏览器打印；补齐服务端 PDF 以收口 PDF 交付面。

## What Changes

- `GET /api/statement/voucher-summary/export-pdf` + 前端按钮 + 文档

## Capabilities

### Modified Capabilities

- `server-pdf`: 增加凭证汇总表 PDF

## Impact

- StatementReportService/Controller、voucher-summary.vue、docs
