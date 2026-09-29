## Why

代账交付的打印链路大半已落地（经典凭证单张/批量、资产负债表/利润表/账簿等浏览器打印），但现金流量表打印按钮仍被注释，凭证编辑页还残留 iframe/过次页打印死代码，产品文档仍写「打印未接入」。S1 应做的是收口打磨，而不是再造一套服务端 PDF。

## What Changes

- 现金流量表页启用「打印」，走与资产负债表等相同的 `tablePrint` 通道（新窗口表格 → 浏览器打印/另存 PDF）。
- 清理凭证编辑页中不再被生产入口调用的 iframe / 过次页打印死代码；确认经典静态页为唯一生产打印路径。
- 标记或移除已无生产引用的 `voucherPrintHtml.ts`（及易漂移的样张），避免后续改打印时改错文件。
- 同步产品文档（`03-voucher` / `05-statement` / `04-ledger` / `20-gap` / `21-roadmap` / `00-overview` 相关段落），反映打印已交付与本次收口。

## Capabilities

### New Capabilities

- `print-delivery`：报表与凭证的打印交付收口——现金流量表可打印；凭证经典打印为唯一生产入口；无服务端 PDF 要求。

### Modified Capabilities

- （无）

## Impact

- **前端**：`cash-flow-statement.vue` 补打印；`voucher-edit.vue` 删减死代码；可能调整/删除 `voucherPrintHtml.ts` 及其测试。
- **后端 / API**：无变更。
- **文档**：产品分册与差距/路线图状态表。
- **非目标**：服务端生成 PDF、新打印模板、多账期批量打印、税局样式套打。
