-- 总账菜单（账簿子菜单），可重复执行
SET @resource_id = '2026082816300000001';
SET @permission_id = '2026082816300000002';
SET @parent_id = '2026082817000000001';

DELETE FROM permission WHERE id = @permission_id OR resource_id = @resource_id;
DELETE FROM resources WHERE id = @resource_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @resource_id,
    '总账',
    'mkt.menu.generalLedger',
    'MENU',
    @resource_id,
    '/statement/general-ledger',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'book',
    'n',
    'n',
    'n',
    'y',
    @parent_id,
    '账簿',
    2,
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
    @permission_id,
    'ROLE_ADMINISTRATORS',
    @resource_id,
    '1',
    NOW(),
    1,
    '1'
);
