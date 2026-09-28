-- 税费测算菜单（挂「报表」组，幂等可重复执行）
SET @report_id = '1886357455563137026';
SET @tax_id = '2026092900000000031';
SET @tax_perm_id = '2026092900000000032';

DELETE FROM permission WHERE id = @tax_perm_id OR resource_id = @tax_id;
DELETE FROM resources WHERE id = @tax_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @tax_id,
    '税费测算',
    '税费测算',
    'MENU',
    @tax_id,
    '/statement/tax-estimate',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'calculator',
    'n',
    'n',
    'n',
    'y',
    @report_id,
    '报表',
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
    @tax_perm_id,
    'ROLE_ADMINISTRATORS',
    @tax_id,
    '1',
    NOW(),
    1,
    '1'
);
