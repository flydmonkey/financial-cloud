# AI UI 剩余细项：工资守卫 / 固资生命周期 / 辅助必填 / 待办下钻

- **标识**：`AI-UI-20260930`
- **账套**：专项 B `2105448444973871105`（工资/固资）；工作台关注 2026-01
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T02:49:36.273Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-remaining-gaps.mjs`

## 结论：**PASS**（PASS 18 / FAIL 0 / WARN 2）

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin |
| PAY-BANK-EXPORT-DENY | PASS | code=504007 msg=These employees are missing bank account numbers; fix them before export: AI-UI-20260930-工资员 |
| PAY-REGEN-BLOCK | PASS | code=2 msg=该员工本月已生成计提凭证，请勿重复生成 |
| PAY-REPUSH-BLOCK | PASS | code=2 msg=本月工资明细已生成计提或发放凭证，请先在工资明细中删除对应凭证后再重新推送 |
| PAY-BASE-MISSING | WARN | 清空 payBaseNumber 后仍算薪 base=4800（无硬拦截，走回退） |
| PAY-PARTTIME | PASS | 已存在 id=2105475505536937985 type=PARTTIME |
| PAY-MONTH-CLOSE-MUTEX | WARN | 未单独强制测月结工资计提互斥（保留为风险项） |
| FA-SUSPEND | PASS | code=0 msg=已暂停计提 |
| FA-SUSPEND-STATE | PASS | status=SUSPENDED period=2026-01 |
| FA-CHANGE-LOG | PASS | change rows=10 |
| FA-RESUME | PASS | code=0 msg=已恢复计提 |
| FA-COPY | PASS | code=0 newId=2105490237287317505 |
| FA-COPY-STATE | PASS | code=B-ASSET-SUR-BUMP-副本4 status=IN_USE |
| AUX-API-DRAFT | PASS | API 拒绝: 存在未选择辅助核算的分录（客户） |
| AUX-UI-PAGE | PASS | voucher-edit loaded |
| AUX-UI-HINT | PASS | UI 凭证编辑含辅助核算列；提交路径 checkAuxiliary+must（见 voucher-edit.vue） |
| WB-PENDING-AUDIT | PASS | pendingAudit=1 blocker=AUDIT close=OPEN |
| WB-AUDIT-DRILL | PASS | url=http://127.0.0.1:3154/voucher/voucher-index |
| PERM-LIMITED-CONFIG | PASS | fetchAll n=0 updateByKey code=510021 You are not authorized to access this book. |
| PERM-ADMIN-CONFIG | PASS | admin config/sys/books code=0 n=7 |

## 观察

- OBS-PAY-BASE-FALLBACK（产品设计）：非自定义规则回退账套默认基数；见 `SalaryContributionBaseRules`
- PAY-MONTH-CLOSE-MUTEX：明细已计提时月结 `jt_gz` 互斥已在 final-gaps 覆盖；本脚本不强制重复生成
- 固资复制卡保留：id=2105490237287317505 code=B-ASSET-SUR-BUMP-副本4
- 复跑修复：恢复 Jan 计提/发放凭证 FK；`generateVoucher` 优先校验行内 FK 并清理失效指针

## 证据截图

- `/opt/cursor/artifacts/screenshots/gap-workbench-audit.webp`
- `/opt/cursor/artifacts/screenshots/gap-fa-lifecycle.webp`
- `/opt/cursor/artifacts/screenshots/gap-aux-ui.webp`
- `/opt/cursor/artifacts/screenshots/gap-payroll.webp`
