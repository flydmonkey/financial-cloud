# 菜单精简 + 缺图标修复 + init 全量对齐

日期：2026-10-01  
状态：已确认设计，待实现

## 背景

侧栏由 `resources`（`classify=MENU`）驱动，`res_style` 映射 `financial-cloud-ui/src/assets/icons/svg`。

本轮诉求：

1. 费用报销 / 增值税申报表 / 银行对账：补齐可见图标（现 `res_style` 指向不存在的 svg）
2. 系统设置仅保留「用户管理」「角色管理」，其余硬删
3. 「日志审计」整组硬删
4. 对应**管理端** CRUD 接口注释停用（登录内部读策略保留）
5. 将本会话全部菜单改动写入 `financial_cloud_init.sql`，避免新库初始化不全

与上一轮（2026-09-30 仪表盘嵌套）一并纳入 init 对齐范围。

## 决策摘要

| 项 | 选择 |
|----|------|
| 图标补齐方式 | 改 `res_style` 指向已有 svg |
| 菜单删除 | 硬删（先 `permission`，再 `resources`） |
| 接口停用 | 注释 `@RestController`（方案 A）；已 DISABLED 的只更新注释 |
| init | 同步本会话全部菜单终态 |

## 1. 图标映射

| 菜单 | ID | 旧 res_style | 新 res_style | svg |
|------|-----|--------------|--------------|-----|
| 费用报销 | `2026092900000000061` | `tickets` | `money-collect` | `money-collect.svg` |
| 增值税申报表 | `2026092900000000051` | `document` | `documentation` | `documentation.svg` |
| 银行对账 | `2026092900000000041` | `menus-yinhangduizhang` | `bank` | `bank.svg` |

## 2. 硬删菜单

### 系统设置（父 `981334679749656576` 保留）

删除子项：

| 名称 | ID |
|------|-----|
| 资源管理 | `981337246718230528` |
| 权限分配 | `981337555771326464` |
| 会话 | `981336054843834368` |
| 社交服务 | `981336254564007936` |
| 电子邮箱 | `981336354157756416` |
| 短信服务 | `981336403415662592` |
| 登录策略 | `981336473196298240` |
| 密码策略 | `981336523834130432` |

保留：用户管理 `981335758977630208`、角色管理 `981335810039087104`。

### 日志审计

删除父 `981334866064834560` 及子：

| 名称 | ID |
|------|-----|
| 登录日志 | `981337003041751040` |
| 系统日志 | `981337181773627392` |
| 同步器日志 | `981337094406275072` |

SQL：`DELETE FROM permission WHERE resource_id IN (...)`；`DELETE FROM resources WHERE id IN (...)`。

## 3. 管理端接口注释（方案 A）

沿用现有惯例：

```java
// DISABLED menu-trim-2026-10-01: admin UI removed, code retained
//@RestController
```

| Controller | 当前状态 | 动作 |
|------------|----------|------|
| `ConfigLoginPolicyController` | `@RestController` 启用 | 注释掉 |
| `ResourcesController` | 启用 | 注释掉 |
| `PermissionController` | 启用 | 注释掉 |
| `SessionController` | 已 DISABLED | 更新注释文案 |
| `ConfigEmailSendersController` | 已 DISABLED | 更新注释 |
| `ConfigSmsProviderController` | 已 DISABLED | 更新注释 |
| `ConfigPasswordPolicyController` | 已 DISABLED | 更新注释 |
| `LoginHistoryController` / `SystemLogsController` / `SynchronizerHistoryController` / `ConnectorHistoryController` | 已 DISABLED | 更新注释 |
| `SocialsProviderController` | 已 DISABLED | 更新注释 |

**必须保留：**

- `UserInfoController`、`RolesController`、`RoleMemberController`
- `PermissionBookController`（账套成员授权，用户管理依赖）
- `OpenFuncListController`（菜单下发）
- `ConfigPasswordPolicyService` / `ConfigLoginPolicyService` 等被登录链路调用的 Service（不删）

## 4. init SQL 全量对齐

文件：`sql/financial_cloud_init.sql`

须使**全新执行 init** 后的菜单态 = 当前库跑完下列 patch 后的终态：

1. `2026-09-30-menu-dashboard-nest-icons.sql`（仪表盘分组、首页、代账嵌套、图标对齐、空壳清理）
2. 本轮 `2026-10-01-menu-trim-sys-audit-icons.sql`

具体手段（实现时择一或组合，以可维护为准）：

- 调整 init 内 `resources` / `permission` 的 INSERT 内容：去掉将删菜单行；修正三图标与仪表盘树；确保首页资源与四角色 permission 存在
- 在 init **末尾**追加幂等 `UPDATE`/`DELETE`/`INSERT ... WHERE NOT EXISTS` 段，与 patch 等价（推荐：末尾对齐段，避免改巨型单行 INSERT）

**不改：** 业务表结构、非菜单种子（除非被菜单 INSERT 连带）。

## 5. 交付物

| 路径 | 内容 |
|------|------|
| `sql/patches/2026-10-01-menu-trim-sys-audit-icons.sql` | 图标 UPDATE + 硬删菜单（幂等） |
| `sql/financial_cloud_init.sql` | 末尾或种子对齐本会话菜单终态 |
| 上述 Controller 文件 | 注释/更新 DISABLED |

不改：`menu.ts` 树逻辑；不删 Vue 页面源文件（仅菜单与管理端 API 不可达）。

## 验收

1. 三菜单侧栏图标非空白（`money-collect` / `documentation` / `bank`）
2. 系统设置下仅用户管理、角色管理
3. 无「日志审计」顶级菜单
4. `/api/permissions/resources`、`/api/permissions/permission`、`/api/security/configLoginPolicy` 管理端 404/未映射；登录与用户/角色/账套授权仍可用
5. 对空库仅跑 init（或 init + 必要非菜单 patch）后，菜单树满足 1–3，且含仪表盘→首页/代账工作台

## 风险与非目标

- **风险：** 硬删不可回滚（需从备份或 git 历史恢复种子）；注释 `PermissionController` 后无法在 UI 改角色-资源授权（角色管理本身仍在）
- **非目标：** 不删除 Vue 页面文件；不删除 Service/表；不改 RBAC 四角色模型
