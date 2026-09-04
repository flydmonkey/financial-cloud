## MODIFIED Requirements

### Requirement: Aging analysis buckets
The system SHALL provide aging analysis for AR and AP as of a selected as-of date, bucketing each counterpart’s **open remaining** balance into at least: within 30 days, 31–60, 61–90, 91–180, and over 180 days. When write-off data exists for the book, aging SHALL allocate open remainders by the original open-item dates (document/voucher dates of the remaining layers). When no write-off activity exists for a counterpart, the system MAY fall back to the prior FIFO estimate from posted movements and MUST label the method in API/UI.

#### Scenario: Aging returns standard buckets
- **WHEN** the user runs AR aging as of date D
- **THEN** each counterpart row SHALL include amounts in the standard buckets and a total equaling the open ending balance at D

#### Scenario: Bucket totals reconcile to ending balance
- **WHEN** aging is computed for a counterpart
- **THEN** the sum of bucket amounts for that counterpart SHALL equal that counterpart’s open AR or AP ending balance as of D

#### Scenario: Zero balance counterparts may be omitted
- **WHEN** a counterpart’s open ending balance as of D is zero
- **THEN** the system MAY omit the row from the default aging list

#### Scenario: Open-item aging preferred after write-off
- **WHEN** write-off matches exist for a counterpart
- **THEN** aging buckets SHALL be based on remaining open layers
- **AND** MUST NOT ignore write-offs by re-estimating from gross movements alone
