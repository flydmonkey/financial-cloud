# Month-end guided wizard (in-flow) — Design

Date: 2026-09-03  
Status: draft for user review  
Related: `openspec/specs/month-end-close/spec.md`, `docs/product/06-settlement.md`

## Goal

Make month-end close a **single guided wizard** where the accountant finishes every required action **inside the step navigation**—including voucher posting, number continuity, depreciation, and required P&L carries—without being blocked by opaque global error toasts. Hard gates still enforce correctness; the UI turns failures into **in-step todos with actions**.

## Non-goals

- Separate sales-cost carry (`qm_jz_xscb`, inventory → COGS) as a hard-required step (user chose **A**: P&L cost carry only).
- Independent year-end close entry.
- Rebuilding carry-forward template admin UI inside the wizard.

## Decisions (approved)

| Topic | Choice |
|-------|--------|
| UX shape | **Approach 1**: single-page wizard embeds all actions |
| Cost carry | Fix `qm_jz_cbfy` to include **5401 and 6401** (主营业务成本 under both standards); still one required code |
| Posting | Required carry vouchers count as done only when **generated and posted** (consistent with unposted hard gate) |
| Errors | Verify/checkout soft failures render in the wizard table / step alerts; **no interceptor toast as the primary UX** for expected verify `code≠0` |

## Wizard steps

```
0 人工确认 → 1 凭证整理 → 2 计提与结转 → 3 系统校验 → 4 结账
```

### Step 0 — 人工确认

- Same checklist as today (bank, inventory, tax cross-checks, etc.).
- **Next** enabled only when acknowledgment checkbox is checked.
- Does not call verify.

### Step 1 — 凭证整理（过账 + 断号）

Embedded in the wizard (no jump to voucher list as the only path):

1. List current-term vouchers that are not posted (draft / under review / rejected as product rules define).
2. Actions: submit / audit / post (reuse existing voucher APIs; batch where APIs already allow).
3. Successive (断号) check + **one-click fix** using the fixed successive rules (all occupying statuses; two-phase renumber).
4. **Next** enabled only when: no blocking unposted vouchers **and** successive check returns empty (or explicit pass).

Refresh list when returning to the step (`onActivated` / after actions).

### Step 2 — 计提与结转

Embedded panels:

| Panel | Behavior |
|-------|----------|
| 固定资产折旧 | Show accrued / N/A; trigger accrue (or open minimal inline flow using existing depreciation API); refresh status |
| 必做损益结转 | Rows for `qm_jz_sr`, `qm_jz_cbfy` (+ `qm_jz_bnlr` in December when applicable). Label `qm_jz_cbfy` as **结转成本费用（含主营业务成本）**. Per row: 生成 → 过账 (or generate-and-post). Status: 未生成 / 已生成未过账 / 已过账 |

**Next** enabled only when:

- Depreciation: accrued **or** N/A; and
- Every required carry for the term is **posted** (missing template for optional Dec year-profit may remain N/A per existing `MonthEndCloseRules`).

Optional / non-required templates (所得税结转等) may appear as secondary links but do not block Next unless later promoted to hard gates.

### Step 3 — 系统校验

- Call existing `verify` API.
- Render full result table (hard / warning / manual).
- Failed hard rows expose **去处理** that jumps to the owning step (e.g. unposted → step 1; missing carry → step 2; depreciation → step 2; successive → step 1).
- Treat verify business failure (`code≠0` with row payload) as **inline result**, not a blocking toast-only experience. Align axios/UI handling for this page so the table is the source of truth.
- **Next: 结账** only when all hard items pass.

### Step 4 — 结账

- Confirm copy; call `checkout`.
- On failure, show message + link back to verify/owning step; do not leave the wizard in an ambiguous state.
- On success, show success and refresh book term (existing behavior).

## Backend changes

### `qm_jz_cbfy` subject coverage

In `SettlementCarryService` for `qm_jz_cbfy`, include **both** `6401` and `5401` when building cost lines (in addition to existing expense codes `6405`, `6601`, `6602`, `6603`, `6711`). Only subjects with non-zero balances contribute lines (existing `addCarryVoucherItems` behavior).

No new required template code; `MonthEndCloseRules` stays `qm_jz_sr` + `qm_jz_cbfy` (+ Dec `qm_jz_bnlr`).

### APIs

Prefer reusing:

- Voucher list / submit / audit / post
- `/api/voucher/successive` check + update
- Settlement carry fetch / generate / delete
- Depreciation status + accrue
- Settlement `verify` / `checkout`

Add thin aggregate endpoints **only if** front-end would otherwise need many round-trips for step readiness (optional `GET .../month-end/readiness`). Default: compose existing APIs in the UI.

### Verify semantics

Unchanged hard gates; carry “generated” continues to mean a carry voucher exists for the template. Posting is enforced by the **unposted vouchers** gate and by wizard step-2 completion rules (must post before Next).

## Frontend structure

Primary surface: `settle-period.vue` (or extract step components under `views/settlement/wizard/`).

- Keep top tabs for 期末结转 / 结账列表 as secondary entry points if still needed, but the **月结** tab is the guided path; deep links from step actions stay in-wizard.
- Step gate logic centralized (computed `canAdvance` per step).
- Suppress or remap global error toast for settlement verify on this page (interceptor opt-out or page-local handler).

## Error / empty states

- Empty unposted list → show pass hint.
- Carry generate with zero balances → follow existing API messages; mark row with clear reason; do not silently pass required codes unless product already treats empty as N/A (today required codes still expect a voucher—keep consistent unless verify already allows empty).
- Network errors → inline alert + retry.

## Testing

- Unit: `qm_jz_cbfy` includes 5401 and/or 6401 when balances exist.
- UI/e2e (smoke): cannot advance step 1 with unposted; cannot advance step 2 without posted required carries; verify failure offers jump back; checkout still blocked until hard pass.
- Regression: December `qm_jz_bnlr` still gated; successive duplicate-with-draft scenario remains fixed.

## Out of scope follow-ups

- Making `qm_jz_xscb` required.
- Full voucher editor embedded in the wizard (view + status actions are enough; “打开凭证” may open existing voucher page in a tab if edit is needed).

## Implementation note

Prefer an OpenSpec change (e.g. `month-end-guided-wizard`) to delta `month-end-close` specs for: in-wizard posting, step gates, inline verify UX, and 5401/6401 cost carry—then implement via apply workflow.
