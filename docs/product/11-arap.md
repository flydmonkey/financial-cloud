# 往来管理（L1+L2）

> 状态：**已实现**余额 / 明细 / 对账单 Excel / 账龄 FIFO；月结系统摘要已接入。**核销 L3 未做**。

## 1. 模块定位

按客户（辅助类型 `2`）/ 供应商（`3`）从已过账凭证辅助分录重建应收、应付余额与明细，支持对账单导出与账龄分桶（0–30 / 31–60 / 61–90 / 91–180 / 180+）。科目根经 `SubjectCodeCompat`（如 1122、2202 及准则别名）。

## 2. 页面与路由

| 页面 | 路由 | 说明 |
|------|------|------|
| 应收应付余额 | `/arap/balance` | 切换 AR/AP；行点击进明细；导出对账单 |
| 往来明细 | `/arap/detail` | 按往来单位 + 期间；支持 URL 参数下钻 |
| 账龄分析 | `/arap/aging` | FIFO 估算；UI 标明非核销账龄 |

菜单种子：`sql/seed/menus/arap_menus.sql`（需执行后赋予角色权限）。

## 3. 核心接口

| 方法 | 路径 |
|------|------|
| GET | `/api/arap/balance` |
| GET | `/api/arap/detail` |
| GET | `/api/arap/aging` |
| GET | `/api/arap/statement/export` |

服务：`ArapService`、`ArapAgingCalculator`、`ArapRules`。

## 4. 已知缺口

- 逐笔核销、开项匹配、收款/付款业务单据：**未做（L3）**。
- 银行余额调节、进销存：不在本模块。
