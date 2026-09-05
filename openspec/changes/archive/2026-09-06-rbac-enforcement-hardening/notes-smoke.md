# Smoke notes (task 5.3)

Automated / code-level verification in this apply session:

| Check | Result |
|-------|--------|
| `AuthzMapper` blank `bookId` → `1=0` (no multi-book merge) | Implemented |
| `AuthzResourceService` blank bookId → empty menus | Implemented |
| `/no-access` route + permission guard (no logout/404 loop) | Implemented |
| ProductRoles without SUPERVISOR/1000 bypass | Implemented + unit test |
| Book `requireBookAdministrator` deny/allow | Unit tests |
| Journal / FA / payroll / ARAP / assist write → `requireWriteBusiness` | Controllers patched |
| Standards / roles / permission assign → `requireAdministrator` | Controllers patched |
| Voucher / settlement guards | Already present; allow-lists cleaned |
| Books UI hide edit/delete/members for non-admin | Already present (`isBookAdmin`) |
| `verify_rbac_menu_packs.py` updated for 系统设置 rename | Updated (run with `JB_DB_PASSWORD`) |

Manual four-role UI smoke (recommended before archive):

1. Admin: full menus; can edit book / members / roles.
2. Bookkeeper: has 系统设置 children except 系统参数/角色管理; voucher write OK; checkout API denied.
3. Reviewer: checkout OK; cannot open 准则管理 write.
4. Viewer: read menus; voucher create API denied; no 系统设置 top.

Record actual HTTP denials in the PR description when available.
