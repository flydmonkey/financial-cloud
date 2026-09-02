-- 账套配置：辅助核算开关（模板 + 已有账套补全）
INSERT INTO `config` (`config_id`, `book_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `created_by`, `created_date`)
SELECT REPLACE(UUID(), '-', ''), 'template', '辅助核算', 'sys.assist.acc.enabled', 'false', 'y', '是否启用辅助核算', '1', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `config` WHERE `book_id` = 'template' AND `config_key` = 'sys.assist.acc.enabled'
);

INSERT INTO `config` (`config_id`, `book_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `created_by`, `created_date`)
SELECT REPLACE(UUID(), '-', ''), b.id, '辅助核算', 'sys.assist.acc.enabled', 'false', 'y', '是否启用辅助核算', '1', NOW()
FROM `book` b
WHERE b.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM `config` c WHERE c.book_id = b.id AND c.config_key = 'sys.assist.acc.enabled'
);
