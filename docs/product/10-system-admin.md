# 10 · 系统管理

> 状态：部分实现（用户/组织/角色/资源/账套授权/会话/登录与安全策略/机构可用；操作日志覆盖面窄且无 IP；业务操作留痕不足）

## 1. 模块定位

负责身份权限、会话、审计、安全策略与机构（部署级）配置，支撑多人协作与代账多账套授权。

## 2. 典型场景

1. 管理员创建用户并分配角色与可访问账套。
2. 配置密码策略与登录失败锁定。
3. 查看登录日志与系统操作日志。
4. 维护机构名称、Logo、域名（品牌展示）。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 用户 CRUD、改密、导入导出 | **已实现** | `views/idm/users.vue` |
| 当前用户 / 切换账套 | **已实现** | `currentUser`、`switchBook` |
| 组织树 CRUD | **已实现** | `organizations.vue` |
| 角色（用户组）与成员 | **已实现** | `groups.vue`、`RoleMember` |
| 资源树（菜单/API）维护 | **已实现** | `permissions/resources.vue` |
| 角色-资源授权 | **已实现** | `PermissionController` |
| 用户-账套授权 | **已实现** | `PermissionBookController` |
| 在线会话查看与终止 | **已实现** | `SessionController` |
| 登录日志 | **已实现** | `history_login`（含 IP） |
| 系统操作日志 | **部分实现** | 主要 IDM/权限；**无 IP**；凭证/结账等不写 |
| 密码策略 / 登录策略 | **已实现** | security 配置 API |
| 邮件 / 短信 / 社交登录配置 | **部分实现** | 后端 CRUD 齐全；部分前端菜单可能无页 |
| 机构信息维护 | **部分实现** | `institutions.vue`；非完整 SaaS 多租户 |
| 预设角色「做账员/审核员/查看员」产品化命名 | **部分实现** | 以种子角色与资源为准，非 PRD 文案一一对应 |
| 数据备份恢复（系统级） | **未实现** | 同账套备份缺口 |

## 4. 页面与路由

| 页面 | 组件 |
|------|------|
| 用户管理 | `views/idm/users.vue` |
| 组织 | `views/idm/organizations.vue` |
| 角色 | `views/idm/groups.vue` |
| 资源管理 | `views/permissions/resources.vue` |
| 审计：登录 / 系统 / 同步器 / 连接器等 | `views/audit/*.vue` |
| 机构 | `views/config/institutions.vue` |
| 个人中心 | `views/system/user/profile/` |

> 种子菜单中部分 `/access/*`、`/security/*` 等路径在本仓库 `views` 下可能无对应页面（遗留 IAM 菜单），以实际可打开页为准。

## 5. 数据模型

| 表 | 用途 |
|----|------|
| `userinfo` | 用户（含当前 `book_id`） |
| `organizations` | 部门组织 |
| `roles` / `role_member` | 角色与成员（成员可带 book_id） |
| `resources` / `permission` | 资源树与角色授权 |
| `permission_book` | 账套访问授权 |
| `session_list` | 在线会话 |
| `history_login` | 登录日志（含 `ip_addr`） |
| `history_system_logs` | 系统操作日志（无 IP 字段） |
| `config_login_policy` / `config_password_policy` | 登录与密码策略 |
| `institutions` | 机构/租户骨架 |

## 6. 认证与权限机制（产品视角）

```
请求 → 机构 Filter（Host → institutions）
     → PermissionInterceptor（JWT/Session，默认拒绝）
     → Controller（@CurrentUser）
```

- 登录：`POST /api/login/signin` → JWT + refreshToken  
- 菜单权限：`GET /open/func/list` → 动态路由  
- 前端指令：`v-hasPermi` / `v-hasRole`（视图使用较少）  
- 白名单：登录、验证码、token 刷新、open func 等  

详见 [../modules/platform.md](../modules/platform.md)。

## 7. 核心接口（摘录）

| 前缀 | 说明 |
|------|------|
| `/api/users` | 用户与 switchBook |
| `/api/orgs` | 组织 |
| `/api/idm/groups`、`/groupmembers` | 角色与成员 |
| `/api/permissions/resources`、`/permission`、`/permissionBook` | 权限 |
| `/api/access/session` | 会话 |
| `/api/historys/*` | 审计查询 |
| `/api/security/*` | 安全策略 |
| `/api/config/institutions` | 机构 |
| `/api/login`、`/api/logout`、`/api/auth/token/refresh` | 认证 |

## 8. 业务规则与约束

1. 无账套授权则无法进入业务数据；onboarding 可创建首个账套并授权。
2. 操作日志 `HistorySystemLogsService.log()` 覆盖用户、组织、角色、权限、资源、机构等；**不做**凭证级审计。
3. 机构解析影响登录页展示与多域名部署，不等同于按机构强制隔离全部业务数据（业务主隔离键仍是 `book_id`）。

## 9. 已知缺口

- 业务操作（凭证、结账、备份）全程留痕与 IP。
- 清理无效 IAM 菜单与缺失页面的一致性。
- 账套/系统备份恢复。
- 将机构升级为真正多租户（若商业化需要）——当前定性为品牌配置 + 骨架。

## 10. 证据索引

- `authn/interceptor/PermissionInterceptor.java`
- `UserInfoController`、`AuthzResourceService`、`HistorySystemLogsService`
- `views/idm/*`、`views/audit/*`、`views/permissions/resources.vue`
