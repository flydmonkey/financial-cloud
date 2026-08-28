# 费用明细表 — 设计说明

日期：2026-08-28  
状态：已确认  
范围：jinbooks Phase 1 — 多期科目树矩阵 + 年度合计列 + 导出（不含图表）

## 一、已确认决策

| # | 项 | 选择 |
|---|-----|------|
| 1 | MVP 范围 | 多期矩阵表 + 底部合计行 + Excel 导出；**不含**图表、同比、打印 |
| 2 | 列结构 | 编码、名称、动态月份列（`YYYY年M期`）、**最右固定年度合计列** |
| 3 | 年度合计列 | **始终显示**；单年区间表头 `{year}年合计`，跨年显示 `区间合计` |
| 4 | 年度合计取值 | 当前查询区间内各月发生额之和（与可见月份列一致） |
| 5 | 默认科目 | 5601 销售费用、5602 管理费用、5603 财务费用及其下级 |
| 6 | 发生额口径 | 费用类科目：`PROFIT_AND_LOSS_AMOUNT` = \|借方\| − \|贷方\| |
| 7 | 取数来源 | `voucher_item` + `voucher`（`status=completed`），按科目 + 月份聚合 |
| 8 | 树形结构 | `book_subject` 层级；父级 = 子级汇总（rollup） |
| 9 | 编码兼容 | `SubjectCodeCompat` 处理 6601/6602/6603 → 5601/5602/5603 |
| 10 | 期间类型 | `periodType=between`，`dateRange=[起始月, 结束月]`（`yyyy-MM`） |
| 11 | 持久化 | 仅实时计算，不写入 `statement_*` 快照表 |
| 12 | 导出 | EasyExcel 程序化写入（动态列 + 年度合计列） |
| 13 | 报表定位 | **期间管理报表**，利润表「期间费用」行的科目级展开 |
| 14 | 利润表勾稽 | 一级费用各行各月合计 **必须** 与利润表对应行一致（容差 0.01） |
| 15 | 研发费用 | 小企业准则无独立利润表行；Phase 1 默认不含，可通过科目筛选扩展 |

## 二、背景与目标

### 2.1 背景

jinbooks 现有报表（资产负债、利润、现金流、科目余额、凭证汇总）均为**单期列**或固定行次结构，缺少「科目树 × 多月份矩阵」的费用分析报表。仪表盘虽有费用饼图与单科目趋势，但无法按科目层级查看各月发生额明细。

### 2.2 目标

1. 新增 **费用明细表**，支持期间区间查询（如 2023-01 ~ 2023-12）
2. 树形展示 5601/5602/5603 及其下级科目，每列对应一个会计期间的发生额
3. 最右列展示区间内合计（年度合计 / 区间合计）
4. 底部合计行汇总各列
5. 支持 Excel 导出

### 2.3 非目标（Phase 1）

- 变动趋势折线图、占比环形图（Phase 2）
- 同比列、工具栏「显示同比」
- 打印模板
- 「展示发生额为 0 的科目」开关（默认隐藏零发生额科目）
- 「明细科目显示全称」开关
- 行级钻取迷你图 / 跳转明细账（Phase 2 可选）

---

## 三、报表结构

### 3.1 列布局

```
| 编码   | 名称       | 2023年1期 | 2023年2期 | ... | 2023年12期 | 2023年合计 |
|--------|------------|-----------|-----------|-----|------------|------------|
| 5601   | 销售费用   | 39,431.72 | 37,733.72 | ... | ...        | 403,320.04 |
| 560102 |   房租     |  ...      |  ...      | ... | ...        |  ...       |
| 5602   | 管理费用   |  ...      |  ...      | ... | ...        |  ...       |
| 5603   | 财务费用   |  ...      |  ...      | ... | ...        |  ...       |
|--------|------------|-----------|-----------|-----|------------|------------|
| 合计   |            |  Σ        |  Σ        | ... |  Σ         |  Σ         |
```

- 月份列数 = `dateRange` 内月份数（`getAllMonths()`）
- 年度合计列 `fixed="right"`，横向滚动时保持可见
- 一级科目行（5601/5602/5603）可使用浅蓝背景区分（前端样式，非必须）

### 3.2 查询条件

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `periodType` | string | `between` | 固定为月份区间 |
| `dateRange` | string[2] | 当年 1 月 ~ 当前账期 | `yyyy-MM` 格式 |
| `subjectCodes` | string[] | `5601,5602,5603` | 支持逗号分隔或范围（如 `5601,5602,2121-2131`） |
| `maxLevel` | int | `0` | `0`=至末级；`1`=仅一级；`2`=至二级… |
| `showAux` | boolean | `false` | 显示辅助核算明细 |
| `postedOnly` | boolean | `true` | 仅统计已过账凭证 |

### 3.3 零发生额处理

- 默认：**隐藏**整行所有月份及年度合计均为 0 的末级科目
- 若某父级下有非零子级，父级仍显示（rollup 后可能非零）
- Phase 2 增加「展示发生额为 0 的科目」开关

---

## 四、费用明细表计算逻辑

### 4.1 报表定位

费用明细表是**期间报表**，反映某一期间内各项费用（销售费用、管理费用、财务费用等）的发生、构成与明细，是利润表「期间费用」行的**科目级展开与支撑明细**。

> 不属于法定四大主表，属于**内部管理报表 / 主表附表**，用于费用管控、成本分析和期间费用归集。  
> jinbooks Phase 1 聚焦「科目 × 月份矩阵 + 利润表勾稽」；预算对比、同比环比、占比分析留 Phase 2。

### 4.2 费用分类体系

费用先按**会计科目**分类（主维度），再按**辅助核算**二次展开（可选）：

#### 4.2.1 一级费用 ↔ jinbooks 科目映射

| 一级费用 | 小企业准则科目 | 企业准则别名 | 利润表 config 键 | 典型明细（标准科目表） |
|----------|----------------|--------------|------------------|------------------------|
| 销售费用 | `5601` | `6601` | `sys.default.sellingExpenses` | 职工薪酬、运输费、广告费、业务招待费… |
| 管理费用 | `5602` | `6602` | `sys.default.administrativeExpenses` | 职工薪酬、办公费、折旧摊销、中介费… |
| 财务费用 | `5603` | `6603` | `sys.default.financialExpenses` | 利息支出、利息收入（红字）、汇兑损益、手续费 |
| 研发费用 | 无独立一级科目* | — | — | `4301.01` 费用化支出（见说明） |

\* **研发费用说明**：jinbooks 默认小企业会计准则利润表**无单独「研发费用」行**，研发支出通过 `4301 研发支出 / 4301.01 费用化支出` 核算，费用化结转后通常计入 `5602 管理费用`。Phase 1 默认科目范围为 `5601,5602,5603`；若需单独列示研发，用户可在科目筛选中加入 `4301.01` 或账套自定义研发明细科目。

#### 4.2.2 管理维度（辅助核算）

| 维度 | 来源 | Phase 1 |
|------|------|---------|
| 部门、项目、员工等 | `voucher_auxiliary` + 科目 `auxiliary` 配置 | `showAux=true` 时按辅助核算项展开子行 |
| 勾稽要求 | 辅助核算明细合计 = 对应科目总额 | **必须**（同一科目、同一期间） |

### 4.3 核心计算逻辑

#### 4.3.1 发生额归集（基础）

```
某费用科目某月发生额
= normalizePeriodAmount(Σ借方, Σ贷方, PROFIT_AND_LOSS_AMOUNT)
= |Σ debit_amount| − |Σ credit_amount|
```

**取数口径**（与利润表 `StatementIncomeService` 一致）：

- 数据源：`voucher_item` ⋈ `voucher`
- 凭证状态：`voucher.status = 'completed'`
- 过账过滤：`postedOnly=true`（默认）→ `voucher.sender_id` 非空
- 期间归属：按 **`voucher_date` 所在自然月** 归集（与利润表、总账一致）
- 删除标记：`deleted = 'n'`

**红字 / 备抵处理**：

- 费用类科目贷方发生（如财务费用-利息收入、费用冲销）通过 `|借| − |贷|` 以**负数或抵减**反映
- 最终一级科目净额 = 各明细净额之和

#### 4.3.2 单期合计（矩阵列合计 / 底部行）

```
某月费用合计(period) = Σ 各一级费用科目该月发生额
                      = 5601(period) + 5602(period) + 5603(period) + …
```

#### 4.3.3 区间 / 年度合计（最右列）

```
某行 yearTotal = Σ amounts[period]   （period ∈ 当前查询 dateRange）
```

- 单年区间：表头 `{year}年合计`
- 跨年区间：表头 `区间合计`
- **yearTotal 恒等于该行各可见月份列之和**

#### 4.3.4 本年累计（与利润表「本年累计」列对齐，Phase 1 内部校验用）

```
某科目本年累计(至 month M) = Σ 发生额(subject, 当年1月 … M)
```

- Phase 1 **不在 UI 展示**累计列，但 Service 层计算单月数据时可复用此口径
- 用于与利润表 `cumulativeBalance` 勾稽（见 4.6）

#### 4.3.5 管理分析指标（Phase 2，本期不算）

| 指标 | 公式 |
|------|------|
| 费用执行率 | 本期实际 ÷ 本期预算 |
| 同比 / 环比 | (本期 − 对比期) ÷ 对比期 |
| 费用占比 | 某明细 ÷ 费用合计（或 ÷ 营业收入） |

### 4.4 关键计算要点

1. **期间口径**：只统计查询区间内、已过账凭证；跨期预提/摊销以凭证 **`voucher_date`** 为准（与现有总账一致）
2. **资本化 vs 费用化**：`4301.02 资本化支出` **不进入**费用明细表；仅费用化部分（`4301.01` 及转入 560x 的分录）计入
3. **费用 vs 成本**：`5001 主营业务成本` 等成本类科目**不在**本表范围；仅期间费用科目（560x 及用户筛选范围）
4. **红字 / 备抵**：贷方冲减以净额体现，保证财务费用等净额准确
5. **辅助核算聚合**：`showAux=true` 时，同一科目下各辅助核算项之和 **必须等于** 该科目未展开时的总额
6. **父级 rollup**：中间节点不直接取凭证，由直接子级汇总，避免重复计算

### 4.5 程序化实现流程

```
1. 解析 StatementParamsDto（periodType=between, dateRange, subjectCodes, postedOnly）
2. getAllMonths() → periods 列表
3. SQL 聚合：selectExpenseAmountByMonth（subject_code × yearPeriod → debit/credit）
4. 对每个 (subject, month) 应用 StatementIncomeRules.normalizePeriodAmount
5. 加载 book_subject 树，按 subjectCodes 过滤 + 补全祖先节点
6. 末级填 amounts；自底向上 rollup 父级
7. showAux=true → 追加辅助核算维度子行，并校验与步骤 4 总额一致
8. 过滤零发生额末级；按 maxLevel 截断
9. 计算 yearTotal、totals 行
10. 利润表勾稽（4.6）→ 通过后方可返回
11. 组装 StatementExpenseDetailReport 响应
```

### 4.6 与利润表的勾稽

费用明细表一级各行必须与利润表对应期间费用行 **逐月一致**：

```
费用明细表 5601 某月合计 = 利润表「销售费用」行 该月 currentBalance
费用明细表 5602 某月合计 = 利润表「管理费用」行 该月 currentBalance
费用明细表 5603 某月合计 = 利润表「财务费用」行 该月 currentBalance
```

**实现方式**（复用现有利润表引擎，避免双口径）：

1. 从 `config` 读取行次映射：
   - `sys.default.sellingExpenses` → itemCode（如 `105`）
   - `sys.default.administrativeExpenses` → itemCode（如 `106`）
   - `sys.default.financialExpenses` → itemCode（如 `107`）
2. 对每个 `period ∈ periods`，构造单月 `StatementParamsDto`（`periodType=month`, `reportDate=period`）
3. 调用 `StatementIncomeService.generateIncomeStatement(dto, false)` 或等价 `accumulateLineAmount` 路径，取对应 itemCode 的 `currentBalance`
4. 与费用明细表一级科目行 `amounts[period]` 比较，容差 `StatementIncomeRules.FORMULA_TOLERANCE`（0.01）

**不一致时**：

| 模式 | 行为 |
|------|------|
| 默认（Phase 1） | `log.warn` + 响应附加 `reconciliationWarnings[]` |
| strict（Phase 2 配置项） | 抛业务异常，阻止出表 |

**注意**：利润表行次通过 `statement_rules` 归集科目，可能与纯 `560x` 前缀不完全相同（如规则含别名科目）。勾稽以 **config 指向的利润表行** 为准；费用明细表一级行展示科目树 rollup 结果，两者应一致——若账套规则配置标准，则自动对齐。

### 4.7 树形汇总与合计行（实现细节）

#### 科目范围

1. 解析 `subjectCodes`：精确码、逗号列表、区间（`2121-2131`）
2. `SubjectCodeCompat.expandLookupCodes()` 展开准则别名
3. 从 `book_subject` 加载匹配科目及祖先节点
4. 按 `maxLevel` 截断显示深度

#### 父级 rollup

- 末级科目：直接取 SQL 聚合 + normalize
- 中间/一级科目：`amounts[period] = SUM(直接子级 amounts[period])`
- `yearTotal = SUM(amounts[period])`

#### 底部合计行

- `totals[period] = SUM(一级费用科目 amounts[period])`
- `totals.yearTotal = SUM(totals[period])`

---

## 五、API 设计

### 5.1 查询

```
GET /api/statement/expense-detail
```

**Query 参数**（Spring 绑定 `StatementParamsDto` + 扩展）：

| 参数 | 示例 |
|------|------|
| `periodType` | `between` |
| `dateRange` | `2023-01` & `2023-12`（数组或 `dateRangeStart`/`dateRangeEnd`） |
| `subjectCodes` | `5601,5602,5603` |
| `maxLevel` | `0` |
| `showAux` | `false` |
| `postedOnly` | `true` |

**响应** `StatementExpenseDetailReport`：

```json
{
  "periods": ["2023-01", "2023-02", "2023-12"],
  "yearLabel": "2023年合计",
  "items": [
    {
      "sourceId": "abc",
      "parentId": null,
      "subjectCode": "5601",
      "subjectName": "销售费用",
      "level": 1,
      "amounts": {
        "2023-01": 39431.72,
        "2023-02": 37733.72
      },
      "yearTotal": 403320.04,
      "children": []
    }
  ],
  "totals": {
    "2023-01": 50000.00,
    "2023-02": 48000.00,
    "yearTotal": 509782.00
  },
  "reconciliationWarnings": []
}
```

`reconciliationWarnings`（可选）：利润表勾稽偏差超过容差时返回，元素含 `{ subjectCode, period, detailAmount, incomeAmount, diff }`。

`yearLabel` 规则：

- `periods` 全部属于同一年 → `{year}年合计`
- 跨年 → `区间合计`

### 5.2 导出

```
GET /api/statement/expense-detail/export
```

- 参数同查询接口
- 响应：`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- 文件名：`费用明细表_{bookName}_{start}-{end}.xlsx`
- 列：编码、名称、各月、`yearLabel` 列、底部合计行

---

## 六、代码落点

| 模块 | 文件 | 职责 |
|------|------|------|
| DTO | `StatementExpenseDetailReport.java` | 响应根对象 |
| DTO | `StatementExpenseDetailItem.java` | 树节点（含 `amounts`、`yearTotal`、`children`） |
| Rules | `StatementExpenseDetailRules.java` | rollup、合计、零行过滤、`yearLabel`、勾稽比对 |
| Reconcile | `StatementExpenseDetailService.reconcileWithIncome()` | 逐月调用利润表引擎，生成 warnings |
| Mapper | `VoucherItemMapper.selectExpenseAmountByMonth` | 按 subject + month 聚合 |
| Service | `StatementExpenseDetailService.java` | 编排：解析参数 → 查数 → 建树 → 返回 |
| Controller | `StatementReportController` | 新增 `expense-detail`、`expense-detail/export` |
| Params | `StatementParamsDto` | 补充 `between` 的 `parse()` 逻辑（若 `dateRange` 未设则报错） |
| Params | `StatementParamsDto.maxLevel` | 新增字段 |
| 前端页面 | `views/statement/expense-detail.vue` | 查询区 + 动态列树表 + 导出 |
| 前端 API | `api/statement/statement-expense-detail.ts` | 封装请求 |
| 菜单 | `resources` / seed SQL | 注册 `statement/expense-detail` |

### 6.1 SQL 概要

```sql
SELECT
  i.subject_code AS subjectCode,
  DATE_FORMAT(i.voucher_date, '%Y-%m') AS yearPeriod,
  SUM(i.debit_amount) AS debitAmount,
  SUM(i.credit_amount) AS creditAmount
FROM voucher_item i
JOIN voucher v ON ...
WHERE v.book_id = #{bookId}
  AND v.status = 'completed'
  AND i.voucher_date BETWEEN #{dateRangeStart} AND #{dateRangeEnd}
  AND (subject_code LIKE '5601%' OR subject_code LIKE '5602%' OR ...)
GROUP BY i.subject_code, DATE_FORMAT(i.voucher_date, '%Y-%m')
ORDER BY i.subject_code, yearPeriod
```

Java 层对每行应用 `StatementIncomeRules.normalizePeriodAmount(debit, credit, PROFIT_AND_LOSS_AMOUNT)`。

### 6.2 复用关系

| 现有模块 | 复用方式 |
|----------|----------|
| `StatementParamsDto.getAllMonths()` | 生成 `periods` 列表 |
| `SubjectCodeCompat` | 科目码兼容 |
| `StatementIncomeRules.normalizePeriodAmount` | 发生额口径 |
| `subject-balance.vue` | 前端树表、展开层级、导出按钮模式 |
| `StatementIncomeService.accumulateLineAmount` | 利润表勾稽（4.6） |
| `StatementIncomeRules.FORMULA_TOLERANCE` | 勾稽容差 0.01 |
| `ConstsSysConfig.SYS_DEFAULT_*_EXPENSES` | 一级费用 ↔ 利润表行次 |

---

## 七、前端设计

### 7.1 页面结构

```
app-container
├── el-card (查询区)
│   ├── 期间区间：双月 picker → dateRange
│   ├── 科目：文本输入（5601,5602,5603）
│   ├── 显示科目级次：下拉（至末级 / 1级 / 2级…）
│   ├── 显示辅助核算：switch
│   └── 查询按钮
├── el-card (表格区)
│   ├── 工具栏：展开所有级次 checkbox、导出按钮
│   └── el-table (tree, show-summary)
│       ├── 编码、名称（tree-props）
│       ├── v-for periods → 动态月份列
│       └── yearTotal 列 (fixed="right")
```

### 7.2 期间选择器

- 使用 `el-date-picker type="monthrange"`
- 显示格式：`YYYY年MM期`
- 默认值：账套当前年度 1 月 ~ 当前账期（读 `SYS_PAYMENT_TERM_CURRENT` 或前端 bookStore）
- 变更后组装：`periodType=between`, `dateRange=[start, end]`

### 7.3 合计行

- `show-summary` + `summary-method`
- 第一列显示「合计」
- 各月列取 `totals[period]`
- 最后一列取 `totals.yearTotal`

---

## 八、错误处理

| 场景 | 错误码 / 行为 |
|------|---------------|
| `bookId` 为空 | 现有 `BOOK_ID_EMPTY` |
| `dateRange` 缺失或非法 | `DATE_RANGE_SIZE` / 新增 `INVALID_DATE_RANGE` |
| 起始月 > 结束月 | `START_DATE_AFTER_END` |
| 区间超过 24 个月 | 新增 `EXPENSE_DETAIL_PERIOD_TOO_LONG`（建议上限 24 期，防性能问题） |
| 无匹配科目 | 返回空 `items`，`totals` 全 0 |

---

## 九、测试计划

### 9.1 单元测试 — `StatementExpenseDetailRulesTest`

- rollup：子级汇总到父级
- `yearTotal` = 各月之和
- 零发生额末级过滤
- 跨年 `yearLabel` = `区间合计`
- `maxLevel` 截断

### 9.2 集成测试 — `StatementExpenseDetailServiceTest`

- Golden 凭证：5601 子科目各月借方发生额
- 与利润表对应行（销售/管理/财务费用）**逐月** `currentBalance` 一致
- 勾稽偏差 > 0.01 时 `reconciliationWarnings` 非空
- 辅助核算展开后明细合计 = 科目总额

### 9.3 单元测试 — 勾稽

- mock 利润表行 105/106/107 与 5601/5602/5603 rollup 一致 → warnings 为空
- 故意偏差 → warnings 含正确 diff

### 9.4 E2E（可选 Phase 1.1）

- `e2e/06-expense-detail-golden-dataset.spec.ts`
- 录入 5601/5602 凭证 → 打开费用明细表 → 断言矩阵单元格与合计列

---

## 十、Phase 2 预留

| 功能 | 说明 |
|------|------|
| 图表区 | 变动趋势折线 + 占比环形图；数据可从同一 API 扩展 `chartSummary` |
| 预算 / 同比 / 占比 | 管理分析指标（4.3.5） |
| 显示零发生额 | `showZero=true` 参数 |
| 明细全称 | 读 `book_subject.fullName` |
| 打印 | 复用导出 PDF 或专用打印模板 |
| 行级钻取 | 跳转 `/voucher/sub-ledger?subjectCode=...&period=...` |

---

## 十一、工作量估算

| 模块 | 预估 |
|------|------|
| 后端 Service + SQL + Rules + DTO | 1.5 天 |
| `StatementParamsDto` between 完善 + maxLevel | 0.25 天 |
| 后端动态列导出 | 0.5 天 |
| 前端 expense-detail.vue | 1 天 |
| 单元/集成测试 + 菜单 | 0.75 天 |
| **合计** | **~4 天** |

---

## 十二、验收标准

1. 选择 2023-01 ~ 2023-12，默认展示 5601/5602/5603 树形矩阵，12 个月份列 + `2023年合计` 列
2. 各末级科目金额与凭证借方发生额（减贷方）一致
3. 父级金额 = 子级之和；底部合计 = 三个一级科目之和
4. 年度合计列 = 该行各月之和；合计行年度合计 = 各月合计之和
5. 导出 Excel 列与页面一致
6. 零发生额末级科目默认不显示
7. **5601/5602/5603 一级行各月金额 = 利润表对应行 `currentBalance`（容差 0.01）**
8. 辅助核算模式下，明细合计 = 科目总额
