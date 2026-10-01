# AI UI CF Remediation + Recheck（主账套 A）

- **time**: 2026-10-01T00:46Z
- **book**: `2105377998655979522`
- **脚本**: `financial-cloud-ui/scripts-ai-ui-cf-remediate.mjs`

## 动作

1. 反结账 2026-02 → API 指定 M2-V02/V05 CF；M2-V04 曾误绑到 V04，已 SQL 纠正到正确分录  
2. SQL 补 V04 `9-jy-zfqt` 10,000（闭账期 API 禁止修改）  
3. 重新结账 2026-02；当前账期仍为 **2026-03**；他账套账期未串改（BUG-TERM-CROSS-BOOK 修复生效）

## 复核结果（monthlyAmount）

| 期间 | 销售 | 购货 | 其他流出 | 经营净额 | 期初现金 | 期末现金 | 结果 |
|---|---:|---:|---:|---:|---:|---:|---|
| 2026-01 | 50,000 | 8,000 | 10,000 | **32,000** | 100,000 | **160,000** | PASS |
| 2026-02 | 40,000 | 7,000 | 5,000 | **28,000** | **30,000** | 58,000 | 主表净额 PASS；期初本月列 FAIL |

- 2 月 YTD 期末现金 **188,000** PASS  
- 2 月附表调整项合计仍为 28,000，且附表经营净额本月亦为 **28,000** PASS  
- **已闭环（2026-10-01）**：`OBS-CF-BEGIN-CASH-FEB` — 期初改为实时重算上期 CF 期末，2 月期初本月列 **160,000**；`OBS-CF-AR-ADJ` — 产品口径含预付款项，−18,000 为正确值（见 `ai-ui-cf-begin-cash-fix-report.md`）

## 证据

- `/opt/cursor/artifacts/screenshots/cf-remediate-ui.webp`
- `/opt/cursor/artifacts/screenshots/cf-begin-cash-feb-fixed.webp`
