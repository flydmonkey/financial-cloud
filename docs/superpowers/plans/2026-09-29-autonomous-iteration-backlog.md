# 自主迭代 backlog（代账专业可用）

> 用户授权自主推进，少打断。每刀：propose → apply → archive → commit。
>
> **状态：已完成（队列关闭）** — 2026-09-29。
>
> **后续主轴**：专业可用收口后的下一程序见 [代账吞吐第二曲线设计规格](../specs/2026-09-29-daizhang-throughput-v2-design.md)（Path B：吞吐 + 薄质量门禁）。

## 专业可用验收条（本仓库口径）

主做账闭环可交付代账客户：建账→期初→凭证→账簿→结账→三表→账本包→备份；打印可用；往来/薪资/日记账可用。近端文档与代码状态一致。

**验收结论：已达成。**

## 队列（全部归档）

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
| 13 | `dashboard-asset-count` | 已归档 | 首页固定资产规模卡 |
| 14 | `journal-voucher-reverse-sync` | 已归档 | 凭证删/改回写或解绑日记账 |
| 15 | `daizhang-professional-closeout` | 已归档 | 专业可用程序收口（文档关账） |

## Post-V1 / Non-goal（不阻塞专业可用）

| 项 | 分类 | 说明 |
|----|------|------|
| 工资条员工自助 | Non-goal | 本期明确不做 |
| 税局直连 | Non-goal | 申报底稿已有；直连不做 |
| 移动端 / AI | Non-goal | — |
| 盘盈自动入账 | **已落地** | `surplusPreview` / `bookSurplus` + `check.vue` 盘盈入账（2026-09-29） |
| 凭证/结账参数独立配置中心 | 已落地 | `voucher-settlement` 页 + `settlement.verify.arap.enabled` |
| 按钮级精细权限铺开 | Post-V1 | 角色-资源 + 账套授权已够用 |

## 决策原则（历史）

- 优先代账吞吐与合规交付，不做税局直连/移动端/AI。
- 复用现有 ExcelImporter/Exporter 与固定资产导入交互。
- 每刀保持可独立验收、可回滚。
