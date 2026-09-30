# Menu Dashboard Nest + Icons Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nest「代账工作台」under「仪表盘」（子项「首页」+「代账工作台」），按功能补齐菜单图标，并删除可见空壳菜单。

**Architecture:** 单一幂等 SQL patch 更新 `resources` / `permission`；前端继续读 `/open/func/list` 的 `res_style`，不改 `menu.ts` 树逻辑。

**Tech Stack:** MySQL 8、`resources`/`permission` 表、Vue 侧栏 `svg-icon`（`res_style` → `assets/icons/svg`）

**Spec:** `docs/superpowers/specs/2026-09-30-menu-dashboard-nest-icons-design.md`

## Global Constraints

- 交付物仅 `sql/patches/2026-09-30-menu-dashboard-nest-icons.sql`（+ 可选注释更新 books-board patch）
- 不改 `financial_cloud_init.sql`、不改 `financial-cloud-ui/src/api/menu.ts`
- 幂等：可重复执行；`INSERT ... WHERE NOT EXISTS` / 按固定 id `UPDATE`
- 空壳定义：可见 MENU + 无可见子 MENU + URL 为 `NULL`/`''`/`#`/`/`
- 固定 ID：仪表盘父 `981331493802475520`；首页 `2026093000000000001`；首页权限 `2026093000000000002`–`0005`；代账工作台 `2026092900000000081`
- Commit 仅在用户明确要求时执行（本仓库惯例）

---

## File map

| File | Responsibility |
|------|----------------|
| Create: `sql/patches/2026-09-30-menu-dashboard-nest-icons.sql` | 嵌套结构 + 图标 + 空壳清理（全部变更） |
| Modify (optional): `sql/patches/2026-09-29-books-board-menu.sql` | 顶部注释注明现挂仪表盘下 |
| Verify: live DB via `pymysql` @ `127.0.0.1:3307` / `financial_cloud` | 验收查询 |

---

### Task 1: 编写并落库幂等 patch

**Files:**
- Create: `sql/patches/2026-09-30-menu-dashboard-nest-icons.sql`
- Modify (optional): `sql/patches/2026-09-29-books-board-menu.sql`（仅第 1 行注释）

**Interfaces:**
- Consumes: 现有资源 ID（见 Global Constraints）与四角色 permission 惯例
- Produces: DB 中仪表盘分组树 + 更新后的 `res_style`；无空壳可见 MENU

- [ ] **Step 1: 创建 patch 文件（完整内容如下）**

```sql
-- 仪表盘嵌套（首页 + 代账工作台）+ 功能图标对齐 + 空壳菜单清理（幂等可重复执行）
-- Spec: docs/superpowers/specs/2026-09-30-menu-dashboard-nest-icons-design.md

SET @root_id = '1';
SET @dashboard_id = '981331493802475520';
SET @home_id = '2026093000000000001';
SET @books_board_id = '2026092900000000081';
SET @perm_home_admin = '2026093000000000002';
SET @perm_home_bk = '2026093000000000003';
SET @perm_home_rv = '2026093000000000004';
SET @perm_home_vw = '2026093000000000005';

-- 1) 仪表盘改为分组壳
UPDATE resources
SET request_url = '',
    res_style = 'dashboard',
    icon = NULL,
    sort_index = 1,
    modified_by = '1',
    modified_date = NOW()
WHERE id = @dashboard_id
  AND deleted = 'n';

-- 2) 首页（原 /index）
INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
)
SELECT
    @home_id, '首页', '首页', 'MENU', @home_id, '/index',
    'GET', NULL, 'r', NULL, NULL, 'home',
    'n', 'n', 'n', 'y',
    @dashboard_id, '仪表盘', 1, '经营看板首页',
    '1', NOW(), '1', NOW(), '1', 'n'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE id = @home_id);

UPDATE resources
SET res_name = '首页',
    i18n = '首页',
    request_url = '/index',
    res_style = 'home',
    icon = NULL,
    parent_id = @dashboard_id,
    parent_name = '仪表盘',
    sort_index = 1,
    is_visible = 'y',
    status = '1',
    deleted = 'n',
    modified_by = '1',
    modified_date = NOW()
WHERE id = @home_id;

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_home_admin, 'ROLE_ADMINISTRATORS', @home_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_home_admin);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_home_bk, 'ROLE_BOOKKEEPER', @home_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_home_bk);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_home_rv, 'ROLE_REVIEWER', @home_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_home_rv);

INSERT INTO permission (id, role_id, resource_id, created_by, created_date, status, book_id)
SELECT @perm_home_vw, 'ROLE_VIEWER', @home_id, '1', NOW(), 1, '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM permission WHERE id = @perm_home_vw);

-- 3) 代账工作台挂到仪表盘下
UPDATE resources
SET parent_id = @dashboard_id,
    parent_name = '仪表盘',
    sort_index = 2,
    res_style = 'monitor',
    icon = NULL,
    request_url = '/workspace/books-board',
    is_visible = 'y',
    status = '1',
    deleted = 'n',
    modified_by = '1',
    modified_date = NOW()
WHERE id = @books_board_id
  AND deleted = 'n';

-- 4) 功能图标对齐（仅缺口 / 纠语义）
UPDATE resources SET res_style = 'swap', icon = NULL WHERE id = '2026090315000000001'; -- 往来管理
UPDATE resources SET res_style = 'account-book', icon = NULL WHERE id = '2026090315000000002'; -- 应收应付余额
UPDATE resources SET res_style = 'file-text', icon = NULL WHERE id = '2026090315000000003'; -- 往来明细
UPDATE resources SET res_style = 'bar-chart', icon = NULL WHERE id = '2026090315000000004'; -- 账龄分析
UPDATE resources SET res_style = 'check-circle', icon = NULL WHERE id = '2026090315000000005'; -- 核销工作台
UPDATE resources SET res_style = 'history', icon = NULL WHERE id = '981334866064834560'; -- 日志审计
UPDATE resources SET res_style = 'audit', icon = NULL WHERE id = '981337003041751040'; -- 登录日志
UPDATE resources SET res_style = 'audit', icon = NULL WHERE id = '981337181773627392'; -- 系统日志
UPDATE resources SET res_style = 'cluster', icon = NULL WHERE id = '981335709019275264'; -- 组织
UPDATE resources SET res_style = 'user', icon = NULL WHERE id = '981335758977630208'; -- 用户管理
UPDATE resources SET res_style = 'group', icon = NULL WHERE id = '981335810039087104'; -- 角色管理
UPDATE resources SET res_style = 'read', icon = NULL WHERE id = '981337246718230528'; -- 资源管理
UPDATE resources SET res_style = 'carry-out', icon = NULL WHERE id = '981337555771326464'; -- 权限分配
UPDATE resources SET res_style = 'eye', icon = NULL WHERE id = '981336054843834368'; -- 会话
UPDATE resources SET res_style = 'mail', icon = NULL WHERE id = '981336354157756416'; -- 电子邮箱
UPDATE resources SET res_style = 'send', icon = NULL WHERE id = '981336403415662592'; -- 短信服务
UPDATE resources SET res_style = 'file-protect', icon = NULL WHERE id = '981336473196298240'; -- 登录策略
UPDATE resources SET res_style = 'file-protect', icon = NULL WHERE id = '981336523834130432'; -- 密码策略

-- 5) 空壳菜单清理（须在仪表盘子菜单挂好之后）
DELETE FROM permission
WHERE resource_id IN (
    SELECT id FROM (
        SELECT r.id
        FROM resources r
        WHERE r.deleted = 'n'
          AND r.status = '1'
          AND r.classify = 'MENU'
          AND r.is_visible = 'y'
          AND (r.request_url IS NULL OR r.request_url IN ('', '#', '/'))
          AND NOT EXISTS (
              SELECT 1 FROM resources c
              WHERE c.parent_id = r.id
                AND c.deleted = 'n'
                AND c.status = '1'
                AND c.classify = 'MENU'
                AND c.is_visible = 'y'
          )
    ) t
);

DELETE FROM resources
WHERE id IN (
    SELECT id FROM (
        SELECT r.id
        FROM resources r
        WHERE r.deleted = 'n'
          AND r.status = '1'
          AND r.classify = 'MENU'
          AND r.is_visible = 'y'
          AND (r.request_url IS NULL OR r.request_url IN ('', '#', '/'))
          AND NOT EXISTS (
              SELECT 1 FROM resources c
              WHERE c.parent_id = r.id
                AND c.deleted = 'n'
                AND c.status = '1'
                AND c.classify = 'MENU'
                AND c.is_visible = 'y'
          )
    ) t
);
```

- [ ] **Step 2:（可选）更新历史 books-board patch 注释**

将 `sql/patches/2026-09-29-books-board-menu.sql` 第 1 行改为：

```sql
-- 代账工作台（多账套月末看板）菜单；初版挂根节点。后续由 2026-09-30-menu-dashboard-nest-icons.sql 挂到「仪表盘」下。幂等可重复执行
```

- [ ] **Step 3: 对本地库执行 patch**

```bash
python -c "import pymysql; from pathlib import Path; sql=Path(r'sql/patches/2026-09-30-menu-dashboard-nest-icons.sql').read_text(encoding='utf-8'); conn=pymysql.connect(host='127.0.0.1',port=3307,user='financial_cloud',password='FinancialCloud321!',database='financial_cloud',charset='utf8mb4',autocommit=True); cur=conn.cursor();
[cur.execute(s) for s in sql.split(';') if s.strip() and not s.strip().startswith('--')]; print('ok', cur.rowcount); conn.close()"
```

更好用：用 `pymysql` 按语句执行（跳过纯注释段）。期望：无异常退出。

- [ ] **Step 4: 验收查询（期望结果见注释）**

保存并运行 `tmp_verify_menu_patch.py`：

```python
# -*- coding: utf-8 -*-
import pymysql
from pathlib import Path

conn = pymysql.connect(
    host="127.0.0.1", port=3307,
    user="financial_cloud", password="FinancialCloud321!",
    database="financial_cloud", charset="utf8mb4",
)
cur = conn.cursor()
out = []

cur.execute("""
SELECT res_name, parent_id, request_url, res_style, sort_index
FROM resources
WHERE id IN ('981331493802475520','2026093000000000001','2026092900000000081')
ORDER BY FIELD(id,'981331493802475520','2026093000000000001','2026092900000000081')
""")
out.append("=== DASHBOARD TREE ===")
for r in cur.fetchall():
    out.append(str(r))

cur.execute("""
SELECT COUNT(*) FROM resources
WHERE parent_id='1' AND id='2026092900000000081' AND deleted='n' AND status='1'
""")
out.append(f"books-board still top-level? {cur.fetchone()[0]} (expect 0)")

cur.execute("""
SELECT res_name, res_style FROM resources
WHERE id IN (
 '2026090315000000001','2026090315000000002','2026090315000000003',
 '2026090315000000004','2026090315000000005','981334866064834560'
) ORDER BY id
""")
out.append("=== ICONS SAMPLE ===")
for r in cur.fetchall():
    out.append(str(r))

cur.execute("""
SELECT COUNT(*) FROM resources r
WHERE r.deleted='n' AND r.status='1' AND r.classify='MENU' AND r.is_visible='y'
  AND (r.request_url IS NULL OR r.request_url IN ('','#','/'))
  AND NOT EXISTS (
    SELECT 1 FROM resources c
    WHERE c.parent_id=r.id AND c.deleted='n' AND c.status='1'
      AND c.classify='MENU' AND c.is_visible='y'
  )
""")
out.append(f"empty shells: {cur.fetchone()[0]} (expect 0)")

cur.execute("SELECT COUNT(*) FROM permission WHERE resource_id='2026093000000000001'")
out.append(f"home perms: {cur.fetchone()[0]} (expect >=4)")

Path("tmp_menu_verify.txt").write_text("\n".join(out), encoding="utf-8")
conn.close()
print("wrote tmp_menu_verify.txt")
```

期望：
- 仪表盘：`parent_id` 根侧、`request_url=''`、`res_style=dashboard`
- 首页：`parent_id=981331493802475520`、`/index`、`home`
- 代账：`parent_id=981331493802475520`、`monitor`；不再挂根
- `empty shells: 0`
- `home perms: 4`（或更多若环境另有账套授权）

- [ ] **Step 5: 再执行一次 patch，确认幂等**

重复 Step 3，再跑 Step 4；资源数与 permission 数不变、无报错。

- [ ] **Step 6: Commit（仅当用户要求）**

```bash
git add sql/patches/2026-09-30-menu-dashboard-nest-icons.sql sql/patches/2026-09-29-books-board-menu.sql docs/superpowers/specs/2026-09-30-menu-dashboard-nest-icons-design.md docs/superpowers/plans/2026-09-30-menu-dashboard-nest-icons.md
git commit -m "$(cat <<'EOF'
refactor(menu): nest books-board under dashboard and align icons

EOF
)"
```

Windows PowerShell 可用：

```powershell
git add sql/patches/2026-09-30-menu-dashboard-nest-icons.sql sql/patches/2026-09-29-books-board-menu.sql docs/superpowers/specs/2026-09-30-menu-dashboard-nest-icons-design.md docs/superpowers/plans/2026-09-30-menu-dashboard-nest-icons.md
git commit -m "refactor(menu): nest books-board under dashboard and align icons"
```

---

### Task 2: 前端冒烟（手工）

**Files:**
- None（只验证 UI）

**Interfaces:**
- Consumes: Task 1 落库后的 `/open/func/list` 菜单树
- Produces: 验收通过记录（口头/聊天即可）

- [ ] **Step 1: 重新登录或刷新**（清菜单缓存：退出再登录）

- [ ] **Step 2: 检查侧栏**

期望：
1. 顶级有「仪表盘」分组，无独立顶级「代账工作台」
2. 展开见「首页」「代账工作台」，图标分别为 home / monitor
3. 首页 → `/index`；代账工作台 → `/workspace/books-board`
4. 往来四子项有非 `list` 图标

- [ ] **Step 3: 完成** — 无需代码变更；若图标缺失则核对 `res_style` 与 svg 文件名是否一致

---

## Spec coverage self-review

| Spec 要求 | Task |
|-----------|------|
| 仪表盘分组 + 首页 + 代账嵌套 | Task 1 §1–3 |
| 图标映射表 | Task 1 §4 |
| 空壳删除 | Task 1 §5 |
| 幂等 patch 文件 | Task 1 |
| 不改 init / menu.ts | 遵守 Global Constraints |
| 验收 1–5 | Task 1 Step 4–5 + Task 2 |

无 placeholder；ID 与 spec 一致。
