# 10 · 系统管理

> 状态：产品面已收口（开放注册 + 账套管理员邀请）；侧栏「系统设置」由原账套管理顶级菜单改名而来（排在基础设置下），含账套管理、角色管理等；旧系统设置/日志审计已隐藏，对应 Controller 入口已注释（代码保留）。业务写操作与角色/准则管理接口另有 `ProductRoles` / 账套管理员强制校验，不仅依赖菜单隐藏。

## 1. 模块定位

支撑开放注册后的账套协作：角色资源包、账套级成员授权。部署运维类菜单（会话/策略/审计/机构等）已从产品菜单下线。

## 2. 典型场景

1. 访客自助注册并登录，无账套时走 onboarding 建账，创建者成为该账套管理员。
2. 账套管理员在「账套管理 → 成员授权」中搜索注册用户、选择产品角色并授权。
3. 管理员在「系统设置 → 角色管理」维护角色-资源（按需）。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 开放注册 | **已实现** | `POST /api/login/register` |
| 当前用户 / 切换账套 | **已实现** | `currentUser`、`switchBook` |
| 角色管理与角色-资源授权 | **已实现** | 挂在侧栏「系统设置」下 |
| 账套成员授权（含选角色） | **已实现** | `/api/book/members/*`，账套管理员 |
| 用户/组织/会话/审计/安全策略/机构菜单 | **已隐藏** | 资源 status=0；Controller 入口注释保留代码 |
| 预设角色「管理员/做账员/审核员/查看员」 | **已落地** | `rbac-four-roles` |
| 数据备份恢复（系统级） | **未实现** | — |

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
