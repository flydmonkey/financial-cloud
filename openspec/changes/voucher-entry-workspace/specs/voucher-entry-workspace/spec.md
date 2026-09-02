## Purpose

Provide accountants with a full-page continuous voucher workspace: stay after save, explicit new, prev/next by date and word number, WYSIWYG layout matching classic print, keyboard coding entry, and list used only for search and batch review.

## ADDED Requirements

### Requirement: Stay on voucher after save or draft
When the user saves or drafts a voucher, the system SHALL keep the user on that voucher’s entry page with the saved data loaded for review. The system MUST NOT automatically navigate to the voucher list, and MUST NOT automatically start a new blank voucher, solely because save or draft succeeded.

#### Scenario: Save success stays on page
- **WHEN** the user saves a valid voucher
- **THEN** the system SHALL remain on the voucher entry page
- **AND** the displayed voucher SHALL reflect the saved identity and fields (including assigned id / word number)

#### Scenario: Draft success stays on page
- **WHEN** the user drafts a voucher
- **THEN** the system SHALL remain on the voucher entry page
- **AND** the draft SHALL remain editable without jumping to the list

### Requirement: Explicit new voucher action
The system SHALL provide an explicit “新建凭证” action that clears the form for a new voucher and allocates a new voucher number according to existing numbering rules. Completing save or draft MUST NOT by itself start a new blank voucher.

#### Scenario: New voucher only on explicit action
- **WHEN** the user clicks “新建凭证”
- **AND** any dirty-guard confirmation (if required) succeeds
- **THEN** the system SHALL show a blank entry form ready for a new voucher
- **AND** a new available word number SHALL be prepared for the current book and period rules

#### Scenario: Save does not auto-create next blank
- **WHEN** the user saves the current voucher successfully
- **THEN** the system SHALL NOT replace the form with a new blank voucher unless the user later triggers “新建凭证”

### Requirement: Previous and next by date and word number
The system SHALL allow navigation to the previous and next voucher in the same book ordered by voucher date ascending, then word head, then word number ascending. At either end of the sequence, the corresponding control SHALL be disabled or non-operative.

#### Scenario: Open next voucher
- **WHEN** the user is viewing a saved voucher that has a next voucher in date+word order
- **AND** the user activates “下一张”
- **AND** dirty-guard confirmation (if required) succeeds
- **THEN** the system SHALL load and display that next voucher

#### Scenario: Open previous voucher
- **WHEN** the user is viewing a saved voucher that has a previous voucher in date+word order
- **AND** the user activates “上一张”
- **AND** dirty-guard confirmation (if required) succeeds
- **THEN** the system SHALL load and display that previous voucher

#### Scenario: Boundary of sequence
- **WHEN** there is no previous or no next voucher in date+word order
- **THEN** the corresponding navigation control SHALL be unavailable or non-operative
- **AND** the current voucher SHALL remain displayed

### Requirement: Discard-without-save dirty guard
If the entry form has unsaved changes, before navigating to another voucher, starting a new voucher, or returning to the list, the system SHALL prompt with at least the choices to leave without saving or to cancel. The system MUST NOT require the user to save before leaving. Concurrent edit conflict prompts for other users are NOT required in this change.

#### Scenario: Leave without saving
- **WHEN** the form has unsaved changes
- **AND** the user activates “下一张”
- **AND** the user chooses to leave without saving
- **THEN** the system SHALL load the next voucher
- **AND** the unsaved changes on the previous form SHALL be discarded

#### Scenario: Cancel keeps edits
- **WHEN** the form has unsaved changes
- **AND** the user activates “新建凭证”
- **AND** the user cancels the dirty prompt
- **THEN** the current form data SHALL remain unchanged

### Requirement: WYSIWYG classic entry layout with unbounded entry rows
The voucher entry workspace SHALL be a full-page UI whose structure visually matches the classic printed voucher (title, date, unit, short word number such as `记 1 号`, attachments, entry grid, total with Chinese uppercase amount, remarks, signature labels). Entry rows SHALL grow beyond six lines on screen so the user can keep entering on one page. Printing SHALL continue to paginate according to the classic print rules (including six lines per printed page). Subject entry SHALL support code typing and Enter as the primary path. The header area SHALL surface loan balance and voucher status in a way suitable for accountants.

#### Scenario: Key regions visible without print mode
- **WHEN** the user opens the voucher entry workspace
- **THEN** the page SHALL show the classic structural regions without requiring print mode
- **AND** editable controls SHALL remain usable with keyboard Tab order through the entry grid

#### Scenario: More than six entry lines on screen
- **WHEN** the user adds more than six journal lines
- **THEN** the entry workspace SHALL keep all lines editable on the scrolling page
- **AND** printing the voucher SHALL still split lines across classic print pages as defined by the print capability

#### Scenario: Printing remains available
- **WHEN** the user prints from the entry workspace
- **THEN** the system SHALL open the classic print presentation for the current voucher data

### Requirement: Full-page entry only; list is for search and batch work
Opening a voucher from the list or starting a new voucher SHALL navigate to the full-page entry workspace. The system MUST NOT use a modal dialog as the primary edit surface. The voucher list remains for finding a voucher, batch review/posting workflows, and exception filtering. Returning to the list SHALL be an explicit action.

#### Scenario: Open from list goes full page
- **WHEN** the user opens a voucher from the list
- **THEN** the system SHALL navigate to the full-page entry workspace for that voucher
- **AND** SHALL NOT present the primary editor inside a modal dialog

#### Scenario: Explicit back to list
- **WHEN** the user chooses “返回列表”
- **AND** dirty-guard confirmation (if required) succeeds
- **THEN** the system SHALL navigate to the voucher list page
