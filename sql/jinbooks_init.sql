-- Jinbooks full init SQL (schema + seed data, no business/test data)
-- Generated at: 2026-08-27 03:01:04
-- Generator: python tools/build_init_sql.py

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `jinbooks` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `jinbooks`;

-- ------------------------------------------------------------------
-- Schema
-- ------------------------------------------------------------------

--
-- Table structure for table `approval_record`
--

DROP TABLE IF EXISTS `approval_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_record` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `voucher_id` varchar(45) NOT NULL COMMENT '审批单ID，凭证ID',
  `approver_id` varchar(45) NOT NULL COMMENT '审批人ID',
  `is_transfer` char(1) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT 'n' COMMENT '是否转派',
  `status` enum('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
  `comment` varchar(1024) DEFAULT NULL COMMENT '审批人评论',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assist_acc`
--

DROP TABLE IF EXISTS `assist_acc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assist_acc` (
  `id` varchar(45) NOT NULL COMMENT '供应商ID',
  `book_id` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属账套',
  `assist_type` varchar(2) NOT NULL COMMENT '辅助类别',
  `assist_code` varchar(16) NOT NULL COMMENT '编码',
  `assist_name` varchar(128) NOT NULL COMMENT '名称',
  `dept` varchar(45) DEFAULT NULL COMMENT '部门',
  `spec` varchar(32) DEFAULT NULL COMMENT '规格',
  `unit` varchar(16) DEFAULT NULL COMMENT '单位',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` varchar(1) DEFAULT '0' COMMENT '是否禁用：y或者n',
  `created_by` varchar(45) NOT NULL COMMENT '创建人',
  `created_date` datetime NOT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='辅助核算项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book`
--

DROP TABLE IF EXISTS `book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book` (
  `id` varchar(50) NOT NULL COMMENT 'ID',
  `name` varchar(64) NOT NULL DEFAULT '' COMMENT '账套名称',
  `company_name` varchar(256) NOT NULL COMMENT '单位名称',
  `enable_date` varchar(7) NOT NULL COMMENT '账套启用年月',
  `credit_code` varchar(64) DEFAULT NULL COMMENT '统一社会信用代码',
  `standard_id` varchar(50) NOT NULL COMMENT '关联的会计准则',
  `address` varchar(128) DEFAULT NULL COMMENT '单位所在地',
  `industry` smallint DEFAULT NULL COMMENT '所属行业',
  `vat_type` smallint NOT NULL DEFAULT '0' COMMENT '增值税种类:0.小规模纳税人、1.一般纳税人',
  `voucher_reviewed` tinyint NOT NULL DEFAULT '0' COMMENT '凭证是否需要审核:0-关闭;1-开启',
  `current_account_date` date DEFAULT NULL COMMENT '当前记账年月',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1-启用;0-禁用',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='账套表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_init_balance`
--

DROP TABLE IF EXISTS `book_init_balance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_init_balance` (
  `id` varchar(45) NOT NULL COMMENT '主键ID',
  `book_id` varchar(50) NOT NULL COMMENT '账套ID',
  `category` varchar(45) NOT NULL COMMENT '科目类型  1 资产 2 负债  3共同  4权益 5成本  6损益',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科目编码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科目名称',
  `direction` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '余额方向 1 借 2 贷',
  `parent_id` varchar(50) DEFAULT NULL COMMENT '上级科目ID',
  `id_path` varchar(500) DEFAULT '/' COMMENT '科目编码路径',
  `level` smallint NOT NULL DEFAULT '1' COMMENT '所在级别',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '初始余额',
  `debit_amount` decimal(10,2) DEFAULT '0.00' COMMENT '本年累计借方总金额',
  `credit_amount` decimal(10,2) DEFAULT '0.00' COMMENT '本年累计贷方总金额',
  `opening_year_balance_debit` decimal(10,2) DEFAULT NULL COMMENT '年初余额（借方）',
  `opening_year_balance_credit` decimal(10,2) DEFAULT NULL COMMENT '年初余额（贷方）',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '单位（RMB）',
  `assist_type` varchar(2) DEFAULT NULL COMMENT '辅助核算类型，存在则为辅助核算项',
  `is_cash` smallint NOT NULL DEFAULT '0' COMMENT '是否为现金类科目:0-否；1-是',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='初始余额表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_subject`
--

DROP TABLE IF EXISTS `book_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_subject` (
  `id` varchar(50) NOT NULL COMMENT '主键ID',
  `category` smallint DEFAULT NULL COMMENT '科目类型  1 资产 2 负债  3共同  4权益 5成本  6损益',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科目编码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科目名称',
  `display_name` varchar(500) DEFAULT NULL,
  `pinyin_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '助记码',
  `pinyin_display_code` varchar(500) DEFAULT NULL,
  `direction` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '余额方向 1 借 2 贷',
  `status` smallint NOT NULL DEFAULT '1' COMMENT '状态:1-启用;0-禁用',
  `parent_id` varchar(50) DEFAULT NULL COMMENT '上级科目ID',
  `id_path` varchar(500) DEFAULT '/' COMMENT '科目编码路径',
  `level` smallint NOT NULL DEFAULT '1' COMMENT '所在级别',
  `system_default` smallint NOT NULL DEFAULT '1' COMMENT '是否为系统默认:1-默认;2-手动',
  `is_cash` smallint NOT NULL DEFAULT '0' COMMENT '是否为现金类科目:0-否；1-是',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '科目余额',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '单位',
  `auxiliary` varchar(256) DEFAULT NULL COMMENT '辅助核算:1 项目 2 客户  3供应商  4部门 5员工 6存货\\r\\nJSON字符串格式如：[{type: ‘1’,must: true}]',
  `currency` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `scope` varchar(45) DEFAULT NULL COMMENT '分类',
  `classify` varchar(45) DEFAULT NULL COMMENT '分类',
  `book_id` varchar(50) DEFAULT NULL COMMENT '账套ID',
  `original_id` varchar(50) DEFAULT NULL COMMENT '原始ID',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  `is_auxiliary` int DEFAULT '0' COMMENT '是否辅助核算数据:0-否;1-是',
  `belong_subject_id` varchar(50) DEFAULT NULL COMMENT '辅助核算关联科目ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='账套科目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config`
--

DROP TABLE IF EXISTS `config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config` (
  `config_id` varchar(45) NOT NULL COMMENT '参数主键',
  `book_id` varchar(45) DEFAULT NULL COMMENT '账套ID',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'n' COMMENT '系统内置（Y是 N否）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_cash_flow_balance`
--

DROP TABLE IF EXISTS `config_cash_flow_balance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_cash_flow_balance` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `item_name` varchar(255) NOT NULL COMMENT '财务项目的名称（如销售产成品收到的现金等）。',
  `item_code` varchar(128) NOT NULL COMMENT '财务项目的助记code',
  `sort_index` int NOT NULL COMMENT '排序序号',
  `is_result` smallint NOT NULL DEFAULT '0' COMMENT '是否为计算结果行:0-否;1-是',
  `is_edit` smallint NOT NULL DEFAULT '0' COMMENT '是否可修改金额:0-否;1-是',
  `is_title` smallint NOT NULL DEFAULT '0' COMMENT '是否为标题:0-否;1-是',
  `balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '本年累计金额',
  `direction` smallint NOT NULL DEFAULT '0' COMMENT '金额方向:1:借;2:贷;',
  `book_id` varchar(45) DEFAULT NULL COMMENT '账套ID',
  `is_main` smallint NOT NULL DEFAULT '0' COMMENT '是否为主表项目:0-否;1-是',
  `is_additional` smallint NOT NULL DEFAULT '0' COMMENT '是否为补充资料项目:0-否;1-是',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='现金流量配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_email_senders`
--

DROP TABLE IF EXISTS `config_email_senders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_email_senders` (
  `id` varchar(50) NOT NULL COMMENT 'ID',
  `smtp_host` varchar(45) DEFAULT NULL COMMENT 'SMTP地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `account` varchar(45) DEFAULT NULL COMMENT '账号',
  `credentials` varchar(500) DEFAULT NULL COMMENT '凭证',
  `ssl_switch` int DEFAULT NULL COMMENT 'SSL',
  `sender` varchar(45) DEFAULT NULL COMMENT '发送人',
  `protocol` varchar(45) DEFAULT NULL COMMENT '协议',
  `encoding` varchar(45) DEFAULT NULL COMMENT '编码',
  `status` int DEFAULT NULL COMMENT '状态',
  `book_id` varchar(45) DEFAULT NULL COMMENT '租户编码',
  `description` varchar(45) DEFAULT NULL COMMENT '描述',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='邮箱配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_insurance_fund`
--

DROP TABLE IF EXISTS `config_insurance_fund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_insurance_fund` (
  `id` varchar(50) NOT NULL COMMENT '主键ID',
  `pay_base` decimal(10,2) DEFAULT '0.00' COMMENT '缴费基数',
  `endowment_personal` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '养老保险个人比列',
  `endowment_business` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '养老保险公司比例',
  `medical_personal` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '医疗保险个人比列',
  `medical_business` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '医疗保险公司比例',
  `serious_medical_business` decimal(10,2) DEFAULT '0.00' COMMENT '大病医疗-公司',
  `serious_medical_personal` decimal(10,2) DEFAULT '0.00' COMMENT '大病医疗-个人',
  `unemployment_personal` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '失业保险个人比列',
  `unemployment_business` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '失业保险公司比例',
  `employment_injury_personal` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '工伤保险个人比列',
  `employment_injury_business` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '工伤保险公司比例',
  `maternity_personal` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '生育保险个人比列',
  `maternity_business` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '生育保险公司比例',
  `provident_fund_sup_personal` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '住房公积金个人比列',
  `provident_fund_sup_business` decimal(10,1) unsigned DEFAULT '0.0' COMMENT '住房公积金公司比例',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_login_policy`
--

DROP TABLE IF EXISTS `config_login_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_login_policy` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `session_validity` tinyint unsigned DEFAULT '24' COMMENT '会话时间，默认24小时',
  `token_validity` tinyint unsigned DEFAULT '8' COMMENT '令牌时间，默认8小时',
  `is_first_password_modify` varchar(1) DEFAULT 'Y' COMMENT '首次登录密码修改',
  `captcha` varchar(10) DEFAULT 'NONE' COMMENT '认证端验证码',
  `captcha_mgt` varchar(10) DEFAULT 'NONE' COMMENT '管理端验证码',
  `two_factor` tinyint DEFAULT '0' COMMENT '二次认证方式',
  `is_auto_lock` varchar(1) DEFAULT 'Y' COMMENT '登录失败次数开启自动锁定',
  `lock_interval` tinyint unsigned DEFAULT '30' COMMENT '锁定时间',
  `login_attempts` tinyint unsigned DEFAULT '10' COMMENT '允许登录失败次数，后锁定',
  `password_attempts` tinyint unsigned DEFAULT '10' COMMENT '密码输入错误次数，后验证码',
  `password_attempts_captcha` varchar(1) DEFAULT NULL COMMENT '密码错误次数验证码',
  `terminals` tinyint unsigned DEFAULT '6' COMMENT '端终端数量',
  `scan_code` varchar(10) DEFAULT NULL COMMENT '扫码登录方式 NONE-无，LOCAL-本地，SOCIAL-第三方提供者',
  `is_mobile` varchar(2) DEFAULT 'N' COMMENT '手机验证码登录',
  `is_social` varchar(2) DEFAULT 'N' COMMENT '社交账号登录',
  `redirect_uri` varchar(250) DEFAULT NULL COMMENT '认证端登录后默认跳转地址',
  `book_id` varchar(45) DEFAULT NULL COMMENT '租户编码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='登录策略表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_password_policy`
--

DROP TABLE IF EXISTS `config_password_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_password_policy` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `min_length` tinyint unsigned DEFAULT '0' COMMENT '最小长度',
  `max_length` tinyint unsigned DEFAULT '0' COMMENT '最大长度',
  `lower_case` tinyint unsigned DEFAULT '0' COMMENT '包含小写字母',
  `upper_case` tinyint unsigned DEFAULT '0' COMMENT '包含大写字母',
  `digits` tinyint unsigned DEFAULT '0' COMMENT '包含数字',
  `special_char` tinyint unsigned DEFAULT '0' COMMENT '特殊字符',
  `attempts` tinyint unsigned DEFAULT '0' COMMENT '登录尝试次数',
  `duration` tinyint unsigned DEFAULT '0' COMMENT '自动解除',
  `expiration` tinyint unsigned DEFAULT '0' COMMENT '密码过期时间',
  `username` tinyint unsigned DEFAULT '0' COMMENT '是否包含登录名称',
  `history` tinyint DEFAULT '0' COMMENT '历史密码次数',
  `dictionary` tinyint DEFAULT NULL COMMENT '简单密码字典',
  `alphabetical` tinyint DEFAULT NULL COMMENT '字母序列策略',
  `numerical` tinyint DEFAULT NULL COMMENT '数字序列策略',
  `qwerty` tinyint DEFAULT NULL COMMENT '键盘策略',
  `occurances` tinyint DEFAULT NULL COMMENT '字符重复次数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='密码策略表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_personal_tax`
--

DROP TABLE IF EXISTS `config_personal_tax`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_personal_tax` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `level` int NOT NULL DEFAULT '0' COMMENT '级数',
  `min_num` int DEFAULT NULL COMMENT '应纳税区间-最小金额',
  `max_num` int DEFAULT NULL COMMENT '应纳税区间-最大金额',
  `tax_rate` tinyint unsigned DEFAULT '0' COMMENT '税率',
  `calculation_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '速算扣除数',
  `basic_deduction` varchar(255) DEFAULT NULL,
  `type` smallint NOT NULL DEFAULT '0' COMMENT '税率类型:0-工资;1-劳务报酬',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='税率表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_salary_formula`
--

DROP TABLE IF EXISTS `config_salary_formula`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_salary_formula` (
  `id` varchar(50) NOT NULL COMMENT '主键ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_description` varchar(300) DEFAULT NULL COMMENT '规则描述',
  `formula` text COMMENT '计算公式JSON格式',
  `formula_text` varchar(500) DEFAULT NULL COMMENT '公式文本描述',
  `status` smallint NOT NULL DEFAULT '1' COMMENT '状态:1-启用;0-禁用',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='薪资计算公式';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `config_sms_provider`
--

DROP TABLE IF EXISTS `config_sms_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `config_sms_provider` (
  `id` varchar(50) NOT NULL COMMENT 'ID',
  `provider` varchar(100) DEFAULT NULL COMMENT '提供商',
  `message` varchar(500) DEFAULT NULL COMMENT '内容',
  `app_key` varchar(100) DEFAULT NULL COMMENT 'AppKey',
  `app_secret` varchar(500) DEFAULT NULL COMMENT 'AppSecret',
  `template_id` varchar(45) DEFAULT NULL COMMENT '模板ID',
  `sign_name` varchar(45) DEFAULT NULL COMMENT '签名',
  `sms_sdk_app_id` varchar(45) DEFAULT NULL COMMENT '腾讯SDKAPPID',
  `status` int DEFAULT NULL COMMENT '状态',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `book_id` varchar(45) DEFAULT NULL COMMENT '租户编码',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='短信网关配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer` (
  `id` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `id` varchar(50) NOT NULL DEFAULT '0',
  `display_name` varchar(45) DEFAULT NULL COMMENT '姓名',
  `mobile` varchar(45) DEFAULT NULL COMMENT '电话号码',
  `email` varchar(45) DEFAULT NULL COMMENT '邮箱地址',
  `gender` smallint DEFAULT '0' COMMENT '性别:0-其他；1-男；2-女',
  `birth_date` date DEFAULT '1970-01-01' COMMENT '出生日期',
  `id_type` tinyint DEFAULT '0' COMMENT '证件类型',
  `id_card_no` varchar(20) DEFAULT NULL COMMENT '证件编码',
  `education` enum('小学','初中','高中','中专','大专','本科','硕士研究生','博士研究生','无','') DEFAULT '' COMMENT '学历',
  `graduate_from` varchar(45) DEFAULT NULL COMMENT '毕业院校',
  `graduate_date` date DEFAULT NULL COMMENT '毕业时间',
  `bank_name` varchar(45) DEFAULT NULL COMMENT '银行名称',
  `bank_card_no` varchar(45) DEFAULT NULL COMMENT '银行卡',
  `home_address` varchar(64) DEFAULT NULL COMMENT '住址',
  `pay_basic` decimal(10,2) DEFAULT '0.00' COMMENT '基本工资',
  `pay_merit` decimal(10,2) DEFAULT '0.00' COMMENT '绩效奖金',
  `pay_post` decimal(10,2) DEFAULT '0.00' COMMENT '岗位工资',
  `labor_fee` decimal(10,2) DEFAULT '0.00',
  `insurance_endowment` decimal(10,2) DEFAULT '0.00' COMMENT '养老保险',
  `insurance_medical` decimal(10,2) DEFAULT '0.00' COMMENT '医疗保险',
  `insurance_unemployment` decimal(10,2) DEFAULT '0.00' COMMENT '失业保险',
  `insurance_employment_injury` decimal(10,2) DEFAULT '0.00' COMMENT '工伤保险',
  `insurance_maternity` decimal(10,2) DEFAULT '0.00' COMMENT '生育保险',
  `housing_provident_fund` decimal(10,2) DEFAULT '0.00' COMMENT '住房公积金',
  `insurance_endowment_sup` decimal(10,2) DEFAULT '0.00' COMMENT '养老保险-补充',
  `insurance_medical_sup` decimal(10,2) DEFAULT '0.00' COMMENT '医疗保险-补充',
  `housing_provident_fund_sup` decimal(10,2) DEFAULT '0.00' COMMENT '住房公积金-补充',
  `employee_number` varchar(45) DEFAULT NULL COMMENT '工号',
  `department_id` varchar(45) DEFAULT NULL COMMENT '部门ID',
  `job_title` varchar(45) DEFAULT NULL COMMENT '职务',
  `manager_id` varchar(45) DEFAULT NULL COMMENT '经理编号',
  `entry_date` datetime DEFAULT NULL COMMENT '入职日期',
  `quit_date` datetime DEFAULT NULL COMMENT '离职日期',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `status` smallint NOT NULL DEFAULT '1' COMMENT '状态:1-启用;0-禁用',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  `insurance_endowment_rule` smallint DEFAULT '0' COMMENT '缴费标准-养老保险:0-系统;1-自定义',
  `insurance_medical_rule` smallint DEFAULT '0' COMMENT '缴费标准-医疗保险:0-系统;1-自定义',
  `insurance_unemployment_rule` smallint DEFAULT '0' COMMENT '缴费标准-失业保险:0-系统;1-自定义',
  `insurance_employment_injury_rule` smallint DEFAULT '0' COMMENT '缴费标准-工伤保险:0-系统;1-自定义',
  `insurance_maternity_rule` smallint DEFAULT '0' COMMENT '缴费标准-医疗保险:0-系统;1-自定义',
  `housing_provident_fund_rule` smallint DEFAULT '0' COMMENT '缴费标准-住房公积金:0-系统;1-自定义',
  `pay_base_rule` smallint DEFAULT '0' COMMENT '缴费标准-基数统一:0-系统;1-自定义',
  `pay_base_number` decimal(10,2) DEFAULT '0.00' COMMENT '缴费基数',
  `insurance_fund_card` varchar(20) DEFAULT NULL COMMENT '社保账户',
  `medical_card` varchar(20) DEFAULT NULL COMMENT '医保账户',
  `employee_type` varchar(45) DEFAULT 'NORMAL' COMMENT '员工类型',
  `employee_status` varchar(45) DEFAULT 'RESIDENT' COMMENT '员工状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='员工表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employee_salary`
--

DROP TABLE IF EXISTS `employee_salary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_salary` (
  `id` varchar(45) NOT NULL DEFAULT '0',
  `employee_id` varchar(45) NOT NULL DEFAULT '0' COMMENT '员工ID',
  `belong_date` varchar(7) NOT NULL COMMENT '工资所属月份',
  `pay_basic` decimal(10,2) DEFAULT '0.00' COMMENT '基本工资',
  `pay_merit` decimal(10,2) DEFAULT '0.00' COMMENT '绩效',
  `pay_post` decimal(10,2) DEFAULT '0.00' COMMENT '岗位工资',
  `bonus` decimal(10,2) DEFAULT '0.00' COMMENT '奖金',
  `overtime` decimal(10,2) DEFAULT '0.00' COMMENT '加班补贴',
  `allowance` decimal(10,2) DEFAULT '0.00' COMMENT '津贴',
  `labor_fee` decimal(10,2) DEFAULT '0.00' COMMENT '劳务费',
  `back_pay` decimal(10,2) DEFAULT '0.00' COMMENT '补发工资',
  `total_social_insurance` decimal(10,5) DEFAULT '0.00000' COMMENT '代扣社保合计',
  `provident_fund` decimal(10,5) DEFAULT '0.00000' COMMENT '代扣公积金合计',
  `attendance` decimal(10,2) DEFAULT '0.00' COMMENT '请假考勤',
  `other_deductions` decimal(10,2) DEFAULT '0.00' COMMENT '其他扣额',
  `personal_tax` decimal(10,2) DEFAULT '0.00' COMMENT '个税',
  `pay_amount` decimal(10,5) DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣',
  `total_amount` decimal(10,5) DEFAULT '0.00000' COMMENT '实发合计',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `accrual_voucher_id` varchar(45) DEFAULT NULL COMMENT '收票凭证编码',
  `salary_voucher_id` varchar(45) DEFAULT NULL COMMENT '发放凭证编码',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  `book_id` varchar(45) DEFAULT NULL COMMENT '账套ID',
  `business_social_insurance` decimal(10,5) DEFAULT '0.00000' COMMENT '公司社保',
  `business_provident_fund` decimal(10,5) DEFAULT '0.00000' COMMENT '公司公积金',
  `taxable_wages` decimal(10,5) DEFAULT '0.00000' COMMENT '应税工资',
  `tax_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '税务扣除',
  `business_expenditure_costs` decimal(10,5) DEFAULT '0.00000' COMMENT '公司支出成本',
  `insurance_endowment` decimal(10,2) DEFAULT '0.00' COMMENT '养老保险',
  `insurance_medical` decimal(10,2) DEFAULT '0.00' COMMENT '医疗保险',
  `insurance_unemployment` decimal(10,2) DEFAULT '0.00' COMMENT '失业保险',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='工资明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employee_salary_summary`
--

DROP TABLE IF EXISTS `employee_salary_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_salary_summary` (
  `id` varchar(45) NOT NULL DEFAULT '0',
  `label` varchar(100) DEFAULT NULL COMMENT '发放标签',
  `people_number` int NOT NULL DEFAULT '0' COMMENT '发放人数',
  `belong_date` varchar(7) NOT NULL COMMENT '工资所属月份',
  `pay_basic` decimal(10,2) DEFAULT '0.00' COMMENT '基本工资',
  `pay_merit` decimal(10,2) DEFAULT '0.00' COMMENT '绩效',
  `pay_post` decimal(10,2) DEFAULT '0.00' COMMENT '岗位工资',
  `bonus` decimal(10,2) DEFAULT '0.00' COMMENT '奖金',
  `overtime` decimal(10,2) DEFAULT '0.00' COMMENT '加班补贴',
  `allowance` decimal(10,2) DEFAULT '0.00' COMMENT '津贴',
  `back_pay` decimal(10,2) DEFAULT '0.00' COMMENT '补发工资',
  `total_social_insurance` decimal(10,2) DEFAULT '0.00' COMMENT '代扣社保合计',
  `provident_fund` decimal(10,2) DEFAULT '0.00' COMMENT '代扣公积金合计',
  `attendance` decimal(10,2) DEFAULT '0.00' COMMENT '请假考勤',
  `other_deductions` decimal(10,2) DEFAULT '0.00' COMMENT '其他扣额',
  `personal_tax` decimal(10,2) DEFAULT '0.00' COMMENT '个税',
  `pay_amount` decimal(10,5) DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣',
  `total_amount` decimal(10,2) DEFAULT '0.00' COMMENT '实发合计',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  `labor_fee` decimal(10,2) DEFAULT '0.00' COMMENT '劳务费',
  `book_id` varchar(45) DEFAULT NULL COMMENT '账套ID',
  `business_social_insurance` decimal(10,5) DEFAULT '0.00000' COMMENT '公司社保',
  `business_provident_fund` decimal(10,5) DEFAULT '0.00000' COMMENT '公司公积金',
  `taxable_wages` decimal(10,5) DEFAULT '0.00000' COMMENT '应税工资',
  `tax_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '税务抵扣',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `business_expenditure_costs` decimal(10,5) DEFAULT '0.00000' COMMENT '公司支出成本',
  `accrual_voucher_id` varchar(45) DEFAULT NULL COMMENT '计提凭证编码',
  `salary_voucher_id` varchar(45) DEFAULT NULL COMMENT '工资发放凭证编码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='工资汇总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employee_salary_temp`
--

DROP TABLE IF EXISTS `employee_salary_temp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_salary_temp` (
  `id` varchar(45) NOT NULL DEFAULT '0',
  `employee_id` varchar(45) NOT NULL DEFAULT '0' COMMENT '员工ID',
  `belong_date` varchar(7) NOT NULL COMMENT '工资所属月份',
  `pay_basic` decimal(10,2) DEFAULT '0.00' COMMENT '基本工资',
  `pay_merit` decimal(10,2) DEFAULT '0.00' COMMENT '绩效',
  `pay_post` decimal(10,2) DEFAULT '0.00' COMMENT '岗位工资',
  `bonus` decimal(10,2) DEFAULT '0.00' COMMENT '奖金',
  `overtime` decimal(10,2) DEFAULT '0.00' COMMENT '加班补贴',
  `allowance` decimal(10,2) DEFAULT '0.00' COMMENT '津贴',
  `labor_fee` decimal(10,2) DEFAULT '0.00' COMMENT '劳务费',
  `back_pay` decimal(10,2) DEFAULT '0.00' COMMENT '补发工资',
  `total_social_insurance` decimal(10,5) DEFAULT '0.00000' COMMENT '代扣社保合计',
  `provident_fund` decimal(10,5) DEFAULT '0.00000' COMMENT '代扣公积金合计',
  `attendance` decimal(10,2) DEFAULT '0.00' COMMENT '请假考勤',
  `other_deductions` decimal(10,2) DEFAULT '0.00' COMMENT '其他扣额',
  `personal_tax` decimal(10,2) DEFAULT '0.00' COMMENT '个税',
  `pay_amount` decimal(10,5) DEFAULT '0.00000' COMMENT '应发工资=工资+应增-应扣',
  `total_amount` decimal(10,5) DEFAULT '0.00000' COMMENT '实发合计',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  `book_id` varchar(45) DEFAULT NULL COMMENT '账套ID',
  `business_social_insurance` decimal(10,5) DEFAULT '0.00000' COMMENT '公司社保',
  `business_provident_fund` decimal(10,5) DEFAULT '0.00000' COMMENT '公司公积金',
  `taxable_wages` decimal(10,5) DEFAULT '0.00000' COMMENT '应税工资',
  `tax_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '税务扣除',
  `business_expenditure_costs` decimal(10,5) DEFAULT '0.00000' COMMENT '公司支出成本',
  `insurance_endowment` decimal(10,2) DEFAULT '0.00' COMMENT '养老保险',
  `insurance_medical` decimal(10,2) DEFAULT '0.00' COMMENT '医疗保险',
  `insurance_unemployment` decimal(10,2) DEFAULT '0.00' COMMENT '失业保险',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='工资明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `employee_tax_deduction`
--

DROP TABLE IF EXISTS `employee_tax_deduction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_tax_deduction` (
  `id` varchar(45) NOT NULL,
  `book_id` varchar(45) DEFAULT NULL,
  `employee_no` varchar(45) DEFAULT NULL COMMENT '工号',
  `employee_name` varchar(45) NOT NULL COMMENT '姓名',
  `id_card_type` varchar(45) NOT NULL COMMENT '证件类型',
  `id_card_no` varchar(45) NOT NULL COMMENT '证件号',
  `education` decimal(10,2) DEFAULT '0.00' COMMENT '子女教育',
  `continuing_education` decimal(10,2) DEFAULT '0.00' COMMENT '继续教育',
  `medical` decimal(10,2) DEFAULT '0.00' COMMENT '大病医疗',
  `housing_loan` decimal(10,2) DEFAULT '0.00' COMMENT '住房贷款利息',
  `rent` decimal(10,2) DEFAULT '0.00' COMMENT '住房租金',
  `elderly_care` decimal(10,2) DEFAULT '0.00' COMMENT '赡养老人',
  `infants_care` decimal(10,2) DEFAULT '0.00' COMMENT '3岁以下婴幼儿照护',
  `individual_pension` decimal(10,2) DEFAULT '0.00' COMMENT '个人养老金',
  `enterprise_pension` decimal(10,2) DEFAULT '0.00' COMMENT '企业(职业)年金',
  `commercial_health` decimal(10,2) DEFAULT '0.00' COMMENT '商业健康保险',
  `deferred_pension` decimal(10,2) DEFAULT '0.00' COMMENT '税延养老保险',
  `donation_allowed` decimal(10,2) DEFAULT '0.00' COMMENT '准予扣除的捐赠额',
  `others` decimal(10,2) DEFAULT '0.00' COMMENT '其他',
  `year_period` int NOT NULL,
  `years` int NOT NULL,
  `periods` int NOT NULL,
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='税务扣除，如果发生变动则需重新导入';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_storage`
--

DROP TABLE IF EXISTS `file_storage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_storage` (
  `id` varchar(100) NOT NULL COMMENT 'ID',
  `file_name` varchar(400) DEFAULT NULL COMMENT '文件名称',
  `data_stored` longblob NOT NULL COMMENT '数据存储',
  `content_size` int DEFAULT NULL COMMENT '内容大小',
  `content_type` varchar(100) DEFAULT 'image/png' COMMENT '内容类型',
  `category` varchar(10) DEFAULT 'temp' COMMENT '类别',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='文件上传表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `history_event`
--

DROP TABLE IF EXISTS `history_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `history_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `event_name` varchar(45) DEFAULT NULL COMMENT '活动名称',
  `data_type` varchar(45) DEFAULT NULL COMMENT '数据类型',
  `data_count` int DEFAULT NULL COMMENT '数据账号',
  `execute_datetime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行日期时间',
  `book_id` varchar(45) NOT NULL COMMENT '租户编码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='系统事件记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `history_login`
--

DROP TABLE IF EXISTS `history_login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `history_login` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `category` tinyint DEFAULT NULL COMMENT '类型 1登录，2注销',
  `session_id` varchar(45) DEFAULT NULL COMMENT '会话标识',
  `style` varchar(10) DEFAULT NULL COMMENT '样式',
  `user_id` varchar(45) NOT NULL COMMENT '用户ID',
  `username` varchar(200) NOT NULL COMMENT '登录名称',
  `display_name` varchar(45) DEFAULT NULL COMMENT '姓名',
  `message` varchar(200) DEFAULT NULL COMMENT '状态',
  `ip_addr` varchar(200) DEFAULT NULL COMMENT '访问地址',
  `country` varchar(200) DEFAULT NULL COMMENT '国家',
  `province` varchar(200) DEFAULT NULL COMMENT '省',
  `city` varchar(200) DEFAULT NULL COMMENT '市',
  `location` varchar(500) DEFAULT NULL COMMENT '归属地',
  `login_type` varchar(45) DEFAULT NULL COMMENT '登录方式',
  `code` varchar(45) DEFAULT NULL COMMENT '代码',
  `provider` varchar(45) DEFAULT NULL COMMENT '提供商',
  `browser` varchar(45) DEFAULT NULL COMMENT '浏览器',
  `platform` varchar(45) DEFAULT NULL COMMENT '平台',
  `application` varchar(45) DEFAULT NULL COMMENT '应用',
  `device_id` varchar(200) DEFAULT NULL COMMENT '设备编码',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `book_id` varchar(45) DEFAULT NULL COMMENT '租户编码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='系统登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `history_synchronizer`
--

DROP TABLE IF EXISTS `history_synchronizer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `history_synchronizer` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `sync_id` varchar(45) NOT NULL COMMENT '同步器编号',
  `sync_name` varchar(45) DEFAULT NULL COMMENT '同步器',
  `object_id` varchar(45) DEFAULT NULL COMMENT '对象编号',
  `object_name` varchar(45) DEFAULT NULL COMMENT '对象名称',
  `object_type` varchar(45) DEFAULT NULL COMMENT '对象类型',
  `sync_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
  `result` varchar(45) DEFAULT NULL COMMENT '结果',
  `book_id` varchar(45) NOT NULL COMMENT '租户编码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='同步器日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `history_system_logs`
--

DROP TABLE IF EXISTS `history_system_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `history_system_logs` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `topic` varchar(100) NOT NULL COMMENT '主题',
  `target_id` varchar(45) DEFAULT NULL COMMENT '目标ID',
  `target_name` varchar(200) DEFAULT NULL COMMENT '目标名称',
  `cipher_text` varchar(500) DEFAULT NULL COMMENT '密文',
  `message` varchar(200) DEFAULT NULL COMMENT '内容',
  `message_action` varchar(45) DEFAULT NULL COMMENT '内容动作',
  `message_result` varchar(45) DEFAULT NULL COMMENT '内容结果',
  `user_id` varchar(45) DEFAULT NULL COMMENT '用户ID',
  `username` varchar(45) DEFAULT NULL COMMENT '登录名称',
  `display_name` varchar(45) DEFAULT NULL COMMENT '显示名称',
  `execute_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `book_id` varchar(45) NOT NULL COMMENT '租户编码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='系统操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `institutions`
--

DROP TABLE IF EXISTS `institutions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `institutions` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `inst_name` varchar(200) NOT NULL COMMENT '机构名称',
  `full_name` varchar(100) DEFAULT NULL COMMENT '全称',
  `division` varchar(45) DEFAULT NULL COMMENT '分支机构',
  `country` varchar(45) DEFAULT NULL COMMENT '国家',
  `region` varchar(45) DEFAULT NULL COMMENT '省',
  `locality` varchar(45) DEFAULT NULL COMMENT '城市',
  `street` varchar(45) DEFAULT NULL COMMENT '街道',
  `contact` varchar(45) DEFAULT NULL COMMENT '联系人',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `postal_code` varchar(45) DEFAULT NULL COMMENT '邮政编码',
  `phone` varchar(200) DEFAULT NULL COMMENT '联系电话',
  `fax` varchar(200) DEFAULT NULL COMMENT '传真',
  `email` varchar(45) DEFAULT NULL COMMENT '电子邮箱',
  `sort_index` int unsigned DEFAULT '0' COMMENT '排序序号',
  `logo` varchar(500) DEFAULT NULL COMMENT 'LOGO',
  `background_image` varchar(200) DEFAULT NULL COMMENT '背景图片',
  `domain` varchar(200) DEFAULT NULL COMMENT '认证域名',
  `front_title` varchar(200) DEFAULT NULL COMMENT '系统名称',
  `console_title` varchar(200) DEFAULT NULL COMMENT '控制台名称',
  `console_domain` varchar(200) DEFAULT NULL COMMENT '控制台域名',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `inst_id` varchar(45) DEFAULT NULL COMMENT '租户编码',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `DOMAIN_UNIQUE` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='institutions机构表，存放租户信息multi-tenancy';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `journal_account`
--

DROP TABLE IF EXISTS `journal_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journal_account` (
  `id` varchar(50) NOT NULL,
  `book_id` varchar(50) DEFAULT NULL COMMENT '账套ID',
  `category` varchar(45) DEFAULT 'deposit' COMMENT '账户类型：现金cash  银行deposit',
  `acc_code` varchar(50) DEFAULT NULL COMMENT '账户编码',
  `acc_name` varchar(50) DEFAULT NULL COMMENT '账户名称',
  `subject_id` varchar(50) DEFAULT NULL COMMENT '入账科目',
  `currency` varchar(10) DEFAULT NULL COMMENT '币种',
  `bank_no` varchar(30) DEFAULT NULL COMMENT '银行账户',
  `bank` varchar(100) DEFAULT NULL COMMENT '银行名称/机构',
  `opening_balance` decimal(10,2) DEFAULT '0.00' COMMENT 'DECIMAL(10,2)',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '可用余额',
  `sort_index` int DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL COMMENT '备注',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `journal_entry`
--

DROP TABLE IF EXISTS `journal_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journal_entry` (
  `id` varchar(50) NOT NULL,
  `book_id` varchar(50) DEFAULT NULL COMMENT '账套ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '摘要',
  `category` varchar(45) DEFAULT 'deposit' COMMENT '账户类型：现金cash  银行deposit',
  `acc_id` varchar(45) DEFAULT NULL,
  `acc_code` varchar(45) DEFAULT NULL,
  `acc_name` varchar(45) DEFAULT NULL,
  `subject_id` varchar(45) DEFAULT NULL COMMENT '科目',
  `voucher_id` varchar(45) DEFAULT NULL COMMENT '凭证编码',
  `direction` varchar(1) DEFAULT 'i' COMMENT '方向收入和支出，  I或E，Income and Expenditure',
  `income` decimal(10,2) DEFAULT NULL COMMENT '收入',
  `expenditure` decimal(10,2) DEFAULT NULL COMMENT '支出',
  `balance` decimal(10,2) DEFAULT NULL COMMENT '本次记账后余额',
  `description` varchar(200) DEFAULT NULL COMMENT '备注',
  `trade_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '交易日期',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日记账';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `journal_summary`
--

DROP TABLE IF EXISTS `journal_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journal_summary` (
  `id` varchar(50) NOT NULL,
  `book_id` varchar(50) DEFAULT NULL COMMENT '账套ID',
  `year_period` int DEFAULT NULL,
  `years` int DEFAULT NULL,
  `periods` int DEFAULT NULL,
  `category` varchar(45) DEFAULT 'deposit' COMMENT '账户类型：现金cash  银行deposit',
  `acc_code` varchar(50) DEFAULT NULL COMMENT '账户编码',
  `acc_name` varchar(50) DEFAULT NULL COMMENT '账户名称',
  `currency` varchar(10) DEFAULT NULL COMMENT '币种',
  `opening_balance` decimal(10,2) DEFAULT '0.00' COMMENT '期初余额',
  `closing_balance` decimal(10,2) DEFAULT '0.00' COMMENT '期末余额',
  `income` decimal(10,2) DEFAULT '0.00' COMMENT '收入',
  `expenditure` decimal(10,2) DEFAULT '0.00' COMMENT '支出',
  `year_income` decimal(10,2) DEFAULT NULL,
  `year_expenditure` decimal(10,2) DEFAULT NULL,
  `sort_index` int DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL COMMENT '备注',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收支汇总表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organizations`
--

DROP TABLE IF EXISTS `organizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizations` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `org_code` varchar(45) DEFAULT NULL COMMENT '组织代码',
  `org_name` varchar(100) NOT NULL COMMENT '组织名称',
  `full_name` varchar(200) DEFAULT NULL COMMENT '全称',
  `type` varchar(45) DEFAULT NULL COMMENT '类型',
  `level` int unsigned DEFAULT NULL COMMENT '等级',
  `parent_id` varchar(45) DEFAULT NULL COMMENT '父级ID',
  `parent_code` varchar(45) DEFAULT NULL COMMENT '父级代码',
  `parent_name` varchar(45) DEFAULT NULL COMMENT '父级名称',
  `code_path` varchar(500) DEFAULT NULL COMMENT 'id路径',
  `name_path` varchar(400) DEFAULT NULL COMMENT '名称路径',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `status` tinyint unsigned DEFAULT NULL COMMENT '状态',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `postal_code` varchar(45) DEFAULT NULL COMMENT '邮政编码',
  `phone` varchar(200) DEFAULT NULL COMMENT '手机号码',
  `fax` varchar(200) DEFAULT NULL COMMENT '传真',
  `sort_index` int unsigned DEFAULT '0' COMMENT '排序序号',
  `division` varchar(45) DEFAULT NULL COMMENT '分支机构',
  `country` varchar(45) DEFAULT NULL COMMENT '国家',
  `region` varchar(45) DEFAULT NULL COMMENT '省',
  `locality` varchar(45) DEFAULT NULL COMMENT '市',
  `street` varchar(45) DEFAULT NULL COMMENT '街道',
  `has_child` varchar(45) DEFAULT NULL COMMENT 'haschild',
  `contact` varchar(45) DEFAULT NULL COMMENT '联系人',
  `email` varchar(45) DEFAULT NULL COMMENT '电子邮箱',
  `ldap_dn` varchar(1000) DEFAULT NULL COMMENT 'ldapdn',
  `book_id` varchar(45) NOT NULL COMMENT '租户编码',
  `extra_attrs` text COMMENT '扩展字段',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='组织部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` varchar(50) NOT NULL COMMENT 'ID',
  `role_id` varchar(50) NOT NULL COMMENT '组ID',
  `resource_id` varchar(50) NOT NULL COMMENT '资源ID',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` int DEFAULT '1' COMMENT '状态',
  `book_id` varchar(45) DEFAULT NULL COMMENT '租户编码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='权限-组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permission_book`
--

DROP TABLE IF EXISTS `permission_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission_book` (
  `id` varchar(45) NOT NULL COMMENT '编号',
  `user_id` varchar(45) NOT NULL COMMENT '用户编码',
  `book_id` varchar(45) NOT NULL COMMENT '账套编码',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户账套权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resources`
--

DROP TABLE IF EXISTS `resources`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resources` (
  `id` varchar(50) NOT NULL COMMENT '资源编码',
  `res_name` varchar(200) NOT NULL COMMENT '资源名称',
  `i18n` varchar(100) DEFAULT NULL COMMENT '前端国际化标识',
  `classify` varchar(20) DEFAULT NULL COMMENT '类型',
  `permission` varchar(64) DEFAULT NULL COMMENT '权限标识',
  `request_url` varchar(250) DEFAULT NULL COMMENT '地址',
  `request_method` varchar(20) DEFAULT NULL COMMENT '动作',
  `params` varchar(100) DEFAULT NULL COMMENT '参数',
  `action_type` varchar(10) DEFAULT NULL COMMENT '操作类型',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `icon_selected` varchar(100) DEFAULT NULL COMMENT '图标选中',
  `res_style` varchar(128) DEFAULT NULL COMMENT '样式',
  `is_open` varchar(1) DEFAULT NULL COMMENT '是否开放',
  `is_frame` varchar(1) DEFAULT NULL COMMENT '外部链接',
  `is_cache` varchar(1) DEFAULT NULL COMMENT '是否缓存',
  `is_visible` varchar(1) DEFAULT NULL COMMENT '是否可见',
  `parent_id` varchar(50) DEFAULT NULL COMMENT '父级ID',
  `parent_name` varchar(200) DEFAULT NULL COMMENT '父级名称',
  `sort_index` int DEFAULT '1' COMMENT '排序序号',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `status` varchar(45) DEFAULT NULL COMMENT '状态',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='资源表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_member`
--

DROP TABLE IF EXISTS `role_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_member` (
  `ID` varchar(100) NOT NULL DEFAULT '' COMMENT 'ID',
  `role_id` varchar(100) NOT NULL COMMENT '组ID',
  `member_id` varchar(100) NOT NULL COMMENT '成员ID：用户ID或者组ID',
  `type` varchar(45) NOT NULL COMMENT '类型：用户  , USER-DYNAMIC 或者岗位',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `book_id` varchar(45) NOT NULL COMMENT '租户编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `role_code` varchar(45) DEFAULT NULL COMMENT '组编码',
  `role_name` varchar(100) NOT NULL COMMENT '组名称',
  `category` varchar(20) NOT NULL DEFAULT 'general' COMMENT '类型',
  `pattern` varchar(10) NOT NULL DEFAULT 'static' COMMENT '模型 静态-static，动态-dynamic',
  `filters` text COMMENT '过滤条件SQL',
  `org_ids_list` text COMMENT '机构列表',
  `status` tinyint unsigned DEFAULT NULL COMMENT '状态',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `isdefault` tinyint unsigned DEFAULT NULL COMMENT '是否默认',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scheduled_lock`
--

DROP TABLE IF EXISTS `scheduled_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scheduled_lock` (
  `lock_name` varchar(64) NOT NULL,
  `lock_until` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(255) NOT NULL,
  PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `session_list`
--

DROP TABLE IF EXISTS `session_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session_list` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `session_id` varchar(45) NOT NULL COMMENT '会话标识',
  `style` varchar(10) NOT NULL COMMENT '风格',
  `user_id` varchar(45) NOT NULL COMMENT '用户ID',
  `username` varchar(200) NOT NULL COMMENT '应用名称',
  `display_name` varchar(45) DEFAULT NULL COMMENT '显示名称',
  `message` varchar(200) DEFAULT NULL COMMENT '内容',
  `ip_addr` varchar(200) DEFAULT NULL COMMENT 'ip地址',
  `country` varchar(200) DEFAULT NULL COMMENT '国家',
  `province` varchar(200) DEFAULT NULL COMMENT '省',
  `city` varchar(200) DEFAULT NULL COMMENT '市',
  `location` varchar(500) DEFAULT NULL COMMENT '归属地',
  `login_type` varchar(45) DEFAULT NULL COMMENT '登录类型',
  `code` varchar(45) DEFAULT NULL COMMENT '代码',
  `provider` varchar(45) DEFAULT NULL COMMENT '供应商',
  `browser` varchar(45) DEFAULT NULL COMMENT '浏览器',
  `platform` varchar(45) DEFAULT NULL COMMENT '平台',
  `application` varchar(45) DEFAULT NULL COMMENT '应用',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `book_id` varchar(45) DEFAULT NULL COMMENT '租户编码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `SESSIONID_UNIQUE` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `settlement`
--

DROP TABLE IF EXISTS `settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `settlement` (
  `id` varchar(45) NOT NULL,
  `book_id` varchar(45) DEFAULT NULL,
  `year` int DEFAULT NULL,
  `year_period` varchar(10) DEFAULT NULL COMMENT '账期，年-月',
  `ending_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '期末余额',
  `status` tinyint DEFAULT NULL,
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结账表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `settlement_carryforward`
--

DROP TABLE IF EXISTS `settlement_carryforward`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `settlement_carryforward` (
  `id` varchar(45) NOT NULL,
  `book_id` varchar(45) DEFAULT NULL,
  `year` smallint DEFAULT NULL,
  `year_period` varchar(10) DEFAULT NULL,
  `voucher_id` varchar(45) DEFAULT NULL,
  `voucher_template_id` varchar(45) DEFAULT NULL,
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结转凭证关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `socials_associate`
--

DROP TABLE IF EXISTS `socials_associate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `socials_associate` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `user_id` varchar(45) NOT NULL COMMENT '用户ID',
  `username` varchar(45) NOT NULL COMMENT '登录名称',
  `provider` varchar(45) NOT NULL COMMENT '供应商',
  `social_user_info` text COMMENT '社交用户信息',
  `social_user_id` varchar(100) NOT NULL COMMENT '社交用户编码',
  `ex_attribute` text COMMENT '扩展属性',
  `access_token` text COMMENT '访问token',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_date` datetime NOT NULL COMMENT '更新时间',
  `transmission` varchar(45) DEFAULT 'automatic' COMMENT '传输',
  `book_id` varchar(45) NOT NULL COMMENT '租户编码',
  PRIMARY KEY (`id`),
  KEY `IDX_USERID_SOCIALUSERID` (`user_id`,`social_user_id`,`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='社交账号关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `socials_provider`
--

DROP TABLE IF EXISTS `socials_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `socials_provider` (
  `id` varchar(45) NOT NULL COMMENT 'ID',
  `provider` varchar(45) DEFAULT NULL COMMENT '供应商',
  `provider_name` varchar(45) DEFAULT NULL COMMENT '供应商名称',
  `icon` varchar(45) DEFAULT NULL COMMENT '图标',
  `client_id` varchar(100) DEFAULT NULL COMMENT 'Client Id',
  `client_secret` varchar(500) DEFAULT NULL COMMENT '密钥',
  `agent_id` varchar(45) DEFAULT NULL COMMENT 'AgentId',
  `display` varchar(45) DEFAULT 'false' COMMENT '是否显示',
  `scan_code` varchar(45) DEFAULT 'none' COMMENT '扫码登录',
  `sort_index` int DEFAULT '1' COMMENT '排序序号',
  `status` int DEFAULT '1' COMMENT '状态',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `book_id` varchar(45) NOT NULL COMMENT '租户编码',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='社交提供商表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `standard`
--

DROP TABLE IF EXISTS `standard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `standard` (
  `id` varchar(50) NOT NULL DEFAULT '0' COMMENT '主键ID',
  `name` varchar(64) NOT NULL COMMENT '准则名称',
  `status` smallint NOT NULL COMMENT '状态:1-启用;0-禁用',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='会计准则模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `standard_statement_balance_sheet`
--

DROP TABLE IF EXISTS `standard_statement_balance_sheet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `standard_statement_balance_sheet` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `standard_id` varchar(45) NOT NULL COMMENT '账套ID',
  `asset_or_liability` enum('asset','liability') NOT NULL COMMENT '表示数据类型是资产还是负债',
  `item_code` varchar(45) NOT NULL,
  `item_name` varchar(255) NOT NULL COMMENT '财务项目的名称（如货币资金、应收账款等）。',
  `sort_index` int DEFAULT NULL COMMENT '排序序号',
  `level` smallint NOT NULL COMMENT '级别',
  `parent_item_code` varchar(45) DEFAULT NULL COMMENT '父级ID',
  `symbol` char(1) NOT NULL DEFAULT '+' COMMENT '计算方式：+，-',
  `rule` smallint NOT NULL DEFAULT '1' COMMENT '根据科目取数1，手动录入2',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产负载表模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `standard_statement_income`
--

DROP TABLE IF EXISTS `standard_statement_income`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `standard_statement_income` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `standard_id` varchar(45) NOT NULL COMMENT '准则ID',
  `sort_index` int DEFAULT '10' COMMENT '排序序号',
  `item_code` varchar(45) DEFAULT NULL,
  `item_name` varchar(255) NOT NULL COMMENT '财务项目的名称（如货币资金、应收账款等）。',
  `level` smallint NOT NULL COMMENT '级别',
  `symbol` varchar(1) DEFAULT '+',
  `subject_flag` varchar(1) DEFAULT 'n' COMMENT '科目计算',
  `parent_item_code` varchar(45) DEFAULT NULL COMMENT '父级ID',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='准则利润表模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `standard_statement_rules`
--

DROP TABLE IF EXISTS `standard_statement_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `standard_statement_rules` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `standard_id` varchar(45) DEFAULT NULL,
  `type` enum('balance_sheet','cash_flow','income') NOT NULL COMMENT '报表类型',
  `item_code` varchar(45) NOT NULL COMMENT '报表类目ID',
  `subject_code` varchar(50) NOT NULL COMMENT '科目code',
  `rule` enum('BALANCE','DEBIT_BALANCE','CREDIT_BALANCE','SUBJECT_DEBIT_BALANCE','SUBJECT_CREDIT_BALANCE','CURRENT_SUBJECT_DEBIT_BALANCE','CURRENT_SUBJECT_CREDIT_BALANCE','PROFIT_AND_LOSS_AMOUNT','DEBIT_AMOUNT','CREDIT_AMOUNT') NOT NULL COMMENT '取数规则：\\r\\nBALANCE, // 余额\\n    \\r\\nDEBIT_BALANCE, // 借方余额\\n    \\r\\nCREDIT_BALANCE, // 贷方余额\\n    \\r\\nSUBJECT_DEBIT_BALANCE, // 科目借方余额\\n    \\r\\nSUBJECT_CREDIT_BALANCE, // 科目贷方余额\\n    \\r\\nCURRENT_SUBJECT_DEBIT_BALANCE, // 本级科目借方余额\\n    \\r\\nCURRENT_SUBJECT_CREDIT_BALANCE // 本级科目贷方余额',
  `symbol` char(1) NOT NULL COMMENT '计算方式：+，-',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表统计规则模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `standard_subject`
--

DROP TABLE IF EXISTS `standard_subject`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `standard_subject` (
  `id` varchar(50) NOT NULL COMMENT '主键ID',
  `category` smallint NOT NULL COMMENT '科目类型  1 资产 2 负债  3共同  4权益 5成本  6损益',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科目编码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科目名称',
  `display_name` varchar(500) DEFAULT NULL,
  `pinyin_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '助记码',
  `pinyin_display_code` varchar(500) DEFAULT NULL,
  `direction` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '余额方向 1 借 2 贷',
  `status` smallint NOT NULL DEFAULT '1' COMMENT '状态:1-启用;0-禁用',
  `parent_id` varchar(50) DEFAULT NULL COMMENT '上级科目ID',
  `id_path` varchar(500) DEFAULT '/' COMMENT '科目编码路径',
  `level` smallint NOT NULL DEFAULT '1' COMMENT '所在级别',
  `system_default` smallint NOT NULL DEFAULT '1' COMMENT '是否为系统默认:1-默认;2-手动',
  `is_cash` smallint NOT NULL DEFAULT '0' COMMENT '是否为现金类科目:0-否；1-是',
  `balance` decimal(10,2) DEFAULT NULL COMMENT '科目余额',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '单位',
  `auxiliary` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '辅助核算:1 项目 2 客户  3供应商  4部门 5员工 6存货\\r\\nJSON字符串格式如：[{type: 1,must: true}]',
  `currency` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `classify` varchar(45) DEFAULT NULL COMMENT '分类',
  `scope` varchar(45) DEFAULT NULL COMMENT '适用范围',
  `standard_id` varchar(50) DEFAULT NULL COMMENT '会计准则ID',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='记账科目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `standard_subject_cash_flow`
--

DROP TABLE IF EXISTS `standard_subject_cash_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `standard_subject_cash_flow` (
  `id` varchar(50) NOT NULL COMMENT '主键ID',
  `item_code` varchar(128) NOT NULL COMMENT '财务项目的助记code',
  `subject_code` varchar(50) NOT NULL COMMENT '科目编码',
  `direction` varchar(1) DEFAULT NULL COMMENT '余额方向 1 借 2 贷',
  `standard_id` varchar(50) DEFAULT NULL COMMENT '会计准则ID',
  `book_id` varchar(45) DEFAULT NULL COMMENT '账套ID',
  `is_template` smallint NOT NULL DEFAULT '0' COMMENT '是否为模板:0-否;1-是',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='科目现金流量关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statement_balance_sheet`
--

DROP TABLE IF EXISTS `statement_balance_sheet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statement_balance_sheet` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `year_period` varchar(7) NOT NULL COMMENT '报表期间',
  `period_type` enum('month','quarter','year','halfYear') NOT NULL COMMENT '报表周期（如：月度、季度、年度）',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产负载表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statement_balance_sheet_item`
--

DROP TABLE IF EXISTS `statement_balance_sheet_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statement_balance_sheet_item` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `balance_sheet_id` varchar(45) DEFAULT NULL,
  `asset_or_liability` enum('asset','liability') NOT NULL COMMENT '表示数据类型是资产还是负债',
  `item_code` varchar(45) NOT NULL,
  `item_name` varchar(255) NOT NULL COMMENT '财务项目的名称（如货币资金、应收账款等）。',
  `sort_index` int DEFAULT NULL COMMENT '排序序号',
  `level` smallint NOT NULL COMMENT '级别',
  `parent_item_code` varchar(45) DEFAULT NULL COMMENT '父级ID',
  `symbol` char(1) DEFAULT '+' COMMENT '计算方式：+，-',
  `rule` smallint DEFAULT '1' COMMENT '根据科目取数1，手动录入2',
  `current_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '期末余额，通常指报表期末的数值。',
  `initial_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '年初余额，通常指报表开始时的数值。',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产负载表-信息项配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statement_cash_flow`
--

DROP TABLE IF EXISTS `statement_cash_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statement_cash_flow` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `report_date` date NOT NULL COMMENT '报表时间',
  `period_type` enum('month','quarter','year','halfYear') NOT NULL COMMENT '报表周期（如：月度、季度、年度）',
  `sort_index` int DEFAULT '10' COMMENT '排序序号',
  `item_name` varchar(255) DEFAULT NULL COMMENT '财务项目的名称',
  `item_code` varchar(32) DEFAULT NULL COMMENT '财务项目的code',
  `current_amount` decimal(18,2) DEFAULT NULL COMMENT '本年累计金额',
  `monthly_amount` decimal(18,2) NOT NULL COMMENT '本月金额',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='现金流量表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statement_income`
--

DROP TABLE IF EXISTS `statement_income`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statement_income` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `year_period` varchar(7) NOT NULL COMMENT '报表期间',
  `period_type` enum('month','quarter','year','halfYear') NOT NULL COMMENT '报表周期（如：月度、季度、年度）',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='利润表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statement_income_item`
--

DROP TABLE IF EXISTS `statement_income_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statement_income_item` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `income_id` varchar(45) NOT NULL COMMENT '报表期间',
  `sort_index` int DEFAULT '10' COMMENT '排序序号',
  `item_code` varchar(45) DEFAULT NULL,
  `item_name` varchar(255) NOT NULL COMMENT '财务项目的名称（如货币资金、应收账款等）。',
  `symbol` varchar(45) DEFAULT NULL,
  `level` smallint NOT NULL COMMENT '级别',
  `current_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '本期金额',
  `cumulative_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '累计金额-本年',
  `parent_item_code` varchar(45) DEFAULT NULL COMMENT '父级ID',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='利润表明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statement_rules`
--

DROP TABLE IF EXISTS `statement_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statement_rules` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `book_id` varchar(45) DEFAULT NULL,
  `type` enum('balance_sheet','cash_flow','income') NOT NULL COMMENT '报表类型',
  `item_code` varchar(45) NOT NULL COMMENT '报表类目ID',
  `subject_code` varchar(50) NOT NULL COMMENT '科目code',
  `rule` enum('BALANCE','DEBIT_BALANCE','CREDIT_BALANCE','SUBJECT_DEBIT_BALANCE','SUBJECT_CREDIT_BALANCE','CURRENT_SUBJECT_DEBIT_BALANCE','CURRENT_SUBJECT_CREDIT_BALANCE','PROFIT_AND_LOSS_AMOUNT','DEBIT_AMOUNT','CREDIT_AMOUNT') NOT NULL COMMENT '取数规则：\\r\\nBALANCE, // 余额\\n    \\r\\nDEBIT_BALANCE, // 借方余额\\n    \\r\\nCREDIT_BALANCE, // 贷方余额\\n    \\r\\nSUBJECT_DEBIT_BALANCE, // 科目借方余额\\n    \\r\\nSUBJECT_CREDIT_BALANCE, // 科目贷方余额\\n    \\r\\nCURRENT_SUBJECT_DEBIT_BALANCE, // 本级科目借方余额\\n    \\r\\nCURRENT_SUBJECT_CREDIT_BALANCE // 本级科目贷方余额',
  `symbol` char(1) NOT NULL COMMENT '计算方式：+，-',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表统计规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `statement_subject_balance`
--

DROP TABLE IF EXISTS `statement_subject_balance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `statement_subject_balance` (
  `id` varchar(45) NOT NULL COMMENT '主键',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `period_type` enum('month','quarter','year','halfYear') NOT NULL COMMENT '报表周期（如：月度、季度、年度）',
  `year_period` varchar(7) NOT NULL COMMENT '报表期间',
  `subject_code` varchar(50) NOT NULL COMMENT '科目编码',
  `source_id` varchar(45) NOT NULL COMMENT '科目原始ID',
  `parent_id` varchar(45) DEFAULT NULL COMMENT '父级ID',
  `subject_name` varchar(64) NOT NULL COMMENT '科目名称',
  `sort_index` int NOT NULL DEFAULT '10' COMMENT '排序序号',
  `direction` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '余额方向 1 借 2 贷',
  `balance` decimal(18,2) NOT NULL COMMENT '余额',
  `opening_year_balance_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '年初余额（借方）',
  `opening_year_balance_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '年初余额（贷方）',
  `opening_balance_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '期初余额（借方）',
  `opening_balance_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '期初余额（贷方）',
  `current_period_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '本期发生额（借方）',
  `current_period_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '本期发生额（贷方）',
  `year_to_date_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '本年累计发生额（借方）',
  `year_to_date_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '本年累计发生额（贷方）',
  `closing_balance_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '期末余额（借方）',
  `closing_balance_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '期末余额（贷方）',
  `prev_balance` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月末余额',
  `prev_closing_balance_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月期末余额（借方）',
  `prev_closing_balance_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月期末余额（贷方）',
  `prev_year_to_date_debit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月本年累计发生额（借方）',
  `prev_year_to_date_credit` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '上月本年累计发生额（贷方）',
  `is_voucher` char(1) NOT NULL DEFAULT 'n' COMMENT '当前期是否使用：n-否;y-是',
  `is_auxiliary` char(1) NOT NULL DEFAULT 'n' COMMENT '是否辅助核算项:n-否;y-是',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科目余额表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userinfo`
--

DROP TABLE IF EXISTS `userinfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userinfo` (
  `id` varchar(45) NOT NULL COMMENT '编号',
  `username` varchar(100) NOT NULL COMMENT '登录名',
  `password` varchar(500) NOT NULL COMMENT '密码',
  `decipherable` varchar(500) NOT NULL COMMENT 'DE密码',
  `two_factor` tinyint unsigned DEFAULT '0' COMMENT '二次认证类型',
  `mobile` varchar(45) DEFAULT NULL COMMENT '手机号码',
  `mobile_verified` varchar(45) DEFAULT NULL COMMENT '手机号验证',
  `email` varchar(45) DEFAULT NULL COMMENT '邮箱',
  `email_verified` smallint unsigned DEFAULT NULL COMMENT '邮箱验证',
  `display_name` varchar(45) DEFAULT NULL COMMENT '显示名称',
  `nick_name` varchar(45) DEFAULT NULL COMMENT '昵称',
  `picture_id` varchar(200) DEFAULT NULL COMMENT '头像ID',
  `time_zone` varchar(45) DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `locale` varchar(45) DEFAULT 'zh_CN' COMMENT '地址',
  `preferred_language` varchar(45) DEFAULT 'zh_CN' COMMENT '语言偏好',
  `password_question` varchar(45) DEFAULT NULL COMMENT '密码问题',
  `password_answer` varchar(45) DEFAULT NULL COMMENT '密码答案',
  `theme` varchar(45) DEFAULT 'default' COMMENT '主题',
  `login_count` int unsigned DEFAULT '0' COMMENT '登录次数统计',
  `is_online` tinyint unsigned DEFAULT '0' COMMENT '在线状态',
  `status` tinyint unsigned DEFAULT NULL COMMENT '用户状态',
  `is_locked` tinyint unsigned DEFAULT NULL COMMENT '锁定状态',
  `un_lock_time` datetime DEFAULT '2020-01-01 01:01:01' COMMENT '解锁时间',
  `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最近登录IP地址',
  `last_login_time` datetime DEFAULT '2020-01-01 01:01:01' COMMENT '最近登录时间',
  `last_logoff_time` datetime DEFAULT '2020-01-01 01:01:01' COMMENT '最近注销时间',
  `login_failed_count` smallint DEFAULT '0' COMMENT '登录失败次数',
  `login_failed_time` datetime DEFAULT NULL COMMENT '登录失败时间',
  `bad_password_time` datetime DEFAULT '2020-01-01 01:01:01' COMMENT '最近密码错误时间',
  `bad_password_count` smallint unsigned DEFAULT NULL COMMENT '密码错误次数',
  `password_last_set_time` datetime DEFAULT '2020-01-01 01:01:01' COMMENT '最近密码修改时间',
  `password_set_type` tinyint unsigned DEFAULT '0' COMMENT '密码重置类型',
  `shared_secret` varchar(500) DEFAULT NULL COMMENT 'TIME-OPT密钥',
  `shared_counter` varchar(45) DEFAULT '0' COMMENT 'COUNTER-OPT密钥',
  `user_type` varchar(45) DEFAULT 'Customer' COMMENT '用户类型',
  `user_state` varchar(45) DEFAULT 'RESIDENT' COMMENT '用户状态',
  `sort_order` tinyint unsigned DEFAULT '0' COMMENT '部门内排序',
  `name_zh_spell` varchar(100) DEFAULT NULL COMMENT '名字中文拼音',
  `name_zh_short_spell` varchar(45) DEFAULT NULL COMMENT '名字中文拼音简称',
  `web_site` varchar(50) DEFAULT NULL COMMENT '个人主页',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `description` varchar(400) DEFAULT NULL COMMENT '描述',
  `ldap_dn` varchar(1000) DEFAULT NULL COMMENT '最近访问book_id',
  `book_id` varchar(45) NOT NULL,
  `sort_index` int DEFAULT NULL COMMENT '排序',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `USERNAME_UNIQUE` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher`
--

DROP TABLE IF EXISTS `voucher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher` (
  `id` varchar(45) COLLATE utf8mb4_bin NOT NULL DEFAULT '0' COMMENT '主键',
  `word` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '凭证字：收字第2024第0001号',
  `word_head` varchar(8) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字头：“收”、“付”、“转”等',
  `word_num` int NOT NULL COMMENT '号码',
  `book_id` varchar(45) COLLATE utf8mb4_bin NOT NULL COMMENT '所属账套',
  `company_id` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '公司ID',
  `company_name` varchar(128) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '公司名称',
  `remark` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注',
  `receipt_num` int NOT NULL DEFAULT '0' COMMENT '附单据数量',
  `debit_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '借方总金额（元）',
  `credit_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '贷方总金额（元）',
  `voucher_year` int NOT NULL COMMENT '年份',
  `voucher_month` int NOT NULL COMMENT '月份',
  `voucher_date` date NOT NULL COMMENT '业务日期',
  `carry_forward` enum('y','n') COLLATE utf8mb4_bin NOT NULL DEFAULT 'n' COMMENT '是否结转损益：y|n',
  `audit_member_id` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '审核人ID',
  `audit_member_name` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '审核人姓名',
  `audit_date` datetime DEFAULT NULL COMMENT '审核时间',
  `sender_id` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '过账人ID',
  `sender_name` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '过账人姓名',
  `sender_date` datetime DEFAULT NULL COMMENT '过账操作时间',
  `manager_id` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '主管ID',
  `manager_name` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '主管姓名',
  `manager_date` datetime DEFAULT NULL COMMENT '主管操作时间',
  `status` enum('draft','reviewing','completed','rejected','cancelled') COLLATE utf8mb4_bin NOT NULL DEFAULT 'draft' COMMENT '状态：\\r\\n暂存 - draft\\n，\\r\\n审核中 - reviewing，\\r\\n已完成 - completed，\\r\\n被拒绝 - rejected，\\r\\n已取消 - cancelled',
  `created_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '制单人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='凭证记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher_auxiliary`
--

DROP TABLE IF EXISTS `voucher_auxiliary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_auxiliary` (
  `id` varchar(45) NOT NULL DEFAULT '0' COMMENT '主键',
  `book_id` varchar(45) NOT NULL,
  `voucher_id` varchar(45) NOT NULL COMMENT '凭证ID',
  `voucher_item_id` varchar(45) NOT NULL COMMENT '凭证明细ID',
  `auxiliary` varchar(2) NOT NULL COMMENT '辅助核算类型',
  `auxiliary_name` varchar(32) NOT NULL COMMENT '辅助核算类型名称',
  `item_id` varchar(45) NOT NULL COMMENT '核算对象ID',
  `item_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '核算对象名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='凭证明细辅助核算关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher_item`
--

DROP TABLE IF EXISTS `voucher_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_item` (
  `id` varchar(45) NOT NULL DEFAULT '0' COMMENT '主键',
  `book_id` varchar(45) NOT NULL,
  `voucher_id` varchar(45) NOT NULL COMMENT '凭证ID',
  `voucher_date` date DEFAULT NULL COMMENT '业务日期',
  `summary` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '摘要',
  `subject_id` varchar(45) NOT NULL COMMENT '会计科目ID',
  `subject_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科目名称',
  `subject_code` varchar(56) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '科目编号',
  `debit_amount` decimal(10,2) DEFAULT NULL COMMENT '借方金额',
  `credit_amount` decimal(10,2) DEFAULT NULL COMMENT '贷方金额',
  `num` int DEFAULT NULL COMMENT '数量',
  `price` decimal(10,0) DEFAULT NULL COMMENT '单价',
  `cumulative_debit` decimal(10,0) DEFAULT NULL COMMENT '期初累计借方',
  `cumulative_credit` decimal(10,0) DEFAULT NULL COMMENT '期初累计贷方',
  `subject_balance` decimal(10,2) NOT NULL COMMENT '科目余额',
  `carry_forward` bit(1) NOT NULL DEFAULT b'0' COMMENT '结转损益',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='凭证明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher_item_cash_flow`
--

DROP TABLE IF EXISTS `voucher_item_cash_flow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_item_cash_flow` (
  `id` varchar(45) NOT NULL COMMENT '主键ID',
  `voucher_item_id` varchar(45) NOT NULL COMMENT '凭证项ID',
  `cash_flow_item_code` varchar(128) DEFAULT NULL COMMENT '现金流量助记码',
  `cash_flow_balance` decimal(18,2) DEFAULT '0.00' COMMENT '现金流量金额',
  `cash_flow_item_type` tinyint NOT NULL DEFAULT '0' COMMENT '项目类型：0-主表；1-补充资料',
  `book_id` varchar(45) NOT NULL COMMENT '账套ID',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='凭证项现金流量关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher_template`
--

DROP TABLE IF EXISTS `voucher_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_template` (
  `id` varchar(45) NOT NULL DEFAULT '0' COMMENT '主键ID',
  `related_id` varchar(45) NOT NULL DEFAULT '0' COMMENT '准则或账套ID',
  `code` varchar(45) NOT NULL DEFAULT '1',
  `name` varchar(45) NOT NULL COMMENT '名称',
  `category` tinyint DEFAULT NULL COMMENT '分类 1期末处理',
  `remark` varchar(45) DEFAULT NULL COMMENT '备注',
  `voucher_type` tinyint NOT NULL DEFAULT '0' COMMENT '凭证类型:0-计提,1-发放',
  `voucher_date` smallint DEFAULT 0 COMMENT '默认凭证日期，为月份的第几天，0为月末',
  `word_head` varchar(8) NOT NULL DEFAULT '' COMMENT '字头',
  `sort_index` smallint DEFAULT '1' COMMENT '排序',
  `status` smallint NOT NULL DEFAULT '1' COMMENT '状态:1-启用;0-禁用',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT 'n' COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher_template_item`
--

DROP TABLE IF EXISTS `voucher_template_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_template_item` (
  `id` varchar(45) NOT NULL DEFAULT '0' COMMENT '主键ID',
  `summary` varchar(64) NOT NULL DEFAULT '' COMMENT '摘要',
  `direction` varchar(32) NOT NULL DEFAULT '1' COMMENT '余额方向 1 借 2 贷',
  `subject_code` varchar(50) NOT NULL DEFAULT '0' COMMENT '科目',
  `selected_value` varchar(64) DEFAULT NULL COMMENT '取值',
  `related_id` varchar(45) NOT NULL,
  `template_id` varchar(45) NOT NULL COMMENT '模板ID',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  `deleted` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher_word`
--

DROP TABLE IF EXISTS `voucher_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_word` (
  `id` varchar(45) NOT NULL DEFAULT '0' COMMENT '主键',
  `book_id` varchar(45) NOT NULL,
  `word_head` varchar(8) NOT NULL COMMENT '字头：“收”、“付”、“转”等',
  `word_year` int NOT NULL COMMENT '年份',
  `word_month` int NOT NULL COMMENT '月份',
  `word_num` int NOT NULL COMMENT '号码',
  `word` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '凭证字，例：收字第2024第0001号',
  `print_title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '打印标题',
  `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认',
  `created_by` varchar(45) DEFAULT NULL COMMENT '创建人',
  `created_date` datetime DEFAULT NULL COMMENT '创建时间',
  `modified_by` varchar(45) DEFAULT NULL COMMENT '修改人',
  `modified_date` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='凭证字表';
/*!40101 SET character_set_client = @saved_cs_client */;

-- ------------------------------------------------------------------
-- Post-schema patches
-- ------------------------------------------------------------------

-- Disable MaxKey/SSO menu entries that no longer have backend or frontend support.
-- Run against the jinbooks database after deploying the frontend cleanup.

UPDATE `jinbooks`.`resources`
SET `status` = '0', `is_visible` = 'n'
WHERE `deleted` = 'n'
  AND (
    `request_url` LIKE '/apps%'
    OR `request_url` LIKE '/sync%'
    OR `request_url` LIKE '/access%'
    OR `request_url` LIKE '/accounts%'
    OR `request_url` LIKE '/monitor%'
    OR `request_url` LIKE '/tool%'
    OR `request_url` LIKE '/system/dict%'
    OR `request_url` LIKE '/system/post%'
    OR `request_url` LIKE '/system/role%'
    OR `request_url` LIKE '/security/gradings%'
    OR `request_url` LIKE '/security/ldapcontext%'
    OR `request_url` LIKE '/security/configweakpassword%'
    OR `request_url` LIKE '/security/configpasswordencrypt%'
    OR `request_url` LIKE '/config/adapters%'
    OR `request_url` LIKE '/config/appscategory%'
    OR `request_url` LIKE '/config/expandattrs%'
    OR `permission` LIKE 'apps:%'
    OR `permission` LIKE 'monitor:%'
    OR `permission` LIKE 'tool:%'
  );

-- Point surviving resource-management menu to the relocated Vue page.
UPDATE `jinbooks`.`resources`
SET `request_url` = '/permissions/resources'
WHERE `deleted` = 'n'
  AND `request_url` IN ('/permissions/apps/resources', 'permissions/apps/resources');

-- 账套配置：辅助核算开关（模板 + 已有账套补全）
INSERT INTO `config` (`config_id`, `book_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `created_by`, `created_date`)
SELECT REPLACE(UUID(), '-', ''), 'template', '辅助核算', 'sys.assist.acc.enabled', 'false', 'y', '是否启用辅助核算', '1', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `config` WHERE `book_id` = 'template' AND `config_key` = 'sys.assist.acc.enabled'
);

INSERT INTO `config` (`config_id`, `book_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `created_by`, `created_date`)
SELECT REPLACE(UUID(), '-', ''), b.id, '辅助核算', 'sys.assist.acc.enabled', 'false', 'y', '是否启用辅助核算', '1', NOW()
FROM `book` b
WHERE b.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM `config` c WHERE c.book_id = b.id AND c.config_key = 'sys.assist.acc.enabled'
);

-- ------------------------------------------------------------------
-- Seed data
-- ------------------------------------------------------------------

-- institutions
LOCK TABLES `institutions` WRITE;
/*!40000 ALTER TABLE `institutions` DISABLE KEYS */;
INSERT INTO `institutions` VALUES ('1','jinbooks','jinbooks','','','','','','','','','','','',1,'./assets/logo.png','./assets/logo.png','sso.maxkey.top','jinbooks','jinbooks','mgt.maxkey.top',1,'',NULL,NULL,NULL,'1','2025-01-22 20:08:37','n');
/*!40000 ALTER TABLE `institutions` ENABLE KEYS */;
UNLOCK TABLES;

-- userinfo
LOCK TABLES `userinfo` WRITE;
/*!40000 ALTER TABLE `userinfo` DISABLE KEYS */;
INSERT INTO `userinfo` VALUES ('1','admin','{plain}maxkey','$2a$10$uCfjmDHxUS2Aow79ZNaJhu5a0c0426c67e44f27630ecb09d7e99c1cfc7c21d49eb4edfb76e4fefe2dafe50bc56dd703a89211c',0,'','0','admin@localhost',0,'系统管理员','系统管理员','1','Asia/Shanghai','de','zh_CN','5','wusdfdsf','default',3981,1,1,1,'2024-02-17 13:26:07','0:0:0:0:0:0:0:1','2025-06-01 07:46:55','2025-05-30 09:01:58',0,'2024-11-01 15:40:33','2025-05-21 09:03:28',0,'2024-10-30 10:56:10',1,'$2a$10$kwA5OXSKwfud102tBwdbZe0763d93797aef4f6cd5e43637330aa95e8dabb3e9800c113474114ef3ef6e8af4a11a5e835c996427a97049e87ca0b7668ae8ffef9f2d298','0','EMPLOYEE','RESIDENT',0,NULL,NULL,'http://login.maxkey.org/',NULL,'2014-01-21 00:00:00','1','2025-02-16 18:49:58',NULL,NULL,'',0,'n');
/*!40000 ALTER TABLE `userinfo` ENABLE KEYS */;
UNLOCK TABLES;

-- standard
LOCK TABLES `standard` WRITE;
/*!40000 ALTER TABLE `standard` DISABLE KEYS */;
INSERT INTO `standard` VALUES ('1','小企业会计准则',1,NULL,'2024-12-27 18:02:32','1','2025-03-05 11:43:35','n'),('2','企业会计制度',1,'1','2025-01-15 16:28:23','1','2025-05-15 09:39:17','n');
/*!40000 ALTER TABLE `standard` ENABLE KEYS */;
UNLOCK TABLES;

-- roles
LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES ('1880190696367833089','6001','会计主管','general','static',NULL,'',1,'1',0,'2025-01-17 17:49:28','1','2025-01-17 17:49:28',NULL,'n'),('1880191070453612545','5001','出纳岗','general','static',NULL,'',1,'1',0,'2025-01-17 17:50:58','1','2025-01-17 17:51:52',NULL,'n'),('1880191154616516610','2001','制单岗','general','static',NULL,'',1,'1',0,'2025-01-17 17:51:18','1','2025-01-17 17:52:02',NULL,'n'),('1880191264779911169','3001','复核岗','general','static',NULL,'',1,'1',0,'2025-01-17 17:51:44','1','2025-01-17 17:51:44',NULL,'n'),('1880191529151086593','1001','单位员工','general','static',NULL,'',1,'admin',0,'2025-01-13 16:45:52','1','2025-02-18 22:38:22','','n'),('1880191529151086594','4001','过账岗','general','static',NULL,'',1,'1',0,'2025-01-17 17:52:47','1','2025-01-17 17:52:47',NULL,'n'),('ROLE_ADMINISTRATORS','1000','系统管理员组','supervisor','static',NULL,'',1,'admin',0,NULL,'1','2025-01-17 18:07:55','系统管理员组','n');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

-- role_member
LOCK TABLES `role_member` WRITE;
/*!40000 ALTER TABLE `role_member` DISABLE KEYS */;
INSERT INTO `role_member` VALUES ('622183103330254848','ROLE_ADMINISTRATORS','1','USER',NULL,'2020-12-12 11:03:10','1');
/*!40000 ALTER TABLE `role_member` ENABLE KEYS */;
UNLOCK TABLES;

-- resources
LOCK TABLES `resources` WRITE;
/*!40000 ALTER TABLE `resources` DISABLE KEYS */;
INSERT INTO `resources` VALUES ('1010190003382255616','弱密码字典','mxk.menu.security.configweakpassword','MENU','1010190003382255616','/security/configweakpassword','GET',NULL,'r','',NULL,'anticon-file-protect','n','n','n','y','981334679749656576','安全配置',7,NULL,NULL,NULL,'1','2025-03-05 08:23:10','1','y'),('1010263020040880128','访问权限-用户','mxk.menu.access.permissions','MENU','1010263020040880128','/access/accessuser','GET',NULL,'r',NULL,NULL,'anticon-check-square','n','n','n','n','981336115820625920','访问控制',2,NULL,NULL,NULL,'1','2025-02-08 03:35:08','1','y'),('1010265410274066432','权限管理-用户','mxk.menu.permissions.privileges','MENU','1010265410274066432','/permissions/apps/permissionuser','GET',NULL,'r',NULL,NULL,'anticon-carry-out','n','n','n','n','981569048993071104','权限管理',1,NULL,NULL,NULL,NULL,NULL,'1','n'),('1010626148297605120','账号管理','mxk.menu.accounts.accounts','MENU','1010626148297605120','/accounts/accounts','GET',NULL,'r',NULL,NULL,'anticon-idcard','n','n','n','y','981334447594930176','账号管理',1,NULL,NULL,NULL,'1','2025-01-08 07:04:36','1','y'),('1010626913313488896','联合账号','mxk.menu.accounts.accountsunited','MENU','1010626913313488896','/accounts/accountsunited','GET',NULL,'r',NULL,NULL,'anticon-idcard','n','n','n','y','981334447594930176','账号管理',2,NULL,NULL,NULL,'1','2025-01-08 07:05:00','1','y'),('1028357050226180096','访问权限-组织','mxk.menu.access.permissions','MENU','1028357050226180096','/access/accessorg','GET',NULL,'r',NULL,NULL,'anticon-check-square','n','n','n','n','981336115820625920','访问权限',3,NULL,'1','2024-09-03 06:44:52','1','2025-02-08 03:35:10','1','y'),('1028746543856877568','权限管理-组织','mxk.menu.permissions.privileges','MENU','1028746543856877568','/permissions/apps/permissionorg','GET',NULL,'r',NULL,NULL,'anticon-carry-out','n','n','n','n','981569048993071104','权限管理',2,NULL,'1','2024-09-04 08:32:35','1','2024-09-04 08:32:57','1','n'),('1029722139441233920','连接器','mxk.menu.config.connectors','MENU','1029722139441233920','/config/connectors','GET','mxk.menu.config.connectors','r',NULL,NULL,'anticon-node-expand','n','n','n','y','981334814802051072','配置管理',11,NULL,'1','2024-09-07 01:09:15','1','2025-03-05 09:19:15','1','y'),('1869692874272862209','凭证','mxk.menu.books','MENU','1869692874272862209','','GET',NULL,'r',NULL,NULL,'anticon-money','n','n','n','y','1','MaxKey管理系统',2,NULL,'1','2024-12-19 10:34:53','1','2025-05-07 10:12:36','1','n'),('1869927956917002242','会计科目','mxk.menu.subjects','MENU','1869927956917002242','/books/subjects','GET',NULL,'r',NULL,NULL,NULL,NULL,NULL,'n','y','981334814802051072','身份管理',1,NULL,'1','2024-12-20 02:09:01','1','2025-01-14 09:05:04','1','y'),('1872485556229599233','会计准则','mxk.menu.accountingStandard','MENU','1872485556229599233','/config/standard','GET',NULL,'r',NULL,NULL,'menus-01-7caiwuguanli-caiwuxitongdaima-huijizhunze','n','n','n','y','1915219176348123138','会计科目',1,NULL,'1','2024-12-27 03:32:00','1','2025-05-04 13:13:30','1','n'),('1874027145762447361','账套管理','mxk.meun.booksSet','MENU','1874027145762447361','/books/index','GET',NULL,'r',NULL,NULL,'menus-zhangtaoguanli','n','n','n','y','981334814802051072','财务管理',1,NULL,'1','2024-12-31 09:37:43','1','2025-05-05 03:33:55','1','n'),('1879028005231357953','新增凭证','mxk.menu.recordingVoucher','MENU','1879028005231357953','/voucher/voucher-edit','GET','{\"c\":\"t\"}','r',NULL,NULL,'menus-xinzengpingzheng-selected','n','n','n','y','1869692874272862209','财务管理',1,NULL,'1','2025-01-14 04:49:21','1','2025-05-12 01:33:37','1','n'),('1879423541940375554','会计科目','mxk.menu.subject','MENU','1879423541940375554','/config/standard-subject','GET',NULL,'r',NULL,NULL,'menus-huijikemu','n','n','n','y','1915219176348123138','配置管理',2,NULL,'1','2025-01-15 07:01:05','1','2025-05-04 13:13:50','1','n'),('1879553833064067074','凭证管理','mxk.menu.voucher','MENU','1879553833064067074','/voucher/voucher-index','GET','{\"c\":\"t\"}','r',NULL,NULL,'menus-pingzhengguanli','n','n','n','y','1869692874272862209','财务管理',1,NULL,'1','2025-01-15 15:38:48','1','2025-05-12 01:33:42','1','n'),('1881534934875557889','出纳','mxk.menu.journal','MENU','1881534934875557889','/','GET',NULL,'r',NULL,NULL,'menus-chunaguanli','n','n','n','y','1','JinBooks',3,NULL,'1','2025-01-21 02:51:00','1','2025-05-30 00:22:00','1','n'),('1881535430596153345','日记账','mxk.menu.journal.journalentry','MENU','1881535430596153345','/journal/journalentry','GET',NULL,'r',NULL,NULL,'menus-rijizhang','n','n','n','y','1881534934875557889','出纳',1,NULL,'1','2025-01-21 02:52:58','1','2025-05-04 10:38:05','1','n'),('1881535629171281921','账户管理','mxk.menu.journal.journalaccout','MENU','1881535629171281921','/journal/journalaccout','GET',NULL,'r',NULL,NULL,'menus-zhanghuguanli','n','n','n','y','1881534934875557889','出纳',2,NULL,'1','2025-01-21 02:53:45','1','2025-05-04 10:38:12','1','n'),('1881633896221446146','员工管理','mxk.meun.employee','MENU','1881633896221446146','/hr/employee','GET',NULL,'r',NULL,NULL,'menus-qiyeguanli_yuangongguanli','n','n','n','y','981334321270882304',NULL,2,NULL,'1','2025-01-21 09:24:14','1','2025-05-04 10:38:51','1','n'),('1886357455563137026','报表','mxk.menu.booksReport','MENU','1886357455563137026',NULL,'GET',NULL,'r',NULL,NULL,'menus-caiwubaobiao','n','n','n','y','1','JinBooks',6,NULL,'1','2025-02-03 10:13:58','1','2025-05-07 10:14:35','1','n'),('1886366126259052545','资产负债表','mxk.menu.balanceSheet','MENU','1886366126259052545','/statement/balance-sheet','GET',NULL,'r',NULL,NULL,'menus-a-yusuanguanli2','n','n','n','y','1886357455563137026','财务报表',1,NULL,'1','2025-02-03 10:48:26','1','2025-05-04 10:44:22','1','n'),('1886384073945915394','利润表','mkt.menu.incomeStatement','MENU','1886384073945915394','/statement/income-statement','GET',NULL,'r',NULL,NULL,'menus-lirunbiao','n','n','n','y','1886357455563137026','财务报表',2,NULL,'1','2025-02-03 11:59:45','1','2025-05-04 10:44:31','1','n'),('1886384309938429954','现金流量表','mkt.menu.cashFlowStatement','MENU','1886384309938429954','/statement/cash-flow-statement','GET',NULL,'r',NULL,NULL,'menus-xianjinliuliangbiao','n','n','n','y','1886357455563137026','财务报表',3,NULL,'1','2025-02-03 12:00:41','1','2025-05-04 10:44:49','1','n'),('1886384516205912065','科目余额表','mkt.menu.accountingStandards','MENU','1886384516205912065','/statement/subject-balance','GET',NULL,'r',NULL,NULL,'menus-kemuyuebiao','n','n','n','y','1886357455563137026','财务报表',4,NULL,'1','2025-02-03 12:01:30','1','2025-05-04 10:44:57','1','n'),('1887317090379808769','个人税率设置','mxk.meun.tax','MENU','1887317090379808769','/config/tax','GET','','r',NULL,NULL,'menus-shuishuaishezhi','n','n','n','y','981334814802051072',NULL,9,NULL,'1','2025-02-06 01:47:13','1','2025-05-04 15:00:42','1','n'),('1888073658178420737','账户汇总','mxk.menu.journal.journalsummary','MENU','1888073658178420737','/journal/journalsummary','GET',NULL,'r',NULL,NULL,'menus-zhanghuhuizong','n','n','n','y','1881534934875557889','出纳',3,NULL,'1','2025-02-08 03:53:33','1','2025-05-04 10:38:18','1','n'),('1888142236179025921','薪资计算公式','mxk.menu.formula','MENU','1888142236179025921','/config/formula','GET',NULL,'r',NULL,NULL,NULL,'n','n','n','n','981334814802051072',NULL,4,NULL,'1','2025-02-08 08:26:03','1','2025-04-10 04:03:00','1','y'),('1889594633392771074','社保公积金','mxk.meun.insurance.fund','MENU','1889594633392771074','/config/insurance-fund','GET',NULL,'r',NULL,NULL,'money-collect','n','n','n','y','981334814802051072',NULL,8,NULL,'1','2025-02-12 08:37:22','1','2025-05-04 15:01:37','1','n'),('1889966284907286529','工资明细','mxk.menu.salary','MENU','1889966284907286529','/hr/salary-detail','GET',NULL,'r',NULL,NULL,'menus-icon-gongzimingxi-yingfagongzi','n','n','n','y','981334321270882304','HR管理',4,NULL,'1','2025-02-13 09:14:10','1','2025-05-16 06:33:50','1','n'),('1890934113406619650','税务个人扣除','mxk.menu.hr.employee.taxdeduction','MENU','1890934113406619650','/hr/employee-tax-deduction','GET',NULL,'r',NULL,NULL,'menus-zengzhishuishenbaonashuishiyongyuxiaoguimonashuiren','n','n','n','y','981334321270882304','HR管理',6,NULL,'1','2025-02-16 01:19:59','1','2025-05-16 06:33:59','1','n'),('1891486309700673537','凭证汇总表','凭证汇总表','MENU','1891486309700673537','/statement/voucher-summary','GET',NULL,'r',NULL,NULL,'menus-caiwu-pingzhenghuizongbiao','n','n','n','y','1869692874272862209','凭证',3,NULL,'1','2025-02-17 13:54:13','1','2025-05-04 10:45:11','1','n'),('1894665979168575489','当月工资计算','mxk.menu.calculateSalary','MENU','1894665979168575489','/hr/calc-salary','GET',NULL,'r',NULL,NULL,'menus-gongzifafang','n','n','n','y','981334321270882304','HR管理',3,NULL,'1','2025-02-26 08:29:05','1','2025-05-16 06:33:44','1','n'),('1895302065003790337','工资总览','mxk.menu.salary-summary','MENU','1895302065003790337','/hr/salary-summary','GET',NULL,'r',NULL,NULL,'menus-gongzizonglan','n','n','n','y','981334321270882304','HR管理',5,NULL,'1','2025-02-28 02:36:39','1','2025-05-16 06:33:54','1','n'),('1899349787629346818','工资凭证规则','mxk.menu.salary-voucher-rule','MENU','1899349787629346818','/hr/salary-voucher-rules','GET',NULL,'r',NULL,NULL,'menus-pingzhengguize','n','n','n','y','981334321270882304','HR管理',7,NULL,'1','2025-03-11 06:40:52','1','2025-05-16 06:34:03','1','n'),('1899369820127911938','初始余额','初始余额','MENU','1899369820127911938','/config/initBalance/index','GET',NULL,'r',NULL,NULL,'menus-kemuyuebiao','n','n','n','y','981334814802051072','配置管理',3,NULL,'1','2025-03-11 08:00:28','1','2025-05-05 03:27:10','1','n'),('1899760631214723073','系统参数','系统参数','MENU','1899760631214723073','/config/sys','GET',NULL,'r',NULL,NULL,'menus-xitongcanshu','n','n','n','y','981334814802051072','配置管理',2,NULL,'1','2025-03-12 09:53:24','1','2025-05-04 14:59:28','1','n'),('1902625741973843969','现金流量初始余额','mxk.menu.cash-flow-balance','MENU','1902625741973843969','/config/cash-flow-balance','GET',NULL,'r',NULL,NULL,'menus-xianjinliuliangbiao','n','n','n','y','981334814802051072','配置管理',6,NULL,'1','2025-03-20 07:38:20','1','2025-05-04 15:00:01','1','n'),('1903024792422047745','明细账','明细账','MENU','1903024792422047745','/voucher/sub-ledger','GET',NULL,'r',NULL,NULL,'menus-wanglaimingxizhang','n','n','n','y','1886357455563137026','财务报表',6,NULL,'1','2025-03-21 10:04:01','1','2025-05-04 10:45:25','1','n'),('1911018836640149506','利润表模板','mxk.meun.standardincomestatement','MENU','1911018836640149506','/config/standard-income-statement','GET',NULL,'r',NULL,NULL,'menus-moban','n','n','n','y','1915219176348123138','配置管理',5,NULL,'1','2025-04-12 11:29:30','1','2025-05-08 11:49:14','1','n'),('1911261101908295681','资产负债表模板','mxk.meun.standardbalancesheet','MENU','1911261101908295681','/config/standard-balance-sheet','GET',NULL,'r',NULL,NULL,'menus-baobiaomoban','n','n','n','y','1915219176348123138','配置管理',4,NULL,'1','2025-04-13 03:32:10','1','2025-05-08 11:49:18','1','n'),('1913072049310191618','科目现金流量项配置','mxk.menu.subject-cash-flow','MENU','1913072049310191618','/config/subject-cash-flow','GET',NULL,'r',NULL,NULL,'menus-xianjinliuliangbiao','n','n','n','y','981334814802051072','配置管理',7,NULL,'1','2025-04-18 03:28:14','1','2025-05-04 15:01:06','1','n'),('1915219176348123138','准则管理','准则管理','MENU','1915219176348123138',NULL,'GET',NULL,'r',NULL,NULL,'menus-guizhangzhunze','n','n','n','y','1','JinBooks',8,NULL,'1','2025-04-24 01:40:09','1','2025-05-04 13:13:12','1','n'),('1917420357065609218','结账','mxk.menu.settlement','MENU','1917420357065609218','','GET',NULL,'r',NULL,NULL,'menus-jiezhang1','n','n','n','y','1','JinBooks',5,NULL,'1','2025-04-30 03:26:51','1','2025-05-30 00:23:10','1','n'),('1917421261886033922','期末处理','期末处理','MENU','1917421261886033922','/settlement/carry-forward','GET','{ \"tab\": \"carry-forward\" }','r',NULL,NULL,'menus-qimochuli','n','n','n','y','1917420357065609218','结账',1,NULL,'1','2025-04-30 03:30:27','1','2025-05-12 01:09:34','1','n'),('1917421313257869313','结账','结账','MENU','1917421313257869313','/settlement/settle-period','GET','{ \"tab\": \"settle-period\" }','r',NULL,NULL,'menus-jiezhang','n','n','n','y','1917420357065609218','结账',2,NULL,'1','2025-04-30 03:30:39','1','2025-05-12 01:50:07','1','n'),('1917421497123573762','账期列表','账期列表','MENU','1917421497123573762','/settlement/settle-list','GET','{ \"tab\": \"settle-list\" }','r',NULL,NULL,'menus-zhangqiguanli','n','n','n','y','1917420357065609218','结账',3,NULL,'1','2025-04-30 03:31:23','1','2025-05-12 01:49:59','1','n'),('1920446221202178049','凭证模板','mxk.menu.vouchertemplate','MENU','1920446221202178049','/voucher/voucher-template','GET',NULL,'r',NULL,NULL,'code','n','n','n','y','1915219176348123138','准则管理',3,NULL,'1','2025-05-08 11:50:33','1','2025-05-08 11:54:11','1','n'),('981331493802475520','仪表盘','mxk.menu.home','MENU','981331493802475520','/index','GET',NULL,'r','',NULL,'anticon-home','n','n','n','y','1','MaxKey管理系统',1,NULL,NULL,NULL,'1','2025-05-04 06:00:02','1','n'),('981334321270882304','薪资','mxk.menu.identities','MENU','981334321270882304','','GET',NULL,'r',NULL,NULL,'anticon-user','n','n','n','y','1','MaxKey管理系统',4,NULL,NULL,NULL,'1','2025-05-30 00:23:04','1','n'),('981334447594930176','账号管理','mxk.menu.accounts','MENU','981334447594930176','','GET',NULL,'r',NULL,NULL,'anticon-idcard','n','n','n','y','1','MaxKey管理系统',3,NULL,NULL,NULL,'1','2025-01-08 07:04:48','1','y'),('981334616696684544','访问控制','mxk.menu.access','MENU','981334616696684544','/access/access','GET',NULL,'r',NULL,NULL,'anticon-safety','n','n','n','y','1','MaxKey管理系统',5,NULL,NULL,NULL,'1','2025-02-08 03:35:21','1','y'),('981334679749656576','配置管理','mxk.menu.permissions','MENU','981334679749656576','','GET',NULL,'r',NULL,NULL,'anticon-radar-chart','n','n','n','y','1','MaxKey管理系统',9,NULL,NULL,NULL,'1','2025-05-25 07:24:59','1','n'),('981334750088134656','安全配置','mxk.menu.security','MENU','981334750088134656','','GET',NULL,'r',NULL,NULL,'anticon-file-protect','n','n','n','y','1','MaxKey管理系统',10,NULL,NULL,NULL,'1','2025-05-25 07:24:45','1','y'),('981334814802051072','账套管理','mxk.menu.config','MENU','981334814802051072','','GET',NULL,'r',NULL,NULL,'anticon-setting','n','n','n','y','1','MaxKey管理系统',7,NULL,NULL,NULL,'1','2025-05-07 10:11:45','1','n'),('981334866064834560','日志审计','mxk.menu.audit','MENU','981334866064834560','','GET',NULL,'r',NULL,NULL,'anticon-history','n','n','n','y','1','MaxKey管理系统',11,NULL,NULL,NULL,'1','2025-04-30 03:27:06','1','n'),('981335709019275264','组织','mxk.menu.organizations','MENU','981335709019275264','/idm/organizations','GET',NULL,'r',NULL,NULL,'anticon-cluster','n','n','n','y','981334321270882304','身份管理',1,NULL,NULL,NULL,NULL,NULL,'1','n'),('981335758977630208','用户管理','mxk.menu.users','MENU','981335758977630208','/idm/users','GET',NULL,'r',NULL,NULL,'anticon-user','n','n','n','y','981334679749656576','身份管理',1,NULL,NULL,NULL,'1','2025-02-16 10:45:24','1','n'),('981335810039087104','角色管理','mxk.menu.access.groups','MENU','981335810039087104','/idm/groups','GET',NULL,'r',NULL,NULL,'anticon-group','n','n','n','y','981334679749656576','身份管理',2,NULL,NULL,NULL,'1','2025-02-16 10:45:32','1','n'),('981336054843834368','会话','mxk.menu.access.sessions','MENU','981336054843834368','/access/sessions','GET',NULL,'r',NULL,NULL,'anticon-eye','n','n','n','y','981334679749656576','权限管理',5,NULL,NULL,NULL,'1','2025-02-16 10:45:58','1','n'),('981336115820625920','访问权限','mxk.menu.access.permissions','MENU','981336115820625920','/access/access','GET',NULL,'r',NULL,NULL,'anticon-check-square','n','n','n','y','981334616696684544','访问控制',2,NULL,NULL,NULL,'1','2025-02-08 03:35:15','1','y'),('981336254564007936','社交服务','mxk.menu.security.socialsproviders','MENU','981336254564007936','/security/socialsproviders','GET',NULL,'r',NULL,NULL,'anticon-comment','n','n','n','n','981334679749656576','安全配置',6,NULL,NULL,NULL,'1','2025-05-25 07:23:46','0','n'),('981336295106150400','LDAP配置','mxk.menu.security.ldapcontext','MENU','981336295106150400','/security/ldapcontext','GET',NULL,'r',NULL,NULL,'anticon-database','n','n','n','y','981334679749656576','安全配置',2,NULL,NULL,NULL,'1','2025-02-16 10:51:31','1','y'),('981336354157756416','电子邮箱','mxk.menu.security.emailsenders','MENU','981336354157756416','/security/emailsender','GET',NULL,'r',NULL,NULL,'anticon-mail','n','n','n','y','981334679749656576','安全配置',7,NULL,NULL,NULL,'1','2025-05-25 07:23:53','1','n'),('981336403415662592','短信服务','mxk.menu.security.smsproviders','MENU','981336403415662592','/security/smsprovider','GET',NULL,'r',NULL,NULL,'anticon-send','n','n','n','y','981334679749656576','安全配置',8,NULL,NULL,NULL,'1','2025-05-25 07:24:05','1','n'),('981336473196298240','登录策略','mxk.menu.security.configloginpolicy','MENU','981336473196298240','/security/configloginpolicy','GET',NULL,'r',NULL,NULL,'anticon-file-protect','n','n','n','y','981334679749656576','安全配置',9,NULL,NULL,NULL,'1','2025-05-25 07:24:12','1','n'),('981336523834130432','密码策略','mxk.menu.security.passwordpolicy','MENU','981336523834130432','/security/passwordpolicy','GET',NULL,'r',NULL,NULL,'anticon-file-protect','n','n','n','y','981334679749656576','安全配置',10,NULL,NULL,NULL,'1','2025-05-25 07:24:25','1','n'),('981336571607252992','密码管理','mxk.menu.security.configpasswordencrypt','MENU','981336571607252992','/security/configpasswordencrypt','GET',NULL,'r',NULL,NULL,'anticon-file-protect','n','n','n','y','981334679749656576','安全配置',6,NULL,NULL,NULL,'1','2025-03-05 08:23:12','1','y'),('981336628184219648','机构管理','mxk.menu.config.institutions','MENU','981336628184219648','/config/institutions','GET',NULL,'r','',NULL,'anticon-appstore','n','n','n','y','981334814802051072','配置管理',1,NULL,NULL,NULL,'1','2025-03-05 08:14:58','1','y'),('981336686065614848','应用分类','mxk.menu.config.appscategory','MENU','981336686065614848','/config/appscategory','GET',NULL,'r',NULL,NULL,'anticon-wallet','n','n','n','y','981334814802051072','配置管理',2,NULL,NULL,NULL,'1','2025-02-16 11:34:25','1','y'),('981336734983782400','账号策略','mxk.menu.config.accountsstrategys','MENU','981336734983782400','/accounts/accountsstrategys','GET',NULL,'r',NULL,NULL,'anticon-control','n','n','n','y','981334447594930176','账号管理',3,NULL,NULL,NULL,'1','2025-01-08 07:05:19','1','y'),('981336810628055040','同步器管理','mxk.menu.config.synchronizers','MENU','981336810628055040','/config/synchronizers','GET',NULL,'r',NULL,NULL,'anticon-node-collapse','n','n','n','y','981334814802051072','配置管理',10,NULL,NULL,NULL,'1','2025-04-30 02:21:09','0','n'),('981336886515597312','适配器注册','mxk.menu.config.adapters','MENU','981336886515597312','/config/adapters','GET',NULL,'r',NULL,NULL,'anticon-wallet','n','n','n','y','981334814802051072','配置管理',6,NULL,NULL,NULL,'1','2025-02-18 07:57:47','1','y'),('981336954593345536','扩展属性','mxk.menu.config.expandattrs','MENU','981336954593345536','/config/expandattrs','GET',NULL,'r',NULL,NULL,'anticon-expand','n','n','n','y','981334814802051072','配置管理',7,NULL,NULL,NULL,'1','2025-02-17 09:36:15','1','y'),('981337003041751040','登录日志','mxk.menu.audit.logins','MENU','981337003041751040','/audit/audit-logins','GET',NULL,'r',NULL,NULL,'anticon-audit','n','n','n','y','981334866064834560','日志审计',1,NULL,NULL,NULL,'1','2024-12-19 09:48:33','1','n'),('981337043332235264','访问日志','mxk.menu.audit.loginapps','MENU','981337043332235264','/audit/audit-login-apps','GET',NULL,'r',NULL,NULL,'anticon-audit','n','n','n','y','981334866064834560','日志审计',2,NULL,NULL,NULL,'1','2025-02-16 11:35:13','1','y'),('981337094406275072','同步器日志','mxk.menu.audit.synchronizer','MENU','981337094406275072','/audit/audit-synchronizer','GET',NULL,'r',NULL,NULL,'anticon-audit','n','n','n','y','981334866064834560','日志审计',3,NULL,NULL,NULL,'1','2025-05-06 02:10:49','0','n'),('981337140065468416','连接器日志','mxk.menu.audit.connector','MENU','981337140065468416','/audit/audit-connector','GET',NULL,'r',NULL,NULL,'anticon-audit','n','n','n','y','981334866064834560','日志审计',4,NULL,NULL,NULL,'1','2025-02-18 07:57:41','1','y'),('981337181773627392','系统日志','mxk.menu.audit.operate','MENU','981337181773627392','/audit/audit-system-logs','GET',NULL,'r',NULL,NULL,'anticon-audit','n','n','n','y','981334866064834560','日志审计',2,NULL,NULL,NULL,'1','2025-02-16 11:35:23','1','n'),('981337246718230528','资源管理','mxk.menu.permissions.resources','MENU','981337246718230528','/permissions/apps/resources','GET',NULL,'r',NULL,NULL,'anticon-read','n','n','n','y','981334679749656576','权限管理',3,NULL,NULL,NULL,'1','2025-02-16 10:45:42','1','n'),('981337555771326464','权限分配','mxk.menu.permissions.openapi','MENU','981337555771326464','/permissions/apps/permission','GET',NULL,'r',NULL,NULL,'anticon-carry-out','n','n','n','y','981334679749656576','权限管理',4,NULL,NULL,NULL,'1','2025-02-16 10:45:51','1','n'),('981568925764419584','角色管理','mxk.menu.permissions.roles','MENU','981568925764419584','/permissions/apps/roles','GET',NULL,'r',NULL,NULL,'anticon-carry-out','n','n','n','n','981337246718230528','资源管理',3,NULL,NULL,NULL,NULL,NULL,'1','n'),('981569048993071104','权限管理','mxk.menu.permissions.privileges','MENU','981569048993071104','/permissions/apps/permission','GET',NULL,'r',NULL,NULL,'anticon-carry-out','n','n','n','n','981337246718230528','资源管理',2,NULL,NULL,NULL,NULL,NULL,'1','n'),('981569201816731648','角色成员管理','mxk.menu.permissions.rolemembers','MENU','981569201816731648','/permissions/apps/rolemembers','GET',NULL,'r',NULL,NULL,'anticon-carry-out','n','n','n','n','981337246718230528','资源管理',4,NULL,NULL,NULL,NULL,NULL,'1','n'),('981570045970743296','权限授权角色',NULL,'MENU','981570045970743296',NULL,'GET',NULL,'r',NULL,NULL,NULL,'n','n','n','n','981568925764419584','角色管理',1,NULL,NULL,NULL,NULL,NULL,'1','n'),('981623658751459328','资源管理','mxk.menu.permissions.resources.resources','MENU','981623658751459328','/permissions/apps/resources','GET',NULL,'r',NULL,NULL,'anticon-read','n','n','n','n','981337246718230528','应用资源',1,NULL,NULL,NULL,'1','2025-02-18 07:48:56','1','n'),('981623658751459329','辅助核算','mxk.menu.assistAcc','MENU','981623658751459329','/config/assistAcc/index','GET',NULL,'r',NULL,NULL,'menus-fuzhuhesuan','n','n','n','y','981334814802051072','配置管理',5,NULL,NULL,NULL,'1','2025-05-05 03:27:16','1','n');
/*!40000 ALTER TABLE `resources` ENABLE KEYS */;
UNLOCK TABLES;

-- permission
LOCK TABLES `permission` WRITE;
/*!40000 ALTER TABLE `permission` DISABLE KEYS */;
INSERT INTO `permission` VALUES ('1010190520619630632','ROLE_ADMINISTRATORS','1010190003382255616','1','2024-07-15 11:37:31',9,'1'),('1010190520619630633','ROLE_ADMINISTRATORS','1010190003382255616','1','2024-07-15 11:37:31',9,'1'),('1010264047334981710','ROLE_ADMINISTRATORS','1010263020040880128','1','2024-07-15 16:29:42',9,'1'),('1010264047334981711','ROLE_ADMINISTRATORS','1010263020040880128','1','2024-07-15 16:29:42',9,'1'),('1010265593976193088','ROLE_ADMINISTRATORS','1010265410274066432','1','2024-07-15 16:35:50',1,'1'),('1010265593976193089','ROLE_ADMINISTRATORS','1010265410274066432','1','2024-07-15 16:35:50',1,'1'),('1010626382729838678','ROLE_ADMINISTRATORS','1010626148297605120','1','2024-07-16 16:29:32',9,'1'),('1010626382729838679','ROLE_ADMINISTRATORS','1010626148297605120','1','2024-07-16 16:29:32',9,'1'),('1010627043437576278','ROLE_ADMINISTRATORS','1010626913313488896','1','2024-07-16 16:32:10',9,'1'),('1010627043437576279','ROLE_ADMINISTRATORS','1010626913313488896','1','2024-07-16 16:32:10',9,'1'),('1029722536780234783','ROLE_ADMINISTRATORS','1028357050226180096','1','2024-09-07 09:10:49',9,'1'),('1029722536780234784','ROLE_ADMINISTRATORS','1028357050226180096','1','2024-09-07 09:10:49',9,'1'),('1029722536780234797','ROLE_ADMINISTRATORS','1028746543856877568','1','2024-09-07 09:10:49',1,'1'),('1029722536780234798','ROLE_ADMINISTRATORS','1028746543856877568','1','2024-09-07 09:10:49',1,'1'),('1029722536780234839','ROLE_ADMINISTRATORS','1029722139441233920','1','2024-09-07 09:10:49',9,'1'),('1029722536780234840','ROLE_ADMINISTRATORS','1029722139441233920','1','2024-09-07 09:10:49',9,'1'),('1869694161779658754','1869687211243282434','1869692874272862209','1','2024-12-19 18:26:41',1,'1'),('1869694161779658755','1869687211243282434','981331493802475520','1','2024-12-19 18:26:41',1,'1'),('1869694161779658756','1869687211243282434','981334321270882304','1','2024-12-19 18:26:41',1,'1'),('1869694161779658757','1869687211243282434','981335709019275264','1','2024-12-19 18:26:41',1,'1'),('1869694161779658758','1869687211243282434','981335758977630208','1','2024-12-19 18:26:41',1,'1'),('1869694161779658759','1869687211243282434','981334447594930176','1','2024-12-19 18:26:41',9,'1'),('1869694161779658760','1869687211243282434','1010626148297605120','1','2024-12-19 18:26:41',9,'1'),('1869694161779658761','1869687211243282434','1010626913313488896','1','2024-12-19 18:26:41',9,'1'),('1869694161779658762','1869687211243282434','981336734983782400','1','2024-12-19 18:26:41',9,'1'),('1869694161779658763','1869687211243282434','981334616696684544','1','2024-12-19 18:26:41',1,'1'),('1869694161779658764','1869687211243282434','981336054843834368','1','2024-12-19 18:26:41',1,'1'),('1869694161779658765','1869687211243282434','981336115820625920','1','2024-12-19 18:26:41',9,'1'),('1869694161779658766','1869687211243282434','1010263020040880128','1','2024-12-19 18:26:41',1,'1'),('1869694161779658767','1869687211243282434','1028357050226180096','1','2024-12-19 18:26:41',1,'1'),('1869694161779658768','1869687211243282434','981334679749656576','1','2024-12-19 18:26:41',1,'1'),('1869694161779658769','1869687211243282434','981335810039087104','1','2024-12-19 18:26:41',1,'1'),('1869694161779658770','1869687211243282434','981337246718230528','1','2024-12-19 18:26:41',1,'1'),('1869694161779658771','1869687211243282434','981568925764419584','1','2024-12-19 18:26:41',1,'1'),('1869694161779658772','1869687211243282434','981570045970743296','1','2024-12-19 18:26:41',1,'1'),('1869694161779658773','1869687211243282434','981569048993071104','1','2024-12-19 18:26:41',1,'1'),('1869694161779658774','1869687211243282434','1010265410274066432','1','2024-12-19 18:26:41',1,'1'),('1869694161779658775','1869687211243282434','1028746543856877568','1','2024-12-19 18:26:41',1,'1'),('1869694161779658776','1869687211243282434','981569201816731648','1','2024-12-19 18:26:41',1,'1'),('1869694161779658777','1869687211243282434','981623658751459328','1','2024-12-19 18:26:41',1,'1'),('1869694161779658778','1869687211243282434','981337555771326464','1','2024-12-19 18:26:41',1,'1'),('1869694161779658779','1869687211243282434','981334750088134656','1','2024-12-19 18:26:41',1,'1'),('1869694161779658780','1869687211243282434','1010190003382255616','1','2024-12-19 18:26:41',1,'1'),('1869694161779658781','1869687211243282434','981336254564007936','1','2024-12-19 18:26:41',1,'1'),('1869694161779658782','1869687211243282434','981336295106150400','1','2024-12-19 18:26:41',1,'1'),('1869694161779658783','1869687211243282434','981336354157756416','1','2024-12-19 18:26:41',1,'1'),('1869694161779658784','1869687211243282434','981336403415662592','1','2024-12-19 18:26:41',1,'1'),('1869694161779658785','1869687211243282434','981336473196298240','1','2024-12-19 18:26:41',1,'1'),('1869694161779658786','1869687211243282434','981336523834130432','1','2024-12-19 18:26:41',1,'1'),('1869694161779658787','1869687211243282434','981336571607252992','1','2024-12-19 18:26:41',1,'1'),('1869694161779658788','1869687211243282434','981334814802051072','1','2024-12-19 18:26:41',1,'1'),('1869694161779658789','1869687211243282434','1029722139441233920','1','2024-12-19 18:26:41',1,'1'),('1869694161779658790','1869687211243282434','981336628184219648','1','2024-12-19 18:26:41',1,'1'),('1869694161779658791','1869687211243282434','981336686065614848','1','2024-12-19 18:26:41',1,'1'),('1869694161779658792','1869687211243282434','981336810628055040','1','2024-12-19 18:26:41',1,'1'),('1869694161779658793','1869687211243282434','981336886515597312','1','2024-12-19 18:26:41',1,'1'),('1869694161779658794','1869687211243282434','981336954593345536','1','2024-12-19 18:26:41',1,'1'),('1869694161779658795','1869687211243282434','981334866064834560','1','2024-12-19 18:26:41',1,'1'),('1869694161779658796','1869687211243282434','981337003041751040','1','2024-12-19 18:26:41',1,'1'),('1869694161779658797','1869687211243282434','981337043332235264','1','2024-12-19 18:26:41',1,'1'),('1869694161779658798','1869687211243282434','981337094406275072','1','2024-12-19 18:26:41',1,'1'),('1869694161779658799','1869687211243282434','981337140065468416','1','2024-12-19 18:26:41',1,'1'),('1869694161779658800','1869687211243282434','981337181773627392','1','2024-12-19 18:26:41',1,'1'),('1869694667566583810','ROLE_ADMINISTRATORS','1869692874272862209','1','2024-12-19 18:28:42',1,'1'),('1869928013762404355','ROLE_ADMINISTRATORS','1869927956917002242','1','2024-12-20 09:55:56',9,'1'),('1872487614252937219','ROLE_ADMINISTRATORS','1872485556229599233','1','2024-12-27 11:26:53',1,'1'),('1874027180180905988','ROLE_ADMINISTRATORS','1874027145762447361','1','2024-12-31 17:37:53',1,'1'),('1879028288606924805','ROLE_ADMINISTRATORS','1879028005231357953','1','2025-01-14 12:50:27',1,'1'),('1879423652527394851','ROLE_ADMINISTRATORS','1879423541940375554','1','2025-01-15 15:01:32',1,'1'),('1879554096852234244','ROLE_ADMINISTRATORS','1879553833064067074','1','2025-01-15 23:39:20',1,'1'),('1881535724990156804','ROLE_ADMINISTRATORS','1881534934875557889','1','2025-01-21 10:54:06',1,'1'),('1881539750238539782','ROLE_ADMINISTRATORS','1881535430596153345','1','2025-01-21 11:10:06',1,'1'),('1881539750238539783','ROLE_ADMINISTRATORS','1881535629171281921','1','2025-01-21 11:10:06',1,'1'),('1881633998289833994','ROLE_ADMINISTRATORS','1881633896221446146','1','2025-01-21 17:24:39',1,'1'),('1886367743347793928','ROLE_ADMINISTRATORS','1886357455563137026','1','2025-02-03 18:53:18',1,'1'),('1886367743347793929','ROLE_ADMINISTRATORS','1886366126259052545','1','2025-02-03 18:53:18',1,'1'),('1886384669453197322','ROLE_ADMINISTRATORS','1886384073945915394','1','2025-02-03 20:00:34',1,'1'),('1886384669453197323','ROLE_ADMINISTRATORS','1886384309938429954','1','2025-02-03 20:00:34',1,'1'),('1886384669453197324','ROLE_ADMINISTRATORS','1886384516205912065','1','2025-02-03 20:00:34',1,'1'),('1887317285163286574','ROLE_ADMINISTRATORS','1887317090379808769','1','2025-02-06 09:48:00',1,'1'),('1888073919013797895','ROLE_ADMINISTRATORS','1888073658178420737','1','2025-02-08 11:54:35',1,'1'),('1888143684090515462','ROLE_ADMINISTRATORS','1888142236179025921','1','2025-02-08 16:31:49',9,'1'),('1889594731417849901','ROLE_ADMINISTRATORS','1889594633392771074','1','2025-02-12 16:37:46',1,'1'),('1889966334748200977','ROLE_ADMINISTRATORS','1889966284907286529','1','2025-02-13 17:14:23',1,'1'),('1890934349826953234','ROLE_ADMINISTRATORS','1890934113406619650','1','2025-02-16 09:20:54',1,'1'),('1891486391741259789','ROLE_ADMINISTRATORS','1891486309700673537','1','2025-02-17 21:54:30',1,'1'),('1891757515878993972','ROLE_ADMINISTRATORS','981334866064834560','1','2025-02-18 15:51:53',1,'1'),('1891854259067404290','ROLE_ADMINISTRATORS','1869692874272862209','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404291','ROLE_ADMINISTRATORS','1879028005231357953','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404292','ROLE_ADMINISTRATORS','1879553833064067074','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404293','ROLE_ADMINISTRATORS','1881534934875557889','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404294','ROLE_ADMINISTRATORS','1881535430596153345','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404295','ROLE_ADMINISTRATORS','1881535629171281921','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404296','ROLE_ADMINISTRATORS','1888073658178420737','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404297','ROLE_ADMINISTRATORS','1886357455563137026','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404298','ROLE_ADMINISTRATORS','1886366126259052545','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404299','ROLE_ADMINISTRATORS','1886384073945915394','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404300','ROLE_ADMINISTRATORS','1886384309938429954','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404301','ROLE_ADMINISTRATORS','1886384516205912065','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404302','ROLE_ADMINISTRATORS','1891486309700673537','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404303','ROLE_ADMINISTRATORS','981331493802475520','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404304','ROLE_ADMINISTRATORS','981334321270882304','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404305','ROLE_ADMINISTRATORS','1881633896221446146','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404306','ROLE_ADMINISTRATORS','1889966284907286529','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404307','ROLE_ADMINISTRATORS','1890934113406619650','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404308','ROLE_ADMINISTRATORS','981335709019275264','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404309','ROLE_ADMINISTRATORS','981334679749656576','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404310','ROLE_ADMINISTRATORS','981335758977630208','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404311','ROLE_ADMINISTRATORS','981335810039087104','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404312','ROLE_ADMINISTRATORS','981336054843834368','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404313','ROLE_ADMINISTRATORS','981337246718230528','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404314','ROLE_ADMINISTRATORS','981568925764419584','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404315','ROLE_ADMINISTRATORS','981570045970743296','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404316','ROLE_ADMINISTRATORS','981569048993071104','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404317','ROLE_ADMINISTRATORS','1010265410274066432','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404318','ROLE_ADMINISTRATORS','1028746543856877568','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404319','ROLE_ADMINISTRATORS','981569201816731648','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404320','ROLE_ADMINISTRATORS','981623658751459328','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404321','ROLE_ADMINISTRATORS','981337555771326464','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404322','ROLE_ADMINISTRATORS','981334750088134656','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404323','ROLE_ADMINISTRATORS','1010190003382255616','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404324','ROLE_ADMINISTRATORS','981336254564007936','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404325','ROLE_ADMINISTRATORS','981336354157756416','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404326','ROLE_ADMINISTRATORS','981336403415662592','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404327','ROLE_ADMINISTRATORS','981336473196298240','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404328','ROLE_ADMINISTRATORS','981336523834130432','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404329','ROLE_ADMINISTRATORS','981336571607252992','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404330','ROLE_ADMINISTRATORS','981334814802051072','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404331','ROLE_ADMINISTRATORS','1029722139441233920','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404332','ROLE_ADMINISTRATORS','1872485556229599233','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404333','ROLE_ADMINISTRATORS','1874027145762447361','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404334','ROLE_ADMINISTRATORS','1879423541940375554','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404335','ROLE_ADMINISTRATORS','1887317090379808769','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404336','ROLE_ADMINISTRATORS','1888142236179025921','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404337','ROLE_ADMINISTRATORS','1889594633392771074','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404338','ROLE_ADMINISTRATORS','981336628184219648','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404339','ROLE_ADMINISTRATORS','981336810628055040','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404340','ROLE_ADMINISTRATORS','981623658751459329','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404341','ROLE_ADMINISTRATORS','981334866064834560','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404342','ROLE_ADMINISTRATORS','981337003041751040','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404343','ROLE_ADMINISTRATORS','981337094406275072','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891854259067404344','ROLE_ADMINISTRATORS','981337181773627392','1','2025-02-18 22:16:18',1,'1891067901277212673'),('1891865019235454977','1880191070453612545','1881534934875557889','1','2025-02-18 22:59:04',1,'1891067901277212673'),('1894666691126513684','ROLE_ADMINISTRATORS','1894665979168575489','1','2025-02-26 16:31:55',1,'1'),('1895302116224630805','ROLE_ADMINISTRATORS','1895302065003790337','1','2025-02-28 10:36:51',1,'1'),('1899350001933115414','ROLE_ADMINISTRATORS','1899349787629346818','1','2025-03-11 14:41:43',1,'1'),('1899369899198930993','ROLE_ADMINISTRATORS','1899369820127911938','1','2025-03-11 16:00:45',1,'1'),('1899760677909909506','ROLE_ADMINISTRATORS','1899760631214723073','1','2025-03-12 17:53:33',1,'1'),('1902638582843928628','ROLE_ADMINISTRATORS','1902625741973843969','1','2025-03-20 16:29:23',1,'1'),('1903025122887065614','ROLE_ADMINISTRATORS','1903024792422047745','1','2025-03-21 18:05:17',1,'1'),('1911019323225550900','ROLE_ADMINISTRATORS','1911018836640149506','1','2025-04-12 19:31:25',1,'1'),('1911261690398507061','ROLE_ADMINISTRATORS','1911261101908295681','1','2025-04-13 11:34:30',1,'1'),('1913072145594634295','ROLE_ADMINISTRATORS','1913072049310191618','1','2025-04-18 11:28:38',1,'1'),('1915220075694981135','ROLE_ADMINISTRATORS','1915219176348123138','1','2025-04-24 09:43:43',1,'1'),('1917421619416895509','ROLE_ADMINISTRATORS','1917420357065609218','1','2025-04-30 11:31:52',1,'1'),('1917421619416895510','ROLE_ADMINISTRATORS','1917421261886033922','1','2025-04-30 11:31:52',1,'1'),('1917421619416895511','ROLE_ADMINISTRATORS','1917421313257869313','1','2025-04-30 11:31:52',1,'1'),('1917421619416895512','ROLE_ADMINISTRATORS','1917421497123573762','1','2025-04-30 11:31:52',1,'1'),('1920446587306197012','ROLE_ADMINISTRATORS','1920446221202178049','1','2025-05-08 19:52:01',1,'1'),('981556434976112640','ROLE_ADMINISTRATORS','981334866064834560','1','2024-03-09 18:24:54',9,'1'),('981556434976112641','ROLE_ADMINISTRATORS','981334814802051072','1','2024-03-09 18:24:54',1,'1'),('981556434976112642','ROLE_ADMINISTRATORS','981334750088134656','1','2024-03-09 18:24:54',1,'1'),('981556434976112643','ROLE_ADMINISTRATORS','981334679749656576','1','2024-03-09 18:24:54',1,'1'),('981556434976112644','ROLE_ADMINISTRATORS','981334616696684544','1','2024-03-09 18:24:54',9,'1'),('981556434976112645','ROLE_ADMINISTRATORS','981334560677560320','1','2024-03-09 18:24:54',9,'1'),('981556434976112646','ROLE_ADMINISTRATORS','981334447594930176','1','2024-03-09 18:24:54',9,'1'),('981556434976112647','ROLE_ADMINISTRATORS','981334321270882304','1','2024-03-09 18:24:54',1,'1'),('981556434976112648','ROLE_ADMINISTRATORS','981331493802475520','1','2024-03-09 18:24:54',1,'1'),('981651685308891136','ROLE_ADMINISTRATORS','981337181773627392','1','2024-03-10 01:14:57',1,'1'),('981651685308891137','ROLE_ADMINISTRATORS','981337181773627392','1','2024-03-10 01:14:57',1,'1'),('981651685308891138','ROLE_ADMINISTRATORS','981337140065468416','1','2024-03-10 01:14:57',9,'1'),('981651685308891139','ROLE_ADMINISTRATORS','981337140065468416','1','2024-03-10 01:14:57',9,'1'),('981651685308891140','ROLE_ADMINISTRATORS','981337094406275072','1','2024-03-10 01:14:57',1,'1'),('981651685308891141','ROLE_ADMINISTRATORS','981337094406275072','1','2024-03-10 01:14:57',1,'1'),('981651685308891142','ROLE_ADMINISTRATORS','981337043332235264','1','2024-03-10 01:14:57',9,'1'),('981651685308891143','ROLE_ADMINISTRATORS','981337043332235264','1','2024-03-10 01:14:57',9,'1'),('981651685308891144','ROLE_ADMINISTRATORS','981337003041751040','1','2024-03-10 01:14:57',1,'1'),('981651685308891145','ROLE_ADMINISTRATORS','981337003041751040','1','2024-03-10 01:14:57',1,'1'),('981651685308891148','ROLE_ADMINISTRATORS','981336954593345536','1','2024-03-10 01:14:57',9,'1'),('981651685308891149','ROLE_ADMINISTRATORS','981336954593345536','1','2024-03-10 01:14:57',9,'1'),('981651685308891150','ROLE_ADMINISTRATORS','981336886515597312','1','2024-03-10 01:14:57',9,'1'),('981651685308891151','ROLE_ADMINISTRATORS','981336886515597312','1','2024-03-10 01:14:57',9,'1'),('981651685308891152','ROLE_ADMINISTRATORS','981336810628055040','1','2024-03-10 01:14:57',1,'1'),('981651685308891153','ROLE_ADMINISTRATORS','981336810628055040','1','2024-03-10 01:14:57',1,'1'),('981651685308891154','ROLE_ADMINISTRATORS','981336734983782400','1','2024-03-10 01:14:57',9,'1'),('981651685308891155','ROLE_ADMINISTRATORS','981336734983782400','1','2024-03-10 01:14:57',9,'1'),('981651685308891156','ROLE_ADMINISTRATORS','981336686065614848','1','2024-03-10 01:14:57',9,'1'),('981651685308891157','ROLE_ADMINISTRATORS','981336686065614848','1','2024-03-10 01:14:57',9,'1'),('981651685308891158','ROLE_ADMINISTRATORS','981336628184219648','1','2024-03-10 01:14:57',9,'1'),('981651685308891159','ROLE_ADMINISTRATORS','981336628184219648','1','2024-03-10 01:14:57',9,'1'),('981651685308891162','ROLE_ADMINISTRATORS','981336571607252992','1','2024-03-10 01:14:57',9,'1'),('981651685308891163','ROLE_ADMINISTRATORS','981336571607252992','1','2024-03-10 01:14:57',9,'1'),('981651685308891164','ROLE_ADMINISTRATORS','981336523834130432','1','2024-03-10 01:14:57',1,'1'),('981651685308891165','ROLE_ADMINISTRATORS','981336523834130432','1','2024-03-10 01:14:57',1,'1'),('981651685308891166','ROLE_ADMINISTRATORS','981336473196298240','1','2024-03-10 01:14:57',1,'1'),('981651685308891167','ROLE_ADMINISTRATORS','981336473196298240','1','2024-03-10 01:14:57',1,'1'),('981651685308891168','ROLE_ADMINISTRATORS','981336403415662592','1','2024-03-10 01:14:57',1,'1'),('981651685308891169','ROLE_ADMINISTRATORS','981336403415662592','1','2024-03-10 01:14:57',1,'1'),('981651685308891170','ROLE_ADMINISTRATORS','981336354157756416','1','2024-03-10 01:14:57',1,'1'),('981651685308891171','ROLE_ADMINISTRATORS','981336354157756416','1','2024-03-10 01:14:57',1,'1'),('981651685308891172','ROLE_ADMINISTRATORS','981336295106150400','1','2024-03-10 01:14:57',9,'1'),('981651685308891173','ROLE_ADMINISTRATORS','981336295106150400','1','2024-03-10 01:14:57',9,'1'),('981651685308891174','ROLE_ADMINISTRATORS','981336254564007936','1','2024-03-10 01:14:57',1,'1'),('981651685308891175','ROLE_ADMINISTRATORS','981336254564007936','1','2024-03-10 01:14:57',1,'1'),('981651685308891178','ROLE_ADMINISTRATORS','981337970474745856','1','2024-03-10 01:14:57',9,'1'),('981651685308891179','ROLE_ADMINISTRATORS','981337970474745856','1','2024-03-10 01:14:57',9,'1'),('981651685308891180','ROLE_ADMINISTRATORS','981337924635197440','1','2024-03-10 01:14:57',9,'1'),('981651685308891181','ROLE_ADMINISTRATORS','981337924635197440','1','2024-03-10 01:14:57',9,'1'),('981651685308891182','ROLE_ADMINISTRATORS','981337882436304896','1','2024-03-10 01:14:57',9,'1'),('981651685308891183','ROLE_ADMINISTRATORS','981337882436304896','1','2024-03-10 01:14:57',9,'1'),('981651685308891184','ROLE_ADMINISTRATORS','981337620871118848','1','2024-03-10 01:14:57',9,'1'),('981651685308891185','ROLE_ADMINISTRATORS','981337620871118848','1','2024-03-10 01:14:57',9,'1'),('981651685308891186','ROLE_ADMINISTRATORS','981337817252626432','1','2024-03-10 01:14:57',9,'1'),('981651685308891187','ROLE_ADMINISTRATORS','981337817252626432','1','2024-03-10 01:14:57',9,'1'),('981651685308891188','ROLE_ADMINISTRATORS','981337752387715072','1','2024-03-10 01:14:57',9,'1'),('981651685308891189','ROLE_ADMINISTRATORS','981337752387715072','1','2024-03-10 01:14:57',9,'1'),('981651685308891190','ROLE_ADMINISTRATORS','981337704388100096','1','2024-03-10 01:14:57',9,'1'),('981651685308891191','ROLE_ADMINISTRATORS','981337704388100096','1','2024-03-10 01:14:57',9,'1'),('981651685308891192','ROLE_ADMINISTRATORS','981337555771326464','1','2024-03-10 01:14:57',1,'1'),('981651685308891193','ROLE_ADMINISTRATORS','981337555771326464','1','2024-03-10 01:14:57',1,'1'),('981651685308891194','ROLE_ADMINISTRATORS','981623658751459328','1','2024-03-10 01:14:57',1,'1'),('981651685308891195','ROLE_ADMINISTRATORS','981623658751459328','1','2024-03-10 01:14:57',1,'1'),('981651685308891196','ROLE_ADMINISTRATORS','981569201816731648','1','2024-03-10 01:14:57',1,'1'),('981651685308891197','ROLE_ADMINISTRATORS','981569201816731648','1','2024-03-10 01:14:57',1,'1'),('981651685308891198','ROLE_ADMINISTRATORS','981569048993071104','1','2024-03-10 01:14:57',1,'1'),('981651685308891199','ROLE_ADMINISTRATORS','981569048993071104','1','2024-03-10 01:14:57',1,'1'),('981651685308891200','ROLE_ADMINISTRATORS','981570045970743296','1','2024-03-10 01:14:57',1,'1'),('981651685308891201','ROLE_ADMINISTRATORS','981570045970743296','1','2024-03-10 01:14:57',1,'1'),('981651685308891202','ROLE_ADMINISTRATORS','981568925764419584','1','2024-03-10 01:14:57',1,'1'),('981651685308891203','ROLE_ADMINISTRATORS','981568925764419584','1','2024-03-10 01:14:57',1,'1'),('981651685308891204','ROLE_ADMINISTRATORS','981337246718230528','1','2024-03-10 01:14:57',1,'1'),('981651685308891205','ROLE_ADMINISTRATORS','981337246718230528','1','2024-03-10 01:14:57',1,'1'),('981651685308891208','ROLE_ADMINISTRATORS','981336115820625920','1','2024-03-10 01:14:57',9,'1'),('981651685308891209','ROLE_ADMINISTRATORS','981336115820625920','1','2024-03-10 01:14:57',9,'1'),('981651685308891210','ROLE_ADMINISTRATORS','981336054843834368','1','2024-03-10 01:14:57',1,'1'),('981651685308891211','ROLE_ADMINISTRATORS','981336054843834368','1','2024-03-10 01:14:57',1,'1'),('981651685308891218','ROLE_ADMINISTRATORS','981335989207171072','1','2024-03-10 01:14:57',9,'1'),('981651685308891219','ROLE_ADMINISTRATORS','981335989207171072','1','2024-03-10 01:14:57',9,'1'),('981651685308891220','ROLE_ADMINISTRATORS','981335810039087104','1','2024-03-10 01:14:57',1,'1'),('981651685308891221','ROLE_ADMINISTRATORS','981335810039087104','1','2024-03-10 01:14:57',1,'1'),('981651685308891222','ROLE_ADMINISTRATORS','981335758977630208','1','2024-03-10 01:14:57',1,'1'),('981651685308891223','ROLE_ADMINISTRATORS','981335758977630208','1','2024-03-10 01:14:57',1,'1'),('981651685308891224','ROLE_ADMINISTRATORS','981335709019275264','1','2024-03-10 01:14:57',1,'1'),('981651685308891225','ROLE_ADMINISTRATORS','981335709019275264','1','2024-03-10 01:14:57',1,'1'),('981651685308891226','ROLE_ADMINISTRATORS','981623658751459329','1','2025-02-18 21:51:31',1,'1');
/*!40000 ALTER TABLE `permission` ENABLE KEYS */;
UNLOCK TABLES;

-- config
LOCK TABLES `config` WRITE;
/*!40000 ALTER TABLE `config` DISABLE KEYS */;
INSERT INTO `config` VALUES ('1923586193565650946','template','应收账款科目编号','sys.default.accountsReceivable','1122','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 09:15:30','1','2025-05-06 09:15:34'),('1923586193754394626','template','现金项科目编号','sys.default.cashSubjectCodes','1001,1002','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:41'),('1923586193943138305','template','利润表-净利润编号','sys.default.incomeNetProfit','4','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:41'),('1923586194123493378','template','利润表-营业收入编号','sys.default.incomeOperatingRevenue','1','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:41'),('1923586194303848449','template','利润表-营业成本编号','sys.default.incomeOperatingCosts','101','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:41'),('1923586194484203522','template','利润表-营业利润编号','sys.default.incomeOperatingProfit','2','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:41'),('1923586194668752898','template','利润表-管理费用','sys.default.administrativeExpenses','105','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:41'),('1923586194849107969','template','利润表-销售费用','sys.default.sellingExpenses','104','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42'),('1923586195033657345','template','利润表-财务费用','sys.default.financialExpenses','106','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42'),('1923586195214012418','template','当前账期','sys.payment.term.current','2025-05','y','系统内置，不可删除','1','2025-03-12 19:39:01','1','2025-05-29 10:57:29'),('1923586195398561794','template','初始账期','sys.payment.term.start','2025-03','y','系统内置，不可删除','1','2025-03-12 19:46:09','1','2025-03-12 19:47:06'),('1923586195587305473','template','是否完成初始化任务','sys.initialize.task','false','y','系统内置，不可删除','1','2025-03-12 19:48:36','1','2025-03-21 16:35:39'),('1923586195767660545','template','科目级数','sys.subject.level','4','y','科目级次和长度调大后，不能再调小（即：不能再恢复到调整前的级次和长度），请谨慎操作！','1','2025-03-18 09:59:06','1','2025-04-23 22:20:14'),('1923586195952209921','template','科目编码长度','sys.subject.codes.length','4,2,2,2','y','科目级次和长度调大后，不能再调小（即：不能再恢复到调整前的级次和长度），请谨慎操作！','1','2025-03-18 10:00:10','1','2025-04-23 22:20:14'),('1923586196128370689','template','应付账款科目编号','sys.default.accountsPayable','2202','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 09:20:50','1','2025-05-06 09:20:53'),('1923586196304531457','template','短期应收款科目编号','sys.default.shortTermAccountsReceivable','1121,1122,1131,1132,1221','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 09:39:37','1','2025-05-06 09:39:40'),('1923586196489080834','template','短期应付款科目编号','sys.default.shortTermAccountsPayable','2201,2202,2211,2231,2232,2241','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 09:40:13','1','2025-05-17 11:47:42'),('1923586196665241602','template','交易性金融资产科目编号','sys.default.tradingFinancialAssets','1101','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42'),('1923586196853985282','template','应收票据科目编号','sys.default.billReceivable','1121','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42'),('1923586197034340354','template','资产负债-流动负债项编号','sys.default.currentLiabilities','2100','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42'),('1923586197214695426','template','资产负债-流动资产项编号','sys.default.currentAssets','1100','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42'),('1923586197390856193','template','资产负债-流动资产-存货项编号','sys.default.currentAssetsInventory','1110','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42'),('1923586197390856194','template','利润表-税金及附加','sys.default.addedTax','102','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42'),('1923586197390856195','template','利润表-所得税费用','sys.default.incomeTaxExpenses','301','y','系统默认，随变动自动更新，不可调整','1','2025-05-06 10:09:15','1','2025-05-17 11:47:42');
/*!40000 ALTER TABLE `config` ENABLE KEYS */;
UNLOCK TABLES;

-- config_login_policy
LOCK TABLES `config_login_policy` WRITE;
/*!40000 ALTER TABLE `config_login_policy` DISABLE KEYS */;
INSERT INTO `config_login_policy` VALUES ('1',24,8,'Y','TEXT','ARITHMETIC',0,'Y',10,10,6,'Y',10,'NONE','N','N','','1');
/*!40000 ALTER TABLE `config_login_policy` ENABLE KEYS */;
UNLOCK TABLES;

-- config_password_policy
LOCK TABLES `config_password_policy` WRITE;
/*!40000 ALTER TABLE `config_password_policy` DISABLE KEYS */;
INSERT INTO `config_password_policy` VALUES ('1',6,20,1,0,0,0,6,30,90,0,3,1,1,1,1,3);
/*!40000 ALTER TABLE `config_password_policy` ENABLE KEYS */;
UNLOCK TABLES;

-- config_personal_tax
LOCK TABLES `config_personal_tax` WRITE;
/*!40000 ALTER TABLE `config_personal_tax` DISABLE KEYS */;
INSERT INTO `config_personal_tax` VALUES ('1887760257594204161',1,0,5000,0,0.00,NULL,0,'1','2025-02-07 15:08:12','1','2025-03-06 17:34:22','n'),('1887761326340612097',2,5000,8000,3,150.00,NULL,0,'1','2025-02-07 15:12:27','1','2025-03-06 17:34:22','n'),('1887762268276432897',3,8000,17000,10,710.00,NULL,0,'1','2025-02-07 15:16:12','1','2025-03-06 17:34:22','n'),('1887795119336226817',4,17000,30000,20,2410.00,NULL,0,'1','2025-02-07 17:26:44','1','2025-03-06 17:34:22','n'),('1888054950085365761',5,30000,40000,25,3910.00,NULL,0,'1','2025-02-08 10:39:13','1','2025-03-06 17:34:22','n'),('1888055058986274817',6,40000,60000,30,5910.00,NULL,0,'1','2025-02-08 10:39:39','1','2025-03-06 17:34:22','n'),('1888076044448079873',7,60000,85000,35,8910.00,NULL,0,'1','2025-02-08 12:03:02','1','2025-03-06 17:34:22','n'),('1897581567807631362',8,85000,NULL,45,17410.00,NULL,0,'1','2025-03-06 17:34:35','1','2025-03-06 17:34:35','n'),('1912430314379091970',1,0,20000,20,0.00,NULL,1,'1','2025-04-16 16:58:12','1','2025-04-16 16:58:12','n'),('1912430400794337282',2,20000,50000,30,2000.00,NULL,1,'1','2025-04-16 16:58:33','1','2025-04-16 16:58:33','n'),('1912430467911589889',3,50000,NULL,40,7000.00,NULL,1,'1','2025-04-16 16:58:49','1','2025-04-16 16:58:49','n');
/*!40000 ALTER TABLE `config_personal_tax` ENABLE KEYS */;
UNLOCK TABLES;

-- standard_statement_balance_sheet
LOCK TABLES `standard_statement_balance_sheet` WRITE;
/*!40000 ALTER TABLE `standard_statement_balance_sheet` DISABLE KEYS */;
INSERT INTO `standard_statement_balance_sheet` VALUES ('1','1','asset','1100','流动资产：',NULL,1,NULL,'+',1,NULL,NULL,'1','2025-05-29 14:47:26','n'),('10','1','asset','1111','持有待售资产',11,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:42:40','n'),('100','1','liability','2299_2199','负债合计',56,1,NULL,'+',1,'1','2025-05-22 10:44:47','1','2025-05-29 13:56:22','n'),('11','1','asset','1112','一年内到期的非流动资产',12,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('12','1','asset','1113','其他流动资产',13,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('13','1','asset','1200','非流动资产：',NULL,1,NULL,'+',1,NULL,NULL,'1','2025-05-22 10:47:39','n'),('14','1','asset','1201','债权投资',15,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:44:10','n'),('15','1','asset','1202','其他债权投资',16,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:44:29','n'),('16','1','asset','1203','长期应收款',17,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('17','1','asset','1204','长期股权投资',18,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('18','1','asset','1205','投资性房地产',19,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('19','1','asset','1206','固定资产',20,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:48:17','n'),('1925379541074939905','1','asset','1199','合计：流动资产',14,1,NULL,'+',1,'1','2025-05-22 10:33:49','1','2025-05-29 13:41:25','n'),('1925380944069627906','1','asset','1299','合计：非流动资产',30,1,NULL,'+',1,'1','2025-05-22 10:39:23','1','2025-05-29 13:41:25','n'),('1925381919354359810','1','asset','1399','总计：资产',31,1,NULL,'+',1,'1','2025-05-22 10:43:16','1','2025-05-29 13:41:25','n'),('1925382166239481858','1','liability','2199','合计：流动负债',45,1,NULL,'+',1,'1','2025-05-22 10:44:14','1','2025-05-29 13:56:22','n'),('1925382301115715585','1','liability','2299','合计：非流动负债',55,1,NULL,'+',1,'1','2025-05-22 10:44:47','1','2025-05-29 20:08:22','n'),('1925382370179125250','1','liability','2399','合计：所有者权益（或股东权益）',67,1,NULL,'+',1,'1','2025-05-22 10:45:03','1','2025-05-29 13:56:22','n'),('1925382483198840834','1','liability','2499','总计：负债和所有者权益（或股东权益）',68,1,NULL,'+',1,'1','2025-05-22 10:45:30','1','2025-05-29 13:56:22','n'),('1927961250546421762','1','asset','1102','交易性金融资产',2,2,'1100','+',1,NULL,'2025-05-29 13:32:36',NULL,'2025-05-29 13:32:36','n'),('1927962019169406978','1','asset','1106','应收款项融资',6,2,'1100','+',1,NULL,'2025-05-29 13:35:39',NULL,'2025-05-29 13:35:39','n'),('1927963475171708929','1','asset','1109','合同资产',9,2,'1100','+',1,'1124407184617111552','2025-05-29 13:41:26','1124407184617111552','2025-05-29 13:41:26','n'),('1927966840819650561','1','liability','2102','交易性金融负债',33,2,'2100','+',1,'1124407184617111552','2025-05-29 13:54:49','1124407184617111552','2025-05-29 13:54:49','n'),('1927967235746926594','1','liability','2106','合同负债',37,2,'2100','+',1,'1124407184617111552','2025-05-29 13:56:23','1124407184617111552','2025-05-29 13:56:23','n'),('1928058364450435074','1','liability','2210','测试',58,2,'2209','+',1,'1124407184617111552','2025-05-29 19:58:30','1124407184617111552','2025-05-29 19:58:54','y'),('2','1','asset','1101','货币资金',1,2,'1100','+',1,NULL,NULL,'1','2025-05-29 09:48:33','n'),('20','1','asset','1207','在建工程',21,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('21','1','asset','1208','生产性生物资产',22,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('22','1','asset','1209','油气资产',23,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('23','1','asset','1210','无形资产',24,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:49:35','n'),('24','1','asset','1211','开发支出',25,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:49:56','n'),('25','1','asset','1212','商誉',26,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('26','1','asset','1213','长期待摊费用',27,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:50:15','n'),('27','1','asset','1214','递延所得税资产',28,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('28','1','asset','1215','其他非流动资产',29,2,'1200','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n'),('29','1','liability','2100','流动负债：',NULL,1,NULL,'+',1,NULL,NULL,'1','2025-05-22 10:47:48','n'),('30','1','liability','2101','短期借款',32,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:50:45','n'),('32','1','liability','2103','衍生金融负债',34,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:54:48','n'),('33','1','liability','2104','应付票据',35,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:55:07','n'),('34','1','liability','2105','应付账款',36,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:55:19','n'),('35','1','liability','2107','预收款项',38,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('36','1','liability','2108','应付职工薪酬',39,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:56:49','n'),('37','1','liability','2109','应交税费',40,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:57:01','n'),('38','1','liability','2110','其他应付款',41,2,'2100','+',1,NULL,NULL,'1','2025-05-29 14:07:47','n'),('39','1','liability','2111','持有待售负债',42,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('4','1','asset','1103','衍生金融资产',3,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:34:45','n'),('40','1','liability','2112','一年内到期的非流动负债',43,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('41','1','liability','2113','其他流动负债',44,2,'2100','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('42','1','liability','2200','非流动负债：',NULL,1,NULL,'+',1,NULL,NULL,'1','2025-05-22 10:47:54','n'),('43','1','liability','2201','长期借款',46,2,'2200','+',1,NULL,NULL,'1','2025-05-29 14:09:05','n'),('44','1','liability','2202','应付债券',47,2,'2200','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('45','1','liability','2203','其中：优先股',48,3,'2202','+',1,NULL,NULL,'1','2025-05-29 14:10:06','n'),('46','1','liability','2204','其中：永续债',49,3,'2202','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('47','1','liability','2205','长期应付款',50,2,'2200','+',1,NULL,NULL,'1','2025-05-29 14:10:31','n'),('48','1','liability','2206','预计负债',51,2,'2200','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('49','1','liability','2207','递延收益',52,2,'2200','+',1,NULL,NULL,'1','2025-05-29 14:10:51','n'),('5','1','asset','1104','应收票据',4,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:34:43','n'),('50','1','liability','2208','递延所得税负债',53,2,'2200','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('51','1','liability','2209','其他非流动负债',54,2,'2200','+',1,NULL,NULL,'1','2025-05-29 20:01:43','n'),('52','1','liability','2300','所有者权益（或股东权益）：',NULL,1,NULL,'+',1,NULL,NULL,'1','2025-05-22 10:47:59','n'),('53','1','liability','2301','实收资本（或股本）',57,2,'2300','+',1,NULL,NULL,'1','2025-05-29 19:59:02','n'),('54','1','liability','2302','其他权益工具',58,2,'2300','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('55','1','liability','2303','其中：优先股',59,3,'2302','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('56','1','liability','2304','其中：永续债',60,3,'2302','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('57','1','liability','2305','资本公积',61,2,'2300','+',1,NULL,NULL,'1','2025-05-29 14:12:40','n'),('58','1','liability','2306','库存股',62,2,'2300','-',1,NULL,NULL,'1','2025-05-29 16:10:33','n'),('59','1','liability','2307','其他综合收益',63,2,'2300','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('6','1','asset','1105','应收账款',5,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:35:05','n'),('60','1','liability','2308','专项储备',64,2,'2300','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('61','1','liability','2309','盈余公积',65,2,'2300','+',1,NULL,NULL,'1','2025-05-29 13:56:22','n'),('62','1','liability','2310','未分配利润',66,2,'2300','+',1,NULL,NULL,'1','2025-05-29 16:56:48','n'),('7','1','asset','1107','预付款项',7,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:35:38','n'),('8','1','asset','1108','其他应收款',8,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:38:11','n'),('9','1','asset','1110','存货',10,2,'1100','+',1,NULL,NULL,'1','2025-05-29 13:41:25','n');
/*!40000 ALTER TABLE `standard_statement_balance_sheet` ENABLE KEYS */;
UNLOCK TABLES;

-- standard_statement_income
LOCK TABLES `standard_statement_income` WRITE;
/*!40000 ALTER TABLE `standard_statement_income` DISABLE KEYS */;
INSERT INTO `standard_statement_income` VALUES ('1911041986216067073','2',1,'1','1',1,'+','y',NULL,'1','2025-04-12 21:01:29','1','2025-04-12 21:39:34','n'),('1911048181198880770','2',2,'2','2',1,'+','y',NULL,'1','2025-04-12 21:26:06','1','2025-04-12 21:39:46','n'),('1925751131481284610','1',1,'1','一、营业收入',1,'f','y',NULL,'admin','2025-04-07 10:41:37','1','2025-04-12 21:46:04','n'),('1925751131481284611','1',2,'101','减：营业成本',1,'+','y',NULL,'admin','2025-04-07 10:41:37','1','2025-05-15 11:28:51','n'),('1925751131481284612','1',3,'102','税金及附加',2,'+','y','101','admin','2025-04-07 10:41:37','1','2025-04-12 20:45:30','n'),('1925751131481284613','1',4,'10301','其中：消费税',3,'+','y','102','admin','2025-04-07 10:41:37','1','2025-04-12 20:44:23','n'),('1925751131481284614','1',5,'10302','营业税',3,'+','y','102','admin','2025-04-07 10:41:37','1','2025-04-10 16:33:49','n'),('1925751131481284615','1',6,'10303','城市维护建设税',3,'+','y','102','admin','2025-04-07 10:41:37','1','2025-04-10 16:34:06','n'),('1925751131481284616','1',7,'10304','资源税',3,'+','y','102','admin','2025-04-07 10:41:37','1','2025-04-10 16:34:23','n'),('1925751131481284617','1',8,'10305','土地增值税',3,'+','y','102','admin','2025-04-07 10:41:37','1','2025-04-10 16:34:51','n'),('1925751131481284618','1',9,'10306','城镇土地使用税、房产税、车船税、印花税',3,'+','y','102','admin','2025-04-07 10:41:37','1','2025-04-12 21:51:47','n'),('1925751131481284619','1',10,'10307','教育费附加、矿产资源补偿税、排污费',3,'+','y','102','admin','2025-04-07 10:41:37','1','2025-04-12 21:51:51','n'),('1925751131481284620','1',11,'104','销售费用',2,'+','y','101','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284621','1',12,'10401','其中：商品维修费',3,'+','y','104','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284622','1',13,'10402','广告费和业务宣传费',3,'+','y','104','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284623','1',14,'105','管理费用',2,'+','y','101','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284624','1',15,'10501','其中：开办费',3,'+','y','105','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284625','1',16,'10502','业务招待费',3,'+','y','105','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284626','1',17,'10503','研究费用',3,'+','y','105','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284627','1',18,'106','财务费用',2,'+','y','101','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284628','1',19,'10601','其中：利息费用（收入以“-”号填列）',3,'+','y','106','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284629','1',20,'107','加：投资收益（损失以“-”号填列）',1,'+','y',NULL,'admin','2025-04-07 10:41:37','1','2025-04-12 20:45:55','n'),('1925751131481284630','1',21,'2','二、营业利润（亏损以“-”号填列）',1,'f','n',NULL,'admin','2025-04-07 10:41:37','1','2025-04-12 22:12:12','n'),('1925751131481284631','1',22,'201','加：营业外收入',1,'+','y',NULL,'admin','2025-04-07 10:41:37','1','2025-04-10 17:10:32','n'),('1925751131481284632','1',23,'20101','其中：政府补助',2,'+','y','201','admin','2025-04-07 10:41:37','1','2025-04-10 17:08:24','n'),('1925751131481284633','1',24,'202','减：营业外支出',1,'-','y',NULL,'admin','2025-04-07 10:41:37','1','2025-04-12 20:46:18','n'),('1925751131481284634','1',25,'20201','其中：坏账损失',2,'-','y','202','admin','2025-04-07 10:41:37','1','2025-04-10 17:14:36','n'),('1925751131481284635','1',26,'20202','无法收回的长期债券投资损失',2,'-','y','202','admin','2025-04-07 10:41:37','1','2025-04-10 17:29:05','n'),('1925751131481284636','1',27,'20203','无法收回的长期股权投资损失',2,'-','y','202','admin','2025-04-07 10:41:37','1','2025-04-10 17:29:15','n'),('1925751131481284637','1',28,'20204','自然灾害等不可抗力因素造成的损失',2,'-','y','202','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284638','1',29,'20205','税收滞纳金',2,'-','y','202','admin','2025-04-07 10:41:37',NULL,NULL,'n'),('1925751131481284639','1',30,'3','三、利润总额（亏损总额以“-”号填列）',1,'f','n',NULL,'admin','2025-04-07 10:41:37','1','2025-04-12 22:12:21','n'),('1925751131481284640','1',31,'301','减：所得税费用',1,'-','y',NULL,'admin','2025-04-07 10:41:37','1','2025-04-12 20:44:49','n'),('1925751131481284641','1',32,'4','四：净利润（净亏损以“-”号填列）',1,'f','n',NULL,'admin','2025-04-07 10:41:37','1','2025-04-12 22:12:32','n');
/*!40000 ALTER TABLE `standard_statement_income` ENABLE KEYS */;
UNLOCK TABLES;

-- standard_statement_rules
LOCK TABLES `standard_statement_rules` WRITE;
/*!40000 ALTER TABLE `standard_statement_rules` DISABLE KEYS */;
INSERT INTO `standard_statement_rules` VALUES ('1920655798443208706','1','balance_sheet','1101','1002','BALANCE','+','1','2025-05-09 09:43:21','1','2025-05-09 09:43:21'),('1920655798443208707','1','balance_sheet','1101','1001','BALANCE','+','1','2025-05-09 09:43:21','1','2025-05-09 09:43:21'),('1925741035011416073','1','income','10303','222105','CREDIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416074','1','income','10304','222106','CREDIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416075','1','income','10305','222107','CREDIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416076','1','income','10306','222108','CREDIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416077','1','income','10306','222109','CREDIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416078','1','income','10306','222111','CREDIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416079','1','income','10306','660213','CREDIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416080','1','income','10307','222110','PROFIT_AND_LOSS_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416081','1','income','10307','222112','PROFIT_AND_LOSS_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416082','1','income','10307','222113','PROFIT_AND_LOSS_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416085','1','income','107','6111','DEBIT_AMOUNT','-','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035011416086','1','income','20101','630105','PROFIT_AND_LOSS_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035078524411','1','income','105','660201','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524412','1','income','105','660202','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524413','1','income','105','660203','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524414','1','income','105','660204','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524415','1','income','105','660205','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524417','1','income','105','660207','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524419','1','income','105','660209','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524420','1','income','105','660210','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524421','1','income','105','660211','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524422','1','income','105','660212','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524423','1','income','105','660213','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524424','1','income','105','660214','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524425','1','income','105','660215','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524426','1','income','105','660216','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524427','1','income','105','660217','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524428','1','income','105','660218','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524429','1','income','105','660219','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524430','1','income','105','660220','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524431','1','income','105','660221','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524432','1','income','105','660222','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524433','1','income','105','660223','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524434','1','income','105','660224','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524435','1','income','105','660225','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524436','1','income','105','660226','DEBIT_AMOUNT','+','1','2025-05-23 16:07:15','1','2025-05-23 16:07:15'),('1925741035078524929','1','income','201','6301','DEBIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035078524933','1','income','20101','605104','PROFIT_AND_LOSS_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035078524941','1','income','202','6711','DEBIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035078524942','1','income','20201','671109','PROFIT_AND_LOSS_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035078524943','1','income','20202','671110','PROFIT_AND_LOSS_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035078524944','1','income','301','680101','DEBIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035078524945','1','income','301','680102','DEBIT_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925741035078524946','1','income','20203','1525','PROFIT_AND_LOSS_AMOUNT','+','1','2025-05-23 10:30:15','1','2025-05-23 10:30:15'),('1925815214465327105','1','income','1','6001','DEBIT_AMOUNT','+','1','2025-05-23 15:25:01','1','2025-05-23 15:25:01'),('1925815214465327106','1','income','1','6051','DEBIT_AMOUNT','+','1','2025-05-23 15:25:01','1','2025-05-23 15:25:01'),('1925816591564382209','1','income','106','660304','DEBIT_AMOUNT','+','1','2025-05-23 15:30:30','1','2025-05-23 15:30:30'),('1925825006848155649','1','income','10502','660207','DEBIT_AMOUNT','+','1','2025-05-23 16:03:56','1','2025-05-23 16:03:56'),('1926104297762631682','1','income','105','660206','DEBIT_AMOUNT','+','1','2025-05-24 10:33:44','1','2025-05-24 10:33:44'),('1926104297762631683','1','income','105','660208','DEBIT_AMOUNT','+','1','2025-05-24 10:33:44','1','2025-05-24 10:33:44'),('1926106878928924673','1','income','104','660101','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924674','1','income','104','660102','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924675','1','income','104','660103','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924676','1','income','104','660104','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924677','1','income','104','660105','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924678','1','income','104','660106','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924679','1','income','104','660107','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924680','1','income','104','660108','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924681','1','income','104','660109','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924682','1','income','104','660110','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926106878928924683','1','income','104','660111','DEBIT_AMOUNT','+','1','2025-05-24 10:43:59','1','2025-05-24 10:43:59'),('1926108205008465921','1','income','106','660301','DEBIT_AMOUNT','+','1','2025-05-24 10:49:16','1','2025-05-24 10:49:16'),('1926108205008465922','1','income','106','660302','DEBIT_AMOUNT','+','1','2025-05-24 10:49:16','1','2025-05-24 10:49:16'),('1926108205008465923','1','income','106','660305','DEBIT_AMOUNT','+','1','2025-05-24 10:49:16','1','2025-05-24 10:49:16'),('1926109965529821185','1','income','10301','222103','CREDIT_AMOUNT','+','1','2025-05-24 10:56:15','1','2025-05-24 10:56:15'),('1926111117881290754','1','income','10601','660302','DEBIT_AMOUNT','+','1','2025-05-24 11:00:50','1','2025-05-24 11:00:50'),('1926113153930670081','1','income','101','6401','DEBIT_AMOUNT','+','1','2025-05-24 11:08:56','1','2025-05-24 11:08:56'),('1926113153930670082','1','income','101','6402','DEBIT_AMOUNT','+','1','2025-05-24 11:08:56','1','2025-05-24 11:08:56'),('1926833885610512386','1','income','102','6405','DEBIT_AMOUNT','+','1','2025-05-26 10:52:51','1','2025-05-26 10:52:51'),('1927961250986315778','1','balance_sheet','1102','1101','BALANCE','+','1124407184617111552','2025-05-29 13:32:36',NULL,'2025-05-29 13:32:36'),('1927961762058063874','1','balance_sheet','1104','1121','BALANCE','+','1124407184617111552','2025-05-29 13:34:38',NULL,'2025-05-29 13:34:38'),('1927961876688392193','1','balance_sheet','1105','1122','BALANCE','+','1124407184617111552','2025-05-29 13:35:05',NULL,'2025-05-29 13:35:05'),('1927962019525414914','1','balance_sheet','1106','1101','BALANCE','+','1124407184617111552','2025-05-29 13:35:39',NULL,'2025-05-29 13:35:39'),('1927962656770215937','1','balance_sheet','1107','1131','BALANCE','+','1124407184617111552','2025-05-29 13:38:11','1124407184617111552','2025-05-29 13:38:11'),('1927962656778604546','1','balance_sheet','1107','1132','BALANCE','+','1124407184617111552','2025-05-29 13:38:11','1124407184617111552','2025-05-29 13:38:11'),('1927962656778604547','1','balance_sheet','1107','1231','BALANCE','+','1124407184617111552','2025-05-29 13:38:11','1124407184617111552','2025-05-29 13:38:11'),('1927962656782798849','1','balance_sheet','1107','1221','BALANCE','+','1124407184617111552','2025-05-29 13:38:11','1124407184617111552','2025-05-29 13:38:11'),('1927965196878467073','1','balance_sheet','1206','1601','BALANCE','+','1124407184617111552','2025-05-29 13:48:17','1124407184617111552','2025-05-29 13:48:17'),('1927965196878467074','1','balance_sheet','1206','1602','BALANCE','+','1124407184617111552','2025-05-29 13:48:17','1124407184617111552','2025-05-29 13:48:17'),('1927965525804175361','1','balance_sheet','1210','1701','BALANCE','+','1124407184617111552','2025-05-29 13:49:35','1124407184617111552','2025-05-29 13:49:35'),('1927965525804175362','1','balance_sheet','1210','1702','BALANCE','+','1124407184617111552','2025-05-29 13:49:35','1124407184617111552','2025-05-29 13:49:35'),('1927965612601102337','1','balance_sheet','1211','5301','BALANCE','+','1124407184617111552','2025-05-29 13:49:56','1124407184617111552','2025-05-29 13:49:56'),('1927965691110084609','1','balance_sheet','1213','1801','BALANCE','+','1124407184617111552','2025-05-29 13:50:15','1124407184617111552','2025-05-29 13:50:15'),('1927965818222661633','1','balance_sheet','2101','2001','BALANCE','+','1124407184617111552','2025-05-29 13:50:45','1124407184617111552','2025-05-29 13:50:45'),('1927966919126798337','1','balance_sheet','2104','2201','BALANCE','+','1124407184617111552','2025-05-29 13:55:08','1124407184617111552','2025-05-29 13:55:08'),('1927966969059987457','1','balance_sheet','2105','2202','BALANCE','+','1124407184617111552','2025-05-29 13:55:19','1124407184617111552','2025-05-29 13:55:19'),('1927967346446684162','1','balance_sheet','2107','2211','BALANCE','+','1124407184617111552','2025-05-29 13:56:49','1124407184617111552','2025-05-29 13:56:49'),('1927967395356463106','1','balance_sheet','2108','2221','BALANCE','+','1124407184617111552','2025-05-29 13:57:01','1124407184617111552','2025-05-29 13:57:01'),('1927970104264478721','1','balance_sheet','2109','2232','BALANCE','+','1124407184617111552','2025-05-29 14:07:47','1124407184617111552','2025-05-29 14:07:47'),('1927970104264478722','1','balance_sheet','2109','1221','BALANCE','+','1124407184617111552','2025-05-29 14:07:47','1124407184617111552','2025-05-29 14:07:47'),('1927970433940967426','1','balance_sheet','2201','2601','BALANCE','+','1124407184617111552','2025-05-29 14:09:06','1124407184617111552','2025-05-29 14:09:06'),('1927970791417303042','1','balance_sheet','2205','2801','BALANCE','+','1124407184617111552','2025-05-29 14:10:31','1124407184617111552','2025-05-29 14:10:31'),('1927970874762317826','1','balance_sheet','2207','2501','BALANCE','+','1124407184617111552','2025-05-29 14:10:51','1124407184617111552','2025-05-29 14:10:51'),('1927971217504063490','1','balance_sheet','2301','4001','BALANCE','+','1124407184617111552','2025-05-29 14:12:12','1124407184617111552','2025-05-29 14:12:12'),('1927971335712133121','1','balance_sheet','2305','4002','BALANCE','+','1124407184617111552','2025-05-29 14:12:41','1124407184617111552','2025-05-29 14:12:41'),('1927976286253338625','1','balance_sheet','2310','4103','BALANCE','+','1124407184617111552','2025-05-29 14:32:21','1124407184617111552','2025-05-29 14:32:21'),('1927976286261727234','1','balance_sheet','2310','4104','BALANCE','+','1124407184617111552','2025-05-29 14:32:21','1124407184617111552','2025-05-29 14:32:21');
/*!40000 ALTER TABLE `standard_statement_rules` ENABLE KEYS */;
UNLOCK TABLES;

-- voucher_template
LOCK TABLES `voucher_template` WRITE;
/*!40000 ALTER TABLE `voucher_template` DISABLE KEYS */;
INSERT INTO `voucher_template` VALUES ('1920056009217282050','1','qm_jz_sr','2-结转收入',1,'结转{yyyy}年{mm}月收入',0,0,'记',201,1,'1','2025-05-07 18:00:00','1','2025-05-28 10:10:42','n'),('1920056147159552001','1','qm_jz_xscb','1-结转销售成本-主营业务成本',1,'结转{yyyy}年{mm}月销售成本',1,0,'记',200,1,'1','2025-05-07 18:00:33','1','2025-05-28 10:10:36','n'),('1920686232061288450','1','jt_sds','计提所得税',1,'计提{yyyy}年{mm}月所得税',1,0,'记',100,1,'1','2025-05-09 11:44:17','1','2025-05-28 10:10:26','n'),('1920687071316353025','1','jt_fjs','计提附加税',1,'计提附加税',1,0,'记',1,1,'1','2025-05-09 11:47:37','1','2025-05-09 15:58:14','y'),('1920687185409810434','1','jt_zj','计提折旧',1,'计提{yyyy}年{mm}月折旧',0,0,'记',100,1,'1','2025-05-09 11:48:04','1','2025-05-28 10:10:31','n'),('1920749230687760386','1','qm_jz_cbfy','3-结转成本费用',1,'结转{yyyy}年{mm}月成本费用',0,0,'记',203,1,'1','2025-05-09 15:54:37','1','2025-05-28 10:10:56','n'),('1920751322139668482','1','jt_gz','计提工资',1,'计提{yyyy}年{mm}月工资',0,0,'记',100,1,'1','2025-05-09 16:02:55','1','2025-05-28 10:10:21','n'),('1920763719705374722','1','22','22',1,'222',0,0,'记',1,1,'1','2025-05-09 16:52:11','1','2025-05-09 16:52:11','y'),('1921196058970251266','2','qm_jz_sr','结转收入',1,'结转收入',0,0,'记',1,1,'1','2025-05-07 18:00:00','1','2025-05-09 15:57:30','n'),('1921196059167383554','2','qm_jz_xscb','结转销售成本',1,'结转销售成本',1,0,'记',1,1,'1','2025-05-07 18:00:33','1','2025-05-09 15:57:17','n'),('1921196059297406977','2','jt_sds','计提所得税',1,'计提所得税',1,0,'记',1,1,'1','2025-05-09 11:44:17','1','2025-05-09 15:58:26','n'),('1921196059490344962','2','jt_fjs','计提附加税',1,'计提附加税',1,0,'记',1,1,'1','2025-05-09 11:47:37','1','2025-05-09 15:58:14','n'),('1921196059691671553','2','jt_zj','计提折旧',1,'计提折旧1',0,0,'记',1,1,'1','2025-05-09 11:48:04','1','2025-05-10 21:40:29','n'),('1921196059825889281','2','qm_jz_cbfy','结转成本费用',1,'结转成本费用',0,0,'记',1,1,'1','2025-05-09 15:54:37','1','2025-05-09 15:57:42','n'),('1921196060023021569','2','jt_gz','计提工资',1,'计提工资',0,0,'记',1,1,'1','2025-05-09 16:02:55','1','2025-05-09 16:02:55','n'),('1921377761617559553','1','qm_jz_sr','结转收入',1,'结转收入',0,0,'记',1,1,'1','2025-05-07 18:00:00','1','2025-05-09 15:57:30','y'),('1921377761810497537','1','qm_jz_xscb','结转销售成本',1,'结转销售成本',1,0,'记',1,1,'1','2025-05-07 18:00:33','1','2025-05-09 15:57:17','y'),('1921377762007629826','1','jt_sds','计提所得税',1,'计提所得税',1,0,'记',1,1,'1','2025-05-09 11:44:17','1','2025-05-09 15:58:26','y'),('1921377762137653250','1','jt_fjs','计提附加税',1,'计提{yyyy}年{mm}月附加税',1,0,'记',100,1,'1','2025-05-09 11:47:37','1','2025-05-28 10:10:16','n'),('1921377762469003266','1','jt_zj','计提折旧',1,'计提折旧',0,0,'记',1,1,'1','2025-05-09 11:48:04','1','2025-05-09 15:58:04','y'),('1921377762666135554','1','qm_jz_cbfy','结转成本费用',1,'结转成本费用',0,0,'记',1,1,'1','2025-05-09 15:54:37','1','2025-05-09 15:57:42','y'),('1921377762859073537','1','jt_gz','计提工资',1,'计提工资',0,0,'记',1,1,'1','2025-05-09 16:02:55','1','2025-05-09 16:02:55','y'),('1921484358457057282','1','qm_jz_sds','4-结转所得税',1,'结转{yyyy}年{mm}月所得税',0,0,'记',202,1,'1','2025-05-11 16:35:45','1','2025-05-28 10:10:48','n'),('1921484857377906689','1','qm_jz_bnlr','4-结转本年利润-年终',1,'结转{yyyy}年本年利润',0,0,'记',204,1,'1','2025-05-11 16:37:44','1','2025-05-28 10:11:05','n');
/*!40000 ALTER TABLE `voucher_template` ENABLE KEYS */;
UNLOCK TABLES;

-- voucher_template_item
LOCK TABLES `voucher_template_item` WRITE;
/*!40000 ALTER TABLE `voucher_template_item` DISABLE KEYS */;
INSERT INTO `voucher_template_item` VALUES ('1921198658528059393','2','1','2001',NULL,'2','1921196059691671553','1','2025-05-10 21:40:28','1','2025-05-10 21:40:28','n');
/*!40000 ALTER TABLE `voucher_template_item` ENABLE KEYS */;
UNLOCK TABLES;

-- standard_subject (from docs/*.xlsx)
DELETE FROM standard_subject_cash_flow;
DELETE FROM standard_subject WHERE standard_id IN ('1','2');
UPDATE standard SET name = '小企业会计准则' WHERE id = '1';
UPDATE standard SET name = '企业会计制度' WHERE id = '2';
-- standard_id=1 小企业会计准则
INSERT INTO standard_subject VALUES ('749702933016700988', 1, '1001', '库存现金', '库存现金', NULL, NULL, '1', 1, NULL, '/749702933016700988', 1, 1, 1, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('902089263573432927', 1, '1002', '银行存款', '银行存款', NULL, NULL, '1', 1, NULL, '/902089263573432927', 1, 1, 1, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('450847121255219615', 1, '1004', '备用金', '备用金', NULL, NULL, '1', 1, NULL, '/450847121255219615', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1084962767134521749', 1, '1012', '其他货币资金', '其他货币资金', NULL, NULL, '1', 1, NULL, '/1084962767134521749', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1063630794247432016', 1, '1012.01', '银行汇票', '其他货币资金_银行汇票', NULL, NULL, '1', 1, '1084962767134521749', '/1084962767134521749/1063630794247432016', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('988635213539034916', 1, '1012.02', '银行本票', '其他货币资金_银行本票', NULL, NULL, '1', 1, '1084962767134521749', '/1084962767134521749/988635213539034916', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('969908648722982491', 1, '1012.03', '信用卡', '其他货币资金_信用卡', NULL, NULL, '1', 1, '1084962767134521749', '/1084962767134521749/969908648722982491', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('883757969450463944', 1, '1012.04', '信用证保证金', '其他货币资金_信用证保证金', NULL, NULL, '1', 1, '1084962767134521749', '/1084962767134521749/883757969450463944', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('56225696272841853', 1, '1012.05', '外埠存款', '其他货币资金_外埠存款', NULL, NULL, '1', 1, '1084962767134521749', '/1084962767134521749/56225696272841853', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('194571372989620404', 1, '1101', '短期投资', '短期投资', NULL, NULL, '1', 1, NULL, '/194571372989620404', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('719902564669235298', 1, '1101.01', '股票', '短期投资_股票', NULL, NULL, '1', 1, '194571372989620404', '/194571372989620404/719902564669235298', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1007828983040400348', 1, '1101.02', '债券', '短期投资_债券', NULL, NULL, '1', 1, '194571372989620404', '/194571372989620404/1007828983040400348', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('589247807095472686', 1, '1101.03', '基金', '短期投资_基金', NULL, NULL, '1', 1, '194571372989620404', '/194571372989620404/589247807095472686', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('330325951343775263', 1, '1101.04', '其他', '短期投资_其他', NULL, NULL, '1', 1, '194571372989620404', '/194571372989620404/330325951343775263', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('548200004702862409', 1, '1121', '应收票据', '应收票据', NULL, NULL, '1', 1, NULL, '/548200004702862409', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('374850848815697853', 1, '1122', '应收账款', '应收账款', NULL, NULL, '1', 1, NULL, '/374850848815697853', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('111159173868441984', 1, '1123', '预付账款', '预付账款', NULL, NULL, '1', 1, NULL, '/111159173868441984', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('424414693048713112', 1, '1131', '应收股利', '应收股利', NULL, NULL, '1', 1, NULL, '/424414693048713112', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('34277037365165975', 1, '1132', '应收利息', '应收利息', NULL, NULL, '1', 1, NULL, '/34277037365165975', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('716286940746391888', 1, '1221', '其他应收款', '其他应收款', NULL, NULL, '1', 1, NULL, '/716286940746391888', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('833045753348603993', 1, '1401', '材料采购', '材料采购', NULL, NULL, '1', 1, NULL, '/833045753348603993', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('644450463533081991', 1, '1402', '在途物资', '在途物资', NULL, NULL, '1', 1, NULL, '/644450463533081991', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('326147926455680688', 1, '1403', '原材料', '原材料', NULL, NULL, '1', 1, NULL, '/326147926455680688', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('301093982769284070', 1, '1404', '材料成本差异', '材料成本差异', NULL, NULL, '1', 1, NULL, '/301093982769284070', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('939880228719910906', 1, '1405', '库存商品', '库存商品', NULL, NULL, '1', 1, NULL, '/939880228719910906', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1028634108464128041', 1, '1407', '商品进销差价', '商品进销差价', NULL, NULL, '2', 1, NULL, '/1028634108464128041', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('199081882926362013', 1, '1408', '委托加工物资', '委托加工物资', NULL, NULL, '1', 1, NULL, '/199081882926362013', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('649022476802640374', 1, '1411', '周转材料', '周转材料', NULL, NULL, '1', 1, NULL, '/649022476802640374', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1009252452349848991', 1, '1411.01', '在库', '周转材料_在库', NULL, NULL, '1', 1, '649022476802640374', '/649022476802640374/1009252452349848991', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('403649480676290353', 1, '1411.02', '在用', '周转材料_在用', NULL, NULL, '1', 1, '649022476802640374', '/649022476802640374/403649480676290353', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('740824986589244889', 1, '1411.03', '摊销', '周转材料_摊销', NULL, NULL, '1', 1, '649022476802640374', '/649022476802640374/740824986589244889', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('21781074887029994', 1, '1412', '包装物', '包装物', NULL, NULL, '1', 1, NULL, '/21781074887029994', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('476638751839009162', 1, '1413', '低值易耗品', '低值易耗品', NULL, NULL, '1', 1, NULL, '/476638751839009162', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('509566601208112795', 1, '1421', '消耗性生物资产', '消耗性生物资产', NULL, NULL, '1', 1, NULL, '/509566601208112795', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('842130220185354653', 1, '1501', '长期债券投资', '长期债券投资', NULL, NULL, '1', 1, NULL, '/842130220185354653', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('258788474905568055', 1, '1501.01', '面值', '长期债券投资_面值', NULL, NULL, '1', 1, '842130220185354653', '/842130220185354653/258788474905568055', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('463488842493590861', 1, '1501.02', '溢折价', '长期债券投资_溢折价', NULL, NULL, '1', 1, '842130220185354653', '/842130220185354653/463488842493590861', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('560415751171177657', 1, '1501.03', '应计利息', '长期债券投资_应计利息', NULL, NULL, '1', 1, '842130220185354653', '/842130220185354653/560415751171177657', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('807413002798397679', 1, '1511', '长期股权投资', '长期股权投资', NULL, NULL, '1', 1, NULL, '/807413002798397679', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('208446554915295969', 1, '1601', '固定资产', '固定资产', NULL, NULL, '1', 1, NULL, '/208446554915295969', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('897591490442780663', 1, '1602', '累计折旧', '累计折旧', NULL, NULL, '2', 1, NULL, '/897591490442780663', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('981981423267944382', 1, '1604', '在建工程', '在建工程', NULL, NULL, '1', 1, NULL, '/981981423267944382', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('743363861791495131', 1, '1605', '工程物资', '工程物资', NULL, NULL, '1', 1, NULL, '/743363861791495131', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('301124037418377497', 1, '1605.01', '专用材料', '工程物资_专用材料', NULL, NULL, '1', 1, '743363861791495131', '/743363861791495131/301124037418377497', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1040234591231879961', 1, '1605.02', '专用设备', '工程物资_专用设备', NULL, NULL, '1', 1, '743363861791495131', '/743363861791495131/1040234591231879961', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('448999279988727251', 1, '1605.03', '工器具', '工程物资_工器具', NULL, NULL, '1', 1, '743363861791495131', '/743363861791495131/448999279988727251', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('119320551140832359', 1, '1606', '固定资产清理', '固定资产清理', NULL, NULL, '1', 1, NULL, '/119320551140832359', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('831564114040212852', 1, '1621', '生产性生物资产', '生产性生物资产', NULL, NULL, '1', 1, NULL, '/831564114040212852', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('669746962748777938', 1, '1621.01', '未成熟生产性生物资产', '生产性生物资产_未成熟生产性生物资产', NULL, NULL, '1', 1, '831564114040212852', '/831564114040212852/669746962748777938', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('640423313572631772', 1, '1621.02', '成熟生产性生物资产', '生产性生物资产_成熟生产性生物资产', NULL, NULL, '1', 1, '831564114040212852', '/831564114040212852/640423313572631772', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('314629835614345705', 1, '1622', '生产性生物资产累计折旧', '生产性生物资产累计折旧', NULL, NULL, '2', 1, NULL, '/314629835614345705', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('4970954352756458', 1, '1701', '无形资产', '无形资产', NULL, NULL, '1', 1, NULL, '/4970954352756458', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('510296499505407655', 1, '1702', '累计摊销', '累计摊销', NULL, NULL, '2', 1, NULL, '/510296499505407655', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('503576780026099238', 1, '1801', '长期待摊费用', '长期待摊费用', NULL, NULL, '1', 1, NULL, '/503576780026099238', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('763544831048612634', 1, '1901', '待处理财产损溢', '待处理财产损溢', NULL, NULL, '1', 1, NULL, '/763544831048612634', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('257019391725425944', 1, '1901.01', '待处理流动资产损溢', '待处理财产损溢_待处理流动资产损溢', NULL, NULL, '1', 1, '763544831048612634', '/763544831048612634/257019391725425944', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('373278428292255186', 1, '1901.02', '待处理非流动资产损溢', '待处理财产损溢_待处理非流动资产损溢', NULL, NULL, '1', 1, '763544831048612634', '/763544831048612634/373278428292255186', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1016024886328512044', 2, '2001', '短期借款', '短期借款', NULL, NULL, '2', 1, NULL, '/1016024886328512044', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('303230040444897375', 2, '2201', '应付票据', '应付票据', NULL, NULL, '2', 1, NULL, '/303230040444897375', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('399699164766810563', 2, '2202', '应付账款', '应付账款', NULL, NULL, '2', 1, NULL, '/399699164766810563', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('608351184384775694', 2, '2203', '预收账款', '预收账款', NULL, NULL, '2', 1, NULL, '/608351184384775694', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('193098573785506060', 2, '2211', '应付职工薪酬', '应付职工薪酬', NULL, NULL, '2', 1, NULL, '/193098573785506060', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1111033804289452764', 2, '2211.01', '职工工资', '应付职工薪酬_职工工资', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/1111033804289452764', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('397915539892013623', 2, '2211.02', '奖金、津贴和补贴', '应付职工薪酬_奖金、津贴和补贴', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/397915539892013623', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('501067428645654047', 2, '2211.03', '职工福利费', '应付职工薪酬_职工福利费', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/501067428645654047', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('280781716855927127', 2, '2211.04', '社会保险费', '应付职工薪酬_社会保险费', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/280781716855927127', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('45305955321125791', 2, '2211.05', '住房公积金', '应付职工薪酬_住房公积金', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/45305955321125791', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('166598167913459128', 2, '2211.06', '工会经费', '应付职工薪酬_工会经费', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/166598167913459128', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('18025324482703032', 2, '2211.07', '职工教育经费', '应付职工薪酬_职工教育经费', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/18025324482703032', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1043466874453169710', 2, '2211.08', '非货币性福利', '应付职工薪酬_非货币性福利', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/1043466874453169710', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('664762842608723759', 2, '2211.09', '辞退福利', '应付职工薪酬_辞退福利', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/664762842608723759', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('891149041444482901', 2, '2211.10', '其他', '应付职工薪酬_其他', NULL, NULL, '2', 1, '193098573785506060', '/193098573785506060/891149041444482901', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1077118329809429686', 2, '2221', '应交税费', '应交税费', NULL, NULL, '2', 1, NULL, '/1077118329809429686', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('838632998063980099', 2, '2221.01', '增值税', '应交税费_增值税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/838632998063980099', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('967456445550485454', 2, '2221.01.01', '进项税额', '应交税费_增值税_进项税额', NULL, NULL, '1', 1, '838632998063980099', '/1077118329809429686/838632998063980099/967456445550485454', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('865912803031381414', 2, '2221.01.02', '销项税额', '应交税费_增值税_销项税额', NULL, NULL, '2', 1, '838632998063980099', '/1077118329809429686/838632998063980099/865912803031381414', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1129437170393087446', 2, '2221.01.03', '出口抵减内销产品应纳税额', '应交税费_增值税_出口抵减内销产品应纳税额', NULL, NULL, '1', 1, '838632998063980099', '/1077118329809429686/838632998063980099/1129437170393087446', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1041650598729644247', 2, '2221.01.04', '进项税额转出', '应交税费_增值税_进项税额转出', NULL, NULL, '2', 1, '838632998063980099', '/1077118329809429686/838632998063980099/1041650598729644247', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('467621788589822429', 2, '2221.01.05', '出口退税', '应交税费_增值税_出口退税', NULL, NULL, '2', 1, '838632998063980099', '/1077118329809429686/838632998063980099/467621788589822429', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('886061377017157715', 2, '2221.01.06', '已交税金', '应交税费_增值税_已交税金', NULL, NULL, '1', 1, '838632998063980099', '/1077118329809429686/838632998063980099/886061377017157715', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('800495199929476459', 2, '2221.02', '消费税', '应交税费_消费税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/800495199929476459', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('718742504605041146', 2, '2221.03', '营业税', '应交税费_营业税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/718742504605041146', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('227570477751639222', 2, '2221.04', '城市维护建设税', '应交税费_城市维护建设税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/227570477751639222', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('159473211376780324', 2, '2221.05', '企业所得税', '应交税费_企业所得税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/159473211376780324', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('434894588167044168', 2, '2221.06', '资源税', '应交税费_资源税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/434894588167044168', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1088858632207943608', 2, '2221.07', '土地增值税', '应交税费_土地增值税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/1088858632207943608', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('857649777367728233', 2, '2221.08', '城镇土地使用税', '应交税费_城镇土地使用税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/857649777367728233', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('774632939900523013', 2, '2221.09', '房产税', '应交税费_房产税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/774632939900523013', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('748993991796590966', 2, '2221.10', '车船税', '应交税费_车船税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/748993991796590966', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('532914001244920834', 2, '2221.11', '教育费附加', '应交税费_教育费附加', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/532914001244920834', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('959988494802052858', 2, '2221.12', '矿产资源补偿费', '应交税费_矿产资源补偿费', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/959988494802052858', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('282096661083617339', 2, '2221.13', '排污费', '应交税费_排污费', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/282096661083617339', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('67801498332726030', 2, '2221.14', '个人所得税', '应交税费_个人所得税', NULL, NULL, '2', 1, '1077118329809429686', '/1077118329809429686/67801498332726030', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('12460327150688449', 2, '2231', '应付利息', '应付利息', NULL, NULL, '2', 1, NULL, '/12460327150688449', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('419889960925002740', 2, '2232', '应付利润', '应付利润', NULL, NULL, '2', 1, NULL, '/419889960925002740', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('926076312956300602', 2, '2241', '其他应付款', '其他应付款', NULL, NULL, '2', 1, NULL, '/926076312956300602', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('725172820757508532', 2, '2401', '递延收益', '递延收益', NULL, NULL, '2', 1, NULL, '/725172820757508532', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('204543592319536108', 2, '2501', '长期借款', '长期借款', NULL, NULL, '2', 1, NULL, '/204543592319536108', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1007136145775191859', 2, '2701', '长期应付款', '长期应付款', NULL, NULL, '2', 1, NULL, '/1007136145775191859', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('727413604339074708', 4, '3001', '实收资本', '实收资本', NULL, NULL, '2', 1, NULL, '/727413604339074708', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('511880383922657836', 4, '3002', '资本公积', '资本公积', NULL, NULL, '2', 1, NULL, '/511880383922657836', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('353943000394406024', 4, '3101', '盈余公积', '盈余公积', NULL, NULL, '2', 1, NULL, '/353943000394406024', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('991754201737382368', 4, '3101.01', '法定盈余公积', '盈余公积_法定盈余公积', NULL, NULL, '2', 1, '353943000394406024', '/353943000394406024/991754201737382368', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('63582038783252421', 4, '3101.02', '任意盈余公积', '盈余公积_任意盈余公积', NULL, NULL, '2', 1, '353943000394406024', '/353943000394406024/63582038783252421', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('969570531640075512', 4, '3101.03', '职工奖励及福利基金', '盈余公积_职工奖励及福利基金', NULL, NULL, '2', 1, '353943000394406024', '/353943000394406024/969570531640075512', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('287971074589664825', 4, '3101.04', '储备基金', '盈余公积_储备基金', NULL, NULL, '2', 1, '353943000394406024', '/353943000394406024/287971074589664825', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('416552915218909060', 4, '3101.05', '企业发展基金', '盈余公积_企业发展基金', NULL, NULL, '2', 1, '353943000394406024', '/353943000394406024/416552915218909060', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('354430320609257558', 4, '3101.06', '利润归还投资', '盈余公积_利润归还投资', NULL, NULL, '2', 1, '353943000394406024', '/353943000394406024/354430320609257558', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('519602121532905294', 4, '3103', '本年利润', '本年利润', NULL, NULL, '2', 1, NULL, '/519602121532905294', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('677049060709200054', 4, '3104', '利润分配', '利润分配', NULL, NULL, '2', 1, NULL, '/677049060709200054', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('762614936715364761', 4, '3104.01', '应付利润', '利润分配_应付利润', NULL, NULL, '2', 1, '677049060709200054', '/677049060709200054/762614936715364761', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('805407824381170921', 4, '3104.02', '未分配利润', '利润分配_未分配利润', NULL, NULL, '2', 1, '677049060709200054', '/677049060709200054/805407824381170921', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('2103139329417127', 5, '4001', '生产成本', '生产成本', NULL, NULL, '1', 1, NULL, '/2103139329417127', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('128073157553436887', 5, '4001.01', '基本生产成本', '生产成本_基本生产成本', NULL, NULL, '1', 1, '2103139329417127', '/2103139329417127/128073157553436887', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('824600182698609206', 5, '4001.02', '辅助生产成本', '生产成本_辅助生产成本', NULL, NULL, '1', 1, '2103139329417127', '/2103139329417127/824600182698609206', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('867348083928787', 5, '4002', '劳务成本', '劳务成本', NULL, NULL, '1', 1, NULL, '/867348083928787', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('153215908165117453', 5, '4101', '制造费用', '制造费用', NULL, NULL, '1', 1, NULL, '/153215908165117453', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('353453201488910980', 5, '4101.01', '机物料消耗', '制造费用_机物料消耗', NULL, NULL, '1', 1, '153215908165117453', '/153215908165117453/353453201488910980', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('450718011053172834', 5, '4101.02', '修理费', '制造费用_修理费', NULL, NULL, '1', 1, '153215908165117453', '/153215908165117453/450718011053172834', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('410649062964307991', 5, '4101.03', '职工薪酬', '制造费用_职工薪酬', NULL, NULL, '1', 1, '153215908165117453', '/153215908165117453/410649062964307991', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('92311836086774594', 5, '4101.04', '折旧费', '制造费用_折旧费', NULL, NULL, '1', 1, '153215908165117453', '/153215908165117453/92311836086774594', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('30196390024403054', 5, '4101.05', '办公费', '制造费用_办公费', NULL, NULL, '1', 1, '153215908165117453', '/153215908165117453/30196390024403054', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('165244064432251237', 5, '4101.06', '水电费', '制造费用_水电费', NULL, NULL, '1', 1, '153215908165117453', '/153215908165117453/165244064432251237', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1151017694598048291', 5, '4101.07', '停工损失', '制造费用_停工损失', NULL, NULL, '1', 1, '153215908165117453', '/153215908165117453/1151017694598048291', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('201569388571593151', 5, '4301', '研发支出', '研发支出', NULL, NULL, '1', 1, NULL, '/201569388571593151', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('277697437154632703', 5, '4301.01', '费用化支出', '研发支出_费用化支出', NULL, NULL, '1', 1, '201569388571593151', '/201569388571593151/277697437154632703', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('78976668621787905', 5, '4301.02', '资本化支出', '研发支出_资本化支出', NULL, NULL, '1', 1, '201569388571593151', '/201569388571593151/78976668621787905', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('710452289168624835', 5, '4401', '工程施工', '工程施工', NULL, NULL, '1', 1, NULL, '/710452289168624835', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('252832747455096570', 5, '4401.01', '合同成本', '工程施工_合同成本', NULL, NULL, '1', 1, '710452289168624835', '/710452289168624835/252832747455096570', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('783687688129166235', 5, '4401.02', '间接费用', '工程施工_间接费用', NULL, NULL, '1', 1, '710452289168624835', '/710452289168624835/783687688129166235', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('664532348911384357', 5, '4403', '机械作业', '机械作业', NULL, NULL, '1', 1, NULL, '/664532348911384357', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('424964055927787542', 6, '5001', '主营业务收入', '主营业务收入', NULL, NULL, '2', 1, NULL, '/424964055927787542', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('36271853737031447', 6, '5051', '其他业务收入', '其他业务收入', NULL, NULL, '2', 1, NULL, '/36271853737031447', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('544126708457321818', 6, '5051.01', '销售材料', '其他业务收入_销售材料', NULL, NULL, '2', 1, '36271853737031447', '/36271853737031447/544126708457321818', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('824574438799221177', 6, '5051.02', '出租固定资产', '其他业务收入_出租固定资产', NULL, NULL, '2', 1, '36271853737031447', '/36271853737031447/824574438799221177', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1115892673783662549', 6, '5051.03', '出租无形资产', '其他业务收入_出租无形资产', NULL, NULL, '2', 1, '36271853737031447', '/36271853737031447/1115892673783662549', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('961195335632795051', 6, '5111', '投资收益', '投资收益', NULL, NULL, '2', 1, NULL, '/961195335632795051', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('830081256033309801', 6, '5301', '营业外收入', '营业外收入', NULL, NULL, '2', 1, NULL, '/830081256033309801', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('179689077329406376', 6, '5301.01', '非流动资产处置净收益', '营业外收入_非流动资产处置净收益', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/179689077329406376', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('181984497524414559', 6, '5301.02', '政府补助', '营业外收入_政府补助', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/181984497524414559', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('115288212038747336', 6, '5301.03', '捐赠收益', '营业外收入_捐赠收益', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/115288212038747336', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('929308014268197543', 6, '5301.04', '盘盈收益', '营业外收入_盘盈收益', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/929308014268197543', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('236192865159096069', 6, '5301.05', '汇兑收益', '营业外收入_汇兑收益', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/236192865159096069', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('413124640042568890', 6, '5301.06', '出租包装物和商品的租金收入', '营业外收入_出租包装物和商品的租金收入', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/413124640042568890', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('488122028612282866', 6, '5301.07', '逾期未退包装物押金收益', '营业外收入_逾期未退包装物押金收益', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/488122028612282866', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1087374101271856562', 6, '5301.08', '确实无法偿付的应付款项', '营业外收入_确实无法偿付的应付款项', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/1087374101271856562', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1059856979520113335', 6, '5301.09', '已作坏账损失处理后又收回的应收款项', '营业外收入_已作坏账损失处理后又收回的应收款项', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/1059856979520113335', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('545109778118082534', 6, '5301.10', '违约金收益', '营业外收入_违约金收益', NULL, NULL, '2', 1, '830081256033309801', '/830081256033309801/545109778118082534', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('277365553312273065', 5, '5401', '主营业务成本', '主营业务成本', NULL, NULL, '1', 1, NULL, '/277365553312273065', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('531920126706187323', 5, '5402', '其他业务成本', '其他业务成本', NULL, NULL, '1', 1, NULL, '/531920126706187323', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('149435717226639233', 5, '5402.01', '销售材料的成本', '其他业务成本_销售材料的成本', NULL, NULL, '1', 1, '531920126706187323', '/531920126706187323/149435717226639233', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('676665199552926487', 5, '5402.02', '出租固定资产的折旧费', '其他业务成本_出租固定资产的折旧费', NULL, NULL, '1', 1, '531920126706187323', '/531920126706187323/676665199552926487', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('540335648716011579', 5, '5402.03', '出租无形资产的摊销额', '其他业务成本_出租无形资产的摊销额', NULL, NULL, '1', 1, '531920126706187323', '/531920126706187323/540335648716011579', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('100427184105997558', 5, '5403', '营业税金及附加', '营业税金及附加', NULL, NULL, '1', 1, NULL, '/100427184105997558', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('489091640774212487', 5, '5403.01', '消费税', '营业税金及附加_消费税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/489091640774212487', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('590341301574678560', 5, '5403.02', '营业税', '营业税金及附加_营业税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/590341301574678560', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('812582450524876474', 5, '5403.03', '城市维护建设税', '营业税金及附加_城市维护建设税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/812582450524876474', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('473721803065327801', 5, '5403.04', '资源税', '营业税金及附加_资源税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/473721803065327801', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('892421039117526742', 5, '5403.05', '土地增值税', '营业税金及附加_土地增值税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/892421039117526742', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('536243218163408169', 5, '5403.06', '城镇土地使用税', '营业税金及附加_城镇土地使用税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/536243218163408169', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('973756253945654789', 5, '5403.07', '房产税', '营业税金及附加_房产税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/973756253945654789', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('364640556608354615', 5, '5403.08', '车船税', '营业税金及附加_车船税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/364640556608354615', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1049621541622074681', 5, '5403.09', '印花税', '营业税金及附加_印花税', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/1049621541622074681', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1485273795201226', 5, '5403.10', '教育费附加', '营业税金及附加_教育费附加', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/1485273795201226', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('318078891172795124', 5, '5403.11', '矿产资源补偿费', '营业税金及附加_矿产资源补偿费', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/318078891172795124', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('285194206545629920', 5, '5403.12', '排污费', '营业税金及附加_排污费', NULL, NULL, '1', 1, '100427184105997558', '/100427184105997558/285194206545629920', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('589632474711710055', 6, '5601', '销售费用', '销售费用', NULL, NULL, '1', 1, NULL, '/589632474711710055', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('555196468194348915', 6, '5601.01', '职工薪酬', '销售费用_职工薪酬', NULL, NULL, '1', 1, '589632474711710055', '/589632474711710055/555196468194348915', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('582001541819729084', 6, '5601.02', '商品维修费', '销售费用_商品维修费', NULL, NULL, '1', 1, '589632474711710055', '/589632474711710055/582001541819729084', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('458458493817103802', 6, '5601.03', '运输费', '销售费用_运输费', NULL, NULL, '1', 1, '589632474711710055', '/589632474711710055/458458493817103802', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('296149409676833371', 6, '5601.04', '装卸费', '销售费用_装卸费', NULL, NULL, '1', 1, '589632474711710055', '/589632474711710055/296149409676833371', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('744326260290692380', 6, '5601.05', '包装费', '销售费用_包装费', NULL, NULL, '1', 1, '589632474711710055', '/589632474711710055/744326260290692380', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1033720801108128122', 6, '5601.06', '保险费', '销售费用_保险费', NULL, NULL, '1', 1, '589632474711710055', '/589632474711710055/1033720801108128122', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('162497604496239875', 6, '5601.07', '广告费和业务宣传费', '销售费用_广告费和业务宣传费', NULL, NULL, '1', 1, '589632474711710055', '/589632474711710055/162497604496239875', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('57149110476442101', 6, '5601.08', '展览费', '销售费用_展览费', NULL, NULL, '1', 1, '589632474711710055', '/589632474711710055/57149110476442101', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('747563325671043307', 6, '5602', '管理费用', '管理费用', NULL, NULL, '1', 1, NULL, '/747563325671043307', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('532793162389283125', 6, '5602.01', '开办费', '管理费用_开办费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/532793162389283125', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('40892562346840558', 6, '5602.02', '折旧费', '管理费用_折旧费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/40892562346840558', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('687333708939165730', 6, '5602.03', '修理费', '管理费用_修理费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/687333708939165730', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('971467653336859720', 6, '5602.04', '办公费', '管理费用_办公费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/971467653336859720', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1102029908922978068', 6, '5602.05', '水电费', '管理费用_水电费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/1102029908922978068', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('60144023596414760', 6, '5602.06', '差旅费', '管理费用_差旅费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/60144023596414760', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1132197634324363653', 6, '5602.07', '职工薪酬', '管理费用_职工薪酬', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/1132197634324363653', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('870798341796723175', 6, '5602.08', '业务招待费', '管理费用_业务招待费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/870798341796723175', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('257561997768865829', 6, '5602.09', '研究费用', '管理费用_研究费用', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/257561997768865829', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('388167455432835277', 6, '5602.10', '技术转让费', '管理费用_技术转让费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/388167455432835277', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('729721951106685620', 6, '5602.11', '长期待摊费用摊销', '管理费用_长期待摊费用摊销', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/729721951106685620', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('779399489521185915', 6, '5602.12', '财产保险费', '管理费用_财产保险费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/779399489521185915', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('68516139057152429', 6, '5602.13', '聘请中介机构费', '管理费用_聘请中介机构费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/68516139057152429', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('765913857394784302', 6, '5602.14', '咨询费', '管理费用_咨询费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/765913857394784302', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('97148475063405750', 6, '5602.15', '诉讼费', '管理费用_诉讼费', NULL, NULL, '1', 1, '747563325671043307', '/747563325671043307/97148475063405750', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('589013534146504622', 6, '5603', '财务费用', '财务费用', NULL, NULL, '1', 1, NULL, '/589013534146504622', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1084966881481960366', 6, '5603.01', '利息费用', '财务费用_利息费用', NULL, NULL, '1', 1, '589013534146504622', '/589013534146504622/1084966881481960366', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('447828457763072081', 6, '5603.02', '汇兑损失', '财务费用_汇兑损失', NULL, NULL, '1', 1, '589013534146504622', '/589013534146504622/447828457763072081', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1079623673989556776', 6, '5603.03', '手续费', '财务费用_手续费', NULL, NULL, '1', 1, '589013534146504622', '/589013534146504622/1079623673989556776', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('303835093415182055', 6, '5603.04', '现金折扣', '财务费用_现金折扣', NULL, NULL, '1', 1, '589013534146504622', '/589013534146504622/303835093415182055', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1103762304363110326', 6, '5711', '营业外支出', '营业外支出', NULL, NULL, '1', 1, NULL, '/1103762304363110326', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('219725009365339635', 6, '5711.01', '存货的盘亏、毁损、报废损失', '营业外支出_存货的盘亏、毁损、报废损失', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/219725009365339635', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('153474543861860283', 6, '5711.02', '非流动资产处置净损失', '营业外支出_非流动资产处置净损失', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/153474543861860283', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('780099573875647498', 6, '5711.03', '坏账损失', '营业外支出_坏账损失', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/780099573875647498', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('428999372175189679', 6, '5711.04', '无法收回的长期债券投资损失', '营业外支出_无法收回的长期债券投资损失', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/428999372175189679', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('748526871472886236', 6, '5711.05', '无法收回的长期股权投资损失', '营业外支出_无法收回的长期股权投资损失', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/748526871472886236', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('527641693993728571', 6, '5711.06', '自然灾害等不可抗力因素造成的损失', '营业外支出_自然灾害等不可抗力因素造成的损失', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/527641693993728571', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('212885987625087676', 6, '5711.07', '税收滞纳金、罚金、罚款', '营业外支出_税收滞纳金、罚金、罚款', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/212885987625087676', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1148313365283309747', 6, '5711.08', '被没收财物的损失', '营业外支出_被没收财物的损失', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/1148313365283309747', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('453079917705989829', 6, '5711.09', '捐赠支出', '营业外支出_捐赠支出', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/453079917705989829', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('776745705172258989', 6, '5711.10', '赞助支出', '营业外支出_赞助支出', NULL, NULL, '1', 1, '1103762304363110326', '/1103762304363110326/776745705172258989', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('439155879295525226', 6, '5801', '所得税费用', '所得税费用', NULL, NULL, '1', 1, NULL, '/439155879295525226', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '1', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
-- standard_id=2 企业会计制度
INSERT INTO standard_subject VALUES ('423528680448921284', 1, '1001', '现金', '现金', NULL, NULL, '1', 1, NULL, '/423528680448921284', 1, 1, 1, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('591648417419909831', 1, '1002', '银行存款', '银行存款', NULL, NULL, '1', 1, NULL, '/591648417419909831', 1, 1, 1, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('8567534440002831', 1, '1009', '其他货币资金', '其他货币资金', NULL, NULL, '1', 1, NULL, '/8567534440002831', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('235853121097875943', 1, '1009.01', '外埠存款', '其他货币资金_外埠存款', NULL, NULL, '1', 1, '8567534440002831', '/8567534440002831/235853121097875943', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('305990297973772982', 1, '1009.02', '银行本票', '其他货币资金_银行本票', NULL, NULL, '1', 1, '8567534440002831', '/8567534440002831/305990297973772982', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('128020627936767403', 1, '1009.03', '银行汇票', '其他货币资金_银行汇票', NULL, NULL, '1', 1, '8567534440002831', '/8567534440002831/128020627936767403', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('657091166274514869', 1, '1009.04', '信用卡', '其他货币资金_信用卡', NULL, NULL, '1', 1, '8567534440002831', '/8567534440002831/657091166274514869', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('622001580154009410', 1, '1009.05', '信用证保证金', '其他货币资金_信用证保证金', NULL, NULL, '1', 1, '8567534440002831', '/8567534440002831/622001580154009410', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('778645278271842227', 1, '1009.06', '存出投资款', '其他货币资金_存出投资款', NULL, NULL, '1', 1, '8567534440002831', '/8567534440002831/778645278271842227', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('736417949575338212', 1, '1101', '短期投资', '短期投资', NULL, NULL, '1', 1, NULL, '/736417949575338212', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('490667428266218084', 1, '1101.01', '股票', '短期投资_股票', NULL, NULL, '1', 1, '736417949575338212', '/736417949575338212/490667428266218084', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('496010640911275598', 1, '1101.02', '债券', '短期投资_债券', NULL, NULL, '1', 1, '736417949575338212', '/736417949575338212/496010640911275598', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1028675841311010108', 1, '1101.03', '基金', '短期投资_基金', NULL, NULL, '1', 1, '736417949575338212', '/736417949575338212/1028675841311010108', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('480863032988672215', 1, '1101.10', '其他', '短期投资_其他', NULL, NULL, '1', 1, '736417949575338212', '/736417949575338212/480863032988672215', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1101799282896693936', 1, '1102', '短期投资跌价准备', '短期投资跌价准备', NULL, NULL, '2', 1, NULL, '/1101799282896693936', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('74412912305034538', 1, '1111', '应收票据', '应收票据', NULL, NULL, '1', 1, NULL, '/74412912305034538', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('776008062246174045', 1, '1121', '应收股利', '应收股利', NULL, NULL, '1', 1, NULL, '/776008062246174045', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1091783075223135775', 1, '1122', '应收利息', '应收利息', NULL, NULL, '1', 1, NULL, '/1091783075223135775', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1062229468051975331', 1, '1131', '应收账款', '应收账款', NULL, NULL, '1', 1, NULL, '/1062229468051975331', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('345172378702060529', 1, '1133', '其他应收款', '其他应收款', NULL, NULL, '1', 1, NULL, '/345172378702060529', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('141479724556910360', 1, '1141', '坏账准备', '坏账准备', NULL, NULL, '2', 1, NULL, '/141479724556910360', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('848552545321778309', 1, '1151', '预付账款', '预付账款', NULL, NULL, '1', 1, NULL, '/848552545321778309', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('798960869617275413', 1, '1161', '应收补贴款', '应收补贴款', NULL, NULL, '1', 1, NULL, '/798960869617275413', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('149339873019001112', 1, '1201', '物资采购', '物资采购', NULL, NULL, '1', 1, NULL, '/149339873019001112', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('350195609921879351', 1, '1211', '原材料', '原材料', NULL, NULL, '1', 1, NULL, '/350195609921879351', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('382222053398943731', 1, '1221', '包装物', '包装物', NULL, NULL, '1', 1, NULL, '/382222053398943731', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('360078236749803871', 1, '1231', '低值易耗品', '低值易耗品', NULL, NULL, '1', 1, NULL, '/360078236749803871', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('761059721863883419', 1, '1232', '材料成本差异', '材料成本差异', NULL, NULL, '1', 1, NULL, '/761059721863883419', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('363985421340731902', 1, '1241', '自制半成品', '自制半成品', NULL, NULL, '1', 1, NULL, '/363985421340731902', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('33093633325539372', 1, '1243', '库存商品', '库存商品', NULL, NULL, '1', 1, NULL, '/33093633325539372', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('478241526240560819', 1, '1244', '商品进销差价', '商品进销差价', NULL, NULL, '1', 1, NULL, '/478241526240560819', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('238333986679162200', 1, '1251', '委托加工物资', '委托加工物资', NULL, NULL, '1', 1, NULL, '/238333986679162200', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('712001399115593780', 1, '1261', '委托代销商品', '委托代销商品', NULL, NULL, '1', 1, NULL, '/712001399115593780', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('120992289469004872', 1, '1271', '受托代销商品', '受托代销商品', NULL, NULL, '1', 1, NULL, '/120992289469004872', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1024056853892948746', 1, '1281', '存货跌价准备', '存货跌价准备', NULL, NULL, '2', 1, NULL, '/1024056853892948746', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('962848249676706788', 1, '1291', '分期收款发出商品', '分期收款发出商品', NULL, NULL, '1', 1, NULL, '/962848249676706788', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('662358227093176277', 1, '1301', '待摊费用', '待摊费用', NULL, NULL, '1', 1, NULL, '/662358227093176277', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('342805702813015986', 1, '1401', '长期股权投资', '长期股权投资', NULL, NULL, '1', 1, NULL, '/342805702813015986', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('486919493677951782', 1, '1401.01', '股票投资', '长期股权投资_股票投资', NULL, NULL, '1', 1, '342805702813015986', '/342805702813015986/486919493677951782', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1003988059210171103', 1, '1401.02', '其他股权投资', '长期股权投资_其他股权投资', NULL, NULL, '1', 1, '342805702813015986', '/342805702813015986/1003988059210171103', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('381242660234069755', 1, '1402', '长期债权投资', '长期债权投资', NULL, NULL, '1', 1, NULL, '/381242660234069755', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('203503626981056536', 1, '1402.01', '债券投资', '长期债权投资_债券投资', NULL, NULL, '1', 1, '381242660234069755', '/381242660234069755/203503626981056536', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('725399809485856987', 1, '1402.02', '其他债权投资', '长期债权投资_其他债权投资', NULL, NULL, '1', 1, '381242660234069755', '/381242660234069755/725399809485856987', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('355777757234808049', 1, '1421', '长期投资减值准备', '长期投资减值准备', NULL, NULL, '2', 1, NULL, '/355777757234808049', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('250611593804040803', 1, '1421.01', '股权投资减值准备', '长期投资减值准备_股权投资减值准备', NULL, NULL, '2', 1, '355777757234808049', '/355777757234808049/250611593804040803', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('578451337362535993', 1, '1421.02', '债权投资减值准备', '长期投资减值准备_债权投资减值准备', NULL, NULL, '2', 1, '355777757234808049', '/355777757234808049/578451337362535993', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('389034801308223467', 1, '1431', '委托贷款', '委托贷款', NULL, NULL, '1', 1, NULL, '/389034801308223467', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('83215054519804415', 1, '1431.01', '本金', '委托贷款_本金', NULL, NULL, '1', 1, '389034801308223467', '/389034801308223467/83215054519804415', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('688334335945684236', 1, '1431.02', '利息', '委托贷款_利息', NULL, NULL, '1', 1, '389034801308223467', '/389034801308223467/688334335945684236', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('563095953840153565', 1, '1431.03', '减值准备', '委托贷款_减值准备', NULL, NULL, '2', 1, '389034801308223467', '/389034801308223467/563095953840153565', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('252809557757628080', 1, '1501', '固定资产', '固定资产', NULL, NULL, '1', 1, NULL, '/252809557757628080', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('64577344603721157', 1, '1502', '累计折旧', '累计折旧', NULL, NULL, '2', 1, NULL, '/64577344603721157', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('30997051404938196', 1, '1505', '固定资产减值准备', '固定资产减值准备', NULL, NULL, '2', 1, NULL, '/30997051404938196', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('794138421580254744', 1, '1601', '工程物资', '工程物资', NULL, NULL, '1', 1, NULL, '/794138421580254744', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('748572243528106252', 1, '1601.01', '专用材料', '工程物资_专用材料', NULL, NULL, '1', 1, '794138421580254744', '/794138421580254744/748572243528106252', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('734481774087285526', 1, '1601.02', '专用设备', '工程物资_专用设备', NULL, NULL, '1', 1, '794138421580254744', '/794138421580254744/734481774087285526', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('264792534433264825', 1, '1601.03', '预付大型设备', '工程物资_预付大型设备', NULL, NULL, '1', 1, '794138421580254744', '/794138421580254744/264792534433264825', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('858605230852274060', 1, '1601.04', '为生产准备的工具及器具', '工程物资_为生产准备的工具及器具', NULL, NULL, '1', 1, '794138421580254744', '/794138421580254744/858605230852274060', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('353737205962942226', 1, '1603', '在建工程', '在建工程', NULL, NULL, '1', 1, NULL, '/353737205962942226', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('586704484687205198', 1, '1605', '在建工程减值准备', '在建工程减值准备', NULL, NULL, '2', 1, NULL, '/586704484687205198', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('275806399682279507', 1, '1701', '固定资产清理', '固定资产清理', NULL, NULL, '1', 1, NULL, '/275806399682279507', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('454414401700628934', 1, '1801', '无形资产', '无形资产', NULL, NULL, '1', 1, NULL, '/454414401700628934', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1080254477794627794', 1, '1805', '无形资产减值准备', '无形资产减值准备', NULL, NULL, '2', 1, NULL, '/1080254477794627794', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1066145570661787728', 1, '1815', '未确认融资费用', '未确认融资费用', NULL, NULL, '1', 1, NULL, '/1066145570661787728', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('610499776218235133', 1, '1901', '长期待摊费用', '长期待摊费用', NULL, NULL, '1', 1, NULL, '/610499776218235133', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('821414780392288197', 1, '1911', '待处理财产损溢', '待处理财产损溢', NULL, NULL, '1', 1, NULL, '/821414780392288197', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('853249951619681338', 1, '1911.01', '待处理流动资产损溢', '待处理财产损溢_待处理流动资产损溢', NULL, NULL, '1', 1, '821414780392288197', '/821414780392288197/853249951619681338', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1066193846488494250', 1, '1911.02', '待处理固定资产损溢', '待处理财产损溢_待处理固定资产损溢', NULL, NULL, '1', 1, '821414780392288197', '/821414780392288197/1066193846488494250', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('160546065768466283', 2, '2101', '短期借款', '短期借款', NULL, NULL, '2', 1, NULL, '/160546065768466283', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('461065975205492268', 2, '2111', '应付票据', '应付票据', NULL, NULL, '2', 1, NULL, '/461065975205492268', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('14671437948390694', 2, '2121', '应付账款', '应付账款', NULL, NULL, '2', 1, NULL, '/14671437948390694', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('796487907799474770', 2, '2131', '预收账款', '预收账款', NULL, NULL, '2', 1, NULL, '/796487907799474770', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('278249179993452599', 2, '2141', '代销商品款', '代销商品款', NULL, NULL, '2', 1, NULL, '/278249179993452599', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('851637215235461973', 2, '2151', '应付工资', '应付工资', NULL, NULL, '2', 1, NULL, '/851637215235461973', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('398614848104059169', 2, '2153', '应付福利费', '应付福利费', NULL, NULL, '2', 1, NULL, '/398614848104059169', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('707905078148585160', 2, '2161', '应付股利', '应付股利', NULL, NULL, '2', 1, NULL, '/707905078148585160', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('96951079817586790', 2, '2171', '应交税金', '应交税金', NULL, NULL, '2', 1, NULL, '/96951079817586790', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('292987966847542104', 2, '2171.01', '应交增值税', '应交税金_应交增值税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/292987966847542104', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('298503540721063382', 2, '2171.01.01', '进项税额', '应交税金_应交增值税_进项税额', NULL, NULL, '1', 1, '292987966847542104', '/96951079817586790/292987966847542104/298503540721063382', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('925650589304717385', 2, '2171.01.02', '已交税金', '应交税金_应交增值税_已交税金', NULL, NULL, '1', 1, '292987966847542104', '/96951079817586790/292987966847542104/925650589304717385', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('683952445428556723', 2, '2171.01.03', '转出未交增值税', '应交税金_应交增值税_转出未交增值税', NULL, NULL, '1', 1, '292987966847542104', '/96951079817586790/292987966847542104/683952445428556723', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('995487266531712315', 2, '2171.01.04', '减免税款', '应交税金_应交增值税_减免税款', NULL, NULL, '1', 1, '292987966847542104', '/96951079817586790/292987966847542104/995487266531712315', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('7407293778554049', 2, '2171.01.05', '销项税额', '应交税金_应交增值税_销项税额', NULL, NULL, '2', 1, '292987966847542104', '/96951079817586790/292987966847542104/7407293778554049', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('147199752451547604', 2, '2171.01.06', '出口退税', '应交税金_应交增值税_出口退税', NULL, NULL, '1', 1, '292987966847542104', '/96951079817586790/292987966847542104/147199752451547604', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('273908113885775338', 2, '2171.01.07', '进项税额转出', '应交税金_应交增值税_进项税额转出', NULL, NULL, '2', 1, '292987966847542104', '/96951079817586790/292987966847542104/273908113885775338', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('223464122272265042', 2, '2171.01.08', '出口抵减内销产品应纳税额', '应交税金_应交增值税_出口抵减内销产品应纳税额', NULL, NULL, '2', 1, '292987966847542104', '/96951079817586790/292987966847542104/223464122272265042', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('932092514839301682', 2, '2171.01.09', '转出多交增值税', '应交税金_应交增值税_转出多交增值税', NULL, NULL, '1', 1, '292987966847542104', '/96951079817586790/292987966847542104/932092514839301682', 3, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1022044156701543777', 2, '2171.02', '未交增值税', '应交税金_未交增值税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/1022044156701543777', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('406647911749579584', 2, '2171.03', '应交营业税', '应交税金_应交营业税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/406647911749579584', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('879600479065666260', 2, '2171.04', '应交消费税', '应交税金_应交消费税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/879600479065666260', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('235883972420633917', 2, '2171.05', '应交资源税', '应交税金_应交资源税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/235883972420633917', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('393328438253587284', 2, '2171.06', '应交所得税', '应交税金_应交所得税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/393328438253587284', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('114231781872905521', 2, '2171.07', '应交土地增值税', '应交税金_应交土地增值税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/114231781872905521', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1003574032079302900', 2, '2171.08', '应交城市维护建设税', '应交税金_应交城市维护建设税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/1003574032079302900', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('884766447552305', 2, '2171.09', '应交房产税', '应交税金_应交房产税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/884766447552305', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('40250506570057051', 2, '2171.10', '应交土地使用税', '应交税金_应交土地使用税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/40250506570057051', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('803885570271243375', 2, '2171.11', '应交车船使用税', '应交税金_应交车船使用税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/803885570271243375', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('882379226510337368', 2, '2171.12', '应交个人所得税', '应交税金_应交个人所得税', NULL, NULL, '2', 1, '96951079817586790', '/96951079817586790/882379226510337368', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('542274779277331862', 2, '2176', '其他应交款', '其他应交款', NULL, NULL, '2', 1, NULL, '/542274779277331862', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('619908440096929755', 2, '2181', '其他应付款', '其他应付款', NULL, NULL, '2', 1, NULL, '/619908440096929755', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('887772867679752126', 2, '2191', '预提费用', '预提费用', NULL, NULL, '2', 1, NULL, '/887772867679752126', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('201973909555745124', 2, '2201', '待转资产价值', '待转资产价值', NULL, NULL, '2', 1, NULL, '/201973909555745124', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('826172061537086101', 2, '2211', '预计负债', '预计负债', NULL, NULL, '2', 1, NULL, '/826172061537086101', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('68261178049352966', 2, '2301', '长期借款', '长期借款', NULL, NULL, '2', 1, NULL, '/68261178049352966', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('59984088919897402', 2, '2311', '应付债券', '应付债券', NULL, NULL, '2', 1, NULL, '/59984088919897402', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('42105070996828897', 2, '2311.01', '债券面值', '应付债券_债券面值', NULL, NULL, '2', 1, '59984088919897402', '/59984088919897402/42105070996828897', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('794920158122536429', 2, '2311.02', '债券溢价', '应付债券_债券溢价', NULL, NULL, '2', 1, '59984088919897402', '/59984088919897402/794920158122536429', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('45095165433927761', 2, '2311.03', '债券折价', '应付债券_债券折价', NULL, NULL, '2', 1, '59984088919897402', '/59984088919897402/45095165433927761', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('279250928995329465', 2, '2311.04', '应计利息', '应付债券_应计利息', NULL, NULL, '2', 1, '59984088919897402', '/59984088919897402/279250928995329465', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('838886264096380638', 2, '2321', '长期应付款', '长期应付款', NULL, NULL, '2', 1, NULL, '/838886264096380638', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('389346891240351083', 2, '2331', '专项应付款', '专项应付款', NULL, NULL, '2', 1, NULL, '/389346891240351083', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('826359925109945820', 2, '2341', '递延税款', '递延税款', NULL, NULL, '2', 1, NULL, '/826359925109945820', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('640060240148553910', 4, '3101', '实收资本（或股本）', '实收资本（或股本）', NULL, NULL, '2', 1, NULL, '/640060240148553910', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('798235802037518513', 4, '3103', '已归还投资', '已归还投资', NULL, NULL, '2', 1, NULL, '/798235802037518513', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('951740003061845428', 4, '3111', '资本公积', '资本公积', NULL, NULL, '2', 1, NULL, '/951740003061845428', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('63202687096166308', 4, '3111.01', '资本（或股本）溢价', '资本公积_资本（或股本）溢价', NULL, NULL, '2', 1, '951740003061845428', '/951740003061845428/63202687096166308', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('864936497147178794', 4, '3111.02', '接受捐赠非现金资产准备', '资本公积_接受捐赠非现金资产准备', NULL, NULL, '2', 1, '951740003061845428', '/951740003061845428/864936497147178794', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1041216905436846689', 4, '3111.03', '股权投资准备', '资本公积_股权投资准备', NULL, NULL, '2', 1, '951740003061845428', '/951740003061845428/1041216905436846689', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('996626674214948990', 4, '3111.04', '拨款转入', '资本公积_拨款转入', NULL, NULL, '2', 1, '951740003061845428', '/951740003061845428/996626674214948990', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('681100052712824600', 4, '3111.05', '外币资本折算差额', '资本公积_外币资本折算差额', NULL, NULL, '2', 1, '951740003061845428', '/951740003061845428/681100052712824600', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1020265364211707864', 4, '3111.06', '其他资本公积', '资本公积_其他资本公积', NULL, NULL, '2', 1, '951740003061845428', '/951740003061845428/1020265364211707864', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1101889306771228992', 4, '3121', '盈余公积', '盈余公积', NULL, NULL, '2', 1, NULL, '/1101889306771228992', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('83086721533288930', 4, '3121.01', '法定盈余公积', '盈余公积_法定盈余公积', NULL, NULL, '2', 1, '1101889306771228992', '/1101889306771228992/83086721533288930', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('78170157780708156', 4, '3121.02', '任意盈余公积', '盈余公积_任意盈余公积', NULL, NULL, '2', 1, '1101889306771228992', '/1101889306771228992/78170157780708156', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('848538037020021070', 4, '3121.03', '法定公益金', '盈余公积_法定公益金', NULL, NULL, '2', 1, '1101889306771228992', '/1101889306771228992/848538037020021070', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('168579916208710660', 4, '3121.04', '储备基金', '盈余公积_储备基金', NULL, NULL, '2', 1, '1101889306771228992', '/1101889306771228992/168579916208710660', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('540589499348035525', 4, '3121.05', '企业发展基金', '盈余公积_企业发展基金', NULL, NULL, '2', 1, '1101889306771228992', '/1101889306771228992/540589499348035525', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('231576612723686237', 4, '3121.06', '利润归还投资', '盈余公积_利润归还投资', NULL, NULL, '2', 1, '1101889306771228992', '/1101889306771228992/231576612723686237', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('572274181377404166', 4, '3131', '本年利润', '本年利润', NULL, NULL, '2', 1, NULL, '/572274181377404166', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('28007339279311329', 4, '3141', '利润分配', '利润分配', NULL, NULL, '2', 1, NULL, '/28007339279311329', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('496553850189175884', 4, '3141.01', '其他转入', '利润分配_其他转入', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/496553850189175884', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('803255703339779985', 4, '3141.02', '提取法定盈余公积', '利润分配_提取法定盈余公积', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/803255703339779985', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('284259471365528945', 4, '3141.03', '提取法定公益金', '利润分配_提取法定公益金', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/284259471365528945', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('493708624741820369', 4, '3141.04', '提取储备基金', '利润分配_提取储备基金', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/493708624741820369', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('348485841295896395', 4, '3141.05', '提取企业发展基金', '利润分配_提取企业发展基金', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/348485841295896395', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('81561895349512656', 4, '3141.06', '提取职工奖励及福利基金', '利润分配_提取职工奖励及福利基金', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/81561895349512656', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('317515074178595961', 4, '3141.07', '利润归还投资', '利润分配_利润归还投资', NULL, NULL, '2', 1, '28007339279311329', '/28007339279311329/317515074178595961', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('226331516683084395', 4, '3141.08', '应付优先股股利', '利润分配_应付优先股股利', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/226331516683084395', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('605040221811759178', 4, '3141.09', '提取任意盈余公积', '利润分配_提取任意盈余公积', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/605040221811759178', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('813561790874902306', 4, '3141.10', '应付普通股股利', '利润分配_应付普通股股利', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/813561790874902306', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('702791353909116178', 4, '3141.11', '转作资本（或股本）的普通股股利', '利润分配_转作资本（或股本）的普通股股利', NULL, NULL, '1', 1, '28007339279311329', '/28007339279311329/702791353909116178', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('386682563141079755', 4, '3141.15', '未分配利润', '利润分配_未分配利润', NULL, NULL, '2', 1, '28007339279311329', '/28007339279311329/386682563141079755', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('779896937110872293', 5, '4101', '生产成本', '生产成本', NULL, NULL, '1', 1, NULL, '/779896937110872293', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('340148518382832289', 5, '4101.01', '基本生产成本', '生产成本_基本生产成本', NULL, NULL, '1', 1, '779896937110872293', '/779896937110872293/340148518382832289', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('252300840306759134', 5, '4101.02', '辅助生产成本', '生产成本_辅助生产成本', NULL, NULL, '1', 1, '779896937110872293', '/779896937110872293/252300840306759134', 2, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('535174008067467091', 5, '4105', '制造费用', '制造费用', NULL, NULL, '1', 1, NULL, '/535174008067467091', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1063293514392596104', 5, '4107', '劳务成本', '劳务成本', NULL, NULL, '1', 1, NULL, '/1063293514392596104', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('17288891443017870', 6, '5101', '主营业务收入', '主营业务收入', NULL, NULL, '2', 1, NULL, '/17288891443017870', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('668732625523838423', 6, '5102', '其他业务收入', '其他业务收入', NULL, NULL, '2', 1, NULL, '/668732625523838423', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('507079449756950411', 6, '5201', '投资收益', '投资收益', NULL, NULL, '2', 1, NULL, '/507079449756950411', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('516977038799954516', 6, '5203', '补贴收入', '补贴收入', NULL, NULL, '2', 1, NULL, '/516977038799954516', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('48536106562198617', 6, '5301', '营业外收入', '营业外收入', NULL, NULL, '2', 1, NULL, '/48536106562198617', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('141481389665586615', 5, '5401', '主营业务成本', '主营业务成本', NULL, NULL, '1', 1, NULL, '/141481389665586615', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('962122181257865797', 5, '5402', '主营业务税金及附加', '主营业务税金及附加', NULL, NULL, '1', 1, NULL, '/962122181257865797', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1079231455663414878', 6, '5405', '其他业务支出', '其他业务支出', NULL, NULL, '1', 1, NULL, '/1079231455663414878', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1060754639412719740', 6, '5501', '营业费用', '营业费用', NULL, NULL, '1', 1, NULL, '/1060754639412719740', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('1138786612816752824', 6, '5502', '管理费用', '管理费用', NULL, NULL, '1', 1, NULL, '/1138786612816752824', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('558858936567245932', 6, '5503', '财务费用', '财务费用', NULL, NULL, '1', 1, NULL, '/558858936567245932', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('93830306306989817', 6, '5601', '营业外支出', '营业外支出', NULL, NULL, '1', 1, NULL, '/93830306306989817', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('568342351647826443', 6, '5701', '所得税', '所得税', NULL, NULL, '1', 1, NULL, '/568342351647826443', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');
INSERT INTO standard_subject VALUES ('428335267205474726', 6, '5801', '以前年度损益调整', '以前年度损益调整', NULL, NULL, '2', 1, NULL, '/428335267205474726', 1, 1, 0, NULL, NULL, '[]', NULL, NULL, NULL, '2', '1', '2026-08-27 02:20:34', '1', '2026-08-27 02:20:34', 'n');

SET FOREIGN_KEY_CHECKS = 1;

-- Default admin: username=admin password=maxkey (change after first login)
