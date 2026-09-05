## 1. Fail-closed role resolution

- [x] 1.1 Change Authz role query so blank `bookId` does not merge multi-book `role_member` rows; verify with a unit/integration case that empty bookId returns no book-scoped product roles
- [x] 1.2 Ensure menu list path uses the same fail-closed behavior; verify `/api/open/func/list` with no active book does not return business module menus from other books
- [x] 1.3 Add/adjust frontend empty-menu / no-role guard (dedicated message, no 404 loop); verify a user with book access but no product role sees the guard page

## 2. ProductRoles and legacy bypass cleanup

- [x] 2.1 Inventory `ROLE_SUPERVISOR` / `1000` usages in backend `ProductRoles` and frontend `hasRole`/`auth` plugins; document the list in the change notes
- [x] 2.2 Remove universal frontend bypass for supervisor/1000 on product checks; verify bookkeeper session cannot open admin-only actions via client helpers
- [x] 2.3 Align backend ProductRoles allow-lists so ordinary book APIs accept only the four product roles (map admin via `ROLE_ADMINISTRATORS`); verify ProductRoles unit tests pass

## 3. Book admin mutate enforcement (baseline + gaps)

- [x] 3.1 Confirm book update/delete and members APIs require book administrator; add tests if missing and verify non-admin calls return permission denied
- [x] 3.2 Confirm books list UI hides edit/delete/members for non-admins; verify in UI with a bookkeeper account

## 4. Module API guards (matrix-aligned)

- [x] 4.1 Inventory write/sensitive endpoints for voucher, settlement, journal, fixed asset, payroll, ARAP, standards, role management; produce checklist vs menu matrix
- [x] 4.2 Add/complete API guards for voucher write/approve and settlement close; verify viewer/bookkeeper denial cases
- [x] 4.3 Add/complete API guards for journal / fixed-asset / payroll / ARAP write paths; verify denial for viewer (and bookkeeper where matrix forbids)
- [x] 4.4 Add/complete API guards for 准则管理 and 角色管理 admin-only APIs; verify non-admin denial
- [x] 4.5 Spot-check corresponding frontend toolbars still hide forbidden actions; verify no obvious orphan buttons for viewer

## 5. Tooling and docs

- [x] 5.1 Extend or run `tools/verify_rbac_menu_packs.py` (or equivalent) after menu rename to 系统设置; verify packs still match intended matrix
- [x] 5.2 Update `docs/product/00-overview.md` / `10-system-admin.md` with enforcement notes (menu ≠ only control); verify docs mention API checks
- [x] 5.3 Smoke four roles on one book (admin/bookkeeper/reviewer/viewer): menus + one forbidden API each; record results in tasks notes or PR description
