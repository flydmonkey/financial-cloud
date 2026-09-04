-- 往来核销工作台菜单（可重复执行）
SET @arap_id = '2026090315000000001';
SET @arap_writeoff_id = '2026090315000000005';
SET @perm_writeoff = '2026090315000000015';

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
)
SELECT
    @arap_writeoff_id, '核销工作台', 'fc.menu.arap.writeoff', 'MENU', @arap_writeoff_id, '/arap/writeoff',
    'GET', NULL, 'r', NULL, NULL, NULL,
    'n', 'n', 'n', 'y',
    @arap_id, '往来管理', 4, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE id = @arap_writeoff_id);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_writeoff, 'ROLE_ADMINISTRATORS', @arap_writeoff_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_writeoff);
