# 往来管理（L1+L2+L3）

> 状态：**已实现**余额 / 明细 / 对账单 / 账龄；**已实现**开项核销（部分核销、建议匹配、撤销守卫）。账龄有核销走开项，否则 FIFO 估算。

## 1. 模块定位

按客户（辅助类型 `2`）/ 供应商（`3`）从已过账凭证辅助分录重建应收、应付；核销落库匹配表，不引入独立发票/收款单据。

## 2. 页面与路由

| 页面 | 路由 | 说明 |
|------|------|------|
| 应收应付余额 | `/arap/balance` | 下钻明细 / 核销 / 对账单 |
| 往来明细 | `/arap/detail` | 按往来单位 + 期间 |
| 账龄分析 | `/arap/aging` | 展示 `OPEN_ITEM` / `FIFO_ESTIMATE` |
| 核销工作台 | `/arap/writeoff` | 未清项勾对、建议、确认、撤销 |

菜单：`sql/seed/menus/arap_menus.sql`、`arap_writeoff_menu.sql`；表：`sql/seed/schema/arap_writeoff_tables.sql`。

## 3. 核心接口

| 方法 | 路径 |
|------|------|
| GET | `/api/arap/balance` `/detail` `/aging` `/statement/export` |
| GET | `/api/arap/writeoff/open-items` `/suggest` `/list` |
| POST | `/api/arap/writeoff/confirm` `/reverse/{id}` |

## 4. 已知缺口

- 银行余额调节、自动消账批处理、多币种、坏账流程：未做。
- 对账单 Excel 暂不强制标注核销状态。
