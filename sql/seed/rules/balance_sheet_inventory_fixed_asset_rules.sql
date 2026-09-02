-- 资产负债表存货 / 固定资产取数规则（小企业准则 standard_id=1）
-- 存货：1403 + 1405 合并；固定资产：1601 - 1602 净值扣备抵

DELETE FROM `standard_statement_rules`
WHERE `standard_id` = '1'
  AND `type` = 'balance_sheet'
  AND `item_code` IN ('1110', '1206');

INSERT INTO `standard_statement_rules`
    (`id`, `standard_id`, `type`, `item_code`, `subject_code`, `rule`, `symbol`, `created_by`, `created_date`, `modified_by`, `modified_date`)
VALUES
    ('bs-inv-1110-1403', '1', 'balance_sheet', '1110', '1403', 'BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-inv-1110-1405', '1', 'balance_sheet', '1110', '1405', 'BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-fa-1206-1601', '1', 'balance_sheet', '1206', '1601', 'BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-fa-1206-1602', '1', 'balance_sheet', '1206', '1602', 'BALANCE', '-', '1', NOW(), '1', NOW());

-- 已建账账套：同步 rules
DELETE sr FROM statement_rules sr
INNER JOIN book b ON sr.book_id = b.id
WHERE sr.type = 'balance_sheet'
  AND sr.item_code IN ('1110', '1206')
  AND b.standard_id = '1';

INSERT INTO statement_rules
    (id, book_id, type, item_code, subject_code, rule, symbol, created_by, created_date, modified_by, modified_date)
SELECT
    CONCAT('bsif-', b.id, '-', s.item_code, '-', s.subject_code),
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
  AND s.item_code IN ('1110', '1206')
  AND b.standard_id = '1';
