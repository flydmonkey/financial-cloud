# AI UI 专项账套 B — 5.3 深路径：固定资产清理 / 盘点

- **标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-专项B`
- **bookId**：`2105448444973871105`
- **启用期间**：`2026-01`（凭证审核开启）
- **环境**：前端 `http://127.0.0.1:3154` / 后端 `http://127.0.0.1:2154`
- **执行时间**：2026-10-01T00:50:27.615Z（脚本终跑）+ 手工补截图
- **脚本**：`financial-cloud-ui/scripts-ai-ui-book-b-fa-dispose.mjs`

## 结论：**PASS**（核心清理/盘点深路径完成；主卡净值 11,000 未破坏）

独立探测卡（`B-ASSET-DISP-002` / `B-ASSET-CHK-*`），不改动主卡 `B-ASSET-001`（原值 12,000 / 净值 11,000）。盘盈仅 preview，未执行 `book-surplus`。

## 结果表（终跑摘要）

| 步骤 | 结果 | 说明 |
|---|---|---|
| LOGIN / SWITCH-BOOK | PASS | admin + ai_reviewer；term=2026-01 |
| DISP-CARD `B-ASSET-DISP-002` | PASS | 原值 2,000；购入凭证过账 |
| DISP-DISPOSE | PASS | `POST /api/fixed-asset/card/dispose/{id}` → DISPOSED；voucher `2105459609103888385` |
| DISP-VOUCHER-LINES | PASS | Dr1606 / Cr1601 / Dr5711.02 / Cr1606（净损失 2,000）已过账 |
| DISP-GL | PASS | 清理后 1606=0；1601 回到主卡路径口径 |
| CHECK 盘点深路径 | PASS | 建单→实盘→完成：盘盈 1（主卡 actual=2）/ 盘亏 1（CHK-001 actual=0） |
| CHECK-SURPLUS-PREVIEW | PASS | `surplus-preview` rows=1，`B-ASSET-001+1` amt=12000 strat=`split_card`；**未入账** |
| CHECK-DEFICIT（长标题） | FAIL→workaround | 长标题摘要超 `varchar(64)`（见缺陷） |
| CHK 直接清理 fallback | PASS | `card/dispose` 短摘要；CHK-001 DISPOSED |
| CHECK2 `B盘点2` | PASS | 短标题+短卡名 → `dispose-deficit` processed=1 |
| MAIN-INTACT | PASS | 主卡仍 IN_USE cost=12000 net=11000 |
| GL-FINAL | PASS | **1601=12,000** / 1602=-1,000 / 1606=0 / 5711.02=6,000 |

## 金额核对

| 检查点 | 预期 | 实际 |
|---|---:|---:|
| 主卡净值 | 11,000 | 11,000 |
| DISP-002 清理净损失 | 2,000 | 2,000 |
| 总账 1601（终） | 12,000 | 12,000 |
| 总账 1606（终） | 0 | 0 |
| 总账 5711.02（终） | 6,000（探测清理累计） | 6,000 |

## 阻塞 / 缺陷

- **BUG-FA-CHECK-DEFICIT-SUMMARY（P1）**：`dispose-deficit` 拼摘要 `盘亏下账（盘点单：{title}）：{code} {name}`，长 MARK 标题实测 68 字符 → `voucher_item.summary varchar(64)` truncation，MyBatis batch flush 失败，`processedCount=0`。短标题 `B盘点2` + 短卡名 `B盘亏2` → **PASS**。
- **BUG-FA-DISPOSE-VOUCHER-DATE（P1）**：`dispose` / `dispose-deficit` 省略 `voucherDate` 时用 `new Date()`；环境日为 2026-10-01 时草稿落在 10 月，开放账期 2026-01 下提交报「非当前期」。Workaround：`voucherDate` 传 epoch millis，或 `voucher/update` 纠期后再审核过账。`yyyy-MM-dd` 字符串会被 Jackson 拒收整包（「缺少请求体」）。
- **OBS-DISP001-ORPHAN**：探测中误删 DISP-001 清理凭证后卡仍 DISPOSED；已用 Dr5711.02/Cr1601 补偿。正式路径以 **DISP-002** 为准。

## 证据截图

- `/opt/cursor/artifacts/screenshots/bookb-fa-disp-cards.webp` — 状态=已清理，探测卡列表
- `/opt/cursor/artifacts/screenshots/bookb-fa-disp-voucher.webp` — 清理凭证已过账（1606/1601/5711.02）
- `/opt/cursor/artifacts/screenshots/bookb-fa-disp-balance.webp` — 科目余额 1601=12,000 / 1606 清零
- `/opt/cursor/artifacts/screenshots/bookb-fa-check-list.webp` — 盘点单列表（盘盈/盘亏）
- `/opt/cursor/artifacts/screenshots/bookb-fa-check-detail.webp` — 明细：主卡盘盈 / CHK 盘亏
- `/opt/cursor/artifacts/screenshots/bookb-fa-check-surplus-preview.webp` — 盘盈入账预览弹窗（已取消，未确认）
