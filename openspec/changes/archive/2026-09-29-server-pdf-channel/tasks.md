## 1. PDF infrastructure

- [x] 1.1 Add OpenPDF dependency to `financial-cloud/pom.xml` and verify Maven resolves it
- [x] 1.2 Implement `PdfTableExporter` (font resolve, title/subtitle, headers/rows, write to response); verify a smoke call produces non-empty PDF bytes when a system CJK font exists
- [x] 1.3 Add unit/smoke test for font resolver or exporter happy-path if feasible without GUI; otherwise document host font requirement in acceptance notes

## 2. Statement endpoints

- [x] 2.1 Balance sheet `GET .../balance-sheet/export-pdf` using query data; verify Content-Type and filename
- [x] 2.2 Income statement `GET .../income/export-pdf`; verify Content-Type and filename
- [x] 2.3 Subject balance `GET .../subject-balance/export-pdf` (StatementReportController path); verify Content-Type and filename

## 3. Frontend & docs

- [x] 3.1 API helpers + 「导出 PDF」 on balance-sheet / income-statement / subject-balance pages; verify blob download
- [x] 3.2 Update `05-statement.md` / gap 4.4 & 7.4 / roadmap P2 PDF wording; verify docs say server PDF partial
- [x] 3.3 Archive change after acceptance notes
