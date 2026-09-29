# 自主迭代 backlog（代账专业可用）

> 用户授权自主推进，少打断。每刀：propose → apply → archive → commit。

## 专业可用验收条（本仓库口径）

主做账闭环可交付代账客户：建账→期初→凭证→账簿→结账→三表→账本包→备份；打印可用；往来/薪资/日记账可用。近端文档与代码状态一致。

## 队列

| # | Change | 状态 | 说明 |
|---|--------|------|------|
| 1 | `print-delivery-polish` | 已归档 | 打印收口 |
| 2 | `opening-balance-excel` | 已归档 | 期初 Excel 导出/模板/导入 |
| 3 | `docs-as-built-sync` | 已归档 | 分册/overview 与已落地能力全面对齐 |
| 4 | `server-pdf-channel` | 已归档 | 服务端 PDF：资产负债/利润/科目余额 |
| 5 | `scheduled-book-backup` | 已归档 | 定时备份落盘 + 保留策略 |
| 6 | `server-pdf-cash-flow` | 已归档 | 现金流量表服务端 PDF |
| 7 | `server-pdf-ledgers` | 已归档 | 总账 + 明细账服务端 PDF |
| 8 | `server-pdf-expense-detail` | 已归档 | 费用明细表服务端 PDF |
| 9 | `server-pdf-specialty-ledgers` | 已归档 | 多栏账 + 数量金额账服务端 PDF |
| 10 | `server-pdf-voucher-summary` | 已归档 | 凭证汇总表服务端 PDF |
| 11 | `book-backup-overwrite` | 已归档 | 覆盖式恢复（确认短语 + 预备份 + 封存拒绝） |
| 12 | `book-void-retain` | 已归档 | 有凭证禁硬删；留存=封存 |

## 后续候选（按客户声音）

| 项 | 说明 |
|----|------|
| 工资条员工自助 | Non-goal |

> **里程碑**：代账专业可用验收条（闭环+打印+往来/薪资/日记账+备份含覆盖恢复+文档对齐）已达成。

## 决策原则

- 优先代账吞吐与合规交付，不做税局直连/移动端/AI。
- 复用现有 ExcelImporter/Exporter 与固定资产导入交互。
- 每刀保持可独立验收、可回滚。
