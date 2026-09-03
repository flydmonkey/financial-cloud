# 08 · 出纳日记账

> 状态：部分实现（账户、流水、汇总、生成凭证、结账联动可用；凭证不回写流水；银行调节等未做）

## 1. 模块定位

管理现金/银行账户与收支流水，支持由流水生成会计凭证，并在结账/反结账时处理账户期初与余额快照。此模块为 **PRD V1.0 未单列、代码已实现** 的能力。

## 2. 典型场景

1. 维护基本户与现金账户及期初。
2. 登记日常收支流水，月末批量生成凭证。
3. 结账时账户余额结转；反结账恢复 `prev_opening_balance`。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 账户管理 CRUD | **已实现** | `journalaccout.vue`（文件名拼写为 accout） |
| 账户余额汇总 | **已实现** | `allBalance` 等 |
| 日记账流水 CRUD | **已实现** | `journalentry.vue` |
| 流水生成凭证 | **已实现** | `generate-voucher` → `voucherService.save` |
| 账户期间汇总 | **已实现** | `journalsummary.vue` |
| 结账时账户 checkout | **已实现** | `JournalAccountService.checkout` |
| 反结账恢复期初 | **已实现** | `restoreOpeningFromPrev` |
| 凭证修改回写流水 | **未实现** | 单向：流水 → 凭证 |
| 银行对账调节表 | **未实现** | OpenSpec backlog 提及，未实现 |

## 4. 页面与路由

| 页面 | 路由 | 组件 |
|------|------|------|
| 日记账 | `/journal/journalentry` | `journalentry.vue` |
| 账户管理 | `/journal/journalaccout` | `journalaccout.vue` |
| 账户汇总 | `/journal/journalsummary` | `journalsummary.vue` |

## 5. 数据模型

| 表 | 用途 |
|----|------|
| `journal_account` | 现金/银行账户、余额、期初及 prev_opening |
| `journal_entry` | 收支流水 |
| `journal_summary` | 期间汇总 |

## 6. 核心接口

| 前缀 | 说明 |
|------|------|
| `/api/journal/account` | 账户 CRUD、余额 |
| `/api/journal/entry` | 流水 CRUD、`generate-voucher` |
| `/api/journal/summary` | 汇总查询 |

删除流水/生成凭证时调用 `settlementService.check()`，已结账期间受限。

## 7. 业务规则与约束

1. **单向联动**：流水可生成凭证；改凭证不自动改流水。
2. **结账协同**：与 `SettlementService`、反结账补丁 `journal-account-prev-opening.sql` 配合。
3. 账户与科目的映射关系以业务配置/分录生成为准。

## 8. 已知缺口

- 银行余额调节、流水与银行回单核销。
- 与往来核销、资金看板更深整合（看板已有资金类指标）。

## 9. 证据索引

- `JournalEntryController`、`JournalAccountController`、`JournalSummaryController`
- `JournalEntryService.generateVoucher`、`JournalAccountService`
- `views/journal/*`
- OpenSpec backlog：`daizhang-polish-priority/backlog-p1-p2.md`
