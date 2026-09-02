-- 菜单微调（可重复执行）
-- 1) 社保公积金、个人税率设置 → 薪资
-- 2) 顶级「配置管理」改名为「系统设置」

SET @salary_id = '981334321270882304';
SET @insurance_id = '1889594633392771074';
SET @tax_rate_id = '1887317090379808769';
SET @config_id = '981334679749656576';

UPDATE resources
SET parent_id = @salary_id,
    parent_name = '薪资',
    sort_index = 8
WHERE id = @insurance_id;

UPDATE resources
SET parent_id = @salary_id,
    parent_name = '薪资',
    sort_index = 9
WHERE id = @tax_rate_id;

UPDATE resources
SET res_name = '系统设置',
    parent_name = CASE WHEN parent_id = '1' THEN parent_name ELSE parent_name END,
    i18n = 'mxk.menu.systemSettings'
WHERE id = @config_id;

-- 其直接子菜单的 parent_name 同步
UPDATE resources
SET parent_name = '系统设置'
WHERE parent_id = @config_id
  AND deleted = 'n';
