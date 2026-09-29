## Why

代账交付常需把报表直接发客户或归档；当前只有浏览器「打印另存 PDF」，依赖本机对话框且版式不可控。需要一条服务端统一 PDF 下载通道，与 Excel 导出并列。

## What Changes

- 引入服务端 PDF 生成能力（OpenPDF），提供可复用的表格型 PDF 写出器（中文字体、标题/副标题、表头与数据行）。
- 首批接通：**资产负债表**、**利润表**、**科目余额表** 的 `export-pdf` 下载接口。
- 前端对应页增加「导出 PDF」按钮（blob 下载）。
- 更新产品文档：服务端 PDF 从「未实现」改为「部分实现（三表通道）」。

## Capabilities

### New Capabilities

- `server-pdf`：服务端表格型财务报表 PDF 导出通道与首批报表覆盖。

### Modified Capabilities

- （无）

## Impact

- **后端**：新依赖 OpenPDF；`util/pdf/PdfTableExporter`；三处 Controller/Service 增 exportPdf。
- **前端**：资产负债表 / 利润表 / 科目余额表页按钮 + API。
- **非目标**：凭证经典版式 PDF、账本包内嵌 PDF、全部账簿一刀切、自定义模板市场。
