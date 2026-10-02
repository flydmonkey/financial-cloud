## Purpose

为小微企业会计提供从建账、期初导入、录凭证到月结与交付的可操作指引，解释阻塞原因并连接实际处理入口，确保结账后用户仍可明确核对和交付刚完成的账期。

## ADDED Requirements

### Requirement: Accounting workflow guide
The homepage SHALL display ordered setup, opening import, voucher, closing and delivery guidance, with links only to available authorized routes. Guidance MUST NOT claim completion without business evidence.

#### Scenario: Authorized guide
- **WHEN** a user opens the homepage
- **THEN** the guide explains each stage and exposes only routes available to that user

### Requirement: Actionable closing checks
Closing checks SHALL show a cause or processing suggestion. Known failed checks SHALL link to the appropriate wizard step or business page; unknown checks MUST NOT offer a link that returns to the same check.

#### Scenario: Receivables blocker
- **WHEN** an applicable receivables check fails
- **THEN** the user can open the aging page to investigate

### Requirement: Closed-period delivery
Successful closing SHALL display the closed period, refresh the current period, and provide report review and books-pack delivery for the closed period. Reopening SHALL invalidate previous successful verification.

#### Scenario: December closing
- **WHEN** December is closed and the current period advances to January
- **THEN** review and delivery still target December rather than the new January
