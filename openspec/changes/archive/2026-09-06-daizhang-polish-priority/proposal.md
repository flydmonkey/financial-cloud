## Why

财务云已具备代账可用的总账骨架（凭证→结转→结账→三表），但产品身份与打磨顺序未写清；月结后无法反结账，代账改错只能将就。需要先钉「代账 × 小企业准则 × 打磨现有」的边界与优先级，并在 P0 补齐反结账与日记账期初可逆，再谈交付与资金往来减负。

## What Changes

- 明确产品身份：面向**代账公司**的**小企业会计准则**总账工作台；策略为打磨现有月结闭环，而非堆进销存/税连。
- 采纳优先级草案 v0.1：`P0 信任 → P1 吞吐与交付 → P2 资金往来 → P3 周边/卫生`，并列出 Non-goals。
- **P0（本 change 首期实现范围）**：
  - 新增反结账：只反最近已结月；下期无凭证且无日记账流水；与结账同权；结账列表 + 输入账期确认；settlement 逻辑删；不级联凭证/结转。
  - 日记账账户增加 `prev_opening_balance`：结账先备份再 `opening:=balance`，反结账写回；无快照则拒绝反结。
  - 维持并强化样账勾稽门禁（含 3103/3104 权益核对、结转小企业编码纯净方向）。
- **P1–P3**：在 design/tasks 中排期，不作为本 change 一次实现完（打印/账本包、银行调节、核销账龄、附件、税费向导、周边防炸等）。
- **Non-goals（写死）**：进销存、税局直连/全电票归集、所有者权益变动表、第二准则同等深度、驳回状态机、跳期反结账、固资变动驱动凭证等二期深挖。
- 结账自查文案：不做的能力改为「请外部完成」，避免假齐全（P1 轻量项）。

## Capabilities

### New Capabilities

- `settlement-uncheckout`：反结账 API/规则、报表与科目余额回滚、与结账对称的事务语义；含日记账 `prev_opening_balance` 备份与恢复。

### Modified Capabilities

- （无：仓库尚无 `openspec/specs/` 基线；后续若沉淀主 specs，再对凭证/结账能力做 delta。）

## Impact

- **后端**：`SettlementService` / `SettlementController`；`JournalAccount` 及 mapper checkout；科目余额下期行删除；利润表/资产负债表结账快照清理；`ConfigSysService` 账期回退；DB 迁移加 `prev_opening_balance`。
- **前端**：结账列表反结账按钮与确认；`settlement` API；账期 store 刷新。
- **测试**：反结账成功/拒绝路径 E2E；反结后重结账与三表勾稽；日记账无快照/有流水拒绝。
- **文档**：产品边界与优先级以本 change 为准；结账自查文案后续对齐 Non-goals。
