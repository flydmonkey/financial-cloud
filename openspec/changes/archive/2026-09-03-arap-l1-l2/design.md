## Context

See `proposal.md` for motivation. Assist masters (`assist_acc`, types 客户=2 / 供应商=3) and `voucher_auxiliary` on posted voucher lines already exist; Fund dashboard only shows coarse AR/AP aggregates. Month-end close (`month-end-close`) currently treats 往来 as manual placeholder after the professional month-end change. No write-off subledger exists—balances must be reconstructed from GL lines with assist tags on receivable/payable subjects (小企业科目经 `SubjectCodeCompat`，如 1122/2202 及准则别名).

## Goals / Non-Goals

**Goals:**

- Query/export AR & AP by counterpart assist: balance, detail, statement Excel, aging buckets.
- Wire month-end verify to real AR/AP + aging summary (warning on overdue, not hard block).
- Reuse existing Excel export patterns and voucher/auxiliary storage; no new business document tables for invoices/receipts.

**Non-Goals:**

- L3 核销 matching, bank recon, inventory, changing assist CRUD, multi-currency.

## Decisions

### D1: Reconstruct from posted voucher auxiliaries

**Choice:** Compute balances by summing posted voucher item amounts joined to `voucher_auxiliary` for customer/supplier on subjects classified as AR or AP (subject code family + book subject flags if present).

**Alternatives:** Separate AR/AP subledger tables—rejected for L1 (duplicate source of truth). Invoice-centric docs—out of scope.

### D2: Subject classification

**Choice:** Start with code allowlist via `SubjectCodeCompat` for 应收账款/其他应收/应付账款/其他应付 families used by 小企业; include leaf subjects under those roots. Document mapping in service constants; extend if book uses custom detail codes under the same roots.

**Alternatives:** Only subjects with assist enabled—may miss misconfigured books; combine allowlist ∩ assist-enabled subjects for inclusion filter.

### D3: Aging allocation

**Choice:** As-of date ending balance per counterpart; allocate remaining balance to voucher lines FIFO by voucher date (oldest open debit/credit first depending on AR vs AP normal balance). Buckets: 0–30, 31–60, 61–90, 91–180, 180+.

**Alternatives:** Age entire ending balance by last movement date only—simpler but less accurate; reject for “账龄分析” expectation. True open-item after 核销—deferred to L3.

### D4: Month-end integration

**Choice:** Add verify item(s) calling the same Arap service; `hard=true` or dedicated `warning` with `result=true` when overdue exists so checkout hard-gate filter (`hard && applicable && !result`) does not block. UI: move 往来 off manual checklist into system verify table; keep bank/存货/税控 manual.

**Alternatives:** Hard-fail on any overdue—too harsh without 核销 workflow.

### D5: API & UI shape

**Choice:** REST under `/api/arap/...` (balance, detail, aging, statement/export); frontend routes under a 往来 menu (应收余额/明细、应付余额/明细、账龄、对账单). Permissions mirror book-scoped report read.

## Risks / Trade-offs

- **[Risk] Custom subjects outside allowlist omitted** → Mitigation: include children of standard roots; allow config follow-up.
- **[Risk] FIFO aging ≠ accountant mental model without 核销** → Mitigation: label method in UI (“按凭证日期先进先出估算，非核销账龄”); L3 later.
- **[Risk] Performance on large voucher sets** → Mitigation: SQL aggregation by book+period; index-friendly filters; page balance lists.
- **[Trade-off] Warning-only overdue** → 代账 can still close; risk of ignoring aging—copy in verify reason.

## Migration Plan

1. Ship Arap APIs + UI without forcing menu for tenants lacking assist data.
2. Deploy month-end verify/UI change with backend; old placeholder copy removed.
3. No DB migration required for v1 (read models only).
4. Rollback: revert services/UI; month-end falls back only if both reverted together.

## Open Questions

- Exact assist_type string/code values in DB seeds (2/3 vs names)—confirm during impl against `AssistAcc` usage; does not change bucket or API shape.
