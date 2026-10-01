# AI UI Export Content Checks（主账套 A）

- **time**: 2026-10-01T00:46Z
- **book**: `2105377998655979522`
- **期间样本**: 2026-01
- **脚本**: `financial-cloud-ui/scripts-ai-ui-export-check.mjs`

## 结论：内容级导出 PASS

| ID | Status | 校验 |
|---|---|---|
| BS-XLSX | PASS | 含 160000 / 242000 / 资产；sheet=资产负债表 |
| IS-XLSX | PASS | 含 80000 / 40000 / 净利润；sheet=利润表 |
| CF-XLSX | PASS | 含 50000 / 32000 / 160000 / 销售商品 |
| SB-XLSX | PASS | 含 1002 / 160000 / 银行存款 |
| BS-PDF | PASS | `%PDF-`，≈64KB |
| CF-PDF | PASS | `%PDF-`，≈66KB |

文件：`/opt/cursor/artifacts/downloads/export-*`  
截图：`/opt/cursor/artifacts/screenshots/export-check-bs.webp`
