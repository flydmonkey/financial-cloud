## Purpose

Close the remaining print-delivery gaps for bookkeeping handoff: cash-flow statements print like the other core reports, and classic voucher print stays the only production print path—without introducing server-side PDF.

## ADDED Requirements

### Requirement: Cash flow statement printable
The system SHALL provide a print action on the cash flow statement page that opens a print-ready view of the currently displayed statement for the selected book and period. The printed content MUST include project names, line numbers, and the period amount columns shown on screen (period amount and year-to-date). Users MUST be able to complete printing or save as PDF through the browser print dialog. The action MUST use the same client-side table-print delivery channel already used by balance sheet and income statement pages.

#### Scenario: Print cash flow for loaded period
- **WHEN** an authorized user has loaded a cash flow statement for a book and period
- **AND** the user activates Print
- **THEN** the system SHALL open a print-ready view whose table rows match the currently displayed statement items
- **AND** the view SHALL identify the statement as a cash flow statement and show book and period context
- **AND** the browser print dialog SHALL be invokable from that view

#### Scenario: Print available alongside export
- **WHEN** the cash flow statement page is shown with data
- **THEN** a Print control SHALL be visible and enabled in the page actions
- **AND** Export SHALL remain available independently

### Requirement: Classic voucher print is the sole production path
Voucher print from the editor and from the voucher list batch action SHALL deliver through the classic voucher print static page. Production print entry points MUST NOT open an in-page iframe print path or rely on the obsolete over-page print layout inside the voucher editor.

#### Scenario: Editor print opens classic page
- **WHEN** a user prints from the voucher editor
- **THEN** the system SHALL open the classic voucher print page with the current voucher payload
- **AND** it MUST NOT inject a hidden iframe solely to print the editor DOM

#### Scenario: Batch print opens classic page
- **WHEN** a user selects one or more vouchers on the voucher list and chooses batch print
- **THEN** the system SHALL open the classic voucher print page with those vouchers
- **AND** each voucher SHALL paginate according to the classic print rules already used for single-voucher print

### Requirement: Orphan print generators are not production-reachable
Any alternate voucher HTML print generator that is not used by production print entry points MUST NOT remain as an ambiguous live path. It MUST either be removed or clearly marked non-production so maintainers do not wire new print buttons to it by mistake.

#### Scenario: No silent dual generators
- **WHEN** a maintainer inspects production print entry points after this change
- **THEN** only the classic static print page path SHALL be reachable from those entries
- **AND** any unused alternate generator SHALL be absent or explicitly documented as non-production
