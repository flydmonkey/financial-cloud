-- 已有库补丁：日记账 prev_opening_balance（与 seed/schema 同语义，可单独执行）
ALTER TABLE `journal_account`
  ADD COLUMN `prev_opening_balance` decimal(10,2) DEFAULT NULL COMMENT '结账前的期初余额快照' AFTER `opening_balance`;
