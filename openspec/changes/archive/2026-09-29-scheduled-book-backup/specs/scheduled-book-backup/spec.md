## Purpose

Automatically persist book business backup packages on a schedule so bookkeeping firms retain recent offline copies without relying on manual export every day.

## ADDED Requirements

### Requirement: Scheduled backup of enabled books
The system SHALL, when scheduled backup is enabled by configuration, periodically export each enabled book's business data as the same backup ZIP format used by manual export, and write the files to a configured local directory.

#### Scenario: Cron run writes ZIP per enabled book
- **WHEN** scheduled backup is enabled and the schedule fires
- **THEN** the system SHALL create one backup ZIP per enabled book under the configured directory
- **AND** each ZIP SHALL be valid for the existing clone-style restore flow

#### Scenario: Disabled schedule does nothing
- **WHEN** scheduled backup is disabled in configuration
- **THEN** the schedule SHALL NOT write backup files

### Requirement: Retention of scheduled backups
The system SHALL retain only the most recent N scheduled backup files per book (N from configuration), deleting older matching files after a successful run.

#### Scenario: Excess files pruned
- **WHEN** a book already has more than N scheduled backup ZIPs after a new file is written
- **THEN** the system SHALL delete the oldest excess files for that book until at most N remain

### Requirement: Visibility and manual trigger
The system SHALL allow an authorized instance administrator to view the schedule configuration summary (enabled flag, cron, directory, retain count, last run outcome) and to trigger one backup cycle immediately without changing the cron expression.

#### Scenario: Admin views schedule status
- **WHEN** an authorized administrator requests scheduled-backup status
- **THEN** the system SHALL return enabled/cron/directory/retainCount and the last run timestamp and outcome summary

#### Scenario: Admin triggers immediate run
- **WHEN** an authorized administrator requests an immediate scheduled-backup cycle
- **THEN** the system SHALL run the same export-and-retain logic as the cron job
- **AND** the last-run outcome SHALL be updated
