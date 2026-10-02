# AI UI 专项账套 C（关闭凭证审核）测试报告

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项C`
- **bookId**：`2105453230146252802`
- **启用期间**：`2026-01`（凭证审核**关闭** voucherReviewed=0）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T00:27:50.243Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-book-c.mjs`

## 结论：**PASS**（PASS 20 / FAIL 0 / WARN 0）

## 状态流转（实际观察）

- draft: status=draft sender=N
- draft: status=draft sender=N
- submit: status=completed sender=N auditor=-
- post: status=completed sender=Y
- unsender: status=completed sender=N
- unaudit: status=draft sender=N

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin（reviewer 按关闭审核流程可不参与） |
| BOOK-FIND | PASS | 已存在 bookId=2105453230146252802 voucherReviewed=0 |
| SWITCH-BOOK | PASS | bookId=2105453230146252802 jwtBook=2105453230146252802 term预期=2026-01 |
| BOOK-REVIEW-FLAG | PASS | voucherReviewed=0 |
| TERM-CURRENT | PASS | 当前账期=2026-01 |
| SUBJECTS | PASS | bank=1002 capital=3001 expense=5602.01 |
| LEDGER-OPENING | PASS | 总账期初已是银行/资本 10000 |
| BAL-OPEN | PASS | 期初后银行=10000 |
| V-DRAFT | PASS | voucherId=2105454413623652353 status=draft |
| V-SUBMIT | PASS | status=completed sender=false auditor=- |
| V-AUDIT-SKIP | PASS | 已是 completed，无需审核人 |
| BAL-AFTER-SUBMIT | PASS | 提交未过账银行=10000（期望 10000） expense=0 |
| V-POST | PASS | status=completed sender=true |
| BAL-AFTER-POST | PASS | 过账后银行=9900（期望 9900） expense=100 |
| UI-REVERSE-POSTED | PASS | phase=posted 反过账下拉=true 反审核下拉=true 工具栏审核=false voucher=2105454413623652353 |
| V-UNSENDER | PASS | code=0 操作总数：1; 成功：1; 失败：0 status=completed sender=false |
| BAL-AFTER-UNSENDER | PASS | 反过账后银行=10000（期望 10000） |
| UI-REVERSE-UNPOSTED | PASS | phase=unposted 反过账下拉=false 反审核下拉=true 工具栏审核=false voucher=2105454413623652353 |
| V-UNAUDIT | PASS | code=0 操作总数：1; 成功：1; 失败：0 → status=draft（audit-off 期望 draft） |
| V-CLEANUP | PASS | delete draft code=0 删除成功 |

## 余额 / 三表检查点

| 检查点 | 预期 | 实际 |
|---|---:|---:|
| 期初后银行 1002 | 10000.00 | 10000 |
| 提交未过账银行 | 10000.00 | 10000 |
| 过账后银行 | 9900.00 | 9900 |
| 反过账后银行 | 10000.00 | 10000 |
| 提交未过账费用(利润表) | 不变/0 | 0 |
| 过账后费用(利润表) | 100.00 或有发生 | 100 |

## 产品观察（audit-off 是否仍要审核人）

- 未发现关闭审核后仍强制审核人的路径；提交后直接 `completed`，admin 可过账，无需 `ai_reviewer`。
- 反审核（audit-off）将 `completed` 退回 `draft`（非 `reviewing`）。
- UI：反审核/反过账在「审核」「过账」split-button 下拉中可用。
- **OBS-BOOK-TERM-INHERIT**：新建账套时 `sys.payment.term.current` 曾为 `2026-03`（启用月为 `2026-01`），疑似继承创建者所在账套账期；脚本强制回启用月后反过账才可用。

## 证据截图

- `/opt/cursor/artifacts/screenshots/bookc-voucher-list-posted.webp`
- `/opt/cursor/artifacts/screenshots/bookc-voucher-list-unposted.webp`
- `/opt/cursor/artifacts/screenshots/bookc-subject-balance-before.webp`
- `/opt/cursor/artifacts/screenshots/bookc-subject-balance-after-post.webp`
- `/opt/cursor/artifacts/screenshots/bookc-subject-balance-after-unsender.webp`
- `/opt/cursor/artifacts/screenshots/bookc-settings.webp`
