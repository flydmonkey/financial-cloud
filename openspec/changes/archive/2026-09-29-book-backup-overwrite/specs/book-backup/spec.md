## ADDED Requirements

### Requirement: Overwrite restore with confirmation and pre-backup
The system SHALL allow an authorized book administrator to restore a validated backup package into an existing target book, replacing that book's business data, only when an explicit confirmation phrase is provided. Before any destructive write, the system SHALL persist a pre-overwrite backup ZIP of the target book to the configured backup directory. The overwrite SHALL keep the target `book_id` and existing membership grants, remap all other primary keys like clone restore, run in a single transaction, and reject sealed books.

#### Scenario: Overwrite requires confirmation phrase
- **WHEN** an overwrite restore is requested without the exact confirmation phrase
- **THEN** the system SHALL reject the request with zero writes to the target book

#### Scenario: Pre-backup written before wipe
- **WHEN** an authorized administrator requests overwrite restore with a valid package and confirmation phrase
- **THEN** the system SHALL first write a pre-overwrite backup ZIP of the target book to disk
- **AND** then replace the target book's in-scope business tables with remapped backup rows in one transaction

#### Scenario: Sealed book rejected
- **WHEN** the target book is sealed
- **THEN** the overwrite restore SHALL be rejected

#### Scenario: Failure rolls back business writes
- **WHEN** any insert fails during overwrite restore after wipe has begun
- **THEN** the transaction SHALL roll back so the target book's prior business data is restored
- **AND** the pre-overwrite ZIP file on disk MAY remain for manual recovery
