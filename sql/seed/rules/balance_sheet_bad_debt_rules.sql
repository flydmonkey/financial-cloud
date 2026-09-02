-- 资产负债表应收账款扣减坏账准备（小企业准则 standard_id=1）
-- 应收账款 1105 = 1122 借方余额 + … − 1141 坏账准备

DELETE FROM `standard_statement_rules`
WHERE `standard_id` = '1'
  AND `type` = 'balance_sheet'
  AND `item_code` = '1105'
  AND `subject_code` = '1141';

INSERT INTO `standard_statement_rules`
    (`id`, `standard_id`, `type`, `item_code`, `subject_code`, `rule`, `symbol`, `created_by`, `created_date`, `modified_by`, `modified_date`)
VALUES
    ('bs-bd-1105-1141', '1', 'balance_sheet', '1105', '1141', 'BALANCE', '-', '1', NOW(), '1', NOW());

DELETE sr FROM statement_rules sr
INNER JOIN book b ON sr.book_id = b.id
WHERE sr.type = 'balance_sheet'
  AND sr.item_code = '1105'
  AND sr.subject_code = '1141'
  AND b.standard_id = '1';

INSERT INTO statement_rules
    (id, book_id, type, item_code, subject_code, rule, symbol, created_by, created_date, modified_by, modified_date)
SELECT
    CONCAT('bsbd-', b.id, '-1105-1141'),
    b.id,
    s.type,
    s.item_code,
    s.subject_code,
    s.rule,
    s.symbol,
    '1',
    NOW(),
    '1',
    NOW()
FROM book b
INNER JOIN standard_statement_rules s ON s.standard_id = b.standard_id
WHERE s.type = 'balance_sheet'
  AND s.item_code = '1105'
  AND s.subject_code = '1141'
  AND b.standard_id = '1';
