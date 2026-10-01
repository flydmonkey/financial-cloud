# AI UI Guards / Export Smoke（主账套 A）

- **time**: 2026-10-01T00:22Z（含恢复）
- **bookId**: `2105377998655979522`（`AI-UI-20260930-主账套A`）
- **脚本**: `financial-cloud-ui/scripts-ai-ui-guards.mjs`

## 结论：守卫主路径 PASS（恢复后账期仍为 2026-03）

| ID | Status | Detail |
|---|---|---|
| SWITCH-A | PASS | `GET /api/users/switchBook/{bookA}` |
| CLOSED-EDIT-BLOCK | PASS | 改已结月凭证：`已结账期间不允许新增或修改凭证（当前开放账期 2026-03）` |
| CLOSED-UNSENDER-BLOCK | PASS | 反过账：`没有可以反过账的凭证（需为已过账且所在期间未结账）` |
| UNCHECKOUT-NON-LATEST | PASS | 反 2026-01：`只能反结账最近已结期间[2026-02]` |
| UNCHECKOUT-LATEST | PASS | 反 2026-02 成功，账期回到 2026-02 |
| RECHECKOUT-FEB | PASS（手动） | `GET /api/settlement/checkout` 可结账；首次脚本误用 POST→405，随后误连结空月，已反结账恢复至 **2026-03** 开放、01/02=已结 |
| UI-UNCHECKOUT-ENTRY | PASS | 账期列表可见「反结账」（主账套 A） |
| UI-EXPORT-ENTRY | PASS | 资产负债表导出/PDF 入口存在 |
| EXPORT-DOWNLOAD | PASS | `资产负载表2026-02 …xlsx` size≈11KB |
| EXPORT-CONTENT-SMOKE | PASS | 非空 xlsx；文件名含「资产」 |

## 事故与恢复

1. 并发时曾误在专项 B 会话反过账日记账凭证，已重新过账恢复。  
2. 复测 uncheckout 后用错误方法结账，空跑结账推进到 05 月；已连续反结账回到 **2026-03**。

## 证据

- `/opt/cursor/artifacts/screenshots/guards-settle-list-a.webp`
- `/opt/cursor/artifacts/screenshots/guards-balance-sheet-a.webp`
- `/opt/cursor/artifacts/downloads/`
