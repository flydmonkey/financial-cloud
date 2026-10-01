# AI UI §六：凭证校验 / 金额边角 / 批量 / 权限

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项C`（免审核）
- **bookId**：`2105453230146252802`
- **期间**：`2026-01`
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T01:33:40.648Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-section6.mjs`

## 结论：**PASS**（PASS 22 / FAIL 0 / WARN 1）

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin |
| SUBJECTS | PASS | bank=1002 expense=5602.01 revenue=5001 |
| VAL-UNBALANCED | PASS | code=2 msg=借贷不平衡 persistedΔ=0 |
| VAL-NO-SUBJECT | PASS | code=2 msg=存在未选择科目的分录 persistedΔ=0 |
| VAL-ZERO | PASS | code=2 msg=存在未填写金额的分录 persistedΔ=0 |
| VAL-SINGLE | PASS | code=2 msg=至少需要两条分录 persistedΔ=0 |
| VAL-OUT-TERM | PASS | code=2 msg=已结账期间不允许新增或修改凭证（当前开放账期 2026-01） persistedΔ=0 |
| VAL-NO-SUMMARY | PASS | API 拒绝: 请至少输入一项摘要 |
| AMT-1D2C | PASS | id=2105471135730675713 status=completed sender=true |
| AMT-2D1C | PASS | id=2105471136045248513 status=completed sender=true |
| AMT-001 | PASS | id=2105471136351432706 status=completed sender=true |
| AMT-12345 | PASS | id=2105471136674394114 status=completed sender=true |
| AMT-LARGE-DRAFT | PASS | draft only id=2105471136976384001（未过账） |
| AMT-BATCH-POSTED | PASS | id=2105471137257402370 status=completed sender=true |
| BATCH-SUBMIT | PASS | ids=2105471137051881473,2105471137127378946,2105471137257402370 msg=成功提交1条凭证, 忽略2条 |
| BATCH-POST | PASS | msg=操作总数：3; 成功：2; 失败：1 |
| BATCH-OUTCOMES | PASS | 1473:completed/sender=true; 8946:completed/sender=true; 2370:completed/sender=true |
| USER-CREATE | PASS | exists id=2105470937037012992 |
| USER-REVOKE-BOOK | PASS | no book C grant |
| PERM-BOOK-LIST | PASS | fetchAll books n=0 |
| PERM-SWITCH | WARN | switchBook 成功但无账套授权（OBS-PERM-SWITCH-NO-GRANT） |
| PERM-VOUCHER-WRITE | PASS | draft code=500014 msg=Your role is not allowed to perform this action. |
| PERM-UI | PASS | uiHint=true |

## 观察

- OBS-PERM-SWITCH-NO-GRANT：无 permission_book 授权时 switchBook 仍成功写入 current bookId
- UI 证据路由：凭证列表应为 `/voucher/voucher-index`（曾误用 `/voucher/voucher` 导致 404；已修正脚本并重截图）

## 证据截图

- `/opt/cursor/artifacts/screenshots/s6-voucher-list.webp`（专项 C 凭证管理：边角金额/大额草稿/已过账）
- `/opt/cursor/artifacts/screenshots/s6-batch.webp`
- `/opt/cursor/artifacts/screenshots/s6-limited-denied.webp`（受限用户 → onboarding 创建账套）
