## ADDED Requirements

### Requirement: System-initiated export for scheduled backups
The system SHALL allow a trusted server-side scheduler to export a book backup package using the same ZIP format and table scope as manual export, without requiring an interactive book-administrator session. Each such export SHALL still be audited as a system-initiated backup.

#### Scenario: Scheduler export produces restore-compatible ZIP
- **WHEN** the scheduler requests export for an enabled book
- **THEN** the system SHALL produce a ZIP accepted by the existing restore validation
- **AND** an audit event SHALL record the system-initiated export outcome
