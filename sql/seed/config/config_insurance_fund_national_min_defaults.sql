-- 社保公积金：全国最低比例默认值（缴费基数 2500）
-- 用途：已有库表结构默认值对齐；不强制覆盖已有账套业务数据。
-- 新建账套 / 首次打开配置页会由后端 InsuranceFundDefaults 自动写入。

ALTER TABLE `config_insurance_fund`
  MODIFY COLUMN `pay_base` decimal(10,2) DEFAULT '2500.00' COMMENT '缴费基数',
  MODIFY COLUMN `endowment_personal` decimal(10,1) unsigned DEFAULT '8.0' COMMENT '养老保险个人比列',
  MODIFY COLUMN `endowment_business` decimal(10,1) unsigned DEFAULT '16.0' COMMENT '养老保险公司比例',
  MODIFY COLUMN `medical_personal` decimal(10,1) unsigned DEFAULT '2.0' COMMENT '医疗保险个人比列',
  MODIFY COLUMN `medical_business` decimal(10,1) unsigned DEFAULT '6.0' COMMENT '医疗保险公司比例',
  MODIFY COLUMN `unemployment_personal` decimal(10,1) unsigned DEFAULT '0.2' COMMENT '失业保险个人比列',
  MODIFY COLUMN `unemployment_business` decimal(10,1) unsigned DEFAULT '0.3' COMMENT '失业保险公司比例',
  MODIFY COLUMN `employment_injury_personal` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '工伤保险个人比列',
  MODIFY COLUMN `employment_injury_business` decimal(10,1) unsigned DEFAULT '0.2' COMMENT '工伤保险公司比例',
  MODIFY COLUMN `maternity_personal` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '生育保险个人比列（已并入医保）',
  MODIFY COLUMN `maternity_business` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '生育保险公司比例（已并入医保）',
  MODIFY COLUMN `provident_fund_sup_personal` decimal(10,1) unsigned DEFAULT '5.0' COMMENT '住房公积金个人比列',
  MODIFY COLUMN `provident_fund_sup_business` decimal(10,1) unsigned DEFAULT '5.0' COMMENT '住房公积金公司比例';
