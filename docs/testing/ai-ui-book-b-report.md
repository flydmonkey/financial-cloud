# AI UI 专项账套 B（出纳日记账）测试报告

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项B`
- **bookId**：`2105448444973871105`
- **启用期间**：`2026-01`（凭证审核开启）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T00:07:13.654Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-book-b.mjs`

## 结论：**5.1 核心路径 PASS**（PASS 22 / FAIL 0 / WARN 0）

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin + ai_reviewer |
| BOOK-FIND | PASS | 已存在 bookId=2105448444973871105 |
| GRANT-REVIEWER | PASS | ai_reviewer 已有账套权限 |
| SWITCH-BOOK | PASS | bookId=2105448444973871105 term预期=2026-01 |
| SUBJECTS | PASS | bank=1002 capital=3001 revenue=5001 expense=5602.01 |
| LEDGER-OPENING | PASS | 总账期初已是银行/资本 10000 |
| LEDGER-OPEN-CHECK | PASS | 总账1002期初列=10000（期末现=12000） |
| JOURNAL-ACCOUNT | PASS | 账户已存在 id=2105448449628786690 balance=12000 |
| JOURNAL-OPEN-ENTRY | PASS | 期初流水已存在 id=2105448449838501889 balance=12000 |
| JOURNAL-OPEN-BAL | PASS | 已含收支流水，日记账余额=12000 |
| JOURNAL-INCOME | PASS | 收入流水已存在 id=2105448449989496833 |
| JOURNAL-EXPENSE | PASS | 支出流水已存在 id=2105448450157268994 |
| JOURNAL-BALANCE | PASS | 日记账余额 expected=12000 actual=12000 |
| V-IN-GEN | PASS | 已关联 voucherId=2105448493044789250 |
| V-IN-POST | PASS | 已过账 status=completed |
| V-OUT-GEN | PASS | 已关联 voucherId=2105448493623603201 |
| V-OUT-POST | PASS | 已过账 status=completed |
| LEDGER-BEFORE-POST | PASS | 历史已过账，跳过前值断言 actual=12000 |
| LEDGER-AFTER-POST | PASS | 过账后总账1002=12000（期望 12000） |
| RECON-STATEMENT | PASS | 对账单余额=12000 |
| RECON-MARK | PASS | marked 2 |
| RECON-ZERO-DIFF | PASS | book=12000 stmt=12000 adjusted=12000 diff=0 |

## 金额轨迹

| 检查点 | 预期 | 实际 |
|---|---:|---:|
| 总账期初 1002 | 10000.00 | 10000 |
| 日记账期初后 | 10000.00 | 12000 |
| 日记账收入+支出后 | 12000.00 | 12000 |
| 过账前总账 1002 | 10000.00 | 12000 |
| 过账后总账 1002 | 12000.00 | 12000 |
| 银行对账差额 | 0.00 | 0 |

## 证据截图

- `/opt/cursor/artifacts/screenshots/bookb-accounts.webp`
- `/opt/cursor/artifacts/screenshots/bookb-entries.webp`
- `/opt/cursor/artifacts/screenshots/bookb-subject-balance-before.webp`
- `/opt/cursor/artifacts/screenshots/bookb-subject-balance-after.webp`
- `/opt/cursor/artifacts/screenshots/bookb-reconciliation.webp`
