# Acceptance notes — server-pdf-ledgers

Date: 2026-09-29

| Item | Result | Evidence |
|------|--------|----------|
| General ledger API | PASS | `GET /api/statement/general-ledger/export-pdf` |
| Sub-ledger API | PASS | `GET /api/voucher/items/export-pdf` (pageSize cap 10000) |
| UI | PASS | general-ledger / sub-ledger 「导出 PDF」 |
| Docs/spec | PASS | server-pdf + 04/05/gap/roadmap/overview/backlog |

Manual: 总账选期间导出 PDF；明细账选科目导出 PDF；中文需主机 CJK 字体。
