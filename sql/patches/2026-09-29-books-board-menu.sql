-- 代账工作台（多账套月末看板）菜单；初版挂根节点。后续由 2026-09-30-menu-dashboard-nest-icons.sql 挂到「仪表盘」下。幂等可重复执行
SET @root_id = '1';
SET @menu_id = '2026092900000000081';
SET @perm_admin = '2026092900000000082';
SET @perm_bk = '2026092900000000083';
SET @perm_rv = '2026092900000000084';
SET @perm_vw = '2026092900000000085';

DELETE FROM permission WHERE id IN (@perm_admin, @perm_bk, @perm_rv, @perm_vw) OR resource_id = @menu_id;
DELETE FROM resources WHERE id = @menu_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @menu_id,
    '代账工作台',
    '代账工作台',
    'MENU',
    @menu_id,
    '/workspace/books-board',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'dashboard',
    'n',
    'n',
    'n',
    'y',
    @root_id,
    'Financial Cloud',
    2,
    '多账套月末进度与批量交账',
    '1',
    NOW(),
    '1',
    NOW(),
    '1',
    'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES
    (@perm_admin, 'ROLE_ADMINISTRATORS', @menu_id, '1', NOW(), 1, '1'),
    (@perm_bk, 'ROLE_BOOKKEEPER', @menu_id, '1', NOW(), 1, '1'),
    (@perm_rv, 'ROLE_REVIEWER', @menu_id, '1', NOW(), 1, '1'),
    (@perm_vw, 'ROLE_VIEWER', @menu_id, '1', NOW(), 1, '1');
