# 01 · 账套管理与初始化

> 状态：部分实现（核心 CRUD / 多账套 / onboarding 可用；封存、作废、备份未实现）

## 1. 模块定位

账套是所有财务数据的载体。代账场景下，一名用户可被授权多个账套，并在顶栏切换。新建账套时按所选会计准则模板一键初始化科目、报表模板、凭证模板与系统参数。

## 2. 典型场景

1. 用户自助注册后创建账套，选择「小企业会计准则」，填写企业信息与启用年月（创建者自动成为该账套管理员）。
2. 新用户登录后无账套 → 进入 `/onboarding` 引导完成首次建账。
3. 账套管理员在账套管理中邀请其他注册用户并选择产品角色。
4. 代账会计在顶栏切换账套，系统刷新 JWT / Session 中的 `bookId`。
5. 禁用空闲账套后删除（有数据时需先清理关联，见服务层约束）。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 新建账套并初始化科目/报表/模板 | **已实现** | `BookService.save()` |
| 账套列表、搜索、编辑 | **已实现** | `views/books/index.vue` |
| 启用 / 禁用 | **已实现** | `status`：1 启用 / 0 禁用 |
| 账套切换 | **已实现** | `GET /api/users/switchBook/{bookId}` |
| 用户-账套授权 | **已实现（主路径）** | 账套管理「成员授权」`/api/book/members/*` |
| Onboarding 向导 | **已实现** | `views/onboarding/index.vue`，`/api/book/setup`、`onboarding-status` |
| 企业信息：信用代码、纳税人类型、行业、启用月、准则 | **已实现** | 见数据模型 |
| 账套封存（结账后禁止改往期） | **未实现** | 往期锁定靠结账期间，非账套封存态 |
| 账套作废（有数据不可删仅作废） | **未实现** | 仅启用/禁用 + 删除约束 |
| 手动/定时备份、备份恢复、导出备份文件 | **未实现** | 无 backup/restore 业务接口 |

## 4. 页面与路由

| 页面 | 路径 / 组件 | 用途 |
|------|-------------|------|
| 账套列表 | `/books/index` → `views/books/index.vue` | CRUD、进入账套 |
| 账套编辑 | `views/books/edit.vue` | 表单（含 vatType、industry、standardId） |
| 初始化向导 | `/onboarding` → `views/onboarding/index.vue` | 无账套时强制引导 |
| 账套授权 | 账套管理「成员授权」`books/members.vue` | 账套管理员邀请注册用户并选择产品角色 |
| 角色管理 | 系统设置「角色管理」 | 角色-资源授权 |

顶栏账套选择：`layout/components/Navbar.vue`（切换后 `location.reload()`）。

## 5. 数据模型

### 表 `book`（实体 `Book`）

| 字段 | 含义 |
|------|------|
| `id` / `name` / `company_name` | 主键、账套名、单位名称 |
| `credit_code` | 统一社会信用代码 |
| `vat_type` | 0 小规模 / 1 一般纳税人 |
| `industry` | 所属行业（整型字典） |
| `enable_date` | 做账起始年月 |
| `standard_id` | 会计准则模板 ID |
| `current_account_date` | 当前记账年月 |
| `voucher_reviewed` | 是否开启凭证审核 |
| `status` | 1 启用 / 0 禁用（**非**封存/作废） |
| `deleted` | 逻辑删除标记 |

关联：`permission_book`（用户授权）、`config`（账套级参数，含当前账期等）、业务表统一 `book_id`。

## 6. 核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/book/fetch` | 分页列表（仅当前用户 `permission_book` 已授权账套） |
| GET | `/api/book/fetchAll` | 当前用户可见账套（顶栏切换） |
| GET | `/api/book/get/{id}` | 详情 |
| POST | `/api/book/save` | 新建并初始化 |
| POST | `/api/book/setup` | Onboarding 建账 |
| GET | `/api/book/onboarding-status` | 是否完成初始化 |
| PUT | `/api/book/update` | 更新 |
| DELETE | `/api/book/delete` | 删除（通常要求先禁用） |
| GET/POST/DELETE | `/api/permissions/permissionBook/*` | 账套授权 |

前端 API：`src/api/book/book.ts`、`src/api/idm/user.ts`（`switchBook`）。

## 7. 业务规则与约束

1. **初始化**：创建时按 `standardId` 复制准则科目、报表模板、凭证模板、现金流量关系等（`BookService`）。
2. **可见范围**：账套管理分页（`/api/book/fetch`）与顶栏切换（`/api/book/fetchAll`）均仅返回当前用户在 `permission_book` 中已授权的账套；首次创建/setup 自动写入授权。管理员也只能看到自己有权限的账套。
3. **管理员操作**：编辑 / 删除 / 成员授权仅账套管理员（`ROLE_ADMINISTRATORS`）可见；后端 `update`/`delete`/`book/members/*` 均校验该角色。
4. **删除**：禁用后方可删；需处理关联数据（服务层校验）。
5. **当前账期**：存在于账套字段与 `config`；结账推进期间见 [06-settlement.md](06-settlement.md)。
6. **无账套用户**：路由守卫 `permission.ts` 导向 `/onboarding`。

## 8. 已知缺口

- PRD 要求的封存 / 作废状态机缺失。
- 无账套级备份、自动备份、一键恢复。
- 「批量备份 / 批量导出」列表操作未实现。
- 机构层与账套层关系对用户侧说明不足（见 [00-overview.md](00-overview.md) §5）。

## 9. 证据索引

- `financial-cloud/.../domain/book/Book.java`
- `financial-cloud/.../service/book/BookService.java`
- `financial-cloud/.../controller/book/BookController.java`
- `sql/financial_cloud_init.sql` → `CREATE TABLE book`
