-- 资产负债表往来科目重分类规则（小企业准则 standard_id=1）
-- 应收账款/预收款项、应付账款/预付款项 按借贷方余额分别取数

DELETE FROM `standard_statement_rules`
WHERE `standard_id` = '1'
  AND `type` = 'balance_sheet'
  AND `item_code` IN ('1105', '1107', '2105', '2107');

INSERT INTO `standard_statement_rules`
    (`id`, `standard_id`, `type`, `item_code`, `subject_code`, `rule`, `symbol`, `created_by`, `created_date`, `modified_by`, `modified_date`)
VALUES
    ('bs-rc-1105-1122-d', '1', 'balance_sheet', '1105', '1122', 'DEBIT_BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-rc-1105-2203-d', '1', 'balance_sheet', '1105', '2203', 'DEBIT_BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-rc-2107-2203-c', '1', 'balance_sheet', '2107', '2203', 'CREDIT_BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-rc-2107-1122-c', '1', 'balance_sheet', '2107', '1122', 'CREDIT_BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-rc-1107-1123-d', '1', 'balance_sheet', '1107', '1123', 'DEBIT_BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-rc-1107-2202-d', '1', 'balance_sheet', '1107', '2202', 'DEBIT_BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-rc-2105-2202-c', '1', 'balance_sheet', '2105', '2202', 'CREDIT_BALANCE', '+', '1', NOW(), '1', NOW()),
    ('bs-rc-2105-1123-c', '1', 'balance_sheet', '2105', '1123', 'CREDIT_BALANCE', '+', '1', NOW(), '1', NOW());

-- 已建账账套：同步重分类 rules（新建账套仍走 initBalanceSheet 复制模板）
DELETE sr FROM statement_rules sr
INNER JOIN book b ON sr.book_id = b.id
WHERE sr.type = 'balance_sheet'
  AND sr.item_code IN ('1105', '1107', '2105', '2107')
  AND b.standard_id = '1';

INSERT INTO statement_rules
    (id, book_id, type, item_code, subject_code, rule, symbol, created_by, created_date, modified_by, modified_date)
SELECT
    CONCAT('bsrc-', b.id, '-', s.item_code, '-', s.subject_code),
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
  AND s.item_code IN ('1105', '1107', '2105', '2107')
  AND b.standard_id = '1';
