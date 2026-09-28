-- 资产盘点菜单（挂「固定资产」组，可重复执行）
SET @check_id = '2026092900000000011';
SET @check_perm = '2026092900000000012';

DELETE FROM permission WHERE id IN (@check_perm) OR resource_id IN (@check_id);
DELETE FROM resources WHERE id IN (@check_id);

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @check_id, '资产盘点', '资产盘点', 'MENU', @check_id, '/fixed-asset/check', 'GET',
    NULL, 'r', NULL, NULL, 'aim',
    'n', 'n', 'n', 'y',
    '2026082818000000001', '固定资产', 7, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES (@check_perm, 'ROLE_ADMINISTRATORS', @check_id, '1', NOW(), 1, '1');
