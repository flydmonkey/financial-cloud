# Migration: legacy job-post roles → four product roles

## Target catalog

| role_id | role_code | role_name | Notes |
|---------|-----------|-----------|-------|
| `ROLE_ADMINISTRATORS` | `1000` | 管理员 | Keep id; rename display from 系统管理员组 |
| `ROLE_BOOKKEEPER` | `BOOKKEEPER` | 做账员 | New product role |
| `ROLE_REVIEWER` | `REVIEWER` | 审核员 | New product role |
| `ROLE_VIEWER` | `VIEWER` | 查看员 | New product role |

## Legacy mapping

| Legacy id | Legacy name | Maps to |
|-----------|-------------|---------|
| `1880191154616516610` (2001 制单岗) | 制单岗 | `ROLE_BOOKKEEPER` |
| `1880191529151086594` (4001 过账岗) | 过账岗 | `ROLE_BOOKKEEPER` |
| `1880191070453612545` (5001 出纳岗) | 出纳岗 | `ROLE_BOOKKEEPER` |
| `1880191264779911169` (3001 复核岗) | 复核岗 | `ROLE_REVIEWER` |
| `1880190696367833089` (6001 会计主管) | 会计主管 | `ROLE_REVIEWER` |
| `1880191529151086593` (1001 单位员工) | 单位员工 | `ROLE_VIEWER` |

Legacy rows are soft-deleted (`deleted='y'`) after members are remapped, so they no longer appear in the default assignment UI.

## Permission packs

See `sql/patches/2026-09-05-rbac-four-roles.sql` for seeded `permission` rows (book_id=`1` template; runtime grants remain per book as today).
