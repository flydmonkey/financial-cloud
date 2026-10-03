-- 薪资事务当前读的非唯一范围索引；不修改业务数据或去重历史工资。
-- 升级时须先执行本补丁，再启用使用 FORCE INDEX 的薪资版本。
-- 正确的同名索引已有时保留；不兼容的同名索引使 ADD INDEX 明确失败。
SET @payroll_scope_index_sql = IF(
  (SELECT COUNT(*) = 4 AND SUM(
      NON_UNIQUE = 1 AND SUB_PART IS NULL AND IS_VISIBLE = 'YES'
      AND COLLATION = 'A' AND INDEX_TYPE = 'BTREE'
      AND COLUMN_NAME = CASE SEQ_IN_INDEX
        WHEN 1 THEN 'book_id' WHEN 2 THEN 'belong_date'
        WHEN 3 THEN 'employee_id' WHEN 4 THEN 'created_date' END) = 4
   FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'employee_salary'
     AND INDEX_NAME = 'idx_salary_payroll_scope'),
  'SELECT 1',
  'ALTER TABLE `employee_salary` ADD INDEX `idx_salary_payroll_scope` (`book_id`, `belong_date`, `employee_id`, `created_date`)');
PREPARE payroll_scope_index_stmt FROM @payroll_scope_index_sql;
EXECUTE payroll_scope_index_stmt;
DEALLOCATE PREPARE payroll_scope_index_stmt;
