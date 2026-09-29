# Design: dashboard-asset-count

## Context

首页已有多张 `FundDashboardService` 统计卡；固定资产在 `fixed_asset` 表，状态 `IN_USE` / `SUSPENDED` / `DISPOSED`。

## Goals

- 一眼展示在册资产总数与价值规模。
- 点击进卡片列表。

## Non-Goals

- 按期间折旧趋势图、盘点汇总卡、资产类别分布图。

## Decisions

1. **在册定义**：`status != DISPOSED`（含 null 视为在用），与盘点快照一致。
2. **净值**：`originalValue - accumDepr - impairment`（空按 0）。
3. **接口**：挂 `/api/statistics/fixed-asset-count`，bookId 取当前用户会话。
4. **UI**：紧凑数字卡，风格贴近既有卡片标题栏，无账期选择（点状时点库存）。

## Risks

- 无资产时全 0 — 可接受，卡片仍显示便于导航。
