## MODIFIED Requirements

### Requirement: Clone-style restore with ID remapping
The system SHALL restore a validated backup as a NEW book (new `book_id`) for the default clone restore path. All primary keys SHALL be reassigned and every foreign-key edge enumerated in the design's table specification SHALL be remapped to the new IDs within a single transaction. Personnel and department references (auditor, poster, manager, department) SHALL be nulled on restore.

#### Scenario: Round-trip fidelity
- **WHEN** a book is exported and then restored
- **THEN** the restored book SHALL contain the same voucher count and the same subject-balance monetary totals as the source book
- **AND** all voucher items, auxiliaries, journal entries, assets and salary rows SHALL reference the restored book's new IDs

#### Scenario: Failure rolls back completely
- **WHEN** any insert fails during restore
- **THEN** the whole transaction SHALL roll back including the new book shell

#### Scenario: Existing books untouched
- **WHEN** a clone restore completes
- **THEN** no pre-existing book's rows SHALL have been modified or deleted

#### Scenario: Attachment restoration uses independent files
- **WHEN** a legacy v1 backup contains attachment file references available on the same instance
- **THEN** the operator SHALL administer every source book associated with each referenced file before any restore writes
- **AND** restored attachments SHALL reference independent copies of the file rows, so deleting restored attachments cannot change source files
- **AND** missing files or unverifiable source ownership SHALL reject restoration; legacy v1 does not embed attachment binaries for transfer to another instance

#### Scenario: Missing scope and unknown report references
- **WHEN** an imported book-scoped row omits book_id or contains an unmapped report header reference
- **THEN** book_id SHALL be bound to the destination and only the explicit template sentinel may survive without remapping
