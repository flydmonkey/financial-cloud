-- 往来核销 L3（可重复执行）
CREATE TABLE IF NOT EXISTS `arap_writeoff` (
  `id` varchar(64) NOT NULL COMMENT '主键',
  `book_id` varchar(64) NOT NULL COMMENT '账套',
  `side` varchar(8) NOT NULL COMMENT 'AR/AP',
  `counterpart_id` varchar(64) NOT NULL COMMENT '往来单位',
  `counterpart_name` varchar(200) DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL COMMENT '核销金额',
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REVERSED',
  `writeoff_date` date DEFAULT NULL,
  `created_by` varchar(64) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(64) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_arap_wo_book_cp` (`book_id`,`counterpart_id`,`side`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='往来核销头';

CREATE TABLE IF NOT EXISTS `arap_writeoff_line` (
  `id` varchar(64) NOT NULL,
  `writeoff_id` varchar(64) NOT NULL,
  `book_id` varchar(64) NOT NULL,
  `voucher_item_id` varchar(64) NOT NULL,
  `voucher_id` varchar(64) DEFAULT NULL,
  `voucher_year` int DEFAULT NULL,
  `voucher_month` int DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL COMMENT '本行核销金额',
  `created_by` varchar(64) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(64) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_arap_wol_wo` (`writeoff_id`),
  KEY `idx_arap_wol_item` (`book_id`,`voucher_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='往来核销行';
