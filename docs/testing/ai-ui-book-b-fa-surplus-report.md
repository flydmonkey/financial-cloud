# AI UI 专项账套 B — 5.3 盘盈入账 book-surplus

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项B`
- **bookId**：`2105448444973871105`
- **启用期间**：`2026-01`（凭证审核开启）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T01:28:17.547Z
- **脚本**：`financial-cloud-ui/scripts-ai-ui-book-b-fa-surplus.mjs`

## 结论：**PASS**（PASS 20 / FAIL 0 / WARN 0）

独立探测卡入账；主卡 `B-ASSET-001` 仅核对未改数量/净值。

## 场景

| 场景 | 结果 | 说明 |
|---|---|---|
| split_card（qty=1） | PASS | B-ASSET-SUR-SPLIT +1 → 新卡；凭证 Dr1601/Cr5301.04=3000 |
| bump 数量（qty>1 无折旧） | PASS | B-ASSET-SUR-BUMP qty 2→3；原值 +1000 |
| 已折旧禁止 bump | PASS | B-ASSET-SUR-DEPR preview 警告 + book skip |
| 主卡完整 | PASS | qty/净值保持 |

## 结果表

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN | PASS | admin + ai_reviewer |
| SWITCH-BOOK | PASS | bookId=2105448444973871105 term=2026-01 |
| SPLIT-PURCHASE-POST | PASS | 已过账 id=2105469473951625218 |
| BUMP-PURCHASE-POST | PASS | 已过账 id=2105469474522050561 |
| DEPR-PURCHASE-POST | PASS | 已过账 id=2105469475008589826 |
| DEPR-READY | PASS | qty=2 accumDepr=200 (openingAccum fixture; month accrue already locked) |
| CHECK-CREATE-B-ASSET-SUR-SPLIT | PASS | reuse id=2105469487662043137 status=completed |
| CHECK-COMPLETE-B-ASSET-SUR-SPLIT | PASS | already completed |
| SURPLUS-PREVIEW-B-ASSET-SUR-SPLIT | PASS | already booked voucher=2105469488010932225 amt=3000 |
| SPLIT-CLONE | PASS | code=B-ASSET-SUR-SPLIT-副本 qty=1 cost=3000 |
| CHECK-CREATE-B-ASSET-SUR-BUMP | PASS | reuse id=2105469488559624194 status=completed |
| CHECK-COMPLETE-B-ASSET-SUR-BUMP | PASS | already completed |
| SURPLUS-PREVIEW-B-ASSET-SUR-BUMP | PASS | already booked voucher=2105469488870764545 amt=1000 |
| BUMP-CARD-AFTER | PASS | qty=3 originalValue=3000 |
| CHECK-CREATE-B-ASSET-SUR-DEPR | PASS | id=2105469791975575554 |
| CHECK-COMPLETE-B-ASSET-SUR-DEPR | PASS | surplus=1 deficit=0 |
| SURPLUS-PREVIEW-B-ASSET-SUR-DEPR | PASS | strat=bump_qty warn=该卡已有累计折旧：不允许在原卡累加数量入账；请将账面数量调整为 1 后按拆新卡入账，或手工处理。 hasDepr=true |
| SURPLUS-BOOK-B-ASSET-SUR-DEPR | PASS | processed=0 skipped=[{"assetCode":"B-ASSET-SUR-DEPR","assetName":"AI-UI-20260930-盘盈禁bump探测","reason":"原卡已有累计折旧，禁止 bump 数量入账；请将账面数量改为 1 后拆新卡入账"}] |
| GL-DELTA | PASS | 1601 Δ=0 (expect 0); 5301.04 Δ=0; fa=23400 gain=-4000 |
| MAIN-INTACT | PASS | qty=1 (was 1) net=11000 |

## 金额

| 检查点 | 预期 | 实际 |
|---|---:|---:|
| 主卡净值 | 11000 | 11000 |
| GL 1601（含购入+盘盈后） | 23,400 | 23400 |
| GL 5301.04（盘盈利得贷方） | −4,000 | -4000 |
| 首跑盘盈 Δ1601（拆卡 3000+累加 1000） | 4,000 | 已入账（本表为幂等复跑 Δ=0） |
| 拆卡新卡原值 | 3000 | 3000 |
| 累加后原卡数量 | 3 | 3 |

## 证据截图

- `/opt/cursor/artifacts/screenshots/bookb-fa-surplus-preview.webp`
- `/opt/cursor/artifacts/screenshots/bookb-fa-surplus-cards.webp`
- `/opt/cursor/artifacts/screenshots/bookb-fa-surplus-balance.webp`
