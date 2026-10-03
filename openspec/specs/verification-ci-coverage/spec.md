# verification-ci-coverage Specification

## Purpose

让财务软件新增的证据核验与薪资事务保护在每次进入主分支前后都实际接受自动检查，并以全新受控测试库和可追溯的原始报告说明验证对象、数据库环境与执行结果，避免不完整流水线或历史报告被误认为当前候选已通过。

## Requirements

### Requirement: Required verification on both integration events
CI MUST execute the evidence verifier, acceptance runner, provenance and SQL offline boundary checks on both pull requests targeting main and pushes to main. It MUST also explicitly execute the real payroll transaction integration suite in an independent database-enabled job. Failed, skipped, empty or unfinished required executions SHALL prevent those jobs from succeeding; ordinary backend unit testing SHALL retain its database-free scope.

#### Scenario: Pull request validation
- **WHEN** a pull request targets main
- **THEN** both the offline verification checks and real payroll transaction integration run without depending on the push-only full accounting E2E condition

#### Scenario: Main branch validation
- **WHEN** a revision is pushed to main
- **THEN** the same required verification jobs execute for that checked-out revision

#### Scenario: Unexecuted required suite
- **WHEN** a required suite contains skipped cases, executes no cases, fails or stops before completion
- **THEN** its job reports failure and does not turn missing execution into a passing conclusion

### Requirement: Fresh isolated schema initialization
The payroll integration initializer MUST require explicit database connection settings and a fresh database in the integration suite's dedicated namespace. Before creating any database it SHALL validate the complete initialization plan and reject a pre-existing target. Initialization SHALL derive table structures and the required payroll index from the candidate's repository DDL without copying source data, invoking a reset initializer, installing seeds or executing destructive or cross-database statements. All initialized tables MUST be empty before the integration suite starts. Incomplete initialization SHALL remain a failure, without dropping, overwriting or silently reusing the target.

#### Scenario: Existing target database
- **WHEN** the requested payroll integration database already exists
- **THEN** initialization exits unsuccessfully before executing schema or data mutations against it

#### Scenario: Unsafe initialization plan
- **WHEN** an initialization plan contains a database reset, seed write, foreign database reference or unsupported schema operation
- **THEN** the initializer rejects the plan before database creation

#### Scenario: Fresh schema with production payroll index
- **WHEN** a valid absent target is initialized from the current repository schema
- **THEN** its tables contain zero rows and its payroll scope index has the candidate's required structure before owned integration fixtures are written

### Requirement: Observed database prerequisites
The payroll integration job MUST use an explicitly fixed MySQL 9.4.0 environment and record the observed server version, selected database, default transaction isolation, production payroll scope index and lock-observation prerequisites. A version mismatch, missing observation permissions, incompatible index or incorrect default isolation SHALL fail the job. Configuration values SHALL remain distinguishable from observed values; success SHALL NOT imply compatibility with untested MySQL versions.

#### Scenario: Configured version differs from observed version
- **WHEN** the database service is configured for 9.4.0 but the connected server reports another version
- **THEN** the job records the observation and fails instead of labelling the environment as validated 9.4.0

#### Scenario: Missing integration prerequisite
- **WHEN** the default isolation, required index or actual lock-observation permissions do not satisfy the integration suite's prerequisites
- **THEN** the job fails rather than skipping integration or replacing the real environment with mocked checks

### Requirement: Candidate bound original verification evidence
Each job SHALL record its actual full checkout SHA, before and after source fingerprints, event, run and attempt identities, execution start and completion status, commands and original report content hashes. Pull request source-head and tested merge revision identities MUST be recorded separately. Tool evidence MUST contain actual case outcomes; payroll evidence MUST include original integration test reports and validate their required suite and case coverage, failures, errors and skips. Stale reports, changed source state, missing report fields or incomplete execution MUST NOT qualify as a successful verification. Evidence upload SHALL run on successful and failed executions; absent required evidence SHALL remain an explicit failure. Local execution without CI identities MUST remain identified as local evidence.

#### Scenario: Pull request merge revision
- **WHEN** CI checks out a pull request merge revision that differs from the source branch head
- **THEN** evidence identifies the full tested checkout SHA and the source-head SHA separately without relabelling one as the other

#### Scenario: Previous report survives a failed launch
- **WHEN** an integration launch fails while an earlier passing report exists
- **THEN** the earlier report is not accepted as the current run's success and the failure evidence is retained

#### Scenario: Source changes during verification
- **WHEN** source bytes or source state change between collection and completion
- **THEN** the evidence records the change and does not report that an unchanged candidate passed

#### Scenario: Local verification without remote execution
- **WHEN** the same tools run locally without an actual CI run identity
- **THEN** their records identify local execution and do not invent remote run IDs, URLs or conclusions

### Requirement: Trusted release profile matches required CI scope
The trusted release evidence profile MUST name both new required verification jobs and their required execution steps using the workflow's actual reportable identities, while retaining existing required checks. Candidate CI evidence missing or skipping these jobs or steps SHALL fail the existing completeness assessment. Profile changes SHALL update its version or identity and dependent fixtures; historical CI records SHALL retain their original scope and candidate identity. Adding CI configuration or passing local verification MUST NOT by itself establish actual remote CI success, production runtime provenance, accountant sign-off, independent restoration or deployment authorization.

#### Scenario: Historical workflow lacks new jobs
- **WHEN** a candidate evidence bundle supplies CI output containing only the earlier backend, frontend and accounting jobs
- **THEN** the expanded trusted profile reports the missing verification jobs even if that historical run was green

#### Scenario: Configuration ready but remote run pending
- **WHEN** the workflow change and local validation are complete but the new revision has not executed remotely
- **THEN** implementation delivery can be recorded separately while actual candidate CI remains pending
