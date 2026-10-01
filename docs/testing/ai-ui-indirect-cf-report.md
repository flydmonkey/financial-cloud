# AI UI 主账套现金流量 / 间接法点测

- **time**: 2026-10-01T00:34Z
- **book**: `2105377998655979522`（主账套 A）
- **脚本**: `financial-cloud-ui/scripts-ai-ui-indirect-cf.mjs`
- **API**: `GET /api/statement/cash-flow?periodType=month&reportDate=YYYY-MM`

## 独立预期（提示词）

| 期间 | 经营净额（直接） | 间接法勾稽 |
|---|---:|---|
| 2026-01 | 50−10−8=**32,000** | 40k+10k−30k+12k=**32,000** |
| 2026-02 | 40−5−7=**28,000** | 13k+12k+10k−7k=**28,000** |
| 现金 | 期初 100k → 1 月末 160k → 2 月末 188k | |

## 2026-01 实际（monthlyAmount）

| 项目 | 实际 | 预期 | 结果 |
|---|---:|---:|---|
| 销售商品收到现金 | 50,000 | 50,000 | PASS |
| 购买商品支付现金 | 8,000 | 8,000 | PASS |
| 支付其他经营现金（V04） | **0** | 10,000 | FAIL（未指定 CF） |
| 购建固定资产 | 12,000 | 12,000 | PASS |
| 取得借款 | 40,000 | 40,000 | PASS |
| 经营净额（主表/附表） | **42,000** | 32,000 | FAIL（缺 V04） |
| 现金净增加 | **70,000** | 60,000 | FAIL |
| 期末现金 | **170,000** | 160,000 | FAIL |
| 净利润（附表） | 40,000 | 40,000 | PASS |
| 存货减少 | 10,000 | 10,000 | PASS |
| 经营性应收减少 | **−18,000** | −30,000 | FAIL（差额 12,000） |
| 经营性应付增加 | 12,000 | 12,000 | PASS |

说明：主表经营净额与附表净额均为 42,000（相互一致），但与独立预期差 10,000（V04 CF 缺失）。应收调整 −18k 与期初 20k→期末 50k（+30k）不符，记 **OBS-CF-AR-ADJ**。

## 2026-02 实际

| 项目 | monthlyAmount | currentAmount(YTD) | 预期本月 | 结果 |
|---|---:|---:|---:|---|
| 销售商品 | **null** | 50,000 | 40,000 | FAIL（2 月未指定 CF） |
| 购买商品 | null | 8,000 | 7,000 | FAIL |
| 支付其他 | null | 0 | 5,000 | FAIL |
| 经营净额 | **0** | 42,000 | 28,000 | FAIL |
| 净利润 | **13,000** | 53,000 | 13,000 | PASS（附表） |
| 存货减少 | **12,000** | 22,000 | 12,000 | PASS |
| 经营性应收减少 | **10,000** | −15,000 | 10,000 | PASS |
| 经营性应付增加 | **−7,000** | 5,000 | −7,000 | PASS |
| 期初现金 | **30,000** | 100,000 | 160,000 | FAIL |
| 期末现金 | **30,000** | 170,000 | 188,000 | FAIL |

附表四项独立合计 13+12+10−7=**28,000**，但 `57-xj-jyje` 本月净额仍为 **0**（**OBS-CF-INDIRECT-NET-ZERO**：直接法无本月指定时附表净额未按调整项汇总）。

## 缺陷 / 观察（历史快照；后续已补全）

1. **OBS-CF-V04-MISSING** — 已在 cf-remediate 补指定闭环。  
2. **OBS-CF-M2-UNSPECIFIED** — 已在 cf-remediate 补指定闭环。  
3. **OBS-CF-AR-ADJ** — **CLOSED（口径说明）**：间接法经营性应收含预付款项，−18,000 正确；见 `ai-ui-cf-begin-cash-fix-report.md`。  
4. **OBS-CF-INDIRECT-NET-ZERO** — CF 补全后附表经营净额本月 28,000。  
5. **OBS-CF-BEGIN-CASH-FEB** — **FIXED**：上期期末改为实时重算，2 月期初本月列 160,000。

## 证据

- `/opt/cursor/artifacts/screenshots/indirect-cf-ui.webp`
- `/opt/cursor/artifacts/screenshots/indirect-cf-ui-bottom.webp`
- `/opt/cursor/artifacts/screenshots/cf-begin-cash-feb-fixed.webp`
