## Purpose

Allow book administrators to recover accounting data and attachment contents from a self-contained backup even when source records are absent, while retaining authorization checks for legacy backups and rejecting corrupt packages before business data is changed.

## Requirements

### Requirement: Self-contained attachment backup
New backups SHALL contain all referenced live voucher and expense attachment bytes, metadata, sizes and checksums. Missing source files MUST fail export rather than produce an incomplete successful backup.

#### Scenario: Missing attachment
- **WHEN** a referenced file no longer exists during export
- **THEN** export fails with an explicit incomplete-attachment error

### Requirement: Independent and isolated recovery
An authorized administrator SHALL recover a v2 package without source book or file records. Restored attachments MUST use new independent file records and match packaged bytes. Legacy v1 recovery MUST retain source ownership authorization.

#### Scenario: Source absent
- **WHEN** a valid v2 backup is restored after source files are removed
- **THEN** book data and attachments are restored using package contents

#### Scenario: Attachment deletion isolation
- **WHEN** an attachment in the restored book is deleted
- **THEN** the original book's attachment remains accessible

### Requirement: Validate before mutation
Restore MUST reject missing or mismatched attachment declarations, checksums, duplicate ZIP entries, unsafe paths and decompression limits before creating or overwriting book data. Export MUST respect the same package limits.

#### Scenario: Corrupt content
- **WHEN** an attachment's bytes differ from its declared checksum
- **THEN** restore fails before any book or file insertion
