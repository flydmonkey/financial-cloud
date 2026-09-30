# Menu Trim Sys/Audit + Icons + Init Align Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix three missing menu icons, hard-delete trimmed system-settings children and the entire 日志审计 tree, disable matching admin Controllers, and align `financial_cloud_init.sql` to the full menu end-state from this session (including 2026-09-30 dashboard nesting).

**Architecture:** Idempotent SQL patch for live DB; comment `@RestController` on admin Controllers (services for login retained); append an idempotent “menu end-state” section near the end of init SQL so fresh installs match patched DBs.

**Tech Stack:** MySQL 8, Spring `@RestController`, Vue sidebar `res_style` → svg

**Spec:** `docs/superpowers/specs/2026-10-01-menu-trim-sys-audit-icons-design.md`

## Global Constraints

- Icons: 费用报销→`money-collect`；增值税申报表→`documentation`；银行对账→`bank`
- Hard-delete menus listed in spec §2 (permission then resources)
- Keep 系统设置 parent + 用户管理 + 角色管理
- Disable admin Controllers via `//@RestController` + DISABLED comment; keep User/Roles/PermissionBook/OpenFuncList and login Services
- Init must encode end-state of `2026-09-30-menu-dashboard-nest-icons` + this patch
- Commit only when user explicitly asks
- Do not edit `menu.ts`; do not delete Vue page files

---

## File map

| File | Responsibility |
|------|----------------|
| Create: `sql/patches/2026-10-01-menu-trim-sys-audit-icons.sql` | Live DB: icons + hard deletes |
| Modify: `sql/financial_cloud_init.sql` | Append menu end-state alignment before final `SET FOREIGN_KEY_CHECKS = 1` (or immediately after existing menu icon block / before balance-sheet rules — prefer **append just before** `SET FOREIGN_KEY_CHECKS = 1` at file end) |
| Modify Controllers under `financial-cloud/src/main/java/com/financial/cloud/controller/...` | Disable admin REST surface |

---

### Task 1: SQL patch（图标 + 硬删）并落库验收

**Files:**
- Create: `sql/patches/2026-10-01-menu-trim-sys-audit-icons.sql`
- Verify via pymysql @ `127.0.0.1:3307` / `financial_cloud`

**Interfaces:**
- Consumes: resource IDs from spec §1–§2
- Produces: DB menu tree matching acceptance 1–3

- [ ] **Step 1: Create patch file with exact content:**

```sql
-- 菜单精简：三图标修复 + 系统设置瘦身 + 删除日志审计（幂等可重复执行）
-- Spec: docs/superpowers/specs/2026-10-01-menu-trim-sys-audit-icons-design.md

-- 1) 图标（缺失 svg → 已有 svg）
UPDATE resources SET res_style = 'money-collect', icon = NULL WHERE id = '2026092900000000061'; -- 费用报销
UPDATE resources SET res_style = 'documentation', icon = NULL WHERE id = '2026092900000000051'; -- 增值税申报表
UPDATE resources SET res_style = 'bank', icon = NULL WHERE id = '2026092900000000041'; -- 银行对账

-- 2) 待删资源 ID
-- 系统设置子：资源管理/权限分配/会话/社交/邮箱/短信/登录策略/密码策略
-- 日志审计父+子：日志审计/登录日志/系统日志/同步器日志
SET @del_ids = NULL; -- documentation only; use IN-list below

DELETE FROM permission
WHERE resource_id IN (
  '981337246718230528','981337555771326464','981336054843834368','981336254564007936',
  '981336354157756416','981336403415662592','981336473196298240','981336523834130432',
  '981334866064834560','981337003041751040','981337181773627392','981337094406275072'
);

DELETE FROM resources
WHERE id IN (
  '981337246718230528','981337555771326464','981336054843834368','981336254564007936',
  '981336354157756416','981336403415662592','981336473196298240','981336523834130432',
  '981334866064834560','981337003041751040','981337181773627392','981337094406275072'
);
```

Remove the useless `SET @del_ids` line when writing the real file (keep only comments + UPDATEs + DELETEs).

- [ ] **Step 2: Apply with pymysql** (strip `--` line comments inside statements; same connection for multi-statement). Expect no errors. Re-run once for idempotency.

- [ ] **Step 3: Verify** — write/run script; expect:

```
expense icon = money-collect
tax icon = documentation
recon icon = bank
sys children names = {用户管理, 角色管理} only (visible MENU)
日志审计 count under parent_id=1 = 0
deleted ids remaining in resources = 0
```

- [ ] **Step 4: Do NOT commit** unless user asks.

---

### Task 2: 注释管理端 Controllers

**Files:**
- Modify: `financial-cloud/src/main/java/com/financial/cloud/controller/security/ConfigLoginPolicyController.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/controller/permissions/ResourcesController.java`
- Modify: `financial-cloud/src/main/java/com/financial/cloud/controller/permissions/PermissionController.java`
- Modify (comment text only if already DISABLED):  
  `SessionController.java`, `ConfigEmailSendersController.java`, `ConfigSmsProviderController.java`, `ConfigPasswordPolicyController.java`, `LoginHistoryController.java`, `SystemLogsController.java`, `SynchronizerHistoryController.java`, `ConnectorHistoryController.java`, `SocialsProviderController.java`

**Interfaces:**
- Consumes: Task 1 menus gone
- Produces: admin HTTP mappings removed for trimmed features; login Services unchanged

- [ ] **Step 1: For the three still-enabled Controllers**, replace active `@RestController` with:

```java
// DISABLED menu-trim-2026-10-01: admin UI removed, code retained
//@RestController
```

Exact locations today:
- `ConfigLoginPolicyController.java` line with `@RestController` (currently active)
- `ResourcesController.java` `@RestController`
- `PermissionController.java` `@RestController`

- [ ] **Step 2: For already-DISABLED Controllers**, replace comment  
  `DISABLED open-register-book-auth: menu hidden, code retained`  
  with  
  `DISABLED menu-trim-2026-10-01: admin UI removed, code retained`  
  (keep `//@RestController`).

- [ ] **Step 3: Confirm NOT modified:** `UserInfoController`, `RolesController`, `RoleMemberController`, `PermissionBookController`, `OpenFuncListController`.

- [ ] **Step 4: Compile check** (optional if heavy):  
  `cd financial-cloud; .\mvnw.ps1 -q -DskipTests compile`  
  Expect BUILD SUCCESS. If environment lacks JDK, note SKIP in report.

- [ ] **Step 5: Do NOT commit** unless user asks.

---

### Task 3: 对齐 `financial_cloud_init.sql` 菜单终态

**Files:**
- Modify: `sql/financial_cloud_init.sql` — insert a new section **immediately before** the final lines:

```sql
SET FOREIGN_KEY_CHECKS = 1;

-- Default admin: username=admin password=changeme (change after first login)
```

**Interfaces:**
- Consumes: content of `sql/patches/2026-09-30-menu-dashboard-nest-icons.sql` + Task 1 patch
- Produces: fresh init yields same menu end-state

- [ ] **Step 1: Append section header and paste combined idempotent SQL** (dashboard nest + icon align from 09-30 patch body + Task 1 trim/icons). Prefer **including the full bodies** of both patches rather than `SOURCE` includes (init is often run as one file).

Structure:

```sql
-- ==================================================================
-- Menu end-state alignment (2026-09-30 nest + 2026-10-01 trim)
-- Spec: docs/superpowers/specs/2026-10-01-menu-trim-sys-audit-icons-design.md
-- Idempotent; safe after earlier seed INSERTs in this file.
-- ==================================================================

-- >>> paste verbatim body of sql/patches/2026-09-30-menu-dashboard-nest-icons.sql
-- >>> paste verbatim body of sql/patches/2026-10-01-menu-trim-sys-audit-icons.sql
```

Order matters: nest/icons first, then trim deletes.

- [ ] **Step 2: Sanity grep init** for deleted IDs still inserted later in file after the alignment block — if any later INSERT re-adds deleted menus, either remove those seed rows or ensure alignment block stays **last** among menu mutations. The append-before-`FOREIGN_KEY_CHECKS` placement makes it last among SQL statements.

- [ ] **Step 3: Spot-check** that init section contains homepage id `2026093000000000001` and delete list for `981334866064834560`.

- [ ] **Step 4: Do NOT commit** unless user asks.

---

### Task 4: 综合验收

**Files:** none (scripts only)

- [ ] **Step 1: Re-query live DB** (after Task 1–2) for acceptance checklist in spec §验收 1–4.

- [ ] **Step 2: Confirm init file contains both patch bodies** near end.

- [ ] **Step 3: Write short report** to `.superpowers/sdd/briefs/task-4-menu-trim-verify-report.md` with PASS/FAIL lines.

---

## Spec coverage self-review

| Spec § | Task |
|--------|------|
| §1 icons | Task 1 |
| §2 hard delete | Task 1 |
| §3 Controllers | Task 2 |
| §4 init | Task 3 |
| Acceptance | Task 4 |

No placeholders; IDs match spec.
