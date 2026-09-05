# book-member-auth Specification

## Purpose

Lets each account-book administrator invite other registered users into that book and assign a product role at grant time, moving collaboration onto a book-centric authorization path.

## Requirements

### Requirement: Book administrator invites registered users
Within account-book management, a user who holds the administrator product role for the current book MUST be able to search registered users and grant them access to that book. Users who are not administrators of the current book MUST NOT be allowed to grant book access.

#### Scenario: Admin grants access
- **WHEN** a book administrator selects a registered user and confirms a book grant for the current book
- **THEN** the invited user gains access to that book on next login or book switch

#### Scenario: Non-admin cannot grant
- **WHEN** a bookkeeper, reviewer, or viewer of the current book attempts to grant book access
- **THEN** the system denies the operation

### Requirement: Role selected when granting book access
Granting book access MUST require selecting exactly one product role for that book among 管理员, 做账员, 审核员, and 查看员. The grant MUST bind both book access and the book-scoped role membership together.

#### Scenario: Grant without role rejected
- **WHEN** a book administrator attempts to grant book access without selecting a product role
- **THEN** the system rejects the grant with a validation error requiring a role

#### Scenario: Invited user gets book-scoped role
- **WHEN** a book administrator grants a user access as 审核员 for book A
- **THEN** that user receives 审核员 authorities and menus only while book A is the active book

### Requirement: Book member list and revoke
Book administrators MUST be able to view members of the current book (user identity and product role) and revoke a member's access. Revoking MUST remove both book access and the book-scoped role for that book.

#### Scenario: Revoke member
- **WHEN** a book administrator revokes a member from the current book
- **THEN** that member can no longer open the book and no longer holds a role for that book

### Requirement: User-admin book grant is not the primary path
The primary product path for inviting collaborators MUST be account-book management. Administrator user-profile book configuration MUST NOT remain the recommended primary UX for inviting collaborators in this product model (it MAY be hidden or demoted).

#### Scenario: Collaboration via book management
- **WHEN** a book administrator needs to add a collaborator
- **THEN** the workflow is completed from account-book management without requiring the platform user-admin book drawer as the primary step

### Requirement: Book administrators only may mutate book settings
Editing or deleting an account book MUST require the administrator product role for that book. Non-administrators who can see the book MUST NOT receive mutate controls for that book and MUST be denied by the API if they call update or delete.

#### Scenario: Non-admin cannot update book
- **WHEN** a bookkeeper, reviewer, or viewer of a book calls book update for that book
- **THEN** the system denies the operation

#### Scenario: Non-admin UI hides mutate actions
- **WHEN** a non-administrator views the book list for books they can access
- **THEN** edit, delete, and member-management actions for those books are not shown
