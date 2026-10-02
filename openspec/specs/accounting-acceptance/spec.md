## Purpose

为会计提供与系统计算规则独立的业务输入、手算预期与验收结果，使账簿、三表、跨期和更正流程能够依据可审阅证据核对，避免仅以测试数量或报表内部平衡认定产品成熟。

## Requirements

### Requirement: Independent accounting reference
Acceptance SHALL publish business events, debit/credit entries and fixed expected balances/profits independently of production report rules. Coverage SHALL include cross-month, year-end, reversal, reopening, receivables/payables, assets and payroll, with explicit automated versus manual evidence.

#### Scenario: Reference review
- **WHEN** an accountant reviews the acceptance pack
- **THEN** they can calculate expected amounts from the documented inputs without querying system report rules

### Requirement: Isolated acceptance execution
The acceptance runner MUST reject non-isolated database names, execute blank-book suites separately, and fail when required tests fail or skip. It SHALL save an auditable summary and individual suite results.

#### Scenario: Invalid environment
- **WHEN** the database name does not begin with financial_cloud_e2e_
- **THEN** acceptance exits before any reset or business write

#### Scenario: Fixed expected results
- **WHEN** the independent reference case runs in an isolated blank book
- **THEN** balances and report amounts are checked against fixed independently calculated amounts before and after closing and reopening

