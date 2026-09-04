# Month-end Guided Wizard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Embed voucher posting, successive fix, depreciation, and required P&L carries inside a gated month-end wizard so accountants finish close step-by-step instead of hitting opaque verify toasts.

**Architecture:** Expand `settle-period.vue` into a 5-step single-page wizard that composes existing voucher / successive / settlement-carry / depreciation / verify APIs. Gate “下一步” on step readiness. Soften axios toast for settlement verify via a request flag. Confirm `qm_jz_cbfy` already covers 5401 via `SubjectCodeCompat` (6401→5401) with a regression test and clearer UI copy.

**Tech Stack:** Vue 3 + Element Plus (`financial-cloud-ui`), Spring Boot + MyBatis-Plus (`financial-cloud`), JUnit 5, existing axios `Request.ts`.

## Global Constraints

- Follow design: `docs/superpowers/specs/2026-09-03-month-end-guided-wizard-design.md`
- Do **not** make `qm_jz_xscb` a hard-required carry
- Required carries remain `qm_jz_sr`, `qm_jz_cbfy`, plus Dec `qm_jz_bnlr` when applicable
- Carry “done” in the wizard means **generated and posted**
- Prefer composing existing APIs; no new readiness endpoint unless a task proves it necessary
- UI copy in 中文; keep existing Element Plus patterns
- Commit only when the user asks (plan steps that say Commit are optional until then)

---

## File map

| File | Responsibility |
|------|----------------|
| `financial-cloud/src/test/java/.../SubjectCodeCompatTest.java` | Assert 6401↔5401 carry aliases |
| `financial-cloud-ui/src/utils/Request.ts` | Support `silentError` / `skipGlobalError` so verify can fail without toast |
| `financial-cloud-ui/src/api/book/settlement.ts` | Pass silent flag on `verify()` |
| `financial-cloud-ui/src/views/settlement/wizard/useMonthEndWizard.ts` | Step state, readiness, action helpers |
| `financial-cloud-ui/src/views/settlement/wizard/StepManual.vue` | Step 0 |
| `financial-cloud-ui/src/views/settlement/wizard/StepVoucherPrep.vue` | Step 1: unposted list + post + successive |
| `financial-cloud-ui/src/views/settlement/wizard/StepAccrueCarry.vue` | Step 2: depreciation + required carries generate/post |
| `financial-cloud-ui/src/views/settlement/wizard/StepVerify.vue` | Step 3: verify table + jump links |
| `financial-cloud-ui/src/views/settlement/wizard/StepCheckout.vue` | Step 4 |
| `financial-cloud-ui/src/views/settlement/settle-period.vue` | Host steps + el-steps navigation |
| `docs/product/06-settlement.md` | Document guided flow |
| `openspec/specs/month-end-close/spec.md` | Delta requirements for in-wizard gates (via OpenSpec change if applying workflow) |

---

### Task 1: Confirm 主营业务成本 alias + UI copy

**Files:**
- Modify: `financial-cloud/src/test/java/com/financial/cloud/util/SubjectCodeCompatTest.java`
- Modify: `financial-cloud-ui/src/views/settlement/settle-period.vue` (temporary label until wizard split; also final label in StepAccrueCarry)
- Test: `SubjectCodeCompatTest`

**Interfaces:**
- Consumes: `SubjectCodeCompat.carryForwardSubjectCodes(String)`
- Produces: proof that `"6401"` candidates include `"5401"`; UI string `结转成本费用（含主营业务成本）`

- [ ] **Step 1: Write failing/asserting test for 6401→5401**

Add to `SubjectCodeCompatTest.java`:

```java
@Test
void carryForwardSubjectCodes_includesXiaorenCostAlias() {
    assertTrue(SubjectCodeCompat.carryForwardSubjectCodes("6401").contains("5401"));
    assertTrue(SubjectCodeCompat.lookupCandidates("6401").contains("5401"));
}
```

- [ ] **Step 2: Run test**

Run:

```powershell
cd financial-cloud
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4.1'
.\mvnw.ps1 -q -Dtest=SubjectCodeCompatTest#carryForwardSubjectCodes_includesXiaorenCostAlias test
```

Expected: PASS (alias already exists). If FAIL, add `CARRY_FORWARD_ALIASES` entry `6401`→`5401` in `SubjectCodeCompat.java` and re-run.

- [ ] **Step 3: Relabel required carry in wizard data**

Wherever `qm_jz_cbfy` display name is shown in month-end UI, prefer:

```ts
name: row?.name || '结转成本费用（含主营业务成本）'
```

and if `row.name` is the seed name `3-结转成本费用`, still append clarity in the step card subtitle: `含主营业务成本（5401/6401）`.

- [ ] **Step 4: Optional commit**

```bash
git add financial-cloud/src/test/java/com/financial/cloud/util/SubjectCodeCompatTest.java
git commit -m "test: assert qm_jz_cbfy cost codes include 5401 alias"
```

---

### Task 2: Silent verify responses (no toast as primary UX)

**Files:**
- Modify: `financial-cloud-ui/src/utils/Request.ts`
- Modify: `financial-cloud-ui/src/api/book/settlement.ts`

**Interfaces:**
- Consumes: axios config
- Produces: `verify({ silentError?: boolean })` that returns/rejects **without** `ElMessage` when `silentError: true`

- [ ] **Step 1: Extend response interceptor**

In `Request.ts` response success handler, before `code === 2` / other error toasts:

```ts
const silent = res.config?.silentError === true || res.config?.headers?.['X-Silent-Error'] === '1'
if (silent && code !== 0 && code !== 401) {
  return Promise.reject(res.data)
}
```

Apply the same early-return (no toast) for `code === 2`, `500`, and the final `else` branch when `silent` is true. Keep `401` behavior unchanged.

- [ ] **Step 2: Update settlement verify API**

```ts
export function verify(options?: { silentError?: boolean }): any {
  return request({
    url: '/settlement/verify',
    method: 'get',
    silentError: options?.silentError === true,
  })
}
```

Ensure `request` axios instance forwards unknown config keys (default axios does).

- [ ] **Step 3: Smoke manually**

Call `verify({ silentError: true })` from browser console / temporary button with known hard failures: Network tab shows response; **no** red Element toast; promise rejects/resolves with payload.

- [ ] **Step 4: Optional commit**

```bash
git add financial-cloud-ui/src/utils/Request.ts financial-cloud-ui/src/api/book/settlement.ts
git commit -m "fix: allow silent settlement verify without global toast"
```

---

### Task 3: Wizard composable — readiness + actions

**Files:**
- Create: `financial-cloud-ui/src/views/settlement/wizard/useMonthEndWizard.ts`

**Interfaces:**
- Produces:

```ts
export type WizardStep = 0 | 1 | 2 | 3 | 4

export interface UnpostedVoucherRow {
  id: string
  word: string
  status: string
  voucherDate: string
}

export interface CarryRow {
  code: string
  name: string
  templateId?: string
  voucherId?: string | null
  voucherStatus?: string | null
  done: boolean // posted
  phase: 'missing' | 'draft' | 'posted' | 'na'
}

export function useMonthEndWizard() {
  // state + methods listed below
}
```

Methods (exact names later tasks import):

- `refreshStep1(): Promise<void>` — load unposted + successive gaps
- `submitAuditPost(ids: string[]): Promise<void>` — `submitBatch` → `auditBatch` → `senderBatch` as needed by status
- `fixSuccessive(): Promise<void>` — GET then PUT successive
- `refreshStep2(): Promise<void>` — depreciation + required carries with voucher status
- `generateAndPostCarry(code: string): Promise<void>`
- `accrueDepreciation(): Promise<void>`
- `runVerify(): Promise<void>` — `verify({ silentError: true })`
- `checkout(): Promise<void>`
- computed: `canLeaveStep0`, `canLeaveStep1`, `canLeaveStep2`, `canLeaveStep3`

- [ ] **Step 1: Scaffold composable with empty reactive state**

Create the file with refs: `active`, `manualAck`, `unposted`, `successiveGaps`, `carryRows`, `deprNeeded`, `deprAccrued`, `verifyRows`, `isVerify`, `isCheckout`, `checkoutOk`, `checkoutError`, loading flags.

- [ ] **Step 2: Implement `refreshStep1`**

```ts
import * as voucherApis from '@/api/voucher/voucher'
import bookStore from '@/store/modules/bookStore'

// fetch current term vouchers — use existing list API params matching voucher-index
const term = String(bookStore().termCurrent || '')
const year = term.slice(0, 4)
const month = term.slice(5, 7)
const res: any = await voucherApis.listVoucher({
  pageNumber: 1,
  pageSize: 200,
  voucherYear: year,
  voucherMonth: Number(month),
})
const records = res?.data?.records || res?.data || []
unposted.value = records.filter((v: any) => v.status !== 'completed' /* confirm enum: posted status used in product */)
```

**Status mapping (verify against `VoucherStatusEnum`):** treat as blocking if status is not the posted/completed value used by settlement unposted check. Read `SettlementService` unposted filter and match it exactly (do not invent a different rule).

Successive:

```ts
const suc: any = await voucherApis.getVoucherSuccessiveList({})
successiveGaps.value = suc?.data || []
```

`canLeaveStep1` = `unposted.length === 0 && successiveGaps.length === 0`.

- [ ] **Step 3: Implement posting helper**

For selected ids, call in order when status requires it (inspect each row’s status):

```ts
await voucherApis.submitBatch(ids.join(','))
await voucherApis.auditBatch(ids.join(','))
await voucherApis.senderBatch(ids.join(','))
```

If batch APIs already move draft→posted in product flows, follow the same sequence as `voucher-index.vue` toolbar (copy that order verbatim).

- [ ] **Step 4: Implement `refreshStep2` + `generateAndPostCarry`**

Required codes: `['qm_jz_sr','qm_jz_cbfy']` + `qm_jz_bnlr` if month === `'12'`.

```ts
const carryRes: any = await settlementApi.fetchcarry({ pageNumber: 1, pageSize: 50, category: 1 })
// map rows; load voucher get(id) for status if voucherId present
// done = voucher status is posted/completed
```

Generate:

```ts
const gen: any = await settlementApi.generate({ id: templateId }) // use real export name from settlement.ts
await voucherApis.submitBatch(voucherId)
await voucherApis.auditBatch(voucherId)
await voucherApis.senderBatch(voucherId)
```

Depreciation: `getDepreciationStatus` + `accrueDepreciation` from `@/api/fixed-asset/depreciation`.

`canLeaveStep2` = depr OK && every required carry `phase === 'posted' || phase === 'na'`.

- [ ] **Step 5: Implement verify + checkout**

```ts
try {
  const res: any = await settlementApi.verify({ silentError: true })
  verifyRows.value = res.data || []
  isVerify.value = res.code === 0 && !hardFailed(verifyRows.value)
} catch (err: any) {
  verifyRows.value = err?.data || []
  isVerify.value = false
}
```

`jumpTargetForItem(item: string): WizardStep` map:

- `未过账` / unposted → `1`
- `凭证号` / successive → `1`
- `结转` / carry → `2`
- `折旧` → `2`
- default → `3`

---

### Task 4: Step components + host rewrite

**Files:**
- Create: `financial-cloud-ui/src/views/settlement/wizard/StepManual.vue`
- Create: `financial-cloud-ui/src/views/settlement/wizard/StepVoucherPrep.vue`
- Create: `financial-cloud-ui/src/views/settlement/wizard/StepAccrueCarry.vue`
- Create: `financial-cloud-ui/src/views/settlement/wizard/StepVerify.vue`
- Create: `financial-cloud-ui/src/views/settlement/wizard/StepCheckout.vue`
- Modify: `financial-cloud-ui/src/views/settlement/settle-period.vue`

**Interfaces:**
- Consumes: `useMonthEndWizard()`
- Produces: gated UI matching design steps 0–4

- [ ] **Step 1: Build StepManual**

Checkbox + manual table (reuse existing `manualCheckData` content from current `settle-period.vue`).

- [ ] **Step 2: Build StepVoucherPrep**

- Table of `unposted` with columns 凭证字号 / 日期 / 状态
- Buttons: 刷新、批量提交审核过账、整理断号
- Alert when `canLeaveStep1` is false listing counts

- [ ] **Step 3: Build StepAccrueCarry**

- Depreciation card: status tag + 计提折旧 + 刷新
- Carry table: code, name (`结转成本费用（含主营业务成本）` for cbfy), phase, 生成并过账 button
- Disable Next hint when incomplete

- [ ] **Step 4: Build StepVerify / StepCheckout**

- Verify table with 去处理 button calling `active = jumpTargetForItem(...)`
- Checkout confirm + result (reuse existing el-result blocks)

- [ ] **Step 5: Rewrite `settle-period.vue` host**

```vue
<el-steps :active="active" finish-status="success" align-center>
  <el-step title="人工确认" />
  <el-step title="凭证整理" />
  <el-step title="计提与结转" />
  <el-step title="系统校验" />
  <el-step title="结账" />
</el-steps>

<StepManual v-if="active===0" ... />
...
<!-- footer buttons: 上一步 / 下一步 with :disabled="!canLeaveStepN" -->
```

On `goNext` from step 1→2 call `refreshStep2`; 2→3 call `runVerify`; never advance if gate fails (show `ElMessage.warning` with one line of what’s missing — acceptable; primary detail stays in-step).

Remove old 4-step layout and “打开期末结转 / 打开折旧” as the **only** path (optional secondary “高级：打开完整结转页” link is OK).

- [ ] **Step 6: Manual UI pass**

Login `admin` / `changeme`, open 月结:

1. Step1 blocks with drafts present  
2. Post + successive clears gate  
3. Step2 generate+post sr/cbfy  
4. Verify shows table without toast  
5. Failed row 去处理 jumps back  

---

### Task 5: Docs + OpenSpec delta

**Files:**
- Modify: `docs/product/06-settlement.md`
- Create or update OpenSpec change under `openspec/changes/` (name: `month-end-guided-wizard`) **or** directly update `openspec/specs/month-end-close/spec.md` if team prefers sync-without-change — prefer propose/apply workflow:

Requirements to add:

1. Wizard SHALL include voucher posting and successive fix as a gated step before accrue/carry.
2. Wizard SHALL generate and post required carries in-flow; `qm_jz_cbfy` SHALL be described as including 主营业务成本 (5401/6401 alias).
3. Verify failures SHALL surface in-wizard with navigation to the owning step; global error toast MUST NOT be the sole feedback for expected verify failures.

- [ ] **Step 1: Update product doc section 2–4** to the 5-step flow
- [ ] **Step 2: Write OpenSpec delta** (`proposal.md` / `specs/month-end-close/spec.md` / `tasks.md`) aligned with this plan
- [ ] **Step 3: Optional commit of docs**

---

### Task 6: Verification checklist (definition of done)

- [x] **Step 1: Backend unit**

```powershell
cd financial-cloud
.\mvnw.ps1 -q -Dtest=SubjectCodeCompatTest,SettlementServiceTest test
```

Expected: PASS  
Evidence (2026-09-03): `SubjectCodeCompatTest` 7/0 fail, `SettlementServiceTest` 11/0 fail, mvn EXIT=0.

- [x] **Step 2: Rebuild & restart backend if Java changed** (alias-only may skip)

Skipped rebuild: running backend on `:2154` already serving verify; no Java rebuild required for this DoD pass.

- [ ] **Step 3: Frontend smoke on :3154**

- No toast on verify failure; table visible — **partial**: API smoke confirms verify returns `data` (code may be 2); toast N/A for API-only; `silentError` wired in `settlement.ts` / `Request.ts` / `runVerify`. Browser `:3154` UI not exercised.  
- Cannot skip to checkout with unposted or missing posted carries — **code spot-check PASS** (`canLeaveStep0..3` + `:disabled="!canLeaveCurrent"`); live UI gate not exercised.  
- After fixing in-wizard, checkout succeeds on a clean book — **NOT done** (verify data shows failed carry items; no end-to-end checkout smoke).

- [x] **Step 4: Mark plan tasks complete in this file**

Marked Steps 1–2 and this step from Task 6 evidence; Step 3 left open for remaining UI/checkout gaps.

---

## Spec coverage (self-review)

| Design requirement | Task |
|--------------------|------|
| 5-step in-flow wizard | 3, 4 |
| Posting + successive in wizard | 3, 4 |
| Carry generate+post in wizard | 3, 4 |
| Verify inline + 去处理 | 2, 3, 4 |
| No toast-primary for verify | 2 |
| 5401/6401 主营业务成本 | 1 (alias + copy) |
| Docs / spec | 5 |
| E2E/smoke DoD | 6 |

**Placeholder scan:** none intentional. Implementers must read `SettlementService` unposted status values and `voucher-index.vue` batch order rather than guessing enums — called out in Task 3 Step 2–3.

**Note on cost carry:** Code already maps `6401`→`5401` via `SubjectCodeCompat`; Task 1 locks that with a test. If smoke still shows 5401 balance uncleared after `qm_jz_cbfy`, file a follow-up bug on `selectSubjectAndChild` / balance source — do not add `qm_jz_xscb` in this plan.
