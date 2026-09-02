# 折旧明细表显示变动信息

## 范围

仅 **折旧明细表**（含导出）：勾选「显示变动信息」后展示期间内资产变动摘要。汇总表不做。

## 行为

- 参数 `includeChangeInfo`（默认 false）
- true 时批量查 `fixed_asset_change`（`year_period` ∈ [start,end]）+ items
- 行字段 `changeInfo`：`字段:前→后`，多条用 `；`；无 item 时用变动单 remark
- 小计/合计行为空；未勾选不查变动表

## 前端

- 勾选控制列与请求参数；导出同步
