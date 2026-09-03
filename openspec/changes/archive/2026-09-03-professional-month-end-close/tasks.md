## 1. Discovery & verify contract

- [x] 1.1 Inventory carry-forward template fields/flags for “required for close” (and 损益结转 defaults) and record the chosen rule in a short comment or design note; verify by listing templates for a standard 小企业 book
- [x] 1.2 Extend `SettlementVerifyVo` (or equivalent DTO) with hard/manual, pass/fail/N/A, and reason fields; verify compilation and existing clients still deserialize safely
- [x] 1.3 Implement extended `SettlementService.verify` hard items: unposted/incomplete vouchers, successive, debit=credit, required carry-forward done, depreciation accrued-or-N/A; verify with unit tests for pass, fail, and depreciation N/A cases
- [x] 1.4 Make `checkout` re-run the same hard-gate evaluation and reject when any hard item fails; verify unit test that checkout is blocked when verify would fail even if called directly

## 2. Period lock on voucher create

- [x] 2.1 Enforce open-period guard on voucher create/save for periods before current open term; verify unit or API test rejects closed-period create with a clear error and does not persist
- [x] 2.2 Confirm existing open-period mutation guards still pass regression tests (`isVoucherInOpenPeriod` paths); verify relevant voucher service tests remain green

## 3. Month-end wizard UI

- [x] 3.1 Rebuild settlement primary UX as a unified month-end wizard (current term + ordered steps: readiness → accrue/carry → verify → checkout); verify no separate year-end menu/route is introduced
- [x] 3.2 Wire wizard to verify API: show hard vs manual items, disable checkout until hard gates pass and manual acknowledgment is completed; verify UI blocks checkout when hard items fail
- [x] 3.3 Embed or deep-link carry-forward generation and fixed-asset depreciation accrue with return-and-refresh of step statuses; verify generating required carry-forward updates checklist without full app reload
- [x] 3.4 Relabel 往来/银行调节/存货/税控等 as「人工确认 / 本期不系统检」and ensure they never appear as failed hard verify rows; verify copy and that verify success is possible without 往来 modules

## 4. Tests & docs alignment

- [x] 4.1 Update settlement E2E helpers/specs so checkout fixtures satisfy new hard gates (post vouchers, required carry, depreciation as needed); verify `settlement*.spec.ts` / carry-forward related E2E pass
- [x] 4.2 Add or extend E2E/API coverage for: hard-gate rejection, successful month-end checkout advancing term, closed-period voucher create rejection; verify new assertions fail before fix and pass after
- [x] 4.3 Re-run uncheckout E2E/unit suite and confirm no behavior regression; verify existing uncheckout tests still pass
- [x] 4.4 Update `docs/product/06-settlement.md` to describe professional month-end wizard, hard gates, manual placeholders, and “only month-end / no year-end entry”; verify doc matches shipped behavior
