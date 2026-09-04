-- Retire jt_zj: depreciation is handled by the fixed-asset accrue API / month-end wizard.
-- Soft-delete template headers and their items (standard catalog + books).

UPDATE voucher_template_item
SET deleted = 'y',
    modified_date = NOW()
WHERE deleted = 'n'
  AND template_id IN (
      SELECT id FROM voucher_template WHERE code = 'jt_zj'
  );

UPDATE voucher_template
SET deleted = 'y',
    modified_date = NOW()
WHERE code = 'jt_zj'
  AND deleted = 'n';
