# 账簿与报表模块

## 概述

账簿与报表模块包含汇总型总账、费用明细矩阵表、侧栏「账簿」菜单重组，以及现金流量表间接法附表。总账与科目余额表同源 `statement_subject_balance`；费用明细与利润表 5601/5602/5603 逐月勾稽。

## 已实现功能

### 总账

- 每科目三行：期初余额 / 本期合计 / 本年累计
- 过滤：起止科目、级次、辅助核算、隐藏无发生且余额为 0
- 科目编码下钻明细账；Excel 导出；仅已过账数据

### 费用明细表

- 科目树 × 多月份矩阵 + 年度合计列
- 默认科目 5601/5602/5603；与利润表逐月勾稽
- 区间上限 24 月；Excel 导出

### 账簿菜单

顶级「账簿」（`sort_index=3`）下挂：明细账 → 总账 → 科目余额表。费用明细表仍在「财务报表」下。

| 菜单 | Resource ID | 路径 |
|------|-------------|------|
| 账簿 | `2026082817000000001` | — |
| 明细账 | `1903024792422047745` | `/voucher/sub-ledger` |
| 总账 | `2026082816300000001` | `/statement/general-ledger` |
| 科目余额表 | `1886384516205912065` | `/statement/subject-balance` |
| 费用明细 | `2026082814300000001` | `/statement/expense-detail` |

### 现金流量表间接法

- 附表自动取数；strict 模式：`financial-cloud.statement.cash-flow.strict-reconciliation`

## 数据与 SQL

| 依赖 | 说明 |
|------|------|
| `statement_subject_balance` | 总账期初/本期/YTD |
| `book_subject` | 科目元数据 |
| `voucher` / `voucher_item` | 费用明细取数 |
| `resources` / `permission` | 菜单 |

**菜单 seed：** `sql/seed/menus/ledger_books_menu.sql`、`general_ledger_menu.sql`、`expense_detail_menu.sql`（已内嵌全量 init）

**初始化：**

```bash
python tools/run_init_sql.py
```

## API 要点

| 方法 | 路径 |
|------|------|
| GET | `/api/statement/general-ledger` |
| GET | `/api/statement/general-ledger/export` |
| GET | `/api/statement/expense-detail` |
| GET | `/api/statement/expense-detail/export` |

共用参数（`StatementParamsDto`）：`periodType`、`dateRange`、`subjectCodeFrom/To`、`maxLevel` 等。

## 未实现 / 二期

- **所有者权益变动表**：设计已完成，代码未实现（`/api/statement/owner-equity` 待定）
- 总账：凭证级流水、多币别、未过账纳入
- 费用明细：图表、同比、辅助核算展开、strict 勾稽拦截
