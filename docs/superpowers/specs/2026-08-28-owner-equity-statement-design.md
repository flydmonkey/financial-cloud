# 所有者权益变动表 — 详细设计

日期：2026-08-28  
状态：待评审  
范围：jinbooks Phase 1 — 简化 5 列矩阵 + 四大变动来源 + 严格勾稽 + 实时计算

## 一、已确认决策

| # | 项 | 选择 |
|---|-----|------|
| 1 | 列结构 | 简化 5 列：实收资本、资本公积、其他综合收益、盈余公积、未分配利润 + 合计 |
| 2 | 变动来源 | 四大类：综合收益 / 投入减少资本 / 利润分配 / 内部结转 |
| 3 | 其他综合收益 | 一期置 0，列保留，勾稽跳过 OCI |
| 4 | 勾稽策略 | 严格模式：不平则报错拦截 |
| 5 | 期间列 | 仅「本年金额」 |
| 6 | 持久化 | 仅实时计算（类似现金流量表），不写入结账快照 |
| 7 | 科目映射 | 走 `SubjectCodeCompat`，按账套科目自动解析 |
| 8 | 内部结转 | 常见场景：盈余公积提取、资本公积/盈余公积转增（凭证科目配对） |
| 9 | 交付物 | 本设计文档 → 评审通过后实现 Phase 1 |

## 二、背景与目标

### 2.1 背景

jinbooks 现有 5 类报表（资产负债、利润、现金流、科目余额、凭证汇总），**缺少所有者权益变动表**。权益数据目前仅作为资产负债表的负债侧子集存在，无法展示「期初 → 变动来源 → 期末」的矩阵结构。

### 2.2 目标

1. 新增第四张主表：**所有者权益变动表**（期间报表）
2. 支持简化 5 列矩阵，展示四大变动来源
3. 与资产负债表权益合计、利润表净利润严格勾稽
4. 复用现有 `StatementParamsDto` 期间参数（月/季/半年/年/区间）
5. 实时计算，不依赖结账持久化

### 2.3 非目标（Phase 1）

- 不上年金额对比列
- 不持久化到 `statement_*` 快照表
- 不做 Excel 导出
- 不扩展利润表「综合收益总额」行
- 不做会计政策变更、前期差错更正行
- 不用 `statement_rules` 多维扩展（采用专用计算引擎）

---

## 三、报表结构

### 3.1 矩阵布局

```
                    │ 实收资本 │ 资本公积 │ 其他综合收益 │ 盈余公积 │ 未分配利润 │  合计  │
────────────────────┼─────────┼─────────┼─────────────┼─────────┼───────────┼────────┤
一、期初余额         │    A1   │   A2    │     A3      │   A4    │    A5     │  ΣA    │
二、本期增减变动     │         │         │             │         │           │        │
  （一）综合收益总额 │    0    │    0    │      0      │    0    │  净利润   │ 净利润 │
  （二）所有者投入   │   B1    │   B2    │      0      │    0    │    B5     │  ΣB    │
      和减少资本     │         │         │             │         │           │        │
  （三）利润分配     │    0    │    0    │      0      │   C4    │   C5      │   0    │
  （四）内部结转     │   D1    │   D2    │      0      │   D4    │   D5      │   0    │
三、期末余额         │    E1   │   E2    │      0      │   E4    │   E5      │  ΣE    │
```

**恒等式（逐行、逐列）：**

```
期末[col] = 期初[col] + 综合收益[col] + 投入减少[col] + 利润分配[col] + 内部结转[col]
合计[row] = Σ 五列[row]
```

**合计列约束：**

- 「（三）利润分配」合计 = 0（权益内部此消彼长）
- 「（四）内部结转」合计 = 0

### 3.2 行项清单（`OwnerEquityItemCodeEnum`）

| itemCode | 行次 | 行名称 | rowType | 是否可配置 |
|----------|------|--------|---------|-----------|
| `opening` | 1 | 一、期初余额 | balance | 否（固定） |
| `section_changes` | 2 | 二、本期增减变动 | header | 否 |
| `comprehensive_income` | 3 | （一）综合收益总额 | change | 否 |
| `owner_contribution` | 4 | （二）所有者投入和减少资本 | change | 否 |
| `profit_distribution` | 5 | （三）利润分配 | change | 否 |
| `internal_transfer` | 6 | （四）所有者权益内部结转 | change | 否 |
| `closing` | 7 | 三、期末余额 | balance | 否 |

> Phase 1 行项固定，不建 `standard_statement_owner_equity` 模板表；行定义在枚举 + 常量中维护。

---

## 四、科目映射

### 4.1 列 → 一级权益科目

通过 `StatementOwnerEquityRules.resolveColumnSubjects(bookId)` 解析账套实际科目编码：

| 列 | 企业会计准则（template 1） | 小企业会计准则（template 2） | 解析策略 |
|----|---------------------------|---------------------------|---------|
| 实收资本 | `3001` | `3101` | `BookSubject` category=4，名称匹配或前缀匹配 |
| 资本公积 | `3002` | `3111` | 同上 |
| 其他综合收益 | — | — | Phase 1 固定 0，预留 `3122`/`3003` 等 |
| 盈余公积 | `3101` | `3121` | 同上 |
| 未分配利润 | `3104.02` | `3141.15` | `SubjectCodeCompat` 候选链 |

**解析顺序：**

1. 读取账套 `BookSubject` 列表，筛选 `category = 4`（所有者权益类）
2. 按 `StatementOwnerEquityRules.COLUMN_SUBJECT_PREFIXES` 匹配一级科目
3. 未匹配时 fallback 到 `SubjectCodeCompat.carryForwardSubjectCodes` 候选
4. 仍无法解析 → 抛 `OWNER_EQUITY_SUBJECT_NOT_FOUND`（列名入参）

### 4.2 变动来源 → 科目/数据源

#### （一）综合收益总额

| 列 | 来源 |
|----|------|
| 未分配利润 | `StatementIncomeService` 净利润，itemCode = `ConstsSysConfig.SYS_DEFAULT_INCOME_NET_PROFIT`（默认 `4`） |
| 其他综合收益 | 固定 `0` |
| 其余列 | `0` |

> 取净利润时使用与报表相同的 `StatementParamsDto`（periodType + reportDate），确保期间口径一致。

#### （二）所有者投入和减少资本

从凭证发生额识别，**排除**已归类为内部结转的凭证：

| 列 | 科目范围 | 计算 |
|----|---------|------|
| 实收资本 | 实收资本科目及其下级 | 本期贷方 − 借方（权益贷增） |
| 资本公积 | 资本公积科目及其下级 | 同上 |
| 未分配利润 | 仅「减资回购等直接冲减未分配利润」场景 | 见 4.3 配对规则 |

> 资本公积转增导致的实收资本增加归（四），不计入（二）。

#### （三）利润分配

| 列 | 识别方式 |
|----|---------|
| 盈余公积 | `3141.02`/`803255703339779985`（提取法定盈余公积）等 **贷方发生额** 汇总 |
| 未分配利润 | 对应 **借方发生额** 汇总（符号取负展示在矩阵中） |

**利润分配相关科目（小企业准则示例）：**

| 用途 | 科目编码 |
|------|---------|
| 提取法定盈余公积 | `3141.02` |
| 提取任意盈余公积 | `3141.09` |
| 应付股利 / 分配股利 | `3141.10`（若账套启用） |
| 未分配利润 | `3141.15` |

> 提取盈余公积凭证：借 未分配利润 / 贷 盈余公积 → 盈余公积 +X，未分配利润 −X，合计 0。

#### （四）内部结转

| 场景 | 识别规则 | 列影响 |
|------|---------|--------|
| 资本公积转增资本 | 借：资本公积 / 贷：实收资本 | 资本公积 −X，实收资本 +X |
| 盈余公积转增资本 | 借：盈余公积 / 贷：实收资本 | 盈余公积 −X，实收资本 +X |
| 盈余公积弥补亏损 | 借：盈余公积 / 贷：未分配利润 | 盈余公积 −X，未分配利润 +X |
| 年末结转本年利润 | 借：本年利润 / 贷：未分配利润 | **不计入**（已在净利润/综合收益反映，避免重复） |

**识别算法（`InternalTransferClassifier`）：**

```
输入：期间内已过账凭证分录 List<VoucherItemVo>
过滤：subjectCode 属于 category=4 权益科目
分组：按 voucherId 聚合
对每个 voucherId：
  if 同时存在 (capitalReserve 借方, paidInCapital 贷方) → 转增资本
  if 同时存在 (surplusReserve 借方, paidInCapital 贷方) → 盈余公积转增
  if 同时存在 (surplusReserve 借方, retainedEarnings 贷方) → 弥补亏损
  if 摘要含 "结转" 且涉及 4103/3103 本年利润 → 跳过
输出：Map<Column, BigDecimal> 变动额
```

---

## 五、计算流程

```
StatementParamsDto.parse()
    │
    ├─► resolveColumnSubjects(bookId)           // 五列科目编码
    │
    ├─► loadOpeningBalances(dto, subjects)      // 期初：subject_balance 年初 或 期初余额
    │       来源：StatementSubjectBalanceService / voucherSubjectBalanceSummary
    │
    ├─► loadClosingBalances(dto, subjects)      // 期末：用于校验，非独立填表
    │
    ├─► fetchNetProfit(dto)                     // StatementIncomeService
    │
    ├─► fetchEquityVoucherAmounts(dto)          // VoucherItemMapper.selectSubjectAmount
    │
    ├─► classifyChanges(vouchers, subjects)     // ②③④ 分类
    │       ├─ ownerContribution
    │       ├─ profitDistribution
    │       └─ internalTransfer
    │
    ├─► buildMatrix()                           // 填充 7 行 × 6 列
    │
    ├─► computeClosingFromFormula()             // 期初 + 变动 = 期末（公式行）
    │
    └─► reconcile()                             // 严格勾稽，失败抛异常
            ├─ 表内：逐行 期初+变动=期末
            ├─ 表内：合计列 = 五列之和
            ├─ 表内：（三）（四）合计 = 0
            ├─ 表外：期末合计 = 资产负债表「所有者权益合计」
            ├─ 表外：净利润 = 利润表净利润
            └─ 表外：未分配利润变动 = 净利润 − 利润分配 ± 内部结转(未分配利润列)
```

### 5.1 期初余额口径

| periodType | 期初定义 |
|------------|---------|
| `month` | 该月月初余额（= 上月月末 = 年初 + 1月至上月发生） |
| `quarter` / `halfYear` / `year` | 该期间起始日余额 |
| `range` | `dateRangeStart` 当日余额 |

实现：复用 `StatementSubjectBalanceService.selectSubjectBalance` 或 `voucherSubjectBalanceSummary`，取 `initialBalance` / 推导期初。

### 5.2 期末余额

**双轨校验：**

1. **公式期末** = 期初 + Σ变动（用于填表）
2. **科目期末** = 科目余额表期末（用于勾稽）

两者差额 > 0.01 → `OWNER_EQUITY_RECONCILIATION_FAILED`。

---

## 六、API 设计

### 6.1 查询接口

```
GET /api/statement/owner-equity
```

**请求参数**（复用 `StatementParamsDto`）：

| 参数 | 必填 | 说明 |
|------|------|------|
| `periodType` | 是 | month / quarter / halfYear / year / range |
| `reportDate` | 是 | 与现有报表一致 |
| `reportQuarter` | 条件 | Q1~Q4 / H1~H2 |
| `dateRange` | 条件 | periodType=range 时 |

**响应** `Message<StatementOwnerEquityVo>`：

```json
{
  "code": 0,
  "data": {
    "bookId": "...",
    "periodType": "month",
    "reportDate": "2026-08",
    "items": [
      {
        "itemCode": "opening",
        "itemName": "一、期初余额",
        "rowType": "balance",
        "paidInCapital": 1000000.00,
        "capitalReserve": 50000.00,
        "otherComprehensiveIncome": 0.00,
        "surplusReserve": 80000.00,
        "retainedEarnings": 200000.00,
        "total": 1330000.00
      }
    ],
    "reconciliation": {
      "passed": true,
      "balanceSheetEquityTotal": 1330000.00,
      "incomeNetProfit": 50000.00,
      "diffs": []
    }
  }
}
```

### 6.2 错误码

| 枚举 | code | 说明 |
|------|------|------|
| `OWNER_EQUITY_RECONCILIATION_FAILED` | 513014 | 勾稽不平（严格模式） |
| `OWNER_EQUITY_SUBJECT_NOT_FOUND` | 513015 | 无法解析某列权益科目 |
| `OWNER_EQUITY_INTERNAL_ERROR` | 513016 | 矩阵自平衡失败 |

消息键写入 `MessageKeys.Statement.*`，三语 `messages*.properties`。

### 6.3 Phase 1 不含

- `GET /owner-equity/export`
- 配置 CRUD 接口

---

## 七、后端组件

### 7.1 新增文件

| 文件 | 职责 |
|------|------|
| `enums/statement/OwnerEquityItemCodeEnum.java` | 行项编码 |
| `enums/statement/OwnerEquityColumnEnum.java` | 列枚举 |
| `domain/statement/StatementOwnerEquityItem.java` | 行数据（宽表 6 个 BigDecimal 列） |
| `dto/statement/StatementOwnerEquityVo.java` | 响应 VO |
| `dto/statement/OwnerEquityReconciliationVo.java` | 勾稽结果 |
| `util/StatementOwnerEquityRules.java` | 科目解析、列映射、常量 |
| `util/InternalTransferClassifier.java` | 内部结转凭证分类 |
| `service/statement/StatementOwnerEquityService.java` | 核心计算 |
| `controller/statement/StatementOwnerEquityController.java` | REST 端点 |
| `enums/statement/StatementTypeEnum.java` | 新增 `owner_equity` |
| `enums/error/StatementErrorCode.java` | 新增 513014~513016 |

### 7.2 依赖关系

```
StatementOwnerEquityController
    └── StatementOwnerEquityService
            ├── StatementSubjectBalanceService   // 期初/期末余额
            ├── StatementIncomeService           // 净利润
            ├── StatementBalanceSheetService     // 权益合计勾稽
            ├── VoucherItemMapper                // 凭证发生额
            ├── BookSubjectMapper                // 科目解析
            ├── StatementOwnerEquityRules
            └── InternalTransferClassifier
```

### 7.3 配置项

```yaml
financial-cloud:
  statement:
    owner-equity:
      strict-reconciliation: true   # 默认 true，与决策一致
      tolerance: 0.01               # 金额容差
```

---

## 八、前端设计

### 8.1 页面

| 文件 | 说明 |
|------|------|
| `views/statement/owner-equity-statement.vue` | 主页面 |
| `api/statement/statement-owner-equity.ts` | API 封装 |

### 8.2 UI 要点

- 期间选择器：复用 `balance-sheet.vue` 的 `periodType` + 日期控件
- 表格：`el-table`，首列为行名称，后 6 列为金额
- 标题行 / 分组行（「二、本期增减变动」）使用 `rowType=header` 加粗
- 勾稽失败：后端抛错 → 前端 `ElMessage.error` 展示；若返回 `reconciliation.diffs` 则展示明细
- 菜单路由：`/statement/owner-equity-statement`，与现有报表并列

### 8.3 金额展示

- 空 / 0 显示 `—` 或 `0.00`（与资产负债表现有风格一致）
- 负数用括号或红色（遵循现有报表样式）

---

## 九、勾稽规则详述

### 9.1 表内校验

| ID | 规则 | 公式 | 失败消息 |
|----|------|------|---------|
| R-01 | 行平衡 | `closing[col] = opening[col] + Σchanges[col]` | 列 {col} 行平衡失败 |
| R-02 | 列合计 | `total[row] = Σ五列[row]` | 行 {row} 列合计失败 |
| R-03 | 利润分配合计 | `total[profit_distribution] = 0` | 利润分配合计应为 0 |
| R-04 | 内部结转合计 | `total[internal_transfer] = 0` | 内部结转合计应为 0 |

### 9.2 表外校验

| ID | 规则 | 公式 | 数据源 |
|----|------|------|--------|
| R-05 | 资产负债勾稽 | `\|closing.total − BS.equityTotal\| ≤ tolerance` | 资产负债表 itemCode `2199` 或「所有者权益合计」行 |
| R-06 | 净利润勾稽 | `\|comprehensive_income.retained − income.netProfit\| ≤ tolerance` | 利润表 itemCode `4` |
| R-07 | 未分配利润勾稽 | `\|Δretained − (netProfit − profitDist.retained − internal.retained)\| ≤ tolerance` | 本表 + 利润表 |

> R-05 需确认资产负债表中「所有者权益合计」行的 `itemCode`（当前负债侧 `2199` / `2299_2199` 组合逻辑需在实现时复用 `StatementBalanceSheetService` 提取方法）。

### 9.3 严格模式行为

```
reconcile():
  diffs = []
  for rule in [R-01..R-07]:
    if abs(diff) > tolerance: diffs.add(rule, diff)
  if diffs not empty and strict:
    throw ServiceException(OWNER_EQUITY_RECONCILIATION_FAILED, diffs)
  return ReconciliationVo(passed: diffs.empty, diffs)
```

---

## 十、测试计划

### 10.1 单元测试

| 文件 | 用例 |
|------|------|
| `StatementOwnerEquityRulesTest` | 科目解析、准则 alias |
| `InternalTransferClassifierTest` | 转增、弥补亏损、本年利润结转排除 |
| `StatementOwnerEquityServiceTest` | 矩阵构建、行平衡、勾稽通过/失败 |

### 10.2 E2E（Phase 1 末或 Phase 2）

| 用例 ID | 场景 |
|---------|------|
| OE-01 | 空白账套：仅期初，无变动，期末=期初 |
| OE-02 | 有收入费用：净利润 → 未分配利润（综合收益行） |
| OE-03 | 提取盈余公积：利润分配行合计为 0 |
| OE-04 | 期末合计 = 资产负债表权益合计 |
| OE-05 | 勾稽故意破坏 → API 返回 513014 |

扩展 `e2e/helpers/reports.ts`：

```typescript
export async function fetchOwnerEquity(page, params) { ... }
export async function assertOwnerEquityReconciliation(data) { ... }
```

---

## 十一、实施顺序

| 步骤 | 内容 | 预估 |
|------|------|------|
| 1 | `StatementOwnerEquityRules` + 科目解析 + 单元测试 | 1d |
| 2 | `InternalTransferClassifier` + 单元测试 | 1d |
| 3 | `StatementOwnerEquityService` 主流程 + 勾稽 | 2d |
| 4 | Controller + ErrorCode + i18n + 配置 | 0.5d |
| 5 | 前端页面 + 路由 + API | 1d |
| 6 | 集成测试 + 文档更新 `jinbooks-voucher-report-test-cases.md` | 1d |

**合计约 6.5 人日**

---

## 十二、风险与后续扩展

| 风险 | 缓解 |
|------|------|
| 两套准则科目不一致 | `SubjectCodeCompat` + 账套实际科目动态解析 |
| 内部结转识别不全 | Phase 1 仅常见场景；未识别部分会体现在 R-01/R-07 勾稽失败中 |
| 年末结转与净利润重复 | 明确排除「本年利润→未分配利润」凭证 |
| 减资/回购业务少见 | （二）中预留未分配利润列；无业务时为 0 |
| 资产负债表权益行 itemCode 不稳定 | 抽取 `getEquityTotal(BalanceSheet)` 共用方法 |

**Phase 2 候选：**

- 上年金额列
- 结账持久化
- Excel 导出
- 其他综合收益与利润表联动
- 可配置行项模板

---

## 十三、评审检查清单

- [ ] 行项 7 行是否满足业务展示需求？
- [ ] 科目映射表是否覆盖在用的企业/小企业账套？
- [ ] 勾稽 R-05 资产负债表权益行 itemCode 是否明确？
- [ ] 严格模式下「无法分类的凭证」是否可接受（勾稽报错）？
- [ ] 前端菜单名称：「所有者权益变动表」是否 OK？
