## Context

See proposal.md for motivation. Current stack already has open login, `permission_book`, book-scoped `role_member`, four product roles, and user-admin「账套配置」as the grant UI. Registration is admin-provisioned; system settings and log-audit menus are still seeded for deployment-style admin. Constraints: keep code for hidden modules; comment API entries; no email/phone verification this phase; book creator = book admin; invite path moves to 账套管理.

## Goals / Non-Goals

**Goals:**
- Public register → create book → become book admin
- Book-admin invite + role select in 账套管理
- Slim menus: 系统设置 only 角色管理; hide 日志审计 tree
- Disable hidden-module HTTP entries by commenting mappings/controllers while retaining implementations

**Non-Goals:**
- Email/SMS verification, invite links, SSO
- Billing / multi-tenant institutions
- Physically deleting hidden module code
- Changing the four product role matrix itself (reuse `rbac-four-roles`)

## Decisions

1. **Registration API**
   - **Choice:** Add anonymous `POST /api/login/register` (or adjacent open endpoint) creating `userinfo` with password policy, no role/book required at signup.
   - **Why:** Matches login-adjacent UX; avoids admin-only user create.
   - **Alternatives:** Reuse `UserInfoController.add` with anonymous flag (risk: exposes admin DTO surface).

2. **Book creator bootstrap**
   - **Choice:** On book create by current user, write `permission_book` + `role_member(ROLE_ADMINISTRATORS, userId, bookId)` for the creator (same transaction as book init).
   - **Why:** Makes “new user can create book and admin it” automatic.
   - **Alternatives:** Separate “claim ownership” step (worse UX).

3. **Invite/grant API ownership**
   - **Choice:** New book-scoped member APIs under book management (e.g. list/search users, add member with `roleId`, remove member), authorized only if caller has book-scoped `ROLE_ADMINISTRATORS` (or equivalent ProductRoles check) for `current bookId`. Persist via existing `permission_book` + book-scoped `role_member`.
   - **Why:** Matches “账套管理员邀请”; avoids platform-admin-only `PermissionBookController` as primary path.
   - **Alternatives:** Keep only user-admin grant UI (rejected by product).

4. **User search for invite**
   - **Choice:** Restricted search by exact/prefix username (or display name) returning non-sensitive fields; only callable by book admins.
   - **Why:** Enough for invite without exposing full user directory.
   - **Alternatives:** Open global user list (privacy risk).

5. **Demote user-admin book drawer**
   - **Choice:** Hide「账套配置」entry from user-admin UI (or leave read-only); do not delete backend grant APIs yet if still used by bootstrap—prefer book member APIs for product UX.
   - **Why:** Spec requires book management as primary path.

6. **Menu slim-down**
   - **Choice:** Soft-delete or un-seed `resources` / `permission` rows for hidden menus for all product roles; keep 角色管理 resource + admin permission. Do not remove role packs for business modules.
   - **Why:** Menu comes from `/open/func/list`; data-driven hide is consistent.
   - **Alternatives:** Front-end-only filter (incomplete; APIs still advertised via menu).

7. **API entry commenting**
   - **Choice:** Comment `@RequestMapping` / class-level mappings (or disable via `@Profile`/`@Conditional` only if commenting is awkward) on controllers for: users/orgs (if under 系统设置 and not needed elsewhere), security policy, sessions, institution, login/system logs, etc. Keep `RolesController` (and related role-permission APIs) active. Document the commented list in tasks.
   - **Why:** User asked 接口注释、代码保留.
   - **Caveat:** User/org APIs may still be needed if product keeps identity elsewhere—only comment endpoints whose menus are removed and that are not required by register/book-invite flows. Prefer commenting audit + pure system-settings siblings; keep minimal identity APIs required by invite search if they live under user controllers (narrow allowlist rather than blanket comment).

8. **Who sees 角色管理**
   - **Choice:** Only book-scoped / platform administrators receive 角色管理 menu (existing admin pack). Registered bookkeepers do not.
   - **Why:** Avoid every self-registered user editing global role-resource packs.

## Risks / Trade-offs

- [Self-registered users creating unlimited books] → Mitigation: accept for phase 1; later add quotas.
- [Commenting user APIs breaks invite search] → Mitigation: keep a narrow search endpoint; comment only unused admin CRUD surfaces or relocate search under book member API.
- [Global role management still deployment-shaped] → Mitigation: phase 1 keep as-is under 系统设置; later move to book-local custom roles if needed.
- [Existing deployments still have old menus in DB] → Mitigation: seed/migration script to hide resources for all environments.

## Migration Plan

1. Deploy registration + book bootstrap + book member APIs behind UI.
2. Run menu seed/migration to hide system-settings siblings and 日志审计; verify admin still sees 角色管理.
3. Comment disabled controller mappings; smoke-test register → create book → invite reviewer → switch user login.
4. Hide user-admin「账套配置」primary entry.
5. Rollback: restore menu permissions from backup seed; uncomment mappings; feature-flag register off if needed.

## Open Questions

- Whether platform superuser `admin` remains a special break-glass account outside book-scoped admin (assume yes, unchanged).
- Exact inventory of controllers to comment vs keep for invite search (finalize during apply by mapping current 系统设置 / 日志审计 menus to controllers).
