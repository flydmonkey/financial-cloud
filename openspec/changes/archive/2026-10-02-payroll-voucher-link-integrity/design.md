## Context

See proposal.md for motivation. The controller already enforces current-book ownership and write roles. EmployeeSalaryService.update copies a DTO without voucher link fields into an entity whose two link fields use ALWAYS update strategy. delete removes detail rows without checking links. deleteVoucher ignores the Message returned by VoucherService.delete and clears a link even when the voucher cannot be deleted.

## Goals / Non-Goals

**Goals:** Enforce integrity in the service layer across direct editing, batch deletion and payroll voucher removal; preserve useful rejection reasons and normal draft correction paths.

**Non-Goals:** No tax algorithm or historical cascade changes, new payroll screens, generic payroll concurrency redesign, production data migration or automated clearing of historical stale links.

## Decisions

1. Load original salary rows before mutation and reuse SalaryVoucherDedupeRules.hasLinkedVoucher for protection. Reject both accrual-only and payment-only links. Preserving an ID while editing amount alone would still desynchronize payroll and its voucher; direct editing therefore rejects linked records. Adjustment continues through the existing preview/recalculate/push workflow after resolving eligible vouchers.
2. Validate all requested deletion IDs and their links before calling the batch mutation. Do not filter out linked rows and silently partially succeed. Missing rows return the existing record-not-found error.
3. Add a transaction to deleteVoucher. Validate type and selected link first, invoke the existing voucher service and return a non-success result without unlinking. On success update only the selected link; throw on unsuccessful unlink to roll back the deletion. The other link is not copied or cleared.
4. Keep current book guards at the controller and use stored ownership for voucher deletion. No schema or permission expansion. Blank or stale links never authorize deleting unrelated vouchers.
5. Use service mocks to verify no mutations on rejection, preserved return reasons, selected-link update and failed-unlink error. Extend the existing independent payroll E2E for actual posted rejection and unchanged detail/voucher/balances, plus draft removal. Existing isolation regressions cover controller ownership; do not write a second tax test duplicating the implementation.

## Risks / Trade-offs

- Historical links can reference deleted vouchers → this change makes payroll voucher removal reject missing vouchers and retain its link; existing generation can still clear stale links after checking voucher liveness. Audit or changes to that historical repair policy require separate evidence and scope.
- Concurrent generation and direct editing are not comprehensively serialized today → this change closes the observed sequential bypasses; a separate concurrency audit remains in the roadmap.
- Existing UI can submit a deletion that now rejects → provide an actionable message explaining the linked voucher; existing voucher status and period guards still determine permitted correction.
- Service mocks do not prove rollback against a real database fault → exercise Spring's actual transaction interceptor with a recording transaction manager for failure rollback and success commit; use isolated integration evidence for real voucher rejection and successful draft removal, and label the verification scope accurately.

## Migration Plan

No schema migration. Deploy with the normal backend release after required validation. Existing linked salary data is protected immediately. Rollback restores the former risky behavior and must be assessed before use; this change does not rewrite data or repair previously cleared links.
