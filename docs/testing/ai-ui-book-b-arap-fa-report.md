# AI UI 专项账套 B — 5.2 往来核销 / 5.3 固定资产

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项B`
- **bookId**：`2105448444973871105`
- **启用期间**：`2026-01`（凭证审核开启）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T00:27:35.214Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-book-b-arap-fa.mjs`

## 结论：**5.2/5.3 核心路径 PASS**（PASS 32 / FAIL 0 / WARN 1）

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin + ai_reviewer |
| SWITCH-BOOK | PASS | bookId=2105448444973871105 term=2026-01 (restored if corrupted) |
| ASSIST-ENABLE | PASS | sys.assist.acc.enabled=true |
| SUBJECTS | PASS | 1002=2105448445384151049 1122=2105448445329625093 2202=2105448445329625096 5001=2105448445333819400 1601=2105448445291876356 1602=2105448445384151048 5602.02=2105448445333819394 5602.04=2105448445392539651 |
| AUX-1122 | PASS | 已绑定 客户(2) |
| AUX-2202 | PASS | 已绑定 供应商(3) |
| ASSIST-2-B-C01 | PASS | 已存在 id=2105453267836268545 |
| ASSIST-3-B-S01 | PASS | 已存在 id=2105453267869822977 |
| AR-OCCUR | PASS | 已过账 id=2105453325885435905 |
| AR-RECEIPT | PASS | 已过账 id=2105454349132034049 |
| AP-OCCUR | PASS | 已过账 id=2105454349694070786 |
| AP-PAY | PASS | 已过账 id=2105454350293856258 |
| AR-BALANCE | PASS | expected=6000 actual=6000 |
| AP-BALANCE | PASS | expected=5000 actual=5000 |
| ARAP-DETAIL | PASS | AR lines=2 AP lines=2 |
| GL-AR-BEFORE-WO | PASS | 1122=6000 |
| WRITEOFF-CONFIRM | PASS | partial 4000 id=2105454467112849409 |
| WRITEOFF-REMAIN | PASS | uncleared increase remaining=6000 expected=6000 |
| WRITEOFF-GL-UNCHANGED | PASS | AR 6000→6000 bank 1000→1000 |
| WRITEOFF-REVERSE | PASS | reversed id=2105454467112849409 |
| WRITEOFF-STATUS | PASS | status=REVERSED |
| WRITEOFF-RESTORED | PASS | increaseRem=10000 decreaseRem=4000 |
| REVERSE-GL-UNCHANGED | PASS | 1122 6000→6000 |
| FA-CATEGORY | PASS | 已存在 id=2105453382655340545 |
| FA-CARD | PASS | 已存在 id=2105454351422124033 purchaseVoucher=2105454351447289857 |
| FA-PURCHASE-VOUCHER | PASS | 购入凭证已过账 id=2105454351447289857 |
| FA-DEPR-STATUS | PASS | accrued=false voucherId=- |
| FA-DEPR-ACCRUE | PASS | voucher=2105454467465932802 amount=1000 raw={"totalAmount":1000,"voucherId":"2105454467465932802","voucherWord":"记-8","yearPeriod":"2026-01"} |
| FA-DEPR-POST | PASS | id=2105454467465932802 sender=true |
| FA-CHECK-DISPOSE | WARN | 按任务要求跳过盘点/清理深路径（入口可另行点测） |
| FA-VALUES | PASS | cost=12000 accum=1000 net=11000 |
| FA-GL | PASS | 1601=12000 1602=-1000 |
| SCREENSHOTS | PASS | bookb-arap-* + bookb-fa-* captured |

## 金额核对

| 检查点 | 预期 | 实际 |
|---|---:|---:|
| 客户应收余额 | 6000 | 6000 |
| 供应商应付余额 | 5000 | 5000 |
| 核销前总账 1122 | 6000 | 6000 |
| 核销后总账 1122（不变） | 6000 | 6000 |
| 撤销后总账 1122（不变） | 6000 | 6000 |
| 固定资产原值 | 12000 | 12000 |
| 累计折旧 | 1000 | 1000 |
| 净值 | 11000 | 11000 |

## 阻塞 / 缺陷

- **BUG-FA-SQL-DATE（P1）**：`FixedAssetService.createPurchaseVoucher` 在 `startUseDate` 所属期 ≠ `entryPeriod` 时把凭证日期设为 `java.sql.Date`，随后 `DateUtils.format` → `toInstant()` 抛 `UnsupportedOperationException`，卡片保存整单回滚。首次建卡用同月 `startUseDate` 绕过并 DB 回调 `2025-12-15` 使 2026-01 成为首个适用折旧月。源码已改为 `java.util.Date`（`Date.from(LocalDate…Asia/Shanghai)`），需重启后端后复测优先路径。
- **OBS-BOOK-B-TERM**：并发代理曾把专项 B 的 `sys.payment.term.current` 误写成 `2026-03`；脚本每次写入前 `ensureOnBook` 恢复为 `2026-01`。
- **SKIP-FA-CHECK-DISPOSE**：任务允许跳过盘点/清理深路径；未执行盘亏盘盈/清理处置用例。

## 证据截图

- `/opt/cursor/artifacts/screenshots/bookb-arap-balance.webp`
- `/opt/cursor/artifacts/screenshots/bookb-arap-detail.webp`
- `/opt/cursor/artifacts/screenshots/bookb-arap-writeoff.webp`
- `/opt/cursor/artifacts/screenshots/bookb-arap-writeoff-reversed.webp`
- `/opt/cursor/artifacts/screenshots/bookb-fa-cards.webp`
- `/opt/cursor/artifacts/screenshots/bookb-fa-depreciation.webp`
- `/opt/cursor/artifacts/screenshots/bookb-fa-subject-balance.webp`
