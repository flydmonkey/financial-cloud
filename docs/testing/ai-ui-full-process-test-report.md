# 财务云 AI UI 全流程测试报告

- **环境**：`http://127.0.0.1:3154`（后端 `:2154`，MySQL `:3307`）
- **执行时间**：2026-09-30 / 2026-10-01（UTC）
- **版本**：财务云 v1.1.0
- **基线文档**：`docs/testing/ai-ui-full-process-test-prompt.md`
- **测试标识**：`AI-UI-20260930`
- **账套**：`AI-UI-20260930-主账套A`（小企业会计准则，启用凭证审核）
- **登录/写入**：用户允许**脚本注入**（登录 + Node/in-page fetch：draft→submit→audit→post、CF specify、结转）；月结向导与报表截图走 UI
- **脚本**：`financial-cloud-ui/scripts-ai-ui-continuation.mjs`、`scripts-ai-ui-month2.mjs`、`scripts-ai-ui-book-b.mjs`、`scripts-ai-ui-book-b-arap-fa.mjs`、`scripts-ai-ui-book-c.mjs`
- **明细**：`docs/testing/ai-ui-continuation-report.md`、`docs/testing/ai-ui-month2-report.md`、`docs/testing/ai-ui-book-b-report.md`、`docs/testing/ai-ui-book-b-arap-fa-report.md`、`docs/testing/ai-ui-book-c-report.md`

---

## 最终结论：**主账套两月闭环 + P0/P1 已修，全流程仍有条件不通过**

主账套已完成建账→期初→V01–V08→CF 指定→V04 反操作→1 月结转/月结→2 月五笔→2 月结转/月结；账期现为 **2026-03**。历史快照抽查：1 月银行 160,000 / 本年利润 40,000；2 月银行 188,000 / 本年利润 53,000。

**缺陷修复（`ec37ae8`）**：
1. **BUG-TZ-DATE（P0）** — 已修；`2026-03-01` 暂存 PASS（见 `ai-ui-verify-fixes-report.md`）
2. **BUG-CF-UI-SIGN（P1）** — 已修；UI 正数 autofill/保存 PASS；历史负余额已 ABS remediation，YTD 销售 50,000 等为正
3. **OBS-CARRY-STALE-POINTER** — 删除凭证时清理结转指针

仍不满足文档「全通过」标准：专项 B 工资/报销与 5.1 扩展、专项 D、导出内容级细查、2 月 CF 补全与间接法附表未齐。反结账/闭账期守卫与导出入口已点测（见 `ai-ui-guards-report.md`）。专项 C（关闭审核）已 PASS。B 往来/固资 5.2–5.3 核心路径 PASS。

---

## 执行汇总

| 分类 | 通过 | 失败 | 跳过 |
|---|---:|---:|---:|
| 登录建账期初 | 8 | 0 | 1 |
| 1 月凭证/报表/反操作/月结 | 主路径通过 | — | V04 CF 项待补 |
| 2 月凭证/结转/月结/历史快照 | 主路径通过 | — | 2 月 CF 指定未齐 |
| P0/P1 修复验证 | TZ+CF UI | — | — |
| 专项 B 出纳日记账 5.1 | 核心路径 PASS | CF 指定 WARN×2 | — |
| 专项 B 往来/固资 5.2–5.3 | 核心路径 PASS | — | 盘点/清理跳过 |
| 专项 C 关闭凭证审核 | PASS（免审核流） | — | — |
| 反结账/闭账守卫 + 导出入口 | PASS | — | 导出仅 smoke（xlsx 非空） |
| 专项 D / 导出细查 | 0 | 0 | D 未测 |

---

## 第一月（已结）

| 检查项 | 结果 |
|---|---|
| V01–V08 录审过（日期 2026-01-15） | PASS |
| 银行 160,000；结转后收入/成本/费用 0；本年利润 40,000 | PASS |
| 资产构成 242,000 | PASS |
| V04 反过账→11,000→恢复 10,000 | PASS |
| 月结五步 → 账期 2026-02 | PASS |
| 现金流量表符号（修复后 YTD） | PASS（销售 50k / 购货 8k / 投资 12k / 借款 40k；缺 V04 其他流出与 2 月项） |

## 第二月（已结）

| 业务 | 金额 | 结果 |
|---|---:|---|
| 赊销 | 30,000 | PASS |
| 收货款 | 40,000 | PASS |
| 销售成本 | 12,000 | PASS |
| 管理费 | 5,000 | PASS |
| 付供应商 | 7,000 | PASS |

结转前/结转后余额抽查：

| 科目 | 预期 | 实际 | 结果 |
|---|---:|---:|---|
| 1002 银行存款 | 188,000 | 188,000 | PASS |
| 1122 应收账款 | 40,000 | 40,000 | PASS |
| 1405 库存商品 | 8,000 | 8,000 | PASS |
| 1601 固定资产 | 12,000 | 12,000 | PASS |
| 2202 应付账款 | 5,000（贷） | 5,000 | PASS |
| 2001 短期借款 | 40,000（贷） | 40,000 | PASS |
| 3103 本年利润累计 | 53,000（贷） | 53,000 | PASS |

结算状态：`2026-01=6`、`2026-02=6`、`2026-03=1`；UI 当前账期 **2026年03月**。

历史快照：切换查询 2026-01 银行仍为 160,000、本年利润 40,000（与结账前一致）。

---

## 缺陷

### BUG-TZ-DATE（P0）— **FIXED** `ec37ae8`
原：开放账期首日被拒。现：`DateUtils` 用 `Asia/Shanghai`；复测 `2026-03-01` draft PASS。

### BUG-CF-UI-SIGN（P1）— **FIXED** `ec37ae8`
原：UI 强制负金额入库。现：绝对值 + 平衡校验仅对流出取负；复测 autofill/保存 PASS。闭账历史负余额已 DB ABS 纠正。

### OBS-UNAUDIT-TO-REVIEWING
反过账后再反审核进入「审核中」，需「撤回审核后修改」（产品路径，非阻断）。

### OBS-CARRY-STALE-POINTER / 重复生成 — **部分 FIXED**
删除结转凭证时清理 `settlement_carryforward`。向导重复生成仍可能产生重复件，需操作纪律。

### BUG-FA-SQL-DATE（P1）— **FIXED + 重启后复测 PASS**
原 `java.sql.Date.toInstant` 崩溃。`299caec` 改为 `java.util.Date`（`Asia/Shanghai`）。重启后端后，`startUseDate=2025-12-15` + `entryPeriod=2026-01` 建卡「新增成功，已生成购入凭证」；探测卡已清理。

---

## 专项账套 B 进度

### 5.1 出纳日记账 — PASS
- **账套**：`AI-UI-20260930-专项B`，**bookId** `2105448444973871105`，启用 `2026-01`，凭证审核开启
- **期初**：总账 1002 / 日记账均为 10,000（期初流水 `direction=o`，不生成凭证）
- **流水**：收入 3,000 + 支出 1,000 → 日记账余额 **12,000**
- **生成凭证→过账**：过账前总账 1002=10,000；过账后 **12,000**
- **银行对账**：对账单 12,000，勾对后差额 **0**
- **明细**：`docs/testing/ai-ui-book-b-report.md`；截图 `bookb-*`

### 5.2 往来与核销 — PASS
- 启用辅助核算；1122↔客户、2202↔供应商；单位 `AI-UI-20260930-客户B/供应商B`
- 过账后客户应收 **6,000**、供应商应付 **5,000**；明细一致
- 部分核销收款 4,000：未清挂账剩余 6,000；核销不改总账；撤销后状态恢复、总账不变
- **明细**：`docs/testing/ai-ui-book-b-arap-fa-report.md`；截图 `bookb-arap-*`

### 5.3 固定资产 — PASS（盘点/清理跳过）
- 类别+卡片原值 12,000 / 残值 0% / 12 月直线法 / 费用 5602.02；UI 购入凭证过账（1601/1002）
- 2026-01 计提折旧 1,000 过账后：原值 12,000 / 累计折旧 1,000 / 净值 **11,000**
- 见缺陷 **BUG-FA-SQL-DATE**；盘点/清理深路径按任务跳过
- 截图 `bookb-fa-*`

### 未测（B 其余）
工资、报销；日记账回写/红冲/未达项 500；盘点/清理深路径

---

## 专项账套 C 进度（关闭凭证审核 §六）

- **账套**：`AI-UI-20260930-专项C`，**bookId** `2105453230146252802`，启用 `2026-01`，**voucherReviewed=0**
- **期初**：银行/实收资本各 10,000
- **凭证**：费用 100 / 银行 100（摘要含 `AI-UI-20260930-C-V01`）
- **状态流**：`draft` → `submit` → **`completed`（无审核人）** → `sender` 过账 → `unsender` 反过账 → `unaudit` → `draft` → 删除
- **余额**：提交未过账银行仍 10,000；过账后 **9,900**；反过账恢复 10,000；利润表管理费用过账后 100
- **观察**：新建账套当前账期可能继承创建者账套（曾为 2026-03），脚本强制回启用月；工具栏仍显示「审核」split-button，但提交后无需 reviewer
- **明细**：`docs/testing/ai-ui-book-c-report.md`；截图 `bookc-*`

---

## 反结账 / 闭账期守卫 / 导出（主账套 A）

- 闭账期修改与反过账均被拦截（开放账期 2026-03）  
- 非最近已结月反结账被拒；最近已结月（2026-02）可反结账，UI 有入口  
- 资产负债表导出可下载非空 xlsx（内容级行列校验未做）  
- 明细：`docs/testing/ai-ui-guards-report.md`；账期已恢复为 **2026-03** 开放  

---

## 未执行 / 进行中

1. 专项账套 B 工资/报销、5.1 扩展、固资盘点/清理  

2. 专项 D（年末）  
3. 导出 Excel/PDF 内容级校验、间接法附表  
4. 补全 V04 / 2 月现金流量指定后的完整勾稽  

---

## 证据

- `/opt/cursor/artifacts/screenshots/`（`m2-*`、`verify-cf-*`、`hist-*`、`bookb-*`、`bookb-arap-*`、`bookb-fa-*`、`bookc-*`、`guards-*` 等）  
- `docs/testing/ai-ui-continuation-report.md`  
- `docs/testing/ai-ui-month2-report.md`  
- `docs/testing/ai-ui-verify-fixes-report.md`  
- `docs/testing/ai-ui-book-b-report.md`  
- `docs/testing/ai-ui-book-b-arap-fa-report.md`  
- `docs/testing/ai-ui-book-c-report.md`  
- `docs/testing/ai-ui-guards-report.md`  


