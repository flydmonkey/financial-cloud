# Acceptance notes — server-pdf-channel

Date: 2026-09-29

## Host font requirement

PDF Chinese rendering needs a CJK font on the host:

1. Prefer env `FINANCIAL_CLOUD_PDF_FONT` = absolute path to TTF/TTC (TTC may use `path,0`).
2. Else auto-detect Windows 微软雅黑/黑体/宋体, Linux Noto/WenQuanYi, macOS PingFang.

Without a font, export endpoints fail with a clear IOException message (no silent empty PDF).

## Code-path verification

| Item | Result | Evidence |
|------|--------|----------|
| OpenPDF dependency | PASS | `financial-cloud/pom.xml` `com.github.librepdf:openpdf`; `mvn -DskipTests compile` OK |
| Shared writer | PASS | `PdfTableExporter` title/subtitle/headers/rows + `application/pdf` + Content-Disposition |
| Unit smoke | PASS | `PdfTableExporterTest.write_producesPdfBytes_whenCjkFontAvailable` |
| Balance sheet API | PASS | `StatementBalanceSheetController` `GET .../balance-sheet/export-pdf` → service `exportPdf` |
| Income API | PASS | `StatementIncomeController` `GET .../income/export-pdf` |
| Subject balance API | PASS | `StatementReportController` `GET .../subject-balance/export-pdf` |
| Frontend | PASS | 三页「导出 PDF」+ `statement.ts` / `statement-income.ts` blob download |
| Docs | PASS | `05-statement` / gap 4.4·7.4 / roadmap / overview 对齐「首批三表服务端 PDF」 |

## Manual UI smoke (recommended on deploy)

1. Open 资产负债表 / 利润表 / 科目余额表 for a book with data
2. Click 导出 PDF → file downloads as `.pdf` and opens with Chinese text
3. Confirm Excel 导出 and 打印 still work beside the new button
