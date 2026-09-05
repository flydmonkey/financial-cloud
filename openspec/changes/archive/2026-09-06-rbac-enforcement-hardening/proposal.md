## Why

四人产品角色与账套级授权已落地，侧栏菜单也按当前账套的 `role_member` 解析；但安全边界仍偏「靠菜单隐藏」。大量业务/配置接口未用 `ProductRoles` 校验，前端还兼容 `ROLE_SUPERVISOR`/`1000`，且 `bookId` 为空时可能合并多账套角色。需要把「菜单可见」与「接口可调」对齐，做成可对外讲清的强制执行模型。

## What Changes

- **BREAKING（接口）**：对已纳入角色菜单矩阵的业务模块，写操作 / 敏感读操作在后端 MUST 按当前账套产品角色拒绝越权调用（不仅依赖前端隐藏）。
- 角色解析 fail-closed：无有效 `bookId` 或无产品角色时，不得合并多账套角色、不得下发业务菜单；前端空菜单须明确兜底（禁止误入 404 环）。
- 清理或收敛遗留超级身份旁路：`ROLE_SUPERVISOR` / `1000` 不再作为普通产品路径的默认放行条件（兼容迁移策略见 design）。
- 系统设置（含账套管理、角色管理等）敏感操作延续「账套管理员」校验模式，与菜单包一致。
- 文档与校验脚本：固化「角色 × 模块 × 接口」检查清单，防止新菜单种子只给管理员。

## Capabilities

### New Capabilities
- `rbac-enforcement`: 产品角色强制执行——接口与菜单对齐、fail-closed 角色解析、遗留超级身份收敛

### Modified Capabilities
- `book-member-auth`: 明确账套级管理员操作（编辑/删除账套、成员授权）的后端强制校验与非管理员 UI 隐藏已作为基线要求

## Impact

- 后端：`ProductRoles` / `AuthzService` / 各业务 Controller；`AuthzMapper` 在无 `bookId` 时的行为；账套 update/delete 已有部分校验需纳入规格。
- 前端：`hasRole` / `auth` 插件；空菜单/无角色兜底；系统设置内操作按钮与角色一致。
- 数据/工具：`permission` 包与 `verify_rbac_menu_packs`；可选清理 `permission.book_id='1'` 噪音（不改变菜单语义）。
- 非目标：按账套定制同一角色的不同菜单包；物理删除注释掉的旧系统设置代码；邮箱验证。
