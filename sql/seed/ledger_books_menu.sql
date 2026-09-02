-- 账簿顶级菜单 + 明细账/总账/科目余额表迁入（可重复执行）
-- 费用明细表不迁移，仍挂在报表下

SET @ledger_id = '2026082817000000001';
SET @permission_id = '2026082817000000002';
SET @root_id = '1';
SET @sub_ledger_id = '1903024792422047745';
SET @general_ledger_id = '2026082816300000001';
SET @subject_balance_id = '1886384516205912065';

-- 仅当账簿父菜单尚不存在时，将原 sort_index>=3 的顶级菜单整体 +1，为账簿腾出 sort=3
UPDATE resources
SET sort_index = sort_index + 1
WHERE parent_id = @root_id
  AND sort_index >= 3
  AND NOT EXISTS (
      SELECT 1 FROM (
          SELECT id FROM resources WHERE id = @ledger_id
      ) AS already
  );

DELETE FROM permission WHERE id = @permission_id OR resource_id = @ledger_id;
DELETE FROM resources WHERE id = @ledger_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @ledger_id,
    '账簿',
    'mxk.menu.ledgerBooks',
    'MENU',
    @ledger_id,
    '',
    'GET',
    NULL,
    'r',
    NULL,
    NULL,
    'account-book',
    'n',
    'n',
    'n',
    'y',
    @root_id,
    'JinBooks',
    3,
    NULL,
    '1',
    NOW(),
    '1',
    NOW(),
    '1',
    'n'
);

INSERT INTO permission (
    id, role_id, resource_id, created_by, created_date, status, book_id
) VALUES (
    @permission_id,
    'ROLE_ADMINISTRATORS',
    @ledger_id,
    '1',
    NOW(),
    1,
    '1'
);

-- 迁入子菜单（URL / resource id 不变）
UPDATE resources
SET parent_id = @ledger_id,
    parent_name = '账簿',
    sort_index = 1
WHERE id = @sub_ledger_id;

UPDATE resources
SET parent_id = @ledger_id,
    parent_name = '账簿',
    sort_index = 2
WHERE id = @general_ledger_id;

UPDATE resources
SET parent_id = @ledger_id,
    parent_name = '账簿',
    sort_index = 3
WHERE id = @subject_balance_id;

-- 顶级会计菜单聚拢（幂等绝对排序）：凭证 → 账簿 → 报表 → 结账 → 日记账 → 薪资
SET @voucher_id = '1869692874272862209';
SET @report_id = '1886357455563137026';
SET @settle_id = '1917420357065609218';
SET @journal_id = '1881534934875557889';
SET @payroll_id = '981334321270882304';

UPDATE resources SET sort_index = 2 WHERE id = @voucher_id;
UPDATE resources SET sort_index = 3 WHERE id = @ledger_id;
UPDATE resources SET sort_index = 4 WHERE id = @report_id;
UPDATE resources SET sort_index = 5 WHERE id = @settle_id;
UPDATE resources SET sort_index = 6 WHERE id = @journal_id;
UPDATE resources SET sort_index = 7 WHERE id = @payroll_id;
