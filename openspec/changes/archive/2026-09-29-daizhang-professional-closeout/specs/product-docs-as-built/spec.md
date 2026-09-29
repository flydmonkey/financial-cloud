## ADDED Requirements

### Requirement: Professional-usable program closeout documented
The product documentation SHALL state that the bookkeeping-firm professional-usable acceptance bar is met and the autonomous iteration queue for that program is closed. The homepage dashboard SHALL be documented as fulfilling the boss-lite reporting surface (no separate boss-report page required). Remaining items SHALL be classified as Non-goal or Post-V1 enhancement, not as open blockers for professional-usable delivery.

#### Scenario: Backlog shows closed program
- **WHEN** a reader opens the autonomous iteration backlog
- **THEN** the professional-usable queue SHALL be marked completed/closed
- **AND** only Non-goal or Post-V1 candidates remain listed

#### Scenario: Boss-lite fulfilled by homepage
- **WHEN** product docs describe boss-lite / 老板极简报表
- **THEN** they SHALL record the homepage dashboard as the delivered surface
- **AND** SHALL NOT list a separate boss-report page as an open gap for V1 professional-usable
