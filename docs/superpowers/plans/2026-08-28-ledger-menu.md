# 账簿菜单 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增与「凭证」同级的顶级菜单「账簿」（紧挨其后），并将明细账、总账、科目余额表迁入其下；费用明细表留在报表。

**Architecture:** 仅改 `resources` / `permission` 菜单数据。用可幂等 seed SQL 创建父菜单并 UPDATE 三个子菜单的 `parent_id`；同步修正 `general_ledger_menu.sql` 的默认父级，避免重跑总账 seed 把总账挂回报表。页面路由与 Vue/API 不动。

**Tech Stack:** MySQL `resources`/`permission`；Python + pymysql apply 脚本（对齐现有 seed 工具）

**Spec:** [docs/superpowers/specs/2026-08-28-ledger-menu-design.md](../specs/2026-08-28-ledger-menu-design.md)

## Global Constraints

- 菜单名：**账簿**（非「账薄」）
- 位置：`凭证 → 账簿 → …`（账簿 `sort_index = 3`）
- 子菜单顺序：明细账 → 总账 → 科目余额表
- **费用明细表**继续留在报表，不迁入
- 不改 URL：`/voucher/sub-ledger`、`/statement/general-ledger`、`/statement/subject-balance`
- 不改 Vue / Controller / API
- Seed **必须幂等**：禁止无条件对顶级菜单反复 `sort_index + 1`
- 不主动 `git commit`（除非用户要求）

---

## File map

| 路径 | 职责 |
|------|------|
| `sql/seed/ledger_books_menu.sql` | 创建「账簿」父菜单；幂等抬升顶级 sort；迁入三个子菜单 |
| `tools/apply_ledger_books_menu.py` | 对本机库执行 seed 并打印校验结果 |
| `sql/seed/general_ledger_menu.sql` | 父级改为账簿 ID / `parent_name=账簿` / `sort_index=2`，避免重跑 undo |
| `sql/seed/README.md` | 补充账簿菜单导入说明 |

**固定 ID（本计划锁定）：**

| 用途 | ID |
|------|-----|
| 账簿父菜单 `resources.id` | `2026082817000000001` |
| 账簿父菜单 permission | `2026082817000000002` |
| 明细账（已有） | `1903024792422047745` |
| 总账（已有） | `2026082816300000001` |
| 科目余额表（已有） | `1886384516205912065` |
| 报表父菜单（不变） | `1886357455563137026` |
| 凭证父菜单（不变） | `1869692874272862209` |
| 顶级根 | `1` |

---

### Task 1: Seed SQL — 账簿父菜单 + 迁入子菜单

**Files:**
- Create: `sql/seed/ledger_books_menu.sql`
- Modify: none in this task

**Interfaces:**
- Consumes: 现有三个子菜单 resource id（上表）
- Produces: 父菜单 id `2026082817000000001`；执行后账簿下三条子菜单 parent/sort 正确

- [ ] **Step 1: 创建 `sql/seed/ledger_books_menu.sql`**

完整文件内容如下（原样写入）：

```sql
-- 账簿顶级菜单 + 明细账/总账/科目余额表迁入（可重复执行）
-- 费用明细表不迁移，仍挂在报表下

SET @ledger_id = '2026082817000000001';
SET @permission_id = '2026082817000000002';
SET @root_id = '1';
SET @sub_ledger_id = '1903024792422047745';
SET @general_ledger_id = '2026082816300000001';
SET @subject_balance_id = '1886384516205912065';

-- 仅当账簿父菜单尚不存在时，将原 sort_index>=3 的顶级菜单整体 +1，为账簿腾出 sort=3
UPDATE resources
SET sort_index = sort_index + 1
WHERE parent_id = @root_id
  AND sort_index >= 3
  AND NOT EXISTS (
      SELECT 1 FROM (
          SELECT id FROM resources WHERE id = @ledger_id
      ) AS already
  );

DELETE FROM permission WHERE id = @permission_id OR resource_id = @ledger_id;
DELETE FROM resources WHERE id = @ledger_id;

INSERT INTO resources (
    id, res_name, i18n, classify, permission, request_url, request_method,
    params, action_type, icon, icon_selected, res_style,
    is_open, is_frame, is_cache, is_visible,
    parent_id, parent_name, sort_index, description,
    created_by, created_date, modified_by, modified_date, status, deleted
) VALUES (
    @ledger_id,
    '账簿',
    'mxk.menu.ledgerBooks',
    'MENU',
    @ledger_id,
    '',
    'GET',
    NULL,
    'r',
    'menus-kemuyuebiao',
    NULL,
    NULL,
    'n',
    'n',
    'n',
    'y',
    @root_id,
    'JinBooks',
    3,
    NULL,
    '1',
    NOW(),
    '1',
    NOW(),
    '1',
    'n'
);

INSERT INTO permission (
    id, role_id, resource_id, created_by, created_date, status, book_id
) VALUES (
    @permission_id,
    'ROLE_ADMINISTRATORS',
    @ledger_id,
    '1',
    NOW(),
    1,
    '1'
);

-- 迁入子菜单（URL / resource id 不变）
UPDATE resources
SET parent_id = @ledger_id,
    parent_name = '账簿',
    sort_index = 1
WHERE id = @sub_ledger_id;

UPDATE resources
SET parent_id = @ledger_id,
    parent_name = '账簿',
    sort_index = 2
WHERE id = @general_ledger_id;

UPDATE resources
SET parent_id = @ledger_id,
    parent_name = '账簿',
    sort_index = 3
WHERE id = @subject_balance_id;
```

- [ ] **Step 2: 语法自检（不连库也可）**

确认文件：三个 `UPDATE` 子菜单、一次条件 `sort_index + 1`、固定 `@ledger_id`、无费用明细表 id。

- [ ] **Step 3: Commit（仅当用户要求时）**

```bash
git add sql/seed/ledger_books_menu.sql
git commit -m "feat: add ledger books menu seed SQL"
```

默认跳过，除非用户明确要求提交。

---

### Task 2: Apply 脚本 + 本地库验证

**Files:**
- Create: `tools/apply_ledger_books_menu.py`
- Test: 对本机 `127.0.0.1:3307` / `jinbooks` 执行并断言查询结果

**Interfaces:**
- Consumes: `sql/seed/ledger_books_menu.sql`
- Produces: 退出码 0 且打印校验通过；失败退出码 1

- [ ] **Step 1: 创建 `tools/apply_ledger_books_menu.py`**

完整文件：

```python
#!/usr/bin/env python3
"""Apply ledger books menu seed (idempotent) and verify structure."""
from pathlib import Path
import pymysql
from pymysql.constants import CLIENT

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "sql" / "seed" / "ledger_books_menu.sql"

LEDGER_ID = "2026082817000000001"
SUB_LEDGER_ID = "1903024792422047745"
GENERAL_LEDGER_ID = "2026082816300000001"
SUBJECT_BALANCE_ID = "1886384516205912065"
REPORT_ID = "1886357455563137026"
EXPENSE_DETAIL_ID = "2026082814300000001"
VOUCHER_ID = "1869692874272862209"


def verify(cur) -> None:
    cur.execute(
        "SELECT id, res_name, sort_index FROM resources "
        "WHERE parent_id='1' AND deleted='n' AND classify='MENU' "
        "ORDER BY sort_index, id"
    )
    tops = cur.fetchall()
    names = [r[1] for r in tops]
    assert "凭证" in names and "账簿" in names, names
    vi = names.index("凭证")
    li = names.index("账簿")
    assert li == vi + 1, f"账簿应紧挨凭证后: {names}"

    cur.execute(
        "SELECT id, res_name, sort_index, request_url FROM resources "
        "WHERE parent_id=%s AND deleted='n' ORDER BY sort_index, id",
        (LEDGER_ID,),
    )
    children = cur.fetchall()
    assert [c[0] for c in children] == [
        SUB_LEDGER_ID,
        GENERAL_LEDGER_ID,
        SUBJECT_BALANCE_ID,
    ], children
    assert [c[1] for c in children] == ["明细账", "总账", "科目余额表"], children
    assert [c[2] for c in children] == [1, 2, 3], children

    cur.execute(
        "SELECT parent_id FROM resources WHERE id=%s",
        (EXPENSE_DETAIL_ID,),
    )
    row = cur.fetchone()
    assert row and row[0] == REPORT_ID, f"费用明细表应仍在报表下: {row}"

    cur.execute(
        "SELECT COUNT(*) FROM resources WHERE parent_id=%s AND id IN (%s,%s,%s)",
        (REPORT_ID, SUB_LEDGER_ID, GENERAL_LEDGER_ID, SUBJECT_BALANCE_ID),
    )
    assert cur.fetchone()[0] == 0, "报表下不应再挂明细账/总账/科目余额表"

    cur.execute(
        "SELECT id FROM permission WHERE resource_id=%s AND role_id='ROLE_ADMINISTRATORS'",
        (LEDGER_ID,),
    )
    assert cur.fetchone(), "缺少账簿父菜单管理员权限"


def main() -> int:
    sql = SQL.read_text(encoding="utf-8")
    for user, password in (("jinbooks", "Jinbooks321!"), ("root", "root")):
        try:
            conn = pymysql.connect(
                host="127.0.0.1",
                port=3307,
                user=user,
                password=password,
                database="jinbooks",
                charset="utf8mb4",
                client_flag=CLIENT.MULTI_STATEMENTS,
                autocommit=True,
            )
            with conn.cursor() as cur:
                cur.execute(sql)
                verify(cur)
                # 幂等：再执行一次仍通过
                cur.execute(sql)
                verify(cur)
            conn.close()
            print(f"OK applied+verified {SQL.name} as {user} (twice)")
            return 0
        except Exception as e:
            print(f"fail {user}: {e}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] **Step 2: 执行 apply（首次）**

Run:

```bash
python tools/apply_ledger_books_menu.py
```

Expected: `OK applied+verified ledger_books_menu.sql as jinbooks (twice)`，退出码 0。

若库中尚无总账/费用明细菜单，先执行：

```bash
python tools/apply_general_ledger_menu.py
python tools/apply_expense_detail_menu.py
```

再跑本脚本。

- [ ] **Step 3: 人工抽查（可选）**

```bash
python -c "import pymysql; c=pymysql.connect(host='127.0.0.1',port=3307,user='jinbooks',password='Jinbooks321!',database='jinbooks',charset='utf8mb4'); cur=c.cursor(); cur.execute(\"SELECT res_name,sort_index FROM resources WHERE parent_id='1' AND deleted='n' ORDER BY sort_index\"); print(cur.fetchall()); cur.execute(\"SELECT res_name,sort_index,request_url FROM resources WHERE parent_id='2026082817000000001' ORDER BY sort_index\"); print(cur.fetchall())"
```

Expected: 顶级含 `凭证` 后紧跟 `账簿`；账簿下三行 URL 分别为 `/voucher/sub-ledger`、`/statement/general-ledger`、`/statement/subject-balance`。

---

### Task 3: 修正总账 seed 父级 + README

**Files:**
- Modify: `sql/seed/general_ledger_menu.sql`
- Modify: `sql/seed/README.md`

**Interfaces:**
- Consumes: 账簿父 id `2026082817000000001`
- Produces: 重跑 `apply_general_ledger_menu.py` 后总账仍挂在账簿下 `sort_index=2`

- [ ] **Step 1: 修改 `sql/seed/general_ledger_menu.sql`**

将文件开头与 INSERT 中父级相关字段改为：

```sql
-- 总账菜单（账簿子菜单），可重复执行
SET @resource_id = '2026082816300000001';
SET @permission_id = '2026082816300000002';
SET @parent_id = '2026082817000000001';
```

并将 INSERT 中：

- `parent_name`：`'财务报表'` → `'账簿'`
- `sort_index`：`9` → `2`

其余字段保持不变（含 `request_url = '/statement/general-ledger'`）。

- [ ] **Step 2: README 追加说明**

在 `sql/seed/README.md` 总账菜单那一段落后追加一行：

```markdown
账簿顶级菜单（迁入明细账/总账/科目余额表）：`python tools/apply_ledger_books_menu.py`（或 `mysql ... < sql/seed/ledger_books_menu.sql`）。若库中尚无总账菜单，先跑 `apply_general_ledger_menu.py`。**推荐顺序**：先 `ledger_books_menu`（会创建父菜单并迁入已存在子项），再按需跑 `general_ledger_menu`（现默认挂到账簿下）。
```

- [ ] **Step 3: 回归：先 ledger 再 general_ledger seed**

Run:

```bash
python tools/apply_ledger_books_menu.py
python tools/apply_general_ledger_menu.py
python tools/apply_ledger_books_menu.py
```

Expected: 三次均成功；最后一次 verify 仍显示总账 `parent_id=2026082817000000001`、`sort_index=2`；费用明细表仍在报表下。

- [ ] **Step 4: 登录侧栏目视（若本地前后端已起）**

打开系统侧栏：凭证旁出现「账簿」；展开为明细账 / 总账 / 科目余额表；报表下仍有费用明细表与三大报表；点击三页可打开。

---

## Spec coverage (self-review)

| Spec 项 | Task |
|---------|------|
| 新增顶级账簿，sort=3，紧挨凭证 | Task 1–2 |
| 子菜单顺序 明细→总账→科目余额 | Task 1–2 |
| 费用明细留报表 | Task 2 verify |
| 仅改菜单数据 / URL 不变 | Task 1（UPDATE parent only） |
| 幂等 sort 抬升 | Task 1 `NOT EXISTS`；Task 2 双次执行 |
| ROLE_ADMINISTRATORS 父菜单权限 | Task 1 INSERT permission |
| general_ledger seed 不 undo | Task 3 |
| README / apply 脚本 | Task 2–3 |

无 TBD；固定 ID 与脚本断言一致。
