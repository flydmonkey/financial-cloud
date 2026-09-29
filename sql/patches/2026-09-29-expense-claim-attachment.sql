-- 报销单票据附件表（幂等可重复执行）
CREATE TABLE IF NOT EXISTS `expense_claim_attachment` (
  `id` varchar(50) COLLATE utf8mb4_bin NOT NULL,
  `book_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `claim_id` varchar(50) COLLATE utf8mb4_bin NOT NULL COMMENT '报销单ID',
  `file_id` varchar(100) COLLATE utf8mb4_bin NOT NULL COMMENT '文件存储ID（file_storage.id）',
  `file_name` varchar(400) COLLATE utf8mb4_bin DEFAULT NULL,
  `content_size` int DEFAULT NULL COMMENT '内容大小（字节）',
  `content_type` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL,
  `sort_index` int DEFAULT 0,
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT 'n',
  PRIMARY KEY (`id`),
  KEY `idx_expense_claim_attachment_claim` (`claim_id`),
  KEY `idx_expense_claim_attachment_book` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='报销单票据附件表';
