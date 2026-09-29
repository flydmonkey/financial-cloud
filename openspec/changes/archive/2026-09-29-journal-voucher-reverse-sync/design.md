# Design: journal-voucher-reverse-sync

## Context

今日记账 → 凭证单向；关联后流水金额字段锁定。凭证删除不清理 `voucherId`。

## Goals

- 删/作废凭证 → 解绑流水。
- 改未过账关联凭证 → 回写流水金额/日期/备注/对方科目并重算余额。

## Non-Goals

- 过账后改凭证回写；批量生成；凭证红冲联动流水。

## Decisions

1. **解环**：`VoucherService` 用 `ObjectProvider<JournalEntryService>`。
2. **回写映射**：资金科目 = `journal_account.subjectId`；该分录有借→收入方向金额，有贷→支出方向金额；对方科目取另一条分录。
3. **结构不匹配**：抛业务异常，事务回滚，不半写。
4. **作废**：与删除一样解绑（作废凭证不再作为流水锁定依据）。

## Risks

- **[Risk] 用户手工把关联凭证改成非标准两分录** → 拒绝保存并提示回日记账改。
- **[Risk] 多流水同 voucherId** → 罕见；对每条分别回写（同金额）。
