# 固定资产三期（资产变动记录）— 设计说明

日期：2026-08-28  
状态：设计已确认，实现中  
范围：变动单 + 审计列表 + 计算字段前瞻重算；不含变动会计凭证

## 一、决策

| # | 项 | 选择 |
|---|-----|------|
| 1 | 形态 | 变动单头表 + 明细行 |
| 2 | 能力 | 审计列表 + 计算类变动前瞻重算 |
| 3 | 锁 | 有 depr 后计算字段仅能通过变动单修改 |
| 4 | 非计算字段 | 卡片直接保存时自动记流水 |

## 二、表

- `fixed_asset_change`：book_id, asset_id, year_period, remark, auditor fields  
- `fixed_asset_change_item`：change_id, field_code, field_label, before_value, after_value  

## 三、重算

不回溯 `fixed_asset_depr`。直线法剩余月折旧 = (新原值−减值−新残值−当前累计)÷剩余月份。工作量法按新基数。NONE 之后不计提。

## 四、API / UI

- `GET /api/fixed-asset/change/fetch`  
- `POST /api/fixed-asset/change/save`  
- 页面：`views/fixed-asset/change-log.vue`；卡片「变动」弹窗  
- 菜单：资产变动记录  
