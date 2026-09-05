-- 已有库补丁：工资个税（type=0）税率档改为国家年度累计预扣法区间
-- 与 CumulativePitRules 语义一致；劳务报酬 type=1 不变。
-- 可重复执行：按 id 更新；第 8 档旧月度行软删。

UPDATE `config_personal_tax` SET
  `level` = 1, `min_num` = 0, `max_num` = 36000, `tax_rate` = 3, `calculation_deduction` = 0.00,
  `modified_by` = '1', `modified_date` = NOW(), `deleted` = 'n'
WHERE `id` = '1887760257594204161' AND `type` = 0;

UPDATE `config_personal_tax` SET
  `level` = 2, `min_num` = 36000, `max_num` = 144000, `tax_rate` = 10, `calculation_deduction` = 2520.00,
  `modified_by` = '1', `modified_date` = NOW(), `deleted` = 'n'
WHERE `id` = '1887761326340612097' AND `type` = 0;

UPDATE `config_personal_tax` SET
  `level` = 3, `min_num` = 144000, `max_num` = 300000, `tax_rate` = 20, `calculation_deduction` = 16920.00,
  `modified_by` = '1', `modified_date` = NOW(), `deleted` = 'n'
WHERE `id` = '1887762268276432897' AND `type` = 0;

UPDATE `config_personal_tax` SET
  `level` = 4, `min_num` = 300000, `max_num` = 420000, `tax_rate` = 25, `calculation_deduction` = 31920.00,
  `modified_by` = '1', `modified_date` = NOW(), `deleted` = 'n'
WHERE `id` = '1887795119336226817' AND `type` = 0;

UPDATE `config_personal_tax` SET
  `level` = 5, `min_num` = 420000, `max_num` = 660000, `tax_rate` = 30, `calculation_deduction` = 52920.00,
  `modified_by` = '1', `modified_date` = NOW(), `deleted` = 'n'
WHERE `id` = '1888054950085365761' AND `type` = 0;

UPDATE `config_personal_tax` SET
  `level` = 6, `min_num` = 660000, `max_num` = 960000, `tax_rate` = 35, `calculation_deduction` = 85920.00,
  `modified_by` = '1', `modified_date` = NOW(), `deleted` = 'n'
WHERE `id` = '1888055058986274817' AND `type` = 0;

UPDATE `config_personal_tax` SET
  `level` = 7, `min_num` = 960000, `max_num` = NULL, `tax_rate` = 45, `calculation_deduction` = 181920.00,
  `modified_by` = '1', `modified_date` = NOW(), `deleted` = 'n'
WHERE `id` = '1888076044448079873' AND `type` = 0;

-- 旧 seed 第 8 档（月度 85000+）软删
UPDATE `config_personal_tax` SET
  `deleted` = 'y', `modified_by` = '1', `modified_date` = NOW()
WHERE `id` = '1897581567807631362' AND `type` = 0 AND `deleted` = 'n';

-- 兜底：任意仍活跃的 type=0 且 max_num <= 85000（典型旧月度档）软删，避免漏 id 的脏数据
UPDATE `config_personal_tax` SET
  `deleted` = 'y', `modified_by` = '1', `modified_date` = NOW()
WHERE `type` = 0
  AND `deleted` = 'n'
  AND `id` NOT IN (
    '1887760257594204161', '1887761326340612097', '1887762268276432897',
    '1887795119336226817', '1888054950085365761', '1888055058986274817',
    '1888076044448079873'
  )
  AND (`max_num` IS NULL OR `max_num` <= 85000);
