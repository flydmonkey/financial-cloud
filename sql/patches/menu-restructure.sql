-- 报表、结账与凭证同级（顶级菜单），不再嵌套在凭证下
UPDATE `resources`
SET `parent_id` = '1', `sort_index` = 6
WHERE `id` = '1886357455563137026';

UPDATE `resources`
SET `parent_id` = '1', `sort_index` = 5
WHERE `id` = '1917420357065609218';
