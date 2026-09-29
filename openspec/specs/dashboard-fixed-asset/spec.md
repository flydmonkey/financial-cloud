# dashboard-fixed-asset Specification

## Purpose

Give bookkeeping users a homepage glance at fixed-asset scale for the current book.

## Requirements

### Requirement: Homepage fixed-asset summary card
The system SHALL expose current-book fixed-asset summary statistics for the homepage dashboard: count of non-disposed asset cards, split of in-use vs suspended, sum of original value, and sum of net book value. The homepage SHALL render a dedicated card with these figures and a navigation affordance to the fixed-asset card list.

#### Scenario: Summary for book with assets
- **WHEN** an authorized user opens the homepage dashboard for a book that has fixed-asset cards
- **THEN** the API SHALL return non-disposed total count and original/net value sums
- **AND** disposed assets SHALL be excluded from the counts and sums

#### Scenario: Empty book
- **WHEN** the book has no non-disposed assets
- **THEN** the API SHALL return zeros without error
- **AND** the homepage card SHALL still render
