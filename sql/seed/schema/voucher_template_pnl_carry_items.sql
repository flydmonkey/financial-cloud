-- Rebuild P&L carry template items by accounting standard of the book / standard.
-- standard_id=1 小企业会计准则；standard_id=2 企业会计制度
-- voucher_template.related_id is bookId or standardId for template catalogs.

-- Remove previously seeded CAS-style items (6401/6001/4103) from carry templates
DELETE FROM voucher_template_item
WHERE template_id IN (SELECT id FROM voucher_template WHERE code IN ('qm_jz_cbfy', 'qm_jz_sr') AND deleted = 'n')
  AND subject_code IN (
    '6001','6051','6301','6401','6405','6601','6602','6603','6711','4103','410406',
    '5001','5051','5111','5301','5401','5402','5601','5602','5603','5711','3103','3104.02',
    '5101','5102','5201','5203','5405','5501','5502','5503','3131','3141.15','5701','5801'
  )
  AND (id LIKE 'cbfy_%' OR id LIKE 'sr_%' OR summary IN ('结转成本费用', '结转收入'));

-- Helper view via joins: resolve standard_id for each template
-- related_id may be book.id or standard.id

-- ===== 小企业会计准则 (standard 1) — cost =====
INSERT INTO voucher_template_item (
  id, summary, direction, subject_code, selected_value, related_id, template_id,
  created_by, created_date, modified_by, modified_date, deleted
)
SELECT
  CONCAT('cbfy1_', t.related_id, '_', s.code),
  '结转成本费用',
  s.direction,
  s.code,
  NULL,
  t.related_id,
  t.id,
  '1', NOW(), '1', NOW(), 'n'
FROM voucher_template t
JOIN book b ON b.id = t.related_id AND b.standard_id = '1' AND b.deleted = 'n'
JOIN (
  SELECT '5401' AS code, '2' AS direction UNION ALL
  SELECT '5402', '2' UNION ALL
  SELECT '5601', '2' UNION ALL
  SELECT '5602', '2' UNION ALL
  SELECT '5603', '2' UNION ALL
  SELECT '5711', '2' UNION ALL
  SELECT '3103', '1'
) s
WHERE t.code = 'qm_jz_cbfy' AND t.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_template_item i
    WHERE i.template_id = t.id AND i.subject_code = s.code AND i.deleted = 'n'
  );

-- standard catalog related_id='1'
INSERT INTO voucher_template_item (
  id, summary, direction, subject_code, selected_value, related_id, template_id,
  created_by, created_date, modified_by, modified_date, deleted
)
SELECT
  CONCAT('cbfy1s_', t.id, '_', s.code),
  '结转成本费用',
  s.direction,
  s.code,
  NULL,
  t.related_id,
  t.id,
  '1', NOW(), '1', NOW(), 'n'
FROM voucher_template t
JOIN (
  SELECT '5401' AS code, '2' AS direction UNION ALL
  SELECT '5402', '2' UNION ALL
  SELECT '5601', '2' UNION ALL
  SELECT '5602', '2' UNION ALL
  SELECT '5603', '2' UNION ALL
  SELECT '5711', '2' UNION ALL
  SELECT '3103', '1'
) s
WHERE t.code = 'qm_jz_cbfy' AND t.related_id = '1' AND t.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_template_item i
    WHERE i.template_id = t.id AND i.subject_code = s.code AND i.deleted = 'n'
  );

-- ===== 小企业 — income =====
INSERT INTO voucher_template_item (
  id, summary, direction, subject_code, selected_value, related_id, template_id,
  created_by, created_date, modified_by, modified_date, deleted
)
SELECT
  CONCAT('sr1_', t.related_id, '_', s.code),
  '结转收入',
  s.direction,
  s.code,
  NULL,
  t.related_id,
  t.id,
  '1', NOW(), '1', NOW(), 'n'
FROM voucher_template t
JOIN book b ON b.id = t.related_id AND b.standard_id = '1' AND b.deleted = 'n'
JOIN (
  SELECT '5001' AS code, '1' AS direction UNION ALL
  SELECT '5051', '1' UNION ALL
  SELECT '5111', '1' UNION ALL
  SELECT '5301', '1' UNION ALL
  SELECT '3103', '2'
) s
WHERE t.code = 'qm_jz_sr' AND t.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_template_item i
    WHERE i.template_id = t.id AND i.subject_code = s.code AND i.deleted = 'n'
  );

INSERT INTO voucher_template_item (
  id, summary, direction, subject_code, selected_value, related_id, template_id,
  created_by, created_date, modified_by, modified_date, deleted
)
SELECT
  CONCAT('sr1s_', t.id, '_', s.code),
  '结转收入',
  s.direction,
  s.code,
  NULL,
  t.related_id,
  t.id,
  '1', NOW(), '1', NOW(), 'n'
FROM voucher_template t
JOIN (
  SELECT '5001' AS code, '1' AS direction UNION ALL
  SELECT '5051', '1' UNION ALL
  SELECT '5111', '1' UNION ALL
  SELECT '5301', '1' UNION ALL
  SELECT '3103', '2'
) s
WHERE t.code = 'qm_jz_sr' AND t.related_id = '1' AND t.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_template_item i
    WHERE i.template_id = t.id AND i.subject_code = s.code AND i.deleted = 'n'
  );

-- ===== 企业会计制度 (standard 2) — cost =====
INSERT INTO voucher_template_item (
  id, summary, direction, subject_code, selected_value, related_id, template_id,
  created_by, created_date, modified_by, modified_date, deleted
)
SELECT
  CONCAT('cbfy2_', t.related_id, '_', s.code),
  '结转成本费用',
  s.direction,
  s.code,
  NULL,
  t.related_id,
  t.id,
  '1', NOW(), '1', NOW(), 'n'
FROM voucher_template t
JOIN book b ON b.id = t.related_id AND b.standard_id = '2' AND b.deleted = 'n'
JOIN (
  SELECT '5401' AS code, '2' AS direction UNION ALL
  SELECT '5405', '2' UNION ALL
  SELECT '5501', '2' UNION ALL
  SELECT '5502', '2' UNION ALL
  SELECT '5503', '2' UNION ALL
  SELECT '5601', '2' UNION ALL
  SELECT '3131', '1'
) s
WHERE t.code = 'qm_jz_cbfy' AND t.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_template_item i
    WHERE i.template_id = t.id AND i.subject_code = s.code AND i.deleted = 'n'
  );

INSERT INTO voucher_template_item (
  id, summary, direction, subject_code, selected_value, related_id, template_id,
  created_by, created_date, modified_by, modified_date, deleted
)
SELECT
  CONCAT('cbfy2s_', t.id, '_', s.code),
  '结转成本费用',
  s.direction,
  s.code,
  NULL,
  t.related_id,
  t.id,
  '1', NOW(), '1', NOW(), 'n'
FROM voucher_template t
JOIN (
  SELECT '5401' AS code, '2' AS direction UNION ALL
  SELECT '5405', '2' UNION ALL
  SELECT '5501', '2' UNION ALL
  SELECT '5502', '2' UNION ALL
  SELECT '5503', '2' UNION ALL
  SELECT '5601', '2' UNION ALL
  SELECT '3131', '1'
) s
WHERE t.code = 'qm_jz_cbfy' AND t.related_id = '2' AND t.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_template_item i
    WHERE i.template_id = t.id AND i.subject_code = s.code AND i.deleted = 'n'
  );

-- ===== 企业会计制度 — income =====
INSERT INTO voucher_template_item (
  id, summary, direction, subject_code, selected_value, related_id, template_id,
  created_by, created_date, modified_by, modified_date, deleted
)
SELECT
  CONCAT('sr2_', t.related_id, '_', s.code),
  '结转收入',
  s.direction,
  s.code,
  NULL,
  t.related_id,
  t.id,
  '1', NOW(), '1', NOW(), 'n'
FROM voucher_template t
JOIN book b ON b.id = t.related_id AND b.standard_id = '2' AND b.deleted = 'n'
JOIN (
  SELECT '5101' AS code, '1' AS direction UNION ALL
  SELECT '5102', '1' UNION ALL
  SELECT '5201', '1' UNION ALL
  SELECT '5203', '1' UNION ALL
  SELECT '5301', '1' UNION ALL
  SELECT '3131', '2'
) s
WHERE t.code = 'qm_jz_sr' AND t.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_template_item i
    WHERE i.template_id = t.id AND i.subject_code = s.code AND i.deleted = 'n'
  );

INSERT INTO voucher_template_item (
  id, summary, direction, subject_code, selected_value, related_id, template_id,
  created_by, created_date, modified_by, modified_date, deleted
)
SELECT
  CONCAT('sr2s_', t.id, '_', s.code),
  '结转收入',
  s.direction,
  s.code,
  NULL,
  t.related_id,
  t.id,
  '1', NOW(), '1', NOW(), 'n'
FROM voucher_template t
JOIN (
  SELECT '5101' AS code, '1' AS direction UNION ALL
  SELECT '5102', '1' UNION ALL
  SELECT '5201', '1' UNION ALL
  SELECT '5203', '1' UNION ALL
  SELECT '5301', '1' UNION ALL
  SELECT '3131', '2'
) s
WHERE t.code = 'qm_jz_sr' AND t.related_id = '2' AND t.deleted = 'n'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_template_item i
    WHERE i.template_id = t.id AND i.subject_code = s.code AND i.deleted = 'n'
  );
