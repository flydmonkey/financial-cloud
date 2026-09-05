-- Patch: rbac-four-roles
-- Prefer: python tools/apply_rbac_four_roles.py
-- This file documents the intended seed; the Python tool is the source of truth for applying
-- role renames, soft-delete of legacy job posts, permission packs, and viewer backfill.

-- Expected product roles after apply:
--   ROLE_ADMINISTRATORS (1000) 管理员
--   ROLE_BOOKKEEPER (BOOKKEEPER) 做账员
--   ROLE_REVIEWER (REVIEWER) 审核员
--   ROLE_VIEWER (VIEWER) 查看员

-- Legacy soft-deleted: 单位员工/制单岗/复核岗/过账岗/出纳岗/会计主管
-- See openspec/changes/rbac-four-roles/migration-note.md
