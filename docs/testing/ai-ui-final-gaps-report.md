# AI UI 收尾细项：累计预扣 / 级联 / 模板 / 账龄 / 月结工资互斥

- **标识**：`AI-UI-20260930`
- **账套**：专项 B `2105448444973871105`
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T03:10:26.929Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-final-gaps.mjs`

## 结论：**PASS**（PASS 20 / FAIL 0 / WARN 0）

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin |
| PAY-JAN-BASE | PASS | pay=8000 taxable=1270.4 tax=38.11 si=489.6 hf=240 add=1000 |
| PAY-FEB-TAXABLE | PASS | expected=2540.8 actual=2540.8 |
| PAY-FEB-PERIOD-TAX | PASS | expected=38.11 (=cum 76.22-jan 38.11) actual=38.11 |
| PAY-FEB-PUSH | PASS | already id=2105476796505198593 |
| PAY-FEB-STORED | PASS | id=2105476796505198593 tax=38.11 taxable=2540.8 |
| PAY-HIST-CASCADE | PASS | 改 Jan pay→8500 后 Jan.taxable 仍 1270.4；Feb 预览 taxable=2540.8（未自动级联） |
| PAY-HIST-RESTORE | PASS | Jan payAmount restored to 8000 |
| PAY-MONTH-END-MUTEX | PASS | code=2 msg=本期已有按员工明细生成的工资计提凭证，请勿再通过期末结转汇总计提，以免重复入账 |
| PAY-MUTEX-DRAFT-CLEAN | PASS | del 2105495475033473026 code=0 |
| TPL-LOAD | PASS | code=jt_gz items=5602.07/D,2211.01/C |
| TPL-APPLY-DRAFT | PASS | code=0 id=2105495475377405953 msg=暂存成功 |
| TPL-APPLY-CHECK | PASS | lines=2 debit/credit 100 ok=true codes=5602.07,2211.01 |
| TPL-CLEANUP | PASS | deleted draft 2105495475377405953 |
| AGING-OPEN-ITEMS | PASS | n=2 |
| AGING-WO | PASS | confirmed id=2105495475544416257 |
| AGING-TOTAL | PASS | method=OPEN_ITEM total=6000 b0_30=6000 |
| AGING-OPEN-METHOD | PASS | OPEN_ITEM buckets sum = total |
| AGING-VS-BALANCE | PASS | arap ending=6000 aging.total=6000 |
| AGING-WO-REVERSE | PASS | code=0 msg=null |

## 观察

- OBS-PAY-HIST-NO-CASCADE：已确认工资明细改应发不重算个税，也不自动重算后续月累计；update 还会清空凭证关联字段
- 互斥探测曾生成草稿计提 2105495475033473026（原过账计提 2105457005623885825 仍在）；测后删除草稿

## 证据截图

- `/opt/cursor/artifacts/screenshots/final-salary-feb.webp`
- `/opt/cursor/artifacts/screenshots/final-aging.webp`
- `/opt/cursor/artifacts/screenshots/final-template.webp`
- `/opt/cursor/artifacts/screenshots/final-mutex.webp`
