## 1. Contribution base validation & visibility

- [x] 1.1 Add calculate-time validation: `NORMAL` + custom base rule requires positive `payBaseNumber`; verify unit test covers reject vs happy custom/default paths
- [x] 1.2 Expose effective contribution base (value + default/custom indicator) on salary preview/temp payload and verify API/DTO returns it for a calculated row
- [x] 1.3 Show effective base + personal SI/HF amounts on calc-salary preview UI and verify columns/labels visible after generate preview

## 2. Bank payment file export

- [x] 2.1 Implement export from confirmed `employee_salary` for book + belonging month (姓名、开户行、账号、实发、所属月；工号可选)；missing `bankCardNo` blocks with employee list; empty month rejects — verify unit/API tests for success, missing-account block, empty month
- [x] 2.2 Wire frontend export CTA (salary-detail or guided step 4) calling the export API and verify file download starts for a valid month

## 3. Guided monthly payroll path

- [x] 3.1 Add four-step indicator + primary CTAs on calc-salary (and deep links to detail/voucher/export as needed): 选月预览 → 调整推送 → 生成凭证 → 导出代发盘； verify step UI renders and CTAs navigate/trigger existing flows
- [x] 3.2 Soft-guard: block voucher generation from guided path when no salary details exist for book+month; verify message instructs to push details first
- [x] 3.3 Soft-guard: hide or disable 代发盘 until details exist for the month; verify cannot export from preview-only state via the guided CTA

## 4. Employee UX & product docs

- [x] 4.1 Clarify employee form for unified custom vs book default base (hint that calculation uses unified base this period); verify save + reload of `payBaseRule`/`payBaseNumber`
- [x] 4.2 Update `docs/product/09-payroll.md` with SMB min-loop steps, unified-base scope, 代发盘 columns, and non-goals (工资条、累计预扣法、分险种基数) — verify doc sections exist

## 5. Verification

- [x] 5.1 Extend or add HR E2E/smoke: open calc-salary guided path pages + salary-detail; verify key pages open without error
- [x] 5.2 Run relevant backend unit tests for base validation and payment export and verify they pass
