# Acceptance notes — server-pdf-cash-flow

Date: 2026-09-29

| Item | Result | Evidence |
|------|--------|----------|
| API | PASS | `GET /api/statement/cash-flow/export-pdf` → `StatementReportService.cashFlowExportPdf` |
| UI | PASS | cash-flow-statement 「导出 PDF」 |
| Spec/docs | PASS | `server-pdf` + 05/gap/roadmap/overview |

Manual: open 现金流量表 → 导出 PDF → Chinese text visible with host CJK font.
