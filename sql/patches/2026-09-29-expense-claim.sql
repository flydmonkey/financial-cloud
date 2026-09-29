-- 费用报销：报销单表 + 菜单（挂「凭证」组，幂等可重复执行）
CREATE TABLE IF NOT EXISTS `expense_claim` (
  `id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `book_id` varchar(45) COLLATE utf8mb4_bin NOT NULL,
  `claim_no` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '报销单号 BXyyyyMM-序号',
  `claimant` varchar(64) COLLATE utf8mb4_bin NOT NULL COMMENT '报销人',
  `claim_date` date NOT NULL COMMENT '报销日期',
  `expense_subject_code` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '费用科目编码',
  `expense_subject_name` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '费用科目名称（冗余）',
  `fund_subject_code` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '付款科目编码（库存现金/银行存款）',
  `fund_subject_name` varchar(128) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '付款科目名称（冗余）',
  `amount` decimal(18,2) NOT NULL COMMENT '报销金额',
  `summary` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '报销事由',
  `claim_status` varchar(16) COLLATE utf8mb4_bin NOT NULL DEFAULT 'draft' COMMENT 'draft/submitted/approved/rejected',
  `voucher_id` varchar(45) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '生成的凭证ID',
  `audit_by` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `reject_reason` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_by` varchar(45) DEFAULT NULL,
  `created_date` datetime DEFAULT NULL,
  `modified_by` varchar(45) DEFAULT NULL,
  `modified_date` datetime DEFAULT NULL,
  `deleted` varchar(1) DEFAULT 'n',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_expense_claim_no` (`book_id`,`claim_no`),
  KEY `idx_expense_claim_status` (`book_id`,`claim_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='费用报销单';

SET @voucher_grp_id = '1869692874272862209';
SET @exp_id = '2026092900000000061';
SET @exp_perm_id = '2026092900000000062';

DELETE FROM permission WHERE id = @exp_perm_id OR resource_id = @exp_id;
DELETE FROM resources WHERE id = @exp_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @exp_id,
    '费用报销',
    '费用报销',
    'MENU',
    @exp_id,
    '/expense/claim',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'tickets',
    'n',
    'n',
    'n',
    'y',
    @voucher_grp_id,
    '凭证',
    20,
    NULL,
    '1',
    NOW(),
    '1',
    NOW(),
    '1',
    'n'
);

INSERT INTO permission (
    id, role_id, resource_id, created_by, created_date, status, book_id
) VALUES (
    @exp_perm_id,
    'ROLE_ADMINISTRATORS',
    @exp_id,
    '1',
    NOW(),
    '1',
    '1'
);
