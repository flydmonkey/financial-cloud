## Purpose

将财务软件候选版本与其构建、自动验收、实际持续集成、真实会计复核及独立实例恢复证据对应起来，提供可审阅的发布阻塞清单，避免将历史绿色结果、声明值或不完整测试范围误认为当前候选版本已经具备发布条件。

## ADDED Requirements

### Requirement: Candidate identity and evidence provenance
The evidence bundle MUST identify its schema version, candidate ID, full commit SHA, source cleanliness, source fingerprint, backend artifact hash and production frontend artifact hash. Every acceptance record SHALL identify the run, collection time, candidate, artifact identities, environment and referenced original evidence with content hashes. Missing identities or evidence collected for a dirty source state MUST prevent a release-ready result; historical evidence SHALL retain its original identity.

#### Scenario: Historical first iteration evidence
- **WHEN** a candidate at a committed revision references tests collected from an earlier revision with uncommitted changes
- **THEN** the verifier reports an identity mismatch and does not relabel those tests as evidence for the candidate

#### Scenario: Missing production artifact
- **WHEN** the bundle contains a backend artifact identity but no production frontend artifact identity
- **THEN** the verifier reports the missing frontend artifact as a release blocker

### Requirement: Observed execution environment binding
Acceptance evidence MUST distinguish observed runtime and datasource identities from supplied configuration values. A service URL, database name or local artifact hash alone SHALL NOT prove which artifact and datasource the running service used. Missing or inconsistent execution-binding evidence MUST block release readiness and identify the information needed to resolve the blocker.

#### Scenario: Declared database without runtime binding
- **WHEN** a report names an isolated database but has no reviewed execution evidence binding the tested API service to that datasource and candidate artifact
- **THEN** the verifier reports insufficient environment provenance even if all tests passed

### Requirement: Original report integrity and complete coverage
The verifier MUST check referenced evidence content hashes and validate the original reports rather than trust a passed flag or aggregate count. All required accounting groups and book-isolation scenarios in the candidate's trusted acceptance profile SHALL be represented. Reports MUST provide the required result fields and actual case identities; failures, skips, flaky results, retry-only passes, missing cases, duplicates or unfinished execution MUST prevent release readiness.

#### Scenario: Green summary with omitted cases
- **WHEN** a summary declares success but an expected accounting group or book-isolation case is absent from the original reports
- **THEN** the verifier reports incomplete coverage and blocks release readiness

#### Scenario: Altered report
- **WHEN** a referenced report's content differs from its recorded hash
- **THEN** the verifier identifies the altered evidence and does not accept its test results

#### Scenario: Missing fields and retry pass
- **WHEN** required report statistics are omitted or a case succeeds only after a failed attempt
- **THEN** the verifier reports invalid or failed evidence instead of treating omitted values as zero failures

### Requirement: Candidate specific external release evidence
Release readiness MUST require actual CI evidence for the candidate's full commit and required release checks, a reviewed real complete accounting month with the required sign-offs, and reviewed restoration into an independent target instance. External records SHALL identify their provenance, candidate, outcomes and referenced evidence hashes. Pending, failed, cancelled, unreviewed, differently versioned or insufficient-scope evidence MUST block readiness. Automatic test success SHALL NOT substitute for human sign-off.

#### Scenario: Smoke CI for another revision
- **WHEN** the only CI record is a passing smoke run or a complete run for another commit
- **THEN** the verifier reports insufficient scope or an identity mismatch and blocks release readiness

#### Scenario: Accountant sign-off remains pending
- **WHEN** automated acceptance passes but the real month or required reviewer signatures remain pending
- **THEN** the verifier lists the missing human evidence and does not generate signatures or declare release readiness

#### Scenario: Same instance restore
- **WHEN** restore evidence uses the same source and target backend instance despite different books or database names
- **THEN** the verifier identifies insufficient restoration scope and keeps independent instance restoration blocked

### Requirement: Reviewed release blocker closure
The evidence bundle MUST include a candidate-specific reviewed issue ledger reference and content hash, reviewer identity, review time and disposition of accounting differences and release-blocking issues. Any unexplained or accountant-unapproved amount difference, unresolved release blocker or missing issue review MUST prevent release readiness even when all other evidence passed. Non-blocking issues SHALL retain their assigned owner and planned resolution without being silently classified as closed.

#### Scenario: All checks pass with an open blocker
- **WHEN** acceptance, CI, sign-off and restoration evidence pass but a known amount error, cross-book access issue, lost restored data or inability to close remains unresolved
- **THEN** the verifier identifies the open release blocker and reports that the candidate is not release-ready

#### Scenario: Missing difference review
- **WHEN** a candidate has no reviewed issue ledger or an accounting difference lacks explanation and accountant approval
- **THEN** the verifier lists the missing review or unapproved difference as a blocker

### Requirement: Read only deterministic readiness assessment
The verifier SHALL accept a local evidence bundle and produce JSON and Markdown assessments with candidate identity, profile, overall readiness and every unmet condition. Each condition MUST state its status, evidence reference, reason and next action. The command MUST return a non-success result for blocked or invalid bundles. It SHALL NOT start services, connect to or reset business databases, alter source evidence, query external systems, sign on behalf of reviewers or deploy a release. Reading referenced files MUST remain within the resolved bundle root; report export SHALL occur only to explicitly selected output paths.

#### Scenario: Multiple missing evidence items
- **WHEN** CI, accountant sign-off and independent restoration evidence are all missing
- **THEN** one assessment lists all three blockers with next actions and returns a non-success result

#### Scenario: Complete reviewed evidence
- **WHEN** all required candidate, coverage, integrity, runtime binding and reviewed external conditions are satisfied
- **THEN** the assessment reports evidence readiness for that candidate without treating that result as deployment authorization

#### Scenario: Evidence path escapes bundle
- **WHEN** a referenced path resolves outside the evidence bundle root
- **THEN** the verifier rejects the reference without reading the external file
