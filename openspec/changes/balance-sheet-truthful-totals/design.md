## Context

Follow-up verification with two books using different accounting standards exposed a rule-read query without a book condition. Rule reads now use the authenticated current book, matching the existing write scope. The enterprise bad-debt regression checks rule ownership before and after switching books.

See proposal.md. Queries build totals in insertSubtotals and then reconcileGrandTotals; exports reuse the query result. The page merges assets and liabilities into table rows. Existing CI deliberately uses non-strict mode for pre-carry reports.

## Goals / Non-Goals

Goals: additive response metadata, truthful totals and a visible warning without making pre-carry report queries unavailable.
Non-Goals: changing accounting formulas, automatic repair, opening-year reconciliation, or migrating historical snapshots.

Follow-up authorized after live rerun: repair the small-enterprise R&D seed binding (5301 to 4301); require zero remaining source balances even when a carry record exists. E2E preparation creates supplemental carry vouchers for later business activity. Carry generation uses ledger balance (debit minus credit), closes each source on the opposite actual side, and balances the profit counterpart from the signed net. The full carry page offers supplemental generation. Existing historical snapshots remain untouched.

## Decisions

- Follow-up scope explicitly authorized by the user: all-module book isolation. `BookOwnershipGuard` validates server-allowlisted tables, complete DTO batches, nested IDs, original owners and stored references before controller mutations. It binds absent book scopes from the authenticated session. This avoids changing global tables into tenant tables or rewriting SQL for intentional target-book administration/backup operations.
- Switch/grant/role APIs validate active membership and target administration, including logically revoked grants. Existing-session financial access is checked on each request. Product role definitions remain fixed; shared-account writes require administration of all their active books. Salary formulas map the existing required book_id column and are isolated per book. Global tax/standard maintenance follows the existing product administrator policy.
- Generic orphan files remain uploader-owned global objects; files linked to vouchers/claims require the current business book. Linked deletion uses the business attachment service. Import paths with client-supplied IDs are checked before saving, and tax-deduction replacement is limited to the current book.
- No schema change, new dependency or automatic historical-data repair. Two-book regressions use fake records and full book snapshots in a database whose name starts with `financial_cloud_e2e_`.

- Keep strict mode opt-in; remove only the non-strict mutation. Making strict mode default would block useful pre-carry inspection.
- Add nullable balanced, assetTotal, liabilityTotal and balanceDifference fields to items. Use BigDecimal and the existing inclusive 0.01 tolerance. Missing grand totals leave metadata null.
- Use server metadata in the page warning, reset on reload/config mode, and put the warning inside printArea. Exported amounts remain truthful through the shared query; this slice does not add warning text to binary export templates.
- Update existing tests that assumed fabricated equality using actual accounting identities or explicit pre-carry differences, without weakening post-carry checks.

## Risks / Trade-offs

- Consumers assuming every report balances → document the changed behavior and inspect affected E2E assertions.
- Historical snapshots → existing query reconstructs subtotal rows; no bulk mutation of stored history.
- Missing UI/API services → run unit/type checks and retain an executable isolated UI regression if live testing cannot run.

## Migration Plan

Deploy backend and frontend together. No schema migration. For existing books, apply `sql/seed/rules/balance_sheet_research_subject_rule.sql` to repair only standard-1 R&D bindings; fresh initialization includes the same correction. No historical snapshot migration is included. Revert this change to roll back; leave original strict-mode configuration unchanged.
