# Proposal: 首页固定资产统计卡片

## Why

PRD 首页要求「资产总数」专项卡片；当前看板仅有资金/往来/利润等，固定资产仅出现在待办折旧项，代账客户缺少一眼可见的在册资产规模。

## What Changes

- 新增 `GET /api/statistics/fixed-asset-count`：当前账套在册资产张数（排除已清理）、使用中/暂停计提拆分、原值合计、净值合计。
- 首页增加「固定资产」统计卡片，可点击跳转资产卡片列表。
- 快捷入口增加「固定资产」。
- 文档/gap 对齐。

## Capabilities

### New Capabilities

- `dashboard-fixed-asset`: 首页固定资产规模统计。

### Modified Capabilities

（无）

## Impact

- `FundDashboardController` / 新 VO / 前端 `index.vue` + 卡片组件；`docs/product/07`、gap §5/§9。
