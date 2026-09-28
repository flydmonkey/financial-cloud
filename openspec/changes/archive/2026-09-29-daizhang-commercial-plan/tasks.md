## 1. Delivery semantics and access baseline

- [x] 1.1 Inventory the existing subject-balance, balance-sheet, income, cash-flow, sub-ledger, voucher-list, voucher-summary, and general-ledger query/export paths; record which paths provide transaction-level versus aggregate content and verify the selected pack member source for every required entry is documented in the implementation notes or tests
- [x] 1.2 Prove the detail-ledger source can produce a period-wide workbook with voucher-level rows for all applicable subjects; verify a sample row retains date, voucher identity, summary, debit, credit, direction, and running balance, and do not proceed by relabeling general-ledger output
- [x] 1.3 Prove the voucher-list source identifies individual period vouchers; verify voucher-summary aggregate output is not used under the voucher-list name
- [x] 1.4 Inventory the effective authentication, current-book scoping, and permission checks on the settlement/statement delivery area and existing exports; record the v1 access rule in endpoint tests without inventing a role distinction absent from the platform

## 2. Export writer reuse

- [x] 2.1 Extract or wrap each template-based exporter so both its public HTTP endpoint and pack assembly can target a request-scoped temp file or output stream through the existing `ExcelExporter` flow; verify a representative report produced through both paths has matching workbook totals
- [x] 2.2 Extract shared workbook/output builders for programmatic POI exports needed by the pack and verify their single-export behavior remains unchanged
- [x] 2.3 Implement cleanup for all intermediate report and ZIP files in success and failure paths; verify an induced member failure leaves no request-scoped temp artifacts

## 3. Books-pack API

- [x] 3.1 Implement books-pack service: validate `YYYY-MM`, derive the current book from authenticated context, and assemble required members (余额表、逐笔明细账、资产负债表、利润表、现金流量表) with accurate Chinese filenames including period; verify ZIP entry names, count, and content type in a service test
- [x] 3.2 Honor `includeVoucherList` (default true): when true add an individual-voucher list; when false omit it; verify both cases and verify aggregate voucher-summary output is not mislabeled as the list
- [x] 3.3 Expose the endpoint under `/api/statement/.../books-pack` (or equivalent) using the access rule established in task 1.4; verify unauthenticated/inaccessible-book requests are rejected, client-selected cross-book scope cannot take effect, malformed period gives a clear error, and happy path returns `application/zip`
- [x] 3.4 Build the complete ZIP in a request-scoped temp location before committing a successful response; on member failure abort with an error and structured log (bookId, period, userId, stage), and verify no usable partial download is returned

## 4. Frontend

- [x] 4.1 Add API client method for books-pack download (blob) and verify request params (`yearPeriod`, `includeVoucherList`) do not include an authoritative client-selected book id
- [x] 4.2 Add「导出本月账本包」to the settlement delivery UI with period defaulting to the book current term and checkbox「含凭证清单」(default on); verify the displayed default period and action visibility follow the established delivery-area access rule
- [x] 4.3 Wire confirm → download via the existing ZIP/blob helper; on non-2xx show error toast and do not save a file; verify manual or E2E happy and error paths

## 5. Acceptance

- [x] 5.1 E2E or API test: export a sample-period pack, unzip it, assert every required file is present, assert the detail ledger contains voucher-level rows, and assert voucher-list presence toggles
- [x] 5.2 Spot-check that pack totals for at least the balance sheet or subject balance match the corresponding single-report export for the same period
- [x] 5.3 Negative tests: malformed period, unauthenticated/inaccessible-book request, cross-book input, and induced member failure are rejected with no successful ZIP body

## 6. Follow-ups (tracking only)

- [x] 6.1 Confirm the S1 next-change candidates (凭证批量专业打印、报表 PDF) remain recorded in this change's design/proposal; no S1 implementation is required here
