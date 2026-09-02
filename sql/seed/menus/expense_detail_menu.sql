-- 费用明细表菜单（财务报表子菜单），可重复执行
SET @resource_id = '2026082814300000001';
SET @permission_id = '2026082814300000002';
SET @parent_id = '1886357455563137026';

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
    '费用明细表',
    'mkt.menu.expenseDetail',
    'MENU',
    @resource_id,
    '/statement/expense-detail',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'wallet',
    'n',
    'n',
    'n',
    'y',
    @parent_id,
    '财务报表',
    10,
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
