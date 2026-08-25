ALTER TABLE `jinbooks`.`employee_salary_temp`
ADD COLUMN `pay_amount` DECIMAL(10,5) NULL DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣' AFTER `personal_tax`;


ALTER TABLE `jinbooks`.`employee_salary`
ADD COLUMN `pay_amount` DECIMAL(10,5) NULL DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣' AFTER `personal_tax`;

ALTER TABLE `jinbooks`.`employee_salary_summary`
ADD COLUMN `pay_amount` DECIMAL(10,5) NULL DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣' AFTER `personal_tax`;

ALTER TABLE `jinbooks`.`employee_salary_summary`
DROP COLUMN `salary_voucher_id`,
DROP COLUMN `accrual_voucher_id`;


DROP TABLE `jinbooks`.`employee_salary_voucher_rule_template`, `jinbooks`.`employee_salary_voucher_rule`;

ALTER TABLE `jinbooks`.`voucher_template`
ADD COLUMN `voucher_date` SMALLINT NULL DEFAULT 0 COMMENT '默认凭证日期，为月份的第几天，0为月末' AFTER `voucher_type`;


-- 上月数据

ALTER TABLE `jinbooks`.`statement_subject_balance`
ADD COLUMN `prev_balance` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月末余额' AFTER `closing_balance_credit`,
ADD COLUMN `prev_closing_balance_debit` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月期末余额（借方）' AFTER `prev_balance`,
ADD COLUMN `prev_closing_balance_credit` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月期末余额（贷方）' AFTER `prev_closing_balance_debit`,
ADD COLUMN `prev_year_to_date_debit` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月本年累计发生额（借方）' AFTER `prev_closing_balance_credit`,
ADD COLUMN `prev_year_to_date_credit` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月本年累计发生额（贷方）' AFTER `prev_year_to_date_debit`;

ALTER TABLE `jinbooks`.`employee_salary`
ADD COLUMN `accrual_voucher_id` varchar(45) DEFAULT NULL COMMENT '收票凭证编码',
ADD COLUMN `salary_voucher_id` varchar(45) DEFAULT NULL COMMENT '发放凭证编码';

ALTER TABLE `jinbooks`.`voucher_item`
MODIFY COLUMN `subject_balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '科目余额';

