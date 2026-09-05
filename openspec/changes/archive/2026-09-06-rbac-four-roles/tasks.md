## 1. Role catalog and seed packs

- [x] 1.1 Define stable codes/names for 管理员 / 做账员 / 审核员 / 查看员 (keep `ROLE_ADMINISTRATORS`) and document mapping from legacy job-post roles in a short migration note under the change folder; verify the four roles appear as the intended catalog targets
- [x] 1.2 Build SQL/seed script that inserts or updates the three non-admin product roles and clears obsolete empty job-post roles from the default assignment UI; verify `roles` table contains the four product roles
- [x] 1.3 Seed `permission` rows for 做账员 from the menu matrix (including 出纳/固定资产/薪资 and 账套管理 P subset); verify `/open/func/list` for a bookkeeper test user matches the matrix
- [x] 1.4 Seed `permission` rows for 审核员 (full business + 结账 + 账套管理 P + 日志审计 P); verify menu snapshot for a reviewer test user
- [x] 1.5 Seed `permission` rows for 查看员 (仪表盘/凭证/账簿/报表/往来 only); verify menu snapshot excludes 结账/出纳/资产/薪资/账套/准则/系统设置

## 2. Provisioning and fail-closed entry

- [x] 2.1 Add backend validation so book authorization (`permissionBook/add`) requires a product `roleId` and writes `role_member` scoped to that book; verify API rejects missing/invalid role
- [x] 2.2 Update 账套配置 UI to require selecting a product role when granting books; remove role/book fields from user create/edit; verify grant list shows role name
- [x] 2.3 Add front-end/router handling when authenticated user has books but empty menus (no silent 404); verify the user sees an authorization message or remediation path
- [x] 2.4 Write migration query/script listing users with books but no `role_member`, and backfill a chosen default (查看员 unless overridden); verify the list is empty afterward or explicitly documented exceptions remain
- [x] 2.5 Scope `queryRolesByMembers` by current `bookId` so switching books refreshes authorities to that book’s role only

## 3. Phase-2 operation guards

- [x] 3.1 Identify voucher write/approve APIs and enforce 查看员 denial (and UI hide write actions); verify viewer can open list/detail but write APIs return authorization failure
- [x] 3.2 Enforce 结账/反结账 APIs for 管理员+审核员 only; verify 做账员/查看员 calls fail and 审核员 succeeds subject to existing close business rules
- [x] 3.3 Align voucher approve/reverse-approve with 管理员+审核员 (做账员 cannot approve); verify button visibility and API checks

## 4. Verification and docs

- [x] 4.1 Add or extend automated checks (API or e2e) that assert menu packs for the four roles against the matrix; verify CI or local run passes
- [x] 4.2 Update `docs/product/00-overview.md` / `10-system-admin.md` role naming to the four product roles and matrix summary; verify docs match seeded behavior
- [x] 4.3 Smoke-test login paths for admin / bookkeeper / reviewer / viewer test accounts on one book; verify each lands on an allowed home page without 404 and without duplicate 首页/仪表盘 sidebar entries
