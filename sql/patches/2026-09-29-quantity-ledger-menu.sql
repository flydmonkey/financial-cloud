-- 数量金额账菜单（挂「账簿」组，幂等可重复执行）
SET @ledger_id = '2026082817000000001';
SET @qal_id = '2026092900000000021';
SET @qal_perm_id = '2026092900000000022';

DELETE FROM permission WHERE id = @qal_perm_id OR resource_id = @qal_id;
DELETE FROM resources WHERE id = @qal_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @qal_id,
    '数量金额账',
    '数量金额账',
    'MENU',
    @qal_id,
    '/voucher/quantity-ledger',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'menus-wanglaimingxizhang',
    'n',
    'n',
    'n',
    'y',
    @ledger_id,
    '账簿',
    5,
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
    @qal_perm_id,
    'ROLE_ADMINISTRATORS',
    @qal_id,
    '1',
    NOW(),
    1,
    '1'
);
