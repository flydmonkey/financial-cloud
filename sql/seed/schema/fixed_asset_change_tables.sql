-- 固定资产变动单（可重复执行）
CREATE TABLE IF NOT EXISTS `fixed_asset_change` (
  `id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `book_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `asset_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `year_period` varchar(7) COLLATE utf8mb4_bin NOT NULL COMMENT 'yyyy-MM',
  `remark` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT 'n',
  PRIMARY KEY (`id`),
  KEY `idx_fa_change_book_period` (`book_id`,`year_period`),
  KEY `idx_fa_change_asset` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='固定资产变动单';

CREATE TABLE IF NOT EXISTS `fixed_asset_change_item` (
  `id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `book_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `change_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `asset_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `field_code` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `field_label` varchar(64) COLLATE utf8mb4_bin NOT NULL,
  `before_value` varchar(512) COLLATE utf8mb4_bin DEFAULT NULL,
  `after_value` varchar(512) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT 'n',
  PRIMARY KEY (`id`),
  KEY `idx_fa_change_item_change` (`change_id`),
  KEY `idx_fa_change_item_period` (`book_id`,`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='固定资产变动明细';
