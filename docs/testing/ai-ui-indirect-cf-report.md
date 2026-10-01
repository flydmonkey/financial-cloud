# AI UI 主账套现金流量 / 间接法点测

- time: 2026-10-01T03:11:35.455Z
- book: 2105377998655979522

独立预期（提示词）：1 月经营净额 32,000；间接法 40k+10k−30k+12k；2 月 28,000；13k+12k+10k−7k。

| ID | Status | Detail |
|---|---|---|
| 2026-01-ROWS | INFO | count=56 |
| 2026-01-FIELDS | INFO | yearPeriod,currentAmount,monthlyAmount |
| 2026-01-SAMPLE | INFO | {"id":null,"yearPeriod":null,"reportDate":null,"periodType":null,"sortIndex":2,"itemName":"销售商品、提供劳务收到的现金","itemCode":"2-jy-sqxj","currentAmount":50000,"monthlyAmount":50000,"bookI \|\| {"id":null,"yearPeriod":null,"reportDate":null,"periodType":null,"sortIndex":3,"itemName":"收到的税费返还","itemCode":"3-jy-sffh","currentAmount":0,"monthlyAmount":null,"bookId":null,"del \|\| {"id":null,"yearPeriod":null,"reportDate":null,"periodType":null,"sortIndex":4,"itemName":"收到其他与经营活动有关的现金","itemCode":"4-jy-sdqt","currentAmount":0,"monthlyAmount":null,"bookId":nu |
| 2026-01-DIRECT-SALES | PASS | actual=50000 expected=50000 |
| 2026-01-DIRECT-PURCHASE | PASS | actual=8000 expected=8000 |
| 2026-01-DIRECT-OTHER-OUT | PASS | actual=10000 expected=10000 (may be missing CF assign) |
| 2026-01-DIRECT-INVEST | PASS | actual=12000 expectedAbs=12000 |
| 2026-01-DIRECT-FINANCE | PASS | actual=40000 expected=40000 |
| 2026-01-IND-NI | PASS | actual=40000 expected=40000 |
| 2026-01-IND-INV | PASS | actual=10000 expectedDec=10000 |
| 2026-01-IND-AR | WARN | actual=-18000 expectedInc=30000 (sign per UI) |
| 2026-01-IND-AP | PASS | actual=12000 expectedInc=12000 |
| 2026-01-OP-NETS | INFO | #0 m=32000 y=0; #1 m=32000 y=0 |
| 2026-01-IND-CALC | WARN | ni+inv+ar+ap=44000 expectedOp=32000 |
| 2026-02-ROWS | INFO | count=56 |
| 2026-02-FIELDS | INFO | yearPeriod,currentAmount,monthlyAmount |
| 2026-02-SAMPLE | INFO | {"id":null,"yearPeriod":null,"reportDate":null,"periodType":null,"sortIndex":2,"itemName":"销售商品、提供劳务收到的现金","itemCode":"2-jy-sqxj","currentAmount":90000,"monthlyAmount":40000,"bookI \|\| {"id":null,"yearPeriod":null,"reportDate":null,"periodType":null,"sortIndex":3,"itemName":"收到的税费返还","itemCode":"3-jy-sffh","currentAmount":0,"monthlyAmount":null,"bookId":null,"del \|\| {"id":null,"yearPeriod":null,"reportDate":null,"periodType":null,"sortIndex":4,"itemName":"收到其他与经营活动有关的现金","itemCode":"4-jy-sdqt","currentAmount":0,"monthlyAmount":null,"bookId":nu |
| 2026-02-DIRECT-SALES | WARN | actual=90000 expected=40000 |
| 2026-02-DIRECT-PURCHASE | WARN | actual=15000 expected=7000 |
| 2026-02-DIRECT-OTHER-OUT | WARN | actual=15000 expected=5000 (may be missing CF assign) |
| 2026-02-DIRECT-INVEST | WARN | actual=12000 expectedAbs=0 |
| 2026-02-DIRECT-FINANCE | WARN | actual=40000 expected=0 |
| 2026-02-IND-NI | WARN | actual=53000 expected=13000 |
| 2026-02-IND-INV | WARN | actual=22000 expectedDec=12000 |
| 2026-02-IND-AR | WARN | actual=-15000 expectedDec=10000 |
| 2026-02-IND-AP | WARN | actual=5000 expectedDec=7000 |
| 2026-02-OP-NETS | INFO | #0 m=60000 y=0; #1 m=60000 y=0 |
| 2026-02-IND-CALC | WARN | ni+inv+ar+ap=65000 expectedOp=28000 |

## 证据

- `/opt/cursor/artifacts/screenshots/indirect-cf-ui.webp`
- `/opt/cursor/artifacts/screenshots/indirect-cf-ui-bottom.webp`
