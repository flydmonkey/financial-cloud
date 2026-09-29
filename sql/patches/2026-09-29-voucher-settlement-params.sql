-- 凭证与结账参数（挂「基础设置」）+ 往来软提示 config 键
SET @menu_id = '2026092900000000071';
SET @perm_admin = '2026092900000000072';
SET @perm_bk = '2026092900000000073';
SET @perm_rv = '2026092900000000074';

DELETE FROM permission WHERE id IN (@perm_admin, @perm_bk, @perm_rv) OR resource_id IN (@menu_id);
DELETE FROM resources WHERE id IN (@menu_id);

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @menu_id, '凭证与结账参数', '凭证与结账参数', 'MENU', @menu_id, '/config/voucher-settlement', 'GET',
    NULL, 'r', NULL, NULL, 'setting',
    'n', 'n', 'n', 'y',
    '1915219176348123138', '基础设置', 10, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES
    (@perm_admin, 'ROLE_ADMINISTRATORS', @menu_id, '1', NOW(), 1, '1'),
    (@perm_bk, 'ROLE_BOOKKEEPER', @menu_id, '1', NOW(), 1, '1'),
    (@perm_rv, 'ROLE_REVIEWER', @menu_id, '1', NOW(), 1, '1');

INSERT INTO `config` (`config_id`, `book_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `created_by`, `created_date`)
SELECT REPLACE(UUID(), '-', ''), 'template', '结账往来校验', 'settlement.verify.arap.enabled', 'true', 'y', '月结是否展示往来账龄软提示', '1', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `config` WHERE `book_id` = 'template' AND `config_key` = 'settlement.verify.arap.enabled'
);

INSERT INTO `config` (`config_id`, `book_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `created_by`, `created_date`)
SELECT REPLACE(UUID(), '-', ''), b.id, '结账往来校验', 'settlement.verify.arap.enabled', 'true', 'y', '月结是否展示往来账龄软提示', '1', NOW()
FROM `book` b
WHERE b.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM `config` c WHERE c.book_id = b.id AND c.config_key = 'settlement.verify.arap.enabled'
);
