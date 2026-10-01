# AI UI §六：凭证校验 / 金额边角 / 批量 / 权限

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项C`（免审核）
- **bookId**：`2105453230146252802`
- **期间**：`2026-01`
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T02:46:20.538Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-section6.mjs`

## 结论：**PASS**（PASS 23 / FAIL 0 / WARN 0）

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
| AMT-1D2C | PASS | id=2105489370395971585 status=completed sender=true |
| AMT-2D1C | PASS | id=2105489371494879233 status=completed sender=true |
| AMT-001 | PASS | id=2105489372291796994 status=completed sender=true |
| AMT-12345 | PASS | id=2105489373017411586 status=completed sender=true |
| AMT-LARGE-DRAFT | PASS | draft only id=2105489373713666050（未过账） |
| AMT-BATCH-POSTED | PASS | id=2105489374326034433 status=completed sender=true |
| BATCH-SUBMIT | PASS | ids=2105489373864660993,2105489374032433153,2105489374326034433 msg=成功提交1条凭证, 忽略2条 |
| BATCH-POST | PASS | msg=操作总数：3; 成功：2; 失败：1 |
| BATCH-OUTCOMES | PASS | 0993:completed/sender=true; 3153:completed/sender=true; 4433:completed/sender=true |
| USER-CREATE | PASS | exists id=2105470937037012992 |
| USER-REVOKE-BOOK | PASS | no book C grant |
| PERM-BOOK-LIST | PASS | fetchAll books n=0 |
| PERM-SWITCH | PASS | switchBook denied: code=510021 You are not authorized to access this book. |
| PERM-VOUCHER-WRITE | PASS | draft code=500014 msg=Your role is not allowed to perform this action. |
| PERM-UI | PASS | uiHint=true |

## 观察

_无_

## 证据截图

- `/opt/cursor/artifacts/screenshots/s6-voucher-list.webp`
- `/opt/cursor/artifacts/screenshots/s6-batch.webp`
- `/opt/cursor/artifacts/screenshots/s6-limited-denied.webp`
