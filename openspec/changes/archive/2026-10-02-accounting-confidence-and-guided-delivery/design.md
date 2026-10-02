## Context

See proposal.md. Existing Golden and scenario suites exercise most modules, but rule-derived reconciliation alone is not an independent oracle. The wizard maps unknown checks to its current step, refreshes only cached book lookup after closing, and the books pack defaults to the open period.

## Goals / Non-Goals

Goals: reuse business APIs and role routes; add a fixed reference dataset with human calculations; consolidate repeatable isolated acceptance evidence; make month-end handoff actionable.
Non-goals: migrations, tax-policy changes, new SaaS roles, backup redesign, production writes, deployment, or treating developer verification as independent accountant sign-off.

## Decisions

- Keep input events and fixed expected amounts in a standalone reference JSON and explanatory document, not generated from backend rules. Add a dedicated API scenario comparing the system to this reference; retain existing module tests as supplementary evidence.
- Run independent and blank-book suites as separate Playwright invocations using the existing isolated reset entry. Export JSON results and fail on skips to prevent green-but-unexecuted acceptance.
- Reuse dynamic router availability for guide buttons; unknown checks use explanatory text without fake navigation. Map known checks with a pure helper tested for actual server labels.
- Capture closedTerm before checkout and refresh book configuration on success. Pass closedTerm to report/delivery routes, which consume validated YYYY-MM parameters. Default ordinary books-pack export to the latest closed month.
- Provide a short operating manual and manual sign-off checklist; no fabricated live user evidence.

## Risks / Trade-offs

- Shared testing fixture → serialize isolated suites; never run against business databases.
- Route authorization is menu-level → backend remains the authority for all operations.
- Reference intentionally excludes tax-policy advice → entries are synthetic amounts, existing tax estimation behavior unchanged.
- UI route query may survive keep-alive → consumers watch valid period parameters where required.

## Migration Plan

No schema changes. Deploy through normal frontend release after validation; rollback frontend/tool/document changes with Git. This task does not deploy.
