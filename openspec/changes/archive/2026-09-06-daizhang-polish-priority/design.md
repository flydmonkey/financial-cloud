## Context

See proposal.md for product identity (代账 × 小企业准则 × 打磨现有) and priority v0.1. This design covers **how** to implement P0 `settlement-uncheckout`, including the journal opening snapshot column. P1–P3 remain roadmap only.

Current checkout (`SettlementService.checkout`) for term T:

1. Persist CF ending balance on a new `settlement` row
2. Generate/save BS and IS snapshots (month; quarter/year when applicable)
3. Copy monthly `statement_subject_balance` from T into new rows for T+1
4. `UPDATE journal_account SET opening_balance = balance` (lossy today)
5. Insert settlement; advance `currentTerm` to T+1

Voucher guards already block edit/unpost in closed periods via current-term comparison. `settlement` uses `@TableLogic`. Income save path fails with「本期报表数据已生成」if a header already exists—uncheckout must clear IS snapshots or re-checkout breaks.

## Goals / Non-Goals

**Goals:**

- Invert checkout side effects for the immediately previous closed month only
- Make journal openings reversible via `prev_opening_balance`
- Keep permissions aligned with checkout; rely on typed confirmation
- Preserve vouchers; leave reverse-post / recreate carry-forward to existing flows

**Non-Goals:**

- Multi-period cascade uncheckout
- Soft-delete vs new settlement status workflow beyond logic delete
- Snapshot table for journal history (column is enough for last checkout)
- Implementing P1 print/pack or P2 bank rec / ARAP in this design
- Fixing all carry-forward enterprise-code purity in the same PR (tracked as P0.6 follow-up tasks)

## Decisions

### D1: API `POST /api/settlement/uncheckout`

- Body/query may include `yearPeriod` but server MUST enforce it equals `currentTerm - 1 month`
- Prefer POST (destructive); do not copy checkout's GET style
- `bookId` from `@CurrentUser`

**Alternatives:** GET like checkout — rejected (unsafe caching/semantics).

### D2: Guard set (hard reject, no partial apply)

- Settlement row exists for T
- T == previous month of current term C
- No vouchers in C
- No `journal_entry` with `tradeDate` in C
- If book has journal accounts, all must have non-null `prev_opening_balance` (else legacy unsafe)

### D3: Transaction order

```
validate guards
delete subject_balance where year_period = C (month)
restore journal opening_balance from prev_opening_balance
delete IS (+ conditional Q/Y) for year_period = T
delete BS snapshots for year_period = T (header+items as applicable)
logic-delete settlement T
updateCurrentTerm(T)
structured log success
```

All in `@Transactional`; any failure rolls back and logs reason.

### D4: Journal column `prev_opening_balance`

- Add nullable decimal column on `journal_account`
- Checkout: `prev := opening; opening := balance` (single SQL or two-step)
- Uncheckout: `opening := prev` (optionally clear prev)
- Rejected alternative: `settlement_journal_snapshot` table — overkill while only last month is reversible

### D5: Settlement delete = logic delete

Amends earlier「物理删」idea; matches `@TableLogic` and keeps audit row soft-deleted.

### D6: UI

- Primary: settlement list row action when `yearPeriod == currentTerm - 1` and status closed
- Modal: type `YYYY-MM` to enable confirm
- On success: refresh book term in store; toast new current term

### D7: Permission

Same as checkout (menu capability). No new role matrix in P0.

### D8: Roadmap layering (not implemented here)

| Phase | Focus |
|-------|--------|
| P0 | This design |
| P1 | Print, 账本包, onboarding/opening UX, checklist honesty |
| P2 | Bank reconciliation, ARAP write-off/aging, attachments, tax accrue wizards |
| P3 | Adjacent modules safety, lint/TS, amortisation light-touch, broader audit logs |

## Risks / Trade-offs

- **[Risk] Legacy closed months lack `prev_opening_balance`** → Mitigation: reject uncheckout with explicit message; only periods checked out after migration are reversible.
- **[Risk] Subject-balance rows for C not only from checkout** → Mitigation: voucher/journal guards; optional extra check that C rows have zero current activity / `is_voucher = n`.
- **[Risk] BS/IS delete semantics incomplete vs how headers are stored** → Mitigation: mirror period keys used in checkout (`isQuarterReportMonth` / `isYearReportMonth`); add E2E re-checkout after uncheckout.
- **[Risk] Same-permission uncheckout is powerful** → Mitigation: typed period confirmation + structured logs; defer split roles.
- **[Trade-off] Column vs snapshot table** → Column wins for P0; revisit if multi-month uncheckout is ever allowed.

## Migration Plan

1. Ship SQL/patch: add `journal_account.prev_opening_balance` (nullable)
2. Deploy checkout that writes the column (backward compatible)
3. Deploy uncheckout API + UI
4. Rollback: disable UI/API route; column can remain; do not remove column in emergency rollback

## Open Questions

- Whether to clear `prev_opening_balance` after restore or leave until next checkout overwrites (either is fine; prefer leave-until-overwrite for simpler SQL)
- Exact structured log sink (application logger vs existing system log table) — pick whatever the codebase already uses for operational audit
