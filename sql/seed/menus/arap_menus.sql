-- 往来 L1+L2 菜单（可重复执行）
-- component = request_url 去首斜杠 → views/arap/*.vue

SET @root_id = '1';
SET @arap_id = '2026090315000000001';
SET @arap_balance_id = '2026090315000000002';
SET @arap_detail_id = '2026090315000000003';
SET @arap_aging_id = '2026090315000000004';
SET @perm_parent = '2026090315000000011';
SET @perm_balance = '2026090315000000012';
SET @perm_detail = '2026090315000000013';
SET @perm_aging = '2026090315000000014';

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
)
SELECT
    @arap_id, '往来管理', 'fc.menu.arap', 'MENU', @arap_id, '',
    'GET', NULL, 'r', NULL, NULL, 'account-book',
    'n', 'n', 'n', 'y',
    @root_id, 'Financial Cloud', 8, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE id = @arap_id);

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
)
SELECT
    @arap_balance_id, '应收应付余额', 'fc.menu.arap.balance', 'MENU', @arap_balance_id, '/arap/balance',
    'GET', NULL, 'r', NULL, NULL, NULL,
    'n', 'n', 'n', 'y',
    @arap_id, '往来管理', 1, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE id = @arap_balance_id);

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
)
SELECT
    @arap_detail_id, '往来明细', 'fc.menu.arap.detail', 'MENU', @arap_detail_id, '/arap/detail',
    'GET', NULL, 'r', NULL, NULL, NULL,
    'n', 'n', 'n', 'y',
    @arap_id, '往来管理', 2, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE id = @arap_detail_id);

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
)
SELECT
    @arap_aging_id, '账龄分析', 'fc.menu.arap.aging', 'MENU', @arap_aging_id, '/arap/aging',
    'GET', NULL, 'r', NULL, NULL, NULL,
    'n', 'n', 'n', 'y',
    @arap_id, '往来管理', 3, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE id = @arap_aging_id);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_parent, 'ROLE_ADMINISTRATORS', @arap_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_parent);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_balance, 'ROLE_ADMINISTRATORS', @arap_balance_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_balance);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_detail, 'ROLE_ADMINISTRATORS', @arap_detail_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_detail);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_aging, 'ROLE_ADMINISTRATORS', @arap_aging_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_aging);
