-- Move 角色管理 under 账套管理; hide 系统设置 menu root.
-- IDs: book=981334814802051072, role=981335810039087104, sys=981334679749656576

UPDATE resources
SET parent_id = '981334814802051072',
    parent_name = '账套管理',
    sort_index = 8,
    status = '1',
    deleted = 'n',
    is_visible = 'y',
    modified_date = NOW()
WHERE id = '981335810039087104';

UPDATE resources
SET status = '0',
    modified_date = NOW()
WHERE id = '981334679749656576'
   OR parent_id = '981334679749656576';

DELETE FROM permission
WHERE resource_id = '981334679749656576';

INSERT INTO permission (id, role_id, resource_id, created_by, status, book_id)
SELECT 'moveRoleMgmtAdm', 'ROLE_ADMINISTRATORS', '981335810039087104', '1', 1, '1'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM permission
  WHERE role_id = 'ROLE_ADMINISTRATORS' AND resource_id = '981335810039087104'
);
