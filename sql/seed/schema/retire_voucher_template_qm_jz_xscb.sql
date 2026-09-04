-- Retire qm_jz_xscb: P&L cost close is covered by qm_jz_cbfy (incl. 主营业务成本).
-- Soft-delete template headers and their items everywhere (standard catalog + books).

UPDATE voucher_template_item
SET deleted = 'y',
    modified_date = NOW()
WHERE deleted = 'n'
  AND template_id IN (
      SELECT id FROM voucher_template WHERE code = 'qm_jz_xscb'
  );

UPDATE voucher_template
SET deleted = 'y',
    modified_date = NOW()
WHERE code = 'qm_jz_xscb'
  AND deleted = 'n';
