# 03 · 凭证管理

> 状态：部分实现（录入→审核→过账主路径可用；附件文件、同人审核校验、经典打印模板接入、rejected/cancelled 写入未完成）

## 1. 模块定位

日常做账核心：凭证录入与列表管理、审核与过账、断号整理、模板、打印与导出。日记账、固定资产、薪资等模块可**生成凭证**汇入本模块。

## 2. 典型场景

1. 会计在整页工作台录入借贷分录，保存草稿或提交审核。
2. 审核人批量审核；出纳/会计批量过账，科目余额更新。
3. 发现断号后执行顺次或按日期整理。
4. 调用常用模板快速填充差旅/工资等凭证。

## 3. 功能清单

| 能力 | 状态 | 说明 |
|------|------|------|
| 凭证录入 / 编辑 / 查看 | **已实现** | `voucher-edit.vue` 整页工作台 |
| 草稿 / 提交 | **已实现** | `POST /draft`、`/submit` |
| 借贷平衡与空行校验 | **已实现** | 前后端校验 |
| 科目模糊搜索、辅助核算弹层 | **已实现** | `SelectAuxiliary` |
| 凭证模板 CRUD 与套用 | **已实现** | `voucher-template` |
| 列表多维筛选、批量审核/取消审核 | **已实现** | `voucher-index.vue` |
| 批量过账 / 取消过账 | **已实现** | `sender` / `unsender` |
| 主管复核字段 | **已实现** | `manage-audit`（不改变 status） |
| 断号检测与整理 | **已实现** | GET/PUT `/successive` |
| Excel 导出 | **已实现** | `/export` |
| 打印 | **部分实现** | 页面内 iframe 打印可用；经典 HTML 模板未接入生产入口 |
| 附单据张数 | **已实现** | `receipt_num` 数字 |
| 附件文件上传绑定凭证 | **未实现** | `file_storage` 无 voucher 关联 |
| 制单人 ≠ 审核人 | **未实现** | `audit()` 无同人校验 |
| 作废状态 `cancelled` / 拒绝 `rejected` | **未实现** | 枚举存在，后端不写入 |
| 邻证导航、留页保存、脏数据守卫 | **已实现** | OpenSpec `voucher-entry-workspace` 已落地 |

## 4. 页面与路由

| 页面 | 路由 | 组件 |
|------|------|------|
| 凭证列表 | `/voucher/voucher-index` | `views/voucher/voucher-index.vue` |
| 新增/编辑凭证 | `/voucher/voucher-edit` | `views/voucher/voucher-edit.vue` |
| 凭证模板 | `/voucher/voucher-template` | `views/voucher/voucher-template.vue` |
| 临时打印路由 | `/temporary/voucher-print` | 同 edit 组件 |

## 5. 数据模型

### `voucher`（实体 `Voucher`）

关键字段：`word` / `word_head` / `word_num`、`voucher_date`、借贷合计、`receipt_num`、`carry_forward`、审核人字段、`sender_*`（过账）、`manager_*`（主管）、`status`、`book_id`。

### 审核状态 `status`（`VoucherStatusEnum`）

| 值 | 含义 | 是否实际写入 |
|----|------|--------------|
| `draft` | 暂存 | 是 |
| `reviewing` | 审核中 | 是（账套开启审核时） |
| `completed` | 已审核 | 是 |
| `rejected` | 被拒绝 | **否**（仅枚举） |
| `cancelled` | 已取消 | **否**（仅枚举） |

### 过账状态（独立于 status）

- 未过账：`sender_id` 为空  
- 已过账：写入 `sender_id` / `sender_name` / `sender_date`，并更新科目余额

### 关联表

| 表 | 用途 |
|----|------|
| `voucher_item` | 分录行（含 num/price，UI 未做数量金额账） |
| `voucher_auxiliary` | 分录辅助核算 |
| `voucher_item_cash_flow` | 分录现金流量指定 |
| `voucher_word` | 字号序列 |
| `voucher_template` / `voucher_template_item` | 模板（含期末结转类） |

## 6. 状态机

```
draft ──submit──► reviewing ──audit──► completed ──sender──► completed+已过账
  ▲                  │                    │                      │
  └── cancel 撤回 ───┘                    │                      │
                     ◄── unaudit ─────────┘                      │
                     ◄──────────── unsender（清 sender，回滚余额）─┘

账套关闭审核：submit 可直接 → completed
manage-audit：写入主管字段，不改 status
```

证据：`VoucherService`（`submit` / `audit` / `unaudit` / `sender` / `unsender` / `manageAudit`）。

## 7. 核心接口

前缀：`/api/voucher`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/fetch`、`/get/{id}`、`/items/fetch` | 列表与详情 |
| POST | `/draft`、`/submit`、`/submit/{ids}` | 草稿与提交 |
| PUT | `/update`、`/audit/{ids}`、`/unaudit/{ids}` | 改与审核 |
| PUT | `/sender/{ids}`、`/unsender/{ids}`、`/manage-audit/{ids}` | 过账与主管 |
| PUT | `/cancel/{ids}` | 撤回审核申请（回 draft，非 cancelled 态） |
| GET/PUT | `/successive` | 断号检测 / 整理（sequential / date） |
| GET | `/able-word-num`、`/export` | 可用字号、导出 |
| DELETE | `/delete/{ids}` | 删除 |

模板：`/api/vouchertemplate`。

## 8. 业务规则与约束

1. **开放期间**：多数变更依赖 `isVoucherInOpenPeriod()`（凭证期间 ≥ 当前账期）；详见 [06-settlement.md](06-settlement.md)。
2. **过账**：仅已审核且未过账；过账更新 `statement_subject_balance` 等。
3. **结转凭证**：`carry_forward` 标记；期末模块生成，见结算分册。
4. **打印**：当前 `onPrint()` → `printContentInIframe()`；`voucherPrintHtml.ts`、`public/voucher-print-classic.html` **未被生产 import**。

## 9. 已知缺口

- 凭证-附件文件绑定、PDF 导出。
- 制单人与审核人分离校验。
- `rejected` / `cancelled` 产品化。
- 经典国标打印模板正式接入（设计见 `docs/superpowers/specs/2026-09-02-voucher-print-classic-design.md`）。
- 明细账侧凭证号超链回跳：见账簿分册。

## 10. 证据索引

- `VoucherController`、`VoucherService`、`VoucherStatusEnum`
- `views/voucher/voucher-edit.vue`、`voucher-index.vue`
- OpenSpec：`openspec/changes/voucher-entry-workspace/`
- E2E：`e2e/voucher-*.spec.ts`、`voucher-entry-workspace.spec.ts`
