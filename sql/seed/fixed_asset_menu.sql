-- 固定资产顶级菜单 + 卡片 / 资产类别 / 计提折旧（可重复执行）
SET @root_id = '1';
SET @parent_id = '2026082818000000001';
SET @parent_perm = '2026082818000000002';
SET @card_id = '2026082818000000011';
SET @card_perm = '2026082818000000012';
SET @category_id = '2026082818000000021';
SET @category_perm = '2026082818000000022';
SET @depr_id = '2026082818000000031';
SET @depr_perm = '2026082818000000032';
SET @detail_id = '2026082818000000041';
SET @detail_perm = '2026082818000000042';
SET @summary_id = '2026082818000000051';
SET @summary_perm = '2026082818000000052';
SET @change_id = '2026082818000000061';
SET @change_perm = '2026082818000000062';

DELETE FROM permission WHERE id IN (@parent_perm, @card_perm, @category_perm, @depr_perm, @detail_perm, @summary_perm, @change_perm)
   OR resource_id IN (@parent_id, @card_id, @category_id, @depr_id, @detail_id, @summary_id, @change_id);
DELETE FROM resources WHERE id IN (@parent_id, @card_id, @category_id, @depr_id, @detail_id, @summary_id, @change_id);

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @parent_id, '固定资产', 'mkt.menu.fixedAsset', 'MENU', @parent_id, '', 'GET',
    NULL, 'r', NULL, NULL, 'build',
    'n', 'n', 'n', 'y',
    @root_id, 'JinBooks', 7, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES (@parent_perm, 'ROLE_ADMINISTRATORS', @parent_id, '1', NOW(), 1, '1');

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @card_id, '卡片', 'mkt.menu.fixedAssetCard', 'MENU', @card_id, '/fixed-asset/card', 'GET',
    NULL, 'r', NULL, NULL, 'idcard',
    'n', 'n', 'n', 'y',
    @parent_id, '固定资产', 1, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES (@card_perm, 'ROLE_ADMINISTRATORS', @card_id, '1', NOW(), 1, '1');

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @category_id, '资产类别', 'mkt.menu.fixedAssetCategory', 'MENU', @category_id, '/fixed-asset/category', 'GET',
    NULL, 'r', NULL, NULL, 'appstore',
    'n', 'n', 'n', 'y',
    @parent_id, '固定资产', 2, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES (@category_perm, 'ROLE_ADMINISTRATORS', @category_id, '1', NOW(), 1, '1');

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @depr_id, '计提折旧', 'mkt.menu.fixedAssetDepreciation', 'MENU', @depr_id, '/fixed-asset/depreciation', 'GET',
    NULL, 'r', NULL, NULL, 'calculator',
    'n', 'n', 'n', 'y',
    @parent_id, '固定资产', 3, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES (@depr_perm, 'ROLE_ADMINISTRATORS', @depr_id, '1', NOW(), 1, '1');

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @detail_id, '折旧明细表', 'mkt.menu.fixedAssetDeprDetail', 'MENU', @detail_id, '/fixed-asset/depreciation-detail', 'GET',
    NULL, 'r', NULL, NULL, 'unordered-list',
    'n', 'n', 'n', 'y',
    @parent_id, '固定资产', 4, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES (@detail_perm, 'ROLE_ADMINISTRATORS', @detail_id, '1', NOW(), 1, '1');

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @summary_id, '折旧汇总表', 'mkt.menu.fixedAssetDeprSummary', 'MENU', @summary_id, '/fixed-asset/depreciation-summary', 'GET',
    NULL, 'r', NULL, NULL, 'bar-chart',
    'n', 'n', 'n', 'y',
    @parent_id, '固定资产', 5, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES (@summary_perm, 'ROLE_ADMINISTRATORS', @summary_id, '1', NOW(), 1, '1');

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @change_id, '资产变动记录', 'mkt.menu.fixedAssetChange', 'MENU', @change_id, '/fixed-asset/change-log', 'GET',
    NULL, 'r', NULL, NULL, 'swap',
    'n', 'n', 'n', 'y',
    @parent_id, '固定资产', 6, NULL,
    '1', NOW(), '1', NOW(), '1', 'n'
);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
VALUES (@change_perm, 'ROLE_ADMINISTRATORS', @change_id, '1', NOW(), 1, '1');
