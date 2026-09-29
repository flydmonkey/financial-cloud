-- 固定资产盘盈入账：盘点明细补充入账字段（幂等，可重复执行）
-- 新库：2026-09-29-fixed-asset-check.sql 的 CREATE TABLE 已直接包含这三列；
-- 已有库：执行本补丁补列（已存在则跳过）。

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fixed_asset_check_item' AND COLUMN_NAME = 'surplus_amount') = 0,
  'ALTER TABLE `fixed_asset_check_item` ADD COLUMN `surplus_amount` decimal(18,2) DEFAULT NULL COMMENT ''盘盈入账金额'' AFTER `remark`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fixed_asset_check_item' AND COLUMN_NAME = 'surplus_voucher_id') = 0,
  'ALTER TABLE `fixed_asset_check_item` ADD COLUMN `surplus_voucher_id` varchar(45) DEFAULT NULL COMMENT ''盘盈凭证ID'' AFTER `surplus_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fixed_asset_check_item' AND COLUMN_NAME = 'surplus_asset_id') = 0,
  'ALTER TABLE `fixed_asset_check_item` ADD COLUMN `surplus_asset_id` varchar(45) DEFAULT NULL COMMENT ''盘盈拆出新卡ID'' AFTER `surplus_voucher_id`',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
