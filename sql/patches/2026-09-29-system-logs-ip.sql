-- 系统操作日志补充操作来源 IP（对应差距清单 8.2）
ALTER TABLE `history_system_logs`
    ADD COLUMN `ip` varchar(45) DEFAULT NULL COMMENT '操作来源 IP' AFTER `execute_time`;
