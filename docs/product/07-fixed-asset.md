# 07 · 固定资产

> 状态：已实现（卡片、类别、折旧、变动、清理、报表、盘点含盘盈入账；购入凭证有条件生成）

## 1. 模块定位

固定资产全生命周期：类别与卡片、折旧计提（含工作量法参数）、变动记录、清理处置及折旧报表。购入/清理可联动生成凭证。

技术细节见 [../modules/fixed-assets.md](../modules/fixed-assets.md)。

## 2. 典型场景

1. 维护资产类别默认残值率与年限，录入办公设备卡片。
2. 月末计提折旧并生成折旧凭证。
3. 资产报废/出售走清理流程，自动生成清理凭证。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 资产类别 CRUD | **已实现** | `category.vue` |
| 资产卡片 CRUD / 复制 | **已实现** | `card.vue` |
| 导入 / 导出卡片 Excel | **已实现** | import/export API |
| 暂停 / 恢复折旧 | **已实现** | `suspend` / `resume` |
| 购入自动生成入账凭证 | **已实现** | `FixedAssetPurchaseRules`：原值+税额贷方合计 >0 才生成；否则成功消息说明未生成原因 |
| 折旧参数与工作量录入 | **已实现** | `depreciation.vue` |
| 一键计提折旧生成凭证 | **已实现** | `POST .../accrue` |
| 折旧明细表 / 汇总表及导出 | **已实现** | report 页 |
| 资产变动记录 | **已实现** | `change-log.vue` + change API |
| 资产清理（报废等）生成凭证 | **已实现** | `dispose` → `createDisposeVoucher` |
| 资产盘点 / 盘点表 | **已实现** | `FixedAssetCheckService`；盘亏可下账生成凭证；**盘盈入账**（预览均摊金额可改→草稿凭证借 1601 贷 5301.04，qty=1 拆新卡 / qty>1 累加原卡） |
| 首页「资产总数」专项卡片 | **已实现** | 首页「固定资产」卡：在册张数/原值/净值；`/api/statistics/fixed-asset-count` |

## 4. 页面与路由

| 页面 | 路由 | 组件 |
|------|------|------|
| 卡片 | `/fixed-asset/card` | `card.vue` |
| 资产类别 | `/fixed-asset/category` | `category.vue` |
| 计提折旧 | `/fixed-asset/depreciation` | `depreciation.vue` |
| 折旧明细表 | `/fixed-asset/depreciation-detail` | `depreciation-detail.vue` |
| 折旧汇总表 | `/fixed-asset/depreciation-summary` | `depreciation-summary.vue` |
| 资产变动记录 | `/fixed-asset/change-log` | `change-log.vue` |

## 5. 数据模型

| 表 | 用途 |
|----|------|
| `asset_category` | 类别 |
| `fixed_asset` | 卡片（原值、残值、年限、部门、状态等） |
| `fixed_asset_work` | 工作量法本期工作量 |
| `fixed_asset_accrual` / `fixed_asset_depr` | 计提头 / 折旧明细 |
| `fixed_asset_change` / `fixed_asset_change_item` | 变动单 |

折旧方法：平均年限法、工作量法、加速折旧等（服务与枚举支持，以卡片配置为准）。

## 6. 核心接口

| 前缀 | 说明 |
|------|------|
| `/api/fixed-asset/card` | 卡片 CRUD、导入导出、suspend/resume/dispose |
| `/api/fixed-asset/category` | 类别 |
| `/api/fixed-asset/depreciation` | 状态、参数、工作量、accrue |
| `/api/fixed-asset/change` | 变动 |
| `/api/fixed-asset/report` | 折旧明细/汇总及 export |

## 7. 业务规则与约束

1. 购入凭证：`FixedAssetPurchaseRules.shouldCreateVoucher`；零原值且零税额时不生成，接口消息返回 `skipVoucherReason`。
2. 清理走 `dispose`，生成清理凭证并更新资产状态。
3. 折旧与结账期间配合：通常在开放账期计提。
4. 盘盈 `bump_qty`（账面数量 >1）：仅允许**未计提折旧**的原卡累加数量/原值；已有累计折旧或已提期数时预览警告且入账跳过，须将账面数量改为 1 后拆新卡。

## 8. 已知缺口

- 盘亏整件可下账生成清理凭证（盘盈入账见上表，仅 `result=surplus` 且未入账行）。
- 变动后折旧重算的产品说明可再加强（以服务实现为准）。
- **升级说明**：已有数据库必须先执行 `sql/patches/2026-09-29-fixed-asset-surplus-booking.sql`（幂等，为 `fixed_asset_check_item` 补盘盈入账三列）；新库由 init 脚本自带。
- **盘盈列表按钮**：仅当仍有未入账盘盈明细（`pendingSurplusCount`）时显示「盘盈入账」；全部入账后按钮隐藏。

## 9. 证据索引

- `FixedAssetService`、`FixedAssetDepreciationService`、`FixedAssetController`
- `views/fixed-asset/*`
- [../modules/fixed-assets.md](../modules/fixed-assets.md)
