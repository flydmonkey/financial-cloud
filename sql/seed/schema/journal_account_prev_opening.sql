-- 日记账账户：结账前 opening 快照，供反结账写回（P0 uncheckout）
-- 迁移前已结账的账户 prev_opening_balance 为 NULL，反结账将拒绝并提示无法安全反结。
ALTER TABLE `journal_account`
  ADD COLUMN `prev_opening_balance` decimal(10,2) DEFAULT NULL COMMENT '结账前的期初余额快照' AFTER `opening_balance`;
