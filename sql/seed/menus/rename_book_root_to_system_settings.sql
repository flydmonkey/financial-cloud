-- Rename top-level 账套管理 → 系统设置, sort after 准则管理 (same level).
-- book root: 981334814802051072
-- standards: 1915219176348123138 (sort_index=9)
-- old sys settings (hidden): 981334679749656576

UPDATE resources
SET res_name = '系统设置',
    sort_index = 10,
    modified_date = NOW()
WHERE id = '981334814802051072';

UPDATE resources
SET parent_name = '系统设置',
    modified_date = NOW()
WHERE parent_id = '981334814802051072';

-- Avoid duplicate visible name confusion for the old hidden root
UPDATE resources
SET res_name = '系统设置(已下线)',
    modified_date = NOW()
WHERE id = '981334679749656576';
