-- 仪表盘嵌套（首页 + 代账工作台）+ 功能图标对齐 + 空壳菜单清理（幂等可重复执行）
-- Spec: docs/superpowers/specs/2026-09-30-menu-dashboard-nest-icons-design.md

SET @root_id = '1';
SET @dashboard_id = '981331493802475520';
SET @home_id = '2026093000000000001';
SET @books_board_id = '2026092900000000081';
SET @perm_home_admin = '2026093000000000002';
SET @perm_home_bk = '2026093000000000003';
SET @perm_home_rv = '2026093000000000004';
SET @perm_home_vw = '2026093000000000005';

-- 1) 仪表盘改为分组壳
UPDATE resources
SET request_url = '',
    res_style = 'dashboard',
    icon = NULL,
    sort_index = 1,
    modified_by = '1',
    modified_date = NOW()
WHERE id = @dashboard_id
  AND deleted = 'n';

-- 2) 首页（原 /index）
INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
)
SELECT
    @home_id, '首页', '首页', 'MENU', @home_id, '/index',
    'GET', NULL, 'r', NULL, NULL, 'home',
    'n', 'n', 'n', 'y',
    @dashboard_id, '仪表盘', 1, '经营看板首页',
    '1', NOW(), '1', NOW(), '1', 'n'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE id = @home_id);

UPDATE resources
SET res_name = '首页',
    i18n = '首页',
    request_url = '/index',
    res_style = 'home',
    icon = NULL,
    parent_id = @dashboard_id,
    parent_name = '仪表盘',
    sort_index = 1,
    is_visible = 'y',
    status = '1',
    deleted = 'n',
    modified_by = '1',
    modified_date = NOW()
WHERE id = @home_id;

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_home_admin, 'ROLE_ADMINISTRATORS', @home_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_home_admin);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_home_bk, 'ROLE_BOOKKEEPER', @home_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_home_bk);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_home_rv, 'ROLE_REVIEWER', @home_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_home_rv);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_home_vw, 'ROLE_VIEWER', @home_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_home_vw);

-- 3) 代账工作台挂到仪表盘下
UPDATE resources
SET parent_id = @dashboard_id,
    parent_name = '仪表盘',
    sort_index = 2,
    res_style = 'monitor',
    icon = NULL,
    request_url = '/workspace/books-board',
    is_visible = 'y',
    status = '1',
    deleted = 'n',
    modified_by = '1',
    modified_date = NOW()
WHERE id = @books_board_id
  AND deleted = 'n';

-- 4) 功能图标对齐（仅缺口 / 纠语义）
UPDATE resources SET res_style = 'swap', icon = NULL WHERE id = '2026090315000000001'; -- 往来管理
UPDATE resources SET res_style = 'account-book', icon = NULL WHERE id = '2026090315000000002'; -- 应收应付余额
UPDATE resources SET res_style = 'file-text', icon = NULL WHERE id = '2026090315000000003'; -- 往来明细
UPDATE resources SET res_style = 'bar-chart', icon = NULL WHERE id = '2026090315000000004'; -- 账龄分析
UPDATE resources SET res_style = 'check-circle', icon = NULL WHERE id = '2026090315000000005'; -- 核销工作台
UPDATE resources SET res_style = 'history', icon = NULL WHERE id = '981334866064834560'; -- 日志审计
UPDATE resources SET res_style = 'audit', icon = NULL WHERE id = '981337003041751040'; -- 登录日志
UPDATE resources SET res_style = 'audit', icon = NULL WHERE id = '981337181773627392'; -- 系统日志
UPDATE resources SET res_style = 'cluster', icon = NULL WHERE id = '981335709019275264'; -- 组织
UPDATE resources SET res_style = 'user', icon = NULL WHERE id = '981335758977630208'; -- 用户管理
UPDATE resources SET res_style = 'group', icon = NULL WHERE id = '981335810039087104'; -- 角色管理
UPDATE resources SET res_style = 'read', icon = NULL WHERE id = '981337246718230528'; -- 资源管理
UPDATE resources SET res_style = 'carry-out', icon = NULL WHERE id = '981337555771326464'; -- 权限分配
UPDATE resources SET res_style = 'eye', icon = NULL WHERE id = '981336054843834368'; -- 会话
UPDATE resources SET res_style = 'mail', icon = NULL WHERE id = '981336354157756416'; -- 电子邮箱
UPDATE resources SET res_style = 'send', icon = NULL WHERE id = '981336403415662592'; -- 短信服务
UPDATE resources SET res_style = 'file-protect', icon = NULL WHERE id = '981336473196298240'; -- 登录策略
UPDATE resources SET res_style = 'file-protect', icon = NULL WHERE id = '981336523834130432'; -- 密码策略

-- 5) 空壳菜单清理（须在仪表盘子菜单挂好之后）
DELETE FROM permission
WHERE resource_id IN (
    SELECT id FROM (
        SELECT r.id
        FROM resources r
        WHERE r.deleted = 'n'
          AND r.status = '1'
          AND r.classify = 'MENU'
          AND r.is_visible = 'y'
          AND (r.request_url IS NULL OR r.request_url IN ('', '#', '/'))
          AND NOT EXISTS (
              SELECT 1 FROM resources c
              WHERE c.parent_id = r.id
                AND c.deleted = 'n'
                AND c.status = '1'
                AND c.classify = 'MENU'
                AND c.is_visible = 'y'
          )
    ) t
);

DELETE FROM resources
WHERE id IN (
    SELECT id FROM (
        SELECT r.id
        FROM resources r
        WHERE r.deleted = 'n'
          AND r.status = '1'
          AND r.classify = 'MENU'
          AND r.is_visible = 'y'
          AND (r.request_url IS NULL OR r.request_url IN ('', '#', '/'))
          AND NOT EXISTS (
              SELECT 1 FROM resources c
              WHERE c.parent_id = r.id
                AND c.deleted = 'n'
                AND c.status = '1'
                AND c.classify = 'MENU'
                AND c.is_visible = 'y'
          )
    ) t
);
