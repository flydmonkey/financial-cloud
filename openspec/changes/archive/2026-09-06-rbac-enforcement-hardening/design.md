## Context

See proposal.md — Why. Menus already resolve via book-scoped `role_member` then global `permission` packs (`AuthzMapper` + `AuthzResourceMapper`). Partial API guards exist (`ProductRoles` on voucher/settlement; book update/delete/member already check book admin). Gaps: many controllers still menu-only; `AuthzMapper` omits `book_id` filter when `bookId` blank; frontend `hasRole` treats `ROLE_SUPERVISOR`/`1000` as universal pass.

## Goals / Non-Goals

**Goals:**
- Align API denial with role menu matrix for priority modules
- Fail-closed role query and empty-menu UX
- Stop treating legacy supervisor codes as default product bypass
- Keep book-admin mutate rules explicit in specs (partially already implemented)

**Non-Goals:**
- Per-book custom menu packs for the same role
- Rewriting the entire interceptor to permission-resource ACL
- Deleting commented legacy system-settings controllers
- Changing the four product role IDs

## Decisions

1. **Enforcement helper stays `ProductRoles` (+ book-admin helpers)**  
   - Extend/normalize `requireWriteVoucher` / `requireApprove` / `requireClosePeriod` / `requireBookAdministrator` rather than inventing a second framework.  
   - Alternative rejected: full `permission` resource ACL on every API — higher cost, not needed for this phase.

2. **Priority module set for this change**  
   - Must cover: voucher mutations/approve, settlement close, book update/delete/members (verify complete), journal write paths, fixed-asset write, payroll write, ARAP write, standards admin, role management.  
   - Read-only list/fetch for modules a role can open may remain allowed if the role has that menu.  
   - Alternative rejected: “guard every controller in one PR” — too large; use inventory checklist in tasks.

3. **Fail-closed `bookId`**  
   - When `bookId` blank: do not run the open `member_id IN (...)` branch without book filter; return empty product roles (keep synthetic login roles if still required for auth plumbing, but they must not grant menus).  
   - Alternative rejected: pick an arbitrary book silently — already handled at login; missing mid-session should fail closed.

4. **Legacy `ROLE_SUPERVISOR` / `1000`**  
   - Remove from frontend universal bypass and from product `ProductRoles` allow-lists for book business checks.  
   - If admin role still carries `role_code=1000` as an authority string, map it only when the DB role is `ROLE_ADMINISTRATORS` for the active book, not as a global superuser.  
   - Alternative rejected: hard-delete supervisor category immediately without audit — migrate by code-path first.

5. **Empty menu UX**  
   - Reuse/extend permission route guard: if functions empty after login with a book, show dedicated page / message (contact book admin), not 404 loop.

## Risks / Trade-offs

- [Over-deny breaks legit flows] → Use matrix from `apply_rbac_four_roles.py` as source of truth; add regression tests per role smoke.  
- [Frontend still shows buttons] → Pair API guards with `v-hasRole` cleanup on known toolbars.  
- [Admin role_code 1000 confusion] → Explicit mapping rules in ProductRoles tests.

## Migration Plan

1. Ship fail-closed AuthzMapper + frontend empty-menu guard (low risk).  
2. Roll API guards module-by-module behind existing ProductRoles helpers; verify with four role accounts.  
3. Remove supervisor/1000 bypasses after admin smoke still works via `ROLE_ADMINISTRATORS`.  
4. Update `verify_rbac_menu_packs.py` / docs; no DB role id changes required.

## Open Questions

- Whether criteria-management (准则管理) APIs need admin-only guards in the same PR or a follow-up once inventory finishes — default: include if exposed and admin-only in matrix.
