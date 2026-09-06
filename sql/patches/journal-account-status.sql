-- journal_account: enable/disable status for UI list and form
ALTER TABLE `journal_account`
  ADD COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态:1-启用;0-禁用' AFTER `description`;
