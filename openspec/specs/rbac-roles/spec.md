# rbac-roles Specification

## Purpose

Defines the four-role RBAC product model for Financial Cloud: standard roles, menu access matrix, mandatory user authorization, and read-only constraints for viewers on vouchers.

## Requirements

### Requirement: Four standard product roles
The system MUST provide exactly four product roles for authorization: 管理员 (Administrator), 做账员 (Bookkeeper), 审核员 (Reviewer), and 查看员 (Viewer). Legacy unused job-post roles MUST be migrated or retired so that new authorizations only use these four roles.

#### Scenario: Role catalog lists four product roles
- **WHEN** an administrator opens role management
- **THEN** the standard product roles 管理员, 做账员, 审核员, and 查看员 are available for assignment

#### Scenario: New user cannot be assigned only a retired job-post role
- **WHEN** an administrator creates or updates a user after migration
- **THEN** the system MUST NOT require or present retired empty job-post roles as the sole authorization option

### Requirement: Role to module menu matrix
The system MUST grant top-level module menus according to the following matrix (Y = visible, - = hidden, P = partial children as specified elsewhere in this spec):

| Module | 管理员 | 做账员 | 审核员 | 查看员 |
|--------|--------|--------|--------|--------|
| 仪表盘 | Y | Y | Y | Y |
| 凭证 | Y | Y | Y | Y |
| 账簿 | Y | Y | Y | Y |
| 报表 | Y | Y | Y | Y |
| 结账 | Y | - | Y | - |
| 出纳 | Y | Y | Y | - |
| 固定资产 | Y | Y | Y | - |
| 薪资 | Y | Y | Y | - |
| 往来管理 | Y | Y | Y | Y |
| 账套管理 | Y | P | P | - |
| 准则管理 | Y | - | - | - |
| 系统设置 | Y | - | - | - |
| 日志审计 | Y | - | P | - |

#### Scenario: Bookkeeper receives daily accounting menus
- **WHEN** a user with only the 做账员 role loads menus
- **THEN** the response includes 仪表盘, 凭证, 账簿, 报表, 出纳, 固定资产, 薪资, 往来管理, and the partial 账套管理 set, and MUST NOT include 结账, 准则管理, or 系统设置

#### Scenario: Viewer does not receive closing or cashier menus
- **WHEN** a user with only the 查看员 role loads menus
- **THEN** the response includes 仪表盘, 凭证, 账簿, 报表, and 往来管理, and MUST NOT include 结账, 出纳, 固定资产, 薪资, 账套管理, 准则管理, 系统设置, or 日志审计

### Requirement: Partial book-set management menus
For 做账员 and 审核员, 账套管理 MUST include 账套列表, 初始余额, 辅助核算, and cash-flow related configuration menus, and MUST NOT include 系统参数.

#### Scenario: Reviewer opens book-set management without system parameters
- **WHEN** a 审核员 user loads menus
- **THEN** 账套管理 children include 账套列表, 初始余额, 辅助核算, and cash-flow configuration entries, and MUST NOT include 系统参数

### Requirement: Partial audit menus for reviewer
For 审核员, 日志审计 MUST include login and system operation log menus, and MUST NOT include security policy or session administration menus that belong to 系统设置.

#### Scenario: Reviewer can open login and system logs
- **WHEN** a 审核员 user loads menus
- **THEN** 日志审计 includes 登录日志 and 系统日志

### Requirement: Role selected when authorizing books
Authorizing a user to a book MUST require selecting one product role for that book. The grant MUST write `permission_book` and a book-scoped `role_member`. Creating or editing a user account alone MUST NOT be the place that assigns product roles. Users that have book access but no product role for the current book MUST NOT be allowed to enter business application pages after login.

#### Scenario: Grant book without role is rejected
- **WHEN** an administrator attempts to add book access without a product role
- **THEN** the system rejects the grant with a validation error requiring a role

#### Scenario: Login with books but no role cannot enter business UI
- **WHEN** an authenticated user has one or more books but no product role membership for the active book
- **THEN** the system MUST NOT render the main application shell with empty menus as a silent success, and MUST instead block entry with a clear authorization error or force logout / guided remediation

#### Scenario: Switching books refreshes role
- **WHEN** a user switches to another authorized book
- **THEN** authorities and menus MUST reflect the product role bound to that book only

### Requirement: Viewer voucher read-only
查看员 MUST be able to open voucher list and voucher detail views, and MUST NOT be allowed to create, edit, delete, submit, approve, or reverse vouchers. The restriction MUST be enforced in the API as well as in the UI.

#### Scenario: Viewer opens voucher detail
- **WHEN** a 查看员 opens an existing voucher detail page
- **THEN** the page loads in read-only mode without create/edit/delete/approve actions

#### Scenario: Viewer write API is rejected
- **WHEN** a 查看员 calls a voucher write API (create, update, delete, approve, or reverse)
- **THEN** the API responds with an authorization failure and performs no mutation

### Requirement: Reviewer period close authority
审核员 MUST be allowed to perform period close and reverse-close operations that are exposed under the 结账 module. 做账员 and 查看员 MUST NOT receive 结账 menus or succeed on close / reverse-close APIs.

#### Scenario: Reviewer can close a period
- **WHEN** a 审核员 with an authorized book performs period close
- **THEN** the operation is permitted subject to existing business close rules

#### Scenario: Bookkeeper close API is rejected
- **WHEN** a 做账员 calls a period close or reverse-close API
- **THEN** the API responds with an authorization failure and performs no mutation
