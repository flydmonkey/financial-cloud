-- 银行对账：流水对账标记 + 对账单余额表（可重复执行）
ALTER TABLE `journal_entry`
  ADD COLUMN `reconciled` varchar(1) COLLATE utf8mb4_bin DEFAULT 'n' COMMENT '银行对账标记：y已对账/n未对账' AFTER `description`;

CREATE TABLE IF NOT EXISTS `journal_reconciliation` (
  `id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `book_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `acc_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `year_period` varchar(7) COLLATE utf8mb4_bin NOT NULL COMMENT '对账期间yyyy-MM',
  `statement_balance` decimal(18,2) DEFAULT NULL COMMENT '银行对账单期末余额',
  `remark` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT 'n',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_journal_recon` (`book_id`,`acc_id`,`year_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='银行对账单余额';
