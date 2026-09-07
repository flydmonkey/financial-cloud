## Why

代账与迁账场景需要批量迁入历史凭证，并在系统间按筛选条件导出后可再导入。当前凭证列表仅有分页感知的 Excel 导出、没有导入与模板下载，无法形成「导出 → 改表/迁移 → 导入」闭环，手工补录成本高。

## What Changes

- 新增凭证 Excel **导入**：下载统一模板、上传 xlsx 批量**新建**凭证；一律落为 **draft（暂存）**，不写入审核/过账。
- **字号**：Excel 提供字头+字号且当期账套内不冲突则采用；空值或冲突则按现有 `able-word-num` 自动分配。
- **增强导出**：按列表当前筛选条件导出**全量**匹配凭证（不受分页限制）；导出文件与导入模板列结构互认，支持再导入。
- 列表「更多」增加「下载模板」「导入」；保留「导出」。
- **非目标**：附件文件、辅助核算/现金流量列往返、导入直接审核或过账、按 id 更新已有凭证、PDF 导出。

## Capabilities

### New Capabilities
- `voucher-import-export`: 凭证 Excel 统一模板的筛选全量导出、模板下载与批量导入（暂存、字号可沿用或自动分配）

### Modified Capabilities
- （无；既有 main specs 无凭证导入导出能力定义）

## Impact

- 后端：`VoucherController` / `VoucherService` — 新增 `GET /import-template`、`POST /import`；增强现有 `GET /export` 为筛选全量导出；复用 `ExcelImport` / `ExcelExporter` 与固定资产导入结果形态
- 前端：`voucher-index.vue`「更多」菜单、`api/voucher/voucher.ts`、复用 `ImportUpload`
- 模板：统一/对齐 `static/export-template/template-voucher.xlsx`（或同构导入模板）
- 不改变凭证状态机、结账与过账规则；导入仅走 draft 创建语义
