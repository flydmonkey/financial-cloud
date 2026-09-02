# 固定资产模块

## 概述

固定资产模块覆盖资产类别、卡片、折旧计提（直线法/工作量法/加速折旧）、购入与清理凭证、暂停恢复、变动单及折旧报表。核心计算在 `FixedAssetDepreciationRules`；凭证生成复用 `VoucherService.save`。

## 已实现功能

- **资产类别**：编码账套内唯一；默认折旧方法、期数、残值率、科目
- **资产卡片**：状态 `IN_USE` / `SUSPENDED` / `DISPOSED`；次月起提、清理当月照提次月停
- **计提折旧**：期间汇总凭证；工作量法缺本期量整批失败；未审核可重提
- **购入/清理凭证**：新建卡片可生成购入凭证；清理生成累计折旧与损益凭证
- **暂停/恢复**：`suspended_period` 控制不计提期间
- **加速折旧**：双倍余额递减、年数总和法（寿命 ≥24 且整年）
- **变动单**：有折旧历史后计算字段仅能通过变动单修改
- **报表**：折旧明细表、汇总表（只读 `fixed_asset_depr`）
- **复制/导入/导出**：复制重置累计与凭证 ID；导入编码冲突跳过

## 数据与 SQL

**表：** `asset_category`、`fixed_asset`、`fixed_asset_work`、`fixed_asset_depr`、`fixed_asset_accrual`、`fixed_asset_change`、`fixed_asset_change_item`

**初始化：** 全量 init 已内嵌（`sql/seed/schema/` + `sql/seed/menus/fixed_asset_menu.sql`）。新环境执行：

```bash
python tools/run_init_sql.py
```

**后端包：** `com.financial.cloud.domain.fixedasset`、`service.fixedasset`、`controller.fixedasset`

**前端：** `financial-cloud-ui/src/views/fixed-asset/`、`api/fixed-asset/`

## API 要点

前缀 `/api/fixed-asset/`，`bookId` 由 `@CurrentUser` 注入。

| 区域 | 路径 |
|------|------|
| 类别 | `GET/POST/PUT/DELETE .../category/fetch\|save\|update\|delete` |
| 卡片 | `.../card/fetch\|save\|update\|delete`、`POST .../card/dispose\|suspend\|resume/{id}` |
| 复制/IO | `POST .../card/copy/{id}`、`GET .../card/export\|import-template`、`POST .../card/import` |
| 计提 | `GET/PUT .../depreciation/work\|params`、`POST .../depreciation/accrue` |
| 变动 | `GET .../change/fetch`、`POST .../change/save` |
| 报表 | `GET .../report/depreciation-detail\|depreciation-summary` |

错误码段：`514001`–`514020`。

## 未实现 / 二期

- 变动单驱动会计凭证（变动仅审计与前瞻重算）
- 更新改造转入在建工程完整流转
- 从采购单据自动归集原值
- 历史折旧回溯重算
