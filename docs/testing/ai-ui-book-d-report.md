# AI UI 专项账套 D（年末结转）测试报告

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项D`
- **bookId**：`2105456763365081090`
- **启用期间**：`2026-12`（凭证审核开启；强制当前账期=启用月）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T00:36:49Z（首跑全路径）/ 00:40+（校验模式复跑）
- **脚本**：`financial-cloud-ui/scripts-ai-ui-book-d.mjs`

## 结论：**年末路径 PASS**（首跑 PASS 37 / FAIL 0；校验复跑 PASS 15 / FAIL 0）

## 结果表（首跑全路径）

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin + ai_reviewer |
| BOOK-CREATE | PASS | created bookId=2105456763365081090 enable=2026-12 voucherReviewed=1 |
| GRANT-REVIEWER | PASS | grant code=0 |
| TERM-CURRENT | PASS | 当前账期=2026-12（未继承创建者账期） |
| SWITCH-BOOK | PASS | bookId=2105456763365081090 |
| SUBJECTS | PASS | bank=1002 capital=3001 revenue=5001 expense=5602.01 profit=3103 und=3104.02 |
| LEDGER-OPENING | PASS | 总账期初 1002借/3001贷 = 50000 |
| V-DRAFT / V-POST | PASS | 收入 V01、费用 V02 审核过账 |
| BAL-BEFORE-PL | PASS | 收入余额=−10000 费用余额=3000 |
| CARRY-qm_jz_sr | PASS | voucherId=2105456805173903361 |
| CARRY-qm_jz_cbfy | PASS | voucherId=2105456805886935041 |
| PL-ZERO-INCOME-EXPENSE | PASS | 收入=0 费用=0 |
| PL-YEAR-PROFIT | PASS | 本年利润=−7000（贷方口径，\|余额\|=7000） |
| IS-AFTER-PL | PASS | 利润表本期净利润=7000 收入=10000 管理费=3000 |
| CARRY-qm_jz_bnlr | PASS | voucherId=2105456825839239170 codes=3103,3104.02 |
| YE-PROFIT-ZERO | PASS | 本年利润=0 |
| YE-UNDISTRIBUTED | PASS | 未分配利润=−7000 Δ≈7000 |
| IS-AFTER-YE | PASS | 年末结转后利润表本期净利润仍为 7000 |
| SETTLE-VERIFY / CHECKOUT | PASS | closed=2026-12 → next=2027-01 |
| OPENING-INHERIT | PASS | 2027-01 银行期初=57000 |
| YTD-CLEAR | PASS | 2027-01 收入本年累计=0 |
| NON-DEC-BNLR | PASS | book C term=2026-01 → code=2「非年末，无需结转本年利润」 |
| REMEDIATE-TERM-BLEED | PASS | 结账后写回 A=2026-03、B/C/template=2026-01 |
| RESTORE-BOOK-A | PASS | admin default → 2105377998655979522 |

## 金额轨迹

| 检查点 | 预期 | 实际 |
|---|---:|---:|
| 期初银行/资本 | 50000.00 | 50000.00 |
| 12 月收入 | 10000.00 | 10000.00 |
| 12 月费用 | 3000.00 | 3000.00 |
| 净利润 | 7000.00 | 7000.00 |
| 损益结转后本年利润 | 7000.00（贷） | −7000（signed） |
| 年末结转后本年利润 | 0.00 | 0 |
| 年末结转后未分配利润 | +7000.00（贷） | −7000（signed） |
| 年末后利润表本期净利润 | 7000.00 | 7000 |
| 结账后账期 | 2027-01 | 2027-01 |
| 2027-01 银行期初 | 57000.00 | 57000 |
| 2027-01 收入本年累计 | 0.00 | 0 |

## 年末结转凭证

- 损益结转：`qm_jz_sr`（2105456805173903361）+ `qm_jz_cbfy`（2105456805886935041）→ 本年利润 = **7000**
- 年末结转：`qm_jz_bnlr`（2105456825839239170）借 3103 / 贷 3104.02 = **7000**
- 非 12 月拦截（账套 C）：`term=2026-01 code=2 msg=非年末，无需结转本年利润`

## OBS / 缺陷

### BUG-TERM-CROSS-BOOK（P1，结账副作用）

- **现象**：账套 D 结账 `termToNext` 后，`config` 表中 **A / C / template** 的 `sys.payment.term.current` 被一并写成 `2027-01`（同秒 `modified_date`）。
- **根因**：`ConfigSysService.getBookConfigList` 只 `select(configKey, configValue)`，`updateCurrentTerm` → `update()` 在无 `configId`/`bookId` 时按 `configKey` 全表更新。
- **影响**：污染其他账套当前账期（本任务已 SQL 写回 A=`2026-03`、B/C/template=`2026-01`；D 保持 `2027-01`）。
- **脚本防护**：`scripts-ai-ui-book-d.mjs` 结账后调用 `remediateCrossBookTermBleed()`；已结账复跑走校验模式，不再强制回退 `2026-12`。

### 其他观察

- 新建账套当前账期可能继承创建者账套（专项 C 已记）；本账套创建后即为 `2026-12`，未触发强制改写。
- 科目余额 `balance` 对权益贷方为负号；断言用绝对值。

## 证据截图

- `/opt/cursor/artifacts/screenshots/bookd-opening.webp`
- `/opt/cursor/artifacts/screenshots/bookd-vouchers.webp`
- `/opt/cursor/artifacts/screenshots/bookd-carry-pl.webp`
- `/opt/cursor/artifacts/screenshots/bookd-subject-after-pl.webp`
- `/opt/cursor/artifacts/screenshots/bookd-carry-yearend.webp`
- `/opt/cursor/artifacts/screenshots/bookd-income-after-yearend.webp`
- `/opt/cursor/artifacts/screenshots/bookd-checkout.webp`
- `/opt/cursor/artifacts/screenshots/bookd-2027-01.webp`

## 保护约束

- 未改写账套 A/B/C **业务凭证**；仅对 C 做 `qm_jz_bnlr` 生成拦截探测
- 结账副作用导致的 A/C/template 账期污染已写回报告约定值
- 结束时将 admin 默认账套切回 A（`2105377998655979522`）
