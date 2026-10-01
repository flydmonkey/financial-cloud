# OBS-CF-BEGIN-CASH-FEB / OBS-CF-AR-ADJ 闭环

- **time**: 2026-10-01T01:12Z
- **book**: `2105377998655979522`（主账套 A）
- **修复**：`StatementReportService` 非首期期初现金改为**实时重算上期现金流量表期末**，`settlement.ending_balance` 仅兜底

## 根因

| 观察项 | 根因 | 处置 |
|---|---|---|
| OBS-CF-BEGIN-CASH-FEB | 1 月结账时 CF 未齐，`settlement.ending_balance=30,000` 冻结；2 月期初读快照 → 30,000≠160,000。另有多条软删重复结账行。 | 代码：上期期末实时重算；数据：活跃行已同步为 160,000 / 188,000 |
| OBS-CF-AR-ADJ | 间接法「经营性应收」= **应收账款 + 预付款项**。1 月应收 20→50、预付 12→0 ⇒ −18,000。提示文档按仅应收 −30,000 简化预期。 | **非缺陷**：单测锁定口径；报告预期改为 −18,000 |

## API 复核（修复后）

| 期间 | 期初现金(本月) | 期末现金(本月) | 经营净额 | 经营性应收调整 | 结果 |
|---|---:|---:|---:|---:|---|
| 2026-01 | 100,000 | 160,000 | 32,000 | **−18,000** | PASS（口径含预付） |
| 2026-02 | **160,000** | 188,000 | 28,000 | 10,000 | PASS（期初已对齐） |

## 证据

- `/opt/cursor/artifacts/screenshots/cf-begin-cash-feb-fixed.webp`
- 单测：`StatementCashFlowIndirectRulesTest.operatingReceivable_includesPrepaid_explainsAiUiJanAdj`
