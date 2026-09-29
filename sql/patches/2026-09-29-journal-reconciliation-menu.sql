-- 银行对账菜单（挂「出纳」组，幂等可重复执行）
SET @journal_id = '1881534934875557889';
SET @recon_id = '2026092900000000041';
SET @recon_perm_id = '2026092900000000042';

DELETE FROM permission WHERE id = @recon_perm_id OR resource_id = @recon_id;
DELETE FROM resources WHERE id = @recon_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @recon_id,
    '银行对账',
    '银行对账',
    'MENU',
    @recon_id,
    '/journal/reconciliation',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'menus-yinhangduizhang',
    'n',
    'n',
    'n',
    'y',
    @journal_id,
    '出纳',
    4,
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
    @recon_perm_id,
    'ROLE_ADMINISTRATORS',
    @recon_id,
    '1',
    NOW(),
    1,
    '1'
);
