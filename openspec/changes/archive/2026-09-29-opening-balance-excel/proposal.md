## Why

代账建账后录入期初是高频阻塞点：科目树逐格点选慢，客户常自带 Excel 余额表。页面已有试算平衡与保存，但缺少与固定资产同等的 Excel 导出/模板/导入通道，接手吞吐明显落后于专业代账软件预期。

## What Changes

- 期初余额支持 **导出当前账套科目期初 Excel**（编码、名称、方向、年初借贷、累计发生、余额）。
- 提供 **导入模板下载** 与 **Excel 导入**：按科目编码匹配，仅更新可编辑末级科目（无凭证占用、初始化未完成）；返回成功/失败行明细。
- 前端期初页增加导出 / 下载模板 / 导入入口；导入成功后刷新列表并可触发试算平衡提示。
- 权限与写守卫对齐现有期初保存（初始化完成后拒绝写入）。

## Capabilities

### New Capabilities

- `opening-balance-excel`：账套期初余额的 Excel 导出、模板与按编码导入。

### Modified Capabilities

- （无）

## Impact

- **后端**：`BookInitBalanceController` / `BookInitBalanceService` 增 export、import-template、import；新增导入结果 DTO。
- **前端**：`initBalance/index.vue` + `api/config/bookInitBalance.ts`。
- **非目标**：现金流量期初 Excel、跨账套合并、服务端 PDF、改科目树结构。
