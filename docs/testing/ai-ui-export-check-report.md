# AI UI Export Content Checks（主账套 A）

- time: 2026-10-01T03:11:20.452Z
- book: 2105377998655979522
- period sample: 2026-01

| ID | Status | Detail |
|---|---|---|
| BS-XLSX | PASS | size=11132 sheets=资产负债表 hits={"160000":true,"242000":true,"银行存款":false,"资产":true} |
| IS-XLSX | PASS | size=9946 sheets=利润表 hits={"40000":true,"80000":true,"主营业务收入":false,"净利润":true} |
| CF-XLSX | PASS | size=10854 sheets=现金流量表 hits={"32000":true,"50000":true,"160000":true,"销售商品":true} |
| SB-XLSX | PASS | size=18909 sheets=科目余额表 hits={"1002":true,"160000":true,"银行存款":true} |
| BS-PDF | PASS | size=63944 head=%PDF- |
| CF-PDF | PASS | size=65899 head=%PDF- |

Files under `/opt/cursor/artifacts/downloads/export-*`.
