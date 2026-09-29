-- 费用报销单明细行（多笔费用一张单，幂等可重复执行）
CREATE TABLE IF NOT EXISTS `expense_claim_item` (
  `id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `book_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `claim_id` varchar(45) COLLATE utf8mb4_bin NOT NULL COMMENT '报销单ID',
  `expense_subject_code` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '费用科目编码',
  `expense_subject_name` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '费用科目名称（冗余）',
  `amount` decimal(18,2) NOT NULL COMMENT '金额',
  `summary` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '费用说明',
  `sort_index` int DEFAULT 0 COMMENT '行序',
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT 'n',
  PRIMARY KEY (`id`),
  KEY `idx_expense_claim_item_claim` (`claim_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='费用报销单明细';
