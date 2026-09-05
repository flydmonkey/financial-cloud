## ADDED Requirements

### Requirement: Book administrators only may mutate book settings
Editing or deleting an account book MUST require the administrator product role for that book. Non-administrators who can see the book MUST NOT receive mutate controls for that book and MUST be denied by the API if they call update or delete.

#### Scenario: Non-admin cannot update book
- **WHEN** a bookkeeper, reviewer, or viewer of a book calls book update for that book
- **THEN** the system denies the operation

#### Scenario: Non-admin UI hides mutate actions
- **WHEN** a non-administrator views the book list for books they can access
- **THEN** edit, delete, and member-management actions for those books are not shown
