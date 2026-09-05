## Purpose

Closes the gap between menu-based authorization and server-side enforcement so product roles actually protect APIs, role resolution fails closed, and leftover super-identity bypasses stop being the default product path.

## ADDED Requirements

### Requirement: Menu-visible modules enforce roles on APIs
For modules included in the product role menu matrix, the system MUST reject unauthorized write and sensitive operations at the API when the caller's current-book product role does not allow that operation. Hiding a control in the UI MUST NOT be the only control.

#### Scenario: Viewer cannot mutate vouchers via API
- **WHEN** a user whose active book role is 查看员 calls a voucher create or update API
- **THEN** the system denies the request

#### Scenario: Bookkeeper cannot checkout period via API
- **WHEN** a user whose active book role is 做账员 calls period checkout or other close-period APIs
- **THEN** the system denies the request

### Requirement: Role resolution fails closed without active book
When resolving product roles for menus or authorities, the system MUST require a valid active book context for book-scoped membership. The system MUST NOT merge roles from multiple books because `bookId` is missing.

#### Scenario: Missing bookId yields no book-scoped product roles
- **WHEN** an authenticated user has no active book id
- **THEN** book-scoped product roles are not granted from any book membership
- **AND** business menus that require a product role are not issued for those roles

#### Scenario: Empty menu fail-closed UX
- **WHEN** an authenticated user with an active book has no product role for that book
- **THEN** the client presents a clear no-access / contact-admin state and MUST NOT loop into a generic 404 for missing home routes

### Requirement: Legacy super-identity is not the product bypass
Product authorization checks for ordinary book users MUST be based on the four product roles for the active book. Legacy identities such as platform supervisor codes MUST NOT unconditionally bypass those checks for book business APIs in the product path.

#### Scenario: Bookkeeper is not treated as super admin
- **WHEN** a user holds only 做账员 for the active book
- **THEN** front-end and back-end product checks treat them as 做账员
- **AND** do not grant administrator-only book settings or role-management capabilities
