## 1. Stay-on-page & explicit new

- [x] 1.1 Remove save/draft auto-navigation to voucher list; keep/reload current voucher — verify URL stays on entry with saved id
- [x] 1.2「新建凭证」= dirty discard-or-cancel + blank + `getVoucherAbleWordNum`; save must not auto-blank — verify save stays; 新建 clears and new word number
- [x] 1.3「返回列表」with discard-or-cancel dirty guard — verify discard leaves; cancel keeps edits

## 2. Prev / next by date + word number

- [x] 2.1 Implement neighbor resolution by `voucherDate`, `wordHead`, `wordNum` within book (not list filter order) — verify fixture order 记1→记2 by date/word
- [x] 2.2 Toolbar「上一张 / 下一张」; disable at ends — verify boundaries and body load
- [x] 2.3 Dirty guard: 不保存离开 / 取消 before neighbor switch — verify discard vs cancel

## 3. Full-page only (no dialog editor)

- [x] 3.1 Change list open / 新建 to route into full-page workspace only; stop modal primary edit — verify list click opens full page
- [x] 3.2 Remove or gut unused `dialog` edit path callers — verify no modal editor on main flows

## 4. WYSIWYG layout + coding entry

- [x] 4.1 Restructure template to classic sheet regions; short word `记 n 号`; show 借贷平衡 + 状态 — verify visible without print mode
- [x] 4.2 Allow >6 entry rows on screen (grow/scroll); keep Tab order through grid — verify 7+ lines editable on one page
- [x] 4.3 Prioritize subject **code + Enter** commit path; keep print → classic HTML multi-page — verify coding enter + print pagination still 6/page

## 5. Verification

- [x] 5.1 Checklist/e2e: save-stay, no auto-new, discard leave, date+word neighbors, full-page from list, >6 lines on screen — verify on sample book
- [x] 5.2 Smoke: draft/save validation, balance check, classic print multipage — verify no regressions
