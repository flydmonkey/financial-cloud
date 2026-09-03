# 04 · 账簿管理

> 状态：部分实现（明细账、总账、科目余额表、凭证汇总可用；无多栏账/数量金额账；溯源链路不完整）

## 1. 模块定位

凭证过账后形成可查询账簿，用于对账、查账与合规核查。菜单上「账簿」与「财务报表」分组并存（见种子菜单重组）。

## 2. 典型场景

1. 按科目打开明细账，核对某科目本期发生与余额。
2. 从总账点击科目编码下钻到明细账。
3. 导出科目余额表做月度核对。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 明细账 | **已实现** | `views/voucher/sub-ledger.vue`；数据来自凭证分录查询 |
| 总账 | **已实现** | `views/statement/general-ledger.vue`；`StatementGeneralLedgerService` |
| 科目余额表 | **已实现** | `views/statement/subject-balance.vue` |
| 凭证汇总表 | **已实现** | `views/statement/voucher-summary.vue`（偏报表，常与账簿共用） |
| 按期间 / 科目筛选 | **已实现** | 各页顶部筛选 |
| Excel 导出 | **已实现** | 总账、余额表、汇总表等 |
| PDF 导出 | **未实现** | — |
| 凭证号点击跳转凭证详情 | **未实现** | 明细账 `word` 列为纯文本 |
| 总账 → 明细账 | **已实现** | `goSubLedger()` |
| 凭证汇总 → 明细账 | **已实现** | `router-link` 带 subjectCode |
| 多栏账 | **未实现** | 无页面/API |
| 数量金额账 | **未实现** | `voucher_item` 有 num/price，无专用账簿 UI |

## 4. 页面与路由

| 页面 | 路由 | 组件 |
|------|------|------|
| 明细账 | `/voucher/sub-ledger` | `views/voucher/sub-ledger.vue` |
| 总账 | `/statement/general-ledger` | `views/statement/general-ledger.vue` |
| 科目余额表 | `/statement/subject-balance` | `views/statement/subject-balance.vue` |
| 凭证汇总表 | `/statement/voucher-summary` | `views/statement/voucher-summary.vue` |

技术补充：[../modules/ledger-and-reports.md](../modules/ledger-and-reports.md)。

## 5. 数据模型

| 来源 | 说明 |
|------|------|
| `voucher` + `voucher_item` | 明细账行级数据 |
| `statement_subject_balance` | 科目余额快照/查询 |
| 过账逻辑 | `VoucherService` / `StatementSubjectBalanceService.update` |

明细账字段语义（产品侧）：日期、凭证号、摘要、借方、贷方、余额、方向。

## 6. 核心接口

| 能力 | 接口线索 |
|------|----------|
| 明细账 | 凭证 items 查询（如 `/api/voucher/items/fetch`）及前端封装 |
| 总账 | `/api/statement/general-ledger`（`StatementReportController` / `StatementGeneralLedgerService`） |
| 科目余额 | `/api/statement/subject-balance` 或 `/api/statement/subject-balance/get` |
| 凭证汇总 | `/api/statement/voucher-summary` |
| 导出 | 对应路径下 `/export` |

前端：`src/api/statement/*.ts`、`src/api/voucher/voucher.ts`。

## 7. 业务规则与约束

1. 账簿数据依赖**已过账**凭证与期初；未过账不进入余额更新主路径。
2. 结账会固化科目余额等快照（见结算模块）。
3. 溯源：总账/汇总可到明细账；**明细账不到凭证详情**；三表数字下钻见报表分册。

## 8. 已知缺口

- 多栏账、数量金额账。
- 明细账凭证号超链接到 `/voucher/voucher-edit?id=&readonly=1`。
- PDF / 打印产品化（部分按钮注释或未接）。

## 9. 证据索引

- `StatementGeneralLedgerService`、`StatementSubjectBalanceService`、`StatementReportService`
- `views/voucher/sub-ledger.vue`、`views/statement/general-ledger.vue`
