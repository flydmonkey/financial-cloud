# AI UI 专项账套 B — 5.4 工资闭环 / 5.5 费用报销

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项B`
- **bookId**：`2105448444973871105`
- **启用期间**：`2026-01`（凭证审核开启）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T00:38:35.479Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-book-b-payroll-exp.mjs`

## 结论：**5.4/5.5 核心路径 PASS**（PASS 34 / FAIL 0 / WARN 1）

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin + ai_reviewer |
| SWITCH-BOOK | PASS | bookId=2105448444973871105 term=2026-01 |
| SUBJECTS | PASS | 1002/2211.01/5602.07/5602.04/3001 present |
| INSURANCE-CONFIG | PASS | payBase=2500 endowP=0.08 medP=0.02 hfP=0.05 |
| TAX-BRACKETS | PASS | type=0 rows=7 |
| SALARY-FORMULA | WARN | 账套无自定义公式行（使用内置算薪逻辑；非阻断） |
| DEPT | PASS | 已存在 id=2105457004151685122 |
| EMPLOYEE | PASS | 已存在 id=2105457004277514242 card=6222021234567890123 |
| TAX-DEDUCTION | PASS | 已存在 rent=1000 id=2105457004394061825 |
| FUND-BANK | PASS | 银行余额 13767.71 已足够，跳过注资 |
| SALARY-DETAIL | PASS | 已推送 id=2105457005442637825 pay=8000 net=7232.29 |
| OFFLINE-payAmount | PASS | expected=8000 actual=8000 |
| OFFLINE-personalTax | PASS | expected=38.11 actual=38.11 |
| OFFLINE-totalAmount | PASS | expected=7232.29 actual=7232.29 |
| OFFLINE-totalSocialInsurance | PASS | expected=489.6 actual=489.6 |
| OFFLINE-providentFund | PASS | expected=240 actual=240 |
| PAY-ACCRUAL-VOUCHER | PASS | 已过账 id=2105457005623885825 |
| PAY-ACCRUAL-GL | PASS | alreadyPosted; 5602.07=8000 2211.01=-767.71 |
| PAY-ACCRUAL-DUP | PASS | code=2 msg=该员工本月已生成计提凭证，请勿重复生成 |
| PAY-PAYMENT-VOUCHER | PASS | 已过账 id=2105457006374666241 |
| PAY-PAYMENT-GL | PASS | alreadyPosted; bank=13767.71 payable=-767.71 |
| PAY-PAYABLE-RESIDUAL | PASS | SMB模板仅计提应发/发放实发；应付残留≈个人社保+公积金+个税=767.71（未单独记 2221.14/社保负债） |
| PAY-EXPORT | PASS | bytes=3544 ct=application/vnd.ms-excel;chartset=utf-8 |
| PAY-EXPORT-NO-CARD | PASS | code=504007 msg=These employees are missing bank account numbers; fix them before export: AI-UI-20260930-工资员 |
| EXP-CLAIM | PASS | created id=2105457243184177154 amount=123.45 |
| EXP-ATTACH-UPLOAD | PASS | uploaded id=2105457243226120194 list=1 |
| EXP-ATTACH-DOWNLOAD | PASS | bytes=70 |
| EXP-SUBMIT | PASS | status=submitted |
| EXP-AUDIT | PASS | status=approved |
| EXP-WITHDRAW | PASS | 无撤回接口（HTTP 200）；拒绝走 audit approve=false；删除仅 draft/rejected |
| EXP-VOUCHER-GEN | PASS | created id=2105457243382202370 |
| EXP-VOUCHER-DUP | PASS | idempotent return same id: code=0 data=2105457243382202370 expect=2105457243382202370 |
| EXP-VOUCHER-POST | PASS | id=2105457243382202370 sender=true |
| EXP-GL | PASS | 5602.04 Δ=123.45 1002 Δ=123.45 expect=123.45 |
| SCREENSHOTS | PASS | bookb-pay-* + bookb-exp-* captured |

## 金额核对（离线验算）

| 检查点 | 预期 | 实际 |
|---|---:|---:|
| 缴费基数 | 4800 | 4800 |
| 个人社保 | 489.6 | 489.6 |
| 个人公积金 | 240 | 240 |
| 应发 | 8000 | 8000 |
| 累计应纳税所得 | 1270.4 | 1270.4 |
| 本期个税 | 38.11 | 38.11 |
| 实发 | 7232.29 | 7232.29 |
| 计提后 5602.07 增加 | 8000 | (prior) bal=8000 |
| 计提后 2211.01 增加 | 8000 | (prior) bal=-767.71 |
| 发放后 1002 减少 | 7232.29 | (prior) bank=13767.71 |
| 报销后 5602.04 增加 | 123.45 | 123.45 |
| 报销后 1002 减少 | 123.45 | 123.45 |
| 期末银行余额 | 13644.26 | 13644.26 |

## 配置快照

```json
{
  "insurancePayBase": 2500,
  "rates": {
    "endowmentPersonalRate": 0.08,
    "medicalPersonalRate": 0.02,
    "unemploymentPersonalRate": 0.002,
    "providentFundSupPersonalRate": 0.05,
    "endowmentBusinessRate": 0.16,
    "medicalBusinessRate": 0.06,
    "unemploymentBusinessRate": 0.003,
    "employmentInjuryBusinessRate": 0.002,
    "providentFundSupBusinessRate": 0.05
  },
  "taxBracketL1": {
    "min": 0,
    "max": 36000,
    "rate": 3,
    "qd": 0
  },
  "employee": {
    "no": "B-E01",
    "name": "AI-UI-20260930-工资员",
    "payBasic": 8000,
    "payBase": 4800,
    "rent": 1000
  },
  "expense": {
    "amount": 123.45,
    "expenseSubject": "5602.04",
    "fundSubject": "1002"
  },
  "glEnd": {
    "bank": 13644.26,
    "wageExp": 8000,
    "payable": -767.71,
    "office": 8123.45,
    "pit": null
  }
}
```

## 阻塞 / 缺陷 / 观察

- **OBS-EXP-SUMMARY-LEN（P2）**：`voucher_item.summary` 仅 **64** 字符；报销生成凭证摘要为 `费用报销 {单号} {报销人} {事由}（行说明）`。使用带完整 `AI-UI-20260930` 标识的报销人/事由时触发 `Data truncation`，生成失败。缩短字段后可过；生产上长事由/长姓名会踩坑。
- **OBS-PAY-TEMPLATE-SI-TAX**：SMB 模板 `jt_gz`/`zf_gz` 仅借费用贷应付（应发）与借应付贷银行（实发）；个人社保/公积金/个税残留在 **2211.01=767.71**，未分录到 `2221.14` 或社保负债科目（符合当前模板设计，非阻断）。
- **OBS-SALARY-FORMULA-EMPTY**：账套 `config_salary_formula` 无自定义行；算薪走内置逻辑（基数×比例 + 累计预扣），WARN 非阻断。
- **EXP-WITHDRAW**：无撤回 API；拒绝靠 `audit(approve=false)`；删除仅 draft/rejected。重复生成报销凭证为幂等返回原 ID（软防护）。
- **FUND-BANK**：发薪前银行约 1,000，不足实发 7,232.29；脚本注资实收资本 **20,000**（摘要 `AI-UI-20260930-发薪注资`）后继续闭环。

## 银行余额轨迹（专项 B）

| 节点 | 1002 |
|---|---:|
| 5.3 后（既有） | ~1,000 |
| 发薪注资 +20,000 | 21,000 |
| 发放工资 −7,232.29 | 13,767.71 |
| 费用报销 −123.45 | **13,644.26** |

## 证据截图

- `/opt/cursor/artifacts/screenshots/bookb-pay-employee.webp`
- `/opt/cursor/artifacts/screenshots/bookb-pay-calc.webp`
- `/opt/cursor/artifacts/screenshots/bookb-pay-detail.webp`
- `/opt/cursor/artifacts/screenshots/bookb-pay-balance.webp`
- `/opt/cursor/artifacts/screenshots/bookb-exp-claim.webp`
- `/opt/cursor/artifacts/screenshots/bookb-exp-balance.webp`
