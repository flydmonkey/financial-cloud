-- 凭证附件表（对应差距清单 3.2：附件上传绑定凭证）
DROP TABLE IF EXISTS `voucher_attachment`;
CREATE TABLE `voucher_attachment` (
  `id` varchar(50) NOT NULL COMMENT 'ID',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `voucher_id` varchar(50) NOT NULL COMMENT '凭证ID',
  `file_id` varchar(100) NOT NULL COMMENT '文件存储ID（file_storage.id）',
  `file_name` varchar(400) DEFAULT NULL COMMENT '文件名称',
  `content_size` int DEFAULT NULL COMMENT '内容大小（字节）',
  `content_type` varchar(100) DEFAULT NULL COMMENT '内容类型',
  `sort_index` int DEFAULT 0 COMMENT '排序',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_voucher_attachment_voucher` (`voucher_id`),
  KEY `idx_voucher_attachment_book` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭证附件表';
