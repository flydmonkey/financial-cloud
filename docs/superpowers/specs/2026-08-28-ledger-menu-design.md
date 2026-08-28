# 账簿菜单 — 设计说明

日期：2026-08-28  
状态：已确认，待实现  
范围：新增顶级「账簿」菜单；将明细账 / 总账 / 科目余额表从「报表」迁入；费用明细表仍留在报表

## 一、已确认决策

| # | 项 | 选择 |
|---|-----|------|
| 1 | 菜单名 | **账簿**（非「账薄」） |
| 2 | 层级 | 与「凭证」同级（`parent_id = 1`） |
| 3 | 位置 | 紧挨凭证之后：`凭证 → 账簿 → …`（方案 A） |
| 4 | 子菜单顺序 | 明细账 → 总账 → 科目余额表 |
| 5 | 费用明细表 | **继续留在报表下**，不迁入账簿 |
| 6 | 实现路径 | **仅改菜单数据**；页面路由 / API / Vue 不变 |
| 7 | 交付 | 可重复执行 SQL + 可选 apply 脚本（对齐现有 seed 模式） |

## 二、背景与目标

### 2.1 背景

当前侧栏「报表」下同时挂有财务报表（资产负债 / 利润 / 现金流）与账簿类查询（明细账 / 总账 / 科目余额表），职责混杂。账簿类与日常凭证查阅更接近，宜与「凭证」并列。

### 2.2 目标

1. 新增顶级菜单 **账簿**，紧挨「凭证」之后  
2. 将 **明细账、总账、科目余额表** 挂到「账簿」下，顺序如上  
3. **费用明细表**及三大报表仍留在「报表」  
4. 不改变既有页面路径与权限资源 ID（仅改 `parent_id` / `parent_name` / `sort_index`）

### 2.3 非目标

- 修改 `/voucher/sub-ledger`、`/statement/general-ledger`、`/statement/subject-balance` 等 URL  
- 改动报表页面、服务端 API、导出逻辑  
- 调整费用明细表菜单位置  
- 为非管理员角色批量补权（仅保证新父菜单对 `ROLE_ADMINISTRATORS` 可用，与现有 seed 一致）

## 三、目标菜单结构

### 3.1 顶级顺序（相关段）

| sort_index | 菜单 |
|------------|------|
| 2 | 凭证（不变） |
| **3** | **账簿（新增）** |
| ≥4 | 原 sort≥3 的顶级菜单依次 +1（日记账、薪资、结账、报表…） |

### 3.2 账簿子菜单

| sort_index | 名称 | 资源 ID（现有） | request_url（不变） |
|------------|------|-----------------|---------------------|
| 1 | 明细账 | `1903024792422047745` | `/voucher/sub-ledger` |
| 2 | 总账 | `2026082816300000001` | `/statement/general-ledger` |
| 3 | 科目余额表 | `1886384516205912065` | `/statement/subject-balance` |

三条子记录：`parent_id` → 新账簿菜单 ID，`parent_name` = `账簿`。

### 3.3 报表下保留

- 资产负债表  
- 利润表  
- 现金流量表  
- 费用明细表  

（以及报表下其他未列入迁出名单的项。）

## 四、数据变更

### 4.1 新增「账簿」父菜单

- 表：`resources`  
- `classify = MENU`，`parent_id = '1'`，`res_name = '账簿'`  
- `request_url`：空或 `/`（与「凭证」等目录型顶级一致）  
- `i18n`：新增稳定 key（如 `mxk.menu.ledgerBooks`），`res_name` 作回退展示  
- `icon`：可复用现有账簿/科目类图标（如 `menus-kemuyuebiao` 或账套相关图标），实现时择一与侧栏风格一致者  
- 固定新 ID（雪花风格字符串，与 `general_ledger_menu.sql` 同类），保证 seed 可重复执行  

### 4.2 权限

- `permission`：为新父菜单插入 `ROLE_ADMINISTRATORS` 一行（`book_id` 与现有 seed 一致）  
- 子菜单原有 permission 行保留，不删不改（仍按原 `resource_id` 鉴权）

### 4.3 排序调整（须幂等）

1. 若「账簿」父菜单**尚不存在**：将 `parent_id = '1'` 且 `sort_index >= 3` 的菜单 `sort_index = sort_index + 1`，再插入账簿 `sort_index = 3`  
2. 若「账簿」**已存在**：不再整体 +1；仅 `UPDATE` 其 `sort_index = 3` 及名称等字段  
3. 无论是否首次：将三个子菜单 `parent_id` / `parent_name` / `sort_index` 更新为账簿下 1、2、3  

也可用「按固定 ID DELETE 父菜单 permission+resources 再 INSERT」模式，但**禁止**无条件地对顶级菜单反复 `sort_index + 1`。

### 4.4 交付文件建议

| 文件 | 作用 |
|------|------|
| `sql/seed/ledger_books_menu.sql`（或 `sql/jinbooks_v1.x.x-ledger-menu.sql`） | 可重复执行的迁移 |
| `tools/apply_ledger_books_menu.py`（可选） | 对本机 Docker/本地库执行并打印校验结果 |

对齐参考：`sql/seed/general_ledger_menu.sql`、`sql/jinbooks_v1.1.3-move-voucher-summary.sql`、`tools/apply_general_ledger_menu.py`。

## 五、验证

1. 执行 seed 后查询：`parent_id=1` 顺序为凭证 → 账簿 → …  
2. 账簿下恰有三条：明细账、总账、科目余额表，顺序正确  
3. 报表下仍有费用明细表；无上述三条（或 parent 已非报表）  
4. 登录管理员：侧栏可见「账簿」及子项；点击仍打开原页面且功能正常  
5. 重复执行 seed：无重复菜单行、结构不变  

## 六、风险与说明

- **动态路由**：前端菜单来自后端 `resources`，改库即可，一般无需改 `router` 静态配置（与历史菜单搬家一致）。  
- **站内链接**：如凭证汇总表链到 `/voucher/sub-ledger` 的路径不变，不受影响。  
- **总账设计文档**中「菜单位置：财务报表下」将在实现后视为过时，以本文为准；不要求同步改总账功能 spec，除非另开文档修订任务。  
