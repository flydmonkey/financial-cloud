# admin-menu-slim Specification

## Purpose

Slips the product menu surface to a SaaS collaboration shape by hiding deployment-style system and audit menus while keeping role management available under system settings.

## Requirements

### Requirement: System settings keeps only role management
Under 系统设置, the product MUST expose 角色管理 and MUST NOT expose other former system-settings modules in the authenticated menu (including but not limited to user/org administration as system modules, security policy, session admin, and institution settings menus that previously lived under this group—except where those capabilities are relocated by other requirements). Role management MUST remain nested under 系统设置.

#### Scenario: Menu shows role management only under system settings
- **WHEN** an administrator loads menus for a book where they have system settings access
- **THEN** 系统设置 children include 角色管理 and do not include the removed sibling modules

### Requirement: Audit log menus hidden
The product MUST NOT expose 日志审计 or any of its child modules in authenticated menus.

#### Scenario: No audit menus
- **WHEN** any authenticated user loads menus
- **THEN** the response does not include 日志审计 or its former children

### Requirement: Hidden modules keep code but disable API entry
Backend implementations for the hidden system-settings modules (except role management) and for log-audit modules MUST remain in the codebase for this phase. Their HTTP API entry points MUST be commented out or otherwise disabled so they are not part of the supported surface, without deleting the underlying implementation classes.

#### Scenario: Disabled endpoint is unavailable
- **WHEN** a client calls a formerly exposed endpoint belonging to a hidden module whose entry has been commented out
- **THEN** the call is not served as a normal success path for that feature (for example no matching handler or equivalent unavailable response)

#### Scenario: Role management APIs remain available
- **WHEN** an authorized administrator uses role management APIs
- **THEN** those APIs remain available and functional
