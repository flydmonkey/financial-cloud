# 代账工作台（books-board）收口 backlog

> 规格：[2026-09-29-daizhang-books-board-design.md](../specs/2026-09-29-daizhang-books-board-design.md)  
> 计划：[2026-09-29-daizhang-books-board.md](2026-09-29-daizhang-books-board.md)  
> 状态：**已完成（程序收口）** — 2026-09-29  
> PR：[#6](https://github.com/flydmonkey/financial-cloud/pull/6)

## 验收（本程序口径）

做账员打开代账工作台即可：

1. 看到授权账套的账期、待办、结账启发式状态与阻塞摘要；
2. 「进入处理」切账套并跳到阻塞页（含落后 → 结账向导）；
3. 对关注月已结账套单套/批量导出账本包。

**结论：已达成。** CI backend-test / frontend-check 绿；本地 API/UI 与已结导出已验。

## 交付清单

| 项 | 状态 |
|----|------|
| Board API + 紧急度排序 + 上限 200 | 已完成 |
| 账本包 bookId / export-batch | 已完成 |
| 前端看板 + 摘要筛选 + session 关注月 | 已完成 |
| 菜单种子 + 四角色 RBAC 包 | 已完成 |
| 首页快捷入口 | 已完成 |
| 单元测试（后端启发式/Board/账本包；前端 utils） | 已完成 |
| E2E API scaffold `books-board.spec.ts` | 已完成 |
| 产品文档 overview / roadmap 对齐 | 已完成（本收口） |

## Post–books-board（不阻塞收口）

| 项 | 说明 |
|----|------|
| OpenSpec change 归档文 | 可选；本程序以 specs/plans 为权威 |
| 完整 Playwright UI E2E（双账套） | 可选增强 |

## 产品边界（已确认）

**不做跨账套结账。** 结账/过账只在单账套内完成。代账工作台边界止于：跨套进度一览、「进入处理」跳进单套、已结账本包导出。

明确排除（不排期、不另开候选）：

- 跨账套一键/批量结账、批量过账
- 「跨套结账就绪软报告」或任何跨套结账编排/就绪聚合写路径准备
- 跨账套 `SettlementService.verify` 编排引擎

## Non-goals（仍不做）

跨账套结账/过账（见上）、verify 编排引擎、SaaS 多租户计费、OCR/移动端/AI/税局直连。
