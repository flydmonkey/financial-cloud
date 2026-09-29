-- 往来逾期硬阻断开关（缺省 false：仅警告）
INSERT INTO `config` (`config_id`, `book_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `created_by`, `created_date`)
SELECT REPLACE(UUID(), '-', ''), 'template', '结账往来逾期硬阻断', 'settlement.verify.arap.overdue.hard', 'false', 'y', '开启后月结逾期往来硬失败；缺省仅警告', '1', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `config` WHERE `book_id` = 'template' AND `config_key` = 'settlement.verify.arap.overdue.hard'
);

INSERT INTO `config` (`config_id`, `book_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `created_by`, `created_date`)
SELECT REPLACE(UUID(), '-', ''), b.id, '结账往来逾期硬阻断', 'settlement.verify.arap.overdue.hard', 'false', 'y', '开启后月结逾期往来硬失败；缺省仅警告', '1', NOW()
FROM `book` b
WHERE b.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM `config` c WHERE c.book_id = b.id AND c.config_key = 'settlement.verify.arap.overdue.hard'
);
