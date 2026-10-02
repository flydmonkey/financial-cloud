## ADDED Requirements

### Requirement: Authorized current book
The system SHALL verify active membership before switching books and before accessing current-book financial data. Revoked grants SHALL not authorize an existing session or token refresh. Creation/onboarding, accessible-book selection, and explicit global read endpoints SHALL remain available as appropriate without a current book.

#### Scenario: Viewer selects an unassigned book
- **WHEN** a viewer with access only to A requests a switch to B
- **THEN** the request is denied and the current book remains A

#### Scenario: Grant is revoked
- **WHEN** the user's A grant is logically deleted while its session still selects A
- **THEN** financial access and token refresh fail until a legitimate book is selected

### Requirement: Target-book administration
Legacy book-grant and role-member APIs SHALL validate target-book administration before any writes. Global accounts shared across books SHALL not be taken over by an administrator of only one of their books. The four product role definitions SHALL not be changed into other product roles through application APIs.

#### Scenario: Self-grant through legacy API
- **WHEN** A's viewer requests an administrator grant for B or a privileged role membership
- **THEN** the request fails and B's grants and roles do not change

### Requirement: Original and associated record ownership
Business object reads and writes SHALL verify original record ownership before execution. The complete batch and nested related IDs SHALL be checked before mutation. Missing query book scopes SHALL use the authenticated current book; forged financial book scopes SHALL either be rejected or be overwritten by an already scoped read endpoint. Existing records SHALL not move between books. Stored references SHALL also be checked before detail reads or balance-affecting operations.

#### Scenario: Foreign primary or nested ID
- **WHEN** A sends B's record, account, employee, department, asset category, subject, voucher, rule or configuration ID
- **THEN** it cannot read or change B's object or balance

#### Scenario: Mixed batch
- **WHEN** a mutation combines valid A IDs and B IDs
- **THEN** the entire mutation fails and both books' records remain unchanged

### Requirement: Templates, files and global data
Default voucher-template lists SHALL be scoped to the current book. Explicit standard-template reads SHALL remain available while book-template mutations SHALL not change standard or other-book templates. Generic file APIs SHALL authorize linked files using business-book ownership and orphan files using uploader ownership. Linked file deletion SHALL go through the owning business attachment API. Global tax and standard maintenance writes SHALL require the existing product administrator role and SHALL not be classified as tenant rows.

#### Scenario: File access bypass
- **WHEN** A attempts a generic read or deletion of B's attached file
- **THEN** it is denied even if its uploader account can access both books

#### Scenario: Read-only global configuration access
- **WHEN** a viewer reads global tax rates or current-book formulas and attempts to modify them
- **THEN** the read remains available and the write fails
