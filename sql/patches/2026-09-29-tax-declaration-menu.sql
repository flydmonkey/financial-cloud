-- 增值税申报表菜单（挂「报表」组，幂等可重复执行）
SET @report_id = '1886357455563137026';
SET @taxd_id = '2026092900000000051';
SET @taxd_perm_id = '2026092900000000052';

DELETE FROM permission WHERE id = @taxd_perm_id OR resource_id = @taxd_id;
DELETE FROM resources WHERE id = @taxd_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @taxd_id,
    '增值税申报表',
    '增值税申报表',
    'MENU',
    @taxd_id,
    '/statement/tax-declaration',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'document',
    'n',
    'n',
    'n',
    'y',
    @report_id,
    '报表',
    21,
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
    @taxd_perm_id,
    'ROLE_ADMINISTRATORS',
    @taxd_id,
    '1',
    NOW(),
    '1',
    '1'
);
