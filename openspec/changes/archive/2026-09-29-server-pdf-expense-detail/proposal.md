## Why

费用明细常与利润表一并交付；三表/账簿已有服务端 PDF，补费用明细可减少浏览器另存依赖。

## What Changes

- `GET /api/statement/expense-detail/export-pdf`
- 费用明细页「导出 PDF」
- 更新 server-pdf 规格与文档

## Capabilities

### Modified Capabilities

- `server-pdf`: 增加费用明细表 PDF 导出

## Impact

- `StatementExpenseDetailService` / Controller / frontend / docs
