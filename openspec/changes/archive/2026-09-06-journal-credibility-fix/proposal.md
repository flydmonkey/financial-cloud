## Why

出纳日记账已有账户/流水/生成凭证骨架，但生成凭证科目占位无效、改流水不重算余额、账期与列表筛选断裂，导致「能登记却不可信、制证桥名存实亡」。在扩银行调节等能力之前，必须先把这条最小闭环修到可过账、可查账、余额可勾稽。

## What Changes

- 流水生成凭证时，按账户资金科目 + 流水对方科目正确生成借贷分录（填充真实科目 id/code/name）；缺科目、期初方向、已挂凭证时拒绝生成。
- 凭证日期与结账校验对齐流水 `tradeDate`（不再用「今天」）。
- 修改流水采用 B2：冲回旧影响、施加新影响，并按账户重算本笔及后续行的滚动余额；已挂凭证禁止改金额/方向/账户/科目；改交易日时校验新旧账期。
- 修复日记账列表筛选：前端绑定正确查询字段；后端按摘要等条件真正过滤；列表排序贴近业务日。
- **Non-goals**：银行余额调节、账户调拨、往来核销、凭证改回写流水、批量生成、打印/导入导出、现金盘点。

## Capabilities

### New Capabilities

- `cashier-journal`: 出纳日记账可信闭环——流水余额生命周期、按业务日的结账守卫、列表查询，以及流水→凭证的科目映射与拒绝规则。

### Modified Capabilities

- （无）现有 `openspec/specs/` 下无日记账能力规格；结账/反结账对日记账期初的既有要求不在本 change 修改。

## Impact

- 后端：`JournalEntryService`（save/update/delete/generateVoucher）、`JournalEntryMapper.xml` 查询与可能的重算查询、科目查询（`BookSubject`）依赖。
- 前端：`views/journal/journalentry.vue` 查询表单字段与可选筛选项。
- API 形状基本不变（仍为既有 CRUD + `generate-voucher`）；生成凭证的分录内容与 update 的余额副作用为行为变更。
- 测试：扩展/新增 `JournalEntryService` 单测与必要时的日记账 e2e 冒烟。
