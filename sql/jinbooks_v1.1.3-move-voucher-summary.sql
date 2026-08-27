-- 凭证汇总表从「报表」移至「凭证」菜单下
UPDATE `resources`
SET `parent_id` = '1869692874272862209',
    `parent_name` = '凭证',
    `sort_index` = 3
WHERE `id` = '1891486309700673537';
