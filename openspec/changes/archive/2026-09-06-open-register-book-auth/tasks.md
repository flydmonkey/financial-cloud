## 1. Open registration

- [x] 1.1 Add anonymous register API (username/password/displayName, password policy, trim username) and verify duplicate username returns a clear error while success creates an active user
- [x] 1.2 Add login-page registration UI wired to the register API and verify a visitor can register then log in without admin provisioning
- [x] 1.3 Ensure post-login empty-book users still reach onboarding/create-book and verify a brand-new registered user can create a book

## 2. Book creator bootstrap

- [x] 2.1 On book create, write `permission_book` + book-scoped `ROLE_ADMINISTRATORS` for the creator in the same flow and verify the creator can enter the book with admin menus
- [x] 2.2 Verify creating an additional book grants admin only on the new book (existing books' roles unchanged)

## 3. Book member authorization

- [x] 3.1 Add book-admin-only member APIs: search registered users (narrow fields), list members with roles, grant with required `roleId`, revoke; verify non-admin callers are denied
- [x] 3.2 Persist grant/revoke via `permission_book` + book-scoped `role_member` and verify invited 审核员 gets reviewer authorities only on that active book
- [x] 3.3 Build 账套管理 members UI (search, role select, grant, list, revoke) and verify the happy path end-to-end in the UI
- [x] 3.4 Hide or demote user-admin「账套配置」as the primary invite path and verify collaboration is done from 账套管理

## 4. Slim menus and disable APIs

- [x] 4.1 Inventory 系统设置 children and 日志审计 tree resource IDs; seed/migrate to hide all except 角色管理 under 系统设置; verify `/open/func/list` for admin matches
- [x] 4.2 Comment out HTTP entry mappings for hidden modules' controllers (code retained); keep role management APIs; keep any narrow user-search needed by book invite; verify commented endpoints are unavailable and role APIs still work
- [x] 4.3 Adjust product-role permission packs if needed so non-admin roles never regain hidden menus; verify bookkeeper/reviewer/viewer menu snapshots exclude 系统设置 siblings and 日志审计

## 5. Docs and smoke

- [x] 5.1 Update `docs/product/00-overview.md`, `01-account-book.md`, and `10-system-admin.md` for open register + book-admin invite model and verify docs match behavior
- [x] 5.2 Smoke: register → create book → invite second user as 审核员 → second user login/switch book → menus/guards correct; verify no 404 and no hidden menus
