# 菜单重构：仪表盘嵌套 + 功能图标 + 空壳清理

日期：2026-09-30  
状态：已确认设计，待实现

## 背景

前端侧栏菜单由后端 `resources` 表（`classify=MENU`）驱动，`res_style` 映射到 `src/assets/icons/svg`（子目录用 `-` 连接，如 `menus-pingzhengguanli`）。

当前问题：

1. 「代账工作台」与「仪表盘」同为顶级，希望归入仪表盘分组。
2. 部分菜单缺图标（往来子项 `res_style=NULL`）或仍写 `anticon-*` 前缀，不够统一。
3. 可见但无子项、无有效 URL 的分组壳应删除。

## 决策摘要

| 项 | 选择 |
|----|------|
| 代账工作台位置 | 嵌套为「仪表盘」子菜单 |
| `/index` 子菜单名 | 首页 |
| 空菜单定义 | 可见 + 无可见子菜单 + URL 为空/`NULL`/`#`/`/` |
| 实现方式 | 幂等 SQL patch（不改 init、不改前端树重组逻辑） |
| 图标范围 | 只补缺口 / 纠语义，已正确的业务图标不动 |

## 目标结构

```
仪表盘 (parent=根, url='', icon=dashboard, sort=1)
├── 首页 (url=/index, icon=home, sort=1)          [新建]
└── 代账工作台 (url=/workspace/books-board, icon=monitor, sort=2)  [移动]
```

### ID 与变更

| 资源 | ID | 动作 |
|------|-----|------|
| 仪表盘（父） | `981331493802475520` | UPDATE：`request_url=''`，`res_style='dashboard'`，`parent_name` 保持根 |
| 首页 | `2026093000000000001`（新建） | INSERT MENU + 四角色 permission |
| 代账工作台 | `2026092900000000081` | UPDATE：`parent_id`→仪表盘，`parent_name='仪表盘'`，`sort_index=2`，`res_style='monitor'` |

首页 permission 固定 ID（幂等按 id）：

| permission id | role_id |
|---------------|---------|
| `2026093000000000002` | `ROLE_ADMINISTRATORS` |
| `2026093000000000003` | `ROLE_BOOKKEEPER` |
| `2026093000000000004` | `ROLE_REVIEWER` |
| `2026093000000000005` | `ROLE_VIEWER` |

均 `book_id='1'`、`status=1`，与代账工作台种子行一致。

父级「仪表盘」原有 permission 保留，用于分组可见性。

## 图标映射

写入 `res_style`，同时 `icon=NULL`（前端只读 `res_style`）。

| 菜单 | res_style |
|------|-----------|
| 仪表盘（父） | `dashboard` |
| 首页 | `home` |
| 代账工作台 | `monitor` |
| 往来管理（父） | `swap` |
| 应收应付余额 | `account-book` |
| 往来明细 | `file-text` |
| 账龄分析 | `bar-chart` |
| 核销工作台 | `check-circle` |
| 日志审计（父） | `history` |
| 登录日志 / 系统日志 | `audit` |
| 组织 | `cluster` |
| 用户管理 | `user` |
| 角色管理 | `group` |
| 资源管理 | `read` |
| 权限分配 | `carry-out` |
| 会话 | `eye` |
| 电子邮箱 | `mail` |
| 短信服务 | `send` |
| 登录策略 / 密码策略 | `file-protect` |

以上 svg 均已存在于 `financial-cloud-ui/src/assets/icons/svg/`。

## 空菜单清理

对 `classify='MENU' AND deleted='n' AND is_visible='y'`：

1. 无可见子菜单（子项同条件 `is_visible='y' AND status='1' AND deleted='n' AND classify='MENU'`）
2. 且 `request_url` 为 `NULL` / `''` / `'#'` / `'/'`

执行：先删 `permission`（`resource_id`），再删 `resources`。

**排除**：本 patch 刚改成的「仪表盘」父节点——清理须在其下已挂「首页」「代账工作台」之后执行，或显式 `WHERE id <> '981331493802475520'`；实际在挂接子菜单后再跑清理即可自然保留。

## 交付物

- `sql/patches/2026-09-30-menu-dashboard-nest-icons.sql`（幂等：`INSERT ... WHERE NOT EXISTS` / 按 id UPDATE）
- 可选：更新 `sql/patches/2026-09-29-books-board-menu.sql` 顶部注释，注明现挂仪表盘下（不强制改历史 INSERT）
- **不改** `financial_cloud_init.sql`
- **不改** `financial-cloud-ui/src/api/menu.ts` 树组装逻辑

## 验收

1. 侧栏：顶级仅见「仪表盘」分组；展开为「首页」「代账工作台」。
2. 点击首页 → `/index`；代账工作台 → `/workspace/books-board`。
3. 往来四子项与系统/审计相关菜单显示功能图标，无默认 `list` 空图标。
4. 无「可见、无子、无 URL」的空壳菜单。
5. patch 重复执行不报错、不产生重复 permission/资源。

## 风险与非目标

- **风险**：把仪表盘改成分组后，依赖「顶级 leaf 直达 /index」的旧书签仍可用（子路由不变）；TopNav 下仪表盘变为可展开分组。
- **非目标**：不重排其他顶级模块顺序；不物理清理 `status=0` 历史菜单；不改角色模型。
