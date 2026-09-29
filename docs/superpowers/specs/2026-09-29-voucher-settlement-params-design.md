# 凭证与结账参数独立页

**日期：** 2026-09-29  
**状态：** 已实施

## 目标

基础设置下提供「凭证与结账参数」页：账套管理员可改凭证审核开关；可关闭月结校验中的往来账龄软提示；硬闸只读说明并跳转月结向导。

## 范围

- 读写 `book.voucherReviewed`（与账套编辑同一字段）。
- 账套 config `settlement.verify.arap.enabled`（默认 `true`）；为 `false` 时 `SettlementService.evaluateHardGates` 不附加往来检查。
- 硬闸（未过账、断号、借贷、必做结转、折旧）不可配置。
- 断号整理仅说明 + 跳转，不在本页执行。

## 非目标

账期/科目编码/默认科目、硬闸可关、税局/工资条。

## 接口

- `GET /api/config/voucher-settlement`：当前账套审核开关、往来软提示开关、是否可编辑。
- `PUT /api/config/voucher-settlement`：账套管理员保存上述两项；封存账套拒绝。

## 权限

菜单对账套角色可见；保存仅账套管理员。
