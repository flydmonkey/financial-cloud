# 现金流量表附表（间接法）自动调整 — 设计说明

日期：2026-08-28  
状态：Phase 1–3 已实现  
范围：附表 41/43/53/54/55 自动取数、56 倒挤、57 独立验算、strict 勾稽、Golden 三表 E2E、**多期 WC/现金衔接 E2E**、**附表 42/44/45/49/50/51/52 与主表 35 自动取数**

## 一、已确认决策

| # | 项 | 选择 |
|---|-----|------|
| 1 | 53/54/55 口径 | 与**资产负债表行项同源**（含往来重分类） |
| 2 | 应收/应付聚合 | 应收侧：`应收账款` + `预付款项`；应付侧：`应付账款` + `预收款项` |
| 3 | 43 折旧 | `1602` 累计折旧本期/本年**贷方发生额** |
| 4 | 自动项优先级 | 覆盖 `voucher_item_cash_flow` 同 code 手动值 |
| 5 | 56 其他 | 倒挤：`11 − Σ(41~55)` |
| 6 | 57 附表经营净额 | `Σ(41~56)` 独立计算，须与主表 `11` 一致 |
| 7 | strict | `financial-cloud.statement.cash-flow.strict-reconciliation` → 不平抛 `513015` |
| 8 | Golden 测试 | 合并进 `04-balance-sheet-golden-dataset.spec.ts` |

## 二、公式

### 主表（直接法，不变）

```
经营流入小计 − 流出小计 = 11-jy-lljh
11 + 24 + 34 + 35 = 36-xj-djje
37 + 36 = 38-xj-qmye
```

### 附表（间接法）

```
53 = 期初存货 − 期末存货          （BS 行「存货」）
54 = 期初经营性应收 − 期末        （BS「应收账款」+「预付款项」）
55 = 期末经营性应付 − 期初        （BS「应付账款」+「预收款项」）
41 = 利润表净利润（行 4）
43 = Σ 1602 贷方发生额（本期/累计）

56 = 11 − Σ(41,42,…,55)
57 = Σ(41~56)  （必须 = 11）
```

### 期初口径

| 场景 | 53/54/55 期初取数 |
|------|-------------------|
| 账套首月（`isSameMonth`） | 当月 BS 行 `initialBalance`（年初） |
| 非首月 | 上月 BS 行 `currentBalance`（上期末） |
| 本年累计列 | 当年 `initialBalance` vs 当期末 `currentBalance` |

## 三、代码落点

| 模块 | 职责 |
|------|------|
| `StatementCashFlowIndirectRules` | 53/54/55/43 + **42/44/45/49/50/51/52/35** 纯函数 |
| `StatementCashFlowRules` | 主表汇总 + 56 倒挤 + 57 求和 |
| `StatementBalanceSheetService.computeReportLineBalances` | BS 同源行余额 |
| `StatementReportService.applyIndirectCashFlowAdjustments` | 写入 period/year map |
| `StatementReportService.validateCashFlowReconciliation` | strict 513015 |

## 四、Golden Dataset 手算预期（首月）

| 步骤 | 11 | 41 | 43 | 53 | 56 | 57 | 38 |
|------|-----|-----|-----|-----|-----|-----|-----|
| 期初 | 0 | 0 | 0 | 0 | 0 | 0 | 100k |
| 采购 10k | -10k | 0 | 0 | -10k | 0 | -10k | 90k |
| 折旧 5k | -10k | 0 | 5k | -10k | — | — | 90k |
| 结转费用 | -10k | -5k | 5k | -10k | 0 | -10k | 90k |

> 折旧后、结转前：**不 assert 11=57**（41 尚未出表）；结转后完整勾稽。

## 五、Phase 3 — 其余附表行与汇率（已实现）

| 行 | code | 取数口径 |
|----|------|----------|
| 42 | `42-xj-jtzc` | `1505` + `1805` 本期/本年贷方发生额 |
| 44 | `44-xj-zctx` | `1702` 累计摊销贷方发生额 |
| 45 | `45-xj-fytx` | `1801` 长期待摊费用贷方发生额 |
| 49 | `49-xj-cwfy` | 利润表财务费用（config `sys.default.financialExpenses` 指向的行） |
| 50 | `50-xj-tzss` | `−` 利润表投资收益（行 `301`） |
| 51 | `51-xj-dyjs` | BS「递延所得税资产」期初 − 期末（fallback `1811` 科目余额） |
| 52 | `52-xj-dyzj` | BS「递延所得税负债」期末 − 期初（fallback `2901`） |
| 35 | `35-hl-djje` | `5603.02` 汇兑损失净额 − `5301.05` 汇兑收益净额 |

`StatementReportService.applyIndirectCashFlowAdjustments` 写入 period/year map，自动项覆盖凭证手动指定同 code 值。

## 六、非目标（后续）

- ~~42/44~52 其余附表行从科目余额全自动~~ → ✅ Phase 3
- ~~外币 35 汇率变动专项~~ → ✅ Phase 3（汇兑损益科目净额）
- ~~非首月 WC 期初跨月 E2E（`multi-period-flow` 扩展）~~ → ✅ CF-M01/M02
- 42 资产减值、51/52 递延所得税等复杂场景 Golden（可选扩展）

## 七、多期衔接（Phase 2）

| 用例 | 验证点 |
|------|--------|
| CF-M01 | 结账后 P2 的 `37-xj-qcye` = P1 的 `38-xj-qmye` |
| CF-M02 | P2 仅采购时 `53 = −采购额`（期初存货 = P1 期末，见 `StatementCashFlowIndirectRules` 非首月分支） |

Java UT：`StatementCashFlowIndirectRulesTest.crossMonthOpening_*`

## 八、测试映射

见 `docs/testing/jinbooks-voucher-report-test-cases.md` §3.3：TC-RPT-026~030、CF-G01~G05、CF-UT01~02、CF-B01、**CF-M01/M02**。
