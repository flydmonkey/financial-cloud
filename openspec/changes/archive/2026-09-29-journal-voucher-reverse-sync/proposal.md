# Proposal: 凭证变更回写日记账流水

## Why

流水生成凭证后，从凭证侧删除会留下悬空 `voucherId`，流水金额被永久锁定；改凭证金额/日期也不会反映到出纳流水，代账对账易不一致。

## What Changes

- 凭证**删除**或**作废**成功后：清除关联 `journal_entry.voucherId`（保留流水，可再生成凭证）。
- 凭证**修改**成功后（未过账）：若存在关联流水，按资金科目分录回写金额、日期、备注与对方科目，并重算账户余额。
- 回写结构无法匹配（资金科目分录缺失等）时拒绝修改并提示。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `cashier-journal`: 增加凭证→流水的解绑与金额回写语义。

## Impact

- `JournalEntryService`、`VoucherService`（`ObjectProvider` 解环）、文档 08 / gap；单测覆盖解绑与回写。
