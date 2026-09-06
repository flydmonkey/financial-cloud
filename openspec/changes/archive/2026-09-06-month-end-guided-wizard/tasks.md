## 1. 主营业务成本 alias + UI 文案

- [x] 1.1 Add `SubjectCodeCompatTest` asserting `6401`→`5401` carry alias
- [x] 1.2 Relabel `qm_jz_cbfy` in wizard as 结转成本费用（含主营业务成本）/ 5401·6401

## 2. 静默 verify（无 toast 主反馈）

- [x] 2.1 Extend `Request.ts` with `silentError` early-return (no global toast)
- [x] 2.2 Update `settlement.ts` `verify({ silentError?: boolean })`

## 3. Wizard composable — 门禁与动作

- [x] 3.1 Scaffold `useMonthEndWizard.ts` with step state and computed gates
- [x] 3.2 Implement `refreshStep1` (unposted list + successive gaps)
- [x] 3.3 Implement posting helper (submit → audit → post batch)
- [x] 3.4 Implement `refreshStep2` + `generateAndPostCarry` + depreciation
- [x] 3.5 Implement `runVerify` (silent) + `checkout` + `jumpTargetForItem`

## 4. 步骤组件 + 宿主页

- [x] 4.1 Build `StepManual` (step 0)
- [x] 4.2 Build `StepVoucherPrep` (step 1: unposted + successive)
- [x] 4.3 Build `StepAccrueCarry` (step 2: depreciation + generate/post carries)
- [x] 4.4 Build `StepVerify` / `StepCheckout` (steps 3–4)
- [x] 4.5 Rewrite `settle-period.vue` as 5-step host with gated 上一步/下一步
- [x] 4.6 Manual UI smoke on :3154 (posting gate, carry post, verify inline, 去处理 jump)

## 5. 文档与 OpenSpec delta

- [x] 5.1 Update `docs/product/06-settlement.md` sections 2–4 for 5-step flow
- [x] 5.2 Create OpenSpec change `month-end-guided-wizard` (proposal + spec delta + tasks)

## 6. 验收清单

- [x] 6.1 Backend unit: `SubjectCodeCompatTest`, `SettlementServiceTest`
- [x] 6.2 Frontend/API smoke: book templates + fetchAll; verify endpoint; wizard gates exercised in session
- [x] 6.3 Mark plan complete in `docs/superpowers/plans/2026-09-03-month-end-guided-wizard.md`
