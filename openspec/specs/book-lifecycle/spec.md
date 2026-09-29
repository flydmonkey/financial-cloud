# book-lifecycle Specification

## Purpose

Define book lifecycle guards for delete vs seal retain so client books with history are not hard-deleted.

## Requirements

### Requirement: Books with business data cannot be hard-deleted
The system SHALL refuse hard-delete of a book that still has voucher rows for that `book_id`. The refusal SHALL instruct the operator to seal (archive retain) the book instead. Empty books (no vouchers) SHALL continue to require disable-before-delete and MAY be removed after disable. Sealed books SHALL remain undeletable.

#### Scenario: Delete rejected when vouchers exist
- **WHEN** an administrator attempts to delete a disabled book that has one or more vouchers
- **THEN** the system SHALL reject the delete with zero row removals
- **AND** the error SHALL indicate sealing/retain instead of delete

#### Scenario: Empty disabled book can be deleted
- **WHEN** an administrator deletes a disabled book with zero vouchers
- **THEN** the system SHALL allow the existing cascade delete path

#### Scenario: Active book still blocked
- **WHEN** an administrator attempts to delete an active book
- **THEN** the system SHALL still require disable before delete
