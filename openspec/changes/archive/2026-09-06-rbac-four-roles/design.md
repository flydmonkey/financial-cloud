## Context

See proposal.md for motivation. Current stack already has `roles` + `role_member` + `permission` (role→resource) + `permission_book` (user→book), menu delivery via `/api/open/func/list`, and light front-end `v-hasPermi` usage. Pain is data and provisioning, not a missing framework: only 系统管理员组 carries a full permission pack; job-post roles are empty; JWT still injects synthetic `ROLE_USER` / `ROLE_ALL_USER` / `ROLE_GENERAL_USER` that do not grant menus.

## Goals / Non-Goals

**Goals:**

- Ship four named product roles with seeded menu packs matching the agreed matrix.
- Enforce role + book on user create/update; fail closed when menus would be empty.
- Phase-2 operation guards for viewer voucher read-only and reviewer-only close APIs.
- Provide a migration path from legacy job-post roles / half-authorized users.

**Non-Goals:**

- Full SaaS multi-tenant IAM rewrite or replacing JWT/session model.
- Fine-grained field-level ACL or per-voucher ownership rules.
- Redesigning the entire resources tree beyond what the matrix requires.
- Deciding yet whether 审核员 may edit vouchers or whether 做账员 may create/delete books (recorded as open questions; default conservatively in tasks).

## Decisions

### 1. Keep table model; change seed + assignment UX

- **Choice:** Reuse `roles` / `role_member` / `permission` / `permission_book` and `/open/func/list`.
- **Why:** Model already matches product docs; chaos is empty packs and missing assignment, not schema.
- **Alternative:** Introduce a new RBAC service — rejected as out of scope and high risk.

### 2. Four roles via rename/merge, preserve admin id when possible

- **Choice:** Keep `ROLE_ADMINISTRATORS` as 管理员. Introduce or rename three general roles to 做账员 / 审核员 / 查看员 with stable codes (e.g. `ROLE_BOOKKEEPER`, `ROLE_REVIEWER`, `ROLE_VIEWER` or mapped legacy codes). Retire unused job-post roles from the default catalog after migrating members.
- **Why:** Admin already holds 212 permissions; preserving id avoids breaking the only working pack.
- **Alternative:** Delete all roles and recreate — riskier for existing admin sessions and FKs.

### 3. Two-phase enforcement

- **Phase 1 — Menu packs + provisioning:** Seed `permission` rows per role; require product role when granting books (user→book + book-scoped `role_member`); front-end guard when `functions` empty.
- **Phase 2 — Operation points:** Map write APIs (voucher mutate, approve, settle/unsettle) to permissions or role checks; hide buttons with `v-hasPermi` / role helpers.
- **Why:** Menu-only cannot express viewer read-only; doing both at once blocks shipping the matrix.

### 4. Partial modules as explicit resource subsets

- **账套管理 P:** grant 账套列表 + 初始余额 + 辅助核算 + cash-flow config resources; exclude 系统参数.
- **日志审计 P (审核员):** grant 登录日志 + 系统日志 only.
- **Why:** Matches product decisions without inventing a new “partial module” abstraction.

### 5. Default answers for deferred ops (until product revisits)

- 审核员: allow voucher edit unless product later says no (needed for corrections during review); approve/reverse-approve yes; close/reverse-close yes.
- 做账员: no book create/delete in phase 1 menu pack beyond 账套列表 navigation; creating books stays admin unless later opened.
- These defaults are recorded so tasks can proceed; changing them only adjusts permission rows / API checks.

## Risks / Trade-offs

- **[Risk] Existing users with books but no role break after fail-closed guard** → Mitigation: migration script listing affected users; one-click assign 查看员 or 做账员; admin bootstrap path remains.
- **[Risk] Over-granting if admin pack is copied wholesale to bookkeeper** → Mitigation: build packs from matrix checklist, not clone-all; verify with automated menu snapshot tests per role.
- **[Risk] UI-only read-only for viewer is bypassable** → Mitigation: phase 2 API denial is mandatory before calling viewer “done”.
- **[Trade-off] Synthetic JWT authorities remain** → Accept for now; menus ignore them. Optional cleanup later.

## Migration Plan

1. Add/rename four product roles; seed permission packs from matrix.
2. Map legacy members: empty job posts → nearest product role; `wjc`-like admin members stay 管理员.
3. Backfill users who have `permission_book` but no `role_member` (default 查看员 unless marked otherwise).
4. Deploy API validation + front-end empty-menu guard.
5. Deploy phase-2 operation checks.
6. Rollback: keep old role rows soft-available; feature-flag fail-closed guard if needed.

## Open Questions

- Should 做账员 later gain “创建账套” for agency workflows? (Default no in phase 1.)
- Exact stable `role_code` strings for the three non-admin roles (cosmetic; decide at seed time).
