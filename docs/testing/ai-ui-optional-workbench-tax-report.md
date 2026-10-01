# AI UI 可选深路径：工作台 / 封存 / 税费

- **标识**：`AI-UI-20260930`
- **关注月**：`2026-02`
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T03:11:02.379Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-optional-workbench-tax.mjs`

## 结论：**PASS**（PASS 21 / FAIL 0 / WARN 0）

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin |
| WB-LIST | PASS | focus=2026-02 n=5 A/B/C/D=true/true/true/true |
| WB-SUMMARY-A | PASS | A close=CLOSED term=2026-03 blocker=READY_PACK; strip={"total":5,"open":0,"closed":2,"behind":3,"withTodo":3} |
| WB-UI | PASS | seesA/C=true |
| WB-DRILL | PASS | AI-UI-20260930-专项B blocker=BEHIND → /settlement/settle-period url=http://127.0.0.1:3154/settlement/settle-period |
| WB-ENTER-BTN | PASS | url=http://127.0.0.1:3154/settlement/settle-period |
| SEAL-BOOK | PASS | id=2105473362390200321 name=AI-UI-20260930-封存探针 |
| SEAL | PASS | code=0 msg=封存成功 |
| SEAL-WRITE-DENY | PASS | draft code=510018 msg=This book is sealed (archived, read-only). Unseal it before making changes. |
| UNSEAL | PASS | code=0 msg=已解除封存 |
| SEAL-RESTORED | PASS | status=1 |
| TAX-EST-CIT | PASS | rev=80000 profit=40000 cit=10000 (expect 80k/40k/10k@25%) |
| TAX-EST-VAT | PASS | output/input/due=0/0/0（主账套无增值税税目发生，口径=已过账分录） |
| TAX-DECL | PASS | row1 amount=80000 lines=20 |
| TAX-EST-UI | PASS | 2026-01 UI shows profit 40,000 / CIT 10,000 |
| TAX-DECL-UI | PASS | 财务云 当前账期：2026年03月 账套： AI-UI-20260930-主账套A 系统管理员 (admin) 仪表盘 凭证 账簿 报表 资产负债表 利润表 现 |
| UI-NO-ACCESS | PASS | http://127.0.0.1:3154/no-access |
| UI-VOUCHER-FILTER | PASS | filter=true |
| UI-VOUCHER-PAGER | PASS | pager=true |
| UI-FILTER-RESET | PASS | restored=true |
| UI-SESSION | PASS | url=http://127.0.0.1:3154/login?redirect=/voucher/voucher-index |

## 观察

- 税费测算：企税=利润总额×税率可独立验算；增值税依赖应交税费科目发生，本夹具为 0；UI 明示非正式申报
- 增值税申报表：由税费测算推导的参考表，非正式外部申报

## 证据截图

- `/opt/cursor/artifacts/screenshots/opt-workbench.webp`
- `/opt/cursor/artifacts/screenshots/opt-tax-estimate.webp`
- `/opt/cursor/artifacts/screenshots/opt-tax-declaration.webp`
- `/opt/cursor/artifacts/screenshots/opt-seal.webp`
- `/opt/cursor/artifacts/screenshots/opt-ui-edges.webp`
