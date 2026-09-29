# 固定资产盘盈入账 + 凭证结账参数页修边

**日期：** 2026-09-29  
**状态：** 已批准  

## 目标

1. 盘点完成后支持「盘盈入账」：按可编辑金额生成草稿凭证并更新/拆分资产卡片。  
2. 小幅修边「凭证与结账参数」页体验。

## 已拍板决策

| 项 | 决策 |
|----|------|
| 计价 | 默认 `原值 / 账面数量 × (实盘−账面)`，入账前可改 |
| 卡片 | 原卡数量=1 → 拆新卡；数量>1 → 原卡累加数量与原值 |
| 范围 | 仅已有卡片 `result=surplus`；不做账外新资产行 |
| 凭证 | 借固定资产，贷 `5301.04` 盘盈收益；草稿凭证 |
| 交互 | 与盘亏对称的一键入账 + 金额确认弹窗 |

## 非目标

- 账外资产录入、待处理财产损溢两段结转、自动过账  
- 部分盘亏自动拆分、按钮级权限铺开  

---

## A · 盘盈入账

### 流程

1. 盘点单状态为已完成。  
2. 「盘盈入账」→ `GET surplus-preview` 列出未入账盘盈行（默认金额、策略提示）。  
3. 用户可改金额；任一 ≤0 不可确认。  
4. `PUT book-surplus` 逐条处理（与盘亏相同：成功/跳过汇总，单条失败不拖死整批已成功项——实现上每条独立事务或 catch 汇总）。  
5. 数量=1：复制新卡（新编码、数量=盘盈量、原值=入账金额），原卡不变；数量>1：原卡 quantity / originalValue 累加。  
6. 写草稿凭证；明细落 `surplus_amount`、`surplus_voucher_id`、可选 `surplus_asset_id`。  
7. 已有 `surplus_voucher_id`、原卡已清理、科目缺失等 → 跳过或业务错误（科目缺失整条失败计入 skipped/failed）。

### API

前缀：`/api/fixed-asset/check`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/surplus-preview/{id}` | 盘盈预览行 |
| PUT | `/book-surplus/{id}` | body `[{ itemId, amount }]` |

### 数据

`fixed_asset_check_item` 新增：

- `surplus_amount` DECIMAL  
- `surplus_voucher_id` VARCHAR  
- `surplus_asset_id` VARCHAR NULL（拆新卡时）  

SQL：`sql/patches/2026-09-29-fixed-asset-surplus-booking.sql` + init 同步。

### 前端

`views/fixed-asset/check.vue`：完成态「盘盈入账」按钮、金额弹窗、结果摘要。  
API：`src/api/fixed-asset/check.ts`。

### 守卫

封存、开放账期与盘亏/`FixedAssetService` 写路径一致。

### 测试

- 默认均摊金额  
- 数量=1 拆新卡 + 凭证  
- 数量>1 原卡累加  
- 已入账跳过  

### 文档

更新 `07-fixed-asset.md`、`20-gap-analysis.md`、autonomous-iteration-backlog。

---

## C · 参数页修边

文件：`financial-cloud-ui/src/views/config/voucher-settlement.vue`

- 断号「月末结账」跳转到 `/settlement/settle-period`（向导），非 `settle-list`  
- 相对加载值无改动时禁用「保存」  
- 非管理员只读提示更醒目（alert 或固定提示条）  

无后端变更。

---

## 验收

- 有盘盈的已完成盘点单可预览默认金额、修改后入账，凭证与卡片正确。  
- 重复入账不重复建凭证。  
- 参数页跳转与脏状态行为符合上述三条。
