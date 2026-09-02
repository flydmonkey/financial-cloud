# 固定资产二期（折旧明细表 / 汇总表）— 设计说明

日期：2026-08-28  
状态：设计已确认，实现中  
范围：只读折旧明细表 + 折旧汇总表；**不含**资产变动记录

## 一、已确认决策

| # | 项 | 选择 |
|---|-----|------|
| 1 | 范围 | 折旧明细表 + 折旧汇总表 |
| 2 | 数据路径 | 读 `fixed_asset_depr` 流水 + `fixed_asset` 卡片补维 |
| 3 | 期间 | 月份区间；默认当前期～当前期 |
| 4 | 分组 | 明细默认按**类别**分组+小计；可选按部门再分组 |
| 5 | 变动信息 | 不做 |
| 6 | 导出 | 简单 Excel 导出 |

## 二、金额口径（单资产）

记 `start`/`end` 为查询起止期（`yyyy-MM`）。

| 字段 | 算法 |
|------|------|
| 原值 | `original_value` |
| 期初累计折旧 | `opening_accum_depr` + Σ(depr.year_period &lt; start) |
| 本期折旧 | Σ(start ≤ depr.year_period ≤ end) |
| 本年折旧额 | Σ(depr 落在 end 所在年 1 月～end) |
| 期末累计折旧 | 期初累计 + 本期折旧 |
| 期末减值准备 | `impairment` |
| 期末净值 | 原值 − 期末累计 − 减值 |

## 三、API

- `GET /api/fixed-asset/report/depreciation-detail`
- `GET /api/fixed-asset/report/depreciation-summary`
- `GET .../export`（可选同路径后缀）

参数：`startPeriod`、`endPeriod`、`includeDisposed`、`groupByDept`（仅明细）

## 四、前端与菜单

- `views/fixed-asset/depreciation-detail.vue`
- `views/fixed-asset/depreciation-summary.vue`
- 菜单：固定资产下「折旧明细表」「折旧汇总表」
- 计提成功页跳转明细表并带当前期

## 五、非目标

- 资产变动记录 / 变动单  
- 显示变动信息勾选  
- 加速折旧方法  
