-- Rename 准则管理 → 基础设置; move init-balance / assist / cashflow menus under it.
-- Root: 1915219176348123138

UPDATE resources
SET res_name = '基础设置',
    i18n = '基础设置',
    modified_date = NOW()
WHERE id = '1915219176348123138';

UPDATE resources SET parent_id = '1915219176348123138', parent_name = '基础设置', sort_index = 6, modified_date = NOW()
WHERE id = '1899369820127911938'; -- 初始余额

UPDATE resources SET parent_id = '1915219176348123138', parent_name = '基础设置', sort_index = 7, modified_date = NOW()
WHERE id = '981623658751459329'; -- 辅助核算

UPDATE resources SET parent_id = '1915219176348123138', parent_name = '基础设置', sort_index = 8, modified_date = NOW()
WHERE id = '1902625741973843969'; -- 现金流量初始余额

UPDATE resources SET parent_id = '1915219176348123138', parent_name = '基础设置', sort_index = 9, modified_date = NOW()
WHERE id = '1913072049310191618'; -- 科目现金流量项配置

-- Bookkeeper / reviewer need parent menu to see moved children
INSERT INTO permission (id, role_id, resource_id, created_by, status, book_id)
SELECT 'baseBkStdRoot01', 'ROLE_BOOKKEEPER', '1915219176348123138', '1', 1, '1'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM permission WHERE role_id='ROLE_BOOKKEEPER' AND resource_id='1915219176348123138' AND status=1
);

INSERT INTO permission (id, role_id, resource_id, created_by, status, book_id)
SELECT 'baseRvStdRoot01', 'ROLE_REVIEWER', '1915219176348123138', '1', 1, '1'
FROM DUAL WHERE NOT EXISTS (
  SELECT 1 FROM permission WHERE role_id='ROLE_REVIEWER' AND resource_id='1915219176348123138' AND status=1
);
