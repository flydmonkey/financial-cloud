# 05 · 财务报表

> 状态：部分实现（资产负债、利润、现金流量、费用明细可用，Excel 导出为主；无 PDF、无老板极简报表页、三表数字不可下钻）

## 1. 模块定位

根据科目余额与取数规则自动生成合规报表，支持月度查询与导出；结账时生成期间快照。报表行项目与规则可在准则/账套配置层调整。

## 2. 典型场景

1. 月末结账后打开资产负债表核对资产=负债+权益。
2. 导出利润表用于所得税预缴参考。
3. 维护现金流量指定与间接法附表。
4. 用费用明细表与利润表交叉核对。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 资产负债表查询 / 导出 Excel | **已实现** | `balance-sheet.vue` |
| 利润表查询 / 导出 | **已实现** | `income-statement.vue` |
| 现金流量表（含间接法相关） | **已实现** | `cash-flow-statement.vue`；可指定/保存 |
| 费用明细表 | **已实现** | `expense-detail.vue` |
| 凭证汇总表 | **已实现** | 见账簿分册 |
| 按年/月筛选、刷新 | **已实现** | 各页顶部 |
| 报表取数规则配置 | **已实现** | `statement_rules`、config API |
| 资产负债/利润表行配置 | **已实现** | config + 准则模板 |
| PDF 导出 | **未实现** | — |
| 打印 | **部分实现** | 部分按钮注释 |
| 数字一键溯源到账簿/凭证 | **部分实现** | 三表无下钻；汇总/总账可到明细账 |
| 老板极简报表独立页 | **未实现** | 首页看板部分承接，待确认是否单列 |
| 季度切换产品化 | **部分实现** | 以月/年为主，视具体页筛选而定 |

## 4. 页面与路由

| 报表 | 路由 | 组件 |
|------|------|------|
| 资产负债表 | `/statement/balance-sheet` | `balance-sheet.vue` |
| 利润表 | `/statement/income-statement` | `income-statement.vue` |
| 现金流量表 | `/statement/cash-flow-statement` | `cash-flow-statement.vue` |
| 费用明细表 | `/statement/expense-detail` | `expense-detail.vue` |
| 凭证汇总 | `/statement/voucher-summary` | `voucher-summary.vue` |

配置类页面见 [02-basic-settings.md](02-basic-settings.md)（准则模板、规则、CF 期初）。

## 5. 数据模型

| 表 | 用途 |
|----|------|
| `statement_balance_sheet` / `statement_balance_sheet_item` | 资产负债快照与行配置 |
| `statement_income` / `statement_income_item` | 利润表 |
| `statement_cash_flow` | 现金流量 |
| `statement_subject_balance` | 科目余额 |
| `statement_rules` | 取数/重分类等规则 |
| `standard_statement_*` | 建账时复制的模板 |

种子规则：`sql/seed/rules/`（重分类、坏账、存货等）。

## 6. 核心接口

| 前缀 / 路径 | 说明 |
|-------------|------|
| `/api/statement/balance-sheet` (+ `/export`) | 资产负债表 |
| `/api/statement/income` (+ `/export`) | 利润表 |
| `/api/statement/cash-flow`、`/api/statement/cash-flow/*` | 现金流量查询与指定 |
| `/api/statement/expense-detail` | 费用明细 |
| `/api/statement/subject-balance`、`/voucher-summary`、`/general-ledger` | 余额、汇总、总账 |
| `/api/statement/config/*` | 资产负债/利润/规则配置 |

服务：`StatementBalanceSheetService`、`StatementIncomeService`、`StatementReportService`、`StatementExpenseDetailService`、`StatementCashFlowService`。

## 7. 业务规则与约束

1. **结账快照**：`SettlementService.checkout()` 调用报表服务写入期间快照。
2. **平衡与勾稽**：E2E 含三表/资产负债表行级/golden 数据集（`e2e/*reconciliation*`、`*-golden-dataset.spec.ts`）。
3. **现金流量**：支持手工指定与科目映射；详见模块技术文档。

## 8. 已知缺口

- PDF、正式打印、老板极简报表产品页。
- 报表单元格溯源。
- 「本月账本包」ZIP 一键导出：OpenSpec `daizhang-commercial-plan` / `monthly-books-pack` **待实现**。

## 9. 证据索引

- `controller/statement/*`、`service/statement/*`
- `views/statement/*.vue`
- [../modules/ledger-and-reports.md](../modules/ledger-and-reports.md)
- [../testing/financial-cloud-voucher-report-test-cases.md](../testing/financial-cloud-voucher-report-test-cases.md)
