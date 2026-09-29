-- 科目余额表 source_id 允许为空（幂等）
-- 辅助核算余额行的 source_id 为合成 ID（不引用 book_subject），
-- 账套恢复时软外键找不到映射会置空，NOT NULL 约束会导致恢复失败。
ALTER TABLE `statement_subject_balance`
  MODIFY COLUMN `source_id` varchar(45) DEFAULT NULL COMMENT '来源ID（科目ID；辅助核算行为合成ID）';
