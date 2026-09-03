## Context

See `proposal.md` for motivation. Current settlement UX is tabbed (`carry-forward` / `settle-period` / `settle-list`): settle-period already has a 3-step UI, but step 0 is static “请外部完成” copy, step 1 `GET /settlement/verify` only checks voucher successive numbers and period debit=credit, and checkout does not re-enforce richer gates. Carry-forward and fixed-asset depreciation live in separate pages/APIs. Period lock via `isVoucherInOpenPeriod` covers many mutations but product docs note new-voucher `save` is incomplete. Uncheckout is already specified and implemented under `daizhang-polish-priority` / `settlement-uncheckout`—this design must not weaken it.

## Goals / Non-Goals

**Goals:**

- One month-end close wizard with ordered steps and hard server-side gates before checkout.
- Extend verify (+ checkout pre-check) for unposted/incomplete vouchers, successive, balance, required carry-forward, depreciation N/A-or-done.
- Close the create/save period-lock gap for non-open periods.
- Keep December year-end template items inside monthly carry-forward; no year-end product surface.
- Keep 往来/银行调节 etc. as manual / “本期不系统检” UI only.

**Non-Goals:**

- Implementing 往来 L1–L3, bank reconciliation, tax wizard, inventory.
- Redesigning uncheckout rules or settlement snapshot schema beyond what verify/checkout need.
- Persisting a full “wizard progress” workflow engine across devices (prefer recomputing status from data).
- Changing会计准则 templates themselves (only consume required flags / existing carry templates).

## Decisions

### D1: Single workspace + deep-links, not a new microservice

**Choice:** Evolve the settlement front-end into one `month-end close` workspace (reuse routes under `/settlement/*` or consolidate into one primary route with steps). Embed or deep-link to existing carry-forward generation and depreciation accrue APIs; on return/refresh, re-query statuses.

**Alternatives:** (a) Keep three tabs with only stronger copy—rejected, does not fix “不像月结”. (b) New backend workflow service with persisted step state—overkill for v1.

### D2: Server is source of truth for hard gates

**Choice:** Expand `SettlementService.verify` to return a structured list of items with `hard` vs `manual` (or equivalent), pass/fail/N/A, and human-readable reason. `checkout` MUST re-run the same hard-gate evaluation and refuse if any hard item fails (do not trust UI-only `isVerify`).

**Alternatives:** UI-only checklist—rejected (bypassable). Separate `/verify-hard` endpoint—unnecessary if one verify payload can classify items.

### D3: How to detect “required carry-forward done”

**Choice:** Treat carry-forward templates already returned by `fetchcarry` as the catalog. Templates marked required for close (existing flag if present; otherwise define/default: 损益结转类 required, optional templates remain optional) are satisfied when a linked carry-forward voucher exists for the current term (via `settlement_carryforward` / `voucher.carry_forward` as today). December “结转本年利润” remains a template in that month’s list, not a special year-end mode.

**Alternatives:** Hard-code subject codes in verify—fragile across 准则. Force all templates required—too strict for optional accruals.

### D4: Depreciation gate

**Choice:** Reuse fixed-asset depreciation status API semantics (`accrued` / not applicable when no depreciable assets for the term). Verify calls the same domain service the depreciation page uses; N/A ⇒ pass.

**Alternatives:** Always require a depreciation voucher—wrong for empty fixed-asset books.

### D5: Unposted / audit gating

**Choice:** Block on any voucher in the current term that is not in posted (已过账) state. If the product allows skipping audit, “unaudited but posted” is already impossible under normal flow; if draft/unaudited/unposted all exist as pre-post states, listing them under one “未完成凭证” hard item is enough. Do not invent a new audit-policy engine in this change.

**Alternatives:** Separate mandatory audit config center—deferred (gap analysis P2).

### D6: Manual items UX

**Choice:** Keep a short manual checklist (往来、银行调节、存货、税控勾稽等) with explicit “本期不系统检 / 请人工确认” and a single acknowledgment control before enabling checkout **in the UI**. These rows MUST NOT appear as failed hard items in verify. Next change (往来 L1+L2) will replace 往来 rows with real checks.

### D7: Period lock on create

**Choice:** In voucher create/save, reject when voucher period &lt; current open term (and reject other writes into settled periods consistently). Align error message with existing open-period guards so E2E can assert one clear code/message family.

### D8: No wizard progress table in v1

**Choice:** Recompute step completion from verify + carry + depreciation queries each load. Avoid new `settlement_wizard_state` table unless UX later needs cross-session drafts.

## Risks / Trade-offs

- **[Risk] Required-template flag unclear on some seeds** → Mitigation: inventory templates in design/impl spike; default 损益结转 to required; document overrides in tasks; E2E on standard 小企业 book.
- **[Risk] Checkout becomes stricter and breaks existing E2E that checkout without carry/depreciation** → Mitigation: update settlement E2E helpers to satisfy new gates; fixture books accrue/carry before checkout.
- **[Risk] Users expect 往来 hard-block like 金蝶 after enabling manual ack only** → Mitigation: copy states roadmap; do not imply system已核销.
- **[Risk] Deep-link out to depreciation confuses context** → Mitigation: prefer drawer/sub-route with “返回月结” and auto-refresh statuses.
- **[Trade-off] Stricter close vs.代账 speed** → Hard gates are the product goal; optional templates stay optional so pure cash books are not blocked by unused accruals.

## Migration Plan

1. Ship backend verify/checkout gates behind the same APIs (`/verify`, `/checkout`) so old UI fails closed rather than closing uncleanly.
2. Deploy frontend wizard that consumes new verify fields; remove or demote misleading “系统暂无” rows into manual section.
3. Regression: uncheckout specs/E2E unchanged; settlement checkout E2E updated.
4. Rollback: revert frontend + backend together; no irreversible data migration expected (no new required columns in v1). If a required flag column is added to carry templates, provide default-compatible migration and rollback default.

## Open Questions

- （已解决）必做结转判定：`voucher_template` 无 required 列；采用 `MonthEndCloseRules` 编码白名单（`qm_jz_sr` / `qm_jz_cbfy`；12 月另加 `qm_jz_bnlr`）。
