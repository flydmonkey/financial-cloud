-- Small-enterprise R&D is 4301; 5301 is non-operating income.
-- Repair only the inherited R&D binding, preserving other custom rules.
UPDATE standard_statement_rules
SET subject_code = '4301'
WHERE standard_id = '1' AND type = 'balance_sheet'
  AND item_code = '1211' AND subject_code = '5301';

UPDATE statement_rules r
JOIN book b ON b.id = r.book_id
SET r.subject_code = '4301'
WHERE b.standard_id = '1' AND r.type = 'balance_sheet'
  AND r.item_code = '1211' AND r.subject_code = '5301';
