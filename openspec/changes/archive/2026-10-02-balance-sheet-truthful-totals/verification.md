# 验收记录（2026-10-01）

## 已完成

- 后端：保留资产和负债及权益实际总计；返回 balanced、assetTotal、liabilityTotal、balanceDifference。缺失总计为 null。严格模式继续返回 513013，容差为绝对差额 <= 0.01。
- 前端：实际报表组件显示期末差额、真实总计及排查提示；浏览器打印同步提示；刷新/配置模式清理旧提示。
- 交付：静态核对 Excel 和 PDF 均使用 queryBalanceSheet 的实际金额。未对二进制导出增加提示文字。
- 业务回归：结转前核对报表差额与末级损益科目贷方净余额，分页读取分类为 6 的科目；多期结账、Golden 结转后、最终会计闭环继续要求严格平衡。
- CI：新增 `npm run test:e2e:balance-sheet-integrity`，在现有 main push E2E job 中运行。

## 验证结果

| 检查 | 结果 |
|---|---|
| `mvn -o '-Dtest=StatementBalanceSheetServiceTest,StatementBalanceSheetRulesTest' test` | 24 passed，0 failed |
| `npm run test:unit` | 23 passed，0 failed |
| `npm run typecheck` | 通过 |
| `npx eslint src/views/statement/balance-sheet.vue src/utils/tablePrint.ts` | 0 error，17 warning（现有未用变量等） |
| `npm run test:e2e:balance-sheet-integrity` | 7 passed：实际组件显示/打印/清理；正负/零差额；分页/父子去重；拒绝假平衡、错误元数据；保留结转后严格断言 |
| `npx playwright test --list` | 成功发现 43 个文件、191 项测试；仅发现检查，不代表全部执行 |
| `openspec validate balance-sheet-truthful-totals --strict` | 通过 |
| `git diff --check` | 通过（Git 提示 LF/CRLF 转换，无空白错误） |

## 验证边界

- Browser plugin 未提供，使用项目 Playwright 和 Chromium 挂载真实 Vue 报表组件，API 响应隔离；未写入任何财务数据。
- 本机浏览器阻止 Vite 热更新 WebSocket；页面 HTTP 加载、组件交互及打印验证通过，未出现 pageerror。该限制不影响本轮静态页面回归。
- 初次验证未运行完整业务 E2E；后续在隔离数据库完成复跑及修复，结果见 [复跑记录](rerun-report.md)。本次 CI 业务分组全部无失败；早期全量混跑中的其他失败未全部单独复核，不宣称所有 E2E 均通过。会计套件已验证导出接口返回可下载文件，但未逐项核对 Excel/PDF 文件内容。
- 原项目临时脚本、日志、数据目录保留。本次未提交、推送或部署。

- 后续新增小企业成本类 5401/5402（含子科目）核对：种子分类为 5，但属于利润表费用。专项最终为 8 passed。

## 继续修复后的验收

- 后端相关单测：46 passed（结账守卫 17、结转符号 5、报表服务 18、报表规则 6）。
- 会计套件：136 passed，0 failed，1 skipped；多期结账及会计闭环的 150/200 元差额不再出现。跳过项为缺少坏账科目的 BS-R04。
- 年末：2 passed；资产负债表 Golden：11 passed；利润表 Golden：6 passed，原 1,000 元差额已消除。
- 新增真实业务回归覆盖旧结转后新增收入拒绝结账、补充结转及混合正负费用余额，并纳入 accounting 脚本。
- typecheck、OpenSpec strict、diff check 通过；结转页 lint 0 error、16 个现有 warning。
- 研发规则幂等修复 SQL 只在隔离库应用；原业务库未应用。完整结转页新增「补充结转」入口。历史报表快照未批量重算。
