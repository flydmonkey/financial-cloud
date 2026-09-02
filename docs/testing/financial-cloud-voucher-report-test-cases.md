# 财务云凭证与三大报表测试用例

> **适用范围**：凭证（录入 / 编辑 / 审核 / 过账）、资产负债表、利润表、现金流量表、期末结账、结转损益  
> **版本**：v1.0（贴合当前代码实现）  
> **核心原则**：数据不能错、余额不能乱、报表勾稽关系必须成立、反操作闭环完整

---

## 0. 系统模型速查（测试前必读）

### 0.1 凭证状态（双维度）

financial-cloud **不是**单一的 4 态模型，而是审核状态 + 过账状态两个维度：

| 维度 | 字段 | 取值 | UI 显示 |
|------|------|------|---------|
| 审核状态 | `status` | `draft` / `reviewing` / `completed` / `rejected`* / `cancelled`* | 暂存 / 审核中 / 已审核 |
| 过账状态 | `senderId` | 空 = 未过账；非空 = 已过账 | 待过账 / 已过账 |

\* `rejected`、`cancelled` 枚举存在，业务流程未完整实现，**不纳入主流程测试**。

**完整生命周期**：

```
暂存(draft)
  → 提交(submit) → [审核中(reviewing)  当 voucher_reviewed=1]
                 → [已审核(completed)  当 voucher_reviewed=0，跳过审核]
  → 审核(audit)  → 已审核(completed)
  → 过账(sender) → senderId 有值（已过账）
```

**余额更新时机**：仅在**过账**时写入科目余额表；审核不更新余额。

**结转凭证**：`carryForward=y` 的凭证过账时**不更新**科目余额（避免重复计入）。

### 0.2 账套级配置

| 配置项 | 字段 | 值 | 影响 |
|--------|------|-----|------|
| 凭证审核开关 | `book.voucherReviewed` | `0` 关闭 / `1` 开启 | 提交后目标状态不同 |

### 0.3 三大报表取数规则（关键差异）

| 报表 | API | 数据源 | 过滤条件 |
|------|-----|--------|----------|
| 资产负债表 | `GET /api/statement/balance-sheet` | `statement_subject_balance` | 仅**已过账**凭证影响余额 |
| 利润表 | `GET /api/statement/income` | 凭证分录 SQL | `status='completed'` 且 **已过账**（`sender_id` 非空） |
| 现金流量表 | `GET /api/statement/cash-flow` | `voucher_item_cash_flow` | 仅**已过账**（`sender_id` 非空）且指定流量项的凭证 |

> **三报表口径已统一**：仅已过账凭证计入报表。已审核但未过账的凭证不影响任何一张报表（见 TC-RPT-003）。

**strict 校验（CI 默认开启）**：

| 报表 | 配置项 | 错误码 | 行为 |
|------|--------|--------|------|
| 资产负债表 | `financial-cloud.statement.balance-sheet.strict-trial-balance` | 513013 | 试算不平则 API 抛错 |
| 利润表 | `financial-cloud.statement.income.strict-formula-validation` | 513014 | 公式链 1→2→3→4 不平则 API 抛错 |

本地默认关闭；`.github/workflows/ci.yml` 启动后端时两项均为 `true`。结账写入利润表快照前同样走 `generateIncomeStatement`，strict 开启时不平会阻断结账。

### 0.4 已知系统限制（不测为 BUG）

| 限制 | 说明 |
|------|------|
| 操作权限未分离 | 任何有菜单权限的用户均可审核/过账，无制单/审核角色隔离 |
| 驳回流程未实现 | `rejected` 状态无完整业务 |
| 无「报表含未过账凭证」开关 | 三张报表口径固定为仅已过账，不可配置 |

### 0.5 主要 API 索引

| 操作 | 方法 | 路径 |
|------|------|------|
| 暂存 | POST | `/api/voucher/draft` |
| 提交 | POST | `/api/voucher/submit` |
| 修改 | PUT | `/api/voucher/update` |
| 删除 | DELETE | `/api/voucher/delete/{ids}` |
| 撤回审核 | PUT | `/api/voucher/cancel/{ids}` |
| 审核 | PUT | `/api/voucher/audit/{ids}` |
| 反审核 | PUT | `/api/voucher/unaudit/{ids}` |
| 过账 | PUT | `/api/voucher/sender/{ids}` |
| 反过账 | PUT | `/api/voucher/unsender/{ids}` |
| 主管复核 | PUT | `/api/voucher/manage-audit/{ids}` |
| 连号检查 | GET | `/api/voucher/successive` |
| 连号整理 | PUT | `/api/voucher/successive` |
| 结账试算 | GET | `/api/settlement/verify` |
| 期末结账 | GET | `/api/settlement/checkout` |
| 生成结转凭证 | POST | `/api/settlementcarry/generate-voucher` |

---

## 1. 测试环境与账套

### 1.1 账套规划

| 账套 | 名称建议 | 用途 | 关键配置 |
|------|----------|------|----------|
| A | TEST-空白账套 | 从零建账，无期初 | `voucherReviewed=1` |
| B | TEST-期初账套 | 贴近真实客户，手工验算主账套 | `voucherReviewed=1`，录入平衡期初 |
| C | TEST-多期账套 | 跨年/跨月、历史数据 | 已结账 ≥2 期 |
| D | TEST-免审核账套 | 审核开关关闭路径 | `voucherReviewed=0` |

### 1.2 账套 B 标准验算数据（手工勾稽基准）

**期初余额（必须借贷平衡）**：

| 科目 | 借方 | 贷方 |
|------|------|------|
| 1002 银行存款 | 100,000 | |
| 3001 实收资本 | | 100,000 |

**标准业务序列**（每步过账后记录科目余额 + 三报表；科目编码为小企业会计准则）：

| 步骤 | 摘要 | 借方 | 贷方 | 金额 |
|------|------|------|------|------|
| 1 | 收到货款 | 1002 银行存款 | 1122 应收账款 | 50,000 |
| 2 | 支付管理费 | 5602 管理费用 | 1002 银行存款 | 10,000 |
| 3 | 确认销售收入 | 1122 应收账款 | 5001 主营业务收入 | 80,000 |

---

## 2. 凭证模块测试用例

### 2.1 录入与校验

| 用例ID | 优先级 | 用例名称 | 前置条件 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|----------|--------|
| TC-VCH-001 | P0 | 借贷平衡凭证暂存成功 | 账套 A，当前账期 | 录入一借一贷平衡凭证 → 暂存 | 返回成功；`status=draft`；借贷合计相等 | E2E ✅ `voucher.spec.ts` |
| TC-VCH-002 | P0 | 借贷不平衡禁止保存 | 账套 A | 借 100 / 贷 90 → 暂存或提交 | 返回「借贷不平衡」，凭证未保存 | Unit ✅ `VoucherServiceTest` |
| TC-VCH-003 | P1 | 凭证明细为空 | 账套 A | 不录入分录 → 暂存 | 返回「凭证明细不能为空」 | Unit ✅ |
| TC-VCH-004 | P1 | 仅一条分录 | 账套 A | 只录入 1 条分录 | 返回「至少需要两条分录」 | Unit ✅ |
| TC-VCH-005 | P1 | 摘要为空 | 账套 A | 有科目和金额，摘要全空 | 返回「请至少输入一项摘要」 | Unit ✅ |
| TC-VCH-006 | P1 | 科目未选择 | 账套 A | 有摘要和金额，科目为空 | 返回「存在未选择科目的分录」 | E2E ✅ |
| TC-VCH-007 | P1 | 金额为 0 | 账套 A | 借贷金额均为 0 | 返回「存在未填写金额的分录」或「借贷不平衡」 | E2E ✅ |
| TC-VCH-008 | P2 | 一借多贷 | 账套 B | 借 1002/50,000；贷 2202/30,000 + 5001/20,000 | 暂存成功，借贷平衡 | E2E ✅ |
| TC-VCH-009 | P2 | 多借一贷 | 账套 B | 借 5602/5,000 + 5601/5,000；贷 1002/10,000 | 暂存成功 | E2E ✅ |
| TC-VCH-010 | P2 | 红字冲销（负数金额） | 账套 B，已有正向凭证 | 录入负数分录冲销 | 暂存成功；过账后余额正确扣减 | E2E ✅ `voucher-edge-cases` |

### 2.2 凭证字号

| 用例ID | 优先级 | 用例名称 | 前置条件 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|----------|--------|
| TC-VCH-011 | P1 | 凭证号自动递增 | 账套 A，当月无凭证 | 连续暂存 2 张凭证 | 字号连续递增（记-1、记-2） | E2E ✅ |
| TC-VCH-012 | P1 | 重复字号自动重编 | 账套 A | 手工指定已存在的 wordNum | 保存成功并提示「凭证字号重复，已为您重新编号」 | E2E ✅ |
| TC-VCH-013 | P1 | 删除凭证后断号 | 账套 A，已有记-1/2/3 | 删除记-2 | 记-3 仍存在；连号检查报不连续 | E2E ✅ |
| TC-VCH-014 | P1 | 凭证连号整理 | TC-VCH-013 之后 | GET successive → PUT successive 整理 | 凭证号重新连续；verify 连号检查通过 | E2E ✅ `zz-accounting-flow.spec.ts` |

### 2.3 提交与审核开关

| 用例ID | 优先级 | 用例名称 | 前置条件 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|----------|--------|
| TC-VCH-020 | P0 | 开启审核：提交进入审核中 | 账套 B，`voucherReviewed=1` | 暂存 → 提交 | `status=reviewing` | E2E ✅ |
| TC-VCH-021 | P0 | 关闭审核：提交直接已审核 | 账套 D，`voucherReviewed=0` | 暂存 → 提交 | `status=completed`，无 reviewing 阶段 | E2E ✅ `no-review-flow` |
| TC-VCH-022 | P0 | 非当前账期禁止提交 | 账套 B，当前期 2026-03 | 凭证日期填 2026-02 → 提交 | 返回「非当前期不允许提交凭证」 | Unit ✅ |
| TC-VCH-023 | P1 | 已提交凭证不可再次提交 | 账套 B | 对已 reviewing/completed 凭证再次 submit | 返回「凭证已提交，不允许修改」 | E2E ✅ |
| TC-VCH-024 | P1 | 批量提交 | 账套 B，3 张 draft | POST `/api/voucher/submit/{ids}` | 全部提交成功；非 draft 的跳过 | E2E ✅ |

### 2.4 审核 / 反审核

| 用例ID | 优先级 | 用例名称 | 前置条件 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|----------|--------|
| TC-VCH-030 | P0 | 正常审核 | reviewing 凭证 | PUT audit | `status=completed`；写入 auditMemberName/auditDate | E2E ✅ |
| TC-VCH-031 | P0 | 重复审核被忽略 | completed 凭证 | 再次 audit | 操作成功但「失败：1」；状态不变 | E2E ✅ |
| TC-VCH-032 | P0 | draft 凭证不能审核 | draft 凭证 | audit | 被过滤，审核失败 | E2E ✅ |
| TC-VCH-033 | P0 | 反审核（未过账） | completed 且 senderId 空 | unaudit | 回到 reviewing（审核开）或 draft（审核关） | E2E ✅ |
| TC-VCH-034 | P0 | 已过账凭证不能反审核 | senderId 有值 | unaudit | 返回「没有可以反审核的凭证」 | E2E ✅ |
| TC-VCH-035 | P1 | 撤回审核（cancel） | reviewing 凭证 | PUT cancel | 回到 draft | E2E ✅ |
| TC-VCH-036 | P1 | 批量审核 | 多张 reviewing | audit 多 ID | 全部变 completed | E2E ✅ |
| TC-VCH-037 | P2 | 主管复核 | completed 凭证 | manage-audit | 写入 managerId | E2E ✅ |

### 2.5 编辑 / 删除（按状态矩阵）

| 凭证状态 | senderId | 允许编辑 | 允许删除 | 对应用例 |
|----------|----------|----------|----------|----------|
| draft | 空 | ✅ | ✅ | TC-VCH-040, TC-VCH-042 |
| reviewing | 空 | ✅* | ❌ | TC-VCH-041 |
| completed | 空 | ❌ | ❌ | TC-VCH-043 |
| completed | 有值 | ❌ | ❌ | TC-VCH-044 |
| 已结账期间 | 任意 | ❌ | ❌ | TC-VCH-045 |

\* reviewing 状态可通过 update 修改（canModifyUnpostedVoucher 允许未过账且未作废）

| 用例ID | 优先级 | 用例名称 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|--------|
| TC-VCH-040 | P0 | draft 凭证编辑 | 修改金额后 update | 修改成功；借贷仍须平衡 | E2E ✅ |
| TC-VCH-041 | P1 | reviewing 凭证编辑 | 修改摘要后 update | 修改成功（未过账可改） | E2E ✅ |
| TC-VCH-042 | P0 | draft 凭证删除 | DELETE | 删除成功；分录一并清除 | E2E ✅ |
| TC-VCH-043 | P0 | completed 凭证禁止删除 | DELETE | 返回「仅暂存状态的凭证可以删除」 | E2E ✅ |
| TC-VCH-044 | P0 | 已过账凭证禁止直接修改 | 尝试 update | 返回「当前不允许修改」 | E2E ✅ |
| TC-VCH-045 | P0 | 已结账期间禁止修改 | 结账后对该期凭证 update/unsender | 操作被拦截 | E2E ✅ |

### 2.6 过账 / 反过账

| 用例ID | 优先级 | 用例名称 | 前置条件 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|----------|--------|
| TC-VCH-050 | P0 | 正常过账 | completed，senderId 空 | PUT sender | senderId 有值；科目余额更新；现金流量项写入 | E2E ✅ |
| TC-VCH-051 | P0 | 未审核凭证不能过账 | reviewing 凭证 | sender | 被过滤，返回失败提示 | E2E ✅ |
| TC-VCH-052 | P0 | 重复过账余额不翻倍 | 已过账凭证 | 再次 sender | 被过滤；科目余额不变 | E2E ✅ `report-reconciliation` |
| TC-VCH-053 | P0 | 反过账回滚余额 | 已过账，期间未结账 | unsender | senderId 清空；科目余额回滚；现金流量项删除 | E2E ✅ |
| TC-VCH-054 | P0 | 已结账期间禁止反过账 | 已结账期间的凭证 | unsender | 返回「没有可以反过账的凭证」 | E2E ✅ |
| TC-VCH-055 | P1 | 批量过账 | 多张 completed 未过账 | sender 多 ID | 全部过账；余额累计正确 | E2E ✅ |
| TC-VCH-056 | P1 | 结转凭证过账更新损益余额 | carryForward=y 凭证 | sender | 过账成功；5001/5602 等损益科目余额归零 | E2E ✅ |
| TC-VCH-057 | P2 | 过账后科目余额核对 | TC-VCH-050 之后 | 导出科目余额表 | 借方累计 = 贷方累计 | E2E ✅ |

---

## 3. 报表模块测试用例

### 3.1 资产负债表

| 用例ID | 优先级 | 用例名称 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|--------|
| TC-RPT-001 | P0 | 资产合计 = 负债及权益合计 | 打开资产负债表 | 总计行差额 ≤ 0.01 | E2E ✅ `report.spec.ts` |
| TC-RPT-002 | P0 | 期初无业务时报表 = 期初余额 | 账套 B，未录凭证 | 查看报表 | 货币资金 = 100,000；实收资本 = 100,000 | E2E ✅ |
| TC-RPT-003 | P0 | **已审核未过账不影响报表** | 审核但不过账 → 分别查三报表 | 三张报表均无变化 | E2E ✅ |
| TC-RPT-004 | P0 | 过账后报表刷新 | 过账 TC-VCH-050 → 查报表 | 对应科目余额变化；恒等式仍成立 | E2E ✅ |
| TC-RPT-005 | P1 | 多月份期末余额滚动 | 账套 C，查 1 月/2 月报表 | 2 月期初 = 1 月期末 | E2E ✅ |
| TC-RPT-006 | P1 | 红字/负数余额科目 | 红字凭证过账后 | 报表正确显示负数，不丢符号 | E2E ✅ |
| TC-RPT-007 | P2 | 报表导出 | 点击导出 | 文件可下载，数据与页面一致 | E2E ✅ |
| BS-M01 | P0 | 货币资金 = 1001+1002 科目余额 | 00-opening 期初后 | 货币资金行 = 绑定科目余额之和 | E2E ✅ `02-balance-sheet-line-reconciliation` |
| BS-D02 | P0 | 实收资本 = 配置 rules 科目余额 | 同上 | 实收资本行 = rules 绑定科目之和 | E2E ✅ |
| BS-CFG | P1 | 报表行 = 配置页 closingBalance 累加 | 同上 | itemCode 1101 与 config API 一致 | E2E ✅ |
| BS-B01 | P0 | 逐行勾稽后试算平衡仍成立 | 同上 | 资产总计 = 负债及权益总计 | E2E ✅ |
| BS-B02 | P1 | 不平衡差额 > 0.01 可被检测 | Unit mock 不平衡 seed | computeGrandTotalDiff 返回差额 | Unit ✅ |
| BS-B03 | P2 | 差额 0.01 内视为平衡 | Unit | withinTolerance=true | Unit ✅ |
| BS-B04 | P1 | strict 模式不平则阻断出表 | Unit strictTrialBalance=true | 抛出 513013 错误 | Unit ✅ |
| BS-M02 | P0 | 账套 B 业务后货币资金/应收账款勾稽 | book-b 三步凭证后 | 关键行 = config rules | E2E ✅ `book-b-verification` |
| BS-R01 | P1 | 1122 贷方余额 → 预收款项 | 03-reclassification E2E | 应收=0，预收=15000 | E2E ✅ |
| BS-R02 | P1 | 2202 借方余额 → 预付款项 | 同上 | 应付=0，预付=6000 | E2E ✅ |
| BS-R03 | P2 | 同科目借贷混存（总账轧差） | 03-reclassification | 1122 借贷同科目时按 net 进应收 | E2E ✅ |
| BS-R04 | P2 | 应收账款扣减坏账准备 1141 | 03-reclassification | 应收净值 = 1122 借方 − 1141 | E2E ✅ |
| BS-R05 | P2 | 辅助核算明细级重分类 | 需启用 assist + 分行余额 | — | 待开发 |
| BS-G01 | P1 | Golden Dataset 复杂期初写入 | 空白账套 | 340,000 平衡（含存货/固定资产） | E2E ✅ `04-balance-sheet-golden` |
| BS-G02 | P1 | Golden 逐行 = 手工验算 + rules 勾稽 | BS-G01 后 | 8 关键行 + 试算平衡 | E2E ✅ |
| BS-G03 | P1 | 存货多科目合并 + 固定资产净值扣备抵 | BS-G01 后 | 存货=1403+1405；固定资产=1601-1602 | E2E ✅（含于 BS-G02） |
| BS-G04 | P1 | 过账采购存货后报表行更新 | BS-G02 后 | 存货+10k、货币资金−10k；指定 `6-jy-zfxj` 过账；试算平衡 | E2E ✅ |
| BS-G05 | P1 | 过账计提折旧后固定资产净值更新 | BS-G04 后 | 固定资产−5k；strict 下 API 阻断 / 非 strict 静默调平 | E2E ✅ |
| BS-B05 | P1 | strict 模式不平则阻断出表 | BS-G05 后（未结转） | 返回 513013；非 strict 跳过 | E2E ✅ |
| BS-G06 | P1 | 结转费用后未分配利润勾稽试算平衡 | BS-G05 后 qm_jz_cbfy | 未分配利润−5k；资产=负债+权益 335k | E2E ✅ |
| BS-T01~03 | P2 | 一年内到期重分类 | 需到期日标记 + 长期负债科目（小企业账套暂无 2501） | — | 待开发 |

### 3.2 利润表

| 用例ID | 优先级 | 用例名称 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|--------|
| TC-RPT-010 | P0 | 收入凭证过账后利润表有数 | 步骤 3 确认收入 80,000 | 营业收入 +80,000 | E2E ✅ |
| TC-RPT-011 | P0 | 费用凭证过账后费用项目有数 | 步骤 2 管理费 10,000 | 管理费用 +10,000 | E2E ✅ |
| TC-RPT-012 | P0 | 净利润逐级公式链 | 账套 B 收入 80k + 费用 10k | 营业利润=70k；公式链 1→2→3→4 成立 | E2E ✅ + Java UT ✅ |
| TC-RPT-013 | P0 | 净利润与资产负债表勾稽 | 结转损益后 | 本年利润增量 ≈ 结转损益净额；损益科目归零 | E2E ✅ |
| TC-RPT-014 | P1 | 本月数 vs 本年累计 | 账套 C，查 2 月报表 | 累计 = 1 月累计 + 2 月本期 | E2E ✅ |
| TC-RPT-015 | P1 | 未结转损益时的利润表 | 有效凭证但未做结转 | 记录报表表现（与结转后对比） | E2E ✅ |
| IS-UT01 | P1 | 利润表规则与公式单测 | `StatementIncomeRulesTest` | DEBIT/CREDIT/P&L 规则；账套 B / 复杂场景公式 | Java ✅ |
| IS-G01 | P1 | Golden 公式链 + config 逐行勾稽 | 空白账套 + 多笔损益凭证 | 1→2→3→4 成立；1/101/104/105/301 rules 对齐 | E2E ✅ `05-income-golden` |
| IS-G02 | P1 | 首月本期 = 累计 | IS-G01 后 | 有数行 cumulative ≈ current | E2E ✅ |
| IS-R01 | P1 | 结转后 3103 ≈ 净利润 | qm_jz_sr + qm_jz_cbfy | 3103 增量 ≈ 净利润；损益科目归零 | E2E ✅ |
| IS-R02 | P1 | 年末未分配利润勾稽 | 12 月 qm_jz_bnlr 过账 | Δ3104.02 ≈ 累计净利润；3103 归零 | E2E ✅ `test:e2e:year-end` |
| IS-R03 | P1 | 月度未分配利润不变 | 非 12 月结转后 | 3104.02 不变；3103 ≈ 净利润；BS 未分配利润行一致 | E2E ✅ `carry-forward-flow` |
| IS-B01 | P1 | strict 公式链校验 | Golden / 正常过账后 | 公式链成立时 API 返回 0；不平返回 513014 | Java ✅ + E2E ✅（CI strict 开启） |

### 3.3 现金流量表

| 用例ID | 优先级 | 用例名称 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|--------|
| TC-RPT-020 | P0 | 现金净增加额三分项之和 | 有现金业务 | 经营 + 投资 + 筹资 = 净增加额 | E2E ✅ |
| TC-RPT-021 | P0 | 期末现金 = 资产负债表货币资金 | 同上 | 两报表货币资金一致 | E2E ✅ |
| TC-RPT-022 | P0 | 收款凭证指定流量项 | 银行存款收款 + 指定经营/销售商品 | `2-jy-sqxj` 增量 = 凭证金额 | E2E ✅ |
| TC-RPT-023 | P1 | 非现金科目转账不产生流量 | 应收 ↔ 应付转账，无现金科目 | 现金流量表无新增 | E2E ✅ |
| TC-RPT-024 | P1 | 未过账凭证不进现金流量表 | 审核但不过账的现金凭证 | 现金流量表无数据 | E2E ✅ |
| TC-RPT-025 | P2 | 现金流量期初配置 | 修改 ConfigCashFlowBalance | 期初项影响报表 | E2E ✅ |
| TC-RPT-026 | P0 | 主表经营净额 = 附表经营净额 | Golden 结转后（CF-G05） | `11-jy-lljh` = `57-xj-jyje` | E2E ✅ `04-balance-sheet-golden` |
| TC-RPT-027 | P1 | 采购付现 + 存货增加 → 53 自动 | Golden 采购 10k（CF-G03） | `6-jy-zfxj`=10k；`53-xj-chjs`=-10k | E2E ✅ |
| TC-RPT-028 | P1 | 折旧 → 43 自动、主表不变 | Golden 折旧 5k（CF-G04） | `43-xj-zczk`=5k；`11-jy-lljh` 仍 -10k | E2E ✅ |
| TC-RPT-029 | P0 | 38 = 资产负债表货币资金 | Golden 每步 | 期末现金 = 货币资金 | E2E ✅ CF-G01/G03/G05 |
| TC-RPT-030 | P1 | 41 = 利润表净利润 | Golden 结转后（CF-G05） | 附表净利润 = IS 行 4 | E2E ✅ |
| CF-UT01 | P1 | 主表公式 + 附表倒挤单测 | `StatementCashFlowRulesTest` | 三分项=净增；56 倒挤后 11=57 | Java ✅ |
| CF-UT02 | P1 | 间接法 53/54/55/43 单测 | `StatementCashFlowIndirectRulesTest` | 符号、BS 同源合并、折旧贷方 | Java ✅ |
| CF-UT03 | P1 | 间接法 42/44/45/49/50/51/52/35 单测 | `StatementCashFlowIndirectRulesTest` | 减值/摊销/财务费用/投资损失/递延税/汇率 | Java ✅ |
| CF-G01 | P1 | Golden 期初现金 = 货币资金 | BS-G01 后 | 37/38=100k；53/54/55=0 | E2E ✅ `04` |
| CF-G03 | P1 | Golden 采购指定流量项 + 间接法存货 | BS-G04 后 | 见 TC-RPT-027/029 | E2E ✅ |
| CF-G04 | P1 | Golden 折旧附表自动 | BS-G05 后 | 见 TC-RPT-028 | E2E ✅ |
| CF-G05 | P0 | Golden 结转后主附表经营勾稽 | BS-G06 后 | 41=-5k；11=57=-10k；56=0 | E2E ✅ |
| CF-B01 | P1 | strict 主附经营净额不平则阻断 | strict-reconciliation=true | 返回 513015 | Java ✅（公式层）+ 配置项 ✅ |
| CF-M01 | P1 | 第 2 期 CF 期初 = 第 1 期 CF 期末 | 多期结账后、P2 录凭证前 | `37` 衔接 | E2E ✅ `multi-period-flow` |
| CF-M02 | P1 | 第 2 期 53 取上月存货期末为期初 | P1/P2 各一笔采购 | `53` = −P2 采购额 | E2E ✅ `multi-period-flow` |

---

## 4. 期末结账与结转损益

### 4.1 结账

| 用例ID | 优先级 | 用例名称 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|--------|
| TC-SET-001 | P0 | 结账试算-连号检查 | GET `/api/settlement/verify` | 凭证断号时检查失败 | E2E ✅ |
| TC-SET-002 | P0 | 结账试算-借贷平衡 | verify | 借贷不平衡时检查失败 | E2E ✅ |
| TC-SET-003 | P0 | 正常期末结账 | verify 通过 → checkout | 当前账期推进；快照写入 | E2E ✅ |
| TC-SET-004 | P0 | 结账后查已结期间报表 | 查 closedTerm 资产负债表 | 恒等式成立；数据与结账前一致 | E2E ✅ |
| TC-SET-005 | P1 | 重复结账同一期间 | 对已结账月再次 checkout | 已结账期间拒绝重复；不产生重复记录 | E2E ✅ + Unit ✅ |
| TC-SET-006 | P1 | 结账列表 12 个月 | GET settlement/fetch | 返回 12 条记录 | E2E ✅ `settlement.spec.ts` |
| TC-SET-007 | P1 | 结账后利润表公式链 | checkout 成功 → 查已结期间利润表 | 1→2→3→4 成立；strict 模式下不平则 checkout 失败 | E2E ✅ `settlement-checkout.spec.ts` |

### 4.2 结转损益

| 用例ID | 优先级 | 用例名称 | 操作步骤 | 预期结果 | 自动化 |
|--------|--------|----------|----------|----------|--------|
| TC-SET-010 | P0 | 结转收入 qm_jz_sr | 生成 → 提交 → 审核 → 过账 | 收入类科目余额归零 | E2E ✅ |
| TC-SET-011 | P0 | 结转成本费用 qm_jz_cbfy | 同上 | 费用类科目余额归零 | E2E ✅ |
| TC-SET-012 | P1 | 结转所得税 qm_jz_sds | 有利润时生成 | 所得税科目正确 | E2E ✅ |
| TC-SET-013 | P1 | 年末结转本年利润 qm_jz_bnlr | 12 月账期 | 本年利润转入未分配利润 | E2E ✅（`test:e2e:year-end` 正例 + 非12月拦截） |
| TC-SET-014 | P0 | 结转凭证 carryForward 标记 | 查看生成的凭证 | carryForward=y；过账后更新科目余额 | E2E ✅ |
| TC-SET-015 | P1 | 删除结转凭证 | DELETE settlementcarry | 关联 draft 凭证删除 | E2E ✅ |

---

## 5. 端到端流程测试用例

| 用例ID | 优先级 | 流程名称 | 步骤概要 | 核心验证点 | 自动化 |
|--------|--------|----------|----------|------------|--------|
| TC-E2E-001 | P0 | 单月完整做账闭环 | 期初 → 录凭证 → 提交 → 审核 → 过账 → 结转 → 结账 → 三报表 | 全部勾稽成立 | E2E ✅ `accounting-flow` + `carry-forward-flow` |
| TC-E2E-002 | P0 | 反向修改闭环 | 反过账 → 反审核 → 改金额 → 再审 → 再过账 → 查报表 | 报表同步更新；恒等式仍成立 | ✅ `voucher-reverse-flow.spec.ts` |
| TC-E2E-003 | P0 | 多期连续做账（2 个月） | 1 月全流程 + 结账 → 2 月全流程 | 2 月累计 = 1 月累计 + 2 月本期 | ✅ `multi-period-flow.spec.ts` |
| TC-E2E-004 | P1 | 免审核账套全流程 | 账套 D：暂存 → 提交(直接completed) → 过账 | 跳过 reviewing 仍正常结账 | ✅ `no-review-flow.spec.ts` |
| TC-E2E-005 | P1 | 三报表口径统一专项 | 审核不过账 → 无变化 → 过账 → 公式链 + 试算平衡 | 仅已过账计入报表 | ✅ `report-reconciliation.spec.ts` |

---

## 6. 异常与边界测试用例

| 用例ID | 优先级 | 用例名称 | 操作步骤 | 预期结果 | 优先级定义 |
|--------|--------|----------|----------|----------|------------|
| TC-EXC-001 | P0 | 重复过账不翻倍 | 同一凭证 sender 两次 | 余额不变 | E2E ✅ |
| TC-EXC-002 | P0 | 绕过 UI 直接 API 修改已过账凭证 | PUT update 已过账凭证 | 返回失败 | E2E ✅ `voucher-state-guards` |
| TC-EXC-003 | P1 | 删除结转损益凭证 | 删除已过账结转凭证 | 系统拦截删除，净利润不变 | E2E ✅（拦截路径） |
| TC-EXC-004 | P1 | 超大金额精度 | 暂存 99,999,999.99 无精度丢失；999,999,999.99 被拒（不过账，避免污染账套） | 无精度丢失 | E2E ✅ |
| TC-EXC-005 | P1 | 借贷双方负数（红字） | 红字冲销全流程 | 余额、报表正确 | E2E ✅ |
| TC-EXC-006 | P2 | 批量过账部分失败 | 混合合法/非法 ID | 合法的成功，非法的跳过，无半更新 | E2E ✅ |
| TC-EXC-007 | P2 | 页面中断后数据一致性 | 过账中途断网/关页 | 无半过账脏数据 | E2E ✅（批量幂等近似；真断网中断需手工） |

---

## 7. 报表勾稽验算工作表（账套 B 执行）

每完成一批过账后填写：

### 7.1 逐步验算表

| 步骤 | 1002 银行存款 | 1122 应收账款 | 5001 收入 | 5602 费用 | 3001 实收资本 | 资产合计 | 负债+权益 | 差额 |
|------|--------------|--------------|----------|----------|--------------|----------|-----------|------|
| 期初 | 100,000 | 0 | 0 | 0 | 100,000 | 100,000 | 100,000 | 0 |
| +步骤1 | | | | | | | | |
| +步骤2 | | | | | | | | |
| +步骤3 | | | | | | | | |
| 结转后 | | | | | | | | |
| 结账后 | | | | | | | | |

### 7.2 勾稽快速自查清单

- [ ] 资产负债表：资产总计 = 负债 + 所有者权益（差额 ≤ 0.01）
- [ ] 利润表：净利润 = 利润总额 - 所得税费用
- [ ] 利润表净利润 ≈ 资产负债表未分配利润本期增加额（IS-R02 年末结转 ✅；月度 IS-R03 未分配利润不变 + IS-R01/3103 ✅）
- [ ] 现金流量表：经营 + 投资 + 筹资 = 现金净增加额
- [ ] 现金流量表期末现金 = 资产负债表货币资金
- [ ] 现金流量表：主表经营净额 = 附表经营净额（结转后，CF-G05 ✅）
- [ ] 现金流量表附表：53/54/55 与 BS 存货/往来行同源（Golden ✅）
- [ ] 利润表净利润 = 附表 41-xj-jlr（Golden CF-G05 ✅）
- [ ] 损益类科目结转后余额 ≈ 0
- [ ] 科目余额表：借方累计 = 贷方累计

---

## 8. 测试执行计划

### 第一轮：单模块功能（Week 1）

| 顺序 | 范围 | 用例 |
|------|------|------|
| 1 | 过账/反过账 + 余额 | TC-VCH-050 ~ 057 |
| 2 | 录入校验 | TC-VCH-001 ~ 007 |
| 3 | 状态拦截 | TC-VCH-040 ~ 045, TC-VCH-030 ~ 034 |
| 4 | 审核开关两条路径 | TC-VCH-020 ~ 021 |
| 5 | 凭证连号 | TC-VCH-011 ~ 014 |

### 第二轮：全流程 + 报表勾稽（Week 2）

| 顺序 | 范围 | 用例 |
|------|------|------|
| 1 | 账套 B 标准验算 | TC-E2E-001 + 第 7 节工作表 |
| 2 | 反向闭环 | TC-E2E-002 |
| 3 | 三报表口径统一 | TC-RPT-003, TC-E2E-005 |
| 4 | 多期 | TC-E2E-003 |
| 5 | 结账 + 结转 | TC-SET-001 ~ 014 |

> **阻断规则**：勾稽关系不平 = 最高优先级 BUG，暂停后续测试。

### 第三轮：异常 + 回归（Week 3）

| 顺序 | 范围 | 用例 |
|------|------|------|
| 1 | 致命异常 | TC-EXC-001 ~ 002 |
| 2 | 边界场景 | TC-EXC-003 ~ 007 |
| 3 | 全量回归 | 复跑 TC-E2E-001 ~ 003 |

---

## 9. 自动化覆盖映射

| 已有自动化 | 文件 | 覆盖用例 |
|------------|------|----------|
| E2E 凭证边界 | `e2e/voucher-edge-cases.spec.ts` | TC-EXC-004, TC-EXC-005, TC-RPT-006 ✅ |
| E2E 批量过账 | `e2e/voucher-batch-ops.spec.ts` | TC-EXC-001, TC-EXC-006, TC-EXC-007（幂等）, TC-VCH-024, TC-VCH-036, TC-VCH-055 |
| E2E 结账守卫 | `e2e/settlement-checkout.spec.ts` | TC-SET-005/007 ✅（重复结账拒绝 + 结账后公式链） |
| E2E 账套初始化 | `e2e/global-setup.ts` / `e2e/setup-book.spec.ts` | 清库 + API 建账（`E2E_RESET_BOOK=1` 时自动） |
| E2E 凭证草稿/提交 | `e2e/voucher.spec.ts` | TC-VCH-001, 020, TC-VCH-002（纳入 `test:e2e:accounting`） |
| E2E 凭证录入校验 | `e2e/voucher-input-validation.spec.ts` | TC-VCH-006, TC-VCH-007 |
| E2E 凭证连号 | `e2e/voucher-numbering.spec.ts` | TC-VCH-011, TC-VCH-012, TC-VCH-013 |
| E2E 多行分录 | `e2e/voucher-multi-line.spec.ts` | TC-VCH-008, TC-VCH-009 |
| E2E 全流程 | `e2e/zz-accounting-flow.spec.ts` | TC-VCH-020,030,050, TC-SET-001~004, TC-E2E-001 ✅ |
| E2E 反向闭环 | `e2e/voucher-reverse-flow.spec.ts` | TC-E2E-002, TC-VCH-053 |
| E2E 三报表勾稽 | `e2e/report-reconciliation.spec.ts` | TC-RPT-003, TC-E2E-005, TC-VCH-052 |
| E2E 多期做账 | `e2e/multi-period-flow.spec.ts` | TC-E2E-003, TC-RPT-005/014, **CF-M01/M02** ✅ |
| E2E 结转损益 | `e2e/carry-forward-flow.spec.ts` | TC-SET-010~015, TC-RPT-013/015, IS-R01/R03, TC-VCH-056, TC-EXC-003, TC-E2E-001 ✅ |
| E2E 主管复核 | `e2e/voucher-manage-audit.spec.ts` | TC-VCH-037 ✅ |
| E2E 现金流量表 | `e2e/01-cash-flow-reconciliation.spec.ts` | TC-RPT-020~025 ✅（opening 后优先执行，保证跨表勾稽） |
| E2E 凭证状态拦截 | `e2e/voucher-state-guards.spec.ts` | TC-VCH-023, 031~035, 033, 040~041, 042~045, 051, 054, TC-EXC-002 |
| E2E 期初余额报表 | `e2e/00-opening-balance-report.spec.ts` | TC-RPT-002 ✅ |
| E2E 资产负债表逐行勾稽 | `e2e/02-balance-sheet-line-reconciliation.spec.ts` | BS-M01/D02/CFG/B01 ✅ |
| E2E 往来重分类 | `e2e/03-balance-sheet-reclassification.spec.ts` | BS-R01~R04 ✅ |
| E2E Golden Dataset | `e2e/04-balance-sheet-golden-dataset.spec.ts` | BS-G01~G06 + BS-B05 + **CF-G01/G03/G04/G05** ✅（`npm run test:e2e:balance-sheet-golden`） |
| E2E 利润表 Golden | `e2e/05-income-statement-golden-dataset.spec.ts` | IS-G01/G02 + IS-R01 ✅（`npm run test:e2e:income-golden`） |
| E2E 报表导出 | `e2e/report-export.spec.ts` | TC-RPT-007 ✅ |
| E2E 账套B验算 | `e2e/book-b-verification.spec.ts` | TC-RPT-004/010~013、TC-VCH-057 ✅ |
| E2E 年末结转 | `e2e/carry-forward-year-end.spec.ts` | TC-SET-013 + IS-R02 ✅（`npm run test:e2e:year-end`） |
| E2E 免审核账套 | `e2e/no-review-flow.spec.ts` | TC-E2E-004, TC-VCH-021, TC-VCH-033（免审核→draft） |
| E2E 报表 | `e2e/report.spec.ts` | TC-RPT-001 ✅（纳入 `test:e2e:accounting`） |
| E2E 结账页 | `e2e/settlement.spec.ts` | TC-SET-006 ✅；UI 页需 `npm run test:e2e:ui-pages` |
| E2E UI 页面冒烟 | `test:e2e:ui-pages` | 凭证/报表/结账页渲染（`E2E_ENABLE_UI=1`，需先 `test:e2e:install`） |
| Unit 凭证校验 | `VoucherServiceTest.java` | TC-VCH-002~005, 020~022 |
| Unit 结账 | `SettlementServiceTest.java` | TC-SET-005（重复结账拒绝） |
| Unit 资产负债表 | `StatementBalanceSheetServiceTest.java` | normalize/refreshItemsBalance/reconcile/试算差额 |
| Unit 利润表规则 | `StatementIncomeRulesTest.java` | IS-UT01：取数规则 + 逐级公式链 |
| Unit 利润表校验 | `StatementIncomeServiceTest.java` | IS-B01：strict 公式链拦截 |
| Unit 现金流量表规则 | `StatementCashFlowRulesTest.java` | CF-UT01：主表公式 + 附表倒挤 |
| Unit 现金流量表间接法 | `StatementCashFlowIndirectRulesTest.java` | CF-UT02~03：53/54/55/43 + 42/44/45/49/50/51/52/35 |
| Unit 科目兼容 | `SubjectCodeCompatTest.java` | 小企业准则 4001→3001 等别名 |
| Unit 科目祖先 | `SubjectBalanceAncestorsTest.java` | idPath 空段过滤，防 source_id='' 误匹配 |
| Unit 配置更新 | `ConfigSysServiceTest.java` | update-by-key 按 bookId 过滤 |

### 建议优先补充自动化

1. ~~`TC-E2E-002` 反向闭环~~ → ✅ `voucher-reverse-flow.spec.ts`
2. ~~`TC-RPT-003` 三报表口径差异~~ → ✅ `report-reconciliation.spec.ts`
3. ~~`TC-VCH-052` 重复过账~~ → ✅ `report-reconciliation.spec.ts`
4. ~~`TC-E2E-003` 多期连续做账~~ → ✅ `multi-period-flow.spec.ts`
5. ~~`TC-RPT-002/007/025` 期初/导出/现金流配置~~ → ✅ 已覆盖
6. ~~小企业准则资产负债表 3001→实收资本~~ → ✅ `SubjectCodeCompat` + `StatementBalanceSheetService` 别名解析
7. ~~`TC-RPT-021` 现金流期末 vs 资产负债表货币资金~~ → ✅ `01-cash-flow-reconciliation`（置于期初之后，避免其它业务污染）
8. ~~`ConfigSysService.update` 必须按 `bookId` 过滤~~ → ✅ 已修（`ConfigSysServiceTest` + `clear_books` 同步清理账套 config）
9. ~~科目余额 `source_id` 空 + `idPath` 空段误匹配~~ → ✅ `BookInitBalanceService` 写入科目 id；`SubjectBalanceAncestors` 忽略 `/` 空段；现金流仅统计已过账凭证
10. ~~现金流量表附表 53/54/55 自动计算 + Golden 三表勾稽~~ → ✅ `StatementCashFlowIndirectRules` + `04-balance-sheet-golden` CF-G01~G05
11. ~~多期 CF 期初衔接 + 53 跨月期初~~ → ✅ `multi-period-flow` CF-M01/M02

---

## 10. 缺陷优先级定义

| 级别 | 定义 | 示例 |
|------|------|------|
| **致命** | 数据错误、余额错乱、勾稽不成立 | 重复过账翻倍、借贷不平、报表恒等式破 |
| **严重** | 核心流程阻断、反操作失败 | 无法反过账、已过账可被修改 |
| **一般** | 非核心功能异常 | 批量操作提示不准确 |
| **优化** | 体验问题 | 按钮文案、颜色 |

---

## 附录 A：凭证操作权限矩阵（financial-cloud 实际）

| 操作 | draft | reviewing | completed(未过账) | completed(已过账) |
|------|-------|-----------|-------------------|-------------------|
| 编辑 update | ✅ | ✅ | ❌ | ❌ |
| 删除 delete | ✅ | ❌ | ❌ | ❌ |
| 提交 submit | ✅ | ❌ | ❌ | ❌ |
| 撤回 cancel | ❌ | ✅ | ❌ | ❌ |
| 审核 audit | ❌ | ✅ | ❌ | ❌ |
| 反审核 unaudit | ❌ | ❌ | ✅ | ❌ |
| 过账 sender | ❌ | ❌ | ✅ | ❌ |
| 反过账 unsender | ❌ | ❌ | ❌ | ✅ |

> 已过账凭证修改路径：**反过账 → 反审核 → 编辑 → 提交 → 审核 → 过账**

## 附录 B：测试交付物清单

1. **本文档** — 测试用例主文档
2. **账套 B 验算工作表** — 第 7 节（建议复制为 Excel）
3. **BUG 缺陷清单** — 按第 10 节分级
4. **已知限制清单** — 第 0.4 节
5. **自动化覆盖报告** — 第 9 节映射完成情况
